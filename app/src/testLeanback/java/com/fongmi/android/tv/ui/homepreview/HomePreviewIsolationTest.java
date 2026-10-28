package com.fongmi.android.tv.ui.homepreview;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.Assert.*;

/** Source boundaries complement executable view tests; no claims of device/network tracing. */
public class HomePreviewIsolationTest {
    private static Path sources() {
        Path path = Path.of("src");
        return Files.isDirectory(path) ? path : Path.of("app/src");
    }

    @Test public void launcherAndWebHomeKeepTheirExistingRouteAndNoPreviewIsDeclaredForReleaseOrMobile() throws Exception {
        Path src = sources();
        String home = Files.readString(src.resolve("leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java"));
        assertFalse(home.contains("new NativeHomeBrowseView"));
        assertFalse(home.contains("NativeHomePreviewActivity"));
        assertTrue(home.contains("NativeHomeModeHost"));
        assertTrue(home.contains("mWeb.load(getHome())"));
        assertTrue(home.contains("mViewModel.homeContent()"));
        for (String manifest : List.of("main/AndroidManifest.xml", "leanback/AndroidManifest.xml", "mobile/AndroidManifest.xml", "debug/AndroidManifest.xml")) {
            assertFalse(Files.readString(src.resolve(manifest)).contains("NativeHomePreviewActivity"));
        }
        for (String variant : List.of("leanbackArm64_v8aDebug", "leanbackArmeabi_v7aDebug")) {
            String manifest = Files.readString(src.resolve(variant + "/AndroidManifest.xml"));
            assertTrue(manifest.contains("NativeHomePreviewActivity"));
            assertFalse(manifest.contains("intent-filter"));
        }
        String entry = Files.readString(src.resolve("leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomePreviewActivity.java"));
        assertTrue(entry.contains("!BuildConfig.DEBUG || !getIntent().getBooleanExtra(EXTRA_ENABLED, false)"));
        assertTrue(entry.contains("if (isTaskRoot()) startActivity"));
    }

    @Test public void presentationHasNoNetworkBusinessLoaderOrCustomPixelScrollDependency() throws Exception {
        Path code = sources().resolve("leanback/java/com/fongmi/android/tv/ui/homepreview");
        try (var paths = Files.list(code)) {
            for (Path path : paths.filter(p -> p.toString().endsWith(".java")).toList()) {
                String text = Files.readString(path);
                for (String forbidden : List.of("import okhttp", "import java.net", "import com.bumptech.glide", "import com.fongmi.android.tv.api.",
                        "import com.fongmi.android.tv.model.", "import com.fongmi.android.tv.service.", "import com.fongmi.android.tv.web.",
                        "import com.fongmi.android.tv.setting.", ".scrollBy(", ".smoothScrollBy(", "new SmoothScroller", "Choreographer")) {
                    assertFalse(path + " contains " + forbidden, text.contains(forbidden));
                }
            }
        }
    }
}
