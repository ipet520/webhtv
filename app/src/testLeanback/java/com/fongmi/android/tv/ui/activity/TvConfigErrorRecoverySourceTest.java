package com.fongmi.android.tv.ui.activity;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class TvConfigErrorRecoverySourceTest {

    @Test
    public void homeRetryRepeatsTheConfigurationLoadThatFailed() throws Exception {
        String source = read("app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java");
        String callback = methodBody(source, "private Callback getCallback()");
        String retry = methodBody(source, "private void showConfigError(String message)");
        assertTrue(callback.contains("showConfigError(msg)"));
        assertTrue(retry.contains("progressLayout.showError"));
        assertTrue(retry.contains("R.string.error_retry"));
        assertTrue(retry.contains("initConfig()"));
        assertTrue(retry.contains("progressLayout.showProgress()"));
    }

    @Test
    public void keepRetryReusesTheSelectedItemAndExistingLoadConfigPath() throws Exception {
        String source = read("app/src/leanback/java/com/fongmi/android/tv/ui/activity/KeepActivity.java");
        String load = methodBody(source, "private void loadConfig(Config config, Keep item)");
        assertTrue(load.contains("mBinding.progressLayout.showError"));
        assertTrue(load.contains("loadConfig(config, item)"));
        assertTrue(load.contains("VodConfig.load(config"));
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) throw new AssertionError("missing method " + signature);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            if (source.charAt(i) == '{') depth++;
            else if (source.charAt(i) == '}' && --depth == 0) return source.substring(start, i + 1);
        }
        throw new AssertionError("unclosed method " + signature);
    }

    private static String read(String relative) throws Exception {
        Path path = Path.of(relative);
        if (!Files.exists(path)) path = Path.of("..").resolve(relative);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}