package com.fongmi.android.tv.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.Configuration;

import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
public class TvCardBadgeContrastTest {

    @Test
    public void posterBadgesKeepOpaqueWhiteTextContrastInBothThemeModes() {
        int[] colors = {R.color.tv_badge_progress, R.color.tv_badge_site, R.color.tv_badge_year};
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            for (int colorId : colors) {
                int background = ContextCompat.getColor(context, colorId);
                assertEquals("badge backgrounds remain opaque", 0xFF000000, background & 0xFF000000);
                assertTrue("white label contrast must remain readable",
                        ThemeContrast.ratio(0xFFFFFFFF, background) >= 4.5);
            }
        }
    }

    @Test
    public void cardBadgeTextAndExcludedClockSizeRemainUnchanged() throws Exception {
        for (String layout : new String[]{"src/leanback/res/layout/adapter_vod.xml",
                "src/leanback/res/layout/adapter_vod_rect.xml"}) {
            String xml = read(layout);
            assertBadgeBinding(xml, "year", "shape_vod_year");
            assertBadgeBinding(xml, "site", "shape_vod_site");
            assertBadgeBinding(xml, layout.endsWith("adapter_vod.xml") ? "playback" : "remark", "shape_vod_remark");
        }

        String home = read("src/leanback/res/layout/activity_home.xml");
        String clock = elementForId(home, "clock");
        assertTrue("clock redesign is outside this repair", clock.contains("android:textSize=\"24sp\""));
    }

    private static void assertBadgeBinding(String xml, String id, String background) {
        String element = elementForId(xml, id);
        assertTrue(element.contains("android:background=\"@drawable/" + background + "\""));
        assertTrue("preserve existing player-control label color",
                element.contains("android:textColor=\"?attr/webhtvColorPlayerControl\""));
    }

    private static String elementForId(String xml, String id) {
        int start = xml.indexOf("android:id=\"@+id/" + id + "\"");
        assertTrue("missing @" + id, start >= 0);
        int tagStart = xml.lastIndexOf("<", start);
        int end = xml.indexOf("/>", start);
        assertTrue(end > start);
        return xml.substring(tagStart, end + 2);
    }

    private static Context themed(boolean dark) {
        Configuration configuration = new Configuration(
                RuntimeEnvironment.getApplication().getResources().getConfiguration());
        configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        return RuntimeEnvironment.getApplication().createConfigurationContext(configuration);
    }

    private static String read(String relative) throws Exception {
        Path root = Files.exists(Path.of("src")) ? Path.of("") : Path.of("app");
        return Files.readString(root.resolve(relative), StandardCharsets.UTF_8);
    }
}
