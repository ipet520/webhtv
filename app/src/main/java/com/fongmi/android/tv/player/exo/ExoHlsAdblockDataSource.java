package com.fongmi.android.tv.player.exo;

import android.net.Uri;

import androidx.annotation.Nullable;
import androidx.media3.common.C;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.AdBlockStatsStore;
import com.fongmi.android.tv.utils.HlsAdblockNotice;
import com.fongmi.android.tv.utils.HlsAdblockPipeline;
import com.fongmi.android.tv.utils.HlsManifestCleaner;
import com.fongmi.android.tv.api.config.HlsRuleConfig;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.Notify;
import com.github.catvod.crawler.SpiderDebug;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class ExoHlsAdblockDataSource implements DataSource {

    private static final String TAG = "exo-hls-adblock";
    static final int MANIFEST_PROBE_BYTES = 8 * 1024;
    private final DataSource upstream;
    private byte[] buffered;
    private int position;
    private boolean bufferedOnly;

    ExoHlsAdblockDataSource(DataSource upstream) {
        this.upstream = upstream;
    }

    @Override
    public void addTransferListener(TransferListener transferListener) {
        upstream.addTransferListener(transferListener);
    }

    @Override
    public long open(DataSpec dataSpec) throws IOException {
        long upstreamLength = upstream.open(dataSpec);
        buffered = null;
        position = 0;
        bufferedOnly = false;
        if (!isManifestRequest(dataSpec.uri.toString(), upstream.getResponseHeaders())) return upstreamLength;
        try {
            byte[] probe = readProbe();
            if (!looksLikePlaylist(probe)) {
                // A segment may carry an .m3u8 suffix. Keep only a bounded prefix in memory
                // until it is identified, then stream the remaining bytes directly upstream.
                buffered = probe;
                if (SpiderDebug.isEnabled()) SpiderDebug.log(TAG, "not-a-playlist probeBytes=%d", probe.length);
                return upstreamLength;
            }
            byte[] original = combine(probe, readAll());
            String text = new String(original, StandardCharsets.UTF_8);
            List<HlsManifestCleaner.Rule> rules = List.of();
            boolean legacyFallback = false;
            if (Setting.isAdblock()) {
                rules = HlsRuleConfig.getRules();
                legacyFallback = HlsRuleConfig.isLegacyFallbackEnabled();
            }
            HlsAdblockPipeline.Outcome outcome = HlsAdblockPipeline.apply(
                    dataSpec.uri.toString(), text, rules, legacyFallback);
            buffered = outcome.manifest().getBytes(StandardCharsets.UTF_8);
            position = 0;
            bufferedOnly = true;
            recordAndNotify(dataSpec.uri, outcome);
            if (SpiderDebug.isEnabled()) {
                SpiderDebug.log(TAG, "manifest bytes=%d rewritten=%d removed=%d structured=%s legacy=%s url=%s",
                        original.length, buffered.length, outcome.removedSegments(), outcome.structured(),
                        outcome.legacy(), dataSpec.uri);
            }
            return buffered.length;
        } catch (RuntimeException e) {
            close();
            throw e;
        }
    }

    @Override
    public int read(byte[] buffer, int offset, int length) throws IOException {
        if (buffered != null && position < buffered.length) {
            int count = Math.min(length, buffered.length - position);
            System.arraycopy(buffered, position, buffer, offset, count);
            position += count;
            return count;
        }
        if (bufferedOnly) return C.RESULT_END_OF_INPUT;
        buffered = null;
        position = 0;
        return upstream.read(buffer, offset, length);
    }

    @Nullable
    @Override
    public Uri getUri() {
        return upstream.getUri();
    }

    @Override
    public Map<String, List<String>> getResponseHeaders() {
        return upstream.getResponseHeaders();
    }

    @Override
    public void close() throws IOException {
        buffered = null;
        position = 0;
        bufferedOnly = false;
        upstream.close();
    }

    static boolean isManifestUrl(String url) {
        if (url == null) return false;
        int query = url.indexOf('?');
        int fragment = url.indexOf('#');
        int end = url.length();
        if (query >= 0) end = Math.min(end, query);
        if (fragment >= 0) end = Math.min(end, fragment);
        return url.substring(0, end).toLowerCase(Locale.ROOT).endsWith(".m3u8");
    }

    static boolean isManifestRequest(String url, Map<String, List<String>> responseHeaders) {
        Boolean byContentType = hlsContentType(responseHeaders);
        // A segment may use an .m3u8 suffix while explicitly identifying itself as media bytes.
        if (byContentType != null) return byContentType;
        return isManifestUrl(url);
    }

    static boolean looksLikePlaylist(byte[] body) {
        if (body == null) return false;
        int index = 0;
        if (body.length >= 3 && (body[0] & 0xFF) == 0xEF && (body[1] & 0xFF) == 0xBB && (body[2] & 0xFF) == 0xBF) index = 3;
        while (index < body.length && (body[index] == ' ' || body[index] == '\r' || body[index] == '\n' || body[index] == '\t')) index++;
        return index + 7 <= body.length
                && body[index] == '#' && body[index + 1] == 'E' && body[index + 2] == 'X'
                && body[index + 3] == 'T' && body[index + 4] == 'M' && body[index + 5] == '3'
                && body[index + 6] == 'U';
    }

    @Nullable
    private static Boolean hlsContentType(Map<String, List<String>> responseHeaders) {
        if (responseHeaders == null) return null;
        Boolean result = null;
        for (Map.Entry<String, List<String>> header : responseHeaders.entrySet()) {
            if (header.getKey() == null || !"content-type".equalsIgnoreCase(header.getKey())) continue;
            for (String value : header.getValue()) {
                if (value == null) continue;
                if (isHlsContentType(value)) return Boolean.TRUE;
                if (isSegmentContentType(value)) result = Boolean.FALSE;
            }
        }
        return result;
    }

    private static boolean isSegmentContentType(String value) {
        String type = value.toLowerCase(Locale.ROOT);
        if (isHlsContentType(type)) return false;
        // octet-stream is common for misconfigured playlist hosts, so let the suffix and
        // bounded body probe decide instead of disabling ad filtering for those hosts.
        return type.contains("mp2t") || type.startsWith("video/") || type.startsWith("audio/");
    }

    private static boolean isHlsContentType(String value) {
        if (value == null) return false;
        String type = value.toLowerCase(Locale.ROOT);
        return type.contains("application/vnd.apple.mpegurl")
                || type.contains("application/x-mpegurl")
                || type.contains("audio/mpegurl")
                || type.contains("audio/x-mpegurl");
    }

    static Result cleanForTest(String url, String manifest) {
        HlsManifestCleaner.Rule rule = HlsManifestCleaner.Rule.builder()
                .id("test-ad-path")
                .segmentUrlPatterns(List.of("(^|/)ad/"))
                .build();
        HlsAdblockPipeline.Outcome outcome = HlsAdblockPipeline.apply(
                url, manifest, List.of(rule), true);
        return new Result(outcome.manifest(), outcome.structured() || outcome.legacy(), notice(outcome));
    }

    private byte[] readAll() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = upstream.read(buffer, 0, buffer.length)) != C.RESULT_END_OF_INPUT) {
            output.write(buffer, 0, count);
        }
        return output.toByteArray();
    }

    private byte[] readProbe() throws IOException {
        return readProbe(upstream);
    }

    static byte[] readProbe(DataSource source) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream(MANIFEST_PROBE_BYTES);
        byte[] chunk = new byte[Math.min(1024, MANIFEST_PROBE_BYTES)];
        while (output.size() < MANIFEST_PROBE_BYTES) {
            int count = source.read(chunk, 0,
                    Math.min(chunk.length, MANIFEST_PROBE_BYTES - output.size()));
            if (count <= 0) break;
            output.write(chunk, 0, count);
            byte[] current = output.toByteArray();
            if (looksLikePlaylist(current) || !couldBePlaylistPrefix(current)) break;
        }
        return output.toByteArray();
    }

    private static boolean couldBePlaylistPrefix(byte[] body) {
        if (body.length == 0) return true;
        int index = 0;
        if ((body[0] & 0xFF) == 0xEF) {
            if (body.length == 1) return true;
            if ((body[1] & 0xFF) != 0xBB) return false;
            if (body.length == 2) return true;
            if ((body[2] & 0xFF) != 0xBF) return false;
            index = 3;
        }
        while (index < body.length
                && (body[index] == ' ' || body[index] == '\r' || body[index] == '\n' || body[index] == '\t')) index++;
        byte[] marker = {'#', 'E', 'X', 'T', 'M', '3', 'U'};
        int available = body.length - index;
        int compared = Math.min(available, marker.length);
        for (int i = 0; i < compared; i++) if (body[index + i] != marker[i]) return false;
        return true;
    }

    private static byte[] combine(byte[] first, byte[] second) {
        ByteArrayOutputStream output = new ByteArrayOutputStream(first.length + second.length);
        output.write(first, 0, first.length);
        output.write(second, 0, second.length);
        return output.toByteArray();
    }

    private static void recordAndNotify(Uri uri, HlsAdblockPipeline.Outcome outcome) {
        if (!outcome.structured() && !outcome.legacy()) return;
        long fallbackCount = outcome.legacy() ? Math.max(1, outcome.removedSegments()) : 0;
        AdBlockStatsStore.recordBlocks(uri.getHost(), "EXO", outcome.ruleCounts(), fallbackCount,
                uri.getHost(), outcome.removedDurationSec(), outcome.removedSegmentDetails());
        if (!HlsAdblockNotice.shouldNotify(uri.toString(), System.currentTimeMillis())) return;
        String message = notice(outcome);
        App.post(() -> Notify.show(message));
    }

    private static String notice(HlsAdblockPipeline.Outcome outcome) {
        int removed = outcome.removedSegments() > 0 ? outcome.removedSegments() : (outcome.legacy() ? 1 : 0);
        return HlsAdblockNotice.message(removed, outcome.removedDurationSec());
    }

    record Result(String manifest, boolean changed, String notice) {}

    static final class Factory implements DataSource.Factory {
        private final DataSource.Factory upstreamFactory;

        Factory(DataSource.Factory upstreamFactory) {
            this.upstreamFactory = upstreamFactory;
        }

        @Override
        public DataSource createDataSource() {
            return new ExoHlsAdblockDataSource(upstreamFactory.createDataSource());
        }
    }
}
