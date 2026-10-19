package com.fongmi.android.tv.ui.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class NoConfigStateContractTest {

    private static String read(String path) throws Exception {
        Path direct = Path.of(path);
        if (Files.exists(direct)) return Files.readString(direct, StandardCharsets.UTF_8).replace("\r\n", "\n");
        return Files.readString(Path.of(path.substring("app/".length())), StandardCharsets.UTF_8).replace("\r\n", "\n");
    }

    @Test
    public void emptyConfigurationUsesItsOwnTerminalCallbackAndDefaultCallbacksStillFinish() throws Exception {
        String config = read("app/src/main/java/com/fongmi/android/tv/api/config/BaseConfig.java");
        String callback = read("app/src/main/java/com/fongmi/android/tv/impl/Callback.java");
        assertTrue(config.contains("TextUtils.isEmpty(config.getUrl())) App.post(callback::noConfig)"));
        assertFalse(config.contains("TextUtils.isEmpty(config.getUrl())) App.post(() -> callback.error(\"\"))"));
        assertTrue(callback.contains("public void noConfig() {\n        error(\"\");\n    }"));
    }

    @Test
    public void mobileNoConfigHasAnActionAndOnlyDismissesWhenSourcesExist() throws Exception {
        String home = read("app/src/mobile/java/com/fongmi/android/tv/ui/activity/HomeActivity.java");
        String fragment = read("app/src/mobile/java/com/fongmi/android/tv/ui/fragment/VodFragment.java");
        assertTrue(home.contains("public void noConfig()") && home.contains("StateEvent.noConfig();"));
        assertTrue(fragment.contains("case NO_CONFIG:") && fragment.contains("showNoConfig();"));
        assertTrue(fragment.contains("HistoryDialog.create().vod().manage().show(this)"));
        assertTrue(fragment.contains("mBinding.empty.getRoot().getVisibility() != View.VISIBLE"));
        assertTrue(fragment.contains("VodConfig.get().getSites().isEmpty()"));
        assertTrue(fragment.contains("dismissNoConfigIfConfigured();"));
    }

    @Test
    public void leanbackNoConfigUsesAnActionableEmptyStateAndRefreshGuard() throws Exception {
        String home = read("app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java");
        String layout = read("app/src/main/java/com/fongmi/android/tv/ui/custom/ProgressLayout.java");
        assertTrue(home.contains("public void noConfig()") && home.contains("showNoConfig();"));
        assertTrue(home.contains("showEmptyContent("));
        assertTrue(home.contains("HistoryDialog.create().vod().manage().show(this)"));
        assertTrue(home.contains("if (!noConfigVisible || VodConfig.get().getSites().isEmpty()) return;"));
        assertTrue(home.contains("dismissNoConfigIfConfigured();"));
        assertTrue(layout.contains("public void showEmptyContent(int title, int description, int action, OnClickListener listener)"));
        assertTrue(layout.contains("resetEmptyContent();"));
        assertTrue(layout.contains("mEmptyBinding.text.setText(com.fongmi.android.tv.R.string.error_empty)"));
    }

    @Test
    public void stateEventKeepsNoConfigSeparateFromFailureAndEmptyResults() throws Exception {
        String event = read("app/src/main/java/com/fongmi/android/tv/event/StateEvent.java");
        assertTrue(event.contains("new StateEvent(Type.NO_CONFIG)"));
        assertTrue(event.contains("EMPTY, PROGRESS, CONTENT, NO_CONFIG"));
        assertFalse(event.contains("message"));
    }

    @Test
    public void tvActionRetainsDistinctPressedAndFocusedThemeStates() throws Exception {
        String empty = read("app/src/leanback/res/layout/view_empty.xml");
        String colors = read("app/src/main/res/color/following_button_primary_bg.xml");
        assertTrue(empty.contains("android:focusable=\"true\""));
        assertTrue(empty.contains("android:focusableInTouchMode=\"false\""));
        assertTrue(empty.contains("@color/following_button_primary_bg"));
        assertTrue(colors.contains("android:state_pressed=\"true\""));
        assertTrue(colors.contains("android:state_focused=\"true\""));
        assertFalse(colors.contains("state_selected") || colors.contains("state_activated"));
    }
}
