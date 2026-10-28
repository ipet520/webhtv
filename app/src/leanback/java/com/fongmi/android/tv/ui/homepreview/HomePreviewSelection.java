package com.fongmi.android.tv.ui.homepreview;

import java.util.Arrays;

/** The only selection state in the fixture preview; never reads or writes production preferences. */
public final class HomePreviewSelection {
    private int source;
    private int category;
    private final int[] secondary;
    private final int[][] otherFilters;

    public HomePreviewSelection(int categoryCount) {
        secondary = new int[categoryCount];
        otherFilters = new int[][]{new int[categoryCount], new int[categoryCount]};
    }
    public int source() { return source; }
    public int category() { return category; }
    public int secondary() { return secondary[category]; }
    public int[] secondarySnapshot() { return secondary.clone(); }
    public int filter(int row) { return row == 0 ? secondary() : otherFilters[row - 1][category]; }
    public int[] filterSnapshot(int row) { return row == 0 ? secondarySnapshot() : otherFilters[row - 1].clone(); }
    public void selectFilter(int row, int index) {
        if (row < 0 || row >= HomePreviewData.FILTERS.length || index < 0 || index >= HomePreviewData.FILTERS[row].length) return;
        int[] values = row == 0 ? secondary : otherFilters[row - 1];
        values[category] = values[category] == index ? 0 : index;
    }

    public void selectCategory(int index) {
        if (index >= 0 && index < secondary.length) category = index;
    }

    public void selectSecondary(int index) {
        selectFilter(0, index);
    }

    public void selectSource(int index) {
        if (index < 0 || index >= HomePreviewData.SOURCES.length || source == index) return;
        source = index;
        category = 0;
        Arrays.fill(secondary, 0);
        for (int[] values : otherFilters) Arrays.fill(values, 0);
    }

    public void restore(int savedSource, int savedCategory, int[] savedSecondary) {
        if (savedSource >= 0 && savedSource < HomePreviewData.SOURCES.length) source = savedSource;
        selectCategory(savedCategory);
        if (savedSecondary == null) return;
        for (int i = 0; i < Math.min(secondary.length, savedSecondary.length); i++) {
            int value = savedSecondary[i];
            secondary[i] = value >= 0 && value < HomePreviewData.SECONDARY.length ? value : 0;
        }
    }

    public void restoreFilter(int row, int[] saved) {
        if (row <= 0 || row >= HomePreviewData.FILTERS.length || saved == null) return;
        for (int i = 0; i < Math.min(saved.length, secondary.length); i++) {
            int value = saved[i];
            otherFilters[row - 1][i] = value >= 0 && value < HomePreviewData.FILTERS[row].length ? value : 0;
        }
    }
}
