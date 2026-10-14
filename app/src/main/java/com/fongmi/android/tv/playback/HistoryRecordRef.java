package com.fongmi.android.tv.playback;

import com.fongmi.android.tv.bean.History;

/** UI identity is separate from the unchanged Room primary key. */
public final class HistoryRecordRef {
    public int version = 1;
    public String kind = "ITEM";
    public String historyKey = "";
    public int cid;
    public String siteKey = "";
    public String vodId = "";
    public String mediaType = "";
    public int tmdbId;
    public int seasonNumber = -1;
    public long updatedAt;
    public String scopeToken = "";

    public static HistoryRecordRef from(History history, String kind) {
        HistoryRecordRef ref = new HistoryRecordRef();
        ref.kind = kind;
        ref.historyKey = history.getKey();
        ref.cid = history.getCid();
        ref.siteKey = history.getSiteKey();
        ref.vodId = history.getVodId();
        ref.mediaType = com.fongmi.android.tv.bean.TmdbSeasonProgress.normalizeMediaType(history.getMediaType());
        ref.tmdbId = history.getTmdbId();
        ref.seasonNumber = history.getTmdbSeasonNumber();
        if (ref.seasonNumber == 0 && history.getTmdbEpisodeNumber() <= 0) ref.seasonNumber = -1;
        ref.updatedAt = history.getCreateTime();
        return ref;
    }

    public boolean isSeason() { return "SEASON".equals(kind); }

    public boolean isValid() {
        return version == 1 && ("ITEM".equals(kind) || isSeason()) && cid >= 0
                && mediaType != null && scopeToken != null
                && historyKey != null && !historyKey.isEmpty() && siteKey != null && !siteKey.isEmpty()
                && vodId != null && !vodId.isEmpty()
                && (!isSeason() || "tv".equals(mediaType) && tmdbId > 0 && seasonNumber >= 0);
    }

    public boolean matches(History history) {
        return isValid() && history != null && cid == history.getCid()
                && historyKey.equals(history.getKey()) && siteKey.equals(history.getSiteKey())
                && vodId.equals(history.getVodId())
                && (tmdbId <= 0 || tmdbId == history.getTmdbId()
                    && mediaType.equals(com.fongmi.android.tv.bean.TmdbSeasonProgress.normalizeMediaType(history.getMediaType())));
    }

    public String displayId() {
        // JSON tuple escaping prevents delimiter-bearing PUSH URLs from colliding.
        return new com.google.gson.GsonBuilder().disableHtmlEscaping().create().toJson(new Object[]{version, kind, cid, historyKey,
                isSeason() ? mediaType : "", isSeason() ? tmdbId : 0, isSeason() ? seasonNumber : -1});
    }

    public String resumePayload(History history) {
        return isSeason() ? HistoryResumePayload.encode(history) : historyKey;
    }
}
