package com.fongmi.android.tv.ui.homepreview;

import android.view.*;
import android.app.Activity;
import android.widget.TextView;
import android.text.TextWatcher;
import android.text.Editable;
import androidx.core.graphics.Insets;
import androidx.core.view.WindowInsetsCompat;
import com.fongmi.android.tv.R;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Robolectric;
import org.robolectric.shadows.ShadowLooper;
import java.util.concurrent.TimeUnit;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

/** Real view/event tests; not television frame-rate claims. */
@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=28,application=com.fongmi.android.tv.ui.home.NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
public class NativeHome10InteractionTest {
    Activity activity;NativeHomeBrowseView view;
    void create(){activity=Robolectric.buildActivity(Activity.class).setup().visible().get();activity.setTheme(R.style.Theme_App);view=new NativeHomeBrowseView(activity,30,5,()->{});activity.setContentView(view);layout();view.findViewById(R.id.nhp_home).requestFocus();}
    void layout(){view.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));view.layout(0,0,1920,1080);ShadowLooper.idleMainLooper(300,TimeUnit.MILLISECONDS);}
    androidx.leanback.widget.HorizontalGridView rail(){return view.findViewById(R.id.nhp_posters);}
    void down(){view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));layout();}
    @After public void close(){if(view!=null)view.release();if(activity!=null)activity.finish();}
    @Test public void firstPosterBackIsNotConsumedAndSubsequentBackReachesOriginalHost(){
        create();down();down();assertTrue(rail().hasFocus());
        assertFalse(view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK)));
        assertFalse(view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK)));
        rail().setSelectedPosition(12);layout();
        assertTrue(view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK)));
        assertTrue(view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK)));layout();
        assertEquals(0,rail().getSelectedPosition());assertFalse(view.handleBack());
        assertFalse(view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK)));
        assertFalse(view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK)));
    }
    @Test public void latestInsetsReplaceOldDirectionAndZeroDoesNotRetainPhoneCutout(){
        create();View top=view.findViewById(R.id.nhp_top);int baseline=top.getLeft();
        for(Insets inset:new Insets[]{Insets.of(64,0,0,0),Insets.of(0,0,64,0),Insets.of(0,24,0,30),Insets.NONE}){
            view.dispatchApplyWindowInsets(new WindowInsetsCompat.Builder().setSystemWindowInsets(inset).build().toWindowInsets());layout();
            assertEquals(inset.left+HomePreviewGeometry.calculate(1920-inset.left-inset.right,1080-inset.top-inset.bottom,view.getResources().getDisplayMetrics().density).gutter(),top.getLeft());
            for(int id:new int[]{R.id.nhp_shade_left,R.id.nhp_shade_bottom})assertEquals(1920,view.findViewById(id).getWidth());
            assertEquals(0,view.getPaddingLeft());
        }
        assertEquals(baseline,top.getLeft());
    }
    @Test public void televisionWindowSizesKeepFullBleedShadeAndOnlyDesignGutter(){
        create();
        for(int[] size:new int[][]{{1280,720},{1920,1080},{3840,2160}}){
            view.measure(View.MeasureSpec.makeMeasureSpec(size[0],View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(size[1],View.MeasureSpec.EXACTLY));view.layout(0,0,size[0],size[1]);
            for(int id:new int[]{R.id.nhp_shade_left,R.id.nhp_shade_bottom}){View shade=view.findViewById(id);assertEquals(0,shade.getLeft());assertEquals(0,shade.getTop());assertEquals(size[0],shade.getRight());assertEquals(size[1],shade.getBottom());}
            assertEquals(HomePreviewGeometry.calculate(size[0],size[1],view.getResources().getDisplayMetrics().density).gutter(),view.findViewById(R.id.nhp_top).getLeft());
        }
    }
    @Test public void repeatedTitleAndHeroMetadataDoNotRebindTextButNewIdentityDoes(){
        create();HomeHeroTitleView title=view.findViewById(R.id.nhp_title);int[] changes={0};
        title.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){changes[0]++;}public void afterTextChanged(Editable e){}});
        String longTitle="A long native marquee title which substantially exceeds the hero slot and must continue without a repeated reset";
        title.bindTitle(900,longTitle);layout();assertTrue(title.isSelected());int bound=changes[0];
        for(int i=0;i<20;i++){title.bindTitle(900,longTitle);title.setMarqueeActive(true);title.setText(longTitle);}
        assertEquals(bound,changes[0]);assertTrue(title.isSelected());
        title.bindTitle(901,longTitle);assertEquals(bound+1,changes[0]);
        var card=HomePreviewData.cards(1).get(0);view.bindHero(card);layout();
        org.robolectric.util.ReflectionHelpers.setField(view,"images",new com.fongmi.android.tv.ui.home.NativeHomeBrowseImages(null,view,view.findViewById(R.id.nhp_logo),title,view.findViewById(R.id.nhp_fallback_poster)));
        TextView meta=view.findViewById(R.id.nhp_meta);int[] updates={0};
        meta.addTextChangedListener(new TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int count,int after){}public void onTextChanged(CharSequence s,int st,int before,int count){updates[0]++;}public void afterTextChanged(Editable e){}});
        for(int i=0;i<10;i++)view.showHero(card.id(),card.metadata(),card.overview(),"","","");
        assertEquals(0,updates[0]);
    }
}
