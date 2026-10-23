package com.fongmi.android.tv.ui.dialog;

import android.graphics.Rect;
import android.view.View;
import androidx.core.widget.NestedScrollView;

/** Reveal the laid-out item, including focus decoration, in the outer viewport. */
final class EpisodeScrollVisibility {
    static void reveal(NestedScrollView scroll, View item, int margin) {
        if (scroll.getHeight() <= 0 || item.getHeight() <= 0) return;
        Rect bounds = new Rect();
        item.getDrawingRect(bounds);
        scroll.offsetDescendantRectToMyCoords(item, bounds);
        int top = scroll.getScrollY() + scroll.getPaddingTop();
        int bottom = scroll.getScrollY() + scroll.getHeight() - scroll.getPaddingBottom();
        int delta = delta(bounds.top - margin, bounds.bottom + margin, top, bottom);
        if (delta != 0) scroll.scrollBy(0, delta);
    }

    static int delta(int itemTop, int itemBottom, int viewportTop, int viewportBottom) {
        if (itemTop < viewportTop) return itemTop - viewportTop;
        if (itemBottom > viewportBottom) return itemBottom - viewportBottom;
        return 0;
    }
}
