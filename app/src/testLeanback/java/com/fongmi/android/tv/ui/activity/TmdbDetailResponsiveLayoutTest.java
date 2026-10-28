package com.fongmi.android.tv.ui.activity;

import android.os.Bundle;
import android.view.*;
import android.widget.*;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ActivityTmdbDetailBinding;
import com.fongmi.android.tv.ui.detail.*;
import com.fongmi.android.tv.ui.utils.TmdbDetailLayoutUtils;
import com.fongmi.android.tv.ui.home.NativeHomeBusinessTest;
import com.google.android.flexbox.FlexboxLayout;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class TmdbDetailResponsiveLayoutTest {
    public static class Host extends TmdbDetailActivity {
        ActivityTmdbDetailBinding views;
        @Override public android.content.res.Resources getResources(){return RuntimeEnvironment.getApplication().getResources();}
        @Override protected androidx.viewbinding.ViewBinding getBinding(){
            // Robolectric/Windows cannot decode Media3 packaged exo_icon_vr.png. Inflate the
            // actual detail XML with a real PlayerView using a controller-free test layout;
            // playback decoding/service behavior is a separate real-device gate.
            LayoutInflater inflater=getLayoutInflater().cloneInContext(this);
            ReflectionHelpers.setField(inflater,"mFactory",null);ReflectionHelpers.setField(inflater,"mFactory2",null);
            inflater.setFactory2(new LayoutInflater.Factory2(){
                public View onCreateView(View parent,String name,android.content.Context context,android.util.AttributeSet attrs){return onCreateView(name,context,attrs);}
                public View onCreateView(String name,android.content.Context context,android.util.AttributeSet attrs){
                    if(!name.equals("androidx.media3.ui.PlayerView"))return null;
                    var attributes=Robolectric.buildAttributeSet().addAttribute(androidx.media3.ui.R.attr.player_layout_id,"@android:layout/simple_list_item_1").addAttribute(androidx.media3.ui.R.attr.use_controller,"false").addAttribute(androidx.media3.ui.R.attr.surface_type,"none").build();
                    var player=new androidx.media3.ui.PlayerView(context,attributes);player.setId(R.id.exo);return player;
                }
            });
            views=ActivityTmdbDetailBinding.inflate(inflater);ReflectionHelpers.setField(this,"binding",views);return views;
        }
        @Override protected void initView(Bundle state){}
        @Override protected void initEvent(){}
        @Override protected boolean shouldBindPlaybackService(){return false;}
        @Override protected boolean customWall(){return false;}
    }
    Host activity;ActivityTmdbDetailBinding b;
    @Before public void create(){activity=Robolectric.buildActivity(Host.class).create().visible().get();b=activity.views;b.root.requestFocusFromTouch();b.playerPanel.addOnLayoutChangeListener((v,l,t,r,bottom,ol,ot,or,ob)->{try{call("syncInlinePlayerToSpacer");}catch(Exception error){throw new AssertionError(error);}});mode(false,false);}
    void mode(boolean fusion,boolean cinema){
        DetailModeHost host=new DetailModeHost(){public android.content.Context context(){return activity;}public androidx.viewbinding.ViewBinding binding(){return b;}public boolean isCinemaStyle(){return cinema;}public void closeDetailFullscreenPlayer(){}public void playInline(){}public void playDetailFullscreen(){}public void playDefaultPlayback(){}};
        TmdbDetailModeController controller=fusion?new FusionDetailController(host):new EnhancedDetailController(host);
        ReflectionHelpers.setField(activity,"modeController",controller);controller.applyInitialLayout();
    }
    Object call(String name,Object... args)throws Exception{java.lang.reflect.Method method=java.util.Arrays.stream(TmdbDetailActivity.class.getDeclaredMethods()).filter(m->m.getName().equals(name)&&m.getParameterCount()==args.length).findFirst().orElseThrow();method.setAccessible(true);return method.invoke(activity,args);}
    void layout(int width,int height){b.root.measure(View.MeasureSpec.makeMeasureSpec(width,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(height,View.MeasureSpec.EXACTLY));b.root.layout(0,0,width,height);}
    void actions(int count){b.contentPanel.setVisibility(View.VISIBLE);b.play.setText("立即播放");b.changeSourceDetail.setText("换源");b.keep.setText("收藏");b.following.setVisibility(count==6?View.VISIBLE:View.GONE);b.rematch.setVisibility(View.VISIBLE);b.themeModeDetail.setVisibility(View.VISIBLE);b.themeModeDetail.setText("深色");}
    @Test public void actualXmlFiveButtonsBecomeSingleRowAndRemainOriginalInstances()throws Exception{
        actions(5);var play=b.play;var source=b.changeSourceDetail;call("applyDetailTemplate");layout(1920,1080);
        assertSame(play,b.play);assertSame(source,b.changeSourceDetail);assertTrue(b.play.getParent() instanceof FlexboxLayout);
        assertEquals(b.play.getTop(),b.themeModeDetail.getTop());assertTrue(b.play.getWidth()<((View)b.play.getParent()).getWidth());
        assertEquals(b.play.getHeight(),b.keep.getHeight());
    }
    @Test public void fusionPanelAndSpacerUseSameMeasuredSixteenNineBoundsWithoutReparent()throws Exception{
        mode(true,false);layout(1920,1080);Object parent=b.playerPanel.getParent(),surface=b.exo.getVideoSurfaceView();
        call("applyInlinePlayerEmbeddedLayout");layout(1920,1080);
        assertEquals(16f/9,b.playerPanel.getWidth()/(float)b.playerPanel.getHeight(),.01f);assertEquals(b.playerPanel.getHeight(),b.playerPanelSpacer.getLayoutParams().height);
        System.out.println("DETAIL fusion root=1920 density="+activity.getResources().getDisplayMetrics().density+" panel="+b.playerPanel.getWidth()+"x"+b.playerPanel.getHeight()+" left="+b.playerPanel.getLeft()+" spacer="+b.playerPanelSpacer.getLayoutParams().height);assertEquals((1920-b.playerPanel.getWidth())/2,b.playerPanel.getLeft(),1);assertSame(parent,b.playerPanel.getParent());assertSame(surface,b.exo.getVideoSurfaceView());
    }
    @Test public void dynamicSixButtonsWrapWithoutOverflowOrZeroWidths()throws Exception{
        actions(6);call("applyDetailTemplate");Object row=b.play.getParent();
        for(int width:new int[]{1920,1280,960}){
            layout(width,1080);ViewGroup parent=(ViewGroup)b.play.getParent();int visible=0;
            for(int i=0;i<parent.getChildCount();i++){View v=parent.getChildAt(i);if(v.getVisibility()!=View.VISIBLE)continue;visible++;assertTrue(v.getWidth()>0);assertTrue(v.getRight()<=parent.getWidth());assertEquals(b.play.getHeight(),v.getHeight());}
            assertEquals(6,visible);System.out.println("DETAIL widths root="+width+" density="+activity.getResources().getDisplayMetrics().density+" actions="+parent.getWidth()+" play="+b.play.getWidth()+" rows="+((FlexboxLayout)parent).getFlexLines().size());
        }
        b.following.setVisibility(View.GONE);layout(1920,1080);assertSame(row,b.play.getParent());assertEquals(b.play.getTop(),b.themeModeDetail.getTop());
    }
    @Test public void repeatedFullscreenLayoutRestoresSamePlayerSurfaceSpacerAndSixteenNine()throws Exception{
        mode(true,false);layout(1920,1080);call("applyInlinePlayerEmbeddedLayout");layout(1920,1080);
        View surface=new SurfaceView(activity);b.exo.addView(surface);Object panelParent=b.playerPanel.getParent(),player=b.exo;
        int width=b.playerPanel.getWidth(),height=b.playerPanel.getHeight();
        for(int i=0;i<3;i++){
            ReflectionHelpers.setField(activity,"inlineFullscreen",true);call("applyInlinePlayerFullscreenLayout");layout(1920,1080);
            assertEquals(1920,b.playerPanel.getWidth());assertEquals(1080,b.playerPanel.getHeight());
            ReflectionHelpers.setField(activity,"inlineFullscreen",false);call("applyInlinePlayerEmbeddedLayout");layout(1920,1080);
            assertEquals(width,b.playerPanel.getWidth());assertEquals(height,b.playerPanel.getHeight());assertEquals(height,b.playerPanelSpacer.getLayoutParams().height);
            assertSame(panelParent,b.playerPanel.getParent());assertSame(player,b.exo);assertSame(b.exo,surface.getParent());
        }
        int[] panel=new int[2],spacer=new int[2];b.playerPanel.getLocationInWindow(panel);b.playerPanelSpacer.getLocationInWindow(spacer);assertEquals(spacer[1],panel[1],1);
        assertTrue(b.fusionActions.getTop()>=b.playerPanelSpacer.getBottom());
    }
    @Test public void measuredDpGeometryCapsWidthAndHonorsAsymmetricSafeInsets(){
        for(float density:new float[]{1f,2f,3f})for(int dp:new int[]{320,640,960,1920}){
            int root=Math.round(dp*density);var size=TmdbDetailLayoutUtils.tvPlayerSize(root,Math.round(20*density),Math.round(8*density),density);
            assertTrue(size.width()>0);assertTrue(size.width()<=Math.round(760*density));assertEquals(16f/9,size.width()/(float)size.height(),.02f);assertEquals(root,size.left()+size.width()+size.right());
            assertTrue(size.left()>=Math.round(20*density));assertTrue(size.right()>=Math.round(8*density));
        }
    }
    @Test public void playFocusHorizontalOrderAndHiddenButtonContinuityUseExistingHandler()throws Exception{
        actions(6);call("applyDetailTemplate");layout(1920,1080);call("requestDetailPlayFocus");org.robolectric.shadows.ShadowLooper.idleMainLooper();assertTrue("play focusable="+b.play.isFocusable()+" shown="+b.play.isShown()+" width="+b.play.getWidth()+" touch="+b.play.isInTouchMode()+" window="+b.play.getWindowVisibility(),b.play.hasFocus());
        View[] order={b.play,b.changeSourceDetail,b.keep,b.following,b.rematch,b.themeModeDetail};
        for(int i=1;i<order.length;i++){assertTrue((boolean)call("onDetailHorizontalButtonGroupKey",b.detailActions,null,order[i-1],new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT)));assertTrue(order[i].hasFocus());}
        assertTrue((boolean)call("onDetailHorizontalButtonGroupKey",b.detailActions,null,b.themeModeDetail,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT)));assertTrue(b.themeModeDetail.hasFocus());
        b.keep.setVisibility(View.GONE);layout(1920,1080);b.changeSourceDetail.requestFocus();call("onDetailHorizontalButtonGroupKey",b.detailActions,null,b.changeSourceDetail,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT));assertTrue(b.following.hasFocus());
        b.play.requestFocus();call("onDetailHorizontalButtonGroupKey",b.detailActions,null,b.play,new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT));assertTrue(b.play.hasFocus());
    }
    @Test public void compactResumeRetainsFullEpisodeDescriptionAndSameClickListener()throws Exception{
        actions(5);call("applyDetailTemplate");java.util.concurrent.atomic.AtomicInteger clicks=new java.util.concurrent.atomic.AtomicInteger();b.play.setOnClickListener(v->clicks.incrementAndGet());
        call("setDetailPlayLabel",true,"Episode 123456789 long identity");assertEquals(activity.getString(R.string.detail_play_resume_compact),b.play.getText().toString());assertTrue(b.play.getContentDescription().toString().contains("123456789"));
        b.play.performClick();assertEquals(1,clicks.get());call("setDetailPlayLabel",false,"");assertEquals(activity.getString(R.string.detail_play_now),b.play.getText().toString());
    }
    @Test public void themesKeepGreenPlayAndDoNotResetFocusOrRecreateButtons()throws Exception{
        actions(5);call("applyDetailTemplate");layout(1920,1080);b.keep.requestFocus();Object row=b.play.getParent();int green=b.play.getBackgroundTintList().getDefaultColor();
        for(boolean cinema:new boolean[]{false,true})for(boolean light:new boolean[]{false,true}){
            mode(false,cinema);ReflectionHelpers.setField(activity,"lightTheme",light);Object colors=call("currentThemeColors");call("setDetailActionButton",b.keep,colors);call("applyDetailTemplate");layout(1920,1080);
            assertSame(row,b.play.getParent());assertTrue("theme focus touch="+b.keep.isInTouchMode()+" shown="+b.keep.isShown(),b.keep.hasFocus());assertEquals(green,b.play.getBackgroundTintList().getDefaultColor());assertEquals(b.play.getTop(),b.themeModeDetail.getTop());
        }
    }

    @Test public void fusionFourAndFiveActionsAreCompactAndSharePlayerCenter()throws Exception{
        mode(true,false);b.changeSource.setText("换源");b.keepFusion.setText("收藏");b.themeMode.setText("深色");b.rematchFusion.setVisibility(View.VISIBLE);b.followingFusion.setVisibility(View.GONE);
        call("applyDetailTemplate");layout(1920,1080);call("applyInlinePlayerEmbeddedLayout");layout(1920,1080);
        assertEquals(b.changeSource.getTop(),b.themeMode.getTop());assertTrue(b.changeSource.getWidth()<b.fusionActions.getWidth()/2);
        assertEquals(960,b.fusionActions.getLeft()+b.fusionActions.getWidth()/2,1);
        b.followingFusion.setVisibility(View.VISIBLE);layout(1920,1080);assertEquals(b.changeSource.getTop(),b.followingFusion.getTop());
    }
    @Test public void tvSafeInsetsLeaveBackdropFullAndPlayerInsideContent()throws Exception{
        mode(true,false);layout(1920,1080);call("applySystemBarInsets");
        var insets=new androidx.core.view.WindowInsetsCompat.Builder().setInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars(),androidx.core.graphics.Insets.of(40,24,12,30)).build();
        assertEquals(40,insets.getInsets(androidx.core.view.WindowInsetsCompat.Type.systemBars()).left);
        // API28 synthetic WindowInsets loses stable-bar metadata on platform rewrapping.
        // Exercise the registered production Compat handler with its typed input; OEM
        // platform dispatch remains a device gate, not a claim from this synthetic inset.
        Object wrapper=b.root.getTag(androidx.core.R.id.tag_on_apply_window_listener);
        var captured=java.util.Arrays.stream(wrapper.getClass().getDeclaredFields()).filter(f->androidx.core.view.OnApplyWindowInsetsListener.class.isAssignableFrom(f.getType())).findFirst().orElseThrow();captured.setAccessible(true);
        androidx.core.view.OnApplyWindowInsetsListener listener=(androidx.core.view.OnApplyWindowInsetsListener)captured.get(wrapper);
        assertNotNull(listener);listener.onApplyWindowInsets(b.root,insets);
        layout(1920,1080);call("applyInlinePlayerEmbeddedLayout");layout(1920,1080);
        assertEquals(1920,b.hero.getWidth());assertEquals(1080,b.hero.getHeight());assertEquals(0,b.root.getPaddingLeft());assertEquals(40,b.scroll.getPaddingLeft());assertEquals(12,b.scroll.getPaddingRight());
        assertTrue(b.playerPanel.getLeft()>40);assertTrue(b.playerPanel.getRight()<1908);
    }
    @Test public void threeControllersKeepDistinctPlaybackDispatch(){
        int[] calls={0,0,0};DetailModeHost host=new DetailModeHost(){public android.content.Context context(){return activity;}public androidx.viewbinding.ViewBinding binding(){return b;}public boolean isCinemaStyle(){return false;}public void closeDetailFullscreenPlayer(){}public void playInline(){calls[0]++;}public void playDetailFullscreen(){calls[1]++;}public void playDefaultPlayback(){calls[2]++;}};
        new EnhancedDetailController(host).play();new PlayerDetailController(host).play();new FusionDetailController(host).play();assertArrayEquals(new int[]{1,1,1},calls);
        assertFalse(new EnhancedDetailController(host).shouldAutoPlay());assertFalse(new PlayerDetailController(host).shouldAutoPlay());
    }

    @Test public void nativeVerticalNavigationConnectsActionsToFlagsAndFusionPlayer()throws Exception{
        actions(5);call("applyDetailTemplate");b.flagScroll.setVisibility(View.VISIBLE);Button flag=new Button(activity);flag.setText("line");b.flagContainer.addView(flag);layout(1920,1080);
        b.play.requestFocus();View down=b.play.focusSearch(View.FOCUS_DOWN);assertNotNull(down);assertTrue("down must reach existing flag section",down==flag||down.getParent()==b.flagContainer);
        mode(true,false);b.detailActions.setVisibility(View.GONE);call("applyDetailTemplate");call("setupInlineFocusNavigation");call("applyInlinePlayerEmbeddedLayout");layout(1920,1080);
        b.changeSource.requestFocus();View up=b.changeSource.focusSearch(View.FOCUS_UP);assertNotNull(up);up.requestFocus();assertTrue("existing spacer bridge must restore player",b.playerPanel.hasFocus());
        View toActions=b.playerPanel.focusSearch(View.FOCUS_DOWN);assertNotNull(toActions);View cursor=toActions;while(cursor!=null&&cursor!=b.fusionActions)cursor=cursor.getParent() instanceof View v?v:null;assertSame(b.fusionActions,cursor);
    }

}
