package com.fongmi.android.tv.ui.homepreview;

import android.app.Activity;
import android.app.Application;
import android.app.Dialog;
import android.os.Bundle;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.leanback.widget.BaseGridView;
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

import static org.junit.Assert.*;

/** Real Android Views under Robolectric; hardware focus/animation performance is a separate gate. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28, application = Application.class, qualifiers = "w960dp-h540dp-land-xhdpi")
public class NativeHomePreviewViewTest {
    private Activity activity;
    private NativeHomeBrowseView view;

    private void create(int cards, int functions) {
        activity = Robolectric.buildActivity(Activity.class).setup().get();
        activity.setTheme(R.style.Theme_App);
        view = new NativeHomeBrowseView(activity, cards, functions, () -> {});
        activity.setContentView(view);
        layout();
        view.findViewById(R.id.nhp_home).requestFocus();
    }

    private void layout() {
        view.measure(View.MeasureSpec.makeMeasureSpec(1920, View.MeasureSpec.EXACTLY), View.MeasureSpec.makeMeasureSpec(1080, View.MeasureSpec.EXACTLY));
        view.layout(0, 0, 1920, 1080);
        ShadowLooper.idleMainLooper(300, TimeUnit.MILLISECONDS);
    }

    private HorizontalGridView grid(int id) { return view.findViewById(id); }
    private void down() { view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN)); }
    private void up() { view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)); }

    @After public void release() {
        if (view != null) view.release();
        if (activity != null) activity.finish();
    }

    @Test public void posterGridAloneOwnsBoundedCenteredAlignment() {
        create(18, 12);
        HorizontalGridView rail = grid(R.id.nhp_posters);
        assertEquals(BaseGridView.WINDOW_ALIGN_BOTH_EDGE, rail.getWindowAlignment());
        assertEquals(50, rail.getWindowAlignmentOffsetPercent(), 0);
        assertFalse(rail.getClipChildren());
        assertFalse(rail.getClipToPadding());
        assertNull(rail.getItemAnimator());
        assertEquals(18, rail.getAdapter().getItemCount());
    }

    @Test public void regionRoundTripRestoresCategoryAndDoesNotLetHeldDownTraverseTwice() {
        create(18, 7);
        down();
        assertTrue(grid(R.id.nhp_categories).hasFocus());
        View category = view.findFocus();
        view.dispatchKeyEvent(new KeyEvent(0, 1, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN, 5));
        assertSame(category, view.findFocus());
        down();
        assertTrue(grid(R.id.nhp_posters).hasFocus());
        up();
        assertSame(category, view.findFocus());
        up();
        assertTrue(view.findViewById(R.id.nhp_home).hasFocus());
    }

    @Test public void shortAndEmptyRailsDoNotJumpToAnotherRegionAtEdges() {
        for (int count : new int[]{0, 1, 2}) {
            create(count, 0);
            down();
            down();
            if (count == 0) assertTrue(grid(R.id.nhp_categories).hasFocus());
            else {
                assertTrue(grid(R.id.nhp_posters).hasFocus());
                grid(R.id.nhp_posters).setSelectedPosition(count - 1);
                layout();
                view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
                assertTrue(grid(R.id.nhp_posters).hasFocus());
                assertEquals(count - 1, grid(R.id.nhp_posters).getSelectedPosition());
                grid(R.id.nhp_posters).setSelectedPosition(0);
                layout();
                view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                assertEquals(0, grid(R.id.nhp_posters).getSelectedPosition());
            }
            view.release();
            activity.finish();
        }
    }

    @Test public void varyingFunctionCountsArePreservedAndLeftEdgeReturnsToSource() {
        for (int count : new int[]{0, 1, 12, 24}) {
            create(2, count);
            HorizontalGridView functions = grid(R.id.nhp_functions);
            assertEquals(count, functions.getAdapter().getItemCount());
            view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT));
            if (count == 0) assertTrue(view.findViewById(R.id.nhp_home).hasFocus());
            else {
                assertTrue(functions.hasFocus());
                for (int i = 0; i < count; i++) {
                    functions.setSelectedPosition(i);
                    layout();
                    assertNotNull(functions.findViewHolderForAdapterPosition(i));
                }
                functions.setSelectedPosition(0);
                layout();
                view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_LEFT));
                assertTrue(view.findViewById(R.id.nhp_home).hasFocus());
            }
            view.release();
            activity.finish();
        }
    }

    @Test public void sourceDialogTrapsEdgesAndRestoresEntryOnCancelAndConfirm() {
        create(2, 2);
        View entry = view.findViewById(R.id.nhp_home);
        entry.performClick();
        Dialog dialog = ShadowDialog.getLatestDialog();
        assertTrue(dialog.isShowing());
        List<Button> options = buttons(dialog.getWindow().getDecorView());
        options.get(0).requestFocus();
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP));
        assertSame(options.get(0), dialog.getCurrentFocus());
        options.get(options.size() - 1).requestFocus();
        dialog.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_DOWN));
        assertSame(options.get(options.size() - 1), dialog.getCurrentFocus());
        dialog.cancel();
        assertTrue(entry.hasFocus());
        entry.performClick();
        dialog = ShadowDialog.getLatestDialog();
        buttons(dialog.getWindow().getDecorView()).get(2).performClick();
        assertFalse(dialog.isShowing());
        assertTrue(entry.hasFocus());
        assertTrue(entry.getContentDescription().toString().contains("C"));
    }

    @Test public void secondaryConfirmStaysOpenMovementDoesNotApplyAndSelectionIsNotFocus() {
        create(2, 2);
        View entry = grid(R.id.nhp_categories).findViewHolderForAdapterPosition(0).itemView;
        entry.requestFocus();
        entry.performClick();
        layout();
        List<Button> options = buttons(grid(R.id.nhp_filter_genre));
        assertTrue(options.get(0).isSelected());
        options.get(3).requestFocus();
        assertTrue(options.get(0).isSelected());
        assertFalse(options.get(3).isSelected());
        options.get(3).performClick();
        assertTrue(view.isFilterOpen());
        assertTrue(options.get(3).hasFocus());
        assertTrue(options.get(3).isSelected());
        options.get(1).performClick();
        assertTrue(options.get(1).isSelected());
        options.get(1).performClick();
        assertTrue(options.get(0).isSelected());
        assertTrue(view.handleBack());
        assertTrue(entry.hasFocus());
    }

    @Test public void heroSlotAndPosterSizeDoNotChangeWhenTextOrFocusChanges() {
        create(2, 2);
        View hero = view.findViewById(R.id.nhp_hero);
        int height = hero.getHeight();
        view.bindHero(new HomePreviewData.Card(88, "很长的影片标题".repeat(12), "2026", "简介".repeat(300), "9.0", "4K", R.drawable.nhp_poster_blue, R.drawable.nhp_backdrop_day, 0));
        layout();
        assertEquals(height, hero.getHeight());
        assertEquals(3, ((TextView) view.findViewById(R.id.nhp_overview)).getMaxLines());
        RecyclerView.ViewHolder holder = grid(R.id.nhp_posters).findViewHolderForAdapterPosition(0);
        int width = holder.itemView.getWidth();
        holder.itemView.requestFocus();
        layout();
        assertEquals(width, holder.itemView.getWidth());
        assertEquals(1.04f, holder.itemView.getScaleX(), .001f);
    }

    @Test public void horizontalDpadVisitsEveryOverflowFunctionAndReturnsWithoutPixelScrollingCode() {
        create(18, 24);
        key(KeyEvent.KEYCODE_DPAD_RIGHT);
        HorizontalGridView functions = grid(R.id.nhp_functions);
        for (int i = 0; i < 24; i++) {
            assertTrue(functions.hasFocus());
            assertEquals(i, functions.getSelectedPosition());
            key(KeyEvent.KEYCODE_DPAD_RIGHT);
        }
        assertEquals(23, functions.getSelectedPosition());
        for (int i = 23; i > 0; i--) key(KeyEvent.KEYCODE_DPAD_LEFT);
        assertEquals(0, functions.getSelectedPosition());
        key(KeyEvent.KEYCODE_DPAD_LEFT);
        assertTrue(view.findViewById(R.id.nhp_home).hasFocus());
    }

    @Test public void posterMiddleIsCenteredEndsAreBoundedAndRepeatAtEndDoesNotBounce() {
        create(18, 2);
        down();
        down();
        HorizontalGridView rail = grid(R.id.nhp_posters);
        for (int i = 0; i < 9; i++) key(KeyEvent.KEYCODE_DPAD_RIGHT);
        assertEquals(9, rail.getSelectedPosition());
        View middle = rail.findViewHolderForAdapterPosition(9).itemView;
        assertEquals(rail.getWidth() / 2f, middle.getLeft() + middle.getWidth() / 2f, 3);
        for (int i = 9; i < 17; i++) key(KeyEvent.KEYCODE_DPAD_RIGHT);
        assertEquals(17, rail.getSelectedPosition());
        View last = rail.findViewHolderForAdapterPosition(17).itemView;
        assertEquals(rail.getWidth() - rail.getPaddingRight(), last.getRight(), 3);
        for (int i = 1; i <= 40; i++) {
            view.dispatchKeyEvent(new KeyEvent(0, i, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, i));
        }
        layout();
        assertEquals(17, rail.getSelectedPosition());
        assertEquals(rail.getWidth() - rail.getPaddingRight(), last.getRight(), 3);
        for (int i = 17; i > 0; i--) key(KeyEvent.KEYCODE_DPAD_LEFT);
        View first = rail.findViewHolderForAdapterPosition(0).itemView;
        assertEquals(rail.getPaddingLeft(), first.getLeft(), 3);
        assertTrue(rail.hasFocus());
    }

    @Test public void savedPreviewStateRestoresPosterAndAppliedSecondaryWithoutBusinessPreferences() {
        create(18, 2);
        down();
        View category = view.findFocus();
        category.performClick();
        layout();
        buttons(grid(R.id.nhp_filter_genre)).get(2).performClick();
        view.handleBack();
        layout(); // Settle the inline state before returning to the poster region.
        assertTrue(category.hasFocus());
        down();
        layout();
        assertTrue(grid(R.id.nhp_posters).hasFocus());
        for (int i = 0; i < 5; i++) key(KeyEvent.KEYCODE_DPAD_RIGHT);
        Bundle state = new Bundle();
        view.savePreviewState(state);
        assertEquals(5, state.getInt("preview.poster"));
        assertEquals(2, state.getIntArray("preview.secondary")[0]);
        view.release();
        view = new NativeHomeBrowseView(activity, 18, 2, () -> {});
        activity.setContentView(view);
        view.restorePreviewState(state);
        layout();
        assertTrue(grid(R.id.nhp_posters).hasFocus());
        assertEquals(5, grid(R.id.nhp_posters).getSelectedPosition());
        Bundle restored = new Bundle();
        view.savePreviewState(restored);
        assertEquals(2, restored.getIntArray("preview.secondary")[0]);
    }

    private void key(int code) {
        boolean handled = view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, code));
        // ViewRootImpl performs this native fallback after a View returns an unhandled arrow.
        // Direct Robolectric View dispatch does not traverse the device's input pipeline.
        if (!handled && view.findFocus() != null) {
            int direction = code == KeyEvent.KEYCODE_DPAD_RIGHT ? View.FOCUS_RIGHT : View.FOCUS_LEFT;
            View next = view.findFocus().focusSearch(direction);
            if (next != null) next.requestFocus(direction);
        }
        view.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, code));
        layout();
    }

    private static List<Button> buttons(View root) {
        List<Button> result = new ArrayList<>();
        if (root instanceof Button button) result.add(button);
        if (root instanceof ViewGroup group) for (int i = 0; i < group.getChildCount(); i++) result.addAll(buttons(group.getChildAt(i)));
        return result;
    }
}
