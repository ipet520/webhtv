package com.fongmi.android.tv.ui.homepreview;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextClock;
import androidx.leanback.widget.HorizontalGridView;
import androidx.recyclerview.widget.RecyclerView;
import com.fongmi.android.tv.R;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowDialog;
import org.robolectric.shadows.ShadowLooper;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class, qualifiers = "w960dp-h540dp-land-xhdpi")
public class NativeHomeVisualContractTest {
    private Activity activity;
    private NativeHomeBrowseView view;
    private final AtomicInteger exits = new AtomicInteger();
    private void create(int functions) {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.Theme_App);
        view = new NativeHomeBrowseView(activity, 18, functions, exits::incrementAndGet);
        activity.setContentView(view); layout();
        view.findViewById(R.id.nhp_home).requestFocus();
    }
    private void layout() {
        view.measure(View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, 1920, 1080);
        ShadowLooper.idleMainLooper(300, TimeUnit.MILLISECONDS);
    }
    @After public void close() { if (view != null) view.release(); if (activity != null) activity.finish(); }
    private HorizontalGridView grid(int id) { return view.findViewById(id); }
    private void key(int code) {
        boolean handled = view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        if (!handled && view.findFocus() != null) {
            int direction = code == KeyEvent.KEYCODE_DPAD_RIGHT ? View.FOCUS_RIGHT : View.FOCUS_LEFT;
            View next = view.findFocus().focusSearch(direction);
            if (next != null) next.requestFocus(direction);
        }
        view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code)); layout();
    }

    @Test public void oneHouseOpensRightFullHeightSourceDrawerAndNeverExits() {
        create(6);
        assertEquals(1, count(view, ImageButton.class));
        assertEquals(0, count(view, TextClock.class));
        View house = view.findViewById(R.id.nhp_home);
        house.performClick();
        ShadowLooper.idleMainLooper();
        Dialog drawer = ShadowDialog.getLatestDialog();
        assertTrue(drawer.isShowing());
        WindowManager.LayoutParams attributes = drawer.getWindow().getAttributes();
        assertEquals(-1, attributes.height);
        assertTrue(attributes.width >= 1920 * .18 && attributes.width <= 1920 * .23);
        List<Button> options = buttons(drawer.getWindow().getDecorView());
        assertTrue(options.get(0).isSelected());
        options.get(2).requestFocus();
        assertFalse(options.get(2).isSelected());
        options.get(2).performClick();
        assertFalse(drawer.isShowing());
        assertTrue(house.hasFocus());
        assertEquals(0, exits.get());
        house.performClick();
        ShadowLooper.idleMainLooper();
        drawer = ShadowDialog.getLatestDialog();
        assertTrue(buttons(drawer.getWindow().getDecorView()).get(2).isSelected());
        drawer.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK));
        drawer.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK));
        assertFalse(drawer.isShowing());
        assertTrue(house.hasFocus());
        house.performClick();
        ShadowLooper.idleMainLooper();
        drawer = ShadowDialog.getLatestDialog();
        view.restorePreviewState(null);
        view.release();
        assertFalse(drawer.isShowing());
        assertEquals(0, view.debugMetrics().getInt("pendingRestores"));
    }

    @Test public void normalTopOrderAndReferenceNormalizedBounds() {
        create(6);
        String[] expected = com.fongmi.android.tv.bean.HomeButton.all().stream().limit(6).map(b -> activity.getString(b.getResId())).toArray(String[]::new);
        for (int i = 0; i < expected.length; i++) {
            Button button = (Button) grid(R.id.nhp_functions).findViewHolderForAdapterPosition(i).itemView;
            assertEquals(expected[i], button.getText().toString());
        }
        View top = view.findViewById(R.id.nhp_top), hero = view.findViewById(R.id.nhp_hero);
        HorizontalGridView rail = grid(R.id.nhp_posters);
        View card = rail.findViewHolderForAdapterPosition(0).itemView;
        View poster = ((ViewGroup) card).getChildAt(0);
        assertEquals(.025f, top.getLeft() / 1920f, .005f);
        assertEquals(.16f, hero.getTop() / 1080f, .01f);
        float posterTop = (rail.getTop() + card.getTop()) / 1080f;
        assertEquals(.60f, posterTop, .02f);
        assertEquals(.324f, poster.getHeight() / 1080f, .012f);
        assertEquals(2f / 3, (float) poster.getWidth() / poster.getHeight(), .006f);
        assertTrue(((android.widget.TextView) view.findViewById(R.id.nhp_overview)).getMaxLines()>=2);
        System.out.println("NH03_GEOMETRY top=" + top.getTop() + " hero=" + hero.getTop() + " category=" + view.findViewById(R.id.nhp_category_row).getTop() + " posterTop=" + posterTop + " poster=" + poster.getWidth() + "x" + poster.getHeight());
    }

    @Test public void inlineFiltersHideOnlyTopAndLogoKeepSamePosterHostAndRestorePrimary() {
        create(6);
        key(KeyEvent.KEYCODE_DPAD_DOWN);
        key(KeyEvent.KEYCODE_DPAD_RIGHT);
        View primary = view.findFocus();
        HorizontalGridView rail = grid(R.id.nhp_posters);
        RecyclerView.Adapter<?> adapter = rail.getAdapter();
        int top = rail.getTop(), geometry = view.debugMetrics().getInt("geometry");
        primary.performClick(); layout();
        assertTrue(view.isFilterOpen());
        assertEquals(View.GONE, view.findViewById(R.id.nhp_top).getVisibility());
        assertEquals(View.GONE, view.findViewById(R.id.nhp_title_slot).getVisibility());
        assertEquals(View.VISIBLE, view.findViewById(R.id.nhp_meta).getVisibility());
        assertEquals(View.INVISIBLE, view.findViewById(R.id.nhp_category_row).getVisibility());
        assertSame(rail, grid(R.id.nhp_posters)); assertSame(adapter, rail.getAdapter());
        assertEquals(top, rail.getTop());
        assertEquals(geometry, view.debugMetrics().getInt("geometry"));
        key(KeyEvent.KEYCODE_DPAD_DOWN);
        assertTrue(grid(R.id.nhp_filter_year).hasFocus());
        Button year = (Button) grid(R.id.nhp_filter_year).findViewHolderForAdapterPosition(1).itemView;
        year.requestFocus(); year.performClick(); year.performClick();
        assertTrue(grid(R.id.nhp_filter_year).findViewHolderForAdapterPosition(0).itemView.isSelected());
        year.performClick();
        // Hidden categories cannot receive focus. Close before switching primary;
        // reopening restores scoped choices and preserves the same poster host.
        assertTrue(view.handleBack());
        View secondPrimary = grid(R.id.nhp_categories).findViewHolderForAdapterPosition(2).itemView;
        secondPrimary.requestFocus(); secondPrimary.performClick(); layout();
        assertTrue(grid(R.id.nhp_filter_year).findViewHolderForAdapterPosition(0).itemView.isSelected());
        assertTrue(view.handleBack());
        primary.requestFocus(); primary.performClick(); layout();
        assertTrue(grid(R.id.nhp_filter_year).findViewHolderForAdapterPosition(1).itemView.isSelected());
        key(KeyEvent.KEYCODE_DPAD_DOWN);
        key(KeyEvent.KEYCODE_DPAD_DOWN);
        assertTrue(grid(R.id.nhp_filter_order).hasFocus());
        key(KeyEvent.KEYCODE_BACK);
        assertFalse(view.isFilterOpen()); assertSame(primary, view.findFocus());
        assertEquals(View.VISIBLE, view.findViewById(R.id.nhp_top).getVisibility());
        assertSame(adapter, rail.getAdapter()); assertEquals(top, rail.getTop());
    }

    @Test public void categoryArrowsApplyLocalContentWithoutOpeningFiltersOrChangingGeometry() {
        create(6);
        key(KeyEvent.KEYCODE_DPAD_DOWN);
        Bundle before = view.debugMetrics(); int railTop = grid(R.id.nhp_posters).getTop();
        for (int i = 0; i < 5; i++) key(KeyEvent.KEYCODE_DPAD_RIGHT);
        Bundle state = new Bundle(); view.savePreviewState(state);
        assertEquals(5, state.getInt("preview.category"));
        assertEquals(5, grid(R.id.nhp_categories).getSelectedPosition());
        assertTrue(view.debugMetrics().getInt("heroes") > before.getInt("heroes"));
        assertFalse(view.isFilterOpen());
        assertEquals(before.getInt("geometry"), view.debugMetrics().getInt("geometry"));
        assertEquals(before.getInt("restorePosts"), view.debugMetrics().getInt("restorePosts"));
        assertEquals(railTop, grid(R.id.nhp_posters).getTop());
    }

    @Test public void restoreIsCoalescedAndCannotOverrideUserInputAndGridHasBoundedNativeQueue() {
        create(6);
        Bundle state = new Bundle(); state.putString("preview.region", "POSTERS"); state.putInt("preview.poster", 8);
        view.restorePreviewState(state); view.restorePreviewState(state);
        assertEquals(1, view.debugMetrics().getInt("pendingRestores"));
        key(KeyEvent.KEYCODE_DPAD_RIGHT);
        assertTrue(grid(R.id.nhp_functions).hasFocus());
        assertEquals(0, view.debugMetrics().getInt("pendingRestores"));
        assertEquals(2, grid(R.id.nhp_posters).getSmoothScrollMaxPendingMoves());
        assertEquals(1f, grid(R.id.nhp_posters).getSmoothScrollSpeedFactor(), 0);
        Bundle before = view.debugMetrics();
        key(KeyEvent.KEYCODE_DPAD_RIGHT);
        assertEquals(before.getInt("keys") + 1, view.debugMetrics().getInt("keys"));
        assertTrue(view.debugMetrics().getInt("focuses") - before.getInt("focuses") <= 1);
        assertEquals(before.getInt("geometry"), view.debugMetrics().getInt("geometry"));
        System.out.println("NH03_COUNTERS " + view.debugMetrics());
    }

    private static int count(View view, Class<?> type) {
        int count = type.isInstance(view) ? 1 : 0;
        if (view instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) count += count(group.getChildAt(i), type);
        return count;
    }
    private static List<Button> buttons(View root) {
        List<Button> result = new ArrayList<>();
        if (root instanceof Button button) result.add(button);
        if (root instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) result.addAll(buttons(group.getChildAt(i)));
        return result;
    }
}

