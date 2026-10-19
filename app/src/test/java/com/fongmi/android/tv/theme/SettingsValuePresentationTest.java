package com.fongmi.android.tv.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

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
public class SettingsValuePresentationTest {

    @Test
    public void errorUsesTheActiveSemanticPairAndSuccessRestoresWallpaperText() {
        Context context = RuntimeEnvironment.getApplication();
        TextView value = new TextView(context);

        for (ThemeTokens tokens : new ThemeTokens[]{ThemeTokens.light(), ThemeTokens.dark()}) {
            SettingsValuePresentation.error(value, tokens);

            assertEquals(tokens.colorOnErrorContainer(), value.getCurrentTextColor());
            assertTrue(value.getBackground() instanceof GradientDrawable);
            GradientDrawable background = (GradientDrawable) value.getBackground();
            assertEquals(tokens.colorErrorContainer(), background.getColor().getDefaultColor());
            assertTrue(ThemeContrast.ratio(tokens.colorOnErrorContainer(), tokens.colorErrorContainer()) >= 4.5);

            SettingsValuePresentation.summary(value, "Ready");
            assertEquals("Ready", value.getText().toString());
            assertEquals(ContextCompat.getColor(context, R.color.webhtv_on_wallpaper), value.getCurrentTextColor());
            assertNull(value.getBackground());
        }
    }

    @Test
    public void allFourExistingSettingSurfacesUseTheSharedTemporaryErrorStyle() throws Exception {
        String[] paths = {
                "leanback/java/com/fongmi/android/tv/ui/activity/SettingAdActivity.java",
                "leanback/java/com/fongmi/android/tv/ui/activity/SettingEnhanceActivity.java",
                "mobile/java/com/fongmi/android/tv/ui/fragment/SettingAdFragment.java",
                "mobile/java/com/fongmi/android/tv/ui/fragment/SettingEnhanceFragment.java"
        };
        for (String relative : paths) {
            String source = read("src/" + relative);
            assertTrue(relative + " must use the current theme error roles",
                    source.contains("SettingsValuePresentation.error(view, ThemeController.current())"));
            assertTrue(relative + " must reset to normal summary presentation after success",
                    source.contains("SettingsValuePresentation.summary(view, text)"));
            assertFalse(relative + " must not retain raw summary writes on the recovery path",
                    source.contains("safeRun(name, () -> view.setText(supplier.get())"));
        }
    }

    private static String read(String moduleRelative) throws Exception {
        Path path = Path.of(moduleRelative);
        if (!Files.exists(path)) path = Path.of("app").resolve(moduleRelative);
        return Files.readString(path, StandardCharsets.UTF_8);
    }
}
