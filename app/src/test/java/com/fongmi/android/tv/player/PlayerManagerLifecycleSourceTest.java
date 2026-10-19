package com.fongmi.android.tv.player;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * 本类断言的是 {@code PlayerManager} 的**实现文本**（仓库既有的 source-text 约定），用于锁定
 * 那些无法从行为上便宜地观测、却容易被顺手改掉的不变量。
 *
 * <p>若日后搬走、改名或重排这些方法，断言会变红 —— 那说明约定被破坏需要同步更新断言，
 * **不是**测试坏了。确认行为未变后请重新对齐断言，而不要删除用例。
 */
public class PlayerManagerLifecycleSourceTest {

    @Test
    public void releasedEngineClassificationIsNullSafeForLateCallbacks() throws Exception {
        String source = readPlayerManager();

        assertTrue("isLive must tolerate a late callback after engine release",
                source.contains("public boolean isLive() {\n        return engine != null && engine.isLive();"));
        assertTrue("isVod must tolerate a late callback after engine release",
                source.contains("public boolean isVod() {\n        return engine != null && engine.isVod();"));
    }

    /**
     * Slices one method body. Relies on the body containing no closing brace at four-space
     * indentation; every method asserted here satisfies that today. If a nested block ever
     * breaks it, the slice truncates early and the assertion fails spuriously — re-align the
     * helper rather than deleting the assertion.
     */
    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue("method must exist: " + signature, start >= 0);
        int end = source.indexOf("\n    }", start);
        assertTrue("method body must be delimited: " + signature, end > start);
        return source.substring(start, end);
    }

    @Test
    public void seekInducedBufferingIsExcludedFromTheRebufferCount() throws Exception {
        String source = readPlayerManager();
        String body = methodBody(source, "private void recordBufferingState(");
        // MpvPlayer now publishes BUFFERING across a seek, which is what lets the UI keep its
        // progress indicator up instead of freezing on the old frame. That state reaches
        // PlaybackBufferingTracker, which counts every post-startup BUFFERING as a rebuffer,
        // and the rebuffer count feeds the network guard and the HLS variant policy. Without
        // this exclusion, scrubbing alone would look like collapsing throughput.
        int exclusion = body.indexOf("isMpvSeekBuffering()");
        int tracker = body.indexOf("playbackBufferingTracker.update(");
        assertTrue("recordBufferingState must recognise seek-induced buffering", exclusion >= 0);
        assertTrue("the seek exclusion must return before the rebuffer tracker is updated",
                tracker > exclusion);
        assertTrue("the exclusion must be traceable", body.contains("result=excluded-from-rebuffer"));
        // Only the entering edge may be dropped. Swallowing the leaving edge as well would
        // leave the tracker latched as buffering for the rest of the session.
        assertTrue("the exclusion must only apply to the entering edge",
                body.contains("!playbackBufferingTracker.isBuffering()"));
    }

    @Test
    public void bufferingStateArmsSharedStallWatchdogForEveryEngine() throws Exception {
        String source = readPlayerManager().replace("\r\n", "\n");
        String stateListener = methodBody(source, "public void onPlaybackStateChanged(int state)");
        String arm = methodBody(source, "private void armBufferingStallWatchdog()");
        String polling = methodBody(source, "private void checkBufferingStall()");
        String timeout = methodBody(source, "private void onBufferingStall(");

        assertTrue("every BUFFERING transition must arm the shared watchdog",
                stateListener.contains("} else if (state == Player.STATE_BUFFERING) {\n                armBufferingStallWatchdog();"));
        assertFalse("the shared arming method must not exclude MPV", arm.contains("isExo()"));
        assertFalse("the shared polling method must not exclude MPV", polling.contains("isExo()"));
        assertTrue("seeking must re-arm the shared watchdog after the discontinuity",
                source.contains("player.seekTo(time);\n        cancelBufferingStallWatchdog();\n        armBufferingStallWatchdog();"));
        assertTrue("stall handling must retain the existing automatic source fallback",
                timeout.contains("if (fallbackPlayback(e)) return;"));
        int observe = polling.indexOf("bufferingStallWatchdog.observe(");
        int decision = polling.indexOf("bufferingStallWatchdog.shouldTimeout(");
        assertTrue("a discontinuity must be observed before timeout is evaluated",
                observe >= 0 && decision > observe);
    }

    @Test
    public void engineReplacementDisarmsSharedStallWatchdogBeforeReleasingOldPlayer() throws Exception {
        String source = readPlayerManager().replace("\r\n", "\n");
        assertCancelledBefore(source, "public void switchDecode(PlaySpec freshSpec,",
                "engine.release();");
        assertCancelledBefore(source, "public void switchDecode(Result result,",
                "engine.release();");
        assertCancelledBefore(source, "public void preparePlayer(int type, boolean force)",
                "engine.release();");
        assertCancelledBefore(source, "public void switchPlayer(int type, PlaySpec freshSpec,",
                "engine.release();");
        assertCancelledBefore(source, "public void switchPlayer(int type, Result result,",
                "engine.release();");
        assertCancelledBefore(source,
                "private void switchEngine(int type, boolean chosen, boolean notifyPrepare, int decode,",
                "engine.release();");
        assertCancelledBefore(source, "private void rebuildPlayer(boolean resetVideoSurface)",
                "engine.rebuild(listener)");
        assertCancelledBefore(source, "private boolean switchIjkRuntimeFallback(",
                "engine.release();");
        assertCancelledBefore(source, "private void prepareIjkRuntimeForUserPlayback(",
                "if (engine != null) engine.release();");
        assertCancelledBefore(source, "private boolean retryHardDecodeSwitch(",
                "engine.release();");
    }

    private static void assertCancelledBefore(String source, String signature, String replacement) {
        String body = methodBody(source, signature);
        int cancel = body.indexOf("cancelBufferingStallWatchdog();");
        int replace = body.indexOf(replacement);
        assertTrue(signature + " must disarm the old playback episode before replacing its player",
                cancel >= 0 && replace > cancel);
    }

    private static String readPlayerManager() throws IOException {
        Path root = Path.of("").toAbsolutePath();
        Path source = root.resolve(Path.of(
                "app", "src", "main", "java", "com", "fongmi", "android", "tv", "player", "PlayerManager.java"));
        if (!Files.exists(source)) {
            source = root.resolve(Path.of(
                    "src", "main", "java", "com", "fongmi", "android", "tv", "player", "PlayerManager.java"));
        }
        return Files.readString(source, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
