package com.fongmi.android.tv.ui.activity;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TvTabFocusRingPaletteContractTest {

    @Test
    public void focusedTabUsesThemeRingAndSharedWidthWithoutChangingItsFill() throws Exception {
        String selector = withoutComments(read("src/main/res/drawable/selector_mpv_tab_focus.xml"));
        String focused = stateItem(selector, "state_focused");
        assertTrue(focused.contains("?attr/colorPrimaryContainer"));
        assertTrue(focused.contains("@dimen/webhtv_focus_ring_width"));
        assertTrue(focused.contains("?attr/tvFocusRing"));
        assertFalse(focused.contains("@color/webhtv_color_focus"));
        assertFalse(focused.contains("#"));
    }

    @Test
    public void pressedStateRemainsDistinctFromFocusedState() throws Exception {
        String selector = withoutComments(read("src/main/res/drawable/selector_mpv_tab_focus.xml"));
        String pressed = stateItem(selector, "state_pressed");
        assertTrue(pressed.contains("?attr/colorPrimaryContainer"));
        assertFalse("Pressed must not acquire the Focused outline", pressed.contains("<stroke"));
        assertTrue("normal state remains transparent", selector.contains("@android:color/transparent"));
    }

    @Test
    public void allTabHostsKeepDynamicDialogShellAndUpdateSettingsBindsTheActiveTheme() throws Exception {
        assertTrue(read("src/main/res/layout/dialog_mpv_config_manager.xml")
                .contains("app:tabBackground=\"@drawable/selector_mpv_tab_focus\""));
        assertTrue(read("src/main/java/com/fongmi/android/tv/ui/dialog/PlaybackPerformanceDialog.java")
                .contains("ThemeController.bindDialog(dialog)"));
        assertTrue(read("src/leanback/java/com/fongmi/android/tv/ui/dialog/AdBlockStatsDialog.java")
                .contains("new WebHtvAlertDialogBuilder"));

        String update = read("src/main/java/com/fongmi/android/tv/ui/dialog/UpdateSettingsDialog.java");
        int shown = update.indexOf("dialog.show();");
        int bound = update.indexOf("ThemeController.bindDialog(dialog)");
        assertTrue(shown >= 0 && bound > shown);
        String shell = read("src/main/res/drawable/shape_shell_proxy_dialog.xml");
        assertTrue(shell.contains("?attr/colorSurfaceContainerHigh"));
    }

    private static String stateItem(String selector, String state) {
        int start = selector.indexOf("<item android:" + state + "=\"true\">");
        assertTrue("missing " + state + " item", start >= 0);
        int end = selector.indexOf("</item>", start);
        assertTrue(end > start);
        return selector.substring(start, end + "</item>".length());
    }

    private static String withoutComments(String xml) {
        return xml.replaceAll("(?s)<!--.*?-->", " ");
    }

    private static String read(String moduleRelative) throws Exception {
        Path path = Path.of(moduleRelative);
        if (!Files.exists(path)) path = Path.of("app").resolve(moduleRelative);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
