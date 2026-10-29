package com.fongmi.android.tv.bean;

import com.fongmi.android.tv.utils.TmdbLanguagePolicy;
import com.github.catvod.utils.Trans;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class C56TmdbLanguageCompatibilityTest {
    @After public void resetLanguage() { Trans.setTraditional(null); }

    @Test public void explicitLegacyLanguagesSurviveInterfaceChangesAndSerialization() {
        for (String language : List.of("zh-CN", "zh-TW", "en-US", "ja-JP")) {
            TmdbConfig config = TmdbConfig.objectFrom("{\"language\":\"" + language + "\"}");
            for (int ui : new int[]{1, 2, 1, 2}) {
                assertEquals(language, TmdbLanguagePolicy.requestLanguage(config, ui));
                assertEquals(language, TmdbLanguagePolicy.requestLanguage(TmdbConfig.effective(config, null), ui));
                assertEquals(language, TmdbLanguagePolicy.requestLanguage(TmdbConfig.objectFrom(config.toJson()), ui));
            }
        }
    }

    @Test public void absentLanguageFollowsChineseInterfaceUntilExplicitlySaved() {
        TmdbConfig config = TmdbConfig.objectFrom("{}");
        assertFalse(config.isLanguageConfigured());
        for (int ui : new int[]{1, 2, 1, 2}) {
            assertEquals(ui == 1 ? "zh-CN" : "zh-TW", TmdbLanguagePolicy.requestLanguage(config, ui));
            assertEquals(ui == 1 ? "zh-CN" : "zh-TW", TmdbLanguagePolicy.requestLanguage(TmdbConfig.effective(config, null), ui));
        }
        config.setLanguage("zh-CN");
        assertEquals("zh-CN", TmdbLanguagePolicy.requestLanguage(config, 2));
    }

    @Test public void oldSimplifiedScopedKeysAreReadableFromTraditionalWithoutChangingStoredData() {
        TmdbMatchCache cache = new TmdbMatchCache();
        cache.getItems().put("site@@@vod@@@庆余年2024", TmdbMatchCache.Entry.from(item(20, "庆余年")));
        assertEquals(20, cache.find("site", "vod", "慶餘年2024").getTmdbId());
        assertNull(cache.find("site", "vod", "慶餘年2019"));
        assertNull(cache.find("other", "vod", "慶餘年2024"));
        assertEquals(1, cache.getItems().size());
        assertTrue(cache.getItems().containsKey("site@@@vod@@@庆余年2024"));
    }

    @Test public void oldManualAliasesSurviveScriptSwitchAndKeepPriority() {
        String json = "{\"items\":{\"site@@@vod\":{\"tmdbId\":42,\"mediaType\":\"tv\",\"title\":\"另一个标题\",\"manual\":true,\"manualTitles\":[\"庆余年\"]}}}";
        TmdbMatchCache cache = new Gson().fromJson(json, TmdbMatchCache.class);
        cache.put("site", "vod", "庆余年", item(99, "庆余年"));
        assertEquals(42, cache.findManual("site", "vod", "慶餘年").getTmdbId());
        assertEquals(42, cache.find("site", "vod", "慶餘年").getTmdbId());
        assertNull(cache.findManual("site", "vod", "赘婿"));
    }

    @Test public void ambiguousOldTitleSpellingsDoNotChooseAnArbitraryMovieOrTvIdentity() {
        TmdbMatchCache cache = new TmdbMatchCache();
        cache.getItems().put("site@@@vod@@@庆余年", TmdbMatchCache.Entry.from(item(20, "庆余年")));
        cache.getItems().put("site@@@vod@@@慶余年", TmdbMatchCache.Entry.from(new TmdbItem(30, "movie", "庆余年", "", "", "", "")));
        assertNull(cache.find("site", "vod", "慶餘年"));
    }

    @Test public void reportedTitlesReadAcrossAllFourScriptAndInterfaceCombinations() {
        String[] titles = {"战斗之魂", "超巡！超条前辈", "归路", "弹珠汽水瓶里的千岁同学"};
        for (String title : titles) {
            String traditional = Trans.s2t(false, title);
            for (boolean ui : new boolean[]{false, true, false}) {
                Trans.setTraditional(ui);
                for (String written : List.of(title, traditional)) {
                    TmdbMatchCache cache = new TmdbMatchCache();
                    cache.put("site", "vod", written, item(42, title));
                    assertEquals(42, cache.find("site", "vod", title).getTmdbId());
                    assertEquals(42, cache.find("site", "vod", traditional).getTmdbId());
                }
            }
        }
    }

    @Test public void conflictingLegacySeasonSpellingsMustNotPickAnArbitrarySeason() {
        TmdbSeasonMatchCache cache = new TmdbSeasonMatchCache();
        cache.getItems().put("site@@@vod@@@庆余年", TmdbSeasonMatchCache.Entry.create(
                100, "tv", 1, TmdbSeasonMatchCache.Mode.MANUAL_SEASON, "fp", 12, 12));
        cache.getItems().put("site@@@vod@@@慶余年", TmdbSeasonMatchCache.Entry.create(
                100, "tv", 2, TmdbSeasonMatchCache.Mode.MANUAL_SEASON, "fp", 12, 12));
        assertNull(cache.find("site", "vod", "慶餘年", 100));
        assertEquals(2, cache.getItems().size());
        cache.put("site", "vod", "慶餘年", 100, "tv", 3,
                TmdbSeasonMatchCache.Mode.MANUAL_SEASON, "fp", 12, 12);
        assertEquals(3, cache.find("site", "vod", "慶餘年", 100).getSeasonNumber().intValue());
        assertEquals(3, cache.getItems().size());
    }

    private static TmdbItem item(int id, String title) { return new TmdbItem(id, "tv", title, "", "", "", ""); }
}
