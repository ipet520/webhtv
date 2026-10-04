package com.fongmi.android.tv.theme;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class MobileThemePhase1Test {

    @Test
    public void appearanceRowsUseIndependentThemeAwareSurfaceAndForeground() throws Exception {
        String source = read("src/mobile/java/com/fongmi/android/tv/ui/dialog/AppearanceDialog.java");
        assertTrue(source.contains("ThemeEditorUi.interactive"));
        assertTrue(source.contains("ThemeEditorUi.readable"));
        assertFalse(source.contains("selector_git_cloud_card"));
    }

    @Test
    public void sharedHistorySelectorWasNotChangedForTheMobileThemeFix() throws Exception {
        String source = read("src/leanback/res/color/config_history_text.xml");
        assertTrue(source.contains("?attr/colorPrimary"));
        assertFalse(source.contains("?attr/colorOnPrimary"));
    }

    private static Path root() {
        Path path = Path.of("").toAbsolutePath();
        while (path != null) {
            if (Files.exists(path.resolve("app/src"))) return path.resolve("app");
            if (Files.exists(path.resolve("src"))) return path;
            path = path.getParent();
        }
        throw new IllegalStateException("app module root not found");
    }

    private static String read(String relative) throws Exception {
        Path path = root().resolve(relative);
        if (Files.isDirectory(path)) return "";
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
