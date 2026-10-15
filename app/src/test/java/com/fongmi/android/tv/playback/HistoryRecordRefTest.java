package com.fongmi.android.tv.playback;

import com.fongmi.android.tv.bean.History;
import org.junit.Test;
import static org.junit.Assert.*;

public class HistoryRecordRefTest {
    private History history() {
        History history=new History(); history.setKey("push_agent@@@https://fixture.test/p?a=1&b=<2>@@@7");
        history.setCid(7); history.setMediaType("tv"); history.setTmdbId(42); history.setTmdbEpisodePosition(0,1);
        return history;
    }
    @Test public void tupleEscapingMatchesJavascriptForPushUrlsAndDoesNotMergeConfigs() {
        HistoryRecordRef ref=HistoryRecordRef.from(history(),"ITEM");
        assertTrue(ref.displayId().contains("a=1&b=<2>")); assertFalse(ref.displayId().contains("\\u0026"));
        String first=ref.displayId(); ref.cid=8; assertNotEquals(first,ref.displayId());
    }
    @Test public void physicalAndSeasonReferencesHaveDifferentDisplayIdsAndPayloads() {
        History row=history(); HistoryRecordRef item=HistoryRecordRef.from(row,"ITEM"),season=HistoryRecordRef.from(row,"SEASON");
        assertNotEquals(item.displayId(),season.displayId()); assertEquals(row.getKey(),item.resumePayload(row));
        assertEquals(HistoryResumePayload.encode(row),season.resumePayload(row)); assertTrue(season.isValid());
        String zero=season.displayId(); season.seasonNumber=1; assertNotEquals(zero,season.displayId());
    }
    @Test public void unknownSeasonCannotBecomeSpecialSeasonZero() {
        History row=history(); row.setTmdbEpisodePositionWithUnknownSeason(0);
        HistoryRecordRef ref=HistoryRecordRef.from(row,"SEASON"); assertEquals(-1,ref.seasonNumber); assertFalse(ref.isValid());
    }
    @Test public void noTmdbStillHasExactPersistentIdentity() {
        History row=history(); row.setTmdbId(0); row.setMediaType(""); HistoryRecordRef ref=HistoryRecordRef.from(row,"ITEM");
        assertTrue(ref.isValid()); assertTrue(ref.matches(row));
    }
}
