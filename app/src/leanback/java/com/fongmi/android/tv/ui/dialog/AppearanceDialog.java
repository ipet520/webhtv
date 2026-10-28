package com.fongmi.android.tv.ui.dialog;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.theme.AppearanceRowTheme;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.theme.ThemeDialogSemantics;
import com.fongmi.android.tv.theme.ThemeEditorUi;
import com.fongmi.android.tv.theme.ThemeProfile;
import com.fongmi.android.tv.theme.ThemeProfileStore;
import com.fongmi.android.tv.theme.ThemeTokens;
import com.fongmi.android.tv.utils.ResUtil;
import com.fongmi.android.tv.utils.Util;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textview.MaterialTextView;

import java.util.ArrayList;
import java.util.List;

public final class AppearanceDialog extends DialogFragment {

    private String[] uiScales;
    private String[] languages;
    private String[] imageSizes;
    private final List<Row> rows = new ArrayList<>();
    private MaterialTextView uiScaleValue;
    private MaterialTextView themeModeValue;
    private MaterialTextView themeValue;
    private MaterialTextView imageSizeValue;
    private MaterialTextView languageValue;

    /** One dialog row, kept so a theme change can re-colour it without a rebuild. */
    private record Row(LinearLayout root, MaterialTextView title, MaterialTextView summary) {
    }

    public static void show(Fragment fragment) {
        new AppearanceDialog().show(fragment.getChildFragmentManager(), AppearanceDialog.class.getSimpleName());
    }

    public static void show(FragmentActivity activity) {
        new AppearanceDialog().show(activity.getSupportFragmentManager(), AppearanceDialog.class.getSimpleName());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        uiScales = ResUtil.getStringArray(R.array.select_ui_scale);
        languages = ResUtil.getStringArray(R.array.select_language);
        imageSizes = ResUtil.getStringArray(R.array.select_size);
        return LightDialog.create(requireContext(), getString(R.string.setting_appearance), createContent(), getString(R.string.dialog_close), null, null, null);
    }

    @Override
    public void onStart() {
        super.onStart();
        applyRowTheme();
        if (!Util.isLeanback()) return;
        Dialog dialog = getDialog();
        if (dialog == null || dialog.getWindow() == null) return;
        MaterialButton close = findCloseButton(dialog.getWindow().getDecorView(), getString(R.string.dialog_close));
        if (close == null) return;
        ChoiceDialog.applyDohCancelColors(close);
        applyDohCancelStroke(close);
    }

    private void applyDohCancelStroke(MaterialButton button) {
        ThemeTokens tokens = ThemeDialogSemantics.tokens();
        button.setStrokeColor(new ColorStateList(new int[][]{
                {-android.R.attr.state_enabled},
                {android.R.attr.state_focused},
                {android.R.attr.state_pressed},
                {}
        }, new int[]{
                tokens.colorOutlineVariant(),
                tokens.colorPrimary(),
                tokens.colorPrimary(),
                tokens.colorOutline()
        }));
        button.setStrokeWidth(dp(1));
    }

    private MaterialButton findCloseButton(View view, CharSequence label) {
        if (view instanceof MaterialButton button && TextUtils.equals(button.getText(), label)) return button;
        if (view instanceof ViewGroup group) {
            for (int i = 0; i < group.getChildCount(); i++) {
                MaterialButton button = findCloseButton(group.getChildAt(i), label);
                if (button != null) return button;
            }
        }
        return null;
    }

    private View createContent() {
        LinearLayout content = new LinearLayout(requireContext());
        content.setOrientation(LinearLayout.VERTICAL);
        uiScaleValue = addRow(content, R.string.setting_ui_scale, uiScales[Setting.getUiScaleIndex()], this::chooseUiScale);
        themeModeValue = addRow(content, R.string.setting_theme_mode, getThemeModeText(), this::chooseThemeMode);
        themeValue = addRow(content, R.string.setting_theme_color, getThemeText(), view -> ThemeDialog.show(this));
        imageSizeValue = addRow(content, R.string.setting_size, imageSizes[PlayerSetting.getSize()], this::chooseImageSize);
        languageValue = addRow(content, R.string.setting_language, languages[Setting.getLanguageIndex()], this::chooseLanguage);
        if (Setting.isLeanbackHomeStyleAvailable()) {
            addRow(content, R.string.setting_native_home_style, "", this::chooseHomeStyle).setVisibility(View.GONE);
        }
        return content;
    }

    private MaterialTextView addRow(LinearLayout content, int titleRes, String value, View.OnClickListener listener) {
        LinearLayout row = new LinearLayout(requireContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setClickable(true);
        row.setFocusable(true);
        row.setPadding(dp(16), 0, dp(16), 0);
        row.setOnClickListener(listener);

        MaterialTextView title = new MaterialTextView(requireContext());
        title.setText(titleRes);
        title.setTextSize(15);
        title.setSingleLine(true);
        title.setDuplicateParentStateEnabled(true);
        row.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

        MaterialTextView summary = new MaterialTextView(requireContext());
        summary.setText(value);
        summary.setTextSize(14);
        summary.setGravity(Gravity.END);
        summary.setSingleLine(true);
        summary.setEllipsize(TextUtils.TruncateAt.END);
        summary.setDuplicateParentStateEnabled(true);
        LinearLayout.LayoutParams summaryParams = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
        summaryParams.leftMargin = dp(12);
        row.addView(summary, summaryParams);

        // Row fill, stroke and both text colours must come from one palette. The row used
        // to keep the fixed light selector_git_cloud_card while its text already followed
        // ThemeController.current(), which on the TV dark table measured 1.16:1 / 1.36:1.
        AppearanceRowTheme.apply(row, title, summary, ThemeController.current());
        rows.add(new Row(row, title, summary));

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58));
        rowParams.bottomMargin = dp(8);
        content.addView(row, rowParams);
        return summary;
    }

    /**
     * Re-colours the rows with the active palette.
     *
     * <p>Called when the dialog is shown and again after a theme change, so an open
     * dialog follows a new profile without being rebuilt.
     */
    private void applyRowTheme() {
        for (Row row : rows) AppearanceRowTheme.apply(row.root(), row.title(), row.summary(), ThemeController.current());
    }

    private void chooseUiScale(View view) {
        ChoiceDialog.showSingle(this, R.string.setting_ui_scale, uiScales, Setting.getUiScaleIndex(), which -> {
            if (which == Setting.getUiScaleIndex()) return;
            uiScaleValue.setText(uiScales[which]);
            Setting.putUiScaleIndex(which);
            dismissAllowingStateLoss();
            requireActivity().recreate();
        });
    }

    private void chooseThemeMode(View view) {
        String[] modes = {getString(R.string.setting_theme_mode_system), getString(R.string.setting_theme_mode_light), getString(R.string.setting_theme_mode_dark)};
        int current = switch (Setting.getThemeMode()) {
            case 0 -> 1;
            case 1 -> 2;
            default -> 0;
        };
        ChoiceDialog.showSingle(this, R.string.setting_theme_mode, modes, current, which -> {
            int mode = which == 0 ? -1 : which - 1;
            if (mode == Setting.getThemeMode()) return;
            Setting.putThemeMode(mode);
            themeModeValue.setText(getThemeModeText());
            ThemeController.applyNightModeToApp();
            dismissAllowingStateLoss();
            RefreshEvent.theme();
        });
    }

    private String getThemeModeText() {
        return switch (Setting.getThemeMode()) {
            case 0 -> getString(R.string.setting_theme_mode_light);
            case 1 -> getString(R.string.setting_theme_mode_dark);
            default -> getString(R.string.setting_theme_mode_system);
        };
    }

    private void chooseImageSize(View view) {
        ChoiceDialog.showSingle(this, R.string.setting_size, imageSizes, PlayerSetting.getSize(), which -> {
            imageSizeValue.setText(imageSizes[which]);
            PlayerSetting.putSize(which);
            RefreshEvent.size();
        });
    }

    private void chooseLanguage(View view) {
        ChoiceDialog.showSingle(this, R.string.setting_language, languages, Setting.getLanguageIndex(), which -> {
            if (which == Setting.getLanguageIndex()) return;
            languageValue.setText(languages[which]);
            Setting.putLanguageIndex(which);
            dismissAllowingStateLoss();
            RefreshEvent.language();
        });
    }

    private void chooseHomeStyle(View view) {
        String[] modes = {getString(R.string.native_home_classic), getString(R.string.native_home_immersive)};
        ChoiceDialog.showSingle(this, R.string.setting_native_home_style, modes,
                Setting.getLeanbackHomeMode() == Setting.LeanbackHomeMode.IMMERSIVE ? 1 : 0,
                which -> Setting.putLeanbackHomeMode(which == 1
                        ? Setting.LeanbackHomeMode.IMMERSIVE : Setting.LeanbackHomeMode.CLASSIC));
    }

    void onThemeProfileApplied() {
        themeValue.setText(getThemeText());
        applyRowTheme();
        dismissAllowingStateLoss();
        RefreshEvent.theme();
    }

    private String getThemeText() {
        ThemeProfile profile = ThemeProfileStore.load();
        String name = profile.displayName();
        return "Default".equals(name) ? themeText(Setting.getThemeColor()) : name;
    }

    private String themeText(int color) {
        if (color == -1) return getString(R.string.setting_off);
        return getString(color == 0 ? R.string.setting_auto : R.string.setting_custom);
    }

    private int dp(int value) {
        return ResUtil.dp2px(value);
    }
}
