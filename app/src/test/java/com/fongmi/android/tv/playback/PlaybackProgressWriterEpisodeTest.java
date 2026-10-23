package com.fongmi.android.tv.playback;

import com.fongmi.android.tv.bean.History;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PlaybackProgressWriterEpisodeTest {

    @Test
    public void sameEpisodeUsesPersistedTmdbPositionWhenIncomingSourceHasOnlyTitle() {
        History local = history("源站第20集", "", 2, 20);
        PlaybackProgressInput input = input("20. 新标题", "");

        assertTrue(PlaybackProgressWriter.isSameEpisode(local, input));
    }

    @Test
    public void emptyUrlsDoNotMakeDifferentEpisodeTitlesLookIdentical() {
        History local = history("源站第20集", "", 2, 20);
        PlaybackProgressInput input = input("源站第21集", "");

        assertFalse(PlaybackProgressWriter.isSameEpisode(local, input));
    }

    @Test
    public void sourceBindingIsCopiedIntoPersistedHistory() {
        History history = new History();
        PlaybackProgressInput input = new PlaybackProgressInput();
        input.sourceBindingKey = "stable-interface-key";

        PlaybackProgressWriter.applySourceBindingKey(history, input);

        assertTrue("stable-interface-key".equals(history.getSourceBindingKey()));
        assertTrue("stable-interface-key".equals(history.copy().getSourceBindingKey()));
    }

    @Test
    public void matchingFingerprintPreservesLocalUrlWhenRemotePayloadOmitsIt() {
        String url = "https://cdn.example.com/ep01.m3u8";
        History local = history("源站第1集", url, -1, -1);
        PlaybackProgressInput input = input("源站第1集", "");
        input.episodeId = PlaybackRecord.episodeId(url);

        assertTrue(PlaybackProgressWriter.sameEpisodeFingerprint(local, input));
        assertEquals(url, PlaybackProgressWriter.resolveEpisodeUrl(local, input, false));
    }

    @Test
    public void differentOrOneSidedFingerprintDoesNotPreserveLocalUrl() {
        History local = history("源站第1集", "https://cdn.example.com/ep01.m3u8", -1, -1);
        PlaybackProgressInput different = input("源站第2集", "");
        different.episodeId = PlaybackRecord.episodeId("https://cdn.example.com/ep02.m3u8");
        PlaybackProgressInput missing = input("源站第1集", "");

        assertFalse(PlaybackProgressWriter.sameEpisodeFingerprint(local, different));
        assertFalse(PlaybackProgressWriter.sameEpisodeFingerprint(local, missing));
        assertEquals("", PlaybackProgressWriter.resolveEpisodeUrl(local, different, false));
    }

    @Test
    public void sameEpisodeFallbackKeepsUrlAndExplicitIncomingUrlWins() {
        String oldUrl = "https://cdn.example.com/old.m3u8";
        History local = history("源站第1集", oldUrl, -1, -1);

        assertEquals(oldUrl, PlaybackProgressWriter.resolveEpisodeUrl(local, input("源站第1集", ""), true));
        assertEquals("https://cdn.example.com/new.m3u8",
                PlaybackProgressWriter.resolveEpisodeUrl(local, input("源站第1集", "https://cdn.example.com/new.m3u8"), false));
        assertEquals("", PlaybackProgressWriter.resolveEpisodeUrl(null, input("源站第1集", ""), false));
    }

    private static History history(String remarks, String url, int season, int episode) {
        History history = new History();
        history.setVodRemarks(remarks);
        history.setEpisodeUrl(url);
        history.setTmdbSeasonNumber(season);
        history.setTmdbEpisodeNumber(episode);
        return history;
    }

    private static PlaybackProgressInput input(String episodeName, String episodeUrl) {
        PlaybackProgressInput input = new PlaybackProgressInput();
        input.episodeName = episodeName;
        input.episodeUrl = episodeUrl;
        return input;
    }
}
