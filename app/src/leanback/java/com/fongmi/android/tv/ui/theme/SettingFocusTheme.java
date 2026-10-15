package com.fongmi.android.tv.ui.theme;

import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;

import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.theme.ThemeTokens;
import com.fongmi.android.tv.ui.activity.SettingActivity;
import com.fongmi.android.tv.ui.activity.SettingAdActivity;
import com.fongmi.android.tv.ui.activity.SettingAiActivity;
import com.fongmi.android.tv.ui.activity.SettingAppearanceActivity;
import com.fongmi.android.tv.ui.activity.SettingDanmakuActivity;
import com.fongmi.android.tv.ui.activity.SettingEnhanceActivity;
import com.fongmi.android.tv.ui.activity.SettingPersonalActivity;
import com.fongmi.android.tv.ui.activity.SettingPlayerActivity;
import com.fongmi.android.tv.ui.activity.SettingSubtitleActivity;
import com.fongmi.android.tv.ui.activity.SettingTmdbActivity;

/** Runtime Focus binding for the shared selector used by Leanback settings rows. */
public final class SettingFocusTheme {

    private static final float ITEM_CORNER_RADIUS_DP = 4f;
    private static final float ITEM_FOCUS_STROKE_WIDTH_DP = 1.5f;

    private SettingFocusTheme() {
    }

    /** Applies the current semantic Focus colour only to the standard settings screens. */
    public static boolean bind(Class<?> activityClass, View root, ThemeTokens tokens) {
        if (!isSettingsActivity(activityClass) || root == null || tokens == null) return false;
        return bind(root, tokens.colorFocus());
    }

    static boolean isSettingsActivity(Class<?> activityClass) {
        return activityClass == SettingActivity.class
                || activityClass == SettingAdActivity.class
                || activityClass == SettingAiActivity.class
                || activityClass == SettingAppearanceActivity.class
                || activityClass == SettingDanmakuActivity.class
                || activityClass == SettingEnhanceActivity.class
                || activityClass == SettingPersonalActivity.class
                || activityClass == SettingPlayerActivity.class
                || activityClass == SettingSubtitleActivity.class
                || activityClass == SettingTmdbActivity.class;
    }

    private static boolean bind(View view, int focusColor) {
        boolean changed = false;
        Drawable background = view.getBackground();
        if (background instanceof StateListDrawable selector) changed |= bindSelectorItem(view, selector, focusColor);
        if (view instanceof ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) {
                changed |= bind(group.getChildAt(index), focusColor);
            }
        }
        return changed;
    }

    private static boolean bindSelectorItem(View view, StateListDrawable selector, int focusColor) {
        if (!isSettingsItemSelector(view, selector)) return false;
        selector.mutate();
        GradientDrawable focused = (GradientDrawable) selector.getStateDrawable(0);
        focused.mutate();
        int strokeWidth = Math.max(1, Math.round(TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, ITEM_FOCUS_STROKE_WIDTH_DP,
                view.getResources().getDisplayMetrics())));
        focused.setStroke(strokeWidth, focusColor);
        view.invalidate();
        return true;
    }

    static boolean isSettingsItemSelector(View view, StateListDrawable selector) {
        if (selector.getStateCount() != 3
                || !hasOnlyState(selector.getStateSet(0), android.R.attr.state_focused)
                || !hasOnlyState(selector.getStateSet(1), android.R.attr.state_selected)
                || !isDefaultState(selector.getStateSet(2))) return false;
        Drawable drawable = selector.getStateDrawable(0);
        if (!(drawable instanceof GradientDrawable focused)
                || focused.getShape() != GradientDrawable.RECTANGLE) return false;
        ColorStateList fill = focused.getColor();
        if (fill == null || fill.getDefaultColor() != ContextCompat.getColor(view.getContext(), R.color.black_40)) return false;
        float expectedRadius = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                ITEM_CORNER_RADIUS_DP, view.getResources().getDisplayMetrics());
        return Math.abs(focused.getCornerRadius() - expectedRadius) <= 0.5f;
    }

    private static boolean hasOnlyState(int[] states, int expected) {
        return states != null && states.length == 1 && states[0] == expected;
    }

    private static boolean isDefaultState(int[] states) {
        return states == null || states.length == 0;
    }
}