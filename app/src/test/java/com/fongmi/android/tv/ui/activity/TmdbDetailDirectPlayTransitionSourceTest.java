package com.fongmi.android.tv.ui.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TmdbDetailDirectPlayTransitionSourceTest {

    private static final Path SOURCE = Paths.get("src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java");

    @Test
    public void firstDirectPlayEntersFullscreenBeforeStartingPlayback() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        String playMethod = methodBody(source, "private void playDetailFullscreen()");

        int enterFullscreen = playMethod.indexOf("enterInlineFullscreen();");
        int startPlayback = playMethod.indexOf("if (!current) playInline();");
        assertTrue(enterFullscreen >= 0);
        assertTrue(startPlayback > enterFullscreen);
        assertTrue(playMethod.contains("detailPlayerFullscreenPending = false;"));
        assertFalse(playMethod.contains("detailPlayerFullscreenPending = !current;"));
        assertFalse(playMethod.contains("revealDetailPlayerFullscreen();"));
        assertTrue(source.contains("protected void onFirstFrameRendered()"));
    }

    @Test
    public void closingDirectPlayClearsPendingTransitionState() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        String closeMethod = methodBody(source, "private void closeDetailFullscreenPlayer()");

        assertTrue(closeMethod.contains("detailPlayerFullscreenPending = false;"));
        assertTrue(closeMethod.contains("inlinePlaybackGeneration++;"));
        assertTrue(closeMethod.contains("currentInlineResult = null;"));
    }

    @Test
    public void directPlaybackSnapshotUsesLanguageIdentityAndUnifiedOverviewPolicy() throws Exception {
        String source = Files.readString(
                Paths.get("src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java"), StandardCharsets.UTF_8);

        assertTrue(source.contains("TmdbDetailCache.take(getIntent().getStringExtra(TmdbDetailCache.EXTRA_KEY), getTmdbItem(), currentTmdbLanguage())"));
        assertFalse(source.contains("TmdbDetailCache.take(getIntent().getStringExtra(TmdbDetailCache.EXTRA_KEY), getTmdbItem())"));
        assertTrue(source.contains("translatedOverview(detail, currentTmdbConfig())"));
        assertFalse(source.contains("cachedTmdbOverviewForLanguage(translations, \"zh-CN\")"));
    }

    @Test
    public void cancellingAnInlinePlaybackRequestReleasesItsLoadingFlag() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);

        String switchMethod = methodBody(source, "private boolean refreshAndSwitchInlinePlayer(int playerType)");
        assertFalse("refreshAndSwitchInlinePlayer must exist", switchMethod.isEmpty());
        int switchGeneration = switchMethod.indexOf("++inlinePlaybackGeneration");
        int switchRelease = switchMethod.indexOf("releaseInlinePlaybackPending();");
        int switchSubmit = switchMethod.indexOf("detailTasks.submit(");
        assertTrue("refreshAndSwitchInlinePlayer must release the loading flag it invalidates",
                switchGeneration >= 0 && switchRelease > switchGeneration);
        assertTrue("release must be synchronous and precede the replacement request",
                switchSubmit > switchRelease);

        String cancelMethod = methodBody(source, "private void cancelPendingInlinePlayerSwitch()");
        assertFalse("cancelPendingInlinePlayerSwitch must exist", cancelMethod.isEmpty());
        assertTrue("cancelPendingInlinePlayerSwitch must release the invalidated request",
                cancelMethod.contains("inlinePlaybackGeneration++;")
                        && cancelMethod.contains("releaseInlinePlaybackPending();"));

        String playMethod = methodBody(source,
                "private void playInline(long resumePosition, String failedUrl, String failureMessage)");
        assertFalse("playInline must exist", playMethod.isEmpty());
        int markGeneration = playMethod.indexOf("++inlinePlaybackGeneration");
        assertTrue("playInline must record the generation that owns the pending flag",
                markGeneration >= 0
                        && playMethod.indexOf("inlinePlaybackPendingGeneration = generation;", markGeneration) > markGeneration);
        int firstStaleRelease = playMethod.indexOf("releaseInlinePlaybackPending(generation);");
        assertTrue("both stale success and failure branches must release their owned marker",
                firstStaleRelease >= 0
                        && playMethod.indexOf("releaseInlinePlaybackPending(generation);", firstStaleRelease + 1) > firstStaleRelease);

        String ownedRelease = methodBody(source, "private void releaseInlinePlaybackPending(int generation)");
        assertFalse("the generation-owned release must exist", ownedRelease.isEmpty());
        assertTrue("an old callback must not clear a newer request's marker",
                ownedRelease.contains("inlinePlaybackPendingGeneration != generation"));
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) return "";
        int brace = source.indexOf('{', start);
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return source.substring(start, i + 1);
        }
        return "";
    }
}
