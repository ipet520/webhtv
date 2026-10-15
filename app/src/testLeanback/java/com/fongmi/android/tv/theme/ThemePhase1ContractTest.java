package com.fongmi.android.tv.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
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
public class ThemePhase1ContractTest {

    @Test
    public void explicitAndSystemThemeModesResolveCorrectly() {
        assertFalse(ThemeController.darkPaletteFor(true, ThemeMode.LIGHT, false));
        assertFalse(ThemeController.darkPaletteFor(true, ThemeMode.LIGHT, true));
        assertTrue(ThemeController.darkPaletteFor(false, ThemeMode.DARK, false));
        assertTrue(ThemeController.darkPaletteFor(false, ThemeMode.DARK, true));
        assertFalse(ThemeController.darkPaletteFor(false, ThemeMode.SYSTEM, false));
        assertTrue(ThemeController.darkPaletteFor(false, ThemeMode.SYSTEM, true));
    }

    @Test
    public void lightAndDarkPalettesKeepDistinctReadableSemanticPairs() {
        ThemeTokens light = ThemeTokens.light();
        ThemeTokens dark = ThemeTokens.dark();
        assertNotEquals(light.colorPrimary(), dark.colorPrimary());
        assertNotEquals(light.colorSurface(), dark.colorSurface());
        for (ThemeTokens tokens : new ThemeTokens[]{light, dark}) {
            assertTrue(ThemeContrast.ratio(tokens.colorOnPrimary(), tokens.colorPrimary()) >= 4.5);
            assertTrue(ThemeContrast.ratio(tokens.colorOnPrimaryContainer(), tokens.colorPrimaryContainer()) >= 4.5);
            assertTrue(ThemeContrast.ratio(tokens.colorOnSurface(), tokens.colorSurfaceContainerHigh()) >= 4.5);
            assertTrue(ThemeContrast.ratio(tokens.colorOnSurfaceVariant(), tokens.colorSurfaceContainerHigh()) >= 4.5);
            assertTrue(ThemeContrast.ratio(tokens.colorFocus(), tokens.colorSurfaceContainerHigh()) >= 3.0);
        }
    }

    @Test
    public void exitDialogInflatesWithSemanticForegroundsInBothModes() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            View root = LayoutInflater.from(context).inflate(R.layout.dialog_exit_confirm, null, false);
            TextView title = root.findViewById(R.id.title);
            TextView message = root.findViewById(R.id.message);
            TextView negative = root.findViewById(R.id.negative);
            TextView positive = root.findViewById(R.id.positive);
            assertEquals(context.getColor(R.color.webhtv_color_on_surface), title.getTextColors().getDefaultColor());
            assertEquals(context.getColor(R.color.webhtv_color_on_surface_variant), message.getTextColors().getDefaultColor());
            assertEquals(context.getColor(R.color.webhtv_color_on_primary), positive.getTextColors().getDefaultColor());

            ColorStateList secondary = ContextCompat.getColorStateList(context, R.color.exit_confirm_secondary_text);
            assertEquals(context.getColor(R.color.webhtv_color_on_surface_variant),
                    secondary.getDefaultColor());
            assertEquals(context.getColor(R.color.webhtv_color_on_primary_container),
                    secondary.getColorForState(new int[]{android.R.attr.state_focused}, 0));

            assertTrue(ThemeContrast.ratio(
                    secondary.getColorForState(new int[]{android.R.attr.state_focused}, 0),
                    context.getColor(R.color.webhtv_color_primary_container)) >= 4.5);
            assertTrue(ThemeContrast.ratio(
                    positive.getTextColors().getDefaultColor(),
                    context.getColor(R.color.webhtv_color_primary)) >= 4.5);
            assertEquals(negative.getTextColors().getDefaultColor(),
                    secondary.getDefaultColor());
        }
    }

    @Test
    public void exitDialogUsesSharedBindingAndSemanticResources() throws Exception {
        String java = read("src/leanback/java/com/fongmi/android/tv/ui/dialog/ExitConfirmDialog.java");
        String layout = read("src/leanback/res/layout/dialog_exit_confirm.xml");
        assertTrue(java.contains("ThemeController.bindDialog(dialog)"));
        assertTrue(layout.contains("@drawable/shape_exit_confirm_dialog"));
        assertTrue(layout.contains("@color/exit_confirm_secondary_text"));
        assertTrue(layout.contains("?attr/colorOnPrimary"));
        assertFalse(layout.contains("selector_git_cloud_card"));
        assertFalse(read("src/leanback/res/drawable/shape_exit_confirm_dialog.xml").contains("#FBFCFF"));
    }

    @Test
    public void appearanceRowsUseComponentThemeSemanticsOnBothFlavours() throws Exception {
        for (String path : new String[]{
                "src/leanback/java/com/fongmi/android/tv/ui/dialog/AppearanceDialog.java",
                "src/mobile/java/com/fongmi/android/tv/ui/dialog/AppearanceDialog.java"}) {
            String source = read(path);
            if (path.contains("/leanback/")) {
                assertTrue(source.contains("AppearanceRowTheme.apply(row, title, summary, ThemeController.current())"));
            } else {
                assertTrue(source.contains("ThemeEditorUi.interactive"));
                assertTrue(source.contains("ThemeEditorUi.readable"));
            }
            assertFalse(source.contains("R.drawable.selector_git_cloud_card"));
        }
    }

    @Test
    public void homeMenuAndHistorySelectorsRemainIsolated() throws Exception {
        Context context = themed(false);
        View root = LayoutInflater.from(context).inflate(R.layout.adapter_home_menu, null, false);
        TextView text = root.findViewById(R.id.text);
        ColorStateList home = text.getTextColors();
        assertEquals(context.getColor(R.color.webhtv_color_on_primary),
                home.getColorForState(new int[]{android.R.attr.state_focused}, 0));
        assertEquals(context.getColor(R.color.webhtv_color_on_surface), home.getDefaultColor());
        assertEquals(context.getColor(R.color.webhtv_color_primary),
                ContextCompat.getColorStateList(context, R.color.config_history_text)
                        .getColorForState(new int[]{android.R.attr.state_focused}, 0));
        assertTrue(read("src/leanback/res/layout/adapter_home_menu.xml")
                .contains("@color/home_menu_text"));
        assertFalse(read("src/leanback/res/color/config_history_text.xml")
                .contains("colorOnPrimary"));
    }

    @Test
    public void siteRebindPreservesViewGeometry() throws Exception {
        String helper = read("src/leanback/java/com/fongmi/android/tv/ui/helper/SiteDialogTheme.java");
        assertTrue(helper.contains("int left = root.getPaddingLeft()"));
        assertTrue(helper.contains("int top = root.getPaddingTop()"));
        assertTrue(helper.contains("int right = root.getPaddingRight()"));
        assertTrue(helper.contains("int bottom = root.getPaddingBottom()"));
        assertTrue(helper.contains("root.setPadding(left, top, right, bottom)"));
    }

    @Test
    public void videoItemAndNativeIntroUseThemeSafeStates() throws Exception {
        String selector = read("src/leanback/res/drawable/selector_video_item.xml");
        assertTrue(selector.contains("state_focused"));
        assertTrue(selector.contains("state_pressed"));
        assertTrue(selector.contains("state_activated"));
        assertTrue(selector.contains("state_selected"));
        assertTrue(selector.contains("#E616202A"));
        assertTrue(selector.contains("#CC101820"));
        assertTrue(selector.contains("#B3101820"));
        assertTrue(selector.contains("?attr/tvFocusRing"));
        assertTrue(selector.contains("?attr/tvCurrentRing"));
        assertTrue(selector.contains("?attr/tvNormalStroke"));
        String layout = read("src/leanback/res/layout/activity_video.xml");
        assertTrue(layout.contains("@+id/content"));
        assertTrue(layout.contains("drawableTint"));
        assertTrue(read("src/leanback/res/drawable/ic_detail_desc.xml")
                .contains("colorOnSurface"));
    }

    @Test
    public void tvThemeNoLongerOverridesSharedLightNightTokens() throws Exception {
        String style = read("src/main/res/values/webhtv_styles.xml");
        assertFalse(style.replaceAll("(?s)<!--.*?-->", "").contains("name=\"isLightTheme\""));
        assertFalse(Files.exists(root().resolve("src/leanback/res/values/webhtv_tokens.xml")));
    }

    private static Context themed(boolean dark) {
        Configuration configuration = new Configuration(
                RuntimeEnvironment.getApplication().getResources().getConfiguration());
        configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        Context context = RuntimeEnvironment.getApplication().createConfigurationContext(configuration);
        context.setTheme(R.style.Theme_App);
        return context;
    }

    private static Path root() {
        return Files.exists(Path.of("src")) ? Path.of("") : Path.of("app");
    }

    private static String read(String relative) throws Exception {
        return Files.readString(root().resolve(relative), StandardCharsets.UTF_8);
    }
}
