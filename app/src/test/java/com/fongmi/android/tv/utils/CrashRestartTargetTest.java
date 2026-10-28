package com.fongmi.android.tv.utils;

import com.fongmi.android.tv.setting.AppBranding;
import com.fongmi.android.tv.ui.activity.HomeActivity;
import com.fongmi.android.tv.ui.activity.CrashActivity;
import cat.ereza.customactivityoncrash.CustomActivityOnCrash;
import cat.ereza.customactivityoncrash.config.CaocConfig;
import com.github.catvod.Init;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import java.nio.file.*;
import javax.xml.parsers.DocumentBuilderFactory;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,manifest=Config.NONE,application=com.fongmi.android.tv.lab.MihomoRoutingIntegrationTest.FixtureApp.class)
public class CrashRestartTargetTest {
    private Path root() {
        Path p=Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while(p!=null && !Files.exists(p.resolve("app/src/main/AndroidManifest.xml")))p=p.getParent();
        if(p==null)throw new IllegalStateException("source root not found");
        return p;
    }
    @Test public void startupExplicitlySelectsRealActivityAndKeepsCrashHandlerConfiguration() throws Exception {
        String source=Files.readString(root().resolve("app/src/main/java/com/fongmi/android/tv/Startup.java"));
        assertTrue(source.contains(".restartActivity(HomeActivity.class)"));
        assertTrue(source.contains(".errorActivity(CrashActivity.class)"));
        assertTrue(source.contains(".trackActivities(true)"));
        assertTrue(source.contains("BACKGROUND_MODE_SHOW_CUSTOM"));
        CaocConfig old=CustomActivityOnCrash.getConfig();
        try {
            CaocConfig.Builder.create().restartActivity(HomeActivity.class).errorActivity(CrashActivity.class).apply();
            assertSame(HomeActivity.class,CustomActivityOnCrash.getConfig().getRestartActivityClass());
            assertSame(HomeActivity.class,Class.forName(CustomActivityOnCrash.getConfig().getRestartActivityClass().getName()));
            assertSame(CrashActivity.class,CustomActivityOnCrash.getConfig().getErrorActivityClass());
        }finally{CustomActivityOnCrash.setConfig(old);}
    }
    @Test public void bothFlavorAliasesStillTargetHomeAndErrorProcessRemainsSeparate() throws Exception {
        var factory=DocumentBuilderFactory.newInstance();factory.setNamespaceAware(true);
        String ns="http://schemas.android.com/apk/res/android";
        for(String flavor:new String[]{"mobile","leanback"}) {
            var doc=factory.newDocumentBuilder().parse(root().resolve("app/src/"+flavor+"/AndroidManifest.xml").toFile());
            var aliases=doc.getElementsByTagName("activity-alias");int found=0;
            for(int i=0;i<aliases.getLength();i++) {
                var node=(org.w3c.dom.Element)aliases.item(i);
                if(node.getAttributeNS(ns,"name").matches(".*HomeActivity(Current|History)$")) {
                    assertEquals(".ui.activity.HomeActivity",node.getAttributeNS(ns,"targetActivity"));found++;
                }
            }
            assertEquals(2,found);
        }
        String manifest=Files.readString(root().resolve("app/src/main/AndroidManifest.xml"));
        assertTrue(manifest.contains("android:process=\":error_activity\""));
    }
    @Test public void iconChoiceChangesLauncherAliasButNeverTheRestartClass() {
        Init.set(RuntimeEnvironment.getApplication());
        int old=AppBranding.getIconMode();
        try {
            for(int mode:new int[]{AppBranding.ICON_CURRENT,AppBranding.ICON_HISTORY}) {
                AppBranding.putIconMode(mode);
                var intent=AppBranding.launcherIntent(RuntimeEnvironment.getApplication());
                assertEquals(HomeActivity.class.getName()+(mode==AppBranding.ICON_HISTORY?"History":"Current"),intent.getComponent().getClassName());
                assertSame(HomeActivity.class,Class.forName(HomeActivity.class.getName()));
            }
        }catch(ClassNotFoundException e){throw new AssertionError(e);}finally{AppBranding.putIconMode(old);}
    }
}
