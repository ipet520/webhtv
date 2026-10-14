package com.fongmi.android.tv.playback;

import androidx.room.Room;
import com.fongmi.android.tv.App;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Config;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.TmdbSeasonProgress;
import com.fongmi.android.tv.db.AppDatabase;
import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.Init;
import com.github.catvod.utils.Prefers;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import java.lang.reflect.Field;
import java.util.List;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk = 28, application = WebHomeHistoryServiceTest.FixtureApp.class)
public class WebHomeHistoryServiceTest {
    public static class FixtureApp extends App {
        @Override public void onCreate() { Init.set(this); }
    }
    private AppDatabase database;
    @Before public void setup() throws Exception {
        Prefers.getPrefers().edit().clear().commit();
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase.class).allowMainThreadQueries().build();
        Field instance = AppDatabase.class.getDeclaredField("instance"); instance.setAccessible(true); instance.set(null, database);
        Config config = new Config(); config.setId(7); VodConfig.get().config(config);
        Site a = new Site(); a.setKey("A"); Site b = new Site(); b.setKey("B");
        Field sites = VodConfig.class.getDeclaredField("sites"); sites.setAccessible(true); sites.set(VodConfig.get(), List.of(a,b));
        ViewingRecordSyncStore.setEnabled(true); ViewingRecordSyncStore.setLocalWriteEnabled(true);
    }
    @After public void cleanup() throws Exception {
        database.close(); Field instance = AppDatabase.class.getDeclaredField("instance"); instance.setAccessible(true); instance.set(null, null);
    }
    private History row(String site, int cid, int season) {
        History h = new History(); h.setKey(site+"@@@vod@@@"+cid); h.setCid(cid); h.setVodName("Example");
        h.setMediaType(season < 0 ? "movie" : "tv"); h.setTmdbId(42);
        h.setTmdbEpisodePosition(season, 3); h.setPosition(50_000); h.setDuration(500_000);
        h.setVodFlag("line"); h.setEpisodeUrl("episode-"+season); h.setCreateTime(1000+cid+season);
        database.getHistoryDao().insertOrUpdate(h); return h;
    }
    private HistoryRecordRef ref(History row, String kind) {
        HistoryRecordRef ref = HistoryRecordRef.from(row, kind); ref.scopeToken = WebHomeHistoryService.scopeToken(ref); return ref;
    }
    private JsonObject delete(HistoryRecordRef... refs) {
        JsonObject request=new JsonObject(); request.addProperty("version",1);
        JsonArray list=new JsonArray(); for(HistoryRecordRef ref:refs)list.add(App.gson().toJsonTree(ref)); request.add("refs",list);
        return WebHomeHistoryService.delete(request);
    }
    private String outcome(JsonObject response) { return response.getAsJsonArray("results").get(0).getAsJsonObject().get("status").getAsString(); }
    @Test public void videoPushSourcesKeepExactResumeDespiteAudioEligibility() throws Exception {
        assertTrue(com.fongmi.android.tv.utils.AudioUtil.isAudioSiteEnabled(com.fongmi.android.tv.api.SiteApi.PUSH));
        String[] ids={"https://example.test/ordinary.mp4?a=1&b=2", "https://pan.quark.cn/s/exact-share", "https://example.test/pushed.m3u8"};
        JsonArray fixtures=new JsonArray();
        for (String id:ids) {
            History h=row("push_agent",7,-1); h.setKey("push_agent@@@"+id+"@@@7");
            h.setEpisodeUrl(id); h.setVodRemarks("正片"); h.setSourceBindingKey("line#0");
            database.getHistoryDao().insertOrUpdate(h);
            JsonObject record=null;
            JsonObject envelope=WebHomeHistoryService.read();
            for(var value:envelope.getAsJsonArray("records")) if(value.getAsJsonObject().get("key").getAsString().equals(h.getKey())) record=value.getAsJsonObject();
            assertNotNull(record); assertTrue(record.get("canResume").getAsBoolean());
            assertEquals("",record.get("capabilityReason").getAsString());
            History resolved=WebHomeHistoryService.resolve(ref(h,"ITEM"),true);
            assertEquals(h.getKey(),resolved.getKey()); assertEquals(id,resolved.getVodId());
            assertEquals(id,resolved.getEpisodeUrl()); assertEquals(50_000,resolved.getPosition());
            var activity=org.robolectric.Robolectric.buildActivity(android.app.Activity.class).setup().get();
            com.fongmi.android.tv.ui.activity.VideoActivity.startHistoryExact(activity,resolved,ref(h,"ITEM").resumePayload(resolved));
            org.robolectric.shadows.ShadowActivity shadow=org.robolectric.shadow.api.Shadow.extract(activity);
            android.content.Intent intent=shadow.getNextStartedActivity();
            assertEquals("push_agent",intent.getStringExtra("key")); assertEquals(id,intent.getStringExtra("id"));
            assertTrue(intent.getBooleanExtra("webhomeHistoryExact",false));
            fixtures.add(record.deepCopy());
        }
        String export=System.getenv("WEBHOME_MULTI_SOURCE_FIXTURE_PATH");
        if(export!=null&&!export.isEmpty()) java.nio.file.Files.writeString(java.nio.file.Path.of(export),fixtures.toString());
    }
    @Test public void pushExceptionKeepsExistingProgressSourceAndAudioGuards() {
        History h=row("push_agent",7,-1); h.setPosition(0); database.getHistoryDao().insertOrUpdate(h);
        assertFalse(WebHomeHistoryService.read().getAsJsonArray("records").get(0).getAsJsonObject().get("canResume").getAsBoolean());
        assertTrue(WebHomeHistoryService.read().getAsJsonArray("records").get(0).getAsJsonObject().get("canPlayExact").getAsBoolean());
        assertEquals(h.getKey(),WebHomeHistoryService.resolve(ref(h,"ITEM"),true).getKey());
        h.setPosition(50_000); h.setCid(8); h.setKey("push_agent@@@vod@@@8"); database.getHistoryDao().insertOrUpdate(h);
        Setting.putGlobalHistoryMode(Setting.GLOBAL_HISTORY_AUTO);
        assertThrows(IllegalArgumentException.class,()->WebHomeHistoryService.resolve(ref(h,"ITEM"),true));
        History audio=row("A",7,-1); Setting.putAudioConfig("{\"configured\":true,\"enabledSites\":[\"A\"]}");
        assertThrows(IllegalArgumentException.class,()->WebHomeHistoryService.resolve(ref(audio,"ITEM"),true));
        for(var record:WebHomeHistoryService.read().getAsJsonArray("records")) if(record.getAsJsonObject().get("key").getAsString().equals(audio.getKey()))
            assertFalse(record.getAsJsonObject().get("canResume").getAsBoolean());
    }
    @Test public void independentReadAndExactResumeDoNotFoldOrChooseNewerSource() {
        History a=row("A",7,-1), b=row("B",7,-1); b.setCreateTime(9000); database.getHistoryDao().insertOrUpdate(b);
        assertEquals(2,WebHomeHistoryService.read().getAsJsonArray("records").size());
        assertEquals(a.getKey(),WebHomeHistoryService.resolve(ref(a,"ITEM"),true).getKey());
        assertEquals(a.getPosition(),WebHomeHistoryService.resolve(ref(a,"ITEM"),true).getPosition());
    }
    @Test public void sameRowLineAndEpisodeChangesRemainOneItem() {
        History a=row("A",7,-1); a.setVodFlag("line2"); a.setEpisodeUrl("episode2"); database.getHistoryDao().insertOrUpdate(a);
        assertEquals(1,WebHomeHistoryService.read().getAsJsonArray("records").size());
    }
    @Test public void differentCidIsOnlyVisibleWithGlobalAndNeverPretendsPlayable() {
        row("A",7,-1); row("B",8,-1);
        assertEquals(1,WebHomeHistoryService.read().getAsJsonArray("records").size());
        Setting.putGlobalHistoryMode(Setting.GLOBAL_HISTORY_AUTO);
        assertEquals(2,WebHomeHistoryService.read().getAsJsonArray("records").size());
        try { WebHomeHistoryService.resolve(ref(History.find(8,"B@@@vod@@@8"),"ITEM"),true); fail(); }
        catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("原配置")); }
    }
    @Test public void validSeasonZeroProjectionHasSeparateDisplayIdentity() {
        History current=row("A",7,2); History special=current.copy(); special.setTmdbEpisodePosition(0,1);
        TmdbSeasonProgress snapshot=TmdbSeasonProgressStore.fromHistory(special); database.getTmdbSeasonProgressDao().insertOrUpdate(snapshot);
        JsonArray items=WebHomeHistoryService.read().getAsJsonArray("records"); assertEquals(2,items.size());
        assertNotEquals(items.get(0).getAsJsonObject().get("displayId"),items.get(1).getAsJsonObject().get("displayId"));
        assertEquals(0,WebHomeHistoryService.resolve(ref(special,"SEASON"),true).getTmdbSeasonNumber());
    }
    @Test public void changedSeasonOwnerRejectsExactReference() {
        History current=row("A",7,2); History special=current.copy(); special.setTmdbEpisodePosition(1,3);
        TmdbSeasonProgress snapshot=TmdbSeasonProgressStore.fromHistory(special); snapshot.sourceHistoryKey="B@@@vod@@@7";
        database.getTmdbSeasonProgressDao().insertOrUpdate(snapshot);
        try { WebHomeHistoryService.resolve(ref(special,"SEASON"),true); fail(); }
        catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("季度")); }
    }
    @Test public void exactItemDeletePreservesOtherSourceAndReadbackMatches() {
        History a=row("A",7,-1),b=row("B",7,-1);
        JsonObject result=delete(ref(b,"ITEM")); assertEquals("DELETED",outcome(result));
        assertNotNull(History.find(7,a.getKey())); assertNull(History.find(7,b.getKey()));
        assertEquals(1,result.getAsJsonObject("readback").getAsJsonArray("records").size());
    }
    @Test public void missingExactKeyDoesNotFallBackToSameSourceVariants() {
        History a=row("A",7,-1); HistoryRecordRef missing=ref(a,"ITEM");
        missing.historyKey="A@@@vod@@@7-missing";
        assertEquals("NOT_FOUND",outcome(delete(missing))); assertNotNull(History.find(7,a.getKey()));
    }
    @Test public void changedRecordAfterFreezeIsNotDeleted() {
        History a=row("A",7,-1); HistoryRecordRef frozen=ref(a,"ITEM");
        a.setPosition(60000); database.getHistoryDao().insertOrUpdate(a);
        assertEquals("FAILED",outcome(delete(frozen))); assertNotNull(History.find(7,a.getKey()));
    }
    @Test public void localPermissionFailureDoesNotWriteOrBypassSettings() {
        History a=row("A",7,-1); ViewingRecordSyncStore.setLocalWriteEnabled(false);
        assertEquals("FAILED",outcome(delete(ref(a,"ITEM")))); assertNotNull(History.find(7,a.getKey()));
        assertFalse(ViewingRecordSyncStore.isLocalWriteEnabled());
    }
    @Test public void physicalRowsWithoutProgressRemainVisibleAndCanStartExactPlayback() {
        History a=row("A",7,-1); a.setPosition(0); database.getHistoryDao().insertOrUpdate(a);
        JsonArray records=WebHomeHistoryService.read().getAsJsonArray("records");
        assertEquals(1,records.size()); assertFalse(records.get(0).getAsJsonObject().get("canResume").getAsBoolean());
        assertTrue(records.get(0).getAsJsonObject().get("canPlayExact").getAsBoolean());
        assertEquals(0,records.get(0).getAsJsonObject().get("position").getAsLong());
        assertEquals(a.getKey(),WebHomeHistoryService.resolve(ref(a,"ITEM"),true).getKey());
        assertEquals("DELETED",outcome(delete(ref(a,"ITEM"))));
        assertNull(History.find(7,a.getKey()));
    }
    @Test public void exactPlaybackClearsInvalidProgressOnlyOnPlaybackCopy() {
        History invalid=row("A",7,-1); invalid.setPosition(50_000); invalid.setDuration(0);
        History prepared=WebHomeHistoryService.prepareExactPlayback(invalid);
        assertNotSame(invalid,prepared); assertFalse(WebHomeHistoryService.played(prepared));
        assertTrue(prepared.getPosition() <= 0); assertTrue(prepared.getDuration() <= 0);
        assertEquals(50_000,invalid.getPosition()); assertEquals(0,invalid.getDuration());

        History valid=row("B",7,-1);
        History resumed=WebHomeHistoryService.prepareExactPlayback(valid);
        assertNotSame(valid,resumed); assertEquals(valid.getPosition(),resumed.getPosition());
        assertEquals(valid.getDuration(),resumed.getDuration()); assertTrue(WebHomeHistoryService.played(resumed));
    }
    @Test public void seasonDeletePreservesOtherQuarterAndPhysicalRoute() {
        History current=row("A",7,2);
        database.getTmdbSeasonProgressDao().insertOrUpdate(TmdbSeasonProgressStore.fromHistory(current));
        History first=current.copy(); first.setTmdbEpisodePosition(1,4);
        database.getTmdbSeasonProgressDao().insertOrUpdate(TmdbSeasonProgressStore.fromHistory(first));
        assertEquals("DELETED",outcome(delete(ref(first,"SEASON"))));
        assertNull(TmdbSeasonProgressStore.find(7,"tv",42,1));
        assertNotNull(TmdbSeasonProgressStore.find(7,"tv",42,2));
        assertNotNull(History.find(7,current.getKey()));
    }
    private JsonObject groupRequest() {
        JsonObject request=new JsonObject(); request.addProperty("version",1); request.addProperty("scope","GROUP"); request.addProperty("confirm",true);
        JsonArray refs=new JsonArray();
        for(com.google.gson.JsonElement item:WebHomeHistoryService.read().getAsJsonArray("records")) refs.add(item.getAsJsonObject().get("historyRecordRef"));
        request.add("refs",refs); return request;
    }
    @Test public void explicitlyFrozenGroupDeletesCompleteWorkAndNoOtherWork() {
        History a=row("A",7,-1),b=row("B",7,-1);
        History other=row("A",8,-1);
        JsonObject result=WebHomeHistoryService.delete(groupRequest());
        assertEquals(2,result.getAsJsonArray("results").size()); assertEquals("DELETED",outcome(result));
        assertNull(History.find(7,a.getKey())); assertNull(History.find(7,b.getKey())); assertNotNull(History.find(8,other.getKey()));
    }
    @Test public void newGroupMemberAfterFreezeRejectsWholeGroupWithoutDeletingIt() {
        History a=row("A",7,-1); JsonObject request=groupRequest(); History b=row("B",7,-1);
        try { WebHomeHistoryService.delete(request); fail(); }
        catch(IllegalArgumentException expected) { assertTrue(expected.getMessage().contains("作品组")); }
        assertNotNull(History.find(7,a.getKey())); assertNotNull(History.find(7,b.getKey()));
    }
    @Test public void exactSelectionRejectsMissingLineAndKeepsDuplicateLineBinding() {
        History history=row("A",7,-1); history.setVodRemarks("第3集");
        var first=new com.fongmi.android.tv.bean.Flag("line");
        first.getEpisodes().add(com.fongmi.android.tv.bean.Episode.create("第3集","other-url"));
        var second=new com.fongmi.android.tv.bean.Flag("line");
        second.getEpisodes().add(com.fongmi.android.tv.bean.Episode.create("第3集",history.getEpisodeUrl()));
        history.setSourceBindingKey("line#1");
        assertSame(second,WebHomeHistoryService.exactSelection(history,List.of(first,second)).flag());
        var replacement=new com.fongmi.android.tv.bean.Flag("another-line");
        replacement.getEpisodes().add(com.fongmi.android.tv.bean.Episode.create("第3集",history.getEpisodeUrl()));
        assertNull(WebHomeHistoryService.exactSelection(history,List.of(replacement)));
    }
    @Test public void legacyBridgeReadAndIndependentReadAreDifferentActualProviderCalls() throws Exception {
        row("A",7,-1); row("B",7,-1);
        var bridge=new com.fongmi.android.tv.web.HomeWebBridge(null,null,null,null);
        var method=com.fongmi.android.tv.web.HomeWebBridge.class.getDeclaredMethod("history",JsonObject.class); method.setAccessible(true);
        String legacy=(String)method.invoke(bridge,new JsonObject());
        assertTrue(com.google.gson.JsonParser.parseString(legacy).isJsonArray());
        JsonObject options=new JsonObject(); options.addProperty("mode","independent"); options.addProperty("version",1);
        JsonObject current=com.google.gson.JsonParser.parseString((String)method.invoke(bridge,options)).getAsJsonObject();
        assertEquals(1,current.get("version").getAsInt()); assertEquals(2,current.getAsJsonArray("records").size());
    }
    @Test public void differentWorksRemainVisibleAcrossProgressAndDurationBoundaries() throws Exception {
        History a=row("A",7,-1), b=row("B",7,-1);
        a.setVodName("Fixture work A"); database.getHistoryDao().insertOrUpdate(a);
        b.setVodName("Fixture work B"); b.setTmdbId(99);
        var bridge=new com.fongmi.android.tv.web.HomeWebBridge(null,null,null,null);
        var method=com.fongmi.android.tv.web.HomeWebBridge.class.getDeclaredMethod("history",JsonObject.class); method.setAccessible(true);
        JsonObject options=new JsonObject(); options.addProperty("mode","independent"); options.addProperty("version",1);
        JsonArray cases=new JsonArray();
        long[] positions={0,-1,50_000};
        long[] durations={500_000,0,androidx.media3.common.C.TIME_UNSET};
        for (long position:positions) for(long duration:durations) {
            b.setPosition(position); b.setDuration(duration); database.getHistoryDao().insertOrUpdate(b);
            JsonArray legacy=com.google.gson.JsonParser.parseString((String)method.invoke(bridge,new JsonObject())).getAsJsonArray();
            JsonObject independent=com.google.gson.JsonParser.parseString((String)method.invoke(bridge,options)).getAsJsonObject();
            System.out.println("HISTORY_LOSS_FIXTURE positionValid="+(position>0)+" durationValid="+(duration>0)
                    +" dao="+database.getHistoryDao().findAll(7).size()+" legacy="+legacy.size()
                    +" independent="+independent.getAsJsonArray("records").size());
            assertEquals(2,database.getHistoryDao().findAll(7).size()); assertEquals(2,legacy.size());
            assertEquals("independent list must not require a resume point",2,independent.getAsJsonArray("records").size());
            JsonObject recordB=null;
            for(var record:independent.getAsJsonArray("records")) if(record.getAsJsonObject().get("tmdbId").getAsInt()==99) recordB=record.getAsJsonObject();
            assertNotNull(recordB); assertEquals(position>0&&duration>0,recordB.get("canResume").getAsBoolean());
            assertTrue("exact physical row may be opened independently of its resume point",recordB.get("canPlayExact").getAsBoolean());
            assertNotEquals(independent.getAsJsonArray("records").get(0).getAsJsonObject().get("displayId"),
                    independent.getAsJsonArray("records").get(1).getAsJsonObject().get("displayId"));
            if(position<=0||duration<=0) {
                assertEquals(b.getKey(),WebHomeHistoryService.resolve(ref(b,"ITEM"),true).getKey());
                assertFalse(WebHomeHistoryService.played(WebHomeHistoryService.prepareExactPlayback(b)));
            }
            JsonObject entry=new JsonObject(); entry.addProperty("case", "position_"+position+"_duration_"+duration);
            entry.add("legacy",legacy); entry.add("independent",independent); cases.add(entry);
        }
        String export=System.getenv("WEBHOME_HISTORY_LOSS_FIXTURE_PATH");
        if(export!=null&&!export.isEmpty()) java.nio.file.Files.writeString(java.nio.file.Path.of(export),cases.toString());
    }
    @Test public void missingTmdbAndUnknownProgressPreservePhysicalIdentityAndSafeDelete() {
        History a=row("A",7,-1),b=row("B",7,-1);
        b.setVodName("Different work without metadata"); b.setTmdbId(0); b.setMediaType("");
        b.resetPlaybackPosition(); database.getHistoryDao().insertOrUpdate(b);
        assertEquals(2,History.get().size());
        JsonArray records=WebHomeHistoryService.read().getAsJsonArray("records"); assertEquals(2,records.size());
        JsonObject recordB=null;
        for(var record:records) if(record.getAsJsonObject().get("key").getAsString().equals(b.getKey())) recordB=record.getAsJsonObject();
        assertNotNull(recordB); assertFalse(recordB.get("canResume").getAsBoolean());
        assertTrue(recordB.get("canDelete").getAsBoolean());
        assertEquals("DELETED",outcome(delete(ref(b,"ITEM"))));
        assertNotNull(History.find(7,a.getKey())); assertNull(History.find(7,b.getKey()));
    }
}
