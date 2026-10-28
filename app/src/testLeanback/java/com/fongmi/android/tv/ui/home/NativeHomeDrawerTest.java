package com.fongmi.android.tv.ui.home;

import android.app.Application;
import android.app.Dialog;
import android.os.Bundle;
import android.view.*;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import androidx.leanback.widget.HorizontalGridView;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.ui.homepreview.NativeHomeBrowseView;
import com.fongmi.android.tv.theme.*;
import com.github.catvod.Init;
import com.github.catvod.utils.Prefers;
import java.util.*;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.*;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
public class NativeHomeDrawerTest {
    public static class Activity extends AppCompatActivity {public void onCreate(Bundle state){setTheme(R.style.Theme_App);super.onCreate(state);}}
    Activity activity;NativeHomeBrowseView view;NativeHomeBusinessTest.Gateway gateway;NativeHomeBrowseController controller;
    @Before public void create(){
        Init.set(RuntimeEnvironment.getApplication());Prefers.getPrefers().edit().clear().commit();
        activity=Robolectric.buildActivity(Activity.class).setup().get();ThemeController.applyFromPreferences(activity);
        gateway=new NativeHomeBusinessTest.Gateway();controller=new NativeHomeBrowseController(gateway,r->{});
        view=new NativeHomeBrowseView(activity,0,0,()->{throw new AssertionError("house must not exit");});activity.setContentView(view);
        view.setBusiness(controller,()->List.of(Site.get("A","站源A"),Site.get("B","站源B")),site->controller.reload(site.getKey()),v->{},v->false,c->{});
        controller.attach("A",c->view.renderBusiness(c,"A"));var result=NativeHomeBusinessTest.home();assertEquals(2,result.getTypes().size());gateway.last().ok().accept(result);NativeHomeBusinessTest.idle();layout();
    }
    void layout(){view.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));view.layout(0,0,1920,1080);ShadowLooper.idleMainLooper(20,TimeUnit.MILLISECONDS);}
    @After public void close(){controller.detach();view.release();activity.finish();}
    @Test public void drawerPreservesHomeAndOnlyConfirmRequestsSource(){
        ThemeProfile custom=ThemeProfile.defaultProfile();custom.light.scrimOpacity=.85f;custom.dark.scrimOpacity=.85f;
        assertTrue(ThemeProfileStore.apply(custom).success());ThemeController.applyFromPreferences(activity);view.refreshTheme();
        HorizontalGridView rail=view.findViewById(R.id.nhp_posters);Object adapter=rail.getAdapter();Object state=controller.state();
        View house=view.findViewById(R.id.nhp_home);house.requestFocus();int requests=gateway.calls.size();
        String hero=((TextView)view.findViewById(R.id.nhp_title)).getText().toString();
        house.performClick();ShadowLooper.idleMainLooper();Dialog drawer=ShadowDialog.getLatestDialog();assertTrue(drawer.isShowing());
        assertEquals(View.VISIBLE,view.getVisibility());assertEquals(1f,view.getAlpha(),0);assertEquals(hero,((TextView)view.findViewById(R.id.nhp_title)).getText().toString());
        assertSame(adapter,rail.getAdapter());assertSame(state,controller.state());assertEquals(requests,gateway.calls.size());
        assertTrue(drawer.getWindow().getAttributes().dimAmount<=.28f);
        assertEquals((ThemeController.current().colorScrim()>>>24)/255f*.28f,drawer.getWindow().getAttributes().dimAmount,.001f);
        android.util.TypedValue value=new android.util.TypedValue();assertTrue(drawer.getContext().getTheme().resolveAttribute(android.R.attr.windowIsTranslucent,value,true));assertNotEquals(0,value.data);
        View focused=drawer.getCurrentFocus();drawer.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_LEFT));assertSame(focused,drawer.getCurrentFocus());
        drawer.cancel();ShadowLooper.idleMainLooper();assertTrue(house.hasFocus());assertSame(adapter,rail.getAdapter());assertEquals(requests,gateway.calls.size());
        house.performClick();ShadowLooper.idleMainLooper();drawer=ShadowDialog.getLatestDialog();
        ScrollView scroll=(ScrollView)((ViewGroup)drawer.findViewById(android.R.id.content)).getChildAt(0);LinearLayout options=(LinearLayout)scroll.getChildAt(0);
        options.getChildAt(1).performClick();assertEquals("B",controller.siteKey());assertEquals(requests+1,gateway.calls.size());
    }
    @Test public void actualFiltersReuseRailAndBackRestoresCategoryWithoutRequest(){
        controller.focusCategory("A");NativeHomeBusinessTest.idle();gateway.last().ok().accept(NativeHomeBusinessTest.page("a",1));NativeHomeBusinessTest.idle();layout();
        HorizontalGridView rail=view.findViewById(R.id.nhp_posters),categories=view.findViewById(R.id.nhp_categories);Object adapter=rail.getAdapter();int requests=gateway.calls.size();
        categories.setSelectedPosition(1);categories.requestFocus();layout();
        assertNotNull("active="+controller.activeType()+", category count="+categories.getAdapter().getItemCount(),categories.findViewHolderForAdapterPosition(1));
        View category=categories.findViewHolderForAdapterPosition(1).itemView;category.requestFocus();category.performClick();layout();
        assertTrue(view.isFilterOpen());assertSame(adapter,rail.getAdapter());assertEquals(requests,gateway.calls.size());assertTrue(view.handleBack());assertTrue(categories.hasFocus());assertSame(adapter,rail.getAdapter());
    }
    @Test public void duplicateRecommendationEntriesRetainOrderWithoutStableIdCollision(){
        controller.seed("A",NativeHomeBusinessTest.result("{\"list\":[{\"vod_id\":\"same\",\"vod_name\":\"影片\"},{\"vod_id\":\"same\",\"vod_name\":\"影片\"}]}"));
        assertEquals(2,controller.state().items.size());HorizontalGridView rail=view.findViewById(R.id.nhp_posters);assertEquals("before layout",2,rail.getAdapter().getItemCount());layout();
        assertEquals("active="+controller.activeType()+", items="+controller.state().items.size()+", requests="+gateway.calls.size(),2,rail.getAdapter().getItemCount());assertNotEquals(rail.getAdapter().getItemId(0),rail.getAdapter().getItemId(1));
    }
    @Test public void downDuringCategoryLoadEntersOnlyThatRailAndUpReturnsActiveCategory(){
        HorizontalGridView categories=view.findViewById(R.id.nhp_categories),rail=view.findViewById(R.id.nhp_posters);
        categories.setSelectedPosition(1);categories.requestFocus();layout();NativeHomeBusinessTest.idle();
        view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));
        assertEquals("A",controller.activeType());assertTrue(categories.hasFocus());
        gateway.last().ok().accept(NativeHomeBusinessTest.page("a",1));NativeHomeBusinessTest.idle();layout();
        assertTrue(rail.hasFocus());view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_UP));layout();
        assertTrue(categories.hasFocus());assertEquals(1,categories.getSelectedPosition());
        View primary=categories.findViewHolderForAdapterPosition(1).itemView;primary.performClick();layout();
        assertEquals(View.INVISIBLE,view.findViewById(R.id.nhp_category_row).getVisibility());assertFalse(categories.isShown());
        assertTrue(view.handleBack());layout();assertTrue(categories.hasFocus());
    }
    @Test public void loadingProjectionHidesColdShellAndKeepsCategoryOperableDuringRailWait(){
        HorizontalGridView categories=view.findViewById(R.id.nhp_categories),rail=view.findViewById(R.id.nhp_posters);
        Object adapter=rail.getAdapter();controller.reload("A");layout();
        assertFalse(categories.isShown());assertEquals(View.INVISIBLE,rail.getVisibility());assertEquals(1,visibleLoaders());
        gateway.last().ok().accept(NativeHomeBusinessTest.home());NativeHomeBusinessTest.idle();layout();
        assertTrue(categories.isShown());assertEquals(View.VISIBLE,rail.getVisibility());assertEquals(0,visibleLoaders());
        assertEquals("更新推荐",((TextView)categories.findViewHolderForAdapterPosition(0).itemView).getText().toString());
        assertEquals("首页",controller.types().get(0).getTypeName());assertEquals("home",controller.activeType());
        controller.focusCategory("A");controller.flushCategory();layout();
        assertTrue(categories.isShown());assertEquals(1,visibleLoaders());assertSame(adapter,rail.getAdapter());
        assertFalse(((TextView)view.findViewById(R.id.nhp_title)).getText().toString().contains("加载中"));
        gateway.last().error().accept(new RuntimeException("offline"));NativeHomeBusinessTest.idle();layout();
        assertEquals(0,visibleLoaders());assertTrue(categories.isShown());assertFalse(controller.state().error.isEmpty());
    }
    int visibleLoaders(){int count=0;for(int i=0;i<view.getChildCount();i++){View child=view.getChildAt(i);if(child instanceof com.fongmi.android.tv.ui.homepreview.HomeBrowseLoadingView&&child.getVisibility()==View.VISIBLE){assertFalse(child.isFocusable());count++;}}return count;}
}
