package com.fongmi.android.tv.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.view.LayoutInflater;
import android.view.View;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.ui.helper.TmdbCinemaTheme;
import com.google.android.material.button.MaterialButton;

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
public class ThemePhase2ContractTest {

    @Test
    public void directDetailActionStatesUsePairedSemanticColorsInLightAndDark() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            View root = LayoutInflater.from(context).inflate(R.layout.view_tmdb_header, null, false);
            MaterialButton action = root.findViewById(R.id.tmdbChangeSource);
            ColorStateList text = action.getTextColors();
            ColorStateList background = action.getBackgroundTintList();
            ColorStateList stroke = action.getStrokeColor();

            int normalText = text.getDefaultColor();
            int focusedText = text.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0);
            int pressedText = text.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed}, 0);
            int selectedText = text.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}, 0);
            int normalBackground = background.getDefaultColor();
            int focusedBackground = background.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0);
            int pressedBackground = background.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed}, 0);
            int selectedBackground = background.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}, 0);

            assertEquals(tokens.colorOnSurface(), normalText);
            assertEquals(tokens.colorOnPrimaryContainer(), focusedText);
            assertEquals(tokens.colorOnPrimaryContainer(), pressedText);
            assertEquals(tokens.colorOnSecondaryContainer(), selectedText);
            assertEquals(tokens.colorSurfaceContainerHigh(), normalBackground);
            assertEquals(tokens.colorPrimaryContainer(), focusedBackground);
            assertEquals(tokens.colorPrimaryContainer(), pressedBackground);
            assertEquals(tokens.colorSecondaryContainer(), selectedBackground);
            assertEquals(tokens.colorFocus(), stroke.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));

            assertTrue(ThemeContrast.ratio(normalText, normalBackground) >= 4.5);
            assertTrue(ThemeContrast.ratio(focusedText, focusedBackground) >= 4.5);
            assertTrue(ThemeContrast.ratio(pressedText, pressedBackground) >= 4.5);
            assertTrue(ThemeContrast.ratio(selectedText, selectedBackground) >= 4.5);
            assertTrue(ThemeContrast.ratio(tokens.colorFocus(), focusedBackground) >= 3.0);
        }
    }

    @Test
    public void directDetailProfileAndCinemaPalettesStayDistinctAcrossThemeModes() {
        assertTrue(ThemeContrast.ratio(ThemeTokens.light().colorOnSurface(), ThemeTokens.light().colorSurfaceContainerHigh()) >= 4.5);
        assertTrue(ThemeContrast.ratio(ThemeTokens.dark().colorOnSurface(), ThemeTokens.dark().colorSurfaceContainerHigh()) >= 4.5);
        for (boolean light : new boolean[]{false, true}) {
            TmdbCinemaTheme.Palette palette = TmdbCinemaTheme.palette(light);
            assertTrue(ThemeContrast.ratio(palette.primary(), palette.background()) >= 4.5);
            assertTrue(ThemeContrast.ratio(palette.primary(), palette.card()) >= 4.5);
        }
        assertFalse(TmdbCinemaTheme.palette(false).background() == TmdbCinemaTheme.palette(true).background());
    }

    @Test
    public void directDetailRuntimeActionPathUsesTheComponentStateHelper() throws Exception {
        String source = read("src/main/java/com/fongmi/android/tv/ui/custom/TmdbHeaderView.java");
        assertTrue(source.contains("applySemanticActionButton(button)"));
        assertTrue(source.contains("R.color.tmdb_action_button_text"));
        assertTrue(source.contains("R.color.tmdb_action_button_bg"));
        assertFalse(source.contains("0xFF16372F"));
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

    private static String read(String relative) throws Exception {
        Path root = Files.exists(Path.of("src")) ? Path.of("") : Path.of("app");
        return Files.readString(root.resolve(relative), StandardCharsets.UTF_8);
    }
}
