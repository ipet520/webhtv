package com.fongmi.android.tv.ui.homepreview;

import android.app.Activity;
import android.graphics.*;
import android.view.*;
import android.widget.*;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.ui.home.NativeHomeBusinessTest;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class NativeHomePolishTest {
    @Test public void focusMarkerIsShortCenteredAndIndependentOfTitleLength(){
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();activity.setTheme(R.style.Theme_App);
        Button text=new Button(activity);text.setLayoutParams(new android.view.ViewGroup.LayoutParams(500,80));text.setAllCaps(false);text.setText("搜索");text.setTextSize(16);text.setGravity(Gravity.CENTER);text.setPadding(20,0,20,0);
        text.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(80,View.MeasureSpec.EXACTLY));text.layout(0,0,500,80);
        var underline=new HomePreviewStyles.Underline(text,0xff00ff00);underline.setBounds(0,0,500,80);
        Bitmap bitmap=Bitmap.createBitmap(500,80,Bitmap.Config.ARGB_8888);underline.draw(new Canvas(bitmap));
        int left=500,right=-1;for(int x=0;x<500;x++)for(int y=0;y<80;y++)if((bitmap.getPixel(x,y)>>>24)>0){left=Math.min(left,x);right=Math.max(right,x);}
        float expected=12*activity.getResources().getDisplayMetrics().density;assertEquals(expected,right-left+1,2);assertEquals(250,(left+right+1)*.5,1);assertTrue(right-left+1<text.getLayout().getLineMax(0)*.6f);
        text.setText("个性设置与排序");text.measure(View.MeasureSpec.makeMeasureSpec(500,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(80,View.MeasureSpec.EXACTLY));text.layout(0,0,500,80);
        Bitmap longer=Bitmap.createBitmap(500,80,Bitmap.Config.ARGB_8888);underline.draw(new Canvas(longer));int pixels=0;for(int x=0;x<500;x++)if((longer.getPixel(x,75)>>>24)>0)pixels++;assertEquals(expected,pixels,2);longer.recycle();
        bitmap.recycle();activity.finish();
    }
    @Test public void portraitKeepsItsAcceptedBoundsAndUsesCardCornerRadius(){
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();activity.setTheme(R.style.Theme_App);
        NativeHomeBrowseView view=new NativeHomeBrowseView(activity,0,0,()->{});activity.setContentView(view);
        view.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));view.layout(0,0,1920,1080);
        ImageView poster=view.findViewById(R.id.nhp_fallback_poster);float density=activity.getResources().getDisplayMetrics().density;
        int h=Math.round(Math.min(1080*.39f,300*density)),w=Math.round(h*2f/3);
        assertEquals(h,poster.getHeight());assertEquals(w,poster.getWidth());assertEquals(1920-Math.round(1920*.08f)-w,poster.getLeft());assertEquals(Math.round(1080*.145f),poster.getTop());
        Outline outline=new Outline();poster.getOutlineProvider().getOutline(poster,outline);assertTrue(poster.getClipToOutline());assertEquals(8*density,outline.getRadius(),.01f);assertEquals(ImageView.ScaleType.CENTER_CROP,poster.getScaleType());
        view.release();activity.finish();
    }
    @Test public void sourceTextHasInternalPaddingWhileWindowAndOuterPaddingStayUnchanged(){
        Activity activity=Robolectric.buildActivity(Activity.class).setup().get();activity.setTheme(R.style.Theme_App);
        View house=new View(activity);activity.setContentView(house);HomePreviewMenuDialog dialog=new HomePreviewMenuDialog(activity,new String[]{"很长的站源名称","末端标题"},()->0,i->{},house);
        dialog.show();WindowManager.LayoutParams attrs=dialog.getWindow().getAttributes();int width=attrs.width,gravity=attrs.gravity,height=attrs.height;
        ScrollView scroll=(ScrollView)((android.view.ViewGroup)dialog.findViewById(android.R.id.content)).getChildAt(0);LinearLayout content=(LinearLayout)scroll.getChildAt(0);float d=activity.getResources().getDisplayMetrics().density;
        assertEquals(Math.round(20*d),content.getPaddingLeft());assertEquals(Math.round(20*d),content.getPaddingRight());
        dialog.refreshTheme();for(int i=0;i<content.getChildCount();i++){Button option=(Button)content.getChildAt(i);assertEquals(Math.round(12*d),option.getPaddingLeft());assertEquals(Math.round(12*d),option.getPaddingRight());assertEquals(15,option.getTextSize()/activity.getResources().getDisplayMetrics().scaledDensity,.01f);}
        assertEquals(width,dialog.getWindow().getAttributes().width);assertEquals(gravity,dialog.getWindow().getAttributes().gravity);assertEquals(height,dialog.getWindow().getAttributes().height);
        dialog.dismiss();activity.finish();
    }
}
