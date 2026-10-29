package com.fongmi.android.tv.ui.home;

import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;
import android.widget.*;
import com.fongmi.android.tv.ui.homepreview.HomeHeroLogoView;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeHomeLogoTimingTest {
    NativeHomeImagesTest fixture;HomeHeroLogoView logo;List<NativeHomeBrowseImages.LogoEvent> events;
    @Before public void setup(){
        fixture=new NativeHomeImagesTest();fixture.setup();logo=new HomeHeroLogoView(fixture.activity,null);
        fixture.root.addView(logo,new FrameLayout.LayoutParams(800,200));fixture.root.addView(fixture.title,new FrameLayout.LayoutParams(800,200));
        fixture.images=new NativeHomeBrowseImages(fixture.wall,fixture.owner,logo,fixture.title,fixture.poster,fixture.loader);
        fixture.images.manageTitle();events=new ArrayList<>();fixture.images.observeLogos(events::add);fixture.layout();logo.layout(0,0,800,200);
    }
    @After public void close(){fixture.close();}
    void advance(long ms){org.robolectric.shadows.ShadowLooper.idleMainLooper(ms,TimeUnit.MILLISECONDS);}
    void draw(){Bitmap b=Bitmap.createBitmap(800,200,Bitmap.Config.ARGB_8888);logo.draw(new Canvas(b));b.recycle();}
    long count(String phase){return events.stream().filter(e->e.phase().equals(phase)).count();}
    @Test public void pendingNeverFlashesTextAndReadyIsNotDrawnUntilDraw(){
        fixture.images.focusSource("");assertEquals(View.INVISIBLE,fixture.title.getVisibility());fixture.images.prepareTitle("A");
        advance(50);fixture.loader.last().ready().accept(new ColorDrawable(0xffff0000));assertEquals(1,count("T3"));assertEquals(0,count("T4"));assertEquals(View.INVISIBLE,fixture.title.getVisibility());
        draw();assertEquals(1,count("T4"));draw();assertEquals(1,count("T4"));
        int requests=fixture.loader.calls.size();fixture.images.enrich("","A");assertEquals(requests,fixture.loader.calls.size());assertEquals(0,count("text"));
        System.out.println("12 simulated pending/ready/draw events="+events);
    }
    @Test public void absentFailedDeadlineAndLateCacheHaveStableFallback(){
        fixture.images.focusSource("");fixture.images.prepareTitle("");assertEquals(View.VISIBLE,fixture.title.getVisibility());
        fixture.images.focusSource("");fixture.images.prepareTitle("fail");fixture.loader.last().failed().run();assertEquals(View.VISIBLE,fixture.title.getVisibility());
        fixture.images.focusSource("");fixture.images.prepareTitle("slow");var late=fixture.loader.last();advance(NativeHomeBrowseImages.TITLE_WAIT_MS-1);assertEquals(View.INVISIBLE,fixture.title.getVisibility());advance(1);assertEquals(View.VISIBLE,fixture.title.getVisibility());
        late.ready().accept(new ColorDrawable(0xffff0000));assertEquals(1,count("late-cached"));assertEquals(View.VISIBLE,fixture.title.getVisibility());assertEquals(View.INVISIBLE,logo.getVisibility());
        fixture.images.focusSource("");fixture.images.prepareTitle("slow");fixture.loader.last().ready().accept(new ColorDrawable(0xffff0000));draw();assertEquals(View.VISIBLE,logo.getVisibility());
    }
    @Test public void abcAbaAndPauseRejectOldResults(){
        fixture.images.focusSource("A");fixture.images.prepareTitle("A");var a=fixture.loader.last();
        fixture.images.focusSource("B");fixture.images.prepareTitle("B");var b=fixture.loader.last();
        fixture.images.focusSource("C");fixture.images.prepareTitle("C");var c=fixture.loader.last();
        a.ready().accept(new ColorDrawable(0xffff0000));b.ready().accept(new ColorDrawable(0xffff0000));assertNull(logo.getDrawable());
        c.ready().accept(new ColorDrawable(0xff00ff00));assertEquals(View.VISIBLE,logo.getVisibility());
        fixture.images.focusSource("A");fixture.images.prepareTitle("A");var current=fixture.loader.last();a.ready().accept(new ColorDrawable(0xffff0000));assertNull(logo.getDrawable());
        fixture.images.titleActive(false);fixture.images.titleActive(true);current.ready().accept(new ColorDrawable(0xffff0000));assertNull(logo.getDrawable());
        fixture.images.prepareTitle("A");fixture.loader.last().ready().accept(new ColorDrawable(0xff00ff00));draw();assertEquals(1,count("T4"));
    }
    @Test public void legacyHistoryKeepsImmediateText(){
        fixture.images.clear();fixture.images=new NativeHomeBrowseImages(fixture.wall,fixture.owner,logo,fixture.title,fixture.poster,fixture.loader);
        fixture.images.focusSource("");assertEquals(View.VISIBLE,fixture.title.getVisibility());fixture.images.enrich("","A");assertEquals(View.VISIBLE,fixture.title.getVisibility());fixture.loader.last().ready().accept(new ColorDrawable(0xffff0000));assertEquals(View.INVISIBLE,fixture.title.getVisibility());
    }

    @Test public void realGlidePreloadAndABAAvoidNetworkAndDecodeRepeat() throws Exception {
        fixture.images.clear();
        com.bumptech.glide.Glide.init(fixture.activity,new com.bumptech.glide.GlideBuilder().setMemoryCache(new com.bumptech.glide.load.engine.cache.LruResourceCache(24L*1024*1024)));
        com.bumptech.glide.Glide.with(fixture.activity).applyDefaultRequestOptions(new com.bumptech.glide.request.RequestOptions().disallowHardwareConfig());
        // Windows JVM cannot load Android's AVIF JNI module, which probes ByteBuffer before
        // the normal PNG decoder. Keep real Glide HTTP/cache/target flow; decode test PNG
        // bytes with Android BitmapFactory instead of that unavailable native format probe.
        java.util.concurrent.atomic.AtomicInteger decodes=new java.util.concurrent.atomic.AtomicInteger();
        com.bumptech.glide.Glide.get(fixture.activity).getRegistry().prepend(java.nio.ByteBuffer.class,Drawable.class,new com.bumptech.glide.load.ResourceDecoder<java.nio.ByteBuffer,Drawable>(){
            public boolean handles(java.nio.ByteBuffer data,com.bumptech.glide.load.Options options){return true;}
            public com.bumptech.glide.load.engine.Resource<Drawable> decode(java.nio.ByteBuffer data,int w,int h,com.bumptech.glide.load.Options options){
                decodes.incrementAndGet();byte[] bytes=new byte[data.remaining()];data.get(bytes);Bitmap bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);
                return new com.bumptech.glide.load.engine.Resource<Drawable>(){
                    public Class<Drawable> getResourceClass(){return Drawable.class;}public Drawable get(){return new BitmapDrawable(fixture.activity.getResources(),bitmap);}
                    public int getSize(){return bitmap.getAllocationByteCount();}public void recycle(){bitmap.recycle();}
                };
            }
        });

        fixture.images=new NativeHomeBrowseImages(fixture.wall,fixture.owner,logo,fixture.title,fixture.poster);fixture.images.manageTitle();fixture.images.observeLogos(events::add);
        try(mockwebserver3.MockWebServer server=new mockwebserver3.MockWebServer()){
            Bitmap bitmap=Bitmap.createBitmap(64,16,Bitmap.Config.ARGB_8888);bitmap.eraseColor(0xffff0000);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.PNG,100,bytes);bitmap.recycle();
            server.enqueue(new mockwebserver3.MockResponse.Builder().addHeader("Content-Type","image/png").body(new okio.Buffer().write(bytes.toByteArray())).build());server.start();String url=server.url("/logo.png").toString();
            java.util.concurrent.atomic.AtomicBoolean ready=new java.util.concurrent.atomic.AtomicBoolean();fixture.images.prefetchLogo(url,ready::set);
            for(int i=0;i<400&&!ready.get();i++){Thread.sleep(5);org.robolectric.shadows.ShadowLooper.idleMainLooper();}assertTrue(ready.get());
            for(int round=0;round<3;round++){
                fixture.images.focusSource("");fixture.images.prepareTitle(url);
                for(int i=0;i<400&&logo.getVisibility()!=View.VISIBLE;i++){Thread.sleep(5);org.robolectric.shadows.ShadowLooper.idleMainLooper();}
                assertEquals(View.VISIBLE,logo.getVisibility());assertEquals(View.INVISIBLE,fixture.title.getVisibility());draw();
                fixture.images.focusSource("");fixture.images.prepareTitle("");
            }
            assertEquals(1,server.getRequestCount());assertEquals(1,decodes.get());assertEquals(3,count("T4"));
            assertTrue(events.stream().filter(e->e.phase().equals("T3")).allMatch(e->e.source().equals("MEMORY_CACHE")));
            System.out.println("12 real Glide cache: network="+server.getRequestCount()+" decode="+decodes.get()+" events="+events);
        }
    }
}
