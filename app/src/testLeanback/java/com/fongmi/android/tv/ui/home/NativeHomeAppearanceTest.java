package com.fongmi.android.tv.ui.home;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;

import com.fongmi.android.tv.App;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.ui.dialog.AppearanceDialog;
import com.fongmi.android.tv.ui.dialog.ChoiceDialog;
import com.github.catvod.utils.Prefers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.util.ReflectionHelpers;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = App.class)
public class NativeHomeAppearanceTest {
    public static class Host extends AppCompatActivity {
        @Override protected void onCreate(Bundle state) {
            setTheme(R.style.Theme_App);
            super.onCreate(state);
        }
    }

    @Before public void reset() { Prefers.getPrefers().edit().clear().commit(); }

    @Test public void titleOnlyRowOpensExistingChoiceWithClassicDefaultAndPersistsConfirmation() {
        try (var controller = Robolectric.buildActivity(Host.class).setup()) {
            Host activity = controller.get();
            AppearanceDialog.show(activity);
            activity.getSupportFragmentManager().executePendingTransactions();
            AppearanceDialog appearance = (AppearanceDialog) activity.getSupportFragmentManager().findFragmentByTag("AppearanceDialog");
            View title = text(appearance.requireDialog().getWindow().getDecorView(), "主页样式");
            assertNotNull(title);
            ViewGroup row = (ViewGroup) title.getParent();
            assertTrue("The home-style entry row must remain visible", row.isShown());
            assertTrue(title.isShown());
            assertTrue(row.isFocusable());
            assertTrue(row.isClickable());
            assertTrue(row.requestFocus());
            assertTrue(row.hasFocus());
            // addRow returns the summary TextView, not the clickable row.
            assertEquals(View.VISIBLE, row.getChildAt(1).getVisibility());
            assertEquals("经典", ((TextView)row.getChildAt(1)).getText().toString());
            List<String> visible = new ArrayList<>();
            collectText(row, visible);
            assertEquals(List.of("主页样式", "经典"), visible);
            assertEquals(58 * activity.getResources().getDisplayMetrics().density, row.getLayoutParams().height, 1);
            assertEquals(15 * activity.getResources().getDisplayMetrics().scaledDensity, ((TextView) title).getTextSize(), 1);
            assertNotNull(row.getBackground());
            assertTrue(row.performClick());
            appearance.getChildFragmentManager().executePendingTransactions();
            ChoiceDialog choice = (ChoiceDialog) appearance.getChildFragmentManager().findFragmentByTag("ChoiceDialog");
            assertNotNull(choice);
            assertArrayEquals(new CharSequence[]{"经典模式", "沉浸模式"}, (CharSequence[]) ReflectionHelpers.getField(choice, "items"));
            assertEquals(0, (int) ReflectionHelpers.getField(choice, "selected"));
            View immersive = text(choice.requireDialog().getWindow().getDecorView(), "沉浸模式");
            assertNotNull(immersive);
            clickRow(immersive);
            assertEquals(Setting.LeanbackHomeMode.IMMERSIVE, Setting.getLeanbackHomeMode());
            assertEquals("沉浸", ((TextView)row.getChildAt(1)).getText().toString());
            appearance.getChildFragmentManager().executePendingTransactions();
            row.performClick();
            appearance.getChildFragmentManager().executePendingTransactions();
            choice = (ChoiceDialog) appearance.getChildFragmentManager().findFragmentByTag("ChoiceDialog");
            assertEquals(1, (int) ReflectionHelpers.getField(choice, "selected"));
            choice.dismiss();
            appearance.getChildFragmentManager().executePendingTransactions();
            assertEquals(Setting.LeanbackHomeMode.IMMERSIVE, Setting.getLeanbackHomeMode());
            appearance.dismiss();
        }
    }

    @Test public void reopenedAppearanceCanConfirmClassicAgain() {
        Setting.putLeanbackHomeMode(Setting.LeanbackHomeMode.IMMERSIVE);
        try (var controller = Robolectric.buildActivity(Host.class).setup()) {
            AppearanceDialog.show(controller.get());
            controller.get().getSupportFragmentManager().executePendingTransactions();
            DialogFragment appearance = (DialogFragment) controller.get().getSupportFragmentManager().findFragmentByTag("AppearanceDialog");
            View title = text(appearance.requireDialog().getWindow().getDecorView(), "主页样式");
            ((View) title.getParent()).performClick();
            appearance.getChildFragmentManager().executePendingTransactions();
            DialogFragment choice = (DialogFragment) appearance.getChildFragmentManager().findFragmentByTag("ChoiceDialog");
            clickRow(text(choice.requireDialog().getWindow().getDecorView(), "经典模式"));
            assertEquals(Setting.LeanbackHomeMode.CLASSIC, Setting.getLeanbackHomeMode());
            appearance.dismiss();
        }
    }

    private static void clickRow(View view) {
        View current = view;
        while (!current.isClickable() && current.getParent() instanceof View parent) current = parent;
        assertTrue(current.isClickable());
        current.performClick();
    }

    private static View text(View root, String expected) {
        if (root instanceof TextView text && expected.contentEquals(text.getText())) return root;
        if (root instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) {
            View found = text(group.getChildAt(i), expected);
            if (found != null) return found;
        }
        return null;
    }

    private static void collectText(View root, List<String> values) {
        if (root.getVisibility() != View.VISIBLE) return;
        if (root instanceof TextView text && text.length() > 0) values.add(text.getText().toString());
        if (root instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) collectText(group.getChildAt(i), values);
    }
}
