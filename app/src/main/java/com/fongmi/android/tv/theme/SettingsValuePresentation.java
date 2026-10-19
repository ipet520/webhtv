package com.fongmi.android.tv.theme;

import android.graphics.drawable.GradientDrawable;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.R;

/** Keeps transient setting-value errors readable without changing the normal wallpaper text style. */
public final class SettingsValuePresentation {

    private SettingsValuePresentation() {
    }

    public static void error(TextView view, ThemeTokens tokens) {
        if (view == null || tokens == null) return;
        view.setTextColor(tokens.colorOnErrorContainer());
        GradientDrawable background = ThemeEditorUi.shape(
                view.getContext(), tokens.colorErrorContainer(), 0, 0, 6);
        view.setBackground(background);
    }

    /** Writes a successful value and restores the existing wallpaper foreground and transparent backing. */
    public static void summary(TextView view, CharSequence text) {
        if (view == null) return;
        view.setText(text);
        view.setTextColor(ContextCompat.getColor(view.getContext(), R.color.webhtv_on_wallpaper));
        view.setBackground(null);
    }
}
