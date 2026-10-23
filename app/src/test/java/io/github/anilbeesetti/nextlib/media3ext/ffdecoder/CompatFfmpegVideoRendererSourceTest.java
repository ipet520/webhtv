package io.github.anilbeesetti.nextlib.media3ext.ffdecoder;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

public class CompatFfmpegVideoRendererSourceTest {

    private static final String SOURCE_PATH = "app/src/main/java/io/github/anilbeesetti/nextlib/media3ext/ffdecoder/CompatFfmpegVideoRenderer.java";
    private static final String EXO_UTIL_PATH = "app/src/main/java/com/fongmi/android/tv/player/exo/ExoUtil.java";

    /**
     * The audio renderer may ask whether the platform supports the exact {@link
     * androidx.media3.common.Format}, but the video renderer must not: a 4K HEVC track that exceeds
     * the decoder's level reports unsupported, which would hand the track back to FFmpeg software
     * decoding and reintroduce the dropped-frame regression.
     */
    @Test
    public void platformOwnershipCheck_asksOnlyAboutMimeTypeNotExactFormat() throws Exception {
        String source = readSource();

        assertTrue(source.contains("getDecoderInfos(mimeType, false, false)"));
        assertFalse(source.contains("isFormatSupported("));
    }

    /**
     * The check must run through the selector the MediaCodec renderers were built with. Hardcoding
     * {@code MediaCodecSelector.DEFAULT} would make FFmpeg yield to a software platform decoder that
     * ExoUtil's hardware-only selector has already filtered out, leaving the track with no renderer
     * at all: a silent black screen with no PlaybackException to trigger any fallback.
     */
    @Test
    public void platformOwnershipCheck_usesTheInjectedSelectorNotTheDefaultOne() throws Exception {
        String source = readSource();

        assertTrue(source.contains("platformDecoderSelector == null ? MediaCodecSelector.DEFAULT : platformDecoderSelector"));
        assertTrue(source.contains("!selector.getDecoderInfos(mimeType, false, false).isEmpty()"));
    }

    @Test
    public void hardModeRegistersFfmpegAfterPlatformOnlyWhenFallbackIsEnabled() throws Exception {
        String exoUtil = readSource(EXO_UTIL_PATH);

        int start = exoUtil.indexOf("private static int getVideoRenderMode(int decode)");
        assertTrue("getVideoRenderMode must exist", start >= 0);
        int end = exoUtil.indexOf("\n    }", start);
        String mode = exoUtil.substring(start, end > start ? end : exoUtil.length());
        String normalized = exoUtil.replaceAll("\\s+", " ");

        assertTrue(mode, mode.contains("PlaybackPerformanceSetting.isDecoderFallbackEnabled()"));
        assertTrue(mode, mode.contains("EXTENSION_RENDERER_MODE_ON"));
        assertTrue(mode, mode.contains("EXTENSION_RENDERER_MODE_OFF"));
        assertTrue(normalized.contains("decoderRuntimeSession != null && videoRenderMode != EXTENSION_RENDERER_MODE_PREFER"));
        assertTrue(exoUtil.contains("if (videoRenderMode == EXTENSION_RENDERER_MODE_PREFER) return mediaCodecSelector;"));
        assertTrue(exoUtil.contains("if (info.hardwareAccelerated) hardwareInfos.add(info);"));
        assertTrue(exoUtil.contains("if (videoRenderMode == EXTENSION_RENDERER_MODE_OFF) return;"));
        assertTrue(exoUtil.contains("buildFfmpegVideoRenderer("));
        assertTrue("Preserve the existing 5 ms late-drop contract",
                exoUtil.contains("ENHANCED_LATE_THRESHOLD_TO_DROP_INPUT_US = 5_000L;"));
    }

    private static String readSource() throws Exception {
        return readSource(SOURCE_PATH);
    }

    private static String readSource(String relativePath) throws Exception {
        File root = new File(System.getProperty("user.dir"));
        while (root != null && !new File(root, relativePath).isFile()) root = root.getParentFile();
        assertNotNull("Unable to locate project root from " + System.getProperty("user.dir"), root);
        return Files.readString(new File(root, relativePath).toPath(), StandardCharsets.UTF_8);
    }
}
