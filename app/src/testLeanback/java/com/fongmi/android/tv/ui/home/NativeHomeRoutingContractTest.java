package com.fongmi.android.tv.ui.home;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

/** Verify narrow integration edges alongside executable host/settings tests, not TV acceptance. */
public class NativeHomeRoutingContractTest {
    private static Path src() { return Files.isDirectory(Path.of("src")) ? Path.of("src") : Path.of("app/src"); }
    private static String home() throws Exception { return Files.readString(src().resolve("leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java")); }
    private static String section(String text, String start, String end) {
        int a = text.indexOf(start);
        int b = text.indexOf(end, a + start.length());
        assertTrue(start, a >= 0 && b > a);
        return text.substring(a, b);
    }

    @Test public void webHomeAlwaysPrecedesNativeModeAndFallbackRemainsExplicit() throws Exception {
        String text = home();
        String route = section(text, "private void getVideo(boolean forceNative)", "private boolean isImmersiveHomeVisible()");
        assertTrue(route.indexOf("mWeb.load(getHome())") < route.indexOf("showImmersiveNativeHome()"));
        assertTrue(route.indexOf("showImmersiveNativeHome()") < route.indexOf("mViewModel.homeContent()"));
        assertTrue(route.contains("if (!forceNative && mWeb != null && mWeb.load(getHome()))"));
        String error = section(text, "public void onWebError()", "public void setToolbar(boolean visible)");
        assertTrue(error.contains("getVideo(true)"));
        String openVod = section(text, "public void openVod()", "public void openSite()");
        assertTrue(openVod.contains("getVideo(true)"));
    }

    @Test public void resumeDoesNotReplaceWebOrStartAnotherHomeActivity() throws Exception {
        String text = home();
        String resume = section(text, "private void applyNativeHomeModeOnResume()", "private void showWebOverlay()");
        assertTrue(resume.contains("!mNativeHomeModeReady || requested == mAppliedNativeHomeMode"));
        assertTrue(resume.contains("mWeb != null && mWeb.isVisible()"));
        assertTrue(resume.indexOf("mAppliedNativeHomeMode = requested;") < resume.indexOf("getVideo(mLastHomeForceNative)"));
        assertTrue(resume.contains("showNoConfig()"));
        assertFalse(resume.contains("startActivity"));
        assertFalse(text.contains("NativeHomePreviewActivity"));
        String onResume = section(text, "protected void onResume()", "private void resumeDeferredFragmentWork()");
        assertTrue(onResume.contains("applyNativeHomeModeOnResume()"));
        assertTrue(onResume.contains("if (!isImmersiveHomeVisible())"));
    }

    @Test public void titleOnlyAppearanceRowUsesExistingChoiceDialogWithTwoExactChoices() throws Exception {
        String text = Files.readString(src().resolve("leanback/java/com/fongmi/android/tv/ui/dialog/AppearanceDialog.java"));
        assertTrue(text.contains("if (Setting.isLeanbackHomeStyleAvailable())"));
        assertTrue(text.contains("homeStyleValue = addRow(content, R.string.setting_native_home_style, getHomeStyleText(), this::chooseHomeStyle)"));
        String choice = section(text, "private void chooseHomeStyle(View view)", "void onThemeProfileApplied()");
        assertTrue(choice.contains("ChoiceDialog.showSingle(this, R.string.setting_native_home_style"));
        assertTrue(choice.contains("String[] modes = {getString(R.string.native_home_classic), getString(R.string.native_home_immersive)}"));
        String resources = Files.readString(src().resolve("leanback/res/values/native_home_mode.xml"));
        assertTrue(resources.contains(">主页样式</string>"));
        assertTrue(resources.contains(">经典模式</string>"));
        assertTrue(resources.contains(">沉浸模式</string>"));
        assertFalse(Files.readString(src().resolve("mobile/java/com/fongmi/android/tv/ui/activity/HomeActivity.java")).contains("NativeHomeModeHost"));
        assertFalse(Files.readString(src().resolve("mobile/java/com/fongmi/android/tv/ui/dialog/AppearanceDialog.java")).contains("setting_native_home_style"));
    }
}
