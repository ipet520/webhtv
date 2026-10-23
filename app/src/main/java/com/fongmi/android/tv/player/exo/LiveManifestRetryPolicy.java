package com.fongmi.android.tv.player.exo;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException;
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy;
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy;

import com.github.catvod.crawler.SpiderDebug;

import java.io.IOException;
import java.util.Locale;

/**
 * Gives transient HLS manifest failures a bounded retry window while leaving segment
 * and non-manifest retry behavior to Media3's default policy.
 */
public final class LiveManifestRetryPolicy implements LoadErrorHandlingPolicy {

    private static final String TAG = "exo-live-retry";
    static final int MAX_MANIFEST_RETRIES = 8;
    private static final long BASE_DELAY_MS = 400L;
    private static final long MAX_DELAY_MS = 3_000L;

    private final DefaultLoadErrorHandlingPolicy delegate = new DefaultLoadErrorHandlingPolicy();

    @NonNull
    @Override
    public FallbackSelection getFallbackSelectionFor(
            @NonNull FallbackOptions fallbackOptions, @NonNull LoadErrorInfo loadErrorInfo) {
        return delegate.getFallbackSelectionFor(fallbackOptions, loadErrorInfo);
    }

    @Override
    public long getRetryDelayMsFor(@NonNull LoadErrorInfo loadErrorInfo) {
        int dataType = loadErrorInfo.mediaLoadData.dataType;
        if (!isTransientManifestFailure(dataType, loadErrorInfo.exception)) {
            return delegate.getRetryDelayMsFor(loadErrorInfo);
        }
        if (loadErrorInfo.errorCount > MAX_MANIFEST_RETRIES) {
            if (SpiderDebug.isEnabled()) SpiderDebug.log(TAG, "give-up attempts=%d error=%s", loadErrorInfo.errorCount, loadErrorInfo.exception.getClass().getSimpleName());
            return delegate.getRetryDelayMsFor(loadErrorInfo);
        }
        long delay = manifestRetryDelayMs(loadErrorInfo.errorCount);
        if (SpiderDebug.isEnabled()) SpiderDebug.log(TAG, "retry attempts=%d delayMs=%d error=%s", loadErrorInfo.errorCount, delay, loadErrorInfo.exception.getClass().getSimpleName());
        return delay;
    }

    @Override
    public int getMinimumLoadableRetryCount(int dataType) {
        int base = delegate.getMinimumLoadableRetryCount(dataType);
        return dataType == C.DATA_TYPE_MANIFEST ? Math.max(base, MAX_MANIFEST_RETRIES) : base;
    }

    static boolean isTransientManifestFailure(int dataType, @Nullable Throwable error) {
        if (dataType != C.DATA_TYPE_MANIFEST || error == null) return false;
        int depth = 0;
        for (Throwable t = error; t != null && depth++ < 12; t = t.getCause() == t ? null : t.getCause()) {
            if (t instanceof UnrecognizedInputFormatException) return true;
            if (t.getClass().getName().equals("androidx.media3.extractor.ParserException")) return true;
            if (t instanceof IOException && isTransientMessage(t.getMessage())) return true;
        }
        return false;
    }

    static long manifestRetryDelayMs(int errorCount) {
        int exponent = Math.min(Math.max(errorCount - 1, 0), 3);
        return Math.min(MAX_DELAY_MS, BASE_DELAY_MS << exponent);
    }

    private static boolean isTransientMessage(@Nullable String message) {
        if (message == null || message.isEmpty()) return true;
        String lower = message.toLowerCase(Locale.ROOT);
        return lower.contains("empty") || lower.contains("unrecognized") || lower.contains("failed to parse")
                || lower.contains("unexpected end") || lower.contains("unexpected eof");
    }
}
