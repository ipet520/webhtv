package com.fongmi.android.tv.playback;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.TmdbSeasonProgress;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.setting.Setting;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Native authority for the versioned WebHome history contract, without a new table. */
public final class WebHomeHistoryService {
    private WebHomeHistoryService() {}

    public static boolean accessible(HistoryRecordRef ref) {
        return ref != null && ref.isValid() && !Setting.isIncognito()
                && (ref.cid == VodConfig.getCid() || Setting.isGlobalHistoryEnabled());
    }

    public static boolean played(History history) {
        return history != null && history.getPosition() > 0 && history.getDuration() > 0;
    }

    public static com.fongmi.android.tv.history.HistorySourceResolver.EpisodeMatch exactSelection(
            History history, List<com.fongmi.android.tv.bean.Flag> flags) {
        if (history == null || flags == null || flags.isEmpty()) return null;
        List<com.fongmi.android.tv.bean.Flag> candidates = new ArrayList<>();
        for (int i = 0; i < flags.size(); i++) {
            com.fongmi.android.tv.bean.Flag flag = flags.get(i);
            if (com.fongmi.android.tv.bean.Flag.stableKey(flag, i).equals(history.getSourceBindingKey())) {
                candidates.add(flag); break;
            }
        }
        if (candidates.isEmpty()) for (com.fongmi.android.tv.bean.Flag flag : flags) {
            if (flag.getFlag().equals(history.getVodFlag())) candidates.add(flag);
        }
        if (candidates.size() > 1) candidates.removeIf(flag -> flag.getEpisodes().stream()
                .noneMatch(episode -> episode.getUrl().equals(history.getEpisodeUrl())));
        if (candidates.size() != 1) return null;
        var match = com.fongmi.android.tv.history.HistorySourceResolver.findEpisode(candidates, history);
        if (match == null) return null;
        if (!match.episode().matchesPlayback(history.getEpisode()) && candidates.get(0).getEpisodes().size() != 1) return null;
        return match;
    }

    public static History resolve(HistoryRecordRef ref, boolean playback) {
        if (!accessible(ref)) throw new IllegalArgumentException("历史引用无效或不在可访问配置范围");
        if (playback && ref.cid != VodConfig.getCid()) throw new IllegalArgumentException("请先切换到该历史的原配置");
        if (playback && com.fongmi.android.tv.utils.AudioUtil.isAudioSiteEnabled(ref.siteKey)) {
            throw new IllegalArgumentException("该来源使用独立音频历史，请通过原生音频页面恢复");
        }
        History route = History.find(ref.cid, ref.historyKey);
        if (!ref.matches(route)) throw new IllegalArgumentException("历史记录已不存在或来源改变");
        History history = route;
        if (ref.isSeason()) {
            History seasonal = route.copy();
            seasonal.setTmdbEpisodePosition(ref.seasonNumber, 1);
            history = HistoryResumePayload.restore(ref.cid, HistoryResumePayload.encode(seasonal));
            if (history == null || history.getTmdbId() != ref.tmdbId
                    || !ref.mediaType.equals(TmdbSeasonProgress.normalizeMediaType(history.getMediaType()))) {
                throw new IllegalArgumentException("季度来源已改变，无法精确恢复");
            }
        }
        if (playback && !played(history)) throw new IllegalArgumentException("历史没有有效播放进度");
        if (playback && !history.isCurrentSourceAvailable()) throw new IllegalArgumentException("原历史站源不可用");
        return history;
    }

    public static JsonObject read() {
        return TmdbSeasonProgressStore.runInTransaction(() -> {
            List<History> routes = Setting.isIncognito() ? List.of() : Setting.isGlobalHistoryEnabled()
                    ? AppDatabase.get().getHistoryDao().findAll()
                    : AppDatabase.get().getHistoryDao().findAll(VodConfig.getCid());
            List<TmdbSeasonProgress> snapshots = AppDatabase.get().getTmdbSeasonProgressDao().findAll();
            List<JsonObject> records = new ArrayList<>();
            for (History route : routes) {
                // Persistent record existence is independent of a resume point.
                // Keep accessible physical rows; never synthesize playback progress.
                HistoryRecordRef item = HistoryRecordRef.from(route, "ITEM");
                if (item.isValid()) {
                    item.scopeToken = scopeToken(item);
                    records.add(record(route, item));
                }
                for (TmdbSeasonProgress snapshot : snapshots) {
                    if (snapshot.cid != route.getCid() || snapshot.tmdbId != route.getTmdbId()
                            || !snapshot.mediaType.equals(TmdbSeasonProgress.normalizeMediaType(route.getMediaType()))
                            || !snapshot.sourceHistoryKey.equals(route.getKey()) || snapshot.episodeNumber <= 0
                            || snapshot.seasonNumber < 0 || snapshot.position <= 0 || snapshot.duration <= 0) continue;
                    if (played(route) && HistoryRecordRef.from(route, "ITEM").seasonNumber == snapshot.seasonNumber) continue;
                    History projection = route.copy();
                    TmdbSeasonProgressStore.apply(projection, snapshot);
                    HistoryRecordRef ref = HistoryRecordRef.from(projection, "SEASON");
                    ref.scopeToken = scopeToken(ref);
                    records.add(record(projection, ref));
                }
            }
            records.sort(Comparator.comparingLong((JsonObject record) -> record.get("createTime").getAsLong()).reversed());
            JsonArray array = new JsonArray();
            records.forEach(array::add);
            JsonObject result = new JsonObject();
            result.addProperty("version", 1);
            result.addProperty("mode", "independent");
            result.addProperty("cidScope", Setting.isGlobalHistoryEnabled() ? "global" : "current");
            result.add("records", array);
            result.add("legacy", App.gson().toJsonTree(History.get()));
            return result;
        });
    }

    private static JsonObject record(History history, HistoryRecordRef ref) {
        JsonObject result = App.gson().toJsonTree(history).getAsJsonObject();
        result.add("historyRecordRef", App.gson().toJsonTree(ref));
        result.addProperty("displayId", ref.displayId());
        result.addProperty("deleteScope", ref.kind);
        result.addProperty("sourceLabel", history.getSiteName().isEmpty() ? ref.siteKey : history.getSiteName());
        boolean audioSource = com.fongmi.android.tv.utils.AudioUtil.isAudioSiteEnabled(ref.siteKey);
        result.addProperty("canResume", played(history) && history.isCurrentSourceAvailable() && !audioSource);
        // Deleting a physical route also removes its snapshots. Do not present that
        // as a harmless single-quarter operation when other projections are visible.
        boolean hasOtherSeasons = !ref.isSeason() && AppDatabase.get().getTmdbSeasonProgressDao()
                .findBySource(ref.cid, history.getMediaType(), history.getTmdbId(), ref.historyKey)
                .stream().anyMatch(item -> item.seasonNumber != ref.seasonNumber);
        result.addProperty("canDelete", ref.cid > 0 && !hasOtherSeasons);
        result.addProperty("capabilityReason", ref.cid <= 0 ? "该配置尚未持久化，不支持本机 API 精确删除"
                : hasOtherSeasons ? "此记录关联其他季度，请先删除具体季度进度"
                : !played(history) ? "尚未建立有效续播点"
                : ref.cid != VodConfig.getCid() ? "请先切换到该历史的原配置"
                : audioSource ? "该来源使用独立音频历史，请通过原生音频页面恢复" : "");
        return result;
    }

    /** Fingerprint the complete native scope touched by the existing writer. */
    public static String scopeToken(HistoryRecordRef ref) {
        List<String> state = new ArrayList<>();
        History row = ref.isSeason() ? null : History.find(ref.cid, ref.historyKey);
        List<History> routes = ref.isSeason()
                ? AppDatabase.get().getHistoryDao().findByTmdbIdentity(ref.cid, ref.mediaType, ref.tmdbId)
                : row == null ? List.of() : List.of(row);
        for (History route : routes) state.add(App.gson().toJson(route));
        List<TmdbSeasonProgress> snapshots = ref.isSeason()
                ? AppDatabase.get().getTmdbSeasonProgressDao().findByMedia(ref.cid, ref.mediaType, ref.tmdbId)
                : AppDatabase.get().getTmdbSeasonProgressDao().findBySource(ref.cid, ref.mediaType, ref.tmdbId, ref.historyKey);
        snapshots.forEach(item -> state.add(App.gson().toJson(item)));
        state.sort(String::compareTo);
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(App.gson().toJson(state).getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte value : digest) result.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
            return result.toString();
        } catch (java.security.NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
    }

    public static JsonObject delete(JsonObject payload) {
        if (!payload.has("version") || payload.get("version").getAsInt() != 1
                || !payload.has("refs") || !payload.get("refs").isJsonArray()
                || payload.getAsJsonArray("refs").size() > 200) throw new IllegalArgumentException("无效历史删除请求");
        // Same lock ordering as deleteInternal: writer first, native transaction second.
        synchronized (PlaybackProgressWriter.class) {
            return TmdbSeasonProgressStore.runInTransaction(() -> deleteLocked(payload));
        }
    }

    private static JsonObject deleteLocked(JsonObject payload) {
        String scope = payload.has("scope") ? payload.get("scope").getAsString() : "";
        if (!scope.isEmpty() && !"GROUP".equals(scope)) throw new IllegalArgumentException("不支持的历史删除范围");
        boolean group = "GROUP".equals(scope);
        List<HistoryRecordRef> refs = new ArrayList<>();
        JsonArray results = new JsonArray();
        for (com.google.gson.JsonElement element : payload.getAsJsonArray("refs")) {
            if (!element.isJsonObject() || !element.getAsJsonObject().has("version")) throw new IllegalArgumentException("无效历史引用");
            HistoryRecordRef ref = App.gson().fromJson(element, HistoryRecordRef.class);
            if (ref == null || !ref.isValid()) throw new IllegalArgumentException("无效历史引用");
            if (refs.stream().noneMatch(item -> item.displayId().equals(ref.displayId()))) refs.add(ref);
        }
        if (group) {
            if (!payload.has("confirm") || !payload.get("confirm").getAsBoolean() || refs.isEmpty()) throw new IllegalArgumentException("作品组删除需要明确确认");
            HistoryRecordRef first = refs.get(0);
            if (first.tmdbId <= 0 || !List.of("movie", "tv").contains(first.mediaType)
                    || refs.stream().anyMatch(ref -> ref.tmdbId != first.tmdbId || !ref.mediaType.equals(first.mediaType))) throw new IllegalArgumentException("作品组身份不一致");
            java.util.Set<String> expected = new java.util.HashSet<>();
            for (com.google.gson.JsonElement element : read().getAsJsonArray("records")) {
                HistoryRecordRef ref = App.gson().fromJson(element.getAsJsonObject().get("historyRecordRef"), HistoryRecordRef.class);
                if (ref.tmdbId == first.tmdbId && ref.mediaType.equals(first.mediaType)) expected.add(ref.displayId());
            }
            java.util.Set<String> selected = new java.util.HashSet<>();
            refs.forEach(ref -> selected.add(ref.displayId()));
            if (!expected.equals(selected)) throw new IllegalArgumentException("作品组已改变或选择不完整，请刷新后重新确认");
            for (HistoryRecordRef ref : refs) {
                resolve(ref, false);
                if (!scopeToken(ref).equals(ref.scopeToken)) throw new IllegalArgumentException("作品组进度已改变，请刷新后重新确认");
            }
        }
        java.util.Map<String, PlaybackProgressApplyResult> groupResults = new java.util.HashMap<>();
        java.util.Map<String, String> invalid = new java.util.HashMap<>();
        if (!group) for (HistoryRecordRef ref : refs) {
            try {
                if (!accessible(ref)) throw new IllegalArgumentException("历史配置不可访问");
                if (History.find(ref.cid, ref.historyKey) == null) continue;
                resolve(ref, false);
                if (!scopeToken(ref).equals(ref.scopeToken)) throw new IllegalArgumentException("历史范围已更新，请刷新后重新选择");
            } catch (IllegalArgumentException error) { invalid.put(ref.displayId(), error.getMessage()); }
        }
        for (HistoryRecordRef original : refs) {
            HistoryRecordRef ref = original;
            PlaybackProgressApplyResult item;
            if (invalid.containsKey(ref.displayId())) {
                item = PlaybackProgressApplyResult.failed((PlaybackProgressDeleteInput) null, invalid.get(ref.displayId()));
            } else if (group && groupResults.containsKey(ref.historyKey)) item = groupResults.get(ref.historyKey);
            else {
            if (group) {
                ref = HistoryRecordRef.from(History.find(original.cid, original.historyKey), "ITEM");
            } else {
                ref = App.gson().fromJson(App.gson().toJson(original), HistoryRecordRef.class);
            }
            // Preflight froze all user-owned states under this transaction. Tokens
            // now account for earlier deletions in this same batch, never new users.
            ref.scopeToken = scopeToken(ref);
            PlaybackProgressDeleteInput input = new PlaybackProgressDeleteInput();
            input.recordRef = ref;
            input.historyKey = ref == null ? "" : ref.historyKey;
            input.cid = ref == null ? -1 : ref.cid;
            input.scope = ref != null && ref.isSeason() ? "season" : "item";
            input.mediaType = ref == null ? "" : ref.mediaType;
            input.tmdbId = ref == null ? 0 : ref.tmdbId;
            input.seasonNumber = ref == null ? -1 : ref.seasonNumber;
            input.exact = true;
            input.confirmedGroup = group;
            PlaybackProgressBatchResult batch = PlaybackProgressWriter.deleteFromLocalApi(List.of(input));
            item = batch.items.get(0);
            if (group) groupResults.put(ref.historyKey, item);
            }
            JsonObject result = new JsonObject();
            result.addProperty("displayId", original.displayId());
            result.addProperty("status", item.success ? item.affected > 0 ? "DELETED" : "NOT_FOUND" : "FAILED");
            result.addProperty("message", item.message);
            results.add(result);
        }
        JsonObject result = new JsonObject();
        result.addProperty("version", 1);
        result.add("results", results);
        result.add("readback", read());
        return result;
    }
}
