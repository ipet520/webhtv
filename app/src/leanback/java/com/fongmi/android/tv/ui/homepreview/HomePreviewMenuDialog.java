package com.fongmi.android.tv.ui.homepreview;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.theme.ThemeTokens;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** Source drawer only. Secondary filters live inside NativeHomeBrowseView. */
final class HomePreviewMenuDialog extends Dialog {
    private final List<Button> options = new ArrayList<>();
    private final ScrollView scroller;

    HomePreviewMenuDialog(Context context, String[] labels, IntSupplier selected,
                          IntConsumer apply, View returnTarget) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(true);
        scroller = new ScrollView(context);
        scroller.setFocusable(false);
        scroller.setVerticalScrollBarEnabled(false);

        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(20));
        scroller.addView(content, new ScrollView.LayoutParams(-1, -2));
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button option = new Button(context);
            option.setId(View.generateViewId());
            option.setText(labels[i]);
            option.setTextSize(15);
            option.setAllCaps(false);
            option.setMinHeight(0);
            option.setMinimumHeight(0);
            HomePreviewStyles.control(option);
            option.setSelected(i == selected.getAsInt());
            option.setOnClickListener(v -> { apply.accept(index); dismiss(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(40));
            lp.bottomMargin = dp(9);
            content.addView(option, lp);
            options.add(option);
        }
        for (int i = 0; i < options.size(); i++) {
            Button option = options.get(i);
            option.setNextFocusUpId(options.get(Math.max(0, i - 1)).getId());
            option.setNextFocusDownId(options.get(Math.min(options.size() - 1, i + 1)).getId());
            option.setNextFocusLeftId(option.getId());
            option.setNextFocusRightId(option.getId());
        }
        setContentView(scroller);
        // Configure before WindowManager can draw the first frame. onShow is too late:
        // a selected MATCH_PARENT option otherwise inherits the theme's initial width.
        prepareWindow();
        setOnShowListener(dialog -> {
            if(!options.isEmpty()) options.get(Math.max(0, Math.min(options.size() - 1, selected.getAsInt()))).requestFocus();
        });
        setOnDismissListener(dialog -> {
            if (returnTarget.isAttachedToWindow() && returnTarget.isShown() && !returnTarget.hasFocus()) returnTarget.requestFocus();
        });
    }

    private void prepareWindow() {
        Window window = getWindow();
        if (window == null) return;
        int width = getContext().getResources().getDisplayMetrics().widthPixels;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setGravity(Gravity.END | Gravity.TOP);
        window.setLayout(Math.min(Math.round(width * .22f), Math.max(dp(180), Math.round(width * .20f))), -1);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        refreshTheme();
    }

    @Override public void show() {
        prepareWindow();
        super.show();
    }

    void refreshTheme() {
        ThemeTokens t = ThemeController.current();
        scroller.setBackgroundColor(HomePreviewStyles.alpha(t.colorSurfaceContainer(), t.dialogOpacity()));
        for (Button option : options) {
            android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
            GradientDrawable selectedFocus = HomePreviewStyles.shape(option, t.colorPrimaryContainer(), t.colorFocus(), 24);
            selectedFocus.setStroke(dp(2), t.colorFocus());
            states.addState(new int[]{android.R.attr.state_selected, android.R.attr.state_focused}, selectedFocus);
            states.addState(new int[]{android.R.attr.state_selected}, HomePreviewStyles.shape(option, t.colorPrimary(), Color.TRANSPARENT, 24));
            states.addState(new int[]{android.R.attr.state_focused}, HomePreviewStyles.shape(option, t.colorSurfaceContainerHigh(), t.colorFocus(), 24));
            states.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
            option.setBackground(states);
            option.setPadding(dp(12),option.getPaddingTop(),dp(12),option.getPaddingBottom());
            option.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_selected, android.R.attr.state_focused}, {android.R.attr.state_selected}, {}},
                    new int[]{t.colorOnPrimaryContainer(), t.colorOnPrimary(), t.colorOnSurface()}));
        }
        // Theme scrim is a color token, not a window opacity policy. Never black out the home.
        if (getWindow() != null) getWindow().setDimAmount(Math.max(.10f, Math.min(.28f, Color.alpha(t.colorScrim()) / 255f * .28f)));
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int index = options.indexOf(getCurrentFocus());
        int key = event.getKeyCode();
        if (index >= 0 && ((key == KeyEvent.KEYCODE_DPAD_UP && index == 0)
                || (key == KeyEvent.KEYCODE_DPAD_DOWN && index == options.size() - 1)
                || key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT)) return true;
        if ((key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) && event.getRepeatCount() > 0) return true;
        return super.dispatchKeyEvent(event);
    }

    private int dp(float value) { return HomePreviewPosterAdapter.dp(getContext(), value); }
}

