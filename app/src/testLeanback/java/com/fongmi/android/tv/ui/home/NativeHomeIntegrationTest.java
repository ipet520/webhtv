package com.fongmi.android.tv.ui.home;

import android.app.Application;
import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ImageView;
import android.graphics.drawable.StateListDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ColorDrawable;
import android.content.res.Configuration;
import androidx.appcompat.app.AppCompatActivity;
import androidx.leanback.widget.HorizontalGridView;
import androidx.recyclerview.widget.RecyclerView;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.bean.HomeButton;

import com.fongmi.android.tv.api.config.LiveConfig;
import com.fongmi.android.tv.following.FollowingSettings;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.theme.*;
import com.fongmi.android.tv.ui.homepreview.NativeHomeBrowseView;
import com.fongmi.android.tv.ui.presenter.FuncPresenter;
import com.github.catvod.Init;
import com.github.catvod.utils.Prefers;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import org.robolectric.shadows.ShadowGradientDrawable;
import org.robolectric.shadow.api.Shadow;
import java.util.*;
import java.util.concurrent.TimeUnit;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, application=NativeHomeBusinessTest.FixtureApp.class, qualifiers="w960dp-h540dp-land-xhdpi")
public class NativeHomeIntegrationTest {
    public static class TestActivity extends AppCompatActivity {
        @Override public void onCreate(Bundle state) { setTheme(R.style.Theme_App); super.onCreate(state); }
    }
    private TestActivity activity;
    private NativeHomeBrowseView view;
    private final List<Integer> clicks = new ArrayList<>(), longs = new ArrayList<>();
    private final FuncPresenter.OnClickListener dispatch = new FuncPresenter.OnClickListener() {
        public void onItemClick(Func item) { clicks.add(item.getResId()); }
        public boolean onLongClick(Func item) { longs.add(item.getResId()); return item.getResId()==R.string.home_search; }
    };
    @Before public void create() {
        Init.set(RuntimeEnvironment.getApplication()); Prefers.getPrefers().edit().clear().commit();
        LiveConfig.get().config(com.fongmi.android.tv.bean.Config.create(1));
        activity=Robolectric.buildActivity(TestActivity.class).setup().get();
        ThemeController.applyFromPreferences(activity);
        view=new NativeHomeBrowseView(activity,18,0,()->{});
        activity.setContentView(view); layout(); view.findViewById(R.id.nhp_home).requestFocus();
    }
    @After public void close() { if(view!=null)view.release(); if(activity!=null)activity.finish(); }
    private HorizontalGridView grid(int id) { return view.findViewById(id); }
    private void layout() {
        view.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));
        view.layout(0,0,1920,1080); ShadowLooper.idleMainLooper(300,TimeUnit.MILLISECONDS);
    }
    private void configure(String order) { Prefers.put("home_button",order); view.setFunctions(HomeButton.getVisibleButtons(),dispatch); layout(); }
    private View item(HorizontalGridView grid,int position) { return grid.findViewHolderForAdapterPosition(position).itemView; }
    private int selectedCategory() { Bundle b=new Bundle();view.savePreviewState(b);return b.getInt("preview.category"); }
    private void focusCategory(int index) { grid(R.id.nhp_categories).setSelectedPosition(index); layout(); item(grid(R.id.nhp_categories),index).requestFocus(); }

    @Test public void configurationUsesExistingVisibilityOrderAndRealCallbackIdentities() {
        configure("2,1,3,6,7,8");
        assertEquals(4,grid(R.id.nhp_functions).getAdapter().getItemCount()); // live/following unavailable
        int[] expected={2,3,6,7};
        for(int i=0;i<expected.length;i++) assertEquals(expected[i],grid(R.id.nhp_functions).getAdapter().getItemId(i));
        FollowingSettings.setEnabled(true);
        com.fongmi.android.tv.bean.Config live=com.fongmi.android.tv.bean.Config.create(1);live.setUrl("https://local.invalid/live");LiveConfig.get().config(live);
        configure("0,1,2,3,8,4,5,6,7,9");
        assertEquals(10,grid(R.id.nhp_functions).getAdapter().getItemCount());
        for(int i=0;i<10;i++) {
            grid(R.id.nhp_functions).setSelectedPosition(i);layout();
            View button=item(grid(R.id.nhp_functions),i);button.performClick();
            assertEquals(HomeButton.getVisibleButtons().get(i).getResId()==R.string.home_search,button.performLongClick());
            assertEquals(HomeButton.getVisibleButtons().get(i).getResId(),(int)clicks.get(i));
            assertEquals(clicks.get(i),longs.get(i));
        }
    }

    @Test public void reorderDisableAndNoOpUpdatesKeepFunctionIdentityAndPosterHost() {
        configure("2,3,6,7"); HorizontalGridView functions=grid(R.id.nhp_functions),rail=grid(R.id.nhp_posters);
        Object railAdapter=rail.getAdapter(); item(functions,1).requestFocus();
        int geometry=view.debugMetrics().getInt("geometry");
        configure("7,6,2,3");
        assertEquals(3,functions.getAdapter().getItemId(functions.getSelectedPosition()));
        assertTrue(functions.hasFocus());assertSame(railAdapter,rail.getAdapter());
        Bundle before=view.debugMetrics(); configure("7,6,2,3");
        assertEquals(before.getInt("focuses"),view.debugMetrics().getInt("focuses"));
        configure("7,6,2"); assertEquals(2,functions.getAdapter().getItemId(functions.getSelectedPosition()));
        configure(""); assertTrue(view.findViewById(R.id.nhp_home).hasFocus());
        assertEquals(geometry,view.debugMetrics().getInt("geometry"));assertSame(railAdapter,rail.getAdapter());
        view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN));assertTrue(grid(R.id.nhp_categories).hasFocus());
        configure("2,3"); Bundle saved=new Bundle();view.savePreviewState(saved);
        view.release();view=new NativeHomeBrowseView(activity,18,0,()->{});activity.setContentView(view);
        view.setFunctions(HomeButton.getVisibleButtons(),dispatch);view.restorePreviewState(saved);layout();
        assertEquals(2,grid(R.id.nhp_functions).getAdapter().getItemId(0));
    }

    @Test public void focusDebounceLastWinsCachesABARestoresRailAndConfirmOnlyOpensFilters() {
        // Keep the exact Handler 99/100ms boundary independent from animator frame time.
        org.robolectric.shadows.ShadowChoreographer.setPaused(true);
        try {
        HorizontalGridView categories=grid(R.id.nhp_categories),rail=grid(R.id.nhp_posters);
        Object adapter=rail.getAdapter(); long first=rail.getAdapter().getItemId(0);
        categories.requestFocus();
        item(categories,1).requestFocus();item(categories,2).requestFocus();item(categories,3).requestFocus();
        assertEquals(1,view.debugMetrics().getInt("categoryPending"));assertEquals(0,selectedCategory());
        long focusTime=android.os.SystemClock.uptimeMillis();ShadowLooper.idleMainLooper(99,TimeUnit.MILLISECONDS);System.out.println("11 debounce elapsed="+(android.os.SystemClock.uptimeMillis()-focusTime)+" selected="+selectedCategory());assertEquals(0,selectedCategory());
        ShadowLooper.idleMainLooper(1,TimeUnit.MILLISECONDS);layout();
        assertEquals(3,selectedCategory()); assertTrue(categories.hasFocus());assertFalse(view.isFilterOpen());
        assertEquals(2,view.debugMetrics().getInt("categoryLoads"));assertNotEquals(first,rail.getAdapter().getItemId(0));
        int loads=view.debugMetrics().getInt("categoryLoads");
        focusCategory(0);ShadowLooper.idleMainLooper(100,TimeUnit.MILLISECONDS);layout();
        assertEquals(first,rail.getAdapter().getItemId(0));assertEquals(loads,view.debugMetrics().getInt("categoryLoads"));
        rail.setSelectedPosition(4);layout();
        focusCategory(3);ShadowLooper.idleMainLooper(100,TimeUnit.MILLISECONDS);layout();
        focusCategory(0);ShadowLooper.idleMainLooper(100,TimeUnit.MILLISECONDS);layout();
        assertEquals(4,rail.getSelectedPosition());assertSame(adapter,rail.getAdapter());
        int applies=view.debugMetrics().getInt("categoryApplies");
        item(categories,0).performClick();layout();assertTrue(view.isFilterOpen());view.handleBack();layout();
        assertEquals(applies,view.debugMetrics().getInt("categoryApplies"));
        item(categories,2).requestFocus();item(categories,0).requestFocus();
        ShadowLooper.idleMainLooper(300,TimeUnit.MILLISECONDS);assertEquals(0,selectedCategory());
        assertEquals(applies,view.debugMetrics().getInt("categoryApplies"));

        } finally {org.robolectric.shadows.ShadowChoreographer.setPaused(false);}
    }

    @Test public void lightDarkAndCustomTokensRefreshAllOwnedStatesWithoutPosterRebind() {
        configure("2,7");
        HorizontalGridView rail=grid(R.id.nhp_posters);Object adapter=rail.getAdapter();
        final int[] notifications={0};rail.getAdapter().registerAdapterDataObserver(new RecyclerView.AdapterDataObserver(){
            @Override public void onChanged(){notifications[0]++;}
            @Override public void onItemRangeChanged(int a,int b){notifications[0]++;}
            @Override public void onItemRangeInserted(int a,int b){notifications[0]++;}
            @Override public void onItemRangeRemoved(int a,int b){notifications[0]++;}
        });
        Bundle before=view.debugMetrics();
        for(int mode:new int[]{0,1,0}) {
            Setting.putThemeMode(mode);ThemeController.applyFromPreferences(activity);view.refreshTheme();layout();
            ThemeTokens t=ThemeController.current();
            assertEquals(t.colorOnSurface(),((TextView)view.findViewById(R.id.nhp_title)).getCurrentTextColor());
            assertEquals(t.colorOnSurfaceVariant(),((TextView)view.findViewById(R.id.nhp_meta)).getCurrentTextColor());
            assertEquals(t.colorOnSurface(),((ImageView)view.findViewById(R.id.nhp_home)).getImageTintList().getDefaultColor());
            StateListDrawable house=(StateListDrawable)view.findViewById(R.id.nhp_home).getBackground();
            house.setState(new int[]{android.R.attr.state_focused});
            assertEquals(t.colorFocus(),Shadow.<ShadowGradientDrawable>extract(house.getCurrent()).getStrokeColor());
            assertNull(((ImageView)view.findViewById(R.id.nhp_fallback_poster)).getImageTintList());
            assertEquals(0,((ColorDrawable)view.getBackground()).getColor());
            assertEquals(t.colorOnSurface(),((TextView)((ViewGroup)item(rail,0)).getChildAt(1)).getCurrentTextColor());
        }
        int oldPrimary=ThemeController.current().colorPrimary();Setting.putThemeColor(0xffe37617);
        ThemeController.applyFromPreferences(activity);view.refreshTheme();layout();
        assertNotEquals(oldPrimary,ThemeController.current().colorPrimary());
        StateListDrawable category=(StateListDrawable)item(grid(R.id.nhp_categories),0).getBackground();category.setState(new int[]{android.R.attr.state_selected});
        assertEquals(androidx.core.graphics.ColorUtils.setAlphaComponent(ThemeController.current().colorPrimary(),Math.round(255*.28f)),((GradientDrawable)category.getCurrent()).getColor().getDefaultColor());
        ThemeProfile custom=ThemeProfile.defaultProfile();custom.light.focus="#006B50";custom.light.primary="#6750A4";custom.light.surface="#FFF8F0";
        assertTrue(ThemeProfileStore.apply(custom).success());ThemeController.applyFromPreferences(activity);view.refreshTheme();layout();
        assertEquals(0xff006b50,ThemeController.current().colorFocus());
        StateListDrawable house=(StateListDrawable)view.findViewById(R.id.nhp_home).getBackground();house.setState(new int[]{android.R.attr.state_focused});
        assertEquals(0xff006b50,Shadow.<ShadowGradientDrawable>extract(house.getCurrent()).getStrokeColor());
        view.findViewById(R.id.nhp_home).performClick();ShadowLooper.idleMainLooper();Dialog drawer=ShadowDialog.getLatestDialog();assertTrue(drawer.isShowing());
        Setting.putThemeMode(1);ThemeController.applyFromPreferences(activity);view.refreshTheme();
        View drawerContent=((ViewGroup)drawer.findViewById(android.R.id.content)).getChildAt(0);
        assertEquals(ThemeController.current().colorSurfaceContainer(),((ColorDrawable)drawerContent.getBackground()).getColor());
        assertEquals(Math.max(.10f,Math.min(.28f,(ThemeController.current().colorScrim() >>> 24)/255f*.28f)),drawer.getWindow().getAttributes().dimAmount,.001f);
        drawer.cancel();
        focusCategory(0);item(grid(R.id.nhp_categories),0).performClick();layout();
        View selected=item(grid(R.id.nhp_filter_genre),0);assertTrue(selected.isSelected());
        assertEquals(ThemeController.current().colorOnPrimaryContainer(),((TextView)selected).getCurrentTextColor());
        GradientDrawable focused=(GradientDrawable)selected.getBackground().getCurrent();
        assertEquals(ThemeController.current().colorFocus(),Shadow.<ShadowGradientDrawable>extract(focused).getStrokeColor());
        selected.getBackground().setState(new int[]{android.R.attr.state_selected});
        GradientDrawable applied=(GradientDrawable)selected.getBackground().getCurrent();
        assertNotEquals(focused.getColor().getDefaultColor(),applied.getColor().getDefaultColor());
        Setting.putThemeMode(0);ThemeController.applyFromPreferences(activity);view.refreshTheme();
        assertTrue(selected.hasFocus());assertTrue(selected.isSelected());
        assertEquals(ThemeController.current().colorOnPrimaryContainer(),((TextView)selected).getCurrentTextColor());
        assertTrue(view.handleBack());assertSame(adapter,rail.getAdapter());assertEquals(0,notifications[0]);
        assertEquals(before.getInt("geometry"),view.debugMetrics().getInt("geometry"));
        assertEquals(before.getInt("heroes"),view.debugMetrics().getInt("heroes"));
    }

    @Test public void lateCategoryCompletionCannotReplaceCurrentAndInflightIsReused() {
        Map<Integer,java.util.function.Consumer<List<com.fongmi.android.tv.ui.homepreview.HomePreviewData.Card>>> completions=new HashMap<>();
        final int[] requests={0};
        view.setCategoryContentSource((category,label,limit,complete)->{requests[0]++;completions.put(category,complete);});
        focusCategory(1);ShadowLooper.idleMainLooper(100,TimeUnit.MILLISECONDS);
        focusCategory(2);ShadowLooper.idleMainLooper(100,TimeUnit.MILLISECONDS);
        completions.get(2).accept(com.fongmi.android.tv.ui.homepreview.HomePreviewData.categoryCards(2,"B",18));layout();
        assertEquals(2,selectedCategory());long current=grid(R.id.nhp_posters).getAdapter().getItemId(0);
        completions.get(1).accept(com.fongmi.android.tv.ui.homepreview.HomePreviewData.categoryCards(1,"A",18));layout();
        assertEquals(2,selectedCategory());assertEquals(current,grid(R.id.nhp_posters).getAdapter().getItemId(0));
        focusCategory(1);ShadowLooper.idleMainLooper(100,TimeUnit.MILLISECONDS);layout();
        assertEquals(1,selectedCategory());assertEquals(2,requests[0]);
        focusCategory(3);ShadowLooper.idleMainLooper(100,TimeUnit.MILLISECONDS);
        focusCategory(4);ShadowLooper.idleMainLooper(100,TimeUnit.MILLISECONDS);
        focusCategory(3);ShadowLooper.idleMainLooper(100,TimeUnit.MILLISECONDS);
        assertEquals(4,requests[0]); // repeated pending category 3 did not duplicate its request
        completions.get(3).accept(com.fongmi.android.tv.ui.homepreview.HomePreviewData.categoryCards(3,"C",18));layout();
        assertEquals(3,selectedCategory());
        view.release();completions.get(4).accept(List.of());assertEquals(3,selectedCategory());
    }

    @Test public void allTenConfiguredFunctionsRemainDpadReachableInReducedWidth() {
        FollowingSettings.setEnabled(true);
        com.fongmi.android.tv.bean.Config live=com.fongmi.android.tv.bean.Config.create(1);live.setUrl("https://local.invalid/live");LiveConfig.get().config(live);
        configure("0,1,2,3,8,4,5,6,7,9");
        for(int position=0;position<10;position++) {
            boolean handled=view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_RIGHT));
            if(!handled){View next=view.findFocus().focusSearch(View.FOCUS_RIGHT);if(next!=null)next.requestFocus(View.FOCUS_RIGHT);}
            view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP,KeyEvent.KEYCODE_DPAD_RIGHT));
            view.measure(View.MeasureSpec.makeMeasureSpec(960,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(540,View.MeasureSpec.EXACTLY));
            view.layout(0,0,960,540);ShadowLooper.idleMainLooper(300,TimeUnit.MILLISECONDS);
            assertEquals(position,grid(R.id.nhp_functions).getSelectedPosition());
            assertTrue(grid(R.id.nhp_functions).hasFocus());
        }
        assertEquals(10,grid(R.id.nhp_functions).getAdapter().getItemCount());
    }
}

