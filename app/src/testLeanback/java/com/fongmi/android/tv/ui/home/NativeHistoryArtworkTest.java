package com.fongmi.android.tv.ui.home;

import com.fongmi.android.tv.bean.*;
import com.fongmi.android.tv.setting.Setting;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class)
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class NativeHistoryArtworkTest {
    @Before public void setup(){com.github.catvod.utils.Prefers.getPrefers().edit().clear().commit();Setting.putTmdbConfig("{\"apiKey\":\"test\",\"language\":\"zh-CN\"}");}
    @Test public void historyBridgeOnlyUsesKnownBackendAndKeepsResumeObjectUntouched()throws Exception{
        AtomicInteger known=new AtomicInteger();NativeHistoryArtwork art=new NativeHistoryArtwork(new NativeHomeHero.Backend(){public NativeHomeHero.Info load(Site s,Vod v,TmdbConfig c){throw new AssertionError("must not search");}public NativeHomeHero.Info loadKnown(Site s,Vod v,TmdbConfig c){known.incrementAndGet();return new NativeHomeHero.Info("metadata","overview","wide","");}});
        History h=new History();h.setCid(7);h.setKey("site@@@id");h.setVodName("Original");h.setTmdbId(336458);h.setMediaType("tv");h.setPosition(1234);h.setVodRemarks("E19");Vod v=art.item(h);assertEquals("id",v.getId());assertEquals(336458,v.getTmdb().getTmdbId());assertEquals("Original",h.getVodName());
        List<NativeHomeHero.Info> result=new ArrayList<>();art.focus(v,result::add,()->{},()->fail("unexpected missing"),true);
        for(int i=0;i<100&&result.isEmpty();i++){Thread.sleep(5);ShadowLooper.idleMainLooper();}assertEquals(1,result.size());assertEquals(1,known.get());assertEquals(1234,h.getPosition());assertEquals("E19",h.getVodRemarks());art.reset();
    }
    @Test public void noCredentialUnknownHistoryDoesNotStartAnyMetadataBackend(){
        Setting.putTmdbConfig("{}");AtomicInteger calls=new AtomicInteger();NativeHistoryArtwork art=new NativeHistoryArtwork((s,v,c)->{calls.incrementAndGet();return null;});History h=new History();h.setKey("site@@@id");h.setVodName("Unknown");Vod v=art.item(h);art.focus(v,i->fail(),()->{},()->{},true);ShadowLooper.idleMainLooper(1000,TimeUnit.MILLISECONDS);assertEquals(0,calls.get());assertNull(v.getTmdb());art.reset();
    }
    @Test public void trustedHistoryWallUsesNativeDrawAcknowledgementAndPortraitSurvivesFailure()throws Exception{
        Setting.putTmdbConfig("{}");
        var activity=Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity.class).setup().visible().get();
        com.fongmi.android.tv.theme.ThemeController.applyFromPreferences(activity);
        android.widget.FrameLayout root=new android.widget.FrameLayout(activity);
        var wall=new com.fongmi.android.tv.ui.custom.CustomWallView(activity,null);root.addView(wall,new android.widget.FrameLayout.LayoutParams(-1,-1));activity.setContentView(root);
        var view=new com.fongmi.android.tv.ui.homepreview.NativeHistoryBrowseView(activity,new com.fongmi.android.tv.ui.homepreview.NativeHistoryBrowseView.Actions(){public void open(History h){}public void details(History h){}public void delete(History h,int p){}public boolean longClick(){return true;}public void manage(){}public void report(){}});
        root.addView(view,new android.widget.FrameLayout.LayoutParams(-1,-1));view.attachBackground(wall);
        android.widget.ImageView poster=org.robolectric.util.ReflectionHelpers.getField(view,"poster"),logo=org.robolectric.util.ReflectionHelpers.getField(view,"logo");android.widget.TextView title=org.robolectric.util.ReflectionHelpers.getField(view,"title");
        var loader=new NativeHomeImagesTest.Loader();var images=new NativeHomeBrowseImages(wall,view,logo,title,poster,loader);org.robolectric.util.ReflectionHelpers.setField(view,"images",images);
        History h=new History();h.setKey("site@@@wall");h.setVodName("wall");h.setVodPic("portrait");h.setWallPic("wide");view.submit(List.of(h),false);
        for(int i=0;i<40;i++){root.measure(android.view.View.MeasureSpec.makeMeasureSpec(1920,1073741824),android.view.View.MeasureSpec.makeMeasureSpec(1080,1073741824));root.layout(0,0,1920,1080);Thread.sleep(5);ShadowLooper.idleMainLooper();}
        var portrait=loader.calls.stream().filter(c->c.url().equals("portrait")).findFirst().orElseThrow();var wide=loader.calls.stream().filter(c->c.url().equals("wide")).findFirst().orElseThrow();
        portrait.ready().accept(new android.graphics.drawable.ColorDrawable(0xffff0000));wide.failed().run();assertEquals(android.view.View.VISIBLE,poster.getVisibility());
        images.enrich("wide","");var retry=loader.last();retry.ready().accept(new android.graphics.drawable.ColorDrawable(0xff10ef10));assertEquals(android.view.View.VISIBLE,poster.getVisibility());
        root.measure(android.view.View.MeasureSpec.makeMeasureSpec(1920,1073741824),android.view.View.MeasureSpec.makeMeasureSpec(1080,1073741824));root.layout(0,0,1920,1080);
        android.graphics.Bitmap bitmap=android.graphics.Bitmap.createBitmap(1920,1080,android.graphics.Bitmap.Config.ARGB_8888);root.draw(new android.graphics.Canvas(bitmap));assertEquals(android.view.View.INVISIBLE,poster.getVisibility());int pixel=bitmap.getPixel(1800,180);assertTrue("native pixel="+Integer.toHexString(pixel)+" width="+root.getWidth(),((pixel>>8)&255)>((pixel>>16)&255)+50);bitmap.recycle();
        assertEquals(2f/3,poster.getWidth()/(float)poster.getHeight(),.01);assertTrue(poster.getClipToOutline());
        view.release();activity.finish();
    }
}
