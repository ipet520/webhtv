package com.fongmi.android.tv.ui.dialog;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class AppearanceDohFocusContractTest {

    @Test
    public void appearanceRowsUseContainerFocusRolesWithoutFocusToken() throws Exception {
        String source = read("src/leanback/java/com/fongmi/android/tv/ui/dialog/AppearanceDialog.java");
        String background = method(source, "private StateListDrawable dohRowBackground(");
        String text = method(source, "private ColorStateList dohRowTextColors(");

        assertTrue(background.contains("tokens.colorPrimaryContainer(), tokens.colorPrimary()"));
        assertTrue(background.contains("tokens.colorSurfaceContainerHigh(), tokens.colorOutlineVariant()"));
        assertFalse(background.contains("colorFocus"));
        assertTrue(text.contains("tokens.colorOnPrimaryContainer(), tokens.colorOnSurface()"));
        assertFalse(text.contains("colorFocus"));
    }

    @Test
    public void dohItemsUseOnPrimaryRingOnlyOnTheDohPath() throws Exception {
        String source = read("src/main/java/com/fongmi/android/tv/ui/dialog/ChoiceDialog.java");
        String entry = method(source, "public static void showSingleDoh(");
        String item = method(source, "private void styleItem(");

        assertTrue(entry.contains("choice, true"));
        assertTrue(item.contains("focused ? tokens.colorOnPrimary() : on ? tokens.colorOnPrimaryContainer()"));
        assertTrue(item.contains("focused ? tokens.colorPrimary() : on ? tokens.colorPrimaryContainer()"));
        assertTrue(item.contains("useDohCancelColors ? tokens.colorOnPrimary() : tokens.colorFocus()"));
    }

    @Test
    public void outlinedCancelFocusUsesPrimaryContainerAndPrimary() throws Exception {
        String source = read("src/main/java/com/fongmi/android/tv/ui/dialog/ChoiceDialog.java");
        String colors = method(source, "public static void applyDohCancelColors(");

        assertTrue(colors.contains("tokens.colorPrimaryContainer()"));
        assertTrue(colors.contains("tokens.colorPrimary()"));
        assertFalse(colors.contains("colorFocus"));
        assertFalse(colors.contains("ThemeEditorUi.readable"));
    }

    @Test
    public void appearanceCloseAndAboutButtonsKeepTheOutlinedStyleHelper() throws Exception {
        String appearance = read("src/leanback/java/com/fongmi/android/tv/ui/dialog/AppearanceDialog.java");
        String about = read("src/main/java/com/fongmi/android/tv/ui/dialog/AboutDialog.java");
        String aboutStyle = method(about, "private static void applyDohCancelStyle(");

        assertTrue(appearance.contains("ChoiceDialog.applyDohCancelColors(close)"));
        assertTrue(appearance.contains("button.setStrokeWidth(dp(1))"));
        assertTrue(about.contains("applyDohCancelStyle(binding.checkUpdate)"));
        assertTrue(about.contains("applyDohCancelStyle(binding.githubProxy)"));
        assertTrue(about.contains("applyDohCancelStyle(binding.confirm)"));
        assertTrue(aboutStyle.contains("ChoiceDialog.applyDohCancelColors(button)"));
        assertTrue(aboutStyle.contains("button.setStrokeWidth(ResUtil.dp2px(1))"));
    }

    private static String method(String source, String signature) {
        int signatureStart = source.indexOf(signature);
        if (signatureStart < 0) throw new AssertionError("Missing method: " + signature);
        int bodyStart = source.indexOf('{', signatureStart);
        if (bodyStart < 0) throw new AssertionError("Missing method body: " + signature);
        int depth = 1;
        for (int index = bodyStart + 1; index < source.length(); index++) {
            char current = source.charAt(index);
            if (current == '{') depth++;
            else if (current == '}' && --depth == 0) return source.substring(bodyStart + 1, index);
        }
        throw new AssertionError("Unclosed method body: " + signature);
    }

    private static String read(String relative) throws Exception {
        Path root = Files.exists(Path.of("src")) ? Path.of("") : Path.of("app");
        return Files.readString(root.resolve(relative), StandardCharsets.UTF_8);
    }
}
