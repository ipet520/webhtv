package com.fongmi.android.tv.server.process;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/** Keeps the local HLS proxy fallback controlled by the master switch and its dedicated toggle. */
public class M3u8LegacyFallbackTest {

    @Test
    public void rewrittenChildrenUseTheCurrentlySelectedNanoPort() {
        int previousPort = com.github.catvod.Proxy.getPort();
        try {
            com.github.catvod.Proxy.set(9987);
            assertEquals(
                    "http://127.0.0.1:9987/m3u8?url=https%3A%2F%2Fcdn.example%2Fmaster.m3u8",
                    new M3u8().proxy("https://cdn.example/master.m3u8"));
        } finally {
            com.github.catvod.Proxy.set(previousPort);
        }
    }

    @Test
    public void proxyUsesLegacyFallbackWithoutRequiringStructuredRules() throws Exception {
        String source = Files.readString(Path.of("src/main/java/com/fongmi/android/tv/server/process/M3u8.java"));

        assertFalse(source.contains("hlsRules(), true"));
        assertFalse(source.contains("rules, true"));
        assertTrue(source.contains("legacyFallback = HlsRuleConfig.isLegacyFallbackEnabled()"));
        assertFalse(source.contains("legacyFallback = !rules.isEmpty()"));
    }
}
