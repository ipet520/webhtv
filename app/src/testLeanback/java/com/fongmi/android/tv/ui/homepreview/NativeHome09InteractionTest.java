package com.fongmi.android.tv.ui.homepreview;

import android.app.*;
import android.graphics.Rect;
import android.view.*;
import androidx.leanback.widget.HorizontalGridView;
import com.fongmi.android.tv.R;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.*;
import org.robolectric.shadows.ShadowLooper;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk=28,application=com.fongmi.android.tv.ui.home.NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
public class NativeHome09InteractionTest {
    Activity activity;NativeHomeBrowseView view;
    void create(){activity=Robolectric.buildActivity(Activity.class).setup().visible().get();activity.setTheme(R.style.Theme_App);view=new NativeHomeBrowseView(activity,30,5,()->{});activity.setContentView(view);layout();view.findViewById(R.id.nhp_home).requestFocus();}
    void layout(){view.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));view.layout(0,0,1920,1080);ShadowLooper.idleMainLooper(300,TimeUnit.MILLISECONDS);}
    HorizontalGridView rail(){return view.findViewById(R.id.nhp_posters);}
    void down(){view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));layout();}
    @After public void close(){if(view!=null)view.release();if(activity!=null)activity.finish();}
    @Test public void backFromCardsSelectsAndActuallyFocusesFirstWithoutRecreatingAdapter(){
        create();down();down();HorizontalGridView rail=rail();Object adapter=rail.getAdapter();
        rail.setSelectedPosition(12);layout();assertTrue(rail.hasFocus());assertEquals(12,rail.getSelectedPosition());
        assertTrue(view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_BACK)));
        assertEquals(12,rail.getSelectedPosition());
        assertTrue(view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_BACK)));layout();
        assertEquals(0,rail.getSelectedPosition());assertTrue(rail.findViewHolderForAdapterPosition(0).itemView.hasFocus());assertSame(adapter,rail.getAdapter());
        assertFalse(view.handleBack());assertTrue(rail.findViewHolderForAdapterPosition(0).itemView.hasFocus());
    }
    @Test public void backClosesFilterBeforeSelectingFirstPoster(){
        create();down();HorizontalGridView categories=view.findViewById(R.id.nhp_categories);View entry=categories.findFocus();entry.performClick();layout();
        assertTrue(view.isFilterOpen());Object adapter=rail().getAdapter();assertTrue(view.handleBack());layout();
        assertFalse(view.isFilterOpen());assertTrue(categories.hasFocus());assertSame(adapter,rail().getAdapter());
    }
    @Test public void systemInsetsOnlyOffsetContentWhileShadesFillEntireRoot(){
        create();View top=view.findViewById(R.id.nhp_top);int initialLeft=top.getLeft();
        view.dispatchApplyWindowInsets(new androidx.core.view.WindowInsetsCompat.Builder().setSystemWindowInsets(androidx.core.graphics.Insets.of(40,24,12,30)).build().toWindowInsets());layout();
        assertEquals(0,view.getPaddingLeft());assertEquals(40+HomePreviewGeometry.calculate(1868,1026,view.getResources().getDisplayMetrics().density).gutter(),top.getLeft());assertTrue(top.getLeft()>initialLeft);
        for(int id:new int[]{R.id.nhp_shade_left,R.id.nhp_shade_bottom}){View shade=view.findViewById(id);assertEquals(0,shade.getLeft());assertEquals(0,shade.getTop());assertEquals(1920,shade.getRight());assertEquals(1080,shade.getBottom());}
        assertTrue(rail().getBottom()<=1050);
    }
    @Test public void overflowingTitleMarqueesWithoutDpadFocusAndStopsOnPauseLogoOrShortText(){
        create();HomeHeroTitleView title=view.findViewById(R.id.nhp_title);
        title.setText("A very long movie title repeated many times until it exceeds the fixed hero title slot by a substantial amount");layout();
        assertEquals(1,title.getLineCount());assertTrue("marquee selected: attached="+title.isAttachedToWindow()+" shown="+title.isShown()+" window="+title.getWindowVisibility()+" width="+title.getWidth()+" line="+title.getLayout().getLineWidth(0)+" text="+title.getText(),title.isSelected());assertFalse(title.isFocusable());assertFalse(title.hasFocus());
        view.setVisualsActive(false);assertFalse(title.isSelected());view.setVisualsActive(true);assertTrue(title.isSelected());
        title.setVisibility(View.INVISIBLE);assertFalse(title.isSelected());title.setVisibility(View.VISIBLE);assertTrue(title.isSelected());
        title.setText("short");layout();assertFalse(title.isSelected());assertEquals(0,title.getScrollX());
    }
    @Test public void actualVisibleWindowPreparesBeforePosterFocusAndBackKeepsBusinessContext(){
        create();java.util.List<java.util.function.Consumer<com.fongmi.android.tv.bean.Result>> homes=new java.util.ArrayList<>();
        var gateway=new com.fongmi.android.tv.ui.home.NativeHomeBrowseController.Gateway(){
            public void home(java.util.function.Consumer<com.fongmi.android.tv.bean.Result> ok,java.util.function.Consumer<Throwable> error){homes.add(ok);}
            public void category(String site,String type,int page,java.util.HashMap<String,String> extend,java.util.function.Consumer<com.fongmi.android.tv.bean.Result> ok,java.util.function.Consumer<Throwable> error){fail("unexpected category request");}
            public void cancel(){}
        };
        var controller=new com.fongmi.android.tv.ui.home.NativeHomeBrowseController(gateway,r->{});
        view.setBusiness(controller,java.util.List::of,s->{},v->{},v->false,c->{});
        java.util.List<NativeHomeBrowseView.PreparationWindow> windows=new java.util.ArrayList<>();view.setPreparationWindowListener(windows::add);
        controller.attach("test",c->view.renderBusiness(c,"test"));
        StringBuilder json=new StringBuilder("{\"list\":[");for(int i=0;i<50;i++){if(i>0)json.append(',');json.append("{\"vod_id\":\"").append(i).append("\",\"vod_name\":\"film ").append(i).append("\"}");}json.append("]}");
        homes.get(0).accept(com.fongmi.android.tv.bean.Result.objectFrom(json.toString()));layout();
        assertFalse(rail().hasFocus());assertFalse(windows.isEmpty());
        var candidates=windows.get(windows.size()-1).candidates();assertTrue("window candidates="+candidates.size()+" rail="+rail().getWidth()+" children="+rail().getChildCount()+" count="+rail().getAdapter().getItemCount(),candidates.size()>2);assertTrue(candidates.size()<=10);assertEquals("0",candidates.get(0).vod().getId());
        Object context=controller.state(),adapter=rail().getAdapter();down();down();rail().setSelectedPosition(9);layout();
        assertTrue(view.handleBack());layout();assertEquals(0,rail().getSelectedPosition());assertTrue(rail().findViewHolderForAdapterPosition(0).itemView.hasFocus());
        assertSame(context,controller.state());assertSame(adapter,rail().getAdapter());assertEquals(1,homes.size());controller.detach();
    }

    @Test public void drawerBackRestoresHouseAndTopBackKeepsExistingBehavior(){
        create();View house=view.findViewById(R.id.nhp_home);house.performClick();ShadowLooper.idleMainLooper();
        assertTrue(org.robolectric.shadows.ShadowDialog.getLatestDialog().isShowing());assertTrue(view.handleBack());ShadowLooper.idleMainLooper();
        assertFalse(org.robolectric.shadows.ShadowDialog.getLatestDialog().isShowing());assertTrue(house.hasFocus());assertFalse(view.handleBack());
    }
    @Test public void secondTenthAndFirstPosterReturnWithinAnotherCategory(){
        create();down();HorizontalGridView categories=view.findViewById(R.id.nhp_categories);categories.setSelectedPosition(1);layout();down();
        Object adapter=rail().getAdapter();for(int position:new int[]{1,9,0}){rail().setSelectedPosition(position);layout();assertEquals(position>0,view.handleBack());layout();assertEquals(0,rail().getSelectedPosition());assertTrue(rail().findViewHolderForAdapterPosition(0).itemView.hasFocus());assertEquals(1,categories.getSelectedPosition());assertSame(adapter,rail().getAdapter());}
    }

}
