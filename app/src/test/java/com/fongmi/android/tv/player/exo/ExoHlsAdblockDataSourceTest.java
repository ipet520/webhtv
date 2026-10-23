package com.fongmi.android.tv.player.exo;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import androidx.media3.common.C;
import androidx.media3.datasource.DataSource;
import androidx.media3.datasource.DataSpec;
import androidx.media3.datasource.TransferListener;

import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ExoHlsAdblockDataSourceTest {

    @Test
    public void identifiesOnlyHlsManifestRequests() {
        assertTrue(ExoHlsAdblockDataSource.isManifestUrl("https://example.test/live/index.m3u8"));
        assertTrue(ExoHlsAdblockDataSource.isManifestUrl("https://example.test/live/playlist.m3u8?token=1"));
        assertFalse(ExoHlsAdblockDataSource.isManifestUrl("https://example.test/live/segment.ts"));
        assertFalse(ExoHlsAdblockDataSource.isManifestUrl("https://example.test/live/key.bin"));
    }

    @Test
    public void identifiesExtensionlessHlsManifestFromResponseContentType() {
        assertTrue(ExoHlsAdblockDataSource.isManifestRequest(
                "https://example.test/playback/token",
                Map.of("Content-Type", List.of("application/vnd.apple.mpegurl; charset=utf-8"))));
        assertTrue(ExoHlsAdblockDataSource.isManifestRequest(
                "https://example.test/playback/token",
                Map.of("content-type", List.of("application/x-mpegURL"))));
        assertTrue(ExoHlsAdblockDataSource.isManifestRequest(
                "https://example.test/live/index.m3u8",
                Map.of("Content-Type", List.of("application/octet-stream"))));
        assertFalse(ExoHlsAdblockDataSource.isManifestRequest(
                "https://example.test/live/segment.ts",
                Map.of("Content-Type", List.of("video/mp2t"))));
        assertFalse(ExoHlsAdblockDataSource.isManifestRequest(
                "https://example.test/live/segment.m3u8?ts=encoded",
                Map.of("Content-Type", List.of("video/mp2t"))));
    }

    @Test
    public void boundedPlaylistProbeDoesNotBufferLargeSegmentBody() throws Exception {
        byte[] segment = new byte[256 * 1024];
        segment[0] = 0x47;
        segment[1] = 0x40;
        segment[2] = 0x11;
        ByteArrayDataSource upstream = new ByteArrayDataSource(segment,
                Map.of("Content-Type", List.of("application/octet-stream")));

        byte[] probe = ExoHlsAdblockDataSource.readProbe(upstream);
        assertTrue(probe.length > 0);
        assertTrue(probe.length <= ExoHlsAdblockDataSource.MANIFEST_PROBE_BYTES);
        assertEquals(probe.length, upstream.bytesRead);
        assertArrayEquals(Arrays.copyOf(segment, probe.length), probe);
        assertFalse(ExoHlsAdblockDataSource.looksLikePlaylist(probe));

        byte[] remainder = new byte[segment.length - probe.length];
        int total = 0;
        int count;
        while ((count = upstream.read(remainder, total, remainder.length - total)) != C.RESULT_END_OF_INPUT) total += count;
        assertEquals(remainder.length, total);
        assertArrayEquals(Arrays.copyOfRange(segment, probe.length, segment.length), remainder);
    }

    @Test
    public void playlistProbeHandlesShortNetworkReads() throws Exception {
        byte[] manifest = "#EXTM3U\n#EXTINF:1,\nsegment.ts".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ByteArrayDataSource upstream = new ByteArrayDataSource(manifest,
                Map.of("Content-Type", List.of("application/octet-stream")), 2);

        byte[] probe = ExoHlsAdblockDataSource.readProbe(upstream);

        assertTrue(ExoHlsAdblockDataSource.looksLikePlaylist(probe));
        assertEquals(8, upstream.bytesRead);
    }

    @Test
    public void playlistSniffAcceptsBomAndLeadingWhitespace() {
        assertTrue(ExoHlsAdblockDataSource.looksLikePlaylist(
                "  \r\n#EXTM3U\n#EXTINF:1,\nsegment.ts".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    @Test
    public void changedOutcomeProducesPlaybackNoticeText() {
        String manifest = "#EXTM3U\n#EXTINF:5.0,\nad/001.ts\n#EXTINF:8.0,\nvideo/002.ts\n#EXTINF:8.0,\nvideo/003.ts\n#EXTINF:8.0,\nvideo/004.ts\n#EXT-X-ENDLIST\n";
        ExoHlsAdblockDataSource.Result result = ExoHlsAdblockDataSource.cleanForTest(
                "https://example.test/live/index.m3u8", manifest);

        assertTrue(result.changed());
        assertEquals("已跳过 1 个广告片段，总广告时长 5.0 秒", result.notice());
    }

    private static final class ByteArrayDataSource implements DataSource {
        private final byte[] data;
        private final Map<String, List<String>> headers;
        private final int maxRead;
        private int position;
        private int bytesRead;

        private ByteArrayDataSource(byte[] data, Map<String, List<String>> headers) {
            this(data, headers, Integer.MAX_VALUE);
        }

        private ByteArrayDataSource(byte[] data, Map<String, List<String>> headers, int maxRead) {
            this.data = data;
            this.headers = headers;
            this.maxRead = maxRead;
        }

        @Override public void addTransferListener(TransferListener transferListener) {}

        @Override
        public long open(DataSpec dataSpec) {
            position = 0;
            bytesRead = 0;
            return data.length;
        }

        @Override
        public int read(byte[] target, int offset, int length) {
            if (position >= data.length) return C.RESULT_END_OF_INPUT;
            int count = Math.min(Math.min(length, maxRead), data.length - position);
            System.arraycopy(data, position, target, offset, count);
            position += count;
            bytesRead += count;
            return count;
        }

        @Override public android.net.Uri getUri() { return null; }

        @Override public Map<String, List<String>> getResponseHeaders() { return headers; }

        @Override public void close() throws IOException {}
    }
}
