package com.fongmi.android.tv.ui.dialog;

import android.view.View;
import android.widget.TextView;

import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.databinding.DialogDisplayBinding;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.utils.ResUtil;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class DisplayDialog extends BaseAlertDialog {

    private static final float DIALOG_WIDTH_LANDSCAPE = 0.42f;
    private static final float DIALOG_WIDTH_PORTRAIT = 0.92f;

    private DialogDisplayBinding binding;
    private Runnable callback;
    private com.fongmi.android.tv.theme.ThemeTokens appliedTokens;
    private Mode mode = Mode.DISPLAY;

    public static void show(FragmentActivity activity, Runnable callback) {
        for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) if (fragment instanceof DisplayDialog) return;
        new DisplayDialog().callback(callback).show(activity.getSupportFragmentManager(), null);
    }

    public static void showPlayerOsd(FragmentActivity activity, Runnable callback) {
        for (Fragment fragment : activity.getSupportFragmentManager().getFragments()) if (fragment instanceof DisplayDialog) return;
        new DisplayDialog().mode(Mode.PLAYER_OSD).callback(callback).show(activity.getSupportFragmentManager(), null);
    }

    private DisplayDialog callback(Runnable callback) {
        this.callback = callback;
        return this;
    }

    private DisplayDialog mode(Mode mode) {
        this.mode = mode;
        return this;
    }

    @Override
    protected ViewBinding getBinding() {
        binding = DialogDisplayBinding.inflate(getLayoutInflater());
        // This panel owns paired fill/text state lists; avoid generic color-role guessing.
        binding.getRoot().setTag("webhtv:ignore");appliedTokens=null;
        return binding;
    }

    @Override
    protected MaterialAlertDialogBuilder getBuilder() {
        return builder().setView(getBinding().getRoot());
    }

    @Override
    protected void initView() {
        if (mode == Mode.PLAYER_OSD) {
            initPlayerOsdView();
            return;
        }
        initDisplayView();
    }

    private void initDisplayView() {
        binding.displayTime.setSelected(PlayerSetting.isDisplayTime());
        binding.displayTraffic.setSelected(PlayerSetting.isDisplayTraffic());
        binding.displaySize.setSelected(PlayerSetting.isDisplaySize());
        binding.displayProgress.setSelected(PlayerSetting.isDisplayProgress());
        binding.displayMini.setSelected(PlayerSetting.isDisplayMini());
        binding.displayTitle.setSelected(PlayerSetting.isDisplayTitle());
    }

    private void initPlayerOsdView() {
        initDisplayView();
        binding.displayParams.setVisibility(View.VISIBLE);
        binding.displayParams.setSelected(PlayerSetting.isOsdDiagnostics());
        binding.displayMediaBadges.setVisibility(View.VISIBLE);
        binding.displayMediaBadges.setSelected(PlayerSetting.isDisplayMediaBadges());
    }

    @Override
    protected void initEvent() {
        if (mode == Mode.PLAYER_OSD) {
            initPlayerOsdEvent();
            return;
        }
        initDisplayEvent();
    }

    private void initDisplayEvent() {
        binding.displayTime.setOnClickListener(v -> setDisplay(binding.displayTime, Display.TIME));
        binding.displayTraffic.setOnClickListener(v -> setDisplay(binding.displayTraffic, Display.TRAFFIC));
        binding.displaySize.setOnClickListener(v -> setDisplay(binding.displaySize, Display.SIZE));
        binding.displayProgress.setOnClickListener(v -> setDisplay(binding.displayProgress, Display.PROGRESS));
        binding.displayMini.setOnClickListener(v -> setDisplay(binding.displayMini, Display.MINI));
        binding.displayTitle.setOnClickListener(v -> setDisplay(binding.displayTitle, Display.TITLE));
    }

    private void initPlayerOsdEvent() {
        initDisplayEvent();
        binding.displayParams.setOnClickListener(v -> {
            boolean selected = !binding.displayParams.isSelected();
            binding.displayParams.setSelected(selected);
            PlayerSetting.putOsdDiagnostics(selected);
            if (callback != null) callback.run();
        });
        binding.displayMediaBadges.setOnClickListener(v -> setDisplay(binding.displayMediaBadges, Display.MEDIA_BADGES));
    }

    private void setDisplay(TextView view, Display display) {
        boolean selected = !view.isSelected();
        view.setSelected(selected);
        switch (display) {
            case TIME:
                PlayerSetting.putDisplayTime(selected);
                break;
            case TRAFFIC:
                PlayerSetting.putDisplayTraffic(selected);
                break;
            case SIZE:
                PlayerSetting.putDisplaySize(selected);
                break;
            case PROGRESS:
                PlayerSetting.putDisplayProgress(selected);
                break;
            case MINI:
                PlayerSetting.putDisplayMini(selected);
                break;
            case TITLE:
                PlayerSetting.putDisplayTitle(selected);
                break;
            case MEDIA_BADGES:
                PlayerSetting.putDisplayMediaBadges(selected);
                break;
        }
        if (callback != null) callback.run();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) getDialog().getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        setWidth(ResUtil.isLand(requireContext()) ? DIALOG_WIDTH_LANDSCAPE : DIALOG_WIDTH_PORTRAIT);
        applyControlTheme();
    }

    @Override public void onResume(){super.onResume();applyControlTheme();}

    private android.graphics.drawable.GradientDrawable optionShape(int fill,int border,int widthDp){
        var shape=new android.graphics.drawable.GradientDrawable();shape.setColor(fill);shape.setCornerRadius(ResUtil.dp2px(8));shape.setStroke(ResUtil.dp2px(widthDp),border);return shape;
    }

    private void applyControlTheme(){
        if(binding==null)return;
        var t=com.fongmi.android.tv.theme.ThemeController.current();if(appliedTokens==t)return;appliedTokens=t;
        var panel=optionShape(t.colorSurfaceContainer(),t.colorOutlineVariant(),1);panel.setCornerRadius(ResUtil.dp2px(22));binding.getRoot().setBackground(panel);
        ((TextView)binding.getRoot().getChildAt(0)).setTextColor(t.colorOnSurface());
        int[][] states={{-android.R.attr.state_enabled},{android.R.attr.state_selected,android.R.attr.state_focused},
                {android.R.attr.state_focused},{android.R.attr.state_selected},{android.R.attr.state_pressed},{}};
        int[] fills={t.colorSurfaceContainer(),t.colorPrimaryContainer(),t.colorSecondaryContainer(),t.colorPrimaryContainer(),t.colorSecondaryContainer(),t.colorSurfaceContainerHigh()};
        int[] borders={t.colorOutlineVariant(),t.colorFocus(),t.colorFocus(),t.colorPrimary(),t.colorFocus(),t.colorOutlineVariant()};
        int[] text={androidx.core.graphics.ColorUtils.setAlphaComponent(t.colorOnSurfaceVariant(),140),t.colorOnPrimaryContainer(),t.colorOnSecondaryContainer(),t.colorOnPrimaryContainer(),t.colorOnSecondaryContainer(),t.colorOnSurface()};
        for(TextView view:new TextView[]{binding.displayTime,binding.displayTraffic,binding.displaySize,binding.displayProgress,binding.displayMini,binding.displayTitle,binding.displayParams,binding.displayMediaBadges}){
            var background=new android.graphics.drawable.StateListDrawable();
            for(int i=0;i<states.length;i++)background.addState(states[i],optionShape(fills[i],borders[i],i==1||i==2||i==4?3:1));
            view.setBackgroundTintList(null);view.setBackground(background);view.setTextColor(new android.content.res.ColorStateList(states,text));
            if(android.os.Build.VERSION.SDK_INT>=26)view.setDefaultFocusHighlightEnabled(false);
        }
    }

    private enum Display {
        TIME, TRAFFIC, SIZE, PROGRESS, MINI, TITLE, MEDIA_BADGES
    }

    private enum Mode {
        DISPLAY, PLAYER_OSD
    }
}
