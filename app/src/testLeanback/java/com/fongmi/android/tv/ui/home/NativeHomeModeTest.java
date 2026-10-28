package com.fongmi.android.tv.ui.home;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.leanback.widget.HorizontalGridView;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.homepreview.NativeHomeBrowseView;
import com.github.catvod.Init;
import com.github.catvod.utils.Prefers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = NativeHomeBusinessTest.FixtureApp.class, qualifiers = "w960dp-h540dp-land-xhdpi")
public class NativeHomeModeTest {
    @Before public void init() {
        Init.set(RuntimeEnvironment.getApplication());
        Prefers.getPrefers().edit().clear().commit();
    }

    @Test public void defaultsToClassicAndPersistsEnumThroughExistingPreferences() {
        assertTrue(Setting.isLeanbackHomeStyleAvailable());
        assertEquals(Setting.LeanbackHomeMode.CLASSIC, Setting.getLeanbackHomeMode());
        Setting.putLeanbackHomeMode(Setting.LeanbackHomeMode.IMMERSIVE);
        assertEquals("IMMERSIVE", Prefers.getString("leanback_native_home_mode"));
        Init.set(RuntimeEnvironment.getApplication());
        assertEquals(Setting.LeanbackHomeMode.IMMERSIVE, Setting.getLeanbackHomeMode());
        Setting.putLeanbackHomeMode(Setting.LeanbackHomeMode.CLASSIC);
        assertEquals(Setting.LeanbackHomeMode.CLASSIC, Setting.getLeanbackHomeMode());
        Prefers.put("leanback_native_home_mode", "corrupt");
        assertEquals(Setting.LeanbackHomeMode.CLASSIC, Setting.getLeanbackHomeMode());
    }

    @Test public void leanbackReleaseAndDebugHonorModeWhileMobileIgnoresIt() {
        assertEquals(Setting.LeanbackHomeMode.IMMERSIVE, Setting.resolveLeanbackHomeMode(true, "leanback", "IMMERSIVE"));
        assertEquals(Setting.LeanbackHomeMode.IMMERSIVE, Setting.resolveLeanbackHomeMode(false, "leanback", "IMMERSIVE"));
        assertEquals(Setting.LeanbackHomeMode.CLASSIC, Setting.resolveLeanbackHomeMode(false, "leanback", "CLASSIC"));
        assertEquals(Setting.LeanbackHomeMode.CLASSIC, Setting.resolveLeanbackHomeMode(false, "leanback", null));
        assertEquals(Setting.LeanbackHomeMode.CLASSIC, Setting.resolveLeanbackHomeMode(true, "mobile", "IMMERSIVE"));
        assertEquals(Setting.LeanbackHomeMode.CLASSIC, Setting.resolveLeanbackHomeMode(false, "mobile", "IMMERSIVE"));
    }

    @Test public void sameActivityHostHasOneFocusableSurfaceAndNoDuplicateOnRepeatedShow() {
        try (var controller = Robolectric.buildActivity(Activity.class).setup()) {
            Activity activity = controller.get();
            activity.setTheme(R.style.Theme_App);
            FrameLayout root = new FrameLayout(activity);
            FrameLayout classic = new FrameLayout(activity);
            Button oldButton = new Button(activity);
            oldButton.setText("classic");
            classic.addView(oldButton);
            View toolbar = new View(activity);
            root.addView(classic, new FrameLayout.LayoutParams(-1, -1));
            root.addView(toolbar);
            activity.setContentView(root);
            AtomicInteger settings = new AtomicInteger();
            Prefers.put("home_button", "2,3,6,4,5,7");
            NativeHomeModeHost host = new NativeHomeModeHost(activity, root, classic, toolbar, () -> {}, new com.fongmi.android.tv.ui.presenter.FuncPresenter.OnClickListener() {
                public void onItemClick(com.fongmi.android.tv.bean.Func item) { if (item.getResId() == R.string.home_setting) settings.incrementAndGet(); }
                public boolean onLongClick(com.fongmi.android.tv.bean.Func item) { return false; }
            }, null);
            for (int iteration = 0; iteration < 3; iteration++) {
                host.show();
                host.show();
                layout(root);
                assertTrue(host.isVisible());
                assertEquals(3, root.getChildCount());
                assertEquals(View.GONE, classic.getVisibility());
                assertEquals(ViewGroup.FOCUS_BLOCK_DESCENDANTS, classic.getDescendantFocusability());
                assertFalse(oldButton.hasFocus());
                NativeHomeBrowseView immersive = (NativeHomeBrowseView) root.getChildAt(2);
                HorizontalGridView functions = immersive.findViewById(R.id.nhp_functions);
                Button setting = (Button) functions.findViewHolderForAdapterPosition(5).itemView;
                assertEquals(activity.getString(R.string.home_setting), setting.getText().toString());
                setting.performClick();
                assertEquals(iteration + 1, settings.get());
                Bundle state = host.saveState();
                assertNotNull(state);
                host.hide();
                host.hide();
                assertFalse(host.isVisible());
                assertEquals(2, root.getChildCount());
                assertEquals(View.VISIBLE, classic.getVisibility());
                assertEquals(View.VISIBLE, toolbar.getVisibility());
                assertTrue(oldButton.requestFocus());
            }
            host.release();
        }
    }

    private static void layout(View root) {
        root.measure(View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY));
        root.layout(0, 0, 1920, 1080);
        ShadowLooper.idleMainLooper(300, TimeUnit.MILLISECONDS);
    }
}

