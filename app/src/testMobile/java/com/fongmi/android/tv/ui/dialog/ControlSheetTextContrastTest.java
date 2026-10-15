package com.fongmi.android.tv.ui.dialog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;

import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
public class ControlSheetTextContrastTest {

    @Test
    public void selectedActivatedAndFocusedControlTextRemainWhite() {
        Context context = RuntimeEnvironment.getApplication();
        context.setTheme(R.style.Theme_App);
        View root = LayoutInflater.from(context).inflate(R.layout.dialog_control, null, false);
        View repeat = root.findViewById(R.id.repeat);
        assertNotNull("the test must exercise the real ControlSheetButton style", repeat);
        ColorStateList colors = ContextCompat.getColorStateList(context, R.color.selector_control_sheet_text);
        assertNotNull(colors);

        repeat.setSelected(true);
        assertEquals(0xFFFFFFFF, ((android.widget.TextView) repeat).getCurrentTextColor());
        assertEquals(0xFFFFFFFF, colors.getColorForState(
                new int[]{android.R.attr.state_enabled, android.R.attr.state_selected}, 0x00000000));
        assertEquals(0xFFFFFFFF, colors.getColorForState(
                new int[]{android.R.attr.state_enabled, android.R.attr.state_activated}, 0x00000000));
        assertEquals(0xFFFFFFFF, colors.getColorForState(
                new int[]{android.R.attr.state_enabled, android.R.attr.state_selected, android.R.attr.state_focused}, 0x00000000));
        assertEquals(0xFFFFFFFF, colors.getColorForState(
                new int[]{android.R.attr.state_enabled, android.R.attr.state_focused}, 0x00000000));

        repeat.setSelected(false);
        assertEquals(0xB3FFFFFF, ((android.widget.TextView) repeat).getCurrentTextColor());
        assertEquals(0xB3FFFFFF, colors.getDefaultColor());
        assertTrue("disabled text retains the existing muted alpha",
                ((colors.getColorForState(new int[]{-android.R.attr.state_enabled}, 0x00000000) >>> 24) & 0xFF) <= 0x74);
    }

    @Test
    public void playerControlBackgroundAndYellowStrokeAuthorityRemainUnchanged() throws Exception {
        String style = read("src/mobile/res/values/styles.xml");
        String background = read("src/mobile/res/drawable/selector_control_sheet_button.xml");

        assertTrue(style.contains("<item name=\"android:background\">@drawable/selector_control_sheet_button</item>"));
        assertTrue(style.contains("<item name=\"android:textColor\">@color/selector_control_sheet_text</item>"));
        assertTrue(background.contains("android:state_focused=\"true\""));
        assertTrue(background.contains("android:state_selected=\"true\""));
        assertTrue(background.contains("android:state_pressed=\"true\""));
        assertTrue(background.contains("android:state_activated=\"true\""));
        assertTrue(background.contains("android:color=\"?attr/webhtvColorPlayerControlActive\""));
        assertTrue(background.contains("android:width=\"2dp\""));
        assertTrue(background.contains("android:color=\"#B3000000\""));
        assertTrue(style.contains("<item name=\"android:layout_height\">34dp</item>"));
    }

    private static String read(String path) throws Exception {
        Path direct = Path.of(path);
        if (Files.exists(direct)) return Files.readString(direct, StandardCharsets.UTF_8);
        return Files.readString(Path.of("..").resolve(path), StandardCharsets.UTF_8);
    }
}