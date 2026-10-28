package com.fongmi.android.tv.ui.home;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import java.util.*;
import java.util.function.Consumer;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class NativeHomeImagesTest {
    record Call(ImageView target,String url,int width,int height,Consumer<Drawable> ready,Runnable failed) {}
    static class Loader implements NativeHomeBrowseImages.Loader {
        final List<Call> calls=new ArrayList<>();
        public void clear(ImageView view) {}
        public void load(ImageView view,String url,int w,int h,boolean crop,Consumer<Drawable> ready,Runnable failed) { calls.add(new Call(view,url,w,h,ready,failed)); }
        Call last(){return calls.get(calls.size()-1);}
    }
    ImageView a,logo,poster;TextView title;Loader loader;NativeHomeBrowseImages images;
    androidx.appcompat.app.AppCompatActivity activity;android.widget.FrameLayout root;
    org.robolectric.android.controller.ActivityController<androidx.appcompat.app.AppCompatActivity> lifecycle;
    com.fongmi.android.tv.ui.custom.CustomWallView wall;Object owner=new Object();
    @Before public void setup(){
        com.github.catvod.utils.Prefers.getPrefers().edit().clear().commit();com.fongmi.android.tv.setting.Setting.putThemeMode(1);
        lifecycle=Robolectric.buildActivity(androidx.appcompat.app.AppCompatActivity.class).setup().visible();activity=lifecycle.get();com.fongmi.android.tv.theme.ThemeController.applyFromPreferences(activity);
        root=new android.widget.FrameLayout(activity);wall=new com.fongmi.android.tv.ui.custom.CustomWallView(activity,null);root.addView(wall,new android.widget.FrameLayout.LayoutParams(-1,-1));activity.setContentView(root);
        a=wall.acquireContentBackground(owner);logo=new ImageView(activity);poster=new ImageView(activity);title=new TextView(activity);loader=new Loader();images=new NativeHomeBrowseImages(wall,owner,logo,title,poster,loader);
    }
    @After public void close(){images.clear();wall.releaseContentBackground(owner);lifecycle.pause().stop().destroy();}
    void layout(){root.measure(View.MeasureSpec.makeMeasureSpec(3840,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(2160,View.MeasureSpec.EXACTLY));root.layout(0,0,3840,2160);logo.layout(0,0,800,200);poster.layout(0,0,320,480);}
    @Test public void neverRequestsUnmeasuredOnePixelLogoAndCancelledLayoutCannotStartRequest(){
        images.source("");images.enrich("","logo-A");assertTrue(loader.calls.isEmpty());
        images.source("");layout();assertTrue(loader.calls.isEmpty());
        images.enrich("","logo-B");assertEquals(800,loader.last().width());assertEquals(200,loader.last().height());
        loader.last().ready().accept(new ColorDrawable(0xffff0000));assertEquals(View.VISIBLE,logo.getVisibility());assertEquals(View.INVISIBLE,title.getVisibility());
    }
    @Test public void portraitRemainsSeparateUntilLandscapeActuallyLoadsAndFailuresRetainFallback(){
        layout();images.source("portrait");assertSame(poster,loader.last().target());
        Drawable art=new ColorDrawable(0xff00ff00);loader.last().ready().accept(art);
        assertSame(art,poster.getDrawable());assertEquals(View.VISIBLE,poster.getVisibility());Drawable original=a.getDrawable();
        images.enrich("landscape","");assertTrue(loader.last().width()<=1920);assertTrue(loader.last().height()<=1080);
        loader.last().failed().run();assertEquals(View.VISIBLE,poster.getVisibility());
        assertSame(original,a.getDrawable());
        images.enrich("landscape","");loader.last().ready().accept(new ColorDrawable(0xff0000ff));
        assertEquals("resource-ready is not yet a drawn background",View.VISIBLE,poster.getVisibility());
        assertTrue("wall image requires layout: "+a.getWidth()+"x"+a.getHeight(),a.getWidth()>0&&a.getHeight()>0);
        assertTrue("wall must be attached and shown",wall.isAttachedToWindow()&&wall.isShown());
        wall.draw(new android.graphics.Canvas(android.graphics.Bitmap.createBitmap(16,16,android.graphics.Bitmap.Config.ARGB_8888)));
        assertEquals("window="+wall.getWindowVisibility()+" imageShown="+a.isShown()+" alpha="+a.getAlpha(),View.INVISIBLE,poster.getVisibility());assertNotNull(a.getDrawable());assertEquals(1,a.getAlpha(),0);
    }
    @Test public void lateAResultsCannotReplaceBAOrHideCurrentText(){
        layout();images.source("A");Call oldPoster=loader.last();images.enrich("wide-A","logo-A");Call oldLogo=loader.last();
        images.source("B");images.source("A");Call current=loader.last();
        oldPoster.ready().accept(new ColorDrawable(0xffff0000));oldLogo.ready().accept(new ColorDrawable(0xffff0000));
        assertNull(poster.getDrawable());assertNull(logo.getDrawable());assertEquals(View.VISIBLE,title.getVisibility());
        current.ready().accept(new ColorDrawable(0xff00ff00));assertEquals(View.VISIBLE,poster.getVisibility());
    }
    @Test public void rapidFocusDoesNotStartImageRequestsAndSettledFallbackSharesRailDecodeSize(){
        layout();images.setPosterDecodeSize(240,360);
        images.focusSource("A");images.focusSource("B");images.focusSource("C");assertTrue(loader.calls.isEmpty());
        images.settleSource();assertEquals(1,loader.calls.size());assertEquals("C",loader.last().url());
        assertEquals(240,loader.last().width());assertEquals(360,loader.last().height());
        images.settleSource();assertEquals(1,loader.calls.size());
    }
}
