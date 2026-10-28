package com.fongmi.android.tv.ui.home;

import android.app.Application;
import com.fongmi.android.tv.bean.*;
import com.fongmi.android.tv.setting.Setting;
import com.github.catvod.Init;
import com.github.catvod.utils.Prefers;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class)
public class NativeHomeHeroTest {
    NativeHomeHero hero;
    @Before public void setup(){Init.set(RuntimeEnvironment.getApplication());Prefers.getPrefers().edit().clear().commit();Setting.putTmdbConfig("{\"apiKey\":\"test-only-key\",\"language\":\"zh-CN\"}");}
    @After public void close(){if(hero!=null)hero.clearFocus();}
    static Vod vod(String id){Vod vod=new Vod();vod.setId(id);vod.setName(id);return vod;}
    static void dwell(){ShadowLooper.idleMainLooper(181,TimeUnit.MILLISECONDS);}
    static void finish(CountDownLatch done) throws Exception {assertTrue(done.await(3,TimeUnit.SECONDS));for(int i=0;i<20;i++){Thread.sleep(5);ShadowLooper.idleMainLooper();}}
    @Test public void rapidFocusOnlyStartsLastFilmAndRunsOffMainThread() throws Exception {
        AtomicInteger calls=new AtomicInteger();CountDownLatch done=new CountDownLatch(1);List<String> results=new ArrayList<>();
        hero=new NativeHomeHero((site,vod,config)->{assertNotEquals(android.os.Looper.getMainLooper().getThread(),Thread.currentThread());calls.incrementAndGet();done.countDown();return new NativeHomeHero.Info(vod.getId(),"","","");});
        Site site=Site.get("film","影视");hero.focus(site,vod("A"),i->results.add(i.metadata()));hero.focus(site,vod("B"),i->results.add(i.metadata()));hero.focus(site,vod("C"),i->results.add(i.metadata()));
        assertEquals(0,calls.get());dwell();finish(done);assertEquals(1,calls.get());assertEquals(List.of("C"),results);
    }
    @Test public void inFlightABAUsesOneRequestAndOldSubscriberCannotPublish() throws Exception {
        AtomicInteger calls=new AtomicInteger();CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(1);List<String> results=new ArrayList<>();
        hero=new NativeHomeHero((site,vod,config)->{calls.incrementAndGet();entered.countDown();release.await(3,TimeUnit.SECONDS);done.countDown();return new NativeHomeHero.Info(vod.getId(),"","","");});
        Site site=Site.get("film","影视");hero.focus(site,vod("A"),i->results.add("old"));dwell();assertTrue(entered.await(3,TimeUnit.SECONDS));
        hero.focus(site,vod("B"),i->results.add("B"));hero.focus(site,vod("A"),i->results.add(i.metadata()));dwell();release.countDown();finish(done);
        assertEquals(1,calls.get());assertEquals(List.of("A"),results);
    }
    @Test public void scopeChangeAndDetachRejectSlowResults() throws Exception {
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1),done=new CountDownLatch(1);List<String> results=new ArrayList<>();
        hero=new NativeHomeHero((site,vod,config)->{entered.countDown();release.await(3,TimeUnit.SECONDS);done.countDown();return new NativeHomeHero.Info("stale","","","");});
        hero.focus(Site.get("film","影视"),vod("A"),i->results.add(i.metadata()));dwell();assertTrue(entered.await(3,TimeUnit.SECONDS));Setting.putTmdbConfig("{\"apiKey\":\"changed\",\"language\":\"en-US\"}");release.countDown();finish(done);assertTrue(results.isEmpty());
        hero.clearFocus();
    }
    @Test public void unavailableOrNonFilmDoesNotStartBackend(){AtomicInteger calls=new AtomicInteger();hero=new NativeHomeHero((s,v,c)->{calls.incrementAndGet();return null;});Setting.putTmdbConfig("{}");hero.focus(Site.get("film","影视"),vod("A"),i->fail());dwell();assertEquals(0,calls.get());}
    @Test public void completedABABAUsesMemoButCredentialAndManualChangesInvalidate() throws Exception {
        AtomicInteger calls=new AtomicInteger();List<String> results=new ArrayList<>();
        hero=new NativeHomeHero((s,v,c)->{calls.incrementAndGet();return new NativeHomeHero.Info(v.getId(),"","wide-"+v.getId(),"");});
        Site site=Site.get("memo","影视");Vod a=vod("memo-A"),b=vod("memo-B");
        for(Vod v:List.of(a,b,a,b,a)){int count=results.size()+1;hero.focus(site,v,i->results.add(i.metadata()));dwell();awaitSize(results,count,v.getId());}
        assertEquals(5,results.size());assertEquals(2,calls.get());
        TmdbMatchCache cache=Setting.getTmdbMatchCache();cache.putManual("memo",a.getId(),List.of(a.getName()),new TmdbItem(321,"movie","手动绑定","","","",""),"zh-CN");Setting.putTmdbMatchCache(cache);
        hero.focus(site,a,i->results.add(i.metadata()));dwell();for(int i=0;i<100&&results.size()<6;i++){Thread.sleep(5);ShadowLooper.idleMainLooper();}assertEquals(3,calls.get());
        Setting.putTmdbConfig("{\"apiKey\":\"changed-key\",\"language\":\"en-US\"}");hero.focus(site,a,i->results.add(i.metadata()));dwell();for(int i=0;i<100&&results.size()<7;i++){Thread.sleep(5);ShadowLooper.idleMainLooper();}assertEquals(4,calls.get());
    }
    static void awaitSize(List<String> results,int count,String id) throws Exception {for(int i=0;i<200&&results.size()<count;i++){Thread.sleep(5);ShadowLooper.idleMainLooper();}assertEquals(count,results.size());assertEquals(id,results.get(results.size()-1));}
    @Test public void firstHeroStartsImmediatelyAndOnlyOneKnownNeighborMayPrepare() throws Exception {
        AtomicInteger full=new AtomicInteger(),known=new AtomicInteger();List<String> output=new ArrayList<>();
        hero=new NativeHomeHero(new NativeHomeHero.Backend(){public NativeHomeHero.Info load(Site s,Vod v,TmdbConfig c){full.incrementAndGet();return new NativeHomeHero.Info(v.getId(),"","wide-"+v.getId(),"");}public NativeHomeHero.Info loadKnown(Site s,Vod v,TmdbConfig c){known.incrementAndGet();return new NativeHomeHero.Info(v.getId(),"","wide-"+v.getId(),"");}});
        Site site=Site.get("prepare","影视");hero.focus(site,vod("A"),i->output.add(i.metadata()),()->{},()->{},true);
        awaitSize(output,1,"A");hero.prepareKnown(site,vod("B"),i->output.add(i.metadata()));hero.prepareKnown(site,vod("C"),i->fail());awaitSize(output,2,"B");assertEquals(1,full.get());assertEquals(1,known.get());
        hero.focus(site,vod("B"),i->output.add(i.metadata()));awaitSize(output,3,"B");assertEquals(1,full.get());assertEquals(1,known.get());
    }
    @Test public void knownMissDoesNotSuppressLaterFocusedMatchingAndMemoExpires() throws Exception {
        AtomicInteger full=new AtomicInteger(),known=new AtomicInteger();List<String> output=new ArrayList<>();Site site=Site.get("known-miss","影视");
        hero=new NativeHomeHero(new NativeHomeHero.Backend(){public NativeHomeHero.Info load(Site s,Vod v,TmdbConfig c){full.incrementAndGet();return new NativeHomeHero.Info(v.getId(),"","","");}public NativeHomeHero.Info loadKnown(Site s,Vod v,TmdbConfig c){known.incrementAndGet();return null;}});
        hero.prepareKnown(site,vod("B"),i->fail());for(int i=0;i<40;i++){Thread.sleep(5);ShadowLooper.idleMainLooper();}
        hero.prepareKnown(site,vod("B"),i->fail());for(int i=0;i<40;i++){Thread.sleep(5);ShadowLooper.idleMainLooper();}assertEquals(1,known.get());
        hero.focus(site,vod("B"),i->output.add(i.metadata()),()->{},()->{},true);awaitSize(output,1,"B");assertEquals(1,full.get());
        ShadowLooper.idleMainLooper(121000,TimeUnit.MILLISECONDS);hero.focus(site,vod("B"),i->output.add(i.metadata()));awaitSize(output,2,"B");assertEquals(2,full.get());
    }
    @Test public void failureIsOptionalAndDoesNotPublishWrongMetadata() throws Exception {
        CountDownLatch done=new CountDownLatch(1);List<String> results=new ArrayList<>();hero=new NativeHomeHero((s,v,c)->{done.countDown();throw new IllegalStateException("offline");});hero.focus(Site.get("film","影视"),vod("A"),i->results.add(i.metadata()));dwell();finish(done);assertTrue(results.isEmpty());
    }
    @Test public void embeddedTmdbUsesExistingAdapterWithoutNetworkAndHonorsManualIdentity() throws Exception {
        Setting.putTmdbConfig("{}");
        Vod vod=vod("source-1");
        vod.setTmdb(new com.google.gson.Gson().fromJson("""
            {"schema":1,"id":550,"media_type":"movie","language":"zh-CN","complete":["core","images"],
             "detail":{"id":550,"title":"影片","overview":"可靠的站源内嵌简介","release_date":"2024-01-01","vote_average":8.5,
              "backdrop_path":"/background.jpg","logo_path":"/logo.png","genres":[{"name":"剧情"}]}}
            """,TmdbSourcePayload.class));
        var backend=new NativeHomeHero.ExistingTmdb();var config=TmdbConfig.effectiveCurrent();
        var info=backend.load(Site.get("film","影视"),vod,config);
        assertNotNull(info);assertTrue(info.metadata().contains("2024"));assertTrue(info.metadata().contains("8.5"));assertTrue(info.backdrop().endsWith("/background.jpg"));assertTrue(info.logo().endsWith("/logo.png"));assertEquals("可靠的站源内嵌简介",info.overview());
        TmdbMatchCache cache=Setting.getTmdbMatchCache();cache.putManual("film",vod.getId(),List.of(vod.getName()),new TmdbItem(999,"tv","手动绑定","","","",""),"zh-CN");Setting.putTmdbMatchCache(cache);
        assertNull(backend.load(Site.get("film","影视"),vod,config)); // no credentials: never substitute source identity for a manual binding
    }
    @Test public void scopeHasStrongCredentialIdentityAndImageUrlsRemainBounded(){
        assertNotEquals(NativeHomeHero.scope(TmdbConfig.objectFrom("{\"apiKey\":\"Aa\"}")),NativeHomeHero.scope(TmdbConfig.objectFrom("{\"apiKey\":\"BB\"}")));
        assertEquals("https://images.test/t/p/w1280/photo.jpg",NativeHomeHero.ExistingTmdb.bounded("https://images.test/t/p/original/photo.jpg"));
        assertEquals("https://images.test/t/p/w1280/photo.jpg",NativeHomeHero.ExistingTmdb.bounded("https://images.test/t/p/w3000/photo.jpg"));
        assertEquals("https://images.test/t/p/w780/photo.jpg",NativeHomeHero.ExistingTmdb.bounded("https://images.test/t/p/w780/photo.jpg"));
        assertEquals("https://cdn.test/original-poster.jpg",NativeHomeHero.ExistingTmdb.bounded("https://cdn.test/original-poster.jpg"));
    }
    @Test public void embeddedIdentityWithMissingArtUsesExistingDetailChannelWithoutSearch() throws Exception {
        AtomicInteger requests=new AtomicInteger();
        var service=new com.fongmi.android.tv.service.TmdbService(){
            @Override public com.google.gson.JsonObject detail(TmdbItem item,TmdbConfig config,boolean related) {
                assertEquals(550,item.getTmdbId());assertFalse(related);requests.incrementAndGet();
                return com.google.gson.JsonParser.parseString("{\"id\":550,\"title\":\"影片\",\"overview\":\"详情简介\",\"backdrop_path\":\"/wide.jpg\",\"poster_path\":\"/poster.jpg\",\"images\":{\"logos\":[{\"iso_639_1\":\"zh\",\"file_path\":\"/logo.png\"}]}}").getAsJsonObject();
            }
        };
        Vod v=vod("embedded");v.setTmdb(new com.google.gson.Gson().fromJson("{\"schema\":1,\"id\":550,\"media_type\":\"movie\",\"language\":\"zh-CN\",\"complete\":[\"core\"],\"detail\":{\"id\":550,\"title\":\"影片\",\"overview\":\"内嵌简介\"}}",TmdbSourcePayload.class));
        var info=new NativeHomeHero.ExistingTmdb(service).load(Site.get("film","影视"),v,TmdbConfig.effectiveCurrent());
        assertEquals(1,requests.get());assertTrue(info.backdrop().endsWith("/wide.jpg"));assertTrue(info.logo().endsWith("/logo.png"));assertTrue(info.poster().endsWith("/poster.jpg"));
    }
    @Test public void failedEmbeddedArtEnrichmentPreservesSourceMetadata() throws Exception {
        var service=new com.fongmi.android.tv.service.TmdbService(){
            @Override public com.google.gson.JsonObject detail(TmdbItem item,TmdbConfig config,boolean related) throws Exception {throw new java.io.IOException("offline");}
        };
        Vod v=vod("embedded");v.setTmdb(new com.google.gson.Gson().fromJson("{\"schema\":1,\"id\":550,\"media_type\":\"movie\",\"language\":\"zh-CN\",\"complete\":[\"core\"],\"detail\":{\"id\":550,\"title\":\"影片\",\"overview\":\"内嵌简介\"}}",TmdbSourcePayload.class));
        var info=new NativeHomeHero.ExistingTmdb(service).load(Site.get("film","影视"),v,TmdbConfig.effectiveCurrent());
        assertNotNull(info);assertEquals("内嵌简介",info.overview());assertEquals("",info.backdrop());
    }
    @Test public void visibleWindowUnknownIdentityUsesExistingFullBackendThenFocusReusesMemo() throws Exception {
        AtomicInteger calls=new AtomicInteger();List<String> results=new ArrayList<>();
        hero=new NativeHomeHero((s,v,c)->{calls.incrementAndGet();return new NativeHomeHero.Info(v.getId(),"","wide-"+v.getId(),"");});
        Site site=Site.get("window","film");Vod item=vod("unknown");
        assertTrue(hero.prepareWindow(site,item,i->results.add(i.metadata())));
        awaitSize(results,1,"unknown");
        hero.focus(site,item,i->results.add(i.metadata()),()->{},()->{},true);
        awaitSize(results,2,"unknown");assertEquals(1,calls.get());
    }
    @Test public void windowPreparationHasOneSlotAndSurvivesUnrelatedFocusChanges() throws Exception {
        CountDownLatch entered=new CountDownLatch(1),release=new CountDownLatch(1);List<String> results=new ArrayList<>();
        hero=new NativeHomeHero((s,v,c)->{entered.countDown();release.await(3,TimeUnit.SECONDS);return new NativeHomeHero.Info(v.getId(),"","wide","");});
        Site site=Site.get("window","film");assertTrue(hero.prepareWindow(site,vod("A"),i->results.add(i.metadata())));
        assertTrue(entered.await(3,TimeUnit.SECONDS));assertFalse(hero.prepareWindow(site,vod("B"),i->fail()));
        hero.clearFocus();release.countDown();awaitSize(results,1,"A");
    }

}
