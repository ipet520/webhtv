package com.fongmi.android.tv.ui.homepreview;

/** Calculated once per viewport change; dimensions are physical pixels, not copied CSS pixels. */
public record HomePreviewGeometry(int gutter, int top, int controlHeight, int posterWidth,
                                  int posterHeight, int captionHeight, int focusSpace,
                                  int railHeight, int railTop, int categoryTop, int categoryHeight,
                                  int heroTop, int heroHeight, int gap) {
    public static HomePreviewGeometry calculate(int width, int height, float density) {
        int gutter = Math.round(width * .025f);
        int top = Math.round(clamp(height * .03f, 8 * density, 24 * density));
        int controls = Math.round(clamp(height * .065f, 28 * density, 42 * density));
        int category = Math.round(clamp(height * .075f, 30 * density, 44 * density));
        int caption = Math.round(clamp(height * .052f, 20 * density, 30 * density));
        int poster = Math.round(clamp(height * .324f, 100 * density, 260 * density));
        // Keep the prototype usable in reduced display areas; leave room for the fixed Hero slot.
        poster = Math.min(poster, Math.round(height * .39f));
        int space = Math.max(Math.round(5 * density), (int) Math.ceil((poster + caption) * .025f) + 2);
        int railHeight = poster + caption + space * 2;
        int bottom = Math.max(Math.round(5 * density), Math.round(height * .012f));
        int railTop = height - bottom - railHeight;
        int categoryTop = railTop - category;
        int heroTop = Math.max(top + controls + Math.round(12 * density), Math.round(height * .16f));
        return new HomePreviewGeometry(gutter, top, controls, Math.round(poster * 2f / 3),
                poster, caption, space, railHeight, railTop, categoryTop, category,
                heroTop, Math.max(0, categoryTop - heroTop - Math.round(4 * density)),
                Math.round(clamp(width * .014f, 10 * density, 22 * density)));
    }

    static float clamp(float value, float min, float max) {
        return Math.max(min, Math.min(max, value));
    }
}
