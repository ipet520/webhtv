package com.fongmi.android.tv.ui.home;

import android.graphics.*;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.ui.custom.CustomWallView;
import com.fongmi.android.tv.ui.homepreview.NativeHomeBrowseView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Draw the composed native wall + actual immersive view, not just an image-ready callback. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeHomeBackgroundTest {
    AppCompatActivity activity;FrameLayout root;CustomWallView wall;NativeHomeBrowseView view;
    NativeHomeImagesTest.Loader loader;NativeHomeBrowseImages images;ImageView poster,background;
    @Before public void setup(){
        com.github.catvod.utils.Prefers.getPrefers().edit().clear().commit();Setting.putThemeMode(1);
        activity=Robolectric.buildActivity(AppCompatActivity.class).setup().get();ThemeController.applyFromPreferences(activity);
        root=new FrameLayout(activity);wall=new CustomWallView(activity,null);root.addView(wall,new FrameLayout.LayoutParams(-1,-1));activity.setContentView(root);
        view=new NativeHomeBrowseView(activity,0,0,()->{});view.setBackgroundHost(wall);root.addView(view,new FrameLayout.LayoutParams(-1,-1));
        loader=new NativeHomeImagesTest.Loader();poster=view.findViewById(R.id.nhp_fallback_poster);background=wall.acquireContentBackground(view);
        images=new NativeHomeBrowseImages(wall,view,view.findViewById(R.id.nhp_logo),view.findViewById(R.id.nhp_title),poster,loader);layout();
    }
    void layout(){root.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));root.layout(0,0,1920,1080);}
    Bitmap draw(){Bitmap bitmap=Bitmap.createBitmap(1920,1080,Bitmap.Config.ARGB_8888);root.draw(new Canvas(bitmap));return bitmap;}
    @After public void close(){images.clear();view.release();activity.finish();}
    void portrait(){images.source("portrait");loader.last().ready().accept(new ColorDrawable(0xffff0000));assertEquals(View.VISIBLE,poster.getVisibility());}
    @Test public void nativeLandscapeActuallyPaintsThroughContentAndOnlyThenDismissesPortrait(){
        portrait();images.enrich("wide","");loader.last().ready().accept(new ColorDrawable(0xff20e040));
        assertSame(background,loader.last().target());assertEquals(View.VISIBLE,poster.getVisibility());
        Bitmap bitmap=draw();int pixel=bitmap.getPixel(1820,216);bitmap.recycle();
        assertTrue("right image region must contain painted green, not opaque theme: "+Integer.toHexString(pixel),((pixel>>>8)&255)>((pixel>>>16)&255)+70&&((pixel>>>8)&255)>(pixel&255)+70);
        assertEquals(View.INVISIBLE,poster.getVisibility());assertEquals(1,background.getAlpha(),0);
        assertEquals(0,((ColorDrawable)view.getBackground()).getColor());
        Setting.putThemeMode(0);ThemeController.applyFromPreferences(activity);ThemeController.bindTheme(root);view.refreshTheme();
        bitmap=draw();pixel=bitmap.getPixel(1820,216);bitmap.recycle();
        assertTrue(((pixel>>>8)&255)>((pixel>>>16)&255)+50);assertNull(background.getImageTintList());
    }
    @Test public void failureMissingAndStaleArtNeverDismissCurrentPortrait(){
        portrait();draw().recycle();assertEquals(View.VISIBLE,poster.getVisibility());
        images.enrich("wide-A","");var old=loader.last();old.failed().run();draw().recycle();assertEquals(View.VISIBLE,poster.getVisibility());
        images.focusSource("B");images.settleSource();loader.last().ready().accept(new ColorDrawable(0xffff0000));
        old.ready().accept(new ColorDrawable(0xff20e040));draw().recycle();assertEquals(View.VISIBLE,poster.getVisibility());
        images.enrich("wide-B","");loader.last().ready().accept(new ColorDrawable(0xff20e040));
        images.focusSource("C");images.settleSource();loader.last().ready().accept(new ColorDrawable(0xffff0000));
        draw().recycle();assertEquals("cancel pending draw acknowledgement too",View.VISIBLE,poster.getVisibility());
    }
    @Test public void existingActivityWallIsTheOnlyBackgroundHostAcrossModeShowHide(){
        view.release();root.removeView(view);FrameLayout classic=new FrameLayout(activity);View toolbar=new View(activity);FrameLayout home=new FrameLayout(activity);
        home.addView(classic);home.addView(toolbar);root.addView(home,new FrameLayout.LayoutParams(-1,-1));
        NativeHomeModeHost host=new NativeHomeModeHost(activity,home,classic,toolbar,()->{},new com.fongmi.android.tv.ui.presenter.FuncPresenter.OnClickListener(){public void onItemClick(com.fongmi.android.tv.bean.Func f){}public boolean onLongClick(com.fongmi.android.tv.bean.Func f){return false;}},null);
        host.show();layout();host.show();assertEquals(2,root.getChildCount());assertSame(wall,root.getChildAt(0));
        host.hide();assertSame(wall,root.getChildAt(0));assertEquals(View.VISIBLE,classic.getVisibility());
        host.show();layout();assertSame(wall,root.getChildAt(0));host.release();
    }
    @SuppressWarnings("deprecation") @Test public void oldIntegerAlphaRemainsInvisibleEvenAfterViewAlphaFinishesFade(){
        ImageView old=new ImageView(activity);old.setImageDrawable(new ColorDrawable(0xff20e040));old.layout(0,0,64,36);
        old.setAlpha(0); // Exact overloaded call in the pre-07 source clear/load path.
        old.setAlpha(1f); // Final View alpha produced by ViewPropertyAnimator.alpha(1).
        assertEquals(1,old.getAlpha(),0);assertEquals(0,old.getImageAlpha());
        Bitmap bitmap=Bitmap.createBitmap(64,36,Bitmap.Config.ARGB_8888);old.draw(new Canvas(bitmap));assertEquals(0,bitmap.getPixel(32,18)>>>24);
        old.setImageAlpha(255);old.draw(new Canvas(bitmap));assertEquals(0xff20e040,bitmap.getPixel(32,18));bitmap.recycle();
    }
    @Test public void existingGlideDecodesRealLocalLandscapeAndComposesItInNativeWall() throws Exception {
        images.clear();images=new NativeHomeBrowseImages(wall,view,view.findViewById(R.id.nhp_logo),view.findViewById(R.id.nhp_title),poster);
        com.bumptech.glide.Glide.with(activity).applyDefaultRequestOptions(new com.bumptech.glide.request.RequestOptions().disallowHardwareConfig());
        java.io.File file=java.io.File.createTempFile("native-home-07-",".png",activity.getCacheDir());
        try {
            Bitmap source=Bitmap.createBitmap(64,36,Bitmap.Config.ARGB_8888);source.eraseColor(0xff20e040);
            try(var output=new java.io.FileOutputStream(file)){assertTrue(source.compress(Bitmap.CompressFormat.PNG,100,output));}source.recycle();
            images.source("");var original=background.getDrawable();poster.setImageDrawable(new ColorDrawable(0xffff0000));poster.setVisibility(View.VISIBLE);
            // ImgUtil intentionally wraps normal URLs as GlideUrl; use its supported local data-URI path.
            images.enrich("data:image/png;base64,"+java.util.Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(file.toPath())),"");
            com.bumptech.glide.Glide.with(activity).resumeRequests();
            for(int i=0;i<200;i++){
                org.robolectric.shadows.ShadowLooper.idleMainLooper();
                if(background.getDrawable()!=original&&background.getDrawable() instanceof android.graphics.drawable.BitmapDrawable decoded
                        &&decoded.getBitmap().getPixel(decoded.getBitmap().getWidth()/2,decoded.getBitmap().getHeight()/2)==0xff20e040)break;
                Thread.sleep(10);
            }
            assertTrue("real Glide decoder must install landscape",background.getDrawable() instanceof android.graphics.drawable.BitmapDrawable);
            android.graphics.Bitmap decoded=((android.graphics.drawable.BitmapDrawable)background.getDrawable()).getBitmap();
            assertEquals("decoded pixel; request="+background.getTag(com.bumptech.glide.R.id.glide_custom_view_target_tag),0xff20e040,decoded.getPixel(decoded.getWidth()/2,decoded.getHeight()/2));
            Bitmap bitmap=draw();int pixel=bitmap.getPixel(1820,216);bitmap.recycle();
            assertTrue("decoded landscape pixel="+Integer.toHexString(pixel)+" imageAlpha="+background.getImageAlpha()+" drawable="+background.getDrawable(),((pixel>>>8)&255)>((pixel>>>16)&255)+70);assertEquals(View.INVISIBLE,poster.getVisibility());
        } finally {file.delete();}
    }
}
