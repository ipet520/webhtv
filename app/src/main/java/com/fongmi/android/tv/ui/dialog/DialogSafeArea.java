package com.fongmi.android.tv.ui.dialog;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.view.View;
import android.view.Window;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.fongmi.android.tv.utils.ResUtil;

import java.util.WeakHashMap;

/** Resolves the area a floating dialog can use without covering system bars. */
final class DialogSafeArea {

    private static final WeakHashMap<View, ThemeDialogLayout.Insets> OBSERVED = new WeakHashMap<>();

    private DialogSafeArea() {
    }

    static ThemeDialogLayout.Insets of(WindowInsetsCompat insets) {
        Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout());
        Insets gestures = insets.getInsets(WindowInsetsCompat.Type.systemGestures());
        // Some OEM gesture-navigation builds under-report systemBars().bottom. Only the bottom
        // gesture inset is used so left/right gesture regions do not needlessly narrow the dialog.
        return new ThemeDialogLayout.Insets(
                bars.left, bars.top, bars.right, Math.max(bars.bottom, gestures.bottom));
    }

    static ThemeDialogLayout.Area area(Activity activity, Window window) {
        ThemeDialogLayout.Insets insets = window == null ? null : OBSERVED.get(window.getDecorView());
        if (insets == null || insets.isEmpty()) insets = hostInsets(activity);
        int displayWidth = ResUtil.getScreenWidth(activity);
        int displayHeight = ResUtil.getScreenHeight(activity);
        if (insets.isEmpty()) {
            insets = ThemeDialogLayout.Insets.ofContent(
                    displayWidth, displayHeight, contentWidth(activity), contentHeight(activity));
        }
        return ThemeDialogLayout.safeArea(displayWidth, displayHeight, insets);
    }

    static ThemeDialogLayout.Area area(Activity activity) {
        return area(activity, null);
    }

    static int width(Activity activity, Window window) {
        return Math.max(1, area(activity, window).width());
    }

    static int height(Activity activity, Window window) {
        return Math.max(1, area(activity, window).height());
    }

    static int width(Context context) {
        Activity activity = activityOf(context);
        return activity == null ? ResUtil.getScreenWidth(context) : width(activity, null);
    }

    static int height(Context context) {
        Activity activity = activityOf(context);
        return activity == null ? ResUtil.getScreenHeight(context) : height(activity, null);
    }

    static void follow(Activity activity, Window window, Runnable resize) {
        if (activity == null || window == null) return;
        View decorView = window.getDecorView();
        ViewCompat.setOnApplyWindowInsetsListener(decorView, (view, insets) -> {
            ThemeDialogLayout.Insets next = of(insets);
            ThemeDialogLayout.Insets cached = OBSERVED.get(view);
            if (!next.equals(cached)) {
                OBSERVED.put(view, next);
                resize.run();
            }
            return insets;
        });
        ViewCompat.requestApplyInsets(decorView);
    }

    private static ThemeDialogLayout.Insets hostInsets(Activity activity) {
        if (activity == null) return ThemeDialogLayout.Insets.none();
        try {
            WindowInsetsCompat insets = ViewCompat.getRootWindowInsets(activity.getWindow().getDecorView());
            return insets == null ? ThemeDialogLayout.Insets.none() : of(insets);
        } catch (RuntimeException error) {
            return ThemeDialogLayout.Insets.none();
        }
    }

    private static int contentWidth(Activity activity) {
        View content = activity == null ? null : activity.findViewById(android.R.id.content);
        return content == null ? 0 : content.getWidth();
    }

    private static int contentHeight(Activity activity) {
        View content = activity == null ? null : activity.findViewById(android.R.id.content);
        return content == null ? 0 : content.getHeight();
    }

    private static Activity activityOf(Context context) {
        Context current = context;
        while (current instanceof ContextWrapper) {
            if (current instanceof Activity activity) return activity;
            current = ((ContextWrapper) current).getBaseContext();
        }
        return current instanceof Activity activity ? activity : null;
    }
}
