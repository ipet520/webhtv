package com.fongmi.android.tv.player;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.Format;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.setting.PlayerSetting;
import com.fongmi.android.tv.ui.custom.PlayerOsdController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28, manifest=Config.NONE, application=com.fongmi.android.tv.lab.MihomoRoutingIntegrationTest.FixtureApp.class)
public class C55MediaBadgeIntegrationTest {
    private Path root() {
        Path p=Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while(p!=null && !Files.exists(p.resolve("app/src/main/AndroidManifest.xml")))p=p.getParent();
        return p;
    }
    @Test public void catalogueHasExactly127UniqueWebpAndExplicitReleaseReferences() throws Exception {
        var rules=MediaBadgeCatalog.rules();
        assertEquals(127,rules.length);
        assertEquals(127,MediaBadgeCatalog.patterns().length);
        var keys=new HashSet<String>();
        String source=Files.readString(root().resolve("app/src/main/java/com/fongmi/android/tv/ui/custom/PlayerOsdController.java"));
        for(var rule:rules) {
            assertTrue(keys.add(rule.plate()));
            byte[] bytes=Files.readAllBytes(root().resolve("app/src/main/res/drawable-nodpi/badge_"+rule.plate()+".webp"));
            assertEquals("RIFF",new String(bytes,0,4,java.nio.charset.StandardCharsets.US_ASCII));
            assertEquals("WEBP",new String(bytes,8,4,java.nio.charset.StandardCharsets.US_ASCII));
            assertTrue(source.contains("R.drawable.badge_"+rule.plate()));
        }
    }
    @Test public void knownSdrAndAvcCannotBeOverriddenByFilenameHdrHevc() {
        Format video=new Format.Builder().setWidth(1920).setHeight(1080)
                .setSampleMimeType("video/avc")
                .setColorInfo(new ColorInfo.Builder().setColorTransfer(C.COLOR_TRANSFER_SDR).build()).build();
        var badges=MediaBadgeResolver.resolve(video,null,null,"https://host/film.2160p.HDR10.x265.mkv",null);
        assertTrue(badges.stream().anyMatch(b -> "gr_1080p".equals(b.plate())));
        assertTrue(badges.stream().anyMatch(b -> "ge_avc".equals(b.plate())));
        assertFalse(badges.stream().anyMatch(b -> b.group()==MediaBadgeCatalog.Group.DYNAMIC_RANGE));
        assertFalse(badges.stream().anyMatch(b -> "ge_hevc".equals(b.plate())));
    }
    @Test public void unknownFactsRemainAbsentAndNonFiniteFrameRateIsNotAClaim() {
        assertTrue(MediaBadgeResolver.resolve(null,null,null,"https://host/play",null).isEmpty());
        // Media3 rejects NaN but accepts positive infinity and its NO_VALUE sentinel.
        assertThrows(IllegalArgumentException.class,() -> new Format.Builder().setFrameRate(Float.NaN));
        for(float fps:new float[]{Float.POSITIVE_INFINITY,Format.NO_VALUE}) {
            var badges=MediaBadgeResolver.resolve(new Format.Builder().setFrameRate(fps).build(),null,null,null,null);
            assertFalse(badges.stream().anyMatch(b -> b.group()==MediaBadgeCatalog.Group.FRAME_RATE));
        }
    }
    private PlayerOsdController controller(View root) {
        return new PlayerOsdController(root,root.findViewById(R.id.osdTopLeft),root.findViewById(R.id.osdTopRight),
                root.findViewById(R.id.osdBottomLeft),root.findViewById(R.id.osdBottomRight),
                root.findViewById(R.id.osdDiagnostics),root.findViewById(R.id.osdMiniProgress),new PlayerOsdController.Source(){
            public PlayerManager getPlayer(){return null;}
            public String getTitle(){return "";}
        },10);
    }
    @Test public void mobileSuppressedControlsKeepPollingUntilMetadataArrives() throws Exception {
        Context ctx=new ContextThemeWrapper(RuntimeEnvironment.getApplication(),R.style.Theme_App);
        View root=LayoutInflater.from(ctx).inflate(R.layout.view_player_osd,null);
        PlayerOsdController c=controller(root);
        for(String name:new String[]{"suppressed","controlsVisible"}) {
            Field f=PlayerOsdController.class.getDeclaredField(name);f.setAccessible(true);f.setBoolean(c,true);
        }
        Method render=PlayerOsdController.class.getDeclaredMethod("render");render.setAccessible(true);
        assertEquals("no metadata yet must not permanently stop the OSD update loop",true,render.invoke(c));
        assertEquals(View.GONE,root.getVisibility());
    }
    @Test public void fourCornersRepositionWithinPortraitAndLandscapeViewports() throws Exception {
        Context ctx=new ContextThemeWrapper(RuntimeEnvironment.getApplication(),R.style.Theme_App);
        int old=PlayerSetting.getMediaBadgePosition();
        try {
            for(int[] size:new int[][]{{1920,1080},{1080,1920},{640,360}}) {
                View root=LayoutInflater.from(ctx).inflate(R.layout.view_player_osd,null);
                PlayerOsdController c=controller(root);
                ViewGroup badges=root.findViewById(R.id.osdMediaBadges);
                Method create=PlayerOsdController.class.getDeclaredMethod("createMediaBadge",MediaBadgeResolver.MediaBadge.class);create.setAccessible(true);
                badges.addView((View)create.invoke(c,new MediaBadgeResolver.MediaBadge("23.976FPS",MediaBadgeCatalog.Group.FRAME_RATE,null)));
                badges.setVisibility(View.VISIBLE);root.setVisibility(View.VISIBLE);
                Method position=PlayerOsdController.class.getDeclaredMethod("applyMediaBadgePosition");position.setAccessible(true);
                for(int corner=0;corner<4;corner++) {
                    PlayerSetting.putMediaBadgePosition(corner);position.invoke(c);
                    root.measure(View.MeasureSpec.makeMeasureSpec(size[0],View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(size[1],View.MeasureSpec.EXACTLY));
                    root.layout(0,0,size[0],size[1]);
                    assertTrue(badges.getLeft()>=0);assertTrue(badges.getTop()>=0);
                    assertTrue(badges.getRight()<=size[0]);assertTrue(badges.getBottom()<=size[1]);
                    assertTrue(badges.getMeasuredHeight()>0);
                    FrameLayout.LayoutParams params=(FrameLayout.LayoutParams)badges.getLayoutParams();
                    assertEquals(corner<2?android.view.Gravity.TOP:android.view.Gravity.BOTTOM,params.gravity&android.view.Gravity.VERTICAL_GRAVITY_MASK);
                }
            }
        }finally{PlayerSetting.putMediaBadgePosition(old);}
    }
    @Test public void positionAndPersistenceSurvivePreferenceAndBackupContracts() throws Exception {
        int old=PlayerSetting.getMediaBadgePosition();boolean on=PlayerSetting.isDisplayMediaBadges();
        try {
            for(int i=0;i<4;i++){PlayerSetting.putMediaBadgePosition(i);assertEquals(i,PlayerSetting.getMediaBadgePosition());}
            PlayerSetting.putMediaBadgePosition(99);assertEquals(3,PlayerSetting.getMediaBadgePosition());
            PlayerSetting.putDisplayMediaBadges(true);assertTrue(PlayerSetting.getDisplayChecked()[7]);
            PlayerSetting.putDisplayMediaBadges(false);assertFalse(PlayerSetting.getDisplayChecked()[7]);
            String backup=Files.readString(root().resolve("app/src/main/java/com/fongmi/android/tv/bean/Backup.java"));
            assertTrue(backup.contains("\"player_osd_media_badges\""));assertTrue(backup.contains("\"player_osd_media_badge_position\""));
            String osd=Files.readString(root().resolve("app/src/main/java/com/fongmi/android/tv/ui/custom/PlayerOsdController.java"));
            int signature=osd.indexOf("if (!next.equals(mediaBadgeSignature))");
            assertTrue(signature>=0);assertTrue(osd.indexOf("mediaBadges.removeAllViews()",signature)>signature);
        }finally{PlayerSetting.putMediaBadgePosition(old);PlayerSetting.putDisplayMediaBadges(on);}
    }
    @Test public void measuredControlsAreExcludedEvenWhenTallerThanUpstreamFixedInsets() throws Exception {
        Context ctx=new ContextThemeWrapper(RuntimeEnvironment.getApplication(),R.style.Theme_App);
        int old=PlayerSetting.getMediaBadgePosition();
        try {
            FrameLayout host=new FrameLayout(ctx);
            var activity=org.robolectric.Robolectric.buildActivity(android.app.Activity.class).setup().visible().get();
            activity.setContentView(host);
            View root=LayoutInflater.from(ctx).inflate(R.layout.view_player_osd,host,false);
            host.addView(root);root.setVisibility(View.VISIBLE);
            View control=new View(ctx);control.setId(R.id.control);
            host.addView(control,new FrameLayout.LayoutParams(-1,150,android.view.Gravity.BOTTOM));
            FrameLayout widget=new FrameLayout(ctx);widget.setId(R.id.widget);
            View top=new View(ctx);top.setId(R.id.top);
            widget.addView(top,new FrameLayout.LayoutParams(-1,100,android.view.Gravity.TOP));
            host.addView(widget,new FrameLayout.LayoutParams(-1,-1));
            PlayerOsdController c=controller(root);
            Field visible=PlayerOsdController.class.getDeclaredField("controlsVisible");visible.setAccessible(true);visible.setBoolean(c,true);
            ViewGroup badges=root.findViewById(R.id.osdMediaBadges);badges.setVisibility(View.VISIBLE);
            Method create=PlayerOsdController.class.getDeclaredMethod("createMediaBadge",MediaBadgeResolver.MediaBadge.class);create.setAccessible(true);
            for(int i=0;i<12;i++)badges.addView((View)create.invoke(c,new MediaBadgeResolver.MediaBadge("23.976FPS",MediaBadgeCatalog.Group.FRAME_RATE,null)));
            Method position=PlayerOsdController.class.getDeclaredMethod("applyMediaBadgePosition");position.setAccessible(true);
            for(int corner=0;corner<4;corner++) {
                host.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));
                host.layout(0,0,800,600);
                PlayerSetting.putMediaBadgePosition(corner);position.invoke(c);
                host.measure(View.MeasureSpec.makeMeasureSpec(800,View.MeasureSpec.EXACTLY),View.MeasureSpec.makeMeasureSpec(600,View.MeasureSpec.EXACTLY));
                host.layout(0,0,800,600);
                int[] point=new int[2];badges.getLocationInWindow(point);
                int[] edge=new int[2];
                if(corner<2) {
                    top.getLocationInWindow(edge);
                    assertTrue("badge top="+point[1]+" control bottom="+(edge[1]+top.getHeight()),point[1]>=edge[1]+top.getHeight());
                }else {
                    control.getLocationInWindow(edge);
                    assertTrue("badge bottom="+(point[1]+badges.getHeight())+" control top="+edge[1],point[1]+badges.getHeight()<=edge[1]);
                }
            }
        }finally{PlayerSetting.putMediaBadgePosition(old);}
    }
    @Test public void settingsBackupActuallyRoundTripsBothBadgePreferences() throws Exception {
        int old=PlayerSetting.getMediaBadgePosition();boolean on=PlayerSetting.isDisplayMediaBadges();
        try {
            var values=java.util.Map.of("player_osd_media_badges",(Object)true,"player_osd_media_badge_position",(Object)2);
            Method filter=com.fongmi.android.tv.bean.Backup.class.getDeclaredMethod("filter",java.util.Map.class,com.fongmi.android.tv.bean.SyncOptions.class);
            filter.setAccessible(true);
            var selected=(java.util.Map<?,?>)filter.invoke(null,values,new com.fongmi.android.tv.bean.SyncOptions().settings(true));
            assertEquals(values,selected);
            var omitted=(java.util.Map<?,?>)filter.invoke(null,values,new com.fongmi.android.tv.bean.SyncOptions().settings(false).spider(false));
            assertTrue(omitted.isEmpty());
            var payload=com.fongmi.android.tv.bean.Backup.objectFrom(new com.google.gson.Gson().toJson(java.util.Map.of("prefers",selected)));
            PlayerSetting.putDisplayMediaBadges(false);PlayerSetting.putMediaBadgePosition(0);
            Method restore=com.fongmi.android.tv.bean.Backup.class.getDeclaredMethod("restorePrefers",java.util.Map.class,boolean.class,boolean.class);
            restore.setAccessible(true);restore.invoke(null,payload.getPrefers(),false,false);
            assertTrue(PlayerSetting.isDisplayMediaBadges());assertEquals(2,PlayerSetting.getMediaBadgePosition());
        }finally{PlayerSetting.putMediaBadgePosition(old);PlayerSetting.putDisplayMediaBadges(on);}
    }
}
