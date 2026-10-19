package com.fongmi.android.tv.ui.activity;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class HistoryKeepEmptyTransitionSourceTest {

    @Test
    public void individualDeletesReevaluateEmptyStateInBothFlavors() throws Exception {
        for (String path : new String[]{
                "app/src/mobile/java/com/fongmi/android/tv/ui/activity/HistoryActivity.java",
                "app/src/mobile/java/com/fongmi/android/tv/ui/activity/KeepActivity.java",
                "app/src/leanback/java/com/fongmi/android/tv/ui/activity/HistoryActivity.java",
                "app/src/leanback/java/com/fongmi/android/tv/ui/activity/KeepActivity.java"}) {
            String source = read(path);
            int start = source.indexOf("public void onItemDelete(");
            int end = source.indexOf("\n    @Override", start);
            assertTrue(path + " must define item deletion", start >= 0 && end > start);
            assertTrue(path + " must resolve CONTENT/EMPTY after the diff completes",
                    source.substring(start, end).contains("mBinding.progressLayout.showContent(true, mAdapter.getItemCount())"));
        }
    }

    @Test
    public void clearAllReevaluatesEmptyStateAfterAdapterDiff() throws Exception {
        for (String path : new String[]{
                "app/src/mobile/java/com/fongmi/android/tv/ui/activity/HistoryActivity.java",
                "app/src/mobile/java/com/fongmi/android/tv/ui/activity/KeepActivity.java",
                "app/src/leanback/java/com/fongmi/android/tv/ui/activity/HistoryActivity.java"}) {
            String source = read(path);
            int clear = source.indexOf("mAdapter.clear(() -> {");
            assertTrue(path + " must wait for clear diff completion", clear >= 0);
            int show = source.indexOf("mBinding.progressLayout.showContent(true, mAdapter.getItemCount())", clear);
            assertTrue(path + " must reevaluate EMPTY after clear", show > clear);
            assertTrue(path + " must leave delete mode when empty",
                    source.indexOf("if (mAdapter.getItemCount() == 0) mAdapter.setDelete(false);", clear) > show);
        }
    }

    @Test
    public void progressLayoutUsesEmptyStateForZeroRows() throws Exception {
        String source = read("app/src/main/java/com/fongmi/android/tv/ui/custom/ProgressLayout.java");
        assertTrue(source.contains("if (flag && size == 0) showEmpty();"));
        assertTrue(source.contains("else showContent();"));
    }

    private static String read(String relative) throws Exception {
        Path path = Path.of(relative);
        if (!Files.exists(path) && relative.startsWith("app/")) path = Path.of(relative.substring(4));
        return Files.readString(path, StandardCharsets.UTF_8).replace("\r\n", "\n");
    }
}
