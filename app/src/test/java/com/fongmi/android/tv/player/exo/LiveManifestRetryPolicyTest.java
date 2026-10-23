package com.fongmi.android.tv.player.exo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.C;

import org.junit.Test;

import java.io.IOException;

public class LiveManifestRetryPolicyTest {

    @Test
    public void emptyBodyAndManifestParseFailuresAreTransientOnlyForManifests() {
        assertTrue(LiveManifestRetryPolicy.isTransientManifestFailure(
                C.DATA_TYPE_MANIFEST, new IOException()));
        assertTrue(LiveManifestRetryPolicy.isTransientManifestFailure(
                C.DATA_TYPE_MANIFEST, new IOException("empty body")));
        assertTrue(LiveManifestRetryPolicy.isTransientManifestFailure(
                C.DATA_TYPE_MANIFEST, new IOException("failed to parse manifest")));
        assertTrue(LiveManifestRetryPolicy.isTransientManifestFailure(
                C.DATA_TYPE_MANIFEST,
                new IOException("load failed", new IOException("unexpected eof"))));
        assertFalse(LiveManifestRetryPolicy.isTransientManifestFailure(
                C.DATA_TYPE_MEDIA, new IOException("failed to parse segment")));
    }

    @Test
    public void definitiveHttpFailuresStayFatal() {
        assertFalse(LiveManifestRetryPolicy.isTransientManifestFailure(
                C.DATA_TYPE_MANIFEST, new IOException("Response code: 404")));
        assertFalse(LiveManifestRetryPolicy.isTransientManifestFailure(
                C.DATA_TYPE_MANIFEST, new IOException("upstream reset")));
        assertFalse(LiveManifestRetryPolicy.isTransientManifestFailure(
                C.DATA_TYPE_MANIFEST, null));
    }

    @Test
    public void longerRetryWindowIsScopedToManifestsAndBounded() {
        LiveManifestRetryPolicy policy = new LiveManifestRetryPolicy();

        assertTrue(policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MANIFEST)
                >= LiveManifestRetryPolicy.MAX_MANIFEST_RETRIES);
        assertTrue(policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MEDIA)
                < policy.getMinimumLoadableRetryCount(C.DATA_TYPE_MANIFEST));
        assertEquals(400L, LiveManifestRetryPolicy.manifestRetryDelayMs(1));
        assertEquals(800L, LiveManifestRetryPolicy.manifestRetryDelayMs(2));
        assertEquals(1_600L, LiveManifestRetryPolicy.manifestRetryDelayMs(3));
        assertEquals(3_000L, LiveManifestRetryPolicy.manifestRetryDelayMs(4));
        assertEquals(3_000L, LiveManifestRetryPolicy.manifestRetryDelayMs(20));
    }
}
