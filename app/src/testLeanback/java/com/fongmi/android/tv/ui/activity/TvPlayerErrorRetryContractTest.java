package com.fongmi.android.tv.ui.activity;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class TvPlayerErrorRetryContractTest {

    @Test
    public void errorLayerExposesFocusablePlayerPanelRetry() throws Exception {
        String layout = read("app/src/leanback/res/layout/view_widget_vod.xml");
        String retry = element(layout, "retry");
        assertTrue(retry.contains("android:focusable=\"true\""));
        assertTrue(retry.contains("android:clickable=\"true\""));
        assertTrue(retry.contains("@drawable/selector_control_sheet_button"));
        assertTrue(retry.contains("@string/error_retry"));
    }

    @Test
    public void retryUsesExistingResetAndPreservesThePriorDpadFocusTarget() throws Exception {
        String source = read("app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java");
        assertTrue(source.contains("mBinding.widget.retry.setOnClickListener(guarded(this::onReset))"));

        String show = methodBody(source, "private void showError(String text)");
        assertTrue(show.contains("getCurrentFocus()"));
        assertTrue(show.contains("mBinding.widget.retry.requestFocus()"));

        String hide = methodBody(source, "private void hideError()");
        assertTrue(hide.contains("boolean retryHadFocus = mBinding.widget.retry.hasFocus()"));
        assertTrue(hide.contains("mErrorFocusReturn"));
        assertTrue(hide.contains("target.requestFocus()"));
        assertTrue(hide.contains("mBinding.video.requestFocus()"));

        String reset = methodBody(source, "private void onReset()");
        assertTrue(reset.contains("isReplay()"));
        assertTrue(reset.contains("onReplay()"));
        assertTrue(reset.contains("onRefresh()"));
    }

    private static String element(String xml, String id) {
        int idAt = xml.indexOf("@+id/" + id + "\"");
        assertTrue("missing @+id/" + id, idAt > 0);
        int start = xml.lastIndexOf("<", idAt);
        int end = xml.indexOf(">", idAt);
        assertTrue(end > start);
        return xml.substring(start, end + 1);
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue("missing " + signature, start >= 0);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return source.substring(start, i + 1);
        }
        throw new AssertionError("unterminated method " + signature);
    }

    private static String read(String path) throws Exception {
        Path direct = Path.of(path);
        if (!Files.exists(direct)) direct = Path.of("..", path);
        return Files.readString(direct, StandardCharsets.UTF_8);
    }
}
