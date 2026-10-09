package com.fongmi.android.tv.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.widget.FrameLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;

import androidx.appcompat.app.AppCompatActivity;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowGradientDrawable;
import org.robolectric.shadow.api.Shadow;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
public class ThemeBinderRuntimeTest {

    private static final String STATE_BINDING =
            "webhtv:state-priority:pressed+focused+selected;"
                    + "webhtv:pressed-fill:secondary_container;"
                    + "webhtv:focused-fill:primary;"
                    + "webhtv:selected-fill:primary_container;"
                    + "webhtv:disabled-text:on_surface_variant;"
                    + "webhtv:pressed-text:on_secondary_container;"
                    + "webhtv:focused-text:on_primary;"
                    + "webhtv:selected-text:on_primary_container;"
                    + "webhtv:pressed-stroke:focus;"
                    + "webhtv:focused-stroke:focus;"
                    + "webhtv:selected-stroke:outline;"
                    + "webhtv:pressed-stroke-width:2dp;"
                    + "webhtv:focused-stroke-width:2dp;"
                    + "webhtv:selected-stroke-width:1dp";

    public static class Host extends AppCompatActivity {
        @Override
        protected void onCreate(android.os.Bundle state) {
            setTheme(R.style.Theme_App);
            super.onCreate(state);
        }
    }

    @Test
    public void selectorRolesAndTextPriorityFollowRepeatedRuntimePaletteChanges() {
        Context context = Robolectric.buildActivity(Host.class).setup().get();
        ThemeTokens baseline = ThemeTokens.light();
        ThemeTokens first = palette("#234A91", "#D9E5FF", "#D8F0DF", "#D81B60");
        ThemeTokens second = palette("#285C3A", "#D7EEDD", "#F8E4C5", "#7B1FA2");

        TextView item = new TextView(context);
        ColorStateList originalText = new ColorStateList(
                new int[][]{
                        {-android.R.attr.state_enabled},
                        {android.R.attr.state_pressed},
                        {android.R.attr.state_focused},
                        {android.R.attr.state_selected},
                        {}
                },
                new int[]{
                        baseline.colorOnSurfaceVariant(),
                        baseline.colorOnSecondaryContainer(),
                        baseline.colorOnPrimary(),
                        baseline.colorOnPrimaryContainer(),
                        baseline.colorOnSurface()
                });
        item.setTextColor(originalText);

        StateListDrawable originalSelector = new StateListDrawable();
        originalSelector.addState(new int[]{android.R.attr.state_pressed},
                shape(baseline.colorSecondaryContainer()));
        originalSelector.addState(new int[]{android.R.attr.state_focused},
                shape(baseline.colorPrimary()));
        originalSelector.addState(new int[]{android.R.attr.state_selected},
                shape(baseline.colorPrimaryContainer()));
        originalSelector.addState(new int[]{}, shape(baseline.colorSurfaceContainerHigh()));
        item.setBackground(originalSelector);
        ThemeBinder.markSemanticBinding(item, STATE_BINDING);

        FrameLayout root = new FrameLayout(context);
        root.addView(item);

        ThemeBinder.bind(root, baseline, first);
        StateListDrawable firstSelector = (StateListDrawable) item.getBackground();
        assertEquals(first.colorPrimary(), fill(firstSelector.getStateDrawable(1)));
        assertEquals(first.colorSecondaryContainer(), fill(firstSelector.getStateDrawable(0)));
        assertEquals(first.colorPrimaryContainer(), fill(firstSelector.getStateDrawable(2)));
        assertEquals(first.colorFocus(), stroke(firstSelector.getStateDrawable(1)));
        assertEquals("binding must not mutate the selector source", 0xFF74777F,
                stroke(originalSelector.getStateDrawable(1)));
        assertEquals("focused fill keeps its source geometry", 12f,
                ((GradientDrawable) firstSelector.getStateDrawable(1)).getCornerRadius(), 0.01f);
        assertEquals(first.colorOnPrimary(), text(item, android.R.attr.state_enabled,
                android.R.attr.state_focused));
        assertEquals("pressed wins when pressed and focused are both present",
                first.colorOnSecondaryContainer(), text(item, android.R.attr.state_enabled,
                        android.R.attr.state_pressed, android.R.attr.state_focused));
        assertEquals(first.colorOnPrimaryContainer(), text(item, android.R.attr.state_enabled,
                android.R.attr.state_selected));
        assertEquals(first.colorOnSurfaceVariant(), text(item, -android.R.attr.state_enabled));

        ThemeBinder.bind(root, baseline, second);
        StateListDrawable secondSelector = (StateListDrawable) item.getBackground();
        assertEquals(second.colorPrimary(), fill(secondSelector.getStateDrawable(1)));
        assertEquals(second.colorFocus(), stroke(secondSelector.getStateDrawable(1)));
        assertEquals(second.colorOnPrimary(), text(item, android.R.attr.state_enabled,
                android.R.attr.state_focused));
        assertEquals(second.colorOnSecondaryContainer(), text(item, android.R.attr.state_enabled,
                android.R.attr.state_pressed, android.R.attr.state_focused));

        ThemeBinder.bind(root, baseline, baseline);
        StateListDrawable restoredSelector = (StateListDrawable) item.getBackground();
        assertEquals(4, restoredSelector.getStateCount());
        assertEquals(baseline.colorSecondaryContainer(), fill(restoredSelector.getStateDrawable(0)));
        assertEquals(baseline.colorPrimary(), fill(restoredSelector.getStateDrawable(1)));
        assertEquals(baseline.colorPrimaryContainer(), fill(restoredSelector.getStateDrawable(2)));
        assertEquals(baseline.colorFocus(), stroke(restoredSelector.getStateDrawable(1)));
        assertEquals(baseline.colorOnPrimary(), text(item, android.R.attr.state_enabled,
                android.R.attr.state_focused));
        assertEquals(baseline.colorOnSecondaryContainer(), text(item, android.R.attr.state_enabled,
                android.R.attr.state_pressed, android.R.attr.state_focused));
        assertEquals(baseline.colorOnPrimaryContainer(), text(item, android.R.attr.state_enabled,
                android.R.attr.state_selected));
        assertEquals(baseline.colorOnSurfaceVariant(), text(item, -android.R.attr.state_enabled));
        assertEquals("the saved source selector remains isolated",
                baseline.colorPrimary(), fill(originalSelector.getStateDrawable(1)));
        assertEquals(0xFF74777F, stroke(originalSelector.getStateDrawable(1)));
        assertEquals("the saved source geometry remains unchanged", 12f,
                ((GradientDrawable) originalSelector.getStateDrawable(1)).getCornerRadius(), 0.01f);
        assertEquals(baseline.colorOnPrimary(), originalText.getColorForState(
                new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
    }

    @Test
    public void materialButtonKeepsIndependentFillTextStrokeAndIconStateChannels() {
        Context context = Robolectric.buildActivity(Host.class).setup().get();
        ThemeTokens baseline = ThemeTokens.light();
        ThemeTokens active = palette("#234A91", "#D9E5FF", "#D8F0DF", "#D81B60");
        ColorStateList fill = new ColorStateList(
                new int[][]{
                        {android.R.attr.state_pressed},
                        {android.R.attr.state_focused},
                        {}
                },
                new int[]{baseline.colorPrimaryContainer(), baseline.colorPrimary(), baseline.colorSurface()});
        ColorStateList text = new ColorStateList(
                new int[][]{
                        {android.R.attr.state_pressed},
                        {android.R.attr.state_focused},
                        {}
                },
                new int[]{baseline.colorOnPrimaryContainer(), baseline.colorOnPrimary(), baseline.colorOnSurface()});
        ColorStateList stroke = new ColorStateList(
                new int[][]{
                        {android.R.attr.state_pressed},
                        {android.R.attr.state_focused},
                        {}
                },
                new int[]{baseline.colorFocus(), baseline.colorFocus(), baseline.colorOutline()});
        ColorStateList icon = new ColorStateList(
                new int[][]{
                        {android.R.attr.state_pressed},
                        {android.R.attr.state_focused},
                        {}
                },
                new int[]{baseline.colorOnPrimaryContainer(), baseline.colorOnPrimary(), baseline.colorOnSurfaceVariant()});

        MaterialButton button = new MaterialButton(context);
        button.setBackgroundTintList(fill);
        button.setTextColor(text);
        button.setStrokeColor(stroke);
        button.setIconTint(icon);
        ThemeBinder.markSemanticBinding(button,
                "webhtv:state-priority:pressed+focused;"
                        + "webhtv:pressed-fill:primary_container;"
                        + "webhtv:focused-fill:primary;"
                        + "webhtv:pressed-text:on_primary_container;"
                        + "webhtv:focused-text:on_primary;"
                        + "webhtv:pressed-stroke:focus;"
                        + "webhtv:focused-stroke:focus;"
                        + "webhtv:pressed-icon:on_primary_container;"
                        + "webhtv:focused-icon:on_primary");

        FrameLayout root = new FrameLayout(context);
        root.addView(button);
        ThemeBinder.bind(root, baseline, active);

        int[] focused = {android.R.attr.state_enabled, android.R.attr.state_focused};
        int[] pressedFocused = {android.R.attr.state_enabled, android.R.attr.state_pressed,
                android.R.attr.state_focused};
        assertEquals(active.colorPrimary(),
                button.getBackgroundTintList().getColorForState(focused, 0));
        assertEquals(active.colorPrimaryContainer(),
                button.getBackgroundTintList().getColorForState(pressedFocused, 0));
        assertEquals(active.colorOnPrimary(), button.getTextColors().getColorForState(focused, 0));
        assertEquals(active.colorOnPrimaryContainer(),
                button.getTextColors().getColorForState(pressedFocused, 0));
        assertEquals(active.colorFocus(), button.getStrokeColor().getColorForState(focused, 0));
        assertEquals(active.colorFocus(), button.getStrokeColor().getColorForState(pressedFocused, 0));
        assertEquals(active.colorOnPrimary(), button.getIconTint().getColorForState(focused, 0));
        assertEquals(active.colorOnPrimaryContainer(),
                button.getIconTint().getColorForState(pressedFocused, 0));
    }
    @Test
    public void dialogAndPopupShellsRebindFromTheirOriginalDrawables() {
        ThemeTokens baseline = ThemeTokens.light();
        ThemeTokens first = palette("#234A91", "#D9E5FF", "#D8F0DF", "#D81B60");
        ThemeTokens second = palette("#285C3A", "#D7EEDD", "#F8E4C5", "#7B1FA2");

        try (var activity = Robolectric.buildActivity(Host.class).setup()) {
            Context context = activity.get();
            Dialog dialog = new Dialog(context, R.style.Theme_App);
            dialog.show();
            GradientDrawable dialogSource = shape(baseline.colorSurfaceContainerHigh());
            dialog.getWindow().setBackgroundDrawable(dialogSource);
            ThemeBinder.bindDialog(dialog, baseline, first);
            assertEquals(first.colorSurfaceContainerHigh(),
                    fill(dialog.getWindow().getDecorView().getBackground()));
            ThemeBinder.bindDialog(dialog, baseline, second);
            assertEquals(second.colorSurfaceContainerHigh(),
                    fill(dialog.getWindow().getDecorView().getBackground()));
            ThemeBinder.bindDialog(dialog, baseline, baseline);
            assertEquals(baseline.colorSurfaceContainerHigh(),
                    fill(dialog.getWindow().getDecorView().getBackground()));
            dialog.dismiss();

            FrameLayout popupContent = new FrameLayout(context);
            popupContent.setBackground(new ColorDrawable(baseline.colorSurfaceContainerHigh()));
            PopupWindow popup = new PopupWindow(popupContent, 180, 100, true);
            popup.setBackgroundDrawable(new ColorDrawable(baseline.colorSurfaceContainerHigh()));

            ThemeBinder.bindPopupWindow(popup, baseline, first);
            assertEquals(first.colorSurfaceContainerHigh(), fill(popup.getBackground()));
            assertEquals(first.colorSurfaceContainerHigh(), fill(popupContent.getBackground()));
            ThemeBinder.bindPopupWindow(popup, baseline, second);
            assertEquals(second.colorSurfaceContainerHigh(), fill(popup.getBackground()));
            assertEquals(second.colorSurfaceContainerHigh(), fill(popupContent.getBackground()));
            ThemeBinder.bindPopupWindow(popup, baseline, baseline);
            assertEquals(baseline.colorSurfaceContainerHigh(), fill(popup.getBackground()));
            assertEquals(baseline.colorSurfaceContainerHigh(), fill(popupContent.getBackground()));
        }
    }

    private static ThemeTokens palette(String primary, String primaryContainer,
                                       String secondaryContainer, String focus) {
        ThemeProfile profile = ThemeProfile.defaultProfile();
        profile.light.primary = primary;
        profile.light.primaryContainer = primaryContainer;
        profile.light.secondaryContainer = secondaryContainer;
        profile.light.focus = focus;
        return ThemeResolver.resolve(ThemeMode.LIGHT, ThemeSeed.NONE, 0, 0, profile, null, false);
    }

    private static GradientDrawable shape(int color) {
        GradientDrawable shape = new GradientDrawable();
        shape.setColor(color);
        shape.setCornerRadius(12f);
        shape.setStroke(1, 0xFF74777F);
        return shape;
    }

    private static int text(TextView view, int... states) {
        return view.getTextColors().getColorForState(states, view.getCurrentTextColor());
    }

    private static int fill(Drawable drawable) {
        assertNotNull(drawable);
        if (drawable instanceof GradientDrawable shape) {
            assertNotNull(shape.getColor());
            return shape.getColor().getDefaultColor();
        }
        if (drawable instanceof ColorDrawable color) return color.getColor();
        throw new AssertionError("unexpected drawable " + drawable);
    }

    private static int stroke(Drawable drawable) {
        return Shadow.<ShadowGradientDrawable>extract((GradientDrawable) drawable).getStrokeColor();
    }
}
