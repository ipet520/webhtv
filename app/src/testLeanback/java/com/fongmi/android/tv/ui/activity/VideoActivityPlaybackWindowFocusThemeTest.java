package com.fongmi.android.tv.ui.activity;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.theme.ThemeBinder;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.theme.ThemeMode;
import com.fongmi.android.tv.theme.ThemeProfile;
import com.fongmi.android.tv.theme.ThemeResolver;
import com.fongmi.android.tv.theme.ThemeSeed;
import com.fongmi.android.tv.theme.ThemeTokens;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.shadows.ShadowGradientDrawable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Contracts for the playback-window selector's opt-in runtime Focus binding. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
public class VideoActivityPlaybackWindowFocusThemeTest {

    public static class Host extends AppCompatActivity {
        @Override
        protected void onCreate(android.os.Bundle state) {
            setTheme(R.style.Theme_App);
            super.onCreate(state);
        }
    }

    @Test
    public void initialAndRestoredPlaybackSelectorUseRuntimeFocusWithoutChangingStageClears() throws Exception {
        String source = read("src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java");
        String initView = section(source, "protected void initView(Bundle savedInstanceState)", "private void applyPlaybackOverlay()");
        String bindHelper = section(source, "private void bindVideoFocusStroke()", "private void restoreVideoFocusForeground()");
        String restoreHelper = section(source, "private void restoreVideoFocusForeground()", "private void restoreEmbeddedVideoLayoutAfterFullscreen()");
        String exitFullscreen = section(source, "private void exitFullscreen()", "private void bindVideoFocusStroke()");
        String audioStage = section(source, "private void applyAudioStageLayout(boolean visible)", "private void setVideoDetailsVisible(boolean visible)");

        assertTrue("the initial XML selector receives runtime Focus binding after view initialization",
                initView.indexOf("mFrameParams = mBinding.video.getLayoutParams()") >= 0
                        && initView.indexOf("bindVideoFocusStroke()") > initView.indexOf("mFrameParams = mBinding.video.getLayoutParams()"));
        assertTrue(bindHelper.contains("ThemeController.bindFocusStroke(mBinding.video, 1.5f)"));
        assertTrue("restoration installs the original selector before rebinding it",
                restoreHelper.indexOf("setForeground(ResUtil.getDrawable(R.drawable.selector_video))")
                        < restoreHelper.indexOf("bindVideoFocusStroke()"));
        assertTrue(exitFullscreen.contains("mBinding.video.setForeground(null)"));
        assertTrue(exitFullscreen.contains("restoreVideoFocusForeground()"));
        assertTrue(audioStage.contains("mBinding.video.setForeground(null)"));
        assertTrue(audioStage.contains("restoreVideoFocusForeground()"));
        assertFalse(exitFullscreen.contains("setForeground(ResUtil.getDrawable(R.drawable.selector_video))"));
        assertFalse(audioStage.contains("setForeground(ResUtil.getDrawable(R.drawable.selector_video))"));
    }

    @Test
    public void playbackSelectorFollowsFocusPaletteAcrossClearRestoreAndRebind() {
        try (var activity = Robolectric.buildActivity(Host.class).setup()) {
            Host host = activity.get();
            ThemeTokens original = ThemeController.current();
            FrameLayout root = new FrameLayout(host);
            FrameLayout video = new FrameLayout(host);
            root.addView(video);
            Drawable sourceSelector = ContextCompat.getDrawable(host, R.drawable.selector_video);
            assertTrue(sourceSelector instanceof StateListDrawable);
            video.setForeground(sourceSelector);

            ThemeTokens first = palette("#234A91", "#D81B60");
            ThemeTokens second = palette("#285C3A", "#7B1FA2");
            try {
                ThemeController.apply(host, first);
                ThemeController.bindFocusStroke(video, 1.5f);
                StateListDrawable firstSelector = (StateListDrawable) video.getForeground();
                GradientDrawable firstShape = focusedShape(firstSelector);
                assertNotSame("binding keeps a view-local copy of the source selector", sourceSelector, firstSelector);
                assertEquals(first.colorFocus(), stroke(firstShape));
                assertEquals("the selector retains its original 1.5dp stroke", Math.round(1.5f * host.getResources().getDisplayMetrics().density),
                        Shadow.<ShadowGradientDrawable>extract(firstShape).getStrokeWidth());
                float radius = firstShape.getCornerRadius();

                video.setForeground(null);
                assertNull("fullscreen/audio stage continues to clear the foreground", video.getForeground());
                ThemeController.apply(host, second);
                ThemeController.bindTheme(video);
                assertNull("theme rebinding must not restore the foreground while the stage owns the view", video.getForeground());

                video.setForeground(ContextCompat.getDrawable(host, R.drawable.selector_video));
                ThemeController.bindFocusStroke(video, 1.5f);
                StateListDrawable restoredSelector = (StateListDrawable) video.getForeground();
                GradientDrawable restoredShape = focusedShape(restoredSelector);
                assertEquals("restoration uses the currently active Focus color", second.colorFocus(), stroke(restoredShape));
                assertEquals("restoration preserves the selector stroke width", Math.round(1.5f * host.getResources().getDisplayMetrics().density),
                        Shadow.<ShadowGradientDrawable>extract(restoredShape).getStrokeWidth());
                assertEquals("restoration preserves source corner geometry", radius, restoredShape.getCornerRadius(), 0.01f);

                ThemeTokens third = palette("#74501B", "#00897B");
                ThemeController.apply(host, third);
                ThemeController.bindTheme(root);
                assertEquals("subsequent theme changes rebind the same view-local Focus outline",
                        third.colorFocus(), stroke(focusedShape((StateListDrawable) video.getForeground())));
            } finally {
                ThemeController.apply(host, original);
                ThemeController.bindTheme(root);
            }
        }
    }

    private static ThemeTokens palette(String primary, String focus) {
        ThemeProfile profile = ThemeProfile.defaultProfile();
        profile.light.primary = primary;
        profile.light.focus = focus;
        return ThemeResolver.resolve(ThemeMode.LIGHT, ThemeSeed.NONE, 0, 0, profile, null, false);
    }

    private static GradientDrawable focusedShape(StateListDrawable states) {
        for (int index = 0; index < states.getStateCount(); index++) {
            for (int state : states.getStateSet(index)) {
                if (state == android.R.attr.state_focused) {
                    Drawable drawable = states.getStateDrawable(index);
                    if (drawable instanceof GradientDrawable shape) return shape;
                    throw new AssertionError("focused playback selector item is not a GradientDrawable");
                }
            }
        }
        throw new AssertionError("playback selector has no focused state");
    }

    private static int stroke(GradientDrawable drawable) {
        return Shadow.<ShadowGradientDrawable>extract(drawable).getStrokeColor();
    }

    private static String section(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        if (from < 0 || to <= from) throw new AssertionError("Missing source section: " + start);
        return source.substring(from, to);
    }

    private static String read(String relativePath) throws Exception {
        Path root = Files.exists(Path.of("src", "leanback")) ? Path.of("") : Path.of("app");
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
