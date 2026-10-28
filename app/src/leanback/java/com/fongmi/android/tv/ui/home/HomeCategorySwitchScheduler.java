package com.fongmi.android.tv.ui.home;

import android.view.View;

/** The existing home category debounce primitive; each active host retains one Runnable. */
public final class HomeCategorySwitchScheduler {
    public static final long DELAY_MS = 100;
    private HomeCategorySwitchScheduler() {}
    public static void replace(View host, Runnable task) {
        host.removeCallbacks(task);
        host.postDelayed(task, DELAY_MS);
    }
}
