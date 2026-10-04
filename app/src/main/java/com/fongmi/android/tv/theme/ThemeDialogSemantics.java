package com.fongmi.android.tv.theme;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputLayout;

/**
 * Explicit semantic styling for programmatic dialogs.
 *
 * <p>{@link ThemeBinder} deliberately does not guess the role of arbitrary
 * player/media/dialog colours. Raw dialogs therefore use this small contract at
 * their construction and rebind points instead of relying on a fixed light
 * palette or on a global colour heuristic.
 */
public final class ThemeDialogSemantics {

    private static final int[][] ENABLED_STATES = {
            {-android.R.attr.state_enabled},
            {android.R.attr.state_enabled, android.R.attr.state_pressed},
            {android.R.attr.state_enabled, android.R.attr.state_focused},
            {}
    };

    private ThemeDialogSemantics() {
    }

    public static ThemeTokens tokens() {
        return ThemeController.current();
    }

    /**
     * Resolves the small legacy neutral palette still used by programmatic
     * dialog builders into the current dialog foreground semantic.
     */
    public static int textColor(int legacyColor) {
        int replacement = legacyForeground(legacyColor, tokens());
        return replacement == 0 ? legacyColor : replacement;
    }

    /**
     * Resolves a legacy dialog surface while leaving content/status colours
     * untouched when they are not a known surface semantic.
     */
    public static int surfaceColor(int legacyColor) {
        int replacement = surfaceReplacement(legacyColor, tokens());
        return replacement == 0 ? legacyColor : replacement;
    }

    public static int outlineColor(int legacyColor) {
        int replacement = outlineReplacement(legacyColor, tokens());
        return replacement == 0 ? legacyColor : replacement;
    }

    public static GradientDrawable panel(Context context, int radiusDp) {
        ThemeTokens tokens = tokens();
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(tokens.colorSurfaceContainerHigh());
        drawable.setCornerRadius(dp(context, radiusDp));
        drawable.setStroke(dp(context, 1), tokens.colorOutlineVariant());
        return drawable;
    }

    public static GradientDrawable input(Context context, boolean focused) {
        ThemeTokens tokens = tokens();
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(tokens.colorSurfaceContainerHighest());
        drawable.setCornerRadius(dp(context, 8));
        drawable.setStroke(dp(context, focused ? 2 : 1), focused ? tokens.colorFocus() : tokens.colorOutline());
        return drawable;
    }

    public static GradientDrawable popup(Context context, int radiusDp) {
        ThemeTokens tokens = tokens();
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(tokens.colorSurfaceContainerHigh());
        drawable.setCornerRadius(dp(context, radiusDp));
        drawable.setStroke(dp(context, 1), tokens.colorOutlineVariant());
        return drawable;
    }

    public static void title(TextView view) {
        if (view != null) view.setTextColor(tokens().colorOnSurface());
    }

    public static void body(TextView view) {
        if (view != null) view.setTextColor(tokens().colorOnSurfaceVariant());
    }

    public static void icon(ImageView view) {
        if (view != null) view.setImageTintList(ColorStateList.valueOf(tokens().colorOnSurfaceVariant()));
    }

    public static void primary(MaterialButton button) {
        if (button == null) return;
        ThemeTokens tokens = tokens();
        button.setTextColor(new ColorStateList(ENABLED_STATES, new int[]{
                tokens.colorOnSurfaceVariant(),
                tokens.colorOnPrimary(),
                tokens.colorOnPrimary(),
                tokens.colorOnSurfaceVariant()
        }));
        button.setBackgroundTintList(new ColorStateList(ENABLED_STATES, new int[]{
                tokens.colorSurfaceContainerHighest(),
                tokens.colorPrimary(),
                tokens.colorPrimary(),
                tokens.colorSurfaceContainerHighest()
        }));
        button.setStrokeColor(ColorStateList.valueOf(tokens.colorPrimary()));
        button.setStrokeWidth(0);
    }

    public static void secondary(MaterialButton button) {
        if (button == null) return;
        ThemeTokens tokens = tokens();
        button.setTextColor(new ColorStateList(ENABLED_STATES, new int[]{
                tokens.colorOnSurfaceVariant(),
                tokens.colorPrimary(),
                tokens.colorPrimary(),
                tokens.colorOnSurfaceVariant()
        }));
        button.setBackgroundTintList(new ColorStateList(ENABLED_STATES, new int[]{
                tokens.colorSurfaceContainerHighest(),
                tokens.colorPrimaryContainer(),
                tokens.colorPrimaryContainer(),
                tokens.colorSurfaceContainerHigh()
        }));
        button.setStrokeColor(new ColorStateList(ENABLED_STATES, new int[]{
                tokens.colorOutlineVariant(),
                tokens.colorFocus(),
                tokens.colorFocus(),
                tokens.colorOutline()
        }));
        button.setStrokeWidth(dp(button.getContext(), 1));
    }

    public static void bindTextTree(View root) {
        if (root == null) return;
        if (root instanceof TextView text) body(text);
        if (root instanceof ImageView image) icon(image);
        if (root instanceof android.view.ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) bindTextTree(group.getChildAt(index));
        }
    }

    /**
     * Migrates the small set of legacy neutral colours used by raw dialogs.
     * This is intentionally opt-in at each dialog call site; it is not part of
     * ThemeBinder and never walks ordinary player/media views.
     */
    public static void bindLegacyDialog(View root) {
        if (root == null) return;
        ThemeTokens tokens = tokens();
        if (root.getBackground() instanceof GradientDrawable background) {
            int fill = background.getColor() == null ? 0 : background.getColor().getDefaultColor();
            int replacement = surfaceReplacement(fill, tokens);
            if (replacement != 0) {
                background.setColor(replacement);
                background.setStroke(dp(root.getContext(), 1), tokens.colorOutlineVariant());
            }
        } else if (root.getBackground() instanceof ColorDrawable background) {
            int fill = background.getColor();
            int replacement = surfaceReplacement(fill, tokens);
            if (replacement != 0) background.setColor(replacement);
        }
        if (root instanceof MaterialButton button) {
            int fill = button.getBackgroundTintList() == null ? 0 : button.getBackgroundTintList().getDefaultColor();
            if (isPrimarySurface(fill)) primary(button);
            else if (isNeutralSurface(fill)) secondary(button);
        } else if (root instanceof TextInputLayout input) {
            input.setBoxBackgroundColor(tokens.colorSurfaceContainerHighest());
            input.setHintTextColor(ColorStateList.valueOf(tokens.colorOnSurfaceVariant()));
        } else if (root instanceof TextView text) {
            int color = text.getTextColors() == null ? 0 : text.getTextColors().getDefaultColor();
            int replacement = legacyForeground(color, tokens);
            if (replacement != 0) text.setTextColor(replacement);
            int hint = text.getHintTextColors() == null ? 0 : text.getHintTextColors().getDefaultColor();
            replacement = legacyForeground(hint, tokens);
            if (replacement != 0) text.setHintTextColor(replacement);
        } else if (root instanceof ImageView image) {
            ColorStateList tint = image.getImageTintList();
            if (tint != null) {
                int replacement = legacyForeground(tint.getDefaultColor(), tokens);
                if (replacement != 0) image.setImageTintList(ColorStateList.valueOf(replacement));
            }
        }
        if (root instanceof android.view.ViewGroup group) {
            for (int index = 0; index < group.getChildCount(); index++) bindLegacyDialog(group.getChildAt(index));
        }
    }

    private static boolean isNeutralSurface(int color) {
        return color == Color.WHITE || color == 0xFFF8F9FA || color == 0xFFF8FAFD
                || color == 0xFFF1F3F4 || color == 0xFFF5F6F7 || color == 0xF2FFFFFF;
    }

    private static int surfaceReplacement(int color, ThemeTokens tokens) {
        return switch (color) {
            case Color.WHITE, 0xFFF8F9FA, 0xFFF8FAFD -> tokens.colorSurfaceContainerHigh();
            case 0xFFF1F3F4, 0xFFF5F6F7 -> tokens.colorSurfaceContainerHighest();
            case 0xF2FFFFFF -> tokens.colorSurfaceContainerHigh();
            case 0xFF1A73E8, 0xFF0B57D0, 0xFF174EA6 -> tokens.colorPrimary();
            case 0xFFE8F0FE, 0xFFD2E3FC -> tokens.colorPrimaryContainer();
            case 0xFFFCE8E6 -> tokens.colorErrorContainer();
            case 0xFFE6F4EA -> tokens.colorSuccessContainer();
            default -> 0;
        };
    }

    private static int outlineReplacement(int color, ThemeTokens tokens) {
        return switch (color) {
            case 0xFFDADCE0, 0xFFE0E3E7, 0xFFE2E5E8, 0xFFC8CDD2 -> tokens.colorOutlineVariant();
            case 0xFFD2E3FC -> tokens.colorPrimary();
            case 0xFFCEEAD6 -> tokens.colorSuccess();
            case 0xFFF2B8B5 -> tokens.colorError();
            default -> 0;
        };
    }

    private static boolean isPrimarySurface(int color) {
        return color == 0xFF0B57D0 || color == 0xFF1A73E8 || color == 0xFF174EA6;
    }

    private static int legacyForeground(int color, ThemeTokens tokens) {
        return switch (color) {
            case 0xFF000000, 0xFF202124, 0xFF3C4043 -> tokens.colorOnSurface();
            case 0xFF5F6368, 0xFF666666, 0xFF6F7782, 0xFF9AA0A6 -> tokens.colorOnSurfaceVariant();
            case 0xFF0B57D0, 0xFF174EA6, 0xFF1A73E8 -> tokens.colorPrimary();
            case 0xFF137333 -> tokens.colorSuccess();
            case 0xFFB3261E -> tokens.colorError();
            case 0xFF8A4B00, 0xFFB06000 -> tokens.colorWarning();
            default -> 0;
        };
    }

    private static int dp(Context context, int value) {
        return Math.max(1, Math.round(value * context.getResources().getDisplayMetrics().density));
    }
}
