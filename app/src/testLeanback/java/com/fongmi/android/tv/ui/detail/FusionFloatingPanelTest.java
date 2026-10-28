package com.fongmi.android.tv.ui.detail;

import android.view.*;
import android.widget.*;
import androidx.core.widget.NestedScrollView;
import androidx.media3.ui.PlayerView;
import com.fongmi.android.tv.R;
import com.google.android.material.card.MaterialCardView;
import com.github.catvod.utils.Prefers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=com.fongmi.android.tv.ui.home.NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class FusionFloatingPanelTest {
    public static class Host extends androidx.appcompat.app.AppCompatActivity {
        @Override protected void onCreate(android.os.Bundle state){setTheme(R.style.Theme_App);super.onCreate(state);}
    }
    Host activity;FrameLayout root;NestedScrollView scroll;LinearLayout content;View spacer;Button detail;MaterialCardView panel;PlayerView video;FusionFloatingPanel floating;int changes;
    @Before public void setup(){
        Prefers.getPrefers().edit().clear().commit();activity=Robolectric.buildActivity(Host.class).setup().visible().get();
        root=new FrameLayout(activity);activity.setContentView(root);scroll=new NestedScrollView(activity);content=new LinearLayout(activity);content.setOrientation(1);scroll.addView(content);root.addView(scroll,new FrameLayout.LayoutParams(-1,-1));
        spacer=new View(activity);spacer.setFocusable(true);content.addView(spacer,new LinearLayout.LayoutParams(-1,720));
        detail=new Button(activity);detail.setText("Episode");detail.setFocusableInTouchMode(true);content.addView(detail,new LinearLayout.LayoutParams(200,100));content.addView(new View(activity),new LinearLayout.LayoutParams(-1,2200));
        panel=new MaterialCardView(activity);panel.setFocusable(true);panel.setFocusableInTouchMode(true);panel.setRadius(20);root.addView(panel,new FrameLayout.LayoutParams(1280,720));
        var attrs=Robolectric.buildAttributeSet().addAttribute(androidx.media3.ui.R.attr.player_layout_id,"@android:layout/simple_list_item_1").addAttribute(androidx.media3.ui.R.attr.use_controller,"false").addAttribute(androidx.media3.ui.R.attr.surface_type,"none").build();
        video=new PlayerView(activity,attrs);panel.addView(video,new FrameLayout.LayoutParams(-1,-1));
        floating=new FusionFloatingPanel(activity,root,panel,spacer,scroll,()->changes++);layout(1920,1080);
    }
    @After public void close(){floating.dispose();activity.finish();}
    void layout(int w,int h){root.measure(View.MeasureSpec.makeMeasureSpec(w,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(h,View.MeasureSpec.EXACTLY));root.layout(0,0,w,h);}
    void enter(){scroll.scrollTo(0,800);detail.requestFocus();assertTrue(floating.update(true,true,false));layout(1920,1080);}
    void press(int key){floating.key(new KeyEvent(KeyEvent.ACTION_DOWN,key),root.findFocus());floating.key(new KeyEvent(KeyEvent.ACTION_UP,key),root.findFocus());}
    @Test public void automaticLayoutPreservesPlayerViewParentScrollAndDetailFocus(){
        assertFalse(floating.update(true,true,false));enter();var handle=floating.handle();var background=handle.getBackground();int top=scroll.getScrollY();
        assertTrue(detail.hasFocus());assertSame(panel,video.getParent());assertSame(root,panel.getParent());assertEquals(720,spacer.getHeight());assertFalse(panel.isFocusable());assertFalse(spacer.isFocusable());assertEquals(ViewGroup.FOCUS_BLOCK_DESCENDANTS,panel.getDescendantFocusability());
        for(int i=0;i<50;i++)assertTrue(floating.update(true,true,false));assertEquals(1,changes);assertSame(background,handle.getBackground());assertEquals(top,scroll.getScrollY());
        scroll.scrollTo(0,0);assertFalse(floating.update(true,true,false));layout(1920,1080);assertTrue(detail.hasFocus());assertTrue(spacer.isFocusable());assertEquals(1280,panel.getWidth());assertEquals(720,panel.getHeight());assertSame(panel,video.getParent());assertEquals(View.GONE,handle.getVisibility());assertEquals(2,changes);
    }
    @Test public void remoteEntryMoveCancelSaveAndReturnToDetails(){
        enter();assertTrue("detail lost before entry root="+root.findFocus()+" activity="+activity.getCurrentFocus(),detail.hasFocus());
        // This controller-free Robolectric host exposes focus through its root, not Window.
        assertSame(detail,root.findFocus());
        assertTrue(floating.key(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT),root.findFocus()));
        assertTrue("entry down focus="+root.findFocus()+" shown="+floating.handle().isShown()+" focusable="+floating.handle().isFocusable()+" size="+floating.handle().getWidth()+"x"+floating.handle().getHeight(),floating.handle().hasFocus());
        floating.key(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_RIGHT),root.findFocus());assertTrue(floating.handle().hasFocus());assertFalse(floating.isMoving());
        var initial=floating.bounds();press(KeyEvent.KEYCODE_DPAD_CENTER);assertTrue(floating.isMoving());press(KeyEvent.KEYCODE_DPAD_LEFT);press(KeyEvent.KEYCODE_DPAD_DOWN);assertNotEquals(initial,floating.bounds());press(KeyEvent.KEYCODE_BACK);assertEquals(initial,floating.bounds());assertFalse(floating.isMoving());assertFalse(Prefers.getPrefers().contains("tmdb_fusion_float_y"));
        press(KeyEvent.KEYCODE_DPAD_CENTER);for(int i=0;i<12;i++)press(KeyEvent.KEYCODE_DPAD_DOWN);press(KeyEvent.KEYCODE_DPAD_CENTER);assertFalse(floating.isMoving());assertTrue(Prefers.getFloat("tmdb_fusion_float_y")>0);assertTrue(floating.handle().hasFocus());press(KeyEvent.KEYCODE_DPAD_LEFT);assertTrue(detail.hasFocus());
    }
    void touch(int action,float x,float y){MotionEvent e=MotionEvent.obtain(0,100,action,x,y,0);floating.handle().dispatchTouchEvent(e);e.recycle();}
    @Test public void localMouseDragCancelSnapAndRelativePositionPersistWithoutRebinding(){
        enter();var first=floating.bounds();touch(MotionEvent.ACTION_DOWN,20,20);touch(MotionEvent.ACTION_MOVE,-80,200);touch(MotionEvent.ACTION_CANCEL,-80,200);assertEquals(first,floating.bounds());
        touch(MotionEvent.ACTION_DOWN,20,20);touch(MotionEvent.ACTION_MOVE,20,200);touch(MotionEvent.ACTION_UP,20,200);assertTrue(Prefers.getFloat("tmdb_fusion_float_y")>0);var p=FusionFloatingLayout.position(new FusionFloatingLayout.Area(24,24,1896,1056),floating.bounds());
        assertEquals(1,p.x(),.01f);assertSame(panel,video.getParent());int scrollY=scroll.getScrollY();layout(3840,2160);floating.refreshBounds();layout(3840,2160);assertTrue(floating.bounds().x()+floating.bounds().width()<=3840-24);assertEquals(scrollY,scroll.getScrollY());
    }
    @Test public void loadingPauseAndFullscreenSuspendHaveSingleLayoutAuthority(){
        enter();assertTrue(floating.update(true,false,true));assertEquals(1,changes);assertSame(panel,video.getParent());
        floating.suspend();assertFalse(floating.isFloating());assertEquals(View.GONE,floating.handle().getVisibility());assertTrue(panel.isFocusable());assertSame(panel,video.getParent());assertEquals(2,changes);
    }
}
