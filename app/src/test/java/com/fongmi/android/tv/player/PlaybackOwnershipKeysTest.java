package com.fongmi.android.tv.player;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlaybackOwnershipKeysTest {

    private static final String HISTORY_KEY = "VP:site@@@vod@@@catalog";

    @Test
    public void episodeNavigationKeyBelongsToItsHistorySession() {
        String episodeKey = "VE:" + HISTORY_KEY + "|line|3";

        assertEquals(HISTORY_KEY, PlaybackOwnershipKeys.sessionKeyOf(episodeKey));
        assertTrue(PlaybackOwnershipKeys.sameSession(HISTORY_KEY, episodeKey));
        assertTrue(PlaybackOwnershipKeys.sameSession(null, episodeKey));
    }

    @Test
    public void historyKeyMayContainEpisodeSeparators() {
        String historyKey = "VP:site|part@@@vod@@@catalog";
        String episodeKey = "VE:" + historyKey + "|line|3";

        assertEquals(historyKey, PlaybackOwnershipKeys.sessionKeyOf(episodeKey));
        assertTrue(PlaybackOwnershipKeys.sameSession(historyKey, episodeKey));
    }

    @Test
    public void differentAndMalformedKeysDoNotShareOwnership() {
        assertFalse(PlaybackOwnershipKeys.sameSession(HISTORY_KEY, "VE:other@@@vod@@@catalog|line|3"));
        assertFalse(PlaybackOwnershipKeys.sameSession(HISTORY_KEY, null));
        assertFalse(PlaybackOwnershipKeys.sameSession(HISTORY_KEY, ""));
        assertEquals("VE:" + HISTORY_KEY, PlaybackOwnershipKeys.sessionKeyOf("VE:" + HISTORY_KEY));
        assertEquals("VE:" + HISTORY_KEY + "||3",
                PlaybackOwnershipKeys.sessionKeyOf("VE:" + HISTORY_KEY + "||3"));
        assertEquals("VE:" + HISTORY_KEY + "|line|",
                PlaybackOwnershipKeys.sessionKeyOf("VE:" + HISTORY_KEY + "|line|"));
        assertFalse(PlaybackOwnershipKeys.sameSession(HISTORY_KEY,
                "VE:" + HISTORY_KEY + "|line|"));
    }

    @Test
    public void emptyExpectedKeyRetainsExactMatchSemantics() {
        assertTrue(PlaybackOwnershipKeys.sameSession("", ""));
        assertFalse(PlaybackOwnershipKeys.sameSession("", HISTORY_KEY));
    }
}
