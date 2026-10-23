package com.fongmi.android.tv.ui.activity;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class HomeActivityDeferredFragmentWorkTest {

    @Test
    public void fragmentTransactionsAreGuardedForHomeAndSiteCleanup() throws Exception {
        String home = readHome();
        String showHome = section(home, "private void showHomeContent()", "private void showCategoryContent(Class item)");
        String cleanup = section(home, "private void clearCategoryContent()", "private void clearStaleSiteTypes()");
        String transactionGuard = section(home, "private boolean canCommitFragmentTransaction()", "private void clearCategoryContent()");

        assertTrue(showHome.contains("if (canCommitFragmentTransaction()) getSupportFragmentManager().beginTransaction().hide(mFolder).commit();"));
        assertTrue(cleanup.contains("if (!canCommitFragmentTransaction())"));
        assertTrue(cleanup.contains("if (canDeferFragmentWork()) mPendingCategoryCleanup = true;"));
        assertTrue(cleanup.contains("removeCategoryFragments();"));
        assertTrue(transactionGuard.contains("!isFinishing() && !isDestroyed() && !getSupportFragmentManager().isStateSaved()"));
        assertFalse(home.contains("commitAllowingStateLoss"));
    }

    @Test
    public void savedRefreshIsCoalescedAndKeepsForceNativeRequest() throws Exception {
        String home = readHome();
        String getVideo = section(home, "private void getVideo(boolean forceNative)", "private void showWebOverlay()");

        assertTrue(getVideo.contains("if (!canCommitFragmentTransaction())"));
        assertTrue(getVideo.contains("if (canDeferFragmentWork())"));
        assertTrue(getVideo.contains("mPendingContentRefresh = true;"));
        assertTrue(getVideo.contains("mPendingContentRefreshForceNative |= forceNative;"));
        assertTrue(getVideo.contains("return;"));
    }

    @Test
    public void deferredWorkReplaysAfterResumeAndClearsFlagsBeforeCallbacks() throws Exception {
        String home = readHome();
        String onResume = section(home, "protected void onResume()", "protected void onPause()");
        String replay = section(home, "private void resumeDeferredFragmentWork()", "protected void onPause()");

        assertTrue(onResume.contains("resumeDeferredFragmentWork();"));
        assertTrue(replay.contains("if (!canCommitFragmentTransaction()) return;"));
        assertTrue(replay.indexOf("mPendingCategoryCleanup = false;") < replay.indexOf("removeCategoryFragments();"));
        assertTrue(replay.indexOf("mPendingContentRefresh = false;") < replay.indexOf("getVideo(forceNative);"));
        assertTrue(replay.contains("mPendingContentRefreshForceNative = false;"));
    }

    private static String section(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from + start.length());
        if (from < 0 || to <= from) throw new AssertionError("Missing section: " + start);
        return source.substring(from, to);
    }

    private static String readHome() throws Exception {
        Path path = Path.of("app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java");
        if (!Files.exists(path)) path = Path.of("src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java");
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
