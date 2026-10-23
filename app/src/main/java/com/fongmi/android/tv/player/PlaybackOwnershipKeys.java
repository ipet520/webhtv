package com.fongmi.android.tv.player;

/** Normalizes the service's episode-navigation key to its owning playback session. */
public final class PlaybackOwnershipKeys {

    static final String EPISODE_PREFIX = "VE:";
    private static final char EPISODE_SEPARATOR = '|';

    private PlaybackOwnershipKeys() {
    }

    /**
     * A null expected key keeps the existing "unrestricted owner" contract. Episode keys have the
     * form {@code VE:<historyKey>|<line>|<index>}; malformed values remain opaque exact keys.
     */
    public static boolean sameSession(String expected, String actual) {
        if (expected == null) return true;
        String expectedSession = sessionKeyOf(expected);
        String actualSession = sessionKeyOf(actual);
        return actualSession != null && expectedSession.equals(actualSession);
    }

    static String sessionKeyOf(String key) {
        if (key == null || !key.startsWith(EPISODE_PREFIX)) return key;
        String body = key.substring(EPISODE_PREFIX.length());
        int last = body.lastIndexOf(EPISODE_SEPARATOR);
        if (last <= 0 || last == body.length() - 1) return key;
        int previous = body.lastIndexOf(EPISODE_SEPARATOR, last - 1);
        if (previous <= 0 || previous == last - 1) return key;
        return body.substring(0, previous);
    }
}
