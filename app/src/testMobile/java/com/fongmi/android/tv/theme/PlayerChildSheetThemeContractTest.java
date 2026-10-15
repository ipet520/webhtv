package com.fongmi.android.tv.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
public class PlayerChildSheetThemeContractTest {

    @Test
    public void childSheetTextUsesPlayerGlassSemanticForEveryMode() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            TextView track = (TextView) LayoutInflater.from(context)
                    .inflate(R.layout.adapter_track, null, false);
            ColorStateList text = track.getTextColors();
            assertEquals(tokens.colorPlayerControlMuted(), text.getDefaultColor());
            assertEquals(tokens.colorPlayerControlActive(), text.getColorForState(
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0));
            assertEquals(tokens.colorPlayerControlActive(), text.getColorForState(
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}, 0));
            assertEquals(tokens.colorPlayerControlActive(), text.getColorForState(
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_activated}, 0));
            assertEquals(tokens.colorPlayerControlActive(), text.getColorForState(
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed}, 0));
            assertEquals((int) (tokens.colorPlayerControlMuted() & 0x00FFFFFF),
                    text.getColorForState(new int[]{-android.R.attr.state_enabled}, 0)
                            & 0x00FFFFFF);
        }
    }

    @Test
    public void controlSheetCallerResolvesFocusedStateAgainstPlayerGlass() {
        for (boolean dark : new boolean[]{false, true}) {
            Context context = themed(dark);
            ThemeTokens tokens = dark ? ThemeTokens.dark() : ThemeTokens.light();
            ViewGroup root = (ViewGroup) LayoutInflater.from(context)
                    .inflate(R.layout.view_danmaku_appearance, null, false);
            TextView gradient = root.findViewById(R.id.colorGradient);
            ColorStateList text = gradient.getTextColors();
            Drawable background = gradient.getBackground();

            assertStatePair(text, background, new int[]{android.R.attr.state_enabled},
                    tokens.colorPlayerControlMuted(), tokens.colorPlayerScrim());
            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_focused},
                    tokens.colorPlayerControlActive(), 0xB3000000);
            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_pressed},
                    tokens.colorPlayerControlActive(), 0xB3000000);
            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_selected},
                    tokens.colorPlayerControlActive(), 0xB3000000);
            assertStatePair(text, background,
                    new int[]{android.R.attr.state_enabled, android.R.attr.state_activated},
                    tokens.colorPlayerControlActive(), 0xB3000000);
        }
    }

    private static void assertStatePair(ColorStateList text, Drawable background, int[] state,
                                        int expectedText, int expectedBackground) {
        assertEquals(expectedText, text.getColorForState(state, 0));
        background.setState(state);
        assertEquals("background=" + Integer.toHexString(expectedBackground)
                        + " class=" + background.getClass().getName()
                        + " state=" + java.util.Arrays.toString(background.getState()),
                expectedBackground, visibleFill(background));
        assertTrue(ThemeContrast.ratio(expectedText, expectedBackground) >= 4.5);
    }

    private static int visibleFill(Drawable drawable) {
        if (drawable instanceof android.graphics.drawable.StateListDrawable states) {
            return visibleFill(states.getCurrent());
        }
        if (drawable instanceof android.graphics.drawable.RippleDrawable ripple) {
            for (int index = 0; index < ripple.getNumberOfLayers(); index++) {
                int color = visibleFill(ripple.getDrawable(index));
                if (color != 0) return color;
            }
            return 0;
        }
        if (drawable instanceof android.graphics.drawable.LayerDrawable layer) {
            for (int index = 0; index < layer.getNumberOfLayers(); index++) {
                int color = visibleFill(layer.getDrawable(index));
                if (color != 0) return color;
            }
            return 0;
        }
        if (drawable instanceof android.graphics.drawable.InsetDrawable inset) {
            return visibleFill(inset.getDrawable());
        }
        if (drawable instanceof android.graphics.drawable.GradientDrawable gradient
                && gradient.getColor() != null) {
            return gradient.getColor().getDefaultColor();
        }
        return 0;
    }

    private static Context themed(boolean dark) {
        Configuration configuration = new Configuration(
                RuntimeEnvironment.getApplication().getResources().getConfiguration());
        configuration.uiMode = (configuration.uiMode & ~Configuration.UI_MODE_NIGHT_MASK)
                | (dark ? Configuration.UI_MODE_NIGHT_YES : Configuration.UI_MODE_NIGHT_NO);
        Context context = RuntimeEnvironment.getApplication().createConfigurationContext(configuration);
        context.setTheme(R.style.Theme_App);
        return context;
    }
}
