package com.fongmi.android.tv.ui.activity;

import android.os.Bundle;
import android.view.View;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.homepreview.NativeHistoryBrowseView;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=com.fongmi.android.tv.ui.home.NativeHomeBusinessTest.FixtureApp.class)
public class NativeHistoryModeTest {
    public static class Probe extends HistoryActivity {
        View binding(){return getBinding().getRoot();}
        @Override protected void initView(Bundle state){}
    }
    @Before public void reset(){com.github.catvod.utils.Prefers.getPrefers().edit().clear().commit();}
    @Test public void sameActivitySelectsImmersiveFromExistingLeanbackPreference(){
        var activity=Robolectric.buildActivity(Probe.class).get();activity.setTheme(com.fongmi.android.tv.R.style.Theme_App);
        assertFalse(activity.binding() instanceof NativeHistoryBrowseView);
        Setting.putLeanbackHomeMode(Setting.LeanbackHomeMode.IMMERSIVE);View immersive=activity.binding();assertTrue(immersive instanceof NativeHistoryBrowseView);((NativeHistoryBrowseView)immersive).release();
        assertEquals(Setting.LeanbackHomeMode.IMMERSIVE,Setting.resolveLeanbackHomeMode(false,"leanback","IMMERSIVE"));assertEquals(Setting.LeanbackHomeMode.CLASSIC,Setting.resolveLeanbackHomeMode(true,"mobile","IMMERSIVE"));
    }
    @Test public void availableHistoryDetailsUsesOrdinaryVideoEntryWithoutResumePayload(){
        var activity=Robolectric.buildActivity(Probe.class).get();activity.setTheme(com.fongmi.android.tv.R.style.Theme_App);
        Setting.putDetailOpenMode(Setting.DETAIL_OPEN_DIRECT);
        var h=new com.fongmi.android.tv.bean.History();h.setCid(com.fongmi.android.tv.api.config.VodConfig.getCid());h.setKey(com.fongmi.android.tv.api.SiteApi.PUSH+"@@@https://example.invalid/film.m3u8");h.setVodName("Film");h.setPosition(50000);h.setVodRemarks("E12");
        activity.onItemDetails(h);var intent=((org.robolectric.shadows.ShadowActivity)org.robolectric.shadow.api.Shadow.extract(activity)).getNextStartedActivity();assertNotNull(intent);assertEquals(VideoActivity.class.getName(),intent.getComponent().getClassName());assertEquals(h.getVodId(),intent.getStringExtra("id"));assertFalse(intent.getBooleanExtra("resume_from_history",false));assertFalse(intent.getBooleanExtra("webhomeHistoryExact",false));assertNull(intent.getStringExtra("mark"));
    }
    @Test public void unavailableHistoryDetailsDoesNotPretendCurrentConfigOwnsOldSource(){
        var activity=Robolectric.buildActivity(Probe.class).get();activity.setTheme(com.fongmi.android.tv.R.style.Theme_App);
        var h=new com.fongmi.android.tv.bean.History();h.setCid(com.fongmi.android.tv.api.config.VodConfig.getCid()+1);h.setKey("old@@@film");h.setVodName("Other config film");activity.onItemDetails(h);
        var intent=((org.robolectric.shadows.ShadowActivity)org.robolectric.shadow.api.Shadow.extract(activity)).getNextStartedActivity();assertEquals(SearchActivity.class.getName(),intent.getComponent().getClassName());assertEquals(h.getVodName(),intent.getStringExtra("keyword"));assertFalse(intent.hasExtra("resume_from_history"));
    }
    @Test public void systemBackRestoresFirstThenUsesOriginalExitAndDeleteModeHasPriority()throws Exception{
        Setting.putLeanbackHomeMode(Setting.LeanbackHomeMode.IMMERSIVE);Setting.putTmdbConfig("{}");
        var activity=Robolectric.buildActivity(Probe.class).create().visible().get();
        try{
            NativeHistoryBrowseView view=org.robolectric.util.ReflectionHelpers.getField(activity,"mImmersive");
            var adapter=new com.fongmi.android.tv.ui.adapter.HistoryAdapter(activity);org.robolectric.util.ReflectionHelpers.setField(activity,"mAdapter",adapter);
            var items=new java.util.ArrayList<com.fongmi.android.tv.bean.History>();for(int i=0;i<12;i++){var h=new com.fongmi.android.tv.bean.History();h.setKey("site@@@"+i);h.setVodName("History "+i);items.add(h);}
            adapter.setItems(items,()->{});view.submit(items,false);settle(view);view.restore(9);settle(view);activity.onBackInvoked();settle(view);assertEquals(0,view.focusedPosition());assertFalse(activity.isFinishing());
            view.restore(9);settle(view);adapter.setDelete(true);view.submit(items,true);settle(view);activity.onBackInvoked();settle(view);assertFalse(adapter.isDelete());assertFalse(activity.isFinishing());
            view.restore(0);settle(view);activity.onBackInvoked();assertTrue(activity.isFinishing());
        }finally{activity.finish();}
    }
    static void settle(NativeHistoryBrowseView view)throws Exception{for(int i=0;i<15;i++){Thread.sleep(5);view.measure(View.MeasureSpec.makeMeasureSpec(1920,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(1080,View.MeasureSpec.EXACTLY));view.layout(0,0,1920,1080);org.robolectric.shadows.ShadowLooper.idleMainLooper(100,java.util.concurrent.TimeUnit.MILLISECONDS);}}

}
