package com.fongmi.android.tv.service;

import com.fongmi.android.tv.bean.TmdbConfig;
import com.fongmi.android.tv.bean.TmdbItem;

import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;
import com.fongmi.android.tv.bean.TmdbPerson;
import com.fongmi.android.tv.ui.helper.TmdbMatchPolicy;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TmdbDisambiguationRequestTest {

    @Test
    public void disambiguationUrlRequestsOnlyCreditBlocks() throws Exception {
        TmdbService service = new TmdbService();
        TmdbConfig config = config("https://api.tmdb.org/3");
        Method detailUrl = TmdbService.class.getDeclaredMethod(
                "detailUrl", TmdbItem.class, TmdbConfig.class, boolean.class, boolean.class);
        detailUrl.setAccessible(true);

        String tv = (String) detailUrl.invoke(service, new TmdbItem(123, "tv", "Example TV", "", "", "", ""), config, false, true);
        String movie = (String) detailUrl.invoke(service, new TmdbItem(456, "movie", "Example Movie", "", "", "", ""), config, false, true);

        assertEquals("credits,aggregate_credits", HttpUrl.parse(tv).queryParameter("append_to_response"));
        assertEquals("credits", HttpUrl.parse(movie).queryParameter("append_to_response"));
    }

    @Test
    public void litePayloadRetainsEveryDisambiguationScoringInput() throws Exception {
        TmdbService service = new TmdbService();
        TmdbConfig config = config("https://api.tmdb.org/3");
        JsonObject full = JsonParser.parseString("""
                {"id":1399,"name":"测试剧","first_air_date":"2011-04-17","origin_country":["US"],
                 "number_of_seasons":2,"seasons":[{"season_number":1,"episode_count":10},{"season_number":2,"episode_count":10}],
                 "credits":{"cast":[{"id":1,"name":"演员甲","character":"角色甲","known_for_department":"Acting"}],"crew":[]},
                 "aggregate_credits":{"cast":[{"id":1,"name":"演员甲","roles":[{"character":"角色甲"}],"known_for_department":"Acting"}],"crew":[]},
                 "images":{"posters":[{"file_path":"/poster.jpg"}]},"translations":{"translations":[]},
                 "external_ids":{"imdb_id":"tt0000001"},"content_ratings":{"results":[]},
                 "recommendations":{"results":[]},"similar":{"results":[]}}
                """).getAsJsonObject();
        JsonObject lite = full.deepCopy();
        for (String field : List.of("images", "translations", "external_ids", "content_ratings", "recommendations", "similar")) lite.remove(field);

        assertEquals(describe(service.cast(full, config)), describe(service.cast(lite, config)));
        for (String title : List.of("测试剧 第一季", "测试剧 分季版", "测试剧", "别名的剧 第2季")) {
            assertEquals(TmdbMatchPolicy.splitSeasonDetailScore(title, full), TmdbMatchPolicy.splitSeasonDetailScore(title, lite));
        }
    }

    @Test
    public void detailLiteUsesDedicatedCacheAndDetailFreshnessPolicy() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/fongmi/android/tv/service/TmdbService.java"), StandardCharsets.UTF_8);
        assertTrue(source.contains("requestJson(url, config, \"detail-lite\", disambiguationCacheKey(item, config)"));
        assertTrue(source.contains("\"detail\".equals(type) || \"detail-lite\".equals(type)"));
    }

    private static List<String> describe(List<TmdbPerson> people) {
        List<String> values = new ArrayList<>();
        for (TmdbPerson person : people) values.add(person.getPersonId() + "|" + person.getName() + "|" + person.getSubtitle());
        return values;
    }

    private static TmdbConfig config(String apiBase) throws Exception {
        TmdbConfig config = new TmdbConfig();
        set(config, "apiBase", apiBase);
        set(config, "apiKey", "test-key");
        set(config, "language", "zh-CN");
        return config.sanitize();
    }

    private static void set(TmdbConfig config, String name, String value) throws Exception {
        Field field = TmdbConfig.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(config, value);
    }
}
