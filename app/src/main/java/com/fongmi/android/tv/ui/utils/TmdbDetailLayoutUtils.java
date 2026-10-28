package com.fongmi.android.tv.ui.utils;

import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;

import com.google.android.flexbox.FlexboxLayout;
import com.google.android.flexbox.FlexWrap;
import com.google.android.flexbox.JustifyContent;
import com.google.android.flexbox.AlignItems;
import com.google.android.flexbox.AlignContent;

import com.fongmi.android.tv.utils.ResUtil;

/**
 * TmdbDetailActivity 布局工具方法集
 *
 * 从 TmdbDetailActivity 提取的无状态布局辅助方法,用于:
 * - 设置视图边距/内边距/尺寸
 * - 创建简单 Drawable
 *
 * 第一批重构: 提取 6 个纯静态方法,减少主类 ~30 行
 */
public class TmdbDetailLayoutUtils {

    /** TV-only caller: keep the binding's outer container and the original button instances. */
    public static void compactDetailActions(LinearLayout outer, Button play) {
        FlexboxLayout row;
        if (outer.getChildCount() == 1 && outer.getChildAt(0) instanceof FlexboxLayout existing) row = existing;
        else {
            java.util.List<Button> buttons = new java.util.ArrayList<>();
            collectButtons(outer, buttons);
            outer.removeAllViews();
            row = new FlexboxLayout(outer.getContext());
            outer.addView(row, new LinearLayout.LayoutParams(-1, -2));
            for (Button button : buttons) {
                if (button.getParent() instanceof ViewGroup parent) parent.removeView(button);
                row.addView(button, new FlexboxLayout.LayoutParams(-2, -2));
            }
        }
        outer.setClipChildren(false);
        outer.setClipToPadding(false);
        compactActionRow(row, play, false);
    }

    private static void collectButtons(ViewGroup group, java.util.List<Button> buttons) {
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof Button button) buttons.add(button);
            else if (child instanceof ViewGroup nested) collectButtons(nested, buttons);
        }
    }

    /** Existing native Flexbox owns wrapping; no weights, scrolling or new focus dispatcher. */
    public static void compactActionRow(FlexboxLayout row, Button play, boolean centered) {
        float density = row.getResources().getDisplayMetrics().density;
        int gap = Math.round(4 * density), padding = Math.round(14 * density);
        int height = Math.round(48 * density);
        for (int i = 0; i < row.getChildCount(); i++) if (row.getChildAt(i) instanceof Button button)
            height = Math.max(height, (int) Math.ceil(button.getPaint().getFontSpacing()) + Math.round(16 * density));
        row.setFlexWrap(FlexWrap.WRAP);
        row.setJustifyContent(centered ? JustifyContent.CENTER : JustifyContent.FLEX_START);
        row.setAlignItems(AlignItems.CENTER);
        row.setAlignContent(AlignContent.FLEX_START);
        row.setClipChildren(false);
        row.setClipToPadding(false);
        row.setPadding(0, gap, 0, gap);
        for (int i = 0; i < row.getChildCount(); i++) {
            if (!(row.getChildAt(i) instanceof Button button)) continue;
            FlexboxLayout.LayoutParams params = new FlexboxLayout.LayoutParams(-2, height);
            params.setFlexBasisPercent(-1);
            params.setFlexGrow(0);
            params.setFlexShrink(0);
            params.setMargins(gap, gap, gap, gap);
            params.setMinWidth(Math.round((button == play ? 112 : 72) * density));
            button.setMinWidth(0);
            button.setMinimumWidth(0);
            button.setSingleLine(true);
            button.setEllipsize(null);
            button.setPaddingRelative(padding, button.getPaddingTop(), padding, button.getPaddingBottom());
            button.setLayoutParams(params);
        }
    }

    public record TvPlayerSize(int width, int height, int left, int right) {}

    /** Physical View bounds are converted using density, never a 1080P/4K breakpoint. */
    public static TvPlayerSize tvPlayerSize(int rootWidth, int safeLeft, int safeRight, float density) {
        int available = Math.max(1, rootWidth - safeLeft - safeRight);
        int gutter = Math.min(Math.round(16 * density), available / 4);
        int width = Math.max(1, Math.round(Math.min(760 * density, (available - gutter * 2) * .64f)));
        int left = safeLeft + (available - width) / 2;
        return new TvPlayerSize(width, Math.max(1, Math.round(width * 9f / 16)), left, Math.max(0, rootWidth - left - width));
    }

    /**
     * 设置视图边距 (DP 单位)
     * 原: TmdbDetailActivity.setMarginsDp() (1600-1602 行)
     * 调用次数: ~89 处
     */
    public static void setMarginsDp(View view, int left, int top, int right, int bottom) {
        setMarginsPx(view, ResUtil.dp2px(left), ResUtil.dp2px(top), ResUtil.dp2px(right), ResUtil.dp2px(bottom));
    }

    /**
     * 设置视图边距 (像素单位)
     * 原: TmdbDetailActivity.setMarginsPx() (1604-1609 行)
     * 调用次数: ~15 处 (含被 setMarginsDp 调用)
     */
    public static void setMarginsPx(View view, int left, int top, int right, int bottom) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (!(params instanceof ViewGroup.MarginLayoutParams marginParams)) return;
        marginParams.setMargins(left, top, right, bottom);
        view.setLayoutParams(marginParams);
    }

    /**
     * 设置视图高度 (DP 单位)
     * 原: TmdbDetailActivity.setHeightDp() (1611-1615 行)
     * 调用次数: ~52 处
     */
    public static void setHeightDp(View view, int height) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.height = ResUtil.dp2px(height);
        view.setLayoutParams(params);
    }

    /**
     * 设置视图宽度 (像素单位)
     * 原: TmdbDetailActivity.setWidthPx() (1617-1621 行)
     * 调用次数: ~23 处
     */
    public static void setWidthPx(View view, int width) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = width;
        view.setLayoutParams(params);
    }

    /**
     * 设置视图宽度为 MATCH_PARENT
     * 原: TmdbDetailActivity.setWidthMatch() (1623-1627 行)
     * 调用次数: ~18 处
     */
    public static void setWidthMatch(View view) {
        ViewGroup.LayoutParams params = view.getLayoutParams();
        params.width = ViewGroup.LayoutParams.MATCH_PARENT;
        view.setLayoutParams(params);
    }

    /**
     * 设置视图内边距 (DP 单位)
     * 原: TmdbDetailActivity.setPaddingDp() (1589-1591 行)
     * 调用次数: ~37 处
     */
    public static void setPaddingDp(View view, int left, int top, int right, int bottom) {
        view.setPadding(ResUtil.dp2px(left), ResUtil.dp2px(top), ResUtil.dp2px(right), ResUtil.dp2px(bottom));
    }

    /**
     * 创建纯色 Drawable
     * 原: TmdbDetailActivity.colorDrawable() (1629-1631 行)
     * 调用次数: ~12 处
     */
    public static Drawable colorDrawable(int color) {
        return new ColorDrawable(color);
    }
}
