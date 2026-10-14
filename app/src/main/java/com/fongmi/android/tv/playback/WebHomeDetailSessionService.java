package com.fongmi.android.tv.playback;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Objects;
import java.util.HashSet;

/** Ephemeral player ownership. Native History remains the only persistent progress store. */
public final class WebHomeDetailSessionService {
    private static final long TTL = 2 * 60 * 60 * 1000L;
    private static final LinkedHashMap<String, Session> sessions = new LinkedHashMap<>();
    private static final LinkedHashMap<String, Long> closed = new LinkedHashMap<>();
    public static final class Session {
        public final String id, mediaKey;
        public final int cid;
        public String requestId;
        public JsonObject route;
        public boolean resume;
        private long expiresAt;
        private History owned;
        private JsonObject lastRequest;
        private final HashSet<String> requests = new HashSet<>();
        private Session(String id, String mediaKey) {
            this.id = id; this.mediaKey = mediaKey; cid = VodConfig.getCid();
        }
    }
    private static String text(JsonObject value, String key) {
        return value.has(key) && value.get(key).isJsonPrimitive() ? value.get(key).getAsString().trim() : "";
    }
    private static String identity(JsonObject value, String key) {
        String result = text(value, key);
        if (!result.matches("[A-Za-z0-9_-]{8,160}")) throw new IllegalArgumentException("无效的详情会话身份");
        return result;
    }
    private static void prune() {
        long now = System.currentTimeMillis();
        sessions.values().removeIf(s -> {
            if (s.expiresAt >= now && s.cid == VodConfig.getCid()) return false;
            closed.put(s.id, now + TTL); return true;
        });
        closed.values().removeIf(expires -> expires < now);
    }
    public record Acceptance(Session session, boolean launch) {}
    public static synchronized Acceptance accept(JsonObject payload) {
        prune();
        if (!payload.has("version") || payload.get("version").getAsInt() != 1) throw new IllegalArgumentException("不支持的详情会话版本");
        String id = identity(payload, "sessionId"), request = identity(payload, "requestId"), media = text(payload, "mediaKey");
        if (closed.containsKey(id)) throw new IllegalArgumentException("详情会话已关闭");
        if (media.isEmpty() || media.length() > 300) throw new IllegalArgumentException("缺少详情作品身份");
        Session s = sessions.get(id);
        if (s != null && !s.mediaKey.equals(media)) throw new IllegalArgumentException("详情会话作品不一致");
        if (s != null && request.equals(s.requestId)) {
            if (!payload.equals(s.lastRequest)) throw new IllegalArgumentException("重复播放请求来源不一致");
            return new Acceptance(s, false);
        }
        if (s != null && (s.requests.contains(request) || s.requests.size() >= 128)) throw new IllegalArgumentException("详情播放请求已失效");
        boolean resume = payload.has("resume") && payload.get("resume").getAsBoolean();
        if (resume && s == null) throw new IllegalArgumentException("详情会话已结束，请重新准备资源");
        if (resume) resumeHistory(s);
        JsonObject route = payload.has("route") && payload.get("route").isJsonObject() ? payload.getAsJsonObject("route").deepCopy() : null;
        if (!resume && (route == null || text(route, "siteKey").isEmpty() || text(route, "vodId").isEmpty()
                || !text(route, "entry").matches("pan-push|native-vod"))) throw new IllegalArgumentException("详情会话播放来源无效");
        if (!resume) {
            route.addProperty("title", text(route, "title")); route.addProperty("pic", text(route, "pic"));
        }
        if (s == null) { s = new Session(id, media); sessions.put(id, s); }
        if (!resume) { s.route = route; s.owned = null; }
        s.requests.add(request); s.lastRequest = payload.deepCopy();
        s.resume = resume; s.requestId = request; s.expiresAt = System.currentTimeMillis() + TTL;
        while (sessions.size() > 16) close(sessions.keySet().iterator().next());
        return new Acceptance(s, true);
    }
    public static synchronized Session require(String id, String requestId) {
        prune(); Session s = sessions.get(id);
        if (s == null || !Objects.equals(s.requestId, requestId)) throw new IllegalArgumentException("详情播放请求已失效");
        return s;
    }
    public static synchronized History resumeHistory(Session s) {
        History owned = s.owned;
        History stored = owned == null ? null : History.find(s.cid, owned.getKey());
        if (stored == null || !sameMedia(s, stored) || !stored.isCurrentSourceAvailable() || stored.getPosition() <= 0 || stored.getDuration() <= 0
                || !Objects.equals(stored.getVodFlag(), owned.getVodFlag())
                || !Objects.equals(stored.getSourceBindingKey(), owned.getSourceBindingKey())
                || !Objects.equals(stored.getEpisodeUrl(), owned.getEpisodeUrl())
                || stored.getTmdbSeasonNumber() != owned.getTmdbSeasonNumber())
            throw new IllegalArgumentException("本次会话尚无有效续播点或播放目标已改变");
        return stored.copy();
    }
    private static boolean sameMedia(Session s, History history) {
        String[] parts = s.mediaKey.split("\\|", 3);
        boolean typed = parts.length >= 2 && parts[0].matches("movie|tv") && parts[1].matches("[1-9][0-9]*");
        String nativeType = history.getMediaType().trim().toLowerCase(java.util.Locale.ROOT);
        boolean known = typed && history.getTmdbId() > 0 && nativeType.matches("movie|tv");
        if (known && (!parts[1].equals(String.valueOf(history.getTmdbId())) || !parts[0].equals(nativeType))) return false;
        return known || (text(s.route,"siteKey").equals(history.getSiteKey()) && text(s.route,"vodId").equals(history.getVodId()));
    }
    public static synchronized void bind(String id, String requestId, History history) {
        Session s = require(id, requestId);
        if (history == null || history.getCid() != s.cid || !sameMedia(s, history)) throw new IllegalArgumentException("本次详情的作品或播放配置不一致");
        s.owned = history;
    }
    public static synchronized JsonObject read(String id) {
        prune(); Session s = sessions.get(id);
        if (s == null) throw new IllegalArgumentException("详情会话已结束");
        JsonObject result = new JsonObject();
        result.addProperty("version", 1); result.addProperty("sessionId", s.id);
        result.addProperty("mediaKey", s.mediaKey); result.addProperty("requestId", s.requestId);
        result.addProperty("accepted", true); result.addProperty("actualPlayback", "UNKNOWN");
        result.addProperty("expiresAt", s.expiresAt);
        result.add("route", s.route.deepCopy());
        try {
            History h = resumeHistory(s); result.add("history", App.gson().toJsonTree(h)); result.addProperty("canResume", true);
        } catch (IllegalArgumentException e) { result.addProperty("canResume", false); result.addProperty("reason", e.getMessage()); }
        return result;
    }
    public static synchronized void close(String id) {
        if (id == null || !id.matches("[A-Za-z0-9_-]{8,160}")) return;
        sessions.remove(id); closed.put(id, System.currentTimeMillis() + TTL);
        while (closed.size() > 256) closed.remove(closed.keySet().iterator().next());
    }
    public static synchronized void fail(String id, String requestId) {
        Session s = sessions.get(id);
        if (s != null && Objects.equals(s.requestId, requestId)) close(id);
    }
    static synchronized void clear() { sessions.clear(); closed.clear(); }
}
