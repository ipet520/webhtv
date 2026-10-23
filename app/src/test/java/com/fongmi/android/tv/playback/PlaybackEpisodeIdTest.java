package com.fongmi.android.tv.playback;

import com.google.gson.JsonObject;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaybackEpisodeIdTest {

    @Test
    public void urlFingerprintIsStableAndDoesNotHashAnEmptyUrl() {
        String url = "https://cdn.example.com/ep01.m3u8?sign=opaque-token";
        String id = PlaybackRecord.episodeId(url);

        assertEquals(64, id.length());
        assertTrue(id.matches("[0-9a-f]{64}"));
        assertEquals(id, PlaybackRecord.episodeId(url));
        assertFalse(id.equals(PlaybackRecord.episodeId("https://cdn.example.com/ep02.m3u8")));
        assertEquals("", PlaybackRecord.episodeId(""));
        assertEquals("", PlaybackRecord.episodeId(null));
    }

    @Test
    public void apiSafeExposesFingerprintWithoutSignedUrl() {
        PlaybackRecord record = new PlaybackRecord();
        record.episodeUrl = "https://cdn.example.com/ep01.m3u8?sign=opaque-token";
        record.episodeId = PlaybackRecord.episodeId(record.episodeUrl);
        PlaybackFieldPolicy policy = PlaybackFieldPolicy.apiSafe();

        assertTrue(policy.includes("episodeId"));
        assertFalse(policy.includes("episodeUrl"));
        JsonObject json = record.toJson(policy);
        assertEquals(record.episodeId, json.get("episodeId").getAsString());
        assertFalse(json.has("episodeUrl"));
    }

    @Test
    public void apiOnlyFingerprintIsNotAddedToCustomWebhookFields() {
        WebhookConfig config = new WebhookConfig();
        config.fieldPreset = WebhookConfig.PRESET_CUSTOM;
        config.fields = java.util.List.of("episodeId");

        assertFalse(PlaybackFieldPolicy.webhook(config).includes("episodeId"));
    }

    @Test
    public void inputReadsFingerprintOrDerivesItFromLegacyUrl() {
        PlaybackProgressInput fromId = PlaybackProgressInput.fromJson(
                "{\"episodeId\":\"abc123\",\"siteKey\":\"site\",\"vodId\":\"vod\"}");
        assertEquals("abc123", fromId.episodeId);

        String url = "https://cdn.example.com/ep01.m3u8";
        PlaybackProgressInput fromUrl = PlaybackProgressInput.fromJson(
                "{\"episodeUrl\":\"" + url + "\",\"siteKey\":\"site\",\"vodId\":\"vod\"}");
        assertEquals(PlaybackRecord.episodeId(url), fromUrl.episodeId);
    }

    @Test
    public void constructionAndSerializationAreWiredToFingerprint() throws Exception {
        String record = readSource("app/src/main/java/com/fongmi/android/tv/playback/PlaybackRecord.java");
        assertTrue(record.contains("record.episodeId = episodeId(record.episodeUrl);"));
        assertTrue(record.contains("if (policy.includes(\"episodeId\") && !TextUtils.isEmpty(episodeId)) object.addProperty(\"episodeId\", episodeId);"));

        String writer = readSource("app/src/main/java/com/fongmi/android/tv/playback/PlaybackProgressWriter.java");
        assertTrue(writer.contains("history.setEpisodeUrl(resolveEpisodeUrl(local, input, sameEpisode));"));
    }

    private static String readSource(String relative) throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve(relative))) root = root.getParent();
        if (root == null) throw new IllegalStateException("Cannot locate source " + relative);
        return Files.readString(root.resolve(relative), StandardCharsets.UTF_8);
    }
}
