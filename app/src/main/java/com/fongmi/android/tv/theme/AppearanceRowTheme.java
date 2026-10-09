package com.fongmi.android.tv.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.TextView;

/**
 * Row rendering for the “Appearance &amp; language” dialog.
 *
 * <p>Why this exists (measured defect, not a style preference): both
 * {@code AppearanceDialog} flavours used to paint every row with the fixed light
 * {@code selector_git_cloud_card} ({@code #F8F9FA} fill, {@code #DADCE0} stroke,
 * {@code #E8F0FE}/{@code #0B57D0} focus) while the row text already came from
 * {@link ThemeController#current()}. The TV flavour compiles the dark token table
 * ({@code values/webhtv_tokens.xml} is overridden by the dark table and the flavour
 * ships no light table), so on TV the label rendered {@code #E2E2E9} and the value
 * {@code #C4C6D0} on a {@code #F8F9FA} card. Measured on device
 * ({@code 192.168.50.3:5559}, 1920x1080): label 1.16:1 and value 1.36:1 against the
 * row background - i.e. the reported "text is unreadable". The same fixed card also
 * ignored any custom theme profile.
 *
 * <p>The row therefore has to follow the same palette as its own text. It cannot be a
 * static {@code ?attr/color*} drawable: {@code ?attr/color*} is a compile-time
 * resource, Android exposes no public API to rewrite it at runtime, and the whole
 * point of the profile feature is that a user override must reach this dialog. So the
 * fill/stroke are built here from {@link ThemeController#current()} - the same single
 * source of truth the row text already used - and {@link ThemeController#bindTheme}
 * then re-colours the already-attached rows whenever the user changes the profile.
 *
 * <p>The non-focused fill is {@code colorSurfaceContainerHigh}, the role
 * {@code shape_shell_proxy_dialog} gives the dialog panel. That is deliberate: a row
 * that reuses the panel colour reads as an inset list inside the panel (the Material 3
 * "surface container" pattern), and on the shipped palettes it clears WCAG AA for both
 * row texts by a wide margin - dark 9.05:1 / 6.85:1, light 13.23:1 / 7.19:1.
 */
public final class AppearanceRowTheme {

    private static final int CORNER_DP = 6;

    private AppearanceRowTheme() {
    }

    /**
     * The row background for the current palette. This preserves the established
     * Appearance row state contract: focused fill is PrimaryContainer with a 1dp
     * Primary outline; normal fill is SurfaceContainerHigh with OutlineVariant.
     */
    public static Drawable background(Context context, ThemeTokens tokens) {
        ThemeTokens safe = tokens == null ? ThemeTokens.dark() : tokens;
        StateListDrawable states = new StateListDrawable();
        states.addState(new int[]{android.R.attr.state_focused},
                shape(context, safe.colorPrimaryContainer(), safe.colorPrimary(), 1));
        states.addState(new int[]{},
                shape(context, safe.colorSurfaceContainerHigh(), safe.colorOutlineVariant(), 1));
        return states;
    }

    /**
     * Applies the palette to one row: background plus the two text colours the row owns.
     *
     * <p>{@code title} and {@code summary} may be {@code null} so the caller can colour a
     * partially built row.
     */
    public static void apply(View row, TextView title, TextView summary, ThemeTokens tokens) {
        ThemeTokens safe = tokens == null ? ThemeTokens.dark() : tokens;
        if (row != null) row.setBackground(background(row.getContext(), safe));
        int[][] states = {
                {android.R.attr.state_focused},
                {}
        };
        if (title != null) {
            title.setDuplicateParentStateEnabled(true);
            title.setTextColor(new ColorStateList(states,
                    new int[]{safe.colorOnPrimaryContainer(), safe.colorOnSurface()}));
        }
        if (summary != null) {
            summary.setDuplicateParentStateEnabled(true);
            summary.setTextColor(new ColorStateList(states,
                    new int[]{safe.colorOnPrimaryContainer(), safe.colorOnSurfaceVariant()}));
        }
    }

    private static GradientDrawable shape(Context context, int fill, int stroke, int strokeDp) {
        float density = context.getResources().getDisplayMetrics().density;
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(fill);
        drawable.setCornerRadius(CORNER_DP * density);
        if (strokeDp > 0) drawable.setStroke(Math.max(1, Math.round(strokeDp * density)), stroke);
        return drawable;
    }
}
