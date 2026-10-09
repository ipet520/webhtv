package com.fongmi.android.tv.ui.theme;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.FrameLayout;

import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.theme.ThemeTokens;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.ui.activity.SettingPlayerActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowGradientDrawable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
public class SettingFocusThemeContractTest {

    private static final int CUSTOM_FOCUS = 0xFF18C65C;
    private static final int UPDATED_FOCUS = 0xFFE5395A;

    @Test
    public void settingsSelectorFocusedStrokeTracksEachRuntimeTheme() {
        Context context = RuntimeEnvironment.getApplication();
        context.setTheme(R.style.Theme_App);
        FrameLayout root = new FrameLayout(context);
        View item = new View(context);
        Drawable background = ContextCompat.getDrawable(context, R.drawable.selector_item);
        assertTrue(background instanceof StateListDrawable);
        item.setBackground(background);
        StateListDrawable selector = (StateListDrawable) background;
        assertEquals(3, selector.getStateCount());
        assertArrayEquals(new int[]{android.R.attr.state_focused}, selector.getStateSet(0));
        assertArrayEquals(new int[]{android.R.attr.state_selected}, selector.getStateSet(1));
        assertTrue("the target activity is in the settings scope", SettingFocusTheme.isSettingsActivity(SettingPlayerActivity.class));
        assertTrue("the resource selector matches the settings row signature", SettingFocusTheme.isSettingsItemSelector(item, selector));
        GradientDrawable shape = (GradientDrawable) selector.getStateDrawable(0);
        assertEquals(GradientDrawable.RECTANGLE, shape.getShape());
        assertEquals(0x66000000, shape.getColor().getDefaultColor());
        assertEquals(4f * context.getResources().getDisplayMetrics().density, shape.getCornerRadius(), 0.5f);
        int originalStroke = strokeColor(selector);
        assertNotEquals("test Focus must differ from the compiled selector color", CUSTOM_FOCUS, originalStroke);
        GradientDrawable selected = (GradientDrawable) selector.getStateDrawable(1);
        int selectedFill = selected.getColor().getDefaultColor();
        int focusedFill = shape.getColor().getDefaultColor();
        root.addView(item);

        assertTrue("settings tree should bind its standard selector", SettingFocusTheme.bind(SettingPlayerActivity.class, root,
                ThemeTokens.light().withColorFocus(CUSTOM_FOCUS)));
        assertEquals("custom Focus reaches the focused stroke", CUSTOM_FOCUS,
                strokeColor(selector));
        assertEquals("the focused fill is left unchanged", focusedFill, shape.getColor().getDefaultColor());
        assertEquals("the selected fill is left unchanged", selectedFill, selected.getColor().getDefaultColor());

        SettingFocusTheme.bind(SettingPlayerActivity.class, root,
                ThemeTokens.light().withColorFocus(UPDATED_FOCUS));
        assertEquals("rebinding updates the focused stroke to the new profile", UPDATED_FOCUS,
                strokeColor(selector));
    }

    @Test
    public void nonSettingsActivityDoesNotReceiveSettingsFocusBinding() {
        Context context = RuntimeEnvironment.getApplication();
        context.setTheme(R.style.Theme_App);
        View item = new View(context);
        item.setBackground(ContextCompat.getDrawable(context, R.drawable.selector_item));
        StateListDrawable selector = (StateListDrawable) item.getBackground();

        int originalStroke = strokeColor(selector);
        assertTrue("non-settings activities are outside the binding scope", !SettingFocusTheme.bind(HomeActivity.class, item,
                ThemeTokens.light().withColorFocus(CUSTOM_FOCUS)));
        assertEquals("non-settings drawables remain unchanged", originalStroke, strokeColor(selector));
    }

    @Test
    public void activityBindsCurrentFocusAfterTheThemeBoundSettingsTreeIsCreated() throws Exception {
        String base = read("src/leanback/java/com/fongmi/android/tv/ui/base/BaseActivity.java");
        String helper = read("src/leanback/java/com/fongmi/android/tv/ui/theme/SettingFocusTheme.java");
        int initialize = base.indexOf("initView(savedInstanceState)");
        int themeBinding = base.indexOf("ThemeController.bindTheme(content)", initialize);
        int focusBinding = base.indexOf("SettingFocusTheme.bind(getClass(), content, ThemeController.current())");
        assertTrue(initialize >= 0 && themeBinding > initialize && focusBinding > themeBinding);
        assertTrue(base.contains("event.getType() == RefreshEvent.Type.THEME") && base.contains("recreate()"));
        assertTrue(helper.contains("tokens.colorFocus()"));
        assertTrue(helper.contains("focused.setStroke(strokeWidth, focusColor)"));
        assertTrue(helper.contains("SettingActivity.class") && helper.contains("SettingPlayerActivity.class"));
        assertTrue(helper.contains("SettingTmdbActivity.class"));
        assertTrue("the helper must update the stroke only", !helper.contains("focused.setColor("));
    }

    private static int strokeColor(StateListDrawable selector) {
        return Shadow.<ShadowGradientDrawable>extract(selector.getStateDrawable(0)).getStrokeColor();
    }

    private static String read(String path) throws Exception {
        Path direct = Path.of(path);
        if (Files.exists(direct)) return Files.readString(direct, StandardCharsets.UTF_8);
        return Files.readString(Path.of("..").resolve(path), StandardCharsets.UTF_8);
    }
}
