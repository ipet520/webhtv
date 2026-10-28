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
}
