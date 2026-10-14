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
    @Test public void metadataOnlyRowsAreNotPretendPlaybackHistory() {
        History a=row("A",7,-1); a.setPosition(0); database.getHistoryDao().insertOrUpdate(a);
        assertEquals(0,WebHomeHistoryService.read().getAsJsonArray("records").size());
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
}
