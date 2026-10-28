package com.fongmi.android.tv.ui.homepreview;

import android.view.KeyEvent;
import android.view.View;
import android.view.ViewGroup;

import androidx.leanback.widget.HorizontalGridView;
import java.util.function.BooleanSupplier;

/** Owns region transitions only. Leanback alone owns horizontal selection and pixel scrolling. */
public final class HomePreviewFocusCoordinator {
    public enum Region { TOP, FILTERS, CATEGORIES, POSTERS }
    private final ViewGroup top;
    private final ViewGroup categoryRow;
    private final View home;
    private HorizontalGridView[] filters;
    public void setFilters(HorizontalGridView[] filters) { this.filters=filters; }
    private final BooleanSupplier filterOpen;
    private final HorizontalGridView functions;
    private final HorizontalGridView categories;
    private final HorizontalGridView posters;
    private boolean returnToFunctions;
    private View categoryAnchor;
    private java.util.function.IntSupplier activeCategory;
    public void setActiveCategory(java.util.function.IntSupplier supplier) { activeCategory=supplier; }

    public HomePreviewFocusCoordinator(ViewGroup top, ViewGroup categoryRow, View home,
                                       HorizontalGridView functions, HorizontalGridView categories,
                                       HorizontalGridView posters, HorizontalGridView[] filters, BooleanSupplier filterOpen) {
        this.top = top;
        this.categoryRow = categoryRow;
        this.home = home;
        this.filters = filters;
        this.filterOpen = filterOpen;
        this.functions = functions;
        this.categories = categories;
        this.posters = posters;
    }

    public Region regionOf(View focused) {
        if (inside(posters, focused)) return Region.POSTERS;
        if (inside(categoryRow, focused)) return Region.CATEGORIES;
        if (filterRow(focused) >= 0) return Region.FILTERS;
        return Region.TOP;
    }

    public void remember(View focused) {
        if (focused == null) return;
        if (inside(top, focused)) returnToFunctions = inside(functions, focused);
        else if (inside(categoryRow, focused)) categoryAnchor = focused;
    }

    public boolean handle(KeyEvent event, View focused) {
        if (event.getAction() != KeyEvent.ACTION_DOWN || focused == null) return false;
        Region region = regionOf(focused);
        int key = event.getKeyCode();
        if (key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
            // Holding DOWN must not repeatedly traverse multiple regions.
            if (event.getRepeatCount() > 0) return true;
            remember(focused);
            if (key == KeyEvent.KEYCODE_DPAD_UP) {
                if (region == Region.POSTERS) {
                    if (filterOpen.getAsBoolean() && filters.length>0) filters[filters.length-1].requestFocus();
                    else focusCategories();
                }
                else if (region == Region.CATEGORIES) {
                    if (filterOpen.getAsBoolean() && filters.length>0) filters[filters.length - 1].requestFocus();
                    else focusTop();
                } else if (region == Region.FILTERS && filterRow(focused) > 0) filters[filterRow(focused) - 1].requestFocus();
            } else {
                if (region == Region.TOP) focusCategories();
                else if (region == Region.CATEGORIES && posters.getAdapter().getItemCount() > 0) posters.requestFocus();
                else if (region == Region.FILTERS) {
                    int row = filterRow(focused);
                    if (row < filters.length - 1) filters[row + 1].requestFocus();
                    else if (posters.getAdapter()!=null && posters.getAdapter().getItemCount()>0) posters.requestFocus();
                }
            }
            return true;
        }
        if (key != KeyEvent.KEYCODE_DPAD_LEFT && key != KeyEvent.KEYCODE_DPAD_RIGHT) return false;
        boolean left = key == KeyEvent.KEYCODE_DPAD_LEFT;
        if (focused == home) {
            if (!left && functions.getAdapter().getItemCount() > 0) functions.requestFocus();
            return true;
        }
        if (inside(functions, focused) && atEdge(functions, left)) {
            if (left) home.requestFocus();
            return true;
        }
        if (inside(categories, focused) && atEdge(categories, left)) {
            return true;
        }
        int filterRow = filterRow(focused);
        if (filterRow >= 0) return atEdge(filters[filterRow], left);
        return inside(posters, focused) && atEdge(posters, left);
    }

    public void focusTop() {
        if (returnToFunctions && functions.getAdapter().getItemCount() > 0) functions.requestFocus();
        else home.requestFocus();
    }

    public void resetCategoryAnchor() { categoryAnchor = null; }
    public void resetTopAnchor() { if (functions.getAdapter().getItemCount() == 0) returnToFunctions = false; }

    public void focusCategories() {
        if(activeCategory!=null) {
            int position=activeCategory.getAsInt();
            if(categories.getSelectedPosition()!=position)categories.setSelectedPosition(position);
            var holder=categories.findViewHolderForAdapterPosition(position);
            if(holder!=null&&holder.itemView.requestFocus())return;
            categories.requestFocus();return;
        }
        if (!restore(categoryAnchor, categoryRow)) categories.requestFocus();
    }

    private static boolean restore(View anchor, ViewGroup group) {
        return anchor != null && inside(group, anchor) && anchor.isShown() && anchor.requestFocus();
    }

    private int filterRow(View focused) {
        for (int i = 0; i < filters.length; i++) if (inside(filters[i], focused)) return i;
        return -1;
    }

    private static boolean atEdge(HorizontalGridView grid, boolean left) {
        int count = grid.getAdapter() == null ? 0 : grid.getAdapter().getItemCount();
        return count == 0 || grid.getSelectedPosition() == (left ? 0 : count - 1);
    }

    private static boolean inside(ViewGroup parent, View view) {
        for (View current = view; current != null; ) {
            if (current == parent) return true;
            current = current.getParent() instanceof View next ? next : null;
        }
        return false;
    }
}
