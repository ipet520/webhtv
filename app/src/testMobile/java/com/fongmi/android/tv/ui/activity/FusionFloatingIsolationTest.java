package com.fongmi.android.tv.ui.activity;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;
public class FusionFloatingIsolationTest {
    @Test public void mobileCannotConstructFloatingPanelAndSystemPipGateIsPreserved()throws Exception{
        String s=Files.readString(Path.of("src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java"));
        assertTrue(s.contains("if (Util.isLeanback() && modeController.isFusionMode() && fusionFloating == null)"));
        assertEquals(1,s.split("new FusionFloatingPanel\\(",-1).length-1);
        assertTrue(s.contains("return Util.isMobile() && !PiP.noPiP();"));
        String manifest=Files.readString(Path.of("src/mobile/AndroidManifest.xml"));assertTrue(manifest.contains("android:supportsPictureInPicture=\"true\""));
    }
}
