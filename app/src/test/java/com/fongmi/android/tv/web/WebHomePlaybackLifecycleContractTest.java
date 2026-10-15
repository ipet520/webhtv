package com.fongmi.android.tv.web;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WebHomePlaybackLifecycleContractTest {

    @Test
    public void ordinaryWebHomePlaybackIsFreshWhileHistoryExactStaysExplicit() throws Exception {
        String bridge = read("app/src/main/java/com/fongmi/android/tv/web/HomeWebBridge.java");
        String playVod = methodBody(bridge, "private String playVod(JsonObject payload)", "private String playVodInline(JsonObject payload)");

        assertTrue(playVod.contains("VideoActivity.startFromWebHome"));
        assertFalse(playVod.contains("findResumeHistory"));
        assertFalse(playVod.contains("VideoActivity.startDirect"));
        assertTrue(playVod.contains("\"history_exact\".equals(Json.safeString(payload, \"intent\"))"));
        assertTrue(playVod.contains("VideoActivity.startHistoryExact"));
        assertFalse(bridge.contains("findResumeHistory"));
    }

    @Test
    public void mobileAndLeanbackSkipOnlyImplicitHistoryForWebHomeHandoffs() throws Exception {
        for (String flavor : new String[]{"mobile", "leanback"}) {
            String source = read("app/src/" + flavor + "/java/com/fongmi/android/tv/ui/activity/VideoActivity.java");
            String checkHistory = methodBody(source, "private boolean checkHistory(Vod item)", "private boolean isWebHomeFreshHandoff()");
            String tmdbReload = methodBody(source, "private boolean reloadHistoryAfterTmdbMatch(TmdbItem matched)", "private void resumeHistoryAfterTmdbMatch");
            String mismatch = methodBody(source, "private boolean mismatch(Vod item)", "private void nextParse");
            String nextSite = methodBody(source, "private void nextSite()", "private void onPaused()");

            assertTrue(flavor + " uses an explicit fresh WebHome launch marker", source.contains("EXTRA_WEBHOME_FRESH_HANDOFF"));
            assertTrue(flavor + " ordinary handoff skips implicit history matching", checkHistory.contains("? webHomeFreshHandoff ? null : History.findPlayback"));
            assertTrue(flavor + " later TMDB enrichment cannot reapply implicit history", tmdbReload.contains("isWebHomeFreshHandoff()"));
            assertTrue(flavor + " replacement matching uses (siteKey,vodId)", mismatch.contains("PlaybackSourceIdentity.matches"));
            assertTrue(flavor + " broken-source filtering uses the same composite identity", mismatch.contains("PlaybackSourceIdentity.key(itemSiteKey, item.getId())"));
            assertTrue(flavor + " records the failed source identity before changing sources", nextSite.contains("mBroken.add(PlaybackSourceIdentity.key(currentSiteKey, currentVodId))"));
            assertTrue(flavor + " explicit exact history selection remains available", source.contains("WebHomeHistoryService.exactSelection(resumeHistory, item.getFlags())"));
        }
        String leanback = read("app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java");
        String fastTmdbHistory = methodBody(leanback, "private void prepareFastTmdbPlaybackHistory(Vod item, Flag flag, Episode episode)", "private void updateFastTmdbPlaybackHistory");
        assertTrue(fastTmdbHistory.contains("isWebHomeFreshHandoff() ? null : History.findPlayback"));
    }

    @Test
    public void emptyPlaybackWaitsForTheCurrentAutomaticSearchToFinish() throws Exception {
        for (String flavor : new String[]{"mobile", "leanback"}) {
            String source = read("app/src/" + flavor + "/java/com/fongmi/android/tv/ui/activity/VideoActivity.java");
            String empty = methodBody(source, "private void setEmpty(boolean finish)", "private void showEmpty()");
            String progressEnd = flavor.equals("mobile") ? "private boolean isQuickSearchVisible()" : "private void showQuickSearchDialog";
            String progress = methodBody(source, "private void setSearchProgress(SearchProgress progress)", progressEnd);

            assertFalse(flavor + " must not have a fixed ten-second failure timer", source.contains("mR4") || source.contains("App.post(mR4, 10000)"));
            assertTrue(flavor + " starts fallback before declaring the page empty", empty.contains("boolean searchStarted = checkSearch(false);") && empty.contains("if (!searchStarted) showEmpty();"));
            assertTrue(flavor + " observes search completion", progress.contains("progress.finished()"));
            assertTrue(flavor + " only shows empty when automatic fallback found no candidates", progress.contains("isInitAuto()") && progress.contains("mQuickAdapter.getItemCount() > 0"));
            assertTrue(flavor + " observes the shared search lifecycle", source.contains("getSearchProgress().observeForever(mObserveSearchProgress)"));
        }
    }

    private static String methodBody(String source, String startToken, String endToken) {
        int start = source.indexOf(startToken);
        int end = source.indexOf(endToken, start + startToken.length());
        assertTrue("Missing source token: " + startToken, start >= 0);
        assertTrue("Missing source token after " + startToken + ": " + endToken, end > start);
        return source.substring(start, end);
    }

    private static String read(String relative) throws Exception {
        Path path = Path.of(relative);
        if (!Files.exists(path)) path = Path.of("..", relative);
        if (!Files.exists(path)) path = Path.of("app", relative);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
