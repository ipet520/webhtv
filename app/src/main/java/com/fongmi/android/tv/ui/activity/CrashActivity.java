package com.fongmi.android.tv.ui.activity;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.WindowManager;

import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityCrashBinding;
import com.fongmi.android.tv.server.Server;
import com.fongmi.android.tv.theme.WebHtvAlertDialogBuilder;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.utils.AppVersion;
import com.fongmi.android.tv.utils.CrashLogSession;
import com.fongmi.android.tv.utils.CrashLogText;
import com.fongmi.android.tv.utils.CrashRestartMode;
import com.fongmi.android.tv.utils.Notify;
import com.fongmi.android.tv.utils.Task;
import com.github.catvod.utils.Util;

import java.util.Objects;

import cat.ereza.customactivityoncrash.CustomActivityOnCrash;

public class CrashActivity extends BaseActivity {

    private ActivityCrashBinding mBinding;
    private final Object logServiceLock = new Object();
    private boolean logServiceClosed;
    private String logUrl;
    private String localUrl;

    @Override
    protected boolean customWall() {
        return false;
    }

    @Override
    protected ViewBinding getBinding() {
        return mBinding = ActivityCrashBinding.inflate(getLayoutInflater());
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    @Override
    protected void initEvent() {
        mBinding.details.setOnClickListener(v -> showError());
        mBinding.restart.setOnClickListener(v -> restartApplication());
        mBinding.silentRestart.setOnClickListener(v -> {
            CrashRestartMode.arm();
            restartApplication();
        });
        mBinding.copyLogAddress.setOnClickListener(v -> copyLogAddress());
        mBinding.openLog.setOnClickListener(v -> openLog());
        mBinding.logAddress.setText(R.string.crash_log_preparing);
        setLogActionsEnabled(false);
        Task.execute(this::startLogService);
    }

    @Override
    protected void onBackInvoked() {
        // Keep the crash report available until the user restarts the app or exits the task.
    }

    @Override
    protected void onDestroy() {
        if (isFinishing()) {
            synchronized (logServiceLock) {
                logServiceClosed = true;
                try {
                    Server.get().shutdown();
                } catch (Throwable ignored) {
                }
            }
        }
        super.onDestroy();
    }

    private void startLogService() {
        try {
            CrashLogSession.prepare(this);
        } catch (Throwable ignored) {
            // The crash screen remains usable if evidence collection fails.
        }
        boolean running;
        synchronized (logServiceLock) {
            if (logServiceClosed || isFinishing() || isDestroyed()) return;
            try {
                Server.get().start();
                running = Server.get().isRunning();
            } catch (Throwable ignored) {
                running = false;
            }
        }
        if (!running) {
            post(this::showLogFailure);
            return;
        }
        String host;
        try {
            host = Util.getIp();
        } catch (Throwable ignored) {
            host = "";
        }
        String lan = Server.get().getAddress(false) + "/debug/logs";
        String local = Server.get().getAddress("/debug/logs");
        String lanHost = host;
        post(() -> bindLogAddress(lanHost, lan, local));
    }

    private void bindLogAddress(String host, String lan, String local) {
        localUrl = local;
        if (host == null || host.isEmpty()) {
            logUrl = local;
            mBinding.logAddress.setText(getString(R.string.crash_log_no_lan, CrashLogText.wrapUrlForDisplay(local)));
        } else {
            logUrl = lan;
            mBinding.logAddress.setText(CrashLogText.wrapUrlForDisplay(lan));
        }
        mBinding.logHint.setText(R.string.crash_log_hint);
        setLogActionsEnabled(true);
    }

    private void showLogFailure() {
        logUrl = null;
        localUrl = null;
        mBinding.logAddress.setText(R.string.crash_log_service_failed);
        mBinding.logHint.setText(R.string.crash_log_unavailable);
        setLogActionsEnabled(false);
    }

    private void setLogActionsEnabled(boolean enabled) {
        mBinding.copyLogAddress.setEnabled(enabled);
        mBinding.openLog.setEnabled(enabled);
    }

    private void copyLogAddress() {
        if (logUrl == null) return;
        ClipboardManager manager = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        if (manager == null) return;
        manager.setPrimaryClip(ClipData.newPlainText(getString(R.string.crash_log_title), logUrl));
        Notify.show(R.string.crash_log_copied);
    }

    private void openLog() {
        if (localUrl == null) return;
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(localUrl)));
        } catch (RuntimeException error) {
            Notify.show(R.string.debug_log_no_browser);
        }
    }

    private void post(Runnable action) {
        runOnUiThread(() -> {
            if (isFinishing() || isDestroyed()) return;
            try {
                action.run();
            } catch (Throwable ignored) {
            }
        });
    }

    private void restartApplication() {
        CustomActivityOnCrash.restartApplication(this, Objects.requireNonNull(CustomActivityOnCrash.getConfigFromIntent(getIntent())));
    }

    private void showError() {
        new WebHtvAlertDialogBuilder(this)
                .setTitle(R.string.crash_details_title)
                .setMessage(getString(R.string.crash_details_message,
                        AppVersion.fullName(),
                        CustomActivityOnCrash.getAllErrorDetailsFromIntent(this, getIntent())))
                .setPositiveButton(R.string.crash_details_close, null)
                .show();
    }
}
