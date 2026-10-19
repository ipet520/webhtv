package com.fongmi.android.tv.ui.custom;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ProgressIndicatorThemeContractTest {

    @Test
    public void bothFlavorsUseWallpaperForegroundForTheirProgressIndicator() throws Exception {
        for (String flavor : new String[]{"leanback", "mobile"}) {
            Path layout = Path.of("src", flavor, "res", "layout", "view_progress.xml");
            if (!Files.exists(layout)) layout = Path.of("app").resolve(layout);
            String source = Files.readString(layout, StandardCharsets.UTF_8);
            assertTrue(flavor + " spinner must use the foreground role paired with wallpaper text",
                    source.contains("app:indicatorColor=\"?attr/webhtvColorOnWallpaper\""));
            assertFalse(flavor + " spinner must not use the primary accent against arbitrary wallpaper",
                    source.contains("app:indicatorColor=\"?attr/colorPrimary\""));
        }
    }
}