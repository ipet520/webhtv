package com.fongmi.android.tv.playback;

/** Stable identity for a native source candidate; VOD ids are scoped to a site. */
public final class PlaybackSourceIdentity {

    private PlaybackSourceIdentity() {}

    public static String key(String siteKey, String vodId) {
        String site = siteKey == null ? "" : siteKey;
        String id = vodId == null ? "" : vodId;
        return site.length() + ":" + site + id.length() + ":" + id;
    }

    public static boolean matches(String leftSiteKey, String leftVodId, String rightSiteKey, String rightVodId) {
        return key(leftSiteKey, leftVodId).equals(key(rightSiteKey, rightVodId));
    }
}
