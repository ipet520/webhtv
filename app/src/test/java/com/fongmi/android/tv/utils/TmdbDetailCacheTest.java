package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.bean.TmdbEpisode;
import com.fongmi.android.tv.bean.TmdbItem;
import com.fongmi.android.tv.bean.TmdbPerson;
import com.google.gson.JsonObject;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

public class TmdbDetailCacheTest {

    @Test
    public void putReturnsEmptyKeyForMissingItemOrDetail() {
        TmdbItem item = new TmdbItem(123, "tv", "庆余年", "", "", "", "");
        JsonObject detail = new JsonObject();

        assertTrue(TmdbDetailCache.put(null, detail, List.of()).isEmpty());
        assertTrue(TmdbDetailCache.put(item, null, List.of()).isEmpty());
    }

    @Test
    public void takeReturnsCachedDetailOnceForExpectedTmdbItem() {
        TmdbItem item = new TmdbItem(123, "tv", "庆余年", "", "", "", "");
        JsonObject detail = new JsonObject();
        detail.addProperty("name", "庆余年");
        List<TmdbPerson> cast = new ArrayList<>();
        cast.add(new TmdbPerson(1, "张若昀", "范闲", "", "Acting", ""));

        String key = TmdbDetailCache.put(item, detail, cast, "zh-Hans");
        cast.clear();

        TmdbDetailCache.Entry entry = TmdbDetailCache.take(key, item, "zh-CN");

        assertTrue(!key.isEmpty());
        assertSame(item, entry.getItem());
        assertSame(detail, entry.getDetail());
        assertEquals(1, entry.getCast().size());
        assertEquals("张若昀", entry.getCast().get(0).getName());
        assertNull(TmdbDetailCache.take(key, item));
    }

    @Test
    public void takeRejectsSnapshotFromAnotherLanguage() {
        TmdbItem item = new TmdbItem(123, "tv", "庆余年", "", "", "", "");
        JsonObject detail = new JsonObject();

        String key = TmdbDetailCache.put(item, detail, List.of(), "en-US");

        assertNull(TmdbDetailCache.take(key, item, "zh-CN"));
    }

    @Test
    public void takeAcceptsEquivalentLanguageIdentity() {
        TmdbItem item = new TmdbItem(123, "tv", "庆余年", "", "", "", "");
        JsonObject detail = new JsonObject();

        String key = TmdbDetailCache.put(item, detail, List.of(), "zh-Hans");

        assertEquals("zh-CN", TmdbDetailCache.take(key, item, "zh-CN").getLanguage());
    }

    @Test
    public void takeRejectsRegionalChineseVariantForSimplifiedTarget() {
        TmdbItem item = new TmdbItem(123, "tv", "慶餘年", "", "", "", "");
        JsonObject detail = new JsonObject();

        String key = TmdbDetailCache.put(item, detail, List.of(), "zh-TW");

        assertNull(TmdbDetailCache.take(key, item, "zh-CN"));
    }

    @Test
    public void takeReturnsNullForMismatchedTmdbItem() {
        TmdbItem item = new TmdbItem(123, "tv", "庆余年", "", "", "", "");
        TmdbItem other = new TmdbItem(456, "tv", "庆余年 第二季", "", "", "", "");
        JsonObject detail = new JsonObject();

        String key = TmdbDetailCache.put(item, detail, List.of(), "zh-CN");

        assertNull(TmdbDetailCache.take(key, other, "zh-CN"));
    }

    @Test
    public void takeCarriesSeasonEpisodesAsDefensiveSnapshot() {
        TmdbItem item = new TmdbItem(123, "tv", "庆余年", "", "", "", "");
        JsonObject detail = new JsonObject();
        List<TmdbEpisode> mutable = new ArrayList<>();
        mutable.add(episode(1, "初入京都", 1));
        Map<Integer, List<TmdbEpisode>> seasons = new LinkedHashMap<>();
        seasons.put(1, mutable);

        String key = TmdbDetailCache.put(item, detail, List.of(), "zh-CN", seasons);
        mutable.clear();
        TmdbDetailCache.Entry entry = TmdbDetailCache.take(key, item, "zh-CN");

        assertNotNull(entry);
        assertEquals(1, entry.getSeasonEpisodes().get(1).size());
        assertEquals("初入京都", entry.getSeasonEpisodes().get(1).get(0).getTitle());
        Map<Integer, List<TmdbEpisode>> handed = entry.getSeasonEpisodes();
        handed.clear();
        assertEquals(1, entry.getSeasonEpisodes().size());
        assertThrows(UnsupportedOperationException.class,
                () -> entry.getSeasonEpisodes().get(1).add(episode(2, "追加", 1)));
    }

    @Test
    public void takeDropsInvalidOrEmptySeasonEntries() {
        TmdbItem item = new TmdbItem(123, "tv", "庆余年", "", "", "", "");
        Map<Integer, List<TmdbEpisode>> seasons = new LinkedHashMap<>();
        seasons.put(-1, List.of(episode(1, "错误季", -1)));
        seasons.put(2, List.of());
        seasons.put(1, new ArrayList<>(java.util.Arrays.asList(null, episode(1, "有效集", 1))));
        seasons.put(null, List.of(episode(1, "空季号", 1)));

        TmdbDetailCache.Entry entry = TmdbDetailCache.take(
                TmdbDetailCache.put(item, new JsonObject(), List.of(), "zh-CN", seasons), item, "zh-CN");

        assertEquals(1, entry.getSeasonEpisodes().size());
        assertFalse(entry.getSeasonEpisodes().containsKey(-1));
        assertFalse(entry.getSeasonEpisodes().containsKey(2));
        assertEquals("有效集", entry.getSeasonEpisodes().get(1).get(0).getTitle());
    }

    @Test
    public void takeWithoutSeasonEpisodesReportsNoHandoff() {
        TmdbItem item = new TmdbItem(123, "tv", "庆余年", "", "", "", "");
        String key = TmdbDetailCache.put(item, new JsonObject(), List.of(), "zh-CN");

        assertNull(TmdbDetailCache.take(key, item, "zh-CN").getSeasonEpisodes());
    }

    @Test
    public void adapterUsesTheSameSeasonKeyForSnapshotAndReads() throws Exception {
        String source = readAdapterSource();

        assertTrue(source.contains("seedSeasonEpisodes(cached);"));
        assertTrue(source.contains("String key = seasonEpisodeKey(tmdbItem, season);"));
        assertTrue(source.contains("seasonEpisodeCache.putIfAbsent(key, List.copyOf(values))"));
        assertTrue(source.contains("String key = seasonEpisodeKey(item, seasonNumber);"));
    }

    @Test
    public void sourceBundleSchedulesEpisodeBindingAfterSeasonCacheIsReady() throws Exception {
        String source = readAdapterSource();
        String loadSource = method(source, "public void loadSource(TmdbBundle bundle, Vod sourceVod, TmdbSourcePayload payload,");

        int apply = loadSource.indexOf("applySourceBundle(bundle, sourceVod, payload);");
        int seed = loadSource.indexOf("seedSeasonEpisodes(episodeHandoff);", apply);
        int schedule = loadSource.indexOf("scheduleStartupEpisodeLoad(sourceVod, tmdbItem, generation);", seed);

        assertTrue("source bundle is applied before snapshot seeding", apply >= 0 && seed > apply);
        assertTrue("episode metadata binding is scheduled after both season caches are populated", schedule > seed);
    }

    @Test
    public void singleSeasonFallbackOnlyAppliesWithoutConflictingSeasonEvidence() throws Exception {
        String source = readAdapterSource();
        String current = method(source, "private Integer currentEpisodeMetadataSeason()");
        String sole = method(source, "private Integer soleOrdinarySeason()");

        assertTrue(current.contains("TmdbSeasonResolver.Status.RESOLVED"));
        assertTrue(current.contains("return soleOrdinarySeason();"));
        assertTrue(sole.contains("if (isMultiSliceResolution()) return null;"));
        assertTrue(sole.contains("source != TmdbSeasonResolver.Source.REQUEST && source != TmdbSeasonResolver.Source.NONE"));
        assertTrue(sole.contains("option.getSeasonNumber() <= 0"));
        assertTrue(sole.contains("if (sole != null) return null;"));
    }

    private static TmdbEpisode episode(int number, String title, int season) {
        return new TmdbEpisode(number, title, "", "", "", 0d, 0, 123, season);
    }

    private static String readAdapterSource() throws Exception {
        Path root = Path.of("").toAbsolutePath();
        Path adapter = root.resolve(Path.of("app", "src", "main", "java", "com", "fongmi", "android", "tv", "ui", "helper", "TmdbUIAdapter.java"));
        if (!Files.exists(adapter)) {
            adapter = root.resolve(Path.of("src", "main", "java", "com", "fongmi", "android", "tv", "ui", "helper", "TmdbUIAdapter.java"));
        }
        return Files.readString(adapter, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    private static String method(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue("method must exist: " + signature, start >= 0);
        int next = source.indexOf("\n    private ", start + signature.length());
        assertTrue("method end must be found: " + signature, next > start);
        return source.substring(start, next);
    }
}
