package com.fongmi.android.tv.ui.activity;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertTrue;

public class MobilePlayerErrorRetryContractTest {

    @Test
    public void errorLayerSeparatesReasonFromRetryControl() throws Exception {
        String layout = read("app/src/mobile/res/layout/view_widget_vod.xml");
        String error = element(layout, "error");
        assertTrue(error.startsWith("<androidx.appcompat.widget.LinearLayoutCompat"));
        String reason = element(layout, "errorText");
        String retry = element(layout, "errorRetry");
        assertTrue(reason.contains("android:textColor=\"@color/white\""));
        assertTrue(reason.contains("tools:text=\"@string/error_play_url\""));
        assertTrue(retry.contains("@style/Widget.WebHTV.Button.Tonal"));
        assertTrue(retry.contains("@string/error_retry"));
    }

    @Test
    public void retryReusesTheExistingResetPathAndErrorTextUsesTheNewBinding() throws Exception {
        String source = read("app/src/mobile/java/com/fongmi/android/tv/ui/activity/VideoActivity.java");
        assertTrue(source.contains("mBinding.widget.errorRetry.setOnClickListener(guarded(this::onReset))"));
        assertTrue(source.contains("mBinding.widget.errorText.setText(text)"));
        assertTrue(source.contains("mBinding.widget.errorText.setText(\"\")"));
        String reset = methodBody(source, "private void onReset()");
        assertTrue(reset.contains("if (isReplay()) onReplay();"));
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
