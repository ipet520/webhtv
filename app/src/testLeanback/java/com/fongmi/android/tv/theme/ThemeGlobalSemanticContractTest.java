package com.fongmi.android.tv.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.appcompat.app.AppCompatActivity;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.ui.custom.SettingClipboardOverlay;
import com.google.android.material.button.MaterialButton;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
public class ThemeGlobalSemanticContractTest {

    @Test
    public void genericSheetTextAndBackgroundStayPairedAcrossModes() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            ColorStateList text = ContextCompat.getColorStateList(context, R.color.selector_theme_sheet_text);
            assertNotNull(text);
            assertEquals(tokens.colorOnSurface(), text.getDefaultColor());
            assertEquals(tokens.colorOnSurfaceVariant(), text.getColorForState(new int[]{-android.R.attr.state_enabled}, 0));
            assertEquals(tokens.colorOnPrimary(), text.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
            assertEquals(tokens.colorOnPrimaryContainer(), text.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}, 0));

            Drawable background = ContextCompat.getDrawable(context, R.drawable.selector_theme_sheet_button);
            assertNotNull(background);
            assertEquals(tokens.colorSurfaceContainer(), fill(background, new int[]{android.R.attr.state_enabled}));
            assertEquals(tokens.colorPrimary(), fill(background, new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}));
            assertEquals(tokens.colorPrimaryContainer(), fill(background, new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}));
            assertTrue(ThemeContrast.ratio(tokens.colorOnSurface(), tokens.colorSurfaceContainer()) >= 4.5);
            assertTrue(ThemeContrast.ratio(tokens.colorOnPrimary(), tokens.colorPrimary()) >= 4.5);
            assertTrue(ThemeContrast.ratio(tokens.colorOnPrimaryContainer(), tokens.colorPrimaryContainer()) >= 4.5);
        }
    }

    @Test
    public void playerSelectionChipResolvesFinalStatePairsAcrossModes() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            TextView chip = (TextView) LayoutInflater.from(context)
                    .inflate(R.layout.adapter_track, null, false);
            ColorStateList text = chip.getTextColors();
            Drawable background = chip.getBackground();

            assertStatePair(text, background, new int[]{android.R.attr.state_enabled},
                    tokens.colorOnSurface(), tokens.colorSurfaceContainerHigh());
            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_focused},
                    tokens.colorOnPrimary(), tokens.colorPrimary());
            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed},
                    tokens.colorOnPrimary(), tokens.colorPrimary());
            int disabledText = ColorUtils.setAlphaComponent(tokens.colorOnSurfaceVariant(), 0x73);
            background.setState(new int[]{-android.R.attr.state_enabled});
            assertEquals(disabledText, text.getColorForState(
                    new int[]{-android.R.attr.state_enabled}, 0));
            assertEquals(tokens.colorSurfaceContainer(), visibleFill(background));
            assertTrue(disabledText != tokens.colorSurfaceContainer());
        }
    }

    @Test
    public void playerControlSheetCallerResolvesFocusedAudioVideoStates() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            ViewGroup timer = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.dialog_timer, null, false);
            TextView option = timer.findViewById(R.id.time1);
            assertNotNull(option);
            ColorStateList text = option.getTextColors();
            Drawable background = option.getBackground();

            assertEquals(tokens.colorPlayerControlMuted(), text.getColorForState(
                    new int[]{android.R.attr.state_enabled}, 0));
            assertStatePair(text, background, new int[]{android.R.attr.state_enabled},
                    tokens.colorPlayerControlMuted(), 0xFF37373A);
            assertEquals(tokens.colorPlayerControlActive(), text.getColorForState(
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
            assertEquals(tokens.colorPlayerControlActive(), text.getColorForState(
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}, 0));
            assertEquals(tokens.colorPlayerControlActive(), text.getColorForState(
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_activated}, 0));
            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_focused},
                    tokens.colorPlayerControlActive(), 0xFF4B4B4F);
            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed},
                    tokens.colorPlayerControlActive(), 0xFF4B4B4F);
        }
    }

    @Test
    public void customCspProxyResolvesFinalStatePairsAcrossModes() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            TextView proxy = (TextView) LayoutInflater.from(context)
                    .inflate(R.layout.adapter_github_proxy, null, false)
                    .findViewById(R.id.text);
            ColorStateList text = proxy.getTextColors();
            Drawable background = proxy.getBackground();

            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled},
                    tokens.colorOnSurface(), tokens.colorSurfaceContainerHigh());
            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_focused},
                    tokens.colorOnPrimaryContainer(), tokens.colorPrimaryContainer());
            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed},
                    tokens.colorOnPrimaryContainer(), tokens.colorPrimaryContainer());
            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_activated},
                    tokens.colorOnSecondaryContainer(), tokens.colorSecondaryContainer());
            assertStatePair(text, background,
                    new int[]{-android.R.attr.state_enabled},
                    tokens.colorOnSurfaceVariant(), tokens.colorSurfaceContainerLow());
        }
    }

    @Test
    public void siteRowsPropagateParentFocusAndSelectionToText() throws Exception {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            ViewGroup row = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.adapter_site, null, false);
            TextView text = row.findViewById(R.id.text);
            assertNotNull(text);
            assertTrue(read("src/leanback/res/layout/adapter_site.xml")
                    .contains("android:duplicateParentState=\"true\""));

            Drawable background = row.getBackground();
            assertStatePair(text.getTextColors(), background,
                    new int[]{android.R.attr.state_enabled},
                    tokens.colorOnSurfaceVariant(), tokens.colorSurfaceContainer());
            assertStatePair(text.getTextColors(), background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_focused},
                    tokens.colorOnPrimary(), tokens.colorPrimary());
            assertStatePair(text.getTextColors(), background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_selected},
                    tokens.colorOnPrimaryContainer(), tokens.colorPrimaryContainer());
        }
    }

    @Test
    public void sharedStatusBadgesUseTheirSurfaceForegroundPair() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();

            ViewGroup about = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.dialog_about, null, false);
            TextView channel = about.findViewById(R.id.channel);
            assertNotNull(channel);
            assertEquals(tokens.colorOnPrimaryContainer(), channel.getCurrentTextColor());
            assertEquals(tokens.colorPrimaryContainer(), visibleFill(channel.getBackground()));
            assertTrue(ThemeContrast.ratio(channel.getCurrentTextColor(),
                    visibleFill(channel.getBackground())) >= 4.5);

            ViewGroup tmdb = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.adapter_tmdb_item, null, false);
            TextView current = tmdb.findViewById(R.id.current);
            assertNotNull(current);
            assertEquals(tokens.colorOnPrimaryContainer(), current.getCurrentTextColor());
            assertEquals(tokens.colorPrimaryContainer(), visibleFill(current.getBackground()));
            assertTrue(ThemeContrast.ratio(current.getCurrentTextColor(),
                    visibleFill(current.getBackground())) >= 4.5);
        }
    }

    @Test
    public void fixedLightBadgesAndStepButtonsResolveSemanticStates() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();

            ViewGroup profile = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.adapter_mpv_config_profile, null, false);
            TextView active = profile.findViewById(R.id.active);
            assertNotNull(active);
            assertEquals(tokens.colorOnPrimaryContainer(), active.getCurrentTextColor());
            assertEquals(tokens.colorPrimaryContainer(), visibleFill(active.getBackground()));

            Drawable step = ContextCompat.getDrawable(context, R.drawable.selector_dialog_step_button);
            assertNotNull(step);
            step.setState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused});
            assertEquals(tokens.colorPrimaryContainer(), visibleFill(step));
            ColorStateList stepText = ContextCompat.getColorStateList(
                    context, R.color.selector_dialog_step_text);
            assertEquals(tokens.colorOnPrimaryContainer(), stepText.getColorForState(
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
            assertTrue(ThemeContrast.ratio(tokens.colorOnPrimaryContainer(),
                    tokens.colorPrimaryContainer()) >= 4.5);
        }
    }

    @Test
    public void disabledStateWinsOverActiveStateAcrossSharedSelectors() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            int disabledOnSurfaceVariant = ColorUtils.setAlphaComponent(
                    tokens.colorOnSurfaceVariant(), 0x73);
            int disabledPrimaryContainer = ColorUtils.setAlphaComponent(
                    tokens.colorPrimaryContainer(), 0x61);

            ColorStateList about = ContextCompat.getColorStateList(
                    context, R.color.about_primary_action_bg);
            assertEquals(disabledPrimaryContainer, about.getColorForState(
                    new int[]{-android.R.attr.state_enabled, android.R.attr.state_focused}, 0));

            ColorStateList tmdbText = ContextCompat.getColorStateList(
                    context, R.color.tmdb_action_button_text);
            ColorStateList tmdbBackground = ContextCompat.getColorStateList(
                    context, R.color.tmdb_action_button_bg);
            ColorStateList tmdbStroke = ContextCompat.getColorStateList(
                    context, R.color.tmdb_action_button_stroke);
            int[] disabledSelected = new int[]{-android.R.attr.state_enabled, android.R.attr.state_selected};
            assertEquals(disabledOnSurfaceVariant, tmdbText.getColorForState(disabledSelected, 0));
            assertEquals(tokens.colorSurfaceContainerHighest(),
                    tmdbBackground.getColorForState(disabledSelected, 0));
            assertEquals(tokens.colorOutlineVariant(), tmdbStroke.getColorForState(disabledSelected, 0));

            ColorStateList control = ContextCompat.getColorStateList(
                    context, R.color.selector_control);
            ColorStateList stroke = ContextCompat.getColorStateList(
                    context, R.color.selector_stroke);
            assertEquals(tokens.colorOnSurfaceVariant(), control.getColorForState(disabledSelected, 0));
            assertEquals(tokens.colorOutlineVariant(), stroke.getColorForState(disabledSelected, 0));

            Drawable lab = ContextCompat.getDrawable(context, R.drawable.shape_lab_run_btn);
            assertNotNull(lab);
            lab.setState(new int[]{-android.R.attr.state_enabled, android.R.attr.state_pressed});
            assertEquals(ContextCompat.getColor(context, R.color.white_20), visibleFill(lab));

            Drawable labAction = ContextCompat.getDrawable(context, R.drawable.shape_lab_click_btn);
            assertNotNull(labAction);
            labAction.setState(new int[]{-android.R.attr.state_enabled, android.R.attr.state_pressed});
            assertEquals(ContextCompat.getColor(context, R.color.white_20), visibleFill(labAction));
        }
    }

    @Test
    public void rawDialogButtonContractResolvesActualStateColors() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            ThemeController.apply(Robolectric.buildActivity(AppCompatActivity.class).setup().get(), tokens);

            MaterialButton primary = new MaterialButton(context);
            ThemeDialogSemantics.primary(primary);
            ColorStateList primaryText = primary.getTextColors();
            ColorStateList primaryFill = primary.getBackgroundTintList();
            assertEquals(tokens.colorOnSurfaceVariant(), primaryText.getDefaultColor());
            assertEquals(tokens.colorOnPrimary(), primaryText.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
            assertTrue("primary default fill expected=" + Integer.toHexString(tokens.colorSurfaceContainerHighest())
                    + " actual=" + Integer.toHexString(primaryFill.getDefaultColor()),
                    primaryFill.getDefaultColor() == tokens.colorSurfaceContainerHighest());
            assertEquals(tokens.colorPrimary(), primaryFill.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));

            MaterialButton secondary = new MaterialButton(context);
            ThemeDialogSemantics.secondary(secondary);
            assertEquals(tokens.colorOnSurfaceVariant(), secondary.getTextColors().getDefaultColor());
            assertEquals(tokens.colorPrimary(), secondary.getTextColors().getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
            assertEquals(tokens.colorPrimaryContainer(), secondary.getBackgroundTintList().getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
            assertTrue(ThemeContrast.ratio(tokens.colorOnPrimary(), tokens.colorPrimary()) >= 4.5);
            assertTrue(ThemeContrast.ratio(tokens.colorPrimary(), tokens.colorPrimaryContainer()) >= 3.0);
        }
    }

    @Test
    public void programmaticOverlayAndPerformanceStatesUseThemeSemantics() throws Exception {
        for (boolean dark : new boolean[]{false, true}) {
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            Activity activity = Robolectric.buildActivity(AppCompatActivity.class).setup().get();
            ThemeController.apply((AppCompatActivity) activity, tokens);

            Class<?> overlayClass = SettingClipboardOverlay.class;
            Constructor<?> constructor = overlayClass.getDeclaredConstructor(
                    Activity.class, androidx.fragment.app.DialogFragment.class, View.class, ViewGroup.class);
            constructor.setAccessible(true);
            FrameLayout host = new FrameLayout(activity);
            Object overlay = constructor.newInstance(activity, null, host, host);
            Method rowMethod = overlayClass.getDeclaredMethod("row", String.class);
            rowMethod.setAccessible(true);

            View normalRow = (View) rowMethod.invoke(overlay, "normal");
            assertEquals(tokens.colorSurfaceContainer(), fill(normalRow.getBackground()));
            assertEquals(tokens.colorOnSurface(), ((TextView) ((ViewGroup) normalRow).getChildAt(1))
                    .getTextColors().getDefaultColor());

            java.lang.reflect.Field selected = overlayClass.getDeclaredField("selected");
            selected.setAccessible(true);
            ((java.util.Set<String>) selected.get(overlay)).add("selected");
            View selectedRow = (View) rowMethod.invoke(overlay, "selected");
            assertEquals(tokens.colorPrimaryContainer(), fill(selectedRow.getBackground()));
            assertEquals(tokens.colorOnPrimaryContainer(), ((TextView) ((ViewGroup) selectedRow).getChildAt(1))
                    .getTextColors().getDefaultColor());

            Class<?> modalClass = Class.forName("com.fongmi.android.tv.ui.dialog.PlaybackPerformanceModal");
            Method listStyle = modalClass.getDeclaredMethod("styleListButton", MaterialButton.class, boolean.class);
            listStyle.setAccessible(true);
            MaterialButton choice = new MaterialButton(activity);
            listStyle.invoke(null, choice, false);
            assertEquals(tokens.colorOnSurface(), choice.getTextColors().getDefaultColor());
            assertEquals(tokens.colorSurfaceContainerHighest(), choice.getBackgroundTintList().getDefaultColor());
            assertEquals(tokens.colorOnPrimary(), choice.getTextColors().getColorForState(
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
            assertEquals(tokens.colorPrimary(), choice.getBackgroundTintList().getColorForState(
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
        }
    }

    @Test
    public void popupEditorAndDialogEntryPointsRemainThemeAware() throws Exception {
        assertTrue(read("src/main/java/com/fongmi/android/tv/ui/dialog/LightDialog.java")
                .contains("ThemeDialogSemantics.bindLegacyDialog"));
        assertTrue(read("src/main/java/com/fongmi/android/tv/ui/dialog/BaseAlertDialog.java")
                .contains("ThemeDialogSemantics.bindLegacyDialog"));
        assertTrue(read("src/main/java/com/fongmi/android/tv/ui/custom/MpvConfigEditText.java")
                .contains("ThemeController.current()"));
        assertTrue(read("src/main/java/com/fongmi/android/tv/ui/dialog/SliderNumberDialog.java")
                .contains("tokens.colorPrimary()"));
        assertTrue(read("src/main/java/com/fongmi/android/tv/ui/dialog/SpeedSettingDialog.java")
                .contains("tokens.colorOutlineVariant()"));

        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            Drawable background = ContextCompat.getDrawable(context, R.drawable.selector_mpv_action_item);
            ColorStateList text = ContextCompat.getColorStateList(context, R.color.selector_mpv_action_text);
            assertNotNull(background);
            assertNotNull(text);
            assertEquals(tokens.colorSurfaceContainerHigh(), fill(background, new int[]{android.R.attr.state_enabled}));
            assertEquals(tokens.colorPrimaryContainer(), fill(background, new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}));
            assertEquals(tokens.colorOnSurface(), text.getDefaultColor());
            assertEquals(tokens.colorOnPrimaryContainer(), text.getColorForState(
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
        }
    }

    @Test
    public void fixedPosterTextSelectorUsesItsIntentionalDarkOverlayContract() throws Exception {
        String source = read("src/main/res/color/selector_video_text.xml");
        assertTrue(source.contains("state_activated"));
        assertTrue(source.contains("state_selected"));
        assertTrue(source.contains("@color/yellow_500"));
        ColorStateList text = ContextCompat.getColorStateList(themed(false), R.color.selector_video_text);
        assertEquals(0xFFFFFFFF, text.getDefaultColor());
        assertEquals(0xFFFFFFFF, text.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
        int selected = text.getColorForState(new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}, 0);
        assertTrue(selected != text.getDefaultColor());
        assertTrue(ThemeContrast.ratio(selected, 0xFF252527) >= 3.0);
        assertTrue(ThemeContrast.ratio(text.getDefaultColor(), 0xFF252527) >= 4.5);
    }

    @Test
    public void highRiskCallersUseSeparatedSemanticFamilies() throws Exception {
        String config = read("src/main/res/layout/adapter_player_button_config.xml");
        String proxy = read("src/main/res/layout/adapter_shell_proxy_rule.xml");
        assertTrue(config.contains("@drawable/selector_settings_card"));
        assertTrue(proxy.contains("@drawable/selector_settings_card"));
        assertTrue(read("src/mobile/java/com/fongmi/android/tv/ui/fragment/SearchFragment.java")
                .contains("ThemeDialogSemantics.popup"));
        assertTrue(read("src/mobile/java/com/fongmi/android/tv/ui/fragment/CollectFragment.java")
                .contains("ThemeDialogSemantics.popup"));
        for (String path : new String[]{
                "src/main/java/com/fongmi/android/tv/ui/dialog/GitCloudDialog.java",
                "src/main/java/com/fongmi/android/tv/ui/dialog/RemoteTrustDialog.java",
                "src/main/java/com/fongmi/android/tv/ui/dialog/PlaybackPerformanceDialog.java",
                "src/main/java/com/fongmi/android/tv/ui/dialog/WebHomeExtensionDebugDialog.java",
                "src/main/java/com/fongmi/android/tv/ui/dialog/AdBlockLogFilterDialog.java",
                "src/main/java/com/fongmi/android/tv/ui/dialog/BackupProgressDialog.java"}) {
            assertTrue(path, read(path).contains("ThemeDialogSemantics.bindLegacyDialog"));
        }
    }

    private static int fill(Drawable drawable, int[] state) {
        drawable.setState(state);
        return fill(drawable);
    }

    private static int fill(Drawable drawable) {
        if (drawable instanceof GradientDrawable gradient) {
            ColorStateList color = gradient.getColor();
            return color == null ? 0x00000000 : color.getDefaultColor();
        }
        if (drawable instanceof StateListDrawable states) return fill(states.getCurrent());
        if (drawable instanceof RippleDrawable ripple) {
            for (int index = 0; index < ripple.getNumberOfLayers(); index++) {
                int color = fill(ripple.getDrawable(index));
                if (color != 0x00000000) return color;
            }
        }
        if (drawable instanceof LayerDrawable layer) {
            for (int index = 0; index < layer.getNumberOfLayers(); index++) {
                int color = fill(layer.getDrawable(index));
                if (color != 0x00000000) return color;
            }
        }
        if (drawable instanceof InsetDrawable inset) return fill(inset.getDrawable());
        if (drawable instanceof ScaleDrawable scale) return fill(scale.getDrawable());
        return 0x00000000;
    }

    private static void assertStatePair(ColorStateList text, Drawable background, int[] state,
                                        int expectedText, int expectedBackground) {
        int actualText = text.getColorForState(state, 0);
        background.setState(state);
        int actualBackground = visibleFill(background);
        assertEquals(expectedText, actualText);
        assertEquals(expectedBackground, actualBackground);
        assertTrue(ThemeContrast.ratio(actualText, actualBackground) >= 4.5);
    }

    private static int visibleFill(Drawable drawable) {
        if (drawable instanceof StateListDrawable states) return visibleFill(states.getCurrent());
        if (drawable instanceof RippleDrawable ripple) {
            return visibleFill(ripple.getDrawable(ripple.getNumberOfLayers() - 1));
        }
        if (drawable instanceof LayerDrawable layer) {
            return visibleFill(layer.getDrawable(layer.getNumberOfLayers() - 1));
        }
        if (drawable instanceof InsetDrawable inset) return visibleFill(inset.getDrawable());
        if (drawable instanceof ScaleDrawable scale) return visibleFill(scale.getDrawable());
        if (drawable instanceof GradientDrawable gradient && gradient.getColor() != null) {
            return gradient.getColor().getDefaultColor();
        }
        return 0x00000000;
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
