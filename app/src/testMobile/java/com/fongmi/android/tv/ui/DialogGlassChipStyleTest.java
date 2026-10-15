package com.fongmi.android.tv.ui;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class DialogGlassChipStyleTest {

    @Test
    public void trackAndTitleSelectionsShareTheSemanticFocusStyle() throws Exception {
        String selector = readMainResource("drawable", "selector_dialog_glass_chip.xml");
        String active = readMainResource("drawable", "shape_dialog_glass_chip_active.xml");
        String text = readMainResource("color", "selector_dialog_glass_chip_text.xml");
        String track = readLeanbackResource("layout", "adapter_track.xml");
        String title = readLeanbackResource("layout", "adapter_title.xml");

        assertEquals("focused/checked/activated/selected chip states must share one active drawable",
                5, occurrences(selector, "@drawable/shape_dialog_glass_chip_active"));
        assertTrue("active chips must use the theme primary fill",
                active.contains("android:color=\"?attr/colorPrimary\""));
        assertTrue("active chips must use the matching onPrimary border",
                active.contains("android:width=\"2dp\"")
                        && active.contains("android:color=\"?attr/colorOnPrimary\""));
        assertTrue("focused chips must have an independent active state",
                selector.contains("android:state_focused=\"true\""));
        assertEquals("pressed, checked, activated, selected and focused text must use onPrimary",
                5, occurrences(text, "android:color=\"?attr/colorOnPrimary\""));
        assertTrue("subtitle rows must use the shared chip background and text selectors",
                usesSharedSelectors(track));
        assertTrue("title rows must use the shared chip background and text selectors",
                usesSharedSelectors(title));
    }

    @Test
    public void trackAdapterUsesActivatedStateForPersistentSelection() throws Exception {
        String adapter = read(findAppPath().resolve(Path.of("src", "main", "java", "com", "fongmi", "android", "tv", "ui", "adapter", "TrackAdapter.java")));

        assertTrue("track selection must use activated state so focus cannot erase the selected text color",
                adapter.contains("holder.binding.text.setActivated(item.isSelected());"));
        assertTrue("selected state is reserved for transient focus and must not represent the playing track",
                !adapter.contains("holder.binding.text.setSelected(item.isSelected());"));
    }

    @Test
    public void mobileTrackStyleSupportsPersistentActivatedState() throws Exception {
        String text = readMobileResource("color", "selector_control_sheet_text.xml");
        String background = readMobileResource("drawable", "selector_player_child_sheet_button.xml");

        assertTrue("mobile selected track text must react to the adapter activated state",
                text.contains("android:state_activated=\"true\""));
        assertTrue("mobile selected track background must react to the adapter activated state",
                background.contains("android:state_activated=\"true\""));
    }

    @Test
    public void mobileCategoryAndFilterDialogsUseTheGenericThemeSheetStyle() throws Exception {
        String category = readMobileResource("layout", "adapter_type_dialog.xml");
        String filter = readMobileResource("layout", "adapter_value.xml");

        assertTrue("category rows must use the generic theme-sheet selectors",
                usesThemeSheetSelectors(category));
        assertTrue("filter rows must use the same generic theme-sheet selectors",
                usesThemeSheetSelectors(filter));
        assertTrue("filter rows must preserve a stable 34dp touch target",
                filter.contains("android:layout_height=\"34dp\""));
    }

    private static boolean usesSharedSelectors(String layout) {
        return layout.contains("android:background=\"@drawable/selector_dialog_glass_chip\"")
                && layout.contains("android:textColor=\"@color/selector_dialog_glass_chip_text\"");
    }

    private static boolean usesThemeSheetSelectors(String layout) {
        return layout.contains("android:background=\"@drawable/selector_theme_sheet_button\"")
                && layout.contains("android:textColor=\"@color/selector_theme_sheet_text\"");
    }

    private static int occurrences(String source, String value) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(value, index)) >= 0) {
            count++;
            index += value.length();
        }
        return count;
    }

    private static String readMainResource(String type, String file) throws Exception {
        return read(findAppPath().resolve(Path.of("src", "main", "res", type, file)));
    }

    private static String readLeanbackResource(String type, String file) throws Exception {
        return read(findAppPath().resolve(Path.of("src", "leanback", "res", type, file)));
    }

    private static String readMobileResource(String type, String file) throws Exception {
        return read(findAppPath().resolve(Path.of("src", "mobile", "res", type, file)));
    }

    private static Path findAppPath() {
        return Files.exists(Path.of("src", "main")) ? Path.of(".") : Path.of("app");
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
