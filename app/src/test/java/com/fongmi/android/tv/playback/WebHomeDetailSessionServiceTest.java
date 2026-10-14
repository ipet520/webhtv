package com.fongmi.android.tv.playback;

import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.db.AppDatabase;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import java.lang.reflect.Field;
import android.app.Activity;
import android.content.Intent;
import org.robolectric.Robolectric;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowActivity;
import com.fongmi.android.tv.ui.activity.VideoActivity;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 28, application = WebHomeHistoryServiceTest.FixtureApp.class)
public class WebHomeDetailSessionServiceTest extends WebHomeHistoryServiceTest {
    @Before @Override public void setup() throws Exception { super.setup(); WebHomeDetailSessionService.clear(); }
    @After public void clearSessions() { WebHomeDetailSessionService.clear(); }
    private JsonObject request(String id, String requestId, boolean resume) {
        JsonObject p = new JsonObject(); p.addProperty("version",1); p.addProperty("operation","play");
        p.addProperty("sessionId",id); p.addProperty("requestId",requestId); p.addProperty("mediaKey","movie|42|Example"); p.addProperty("resume",resume);
        JsonObject route = new JsonObject(); route.addProperty("siteKey","B"); route.addProperty("vodId","vod"); route.addProperty("entry","native-vod");
        route.addProperty("title","Example"); route.addProperty("pic",""); p.add("route",route); return p;
    }
    private History history(String site, long position) {
        History h = new History(); h.setCid(7); h.setKey(site+"@@@vod@@@7"); h.setVodName("Example");
        h.setVodFlag("lineB"); h.setSourceBindingKey("lineB#1"); h.setEpisodeUrl("episodeB"); h.setPosition(position); h.setDuration(500000);
        h.setMediaType("movie"); h.setTmdbId(42); AppDatabase.get().getHistoryDao().insertOrUpdate(h); return h;
    }
    @Test public void firstAcceptanceNeverUsesPersistedA() {
        history("A",90000); history("B",40000);
        var accepted=WebHomeDetailSessionService.accept(request("session_01","request_01",false));
        assertTrue(accepted.launch()); assertFalse(accepted.session().resume);
        assertFalse(WebHomeDetailSessionService.read("session_01").get("canResume").getAsBoolean());
        assertEquals(2,AppDatabase.get().getHistoryDao().findAll().size());
    }
    @Test public void resumeIsBoundToBDespiteNewerA() {
        History b=history("B",50000); history("A",90000);
        var accepted=WebHomeDetailSessionService.accept(request("session_01","request_01",false));
        WebHomeDetailSessionService.bind("session_01","request_01",b);
        var resumed=WebHomeDetailSessionService.accept(request("session_01","request_02",true));
        assertEquals(b.getKey(),WebHomeDetailSessionService.resumeHistory(resumed.session()).getKey());
        assertEquals(50000,WebHomeDetailSessionService.resumeHistory(resumed.session()).getPosition());
    }
    @Test public void zeroProgressCannotPretendToResume() {
        History b=history("B",0); WebHomeDetailSessionService.accept(request("session_01","request_01",false));
        WebHomeDetailSessionService.bind("session_01","request_01",b);
        assertFalse(WebHomeDetailSessionService.read("session_01").get("canResume").getAsBoolean());
        assertThrows(IllegalArgumentException.class,()->WebHomeDetailSessionService.accept(request("session_01","request_02",true)));
    }
    @Test public void duplicateRequestDoesNotLaunchAgainAndCannotChangeRoute() {
        JsonObject p=request("session_01","request_01",false);
        assertTrue(WebHomeDetailSessionService.accept(p).launch()); assertFalse(WebHomeDetailSessionService.accept(p).launch());
        p.getAsJsonObject("route").addProperty("siteKey","A");
        assertThrows(IllegalArgumentException.class,()->WebHomeDetailSessionService.accept(p));
    }
    @Test public void closedSessionRejectsLateHandoffButPreservesHistory() {
        History b=history("B",50000); JsonObject p=request("session_01","request_01",false);
        WebHomeDetailSessionService.accept(p); WebHomeDetailSessionService.close("session_01");
        assertThrows(IllegalArgumentException.class,()->WebHomeDetailSessionService.accept(p));
        assertNotNull(History.find(7,b.getKey()));
    }
    @Test public void latePlayerCannotBindAfterNewRequest() {
        WebHomeDetailSessionService.accept(request("session_01","request_01",false));
        WebHomeDetailSessionService.accept(request("session_01","request_02",false));
        assertThrows(IllegalArgumentException.class,()->WebHomeDetailSessionService.bind("session_01","request_01",history("B",50000)));
        assertThrows(IllegalArgumentException.class,()->WebHomeDetailSessionService.accept(request("session_01","request_01",false)));
    }
    @Test public void changedOrDeletedEpisodeCannotRestoreAnotherLine() {
        History b=history("B",50000); WebHomeDetailSessionService.accept(request("session_01","request_01",false));
        WebHomeDetailSessionService.bind("session_01","request_01",b);
        History replaced=b.copy(); replaced.setEpisodeUrl("otherEpisode"); AppDatabase.get().getHistoryDao().insertOrUpdate(replaced);
        assertFalse(WebHomeDetailSessionService.read("session_01").get("canResume").getAsBoolean());
        AppDatabase.get().getHistoryDao().delete(7,replaced.getKey());
        assertThrows(IllegalArgumentException.class,()->WebHomeDetailSessionService.resumeHistory(WebHomeDetailSessionService.require("session_01","request_01")));
    }
    @Test public void expiryAndCidChangesRejectRecovery() throws Exception {
        var s=WebHomeDetailSessionService.accept(request("session_01","request_01",false)).session();
        Field expiry=s.getClass().getDeclaredField("expiresAt"); expiry.setAccessible(true); expiry.setLong(s,0);
        assertThrows(IllegalArgumentException.class,()->WebHomeDetailSessionService.read(s.id));
        WebHomeDetailSessionService.accept(request("session_02","request_02",false));
        Config config=new Config(); config.setId(8); VodConfig.get().config(config);
        assertThrows(IllegalArgumentException.class,()->WebHomeDetailSessionService.read("session_02"));
    }
    @Test public void actualNativeSourceAndEpisodeChangeRemainOwnedBySession() {
        History b=history("B",50000); WebHomeDetailSessionService.accept(request("session_01","request_01",false));
        WebHomeDetailSessionService.bind("session_01","request_01",b);
        b.setEpisodeUrl("episode2"); b.setVodFlag("line2"); b.setPosition(70000); AppDatabase.get().getHistoryDao().insertOrUpdate(b);
        assertEquals("episode2",WebHomeDetailSessionService.resumeHistory(WebHomeDetailSessionService.require("session_01","request_01")).getEpisodeUrl());
    }
    @Test public void realPlayerIntentSeparatesFreshFromOwnedItemResume() {
        history("A",90000); History b=history("B",50000);
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();
        WebHomeDetailSessionService.accept(request("session_01","request_01",false));
        VideoActivity.startDetailSession(activity,"session_01","request_01");
        ShadowActivity shadowActivity = Shadow.extract(activity);
        Intent fresh=shadowActivity.getNextStartedActivity();
        assertEquals("B",fresh.getStringExtra("key")); assertEquals("vod",fresh.getStringExtra("id"));
        assertFalse(fresh.getBooleanExtra("webhomeHistoryExact",false)); assertFalse(fresh.getBooleanExtra("tmdbMode",true));
        WebHomeDetailSessionService.bind("session_01","request_01",b);
        WebHomeDetailSessionService.accept(request("session_01","request_02",true));
        VideoActivity.startDetailSession(activity,"session_01","request_02");
        Intent resume=shadowActivity.getNextStartedActivity();
        assertEquals("B",resume.getStringExtra("key")); assertTrue(resume.getBooleanExtra("webhomeHistoryExact",false));
        assertEquals("session_01",resume.getStringExtra("webhomeDetailSessionId"));
    }
    @Test public void unrelatedNativeWorkCannotBecomeSessionOwner() {
        WebHomeDetailSessionService.accept(request("session_01","request_01",false));
        History other=history("B",50000); other.setTmdbId(99);
        assertThrows(IllegalArgumentException.class,()->WebHomeDetailSessionService.bind("session_01","request_01",other));
        History unknown=history("A",50000); unknown.setTmdbId(0);
        assertThrows(IllegalArgumentException.class,()->WebHomeDetailSessionService.bind("session_01","request_01",unknown));
        History owned=history("A",50000); WebHomeDetailSessionService.bind("session_01","request_01",owned);
        assertEquals(owned.getKey(),WebHomeDetailSessionService.resumeHistory(WebHomeDetailSessionService.require("session_01","request_01")).getKey());
    }
}
