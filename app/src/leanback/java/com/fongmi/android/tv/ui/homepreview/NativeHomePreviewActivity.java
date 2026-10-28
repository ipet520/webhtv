package com.fongmi.android.tv.ui.homepreview;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.theme.ThemeController;

/** Explicit Debug-only entry, declared only by complete Leanback Debug variant manifests. */
public final class NativeHomePreviewActivity extends AppCompatActivity {
    public static final String EXTRA_ENABLED = "native_home_preview";
    private NativeHomeBrowseView preview;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ThemeController.applyFromPreferences(this);
        if (!BuildConfig.DEBUG || !getIntent().getBooleanExtra(EXTRA_ENABLED, false)) {
            leavePreview();
            return;
        }
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        int cards = Math.max(0, Math.min(40, getIntent().getIntExtra("preview_cards", 18)));
        int functions = Math.max(0, Math.min(24, getIntent().getIntExtra("preview_functions", 6)));
        preview = new NativeHomeBrowseView(this, cards, functions, this::leavePreview);
        android.widget.FrameLayout root=new android.widget.FrameLayout(this);
        com.fongmi.android.tv.ui.custom.CustomWallView wall=new com.fongmi.android.tv.ui.custom.CustomWallView(this,null);
        wall.setMotionEnabled(false);
        root.addView(wall,new android.widget.FrameLayout.LayoutParams(-1,-1));
        preview.setBackgroundHost(wall);
        root.addView(preview,new android.widget.FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        preview.restorePreviewState(savedInstanceState);
    }

    private void leavePreview() {
        // Existing task: finish back to the exact prior page, including a WebHome session.
        // Cold explicit launch: create the unchanged old home only when leaving the preview.
        if (isTaskRoot()) startActivity(new Intent(this, HomeActivity.class)
                .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP));
        finish();
    }

    @Override public void onBackPressed() {
        if (preview == null || !preview.handleBack()) leavePreview();
    }

    @Override protected void onResume() {
        super.onResume();
        ThemeController.applyFromPreferences(this);
        if (preview != null) preview.refreshTheme();
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        if (preview != null) preview.savePreviewState(state);
        super.onSaveInstanceState(state);
    }

    @Override protected void onDestroy() {
        if (preview != null) preview.release();
        super.onDestroy();
    }
}
