package com.fongmi.android.tv.ui.home;

import android.graphics.*;
import android.graphics.drawable.*;
import android.view.View;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeHomePreparationTest {
    NativeHomeBackgroundTest fixture;
    @Before public void setup(){fixture=new NativeHomeBackgroundTest();fixture.setup();}
    @After public void close(){fixture.close();}
    @Test public void readyBackgroundsFadeInOneNativeHostWithoutNeutralResetOrPortraitFlash(){
        fixture.portrait();fixture.images.enrich("A","");fixture.loader.last().ready().accept(new ColorDrawable(0xff20e040));fixture.draw().recycle();
        Drawable a=fixture.background.getDrawable();int requests=fixture.loader.calls.size();
        fixture.images.focusSource("B-poster");assertSame(a,fixture.background.getDrawable());fixture.images.settleSource();fixture.loader.last().ready().accept(new ColorDrawable(0xffff0000));
        assertEquals(View.INVISIBLE,fixture.poster.getVisibility());fixture.images.enrich("B","");fixture.loader.last().ready().accept(new ColorDrawable(0xff0040ff));
        assertTrue(fixture.background.getDrawable() instanceof TransitionDrawable);fixture.draw().recycle();
        org.robolectric.shadows.ShadowLooper.idleMainLooper(180,TimeUnit.MILLISECONDS);fixture.draw().recycle();assertFalse(fixture.background.getDrawable() instanceof TransitionDrawable);
        assertEquals(View.INVISIBLE,fixture.poster.getVisibility());assertEquals(requests+2,fixture.loader.calls.size());
        fixture.images.focusSource("same-B-poster");fixture.images.enrich("B","");assertEquals(requests+2,fixture.loader.calls.size());
    }
    @Test public void absentOrFailedNewBackdropReleasesOldImageAndKeepsCurrentPortrait(){
        fixture.portrait();fixture.images.enrich("A","");fixture.loader.last().ready().accept(new ColorDrawable(0xff20e040));fixture.draw().recycle();
        fixture.images.focusSource("B");fixture.images.settleSource();fixture.loader.last().ready().accept(new ColorDrawable(0xffff0000));
        fixture.images.noBackdrop();assertEquals(View.VISIBLE,fixture.poster.getVisibility());
        fixture.images.enrich("failure","");fixture.loader.last().failed().run();assertEquals(View.VISIBLE,fixture.poster.getVisibility());
    }
    @Test public void rapidReadyTransitionsAreCollapsedInsteadOfNested(){
        fixture.portrait();fixture.images.enrich("A","");fixture.loader.last().ready().accept(new ColorDrawable(0xff20e040));fixture.draw().recycle();
        for(String key:new String[]{"B","C","D"}){
            fixture.images.focusSource(key);fixture.images.enrich(key,"");fixture.loader.last().ready().accept(new ColorDrawable(0xff0040ff));
            TransitionDrawable transition=(TransitionDrawable)fixture.background.getDrawable();assertFalse(transition.getDrawable(0) instanceof TransitionDrawable);
        }
        org.robolectric.shadows.ShadowLooper.idleMainLooper(180,TimeUnit.MILLISECONDS);fixture.draw().recycle();assertFalse(fixture.background.getDrawable() instanceof TransitionDrawable);
    }
    @Test public void realGlideABABAReusesMemoryAndPrefetchUsesSameDecodedResource() throws Exception {
        fixture.images.clear();fixture.images=new NativeHomeBrowseImages(fixture.wall,fixture.view,fixture.view.findViewById(com.fongmi.android.tv.R.id.nhp_logo),fixture.view.findViewById(com.fongmi.android.tv.R.id.nhp_title),fixture.poster);
        // Verify cache reuse with known sufficient existing-Glide cache capacity, rather
        // than Robolectric's artificial small-heap eviction. Production cache is unchanged.
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
        try(mockwebserver3.MockWebServer server=new mockwebserver3.MockWebServer()){
            for(int color:new int[]{0xff20e040,0xff0040ff}){Bitmap bitmap=Bitmap.createBitmap(64,36,Bitmap.Config.ARGB_8888);bitmap.eraseColor(color);java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();bitmap.compress(Bitmap.CompressFormat.PNG,100,bytes);bitmap.recycle();server.enqueue(new mockwebserver3.MockResponse.Builder().addHeader("Content-Type","image/png").body(new okio.Buffer().write(bytes.toByteArray())).build());}
            server.start();String a=server.url("/A.png").toString(),b=server.url("/B.png").toString();
            fixture.images.focusSource("");fixture.images.enrich(a,"");awaitColor(0xff20e040);
            java.util.concurrent.atomic.AtomicBoolean decodedReady=new java.util.concurrent.atomic.AtomicBoolean();
            fixture.images.prefetch(b,decodedReady::set);
            for(int i=0;i<400&&!decodedReady.get();i++){Thread.sleep(5);org.robolectric.shadows.ShadowLooper.idleMainLooper();}
            assertTrue("preparation requires actual Glide decoded-resource completion",decodedReady.get());
            assertEquals(2,server.getRequestCount());
            for(String key:new String[]{b,a,b,a}){fixture.images.focusSource("");fixture.images.enrich(key,"");awaitColor(key.equals(a)?0xff20e040:0xff0040ff);}
            assertEquals("two distinct images, no A-B-A network amplification",2,server.getRequestCount());
            assertEquals("matching preload/display keys also reuse decoded resources",2,decodes.get());
            System.out.println("NATIVE-HOME-09 local HTTP A-B-A-B-A: network="+server.getRequestCount()+", decode="+decodes.get());
        }
    }
    void awaitColor(int color) throws Exception {
        for(int i=0;i<400;i++){
            org.robolectric.shadows.ShadowLooper.idleMainLooper();Drawable current=fixture.background.getDrawable();
            if(current instanceof TransitionDrawable transition)current=transition.getDrawable(1);
            if(current instanceof BitmapDrawable image){Bitmap bitmap=image.getBitmap();if(bitmap.getPixel(bitmap.getWidth()/2,bitmap.getHeight()/2)==color){org.robolectric.shadows.ShadowLooper.idleMainLooper(180,TimeUnit.MILLISECONDS);fixture.draw().recycle();return;}}
            Thread.sleep(5);
        }
        fail("real Glide resource did not arrive: "+fixture.background.getDrawable());
    }
    @Test public void focusAndMissingBackdropDoNotCancelWindowPreparationButReleaseDoes(){
        fixture.images.clear();
        var cancels=new java.util.concurrent.atomic.AtomicInteger();
        var loader=new NativeHomeImagesTest.Loader(){@Override public void cancelPrefetch(android.widget.ImageView target){cancels.incrementAndGet();}};
        fixture.images=new NativeHomeBrowseImages(fixture.wall,fixture.view,fixture.view.findViewById(com.fongmi.android.tv.R.id.nhp_logo),fixture.view.findViewById(com.fongmi.android.tv.R.id.nhp_title),fixture.poster,loader);
        fixture.images.focusSource("A");fixture.images.focusSource("B");fixture.images.noBackdrop();assertEquals(0,cancels.get());
        fixture.images.clear();assertEquals(1,cancels.get());
    }

}
