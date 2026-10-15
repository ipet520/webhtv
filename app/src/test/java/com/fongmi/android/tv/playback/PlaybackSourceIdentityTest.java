package com.fongmi.android.tv.playback;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaybackSourceIdentityTest {

    @Test
    public void sameVodIdOnDifferentSitesIsAValidReplacement() {
        assertFalse(PlaybackSourceIdentity.matches("site-a", "42", "site-b", "42"));
        assertTrue(PlaybackSourceIdentity.matches("site-a", "42", "site-a", "42"));
    }

    @Test
    public void keyIsUnambiguousWhenEitherPartContainsDelimiters() {
        assertNotSameKey("a:b", "c", "a", "b:c");
        assertEquals(PlaybackSourceIdentity.key("", ""), "0:0:");
    }

    private static void assertNotSameKey(String firstSite, String firstId, String secondSite, String secondId) {
        assertFalse(PlaybackSourceIdentity.key(firstSite, firstId)
                .equals(PlaybackSourceIdentity.key(secondSite, secondId)));
    }
}
