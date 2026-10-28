package com.fongmi.android.tv.ui.homepreview;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import com.fongmi.android.tv.BuildConfig;

/** Bounded counters; optional on-device logging, never posts a frame or focus task. */
final class HomePreviewTiming {
    private long keyStart;
    private long focusStart,scrollStart;
    int keys, focuses, geometry, heroes, backgrounds, restorePosts, measures, draws, scrollStates;
    private int logBudget = 120;
    private int drawBudget;
    void key(int code, int repeat, long eventTime) {
        keys++;
        keyStart = System.nanoTime();
        drawBudget = 2;
        if (enabled()) Log.d("NativeHomeFocus", "key=" + code + " repeat=" + repeat + " count=" + keys
                + " queuedMs=" + Math.max(0, SystemClock.uptimeMillis() - eventTime));
    }
    void focus(String region, int position) {
        focuses++;
        focusStart=System.nanoTime();
        if (enabled()) Log.d("NativeHomeFocus", "focus=" + region + ":" + position + " afterKeyUs=" + (keyStart == 0 ? -1 : (System.nanoTime() - keyStart) / 1000));
    }
    void dispatch(long started) {
        if (enabled()) Log.d("NativeHomeFocus", "dispatchUs=" + (System.nanoTime() - started) / 1000);
    }
    void scroll(int state) {
        scrollStates++;
        long now=System.nanoTime();
        if(state!=0&&scrollStart==0)scrollStart=now;
        if (enabled()) Log.d("NativeHomeFocus", "scrollState=" + state
            + " afterFocusUs="+(focusStart==0?-1:(now-focusStart)/1000)
            + " elapsedScrollUs="+(scrollStart==0?-1:(now-scrollStart)/1000));
        if(state==0)scrollStart=0;
    }
    void draw(long started) {
        draws++;
        if (drawBudget > 0) {
            drawBudget--;
            if (enabled()) Log.d("NativeHomeFocus", "drawUs=" + (System.nanoTime() - started) / 1000 + " afterKeyUs=" + (keyStart == 0 ? -1 : (System.nanoTime() - keyStart) / 1000));
        }
    }
    private boolean enabled() {
        if (!BuildConfig.DEBUG || logBudget <= 0 || !Log.isLoggable("NativeHomeFocus", Log.DEBUG)) return false;
        logBudget--;
        return true;
    }
    Bundle snapshot() {
        Bundle result = new Bundle();
        result.putInt("keys", keys); result.putInt("focuses", focuses); result.putInt("geometry", geometry);
        result.putInt("heroes", heroes); result.putInt("backgrounds", backgrounds); result.putInt("restorePosts", restorePosts);
        result.putInt("measures", measures); result.putInt("draws", draws); result.putInt("scrollStates", scrollStates);
        return result;
    }
}
