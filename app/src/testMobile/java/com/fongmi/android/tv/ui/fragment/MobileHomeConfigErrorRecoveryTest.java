package com.fongmi.android.tv.ui.fragment;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.fongmi.android.tv.event.StateEvent;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MobileHomeConfigErrorRecoveryTest {

    @Test
    public void errorEventCarriesTheFailureReasonWithoutChangingOtherEventFactories() {
        StateEvent error = new StateEvent(StateEvent.Type.ERROR, "offline");
        assertEquals(StateEvent.Type.ERROR, error.type());
        assertEquals("offline", error.message());
        assertEquals(StateEvent.Type.EMPTY, new StateEvent(StateEvent.Type.EMPTY).type());
        assertEquals(null, new StateEvent(StateEvent.Type.PROGRESS).message());
    }

    @Test
    public void mobileConfigFailureUsesErrorAndRetryReusesInitConfig() throws Exception {
        String home = read("app/src/mobile/java/com/fongmi/android/tv/ui/activity/HomeActivity.java");
        String callback = methodBody(home, "private Callback getCallback()");
        String retry = methodBody(home, "public void retryConfig()");
        assertTrue(callback.contains("StateEvent.error(msg)"));
        assertTrue(retry.contains("StateEvent.progress()"));
        assertTrue(retry.contains("initConfig()"));
    }

    @Test
    public void vodFragmentRendersTheReasonAndReturnsRetryToTheActivity() throws Exception {
        String fragment = read("app/src/mobile/java/com/fongmi/android/tv/ui/fragment/VodFragment.java");
        String handler = methodBody(fragment, "public void onStateEvent(StateEvent event)");
        String error = methodBody(fragment, "private void showError(String message)");
        assertTrue(handler.contains("case ERROR"));
        assertTrue(handler.contains("showError(event.message())"));
        assertTrue(error.contains("R.string.error_config_load"));
        assertTrue(error.contains("R.string.error_retry"));
        assertTrue(error.contains("retryConfig()"));
        assertTrue(error.contains("instanceof ConfigRetry"));
    }

    @Test
    public void errorOverlayUsesTheExistingProgressLayoutInstance() throws Exception {
        String layout = read("app/src/mobile/res/layout/fragment_vod.xml");
        assertTrue(layout.contains("android:id=\"@+id/state\""));
        assertTrue(layout.contains("com.fongmi.android.tv.ui.custom.ProgressLayout"));
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