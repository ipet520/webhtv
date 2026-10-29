package com.fongmi.android.tv.ui.home;

import com.fongmi.android.tv.bean.Vod;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class)
public class NativeHomePreparationQueueTest {
    record Meta(Vod vod,Consumer<NativeHomeHero.Info> done){}
    record Image(String url,Consumer<Boolean> done){}
    List<Meta> metadata=new ArrayList<>();List<Image> images=new ArrayList<>();NativeHomePreparationQueue queue;Object context=new Object();
    @Before public void setup(){queue=new NativeHomePreparationQueue((vod,done)->{metadata.add(new Meta(vod,done));return true;},(url,done)->images.add(new Image(url,done)));}
    @After public void close(){queue.pause();}
    static Vod vod(String id){Vod vod=new Vod();vod.setId(id);vod.setName(id);return vod;}
    static NativeHomePreparationQueue.Candidate candidate(String id,int priority){return new NativeHomePreparationQueue.Candidate(vod(id),priority);}
    static NativeHomeHero.Info info(String id){return new NativeHomeHero.Info(id,"","wide-"+id,"");}
    static void advance(long ms){ShadowLooper.idleMainLooper(ms,TimeUnit.MILLISECONDS);}
    @Test public void firstVisibleWindowStartsWithoutFocusAndReadinessWaitsForActualImage(){
        queue.window(context,"site",List.of(candidate("A",0),candidate("B",1),candidate("C",2)));
        assertEquals(1,metadata.size());assertEquals("A",metadata.get(0).vod().getId());assertTrue(images.isEmpty());
        assertFalse(queue.focus(vod("A")));metadata.get(0).done().accept(info("A"));
        assertEquals("wide-A",images.get(0).url());assertFalse(queue.focus(vod("A")));
        images.get(0).done().accept(true);assertTrue(queue.focus(vod("A")));
        assertEquals(1,queue.preparedFocusCount());assertEquals(3,queue.focusCount());
    }
    @Test public void movingBothDirectionsReprioritizesPendingEntriesWithoutCancellingActiveWork(){
        queue.window(context,"site",List.of(candidate("A",0),candidate("B",1),candidate("C",2)));
        queue.window(context,"site",List.of(candidate("A",5),candidate("B",3),candidate("C",1)));
        assertEquals(1,metadata.size());metadata.get(0).done().accept(info("A"));images.get(0).done().accept(true);
        advance(750);assertEquals("C",metadata.get(1).vod().getId());
        queue.window(context,"site",List.of(candidate("A",2),candidate("B",0),candidate("C",5)));
        metadata.get(1).done().accept(info("C"));images.get(1).done().accept(true);advance(750);
        assertEquals("B",metadata.get(2).vod().getId());assertEquals(3,queue.queuedCount());
    }
    @Test public void budgetBoundsHugeListsAndRapidWindowChanges(){
        List<NativeHomePreparationQueue.Candidate> many=new ArrayList<>();for(int i=0;i<200;i++)many.add(candidate("film"+i,i));
        queue.window(context,"site",many);assertEquals(10,queue.queuedCount());assertEquals(1,metadata.size());
        for(int i=0;i<30;i++)queue.window(context,"site",List.of(candidate("last"+i,0)));
        assertEquals(1,queue.queuedCount());assertEquals(1,metadata.size());
        metadata.get(0).done().accept(info("film0"));advance(750);assertEquals("last29",metadata.get(1).vod().getId());assertTrue(images.isEmpty());
    }
    @Test public void oldCategoryMetadataCannotWarmNewCategoryAndABARestoresPreparedIdentity(){
        Object a=context,b=new Object();queue.window(a,"site",List.of(candidate("A",0)));
        queue.window(b,"site",List.of(candidate("B",0)));metadata.get(0).done().accept(info("A"));assertTrue(images.isEmpty());
        advance(750);metadata.get(1).done().accept(info("B"));images.get(0).done().accept(true);
        queue.window(a,"site",List.of(candidate("A",0)));advance(750);metadata.get(2).done().accept(info("A"));images.get(1).done().accept(true);
        int requests=metadata.size();queue.window(b,"site",List.of(candidate("B",0)));assertTrue(queue.focus(vod("B")));assertEquals(requests,metadata.size());
        queue.window(b,"different-credentials",List.of(candidate("B",0)));advance(750);assertEquals(requests+1,metadata.size());
    }
    @Test public void failureCooldownAndPausePreventInfiniteRetries(){
        queue.window(context,"site",List.of(candidate("A",0)));metadata.get(0).done().accept(null);
        for(int i=0;i<20;i++)queue.window(context,"site",List.of(candidate("A",0)));assertEquals(1,metadata.size());
        queue.pause();advance(31000);queue.window(context,"site",List.of(candidate("A",0)));assertEquals(1,metadata.size());
        queue.resume();assertEquals(2,metadata.size());metadata.get(1).done().accept(info("A"));images.get(0).done().accept(false);assertFalse(queue.focus(vod("A")));
    }
    @Test public void currentMetadataAndImagesHaveSeparateSingleBackgroundBudgets(){
        queue.window(context,"site",List.of(candidate("A",0),candidate("B",1),candidate("C",2)));
        metadata.get(0).done().accept(info("A"));advance(750);metadata.get(1).done().accept(info("B"));
        assertEquals(1,images.size());images.get(0).done().accept(true);assertEquals("wide-B",images.get(1).url());
        queue.currentMetadata(vod("C"),info("C"));assertEquals(2,images.size());images.get(1).done().accept(true);assertEquals("wide-C",images.get(2).url());
    }
    @Test public void controlledFocusArrivalMeasuresTwoPreparedHitsOutOfFour(){
        long start=android.os.SystemClock.uptimeMillis();queue.window(context,"site",List.of(candidate("A",0),candidate("B",1),candidate("C",2)));
        assertFalse(queue.focus(vod("A")));metadata.get(0).done().accept(info("A"));advance(50);images.get(0).done().accept(true);
        advance(200);assertTrue(queue.focus(vod("A")));advance(500);metadata.get(1).done().accept(info("B"));advance(50);images.get(1).done().accept(true);
        advance(100);assertTrue(queue.focus(vod("B")));assertFalse(queue.focus(vod("C")));
        assertEquals(2,queue.preparedFocusCount());assertEquals(4,queue.focusCount());
        System.out.println("09 controlled focus readiness: A@0ms=false, A@250ms=true lead=200ms, B@900ms=true lead=100ms, C@900ms=false; hits=2/4; elapsed="+(android.os.SystemClock.uptimeMillis()-start));
    }
    @Test public void synchronousCacheCallbacksStillRespectStartSpacing(){
        List<String> calls=new ArrayList<>();queue=new NativeHomePreparationQueue((vod,done)->{calls.add(vod.getId());done.accept(info(vod.getId()));return true;},(url,done)->done.accept(true));
        queue.window(context,"site",List.of(candidate("A",0),candidate("B",1)));
        assertEquals(List.of("A"),calls);advance(749);assertEquals(1,calls.size());advance(1);assertEquals(List.of("A","B"),calls);
    }

    @Test public void abcThenCachedAReleasesOldImageSlotAndOnlyWarmsCurrentCandidates(){
        Object a=new Object(),b=new Object(),c=new Object();
        queue.window(a,"site",List.of(candidate("A",0)));metadata.get(0).done().accept(info("A"));
        queue.window(b,"site",List.of(candidate("B",0)));advance(750);metadata.get(1).done().accept(info("B"));
        queue.window(c,"site",List.of(candidate("C",0)));advance(750);metadata.get(2).done().accept(info("C"));
        assertEquals(1,images.size());assertFalse(queue.focus(vod("C")));
        images.get(0).done().accept(true);assertEquals(2,images.size());assertEquals("wide-C",images.get(1).url());assertFalse(queue.focus(vod("C")));
        images.get(1).done().accept(true);assertTrue(queue.focus(vod("C")));
        int requests=metadata.size();queue.window(a,"site",List.of(candidate("A",0)));assertTrue(queue.focus(vod("A")));assertEquals(requests,metadata.size());
    }
    @Test public void unknownIdentityOrNoWideImageNeverCountsAsPreparedAndDoesNotSpin(){
        queue.window(context,"site",List.of(candidate("unknown",0),candidate("no-wide",1)));
        metadata.get(0).done().accept(null);advance(750);metadata.get(1).done().accept(new NativeHomeHero.Info("meta","overview","",""));
        assertTrue(images.isEmpty());assertFalse(queue.focus(vod("unknown")));assertFalse(queue.focus(vod("no-wide")));
        for(int i=0;i<20;i++)queue.window(context,"site",List.of(candidate("unknown",0),candidate("no-wide",1)));
        assertEquals(2,metadata.size());assertEquals(0,queue.preparedFocusCount());
    }
    @Test public void lateSameImageCompletionIsReusedAfterABAReturnWithoutDuplicatePrefetch(){
        Object a=new Object(),b=new Object();queue.window(a,"site",List.of(candidate("A",0)));
        metadata.get(0).done().accept(info("A"));queue.window(b,"site",List.of(candidate("B",0)));advance(750);
        queue.window(a,"site",List.of(candidate("A",0)));queue.currentMetadata(vod("A"),info("A"));
        assertFalse(queue.focus(vod("A")));assertEquals(1,images.size());
        images.get(0).done().accept(true);
        assertTrue("current A has the same verified URL and its image completed before focus",queue.focus(vod("A")));
        assertEquals("do not preload the same decoded URL a second time",1,images.size());
        metadata.get(1).done().accept(info("B"));advance(750);assertEquals(2,metadata.size());
    }
    @Test public void oldImageSuccessCannotMarkReplacementUrlPrepared(){
        queue.window(context,"site",List.of(candidate("A",0)));metadata.get(0).done().accept(info("A"));
        queue.currentMetadata(vod("A"),info("A-replacement"));images.get(0).done().accept(true);
        assertFalse(queue.focus(vod("A")));assertEquals(2,images.size());assertEquals("wide-A-replacement",images.get(1).url());
        images.get(1).done().accept(true);assertTrue(queue.focus(vod("A")));
    }
    @Test public void oldImageFailureDoesNotFailReplacementUrlOrBlockItsPreparation(){
        queue.window(context,"site",List.of(candidate("A",0)));metadata.get(0).done().accept(info("A"));
        queue.currentMetadata(vod("A"),info("A-replacement"));images.get(0).done().accept(false);
        assertEquals(2,images.size());assertEquals("wide-A-replacement",images.get(1).url());assertFalse(queue.focus(vod("A")));
        images.get(1).done().accept(true);assertTrue(queue.focus(vod("A")));
    }

    @Test public void optInLogoSharesOneImageSlotAndNeedsNoExtraMetadata(){
        List<Image> logos=new ArrayList<>();queue=new NativeHomePreparationQueue((vod,done)->{metadata.add(new Meta(vod,done));return true;},new NativeHomePreparationQueue.Images(){
            public void start(String url,Consumer<Boolean> done){images.add(new Image(url,done));}
            public void logo(String url,Consumer<Boolean> done){logos.add(new Image(url,done));}
        },true);
        queue.window(context,"site",List.of(candidate("A",0),candidate("B",1)));
        assertNull(queue.logo(vod("A")));metadata.get(0).done().accept(new NativeHomeHero.Info("","","wide-A","logo-A"));
        assertEquals("logo-A",queue.logo(vod("A")));assertEquals(1,images.size());assertTrue(logos.isEmpty());
        images.get(0).done().accept(true);assertEquals(1,logos.size());assertEquals(1,metadata.size());
        advance(750);metadata.get(1).done().accept(new NativeHomeHero.Info("","","","logo-B"));assertEquals(1,logos.size());
        logos.get(0).done().accept(true);assertEquals("logo-B",logos.get(1).url());logos.get(1).done().accept(true);
        assertEquals(2,metadata.size());assertEquals(1,images.size());assertEquals(2,logos.size());
        queue.window(context,"site",List.of(candidate("A",0),candidate("B",1)));assertEquals(2,logos.size());
    }
}
