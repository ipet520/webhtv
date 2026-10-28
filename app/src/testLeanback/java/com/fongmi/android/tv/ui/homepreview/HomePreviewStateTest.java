package com.fongmi.android.tv.ui.homepreview;

import org.junit.Test;

import static org.junit.Assert.*;

public class HomePreviewStateTest {
    @Test public void secondaryCanBeSelectedRepeatedlyWithoutClosingOrLosingPrimaryContext() {
        HomePreviewSelection state = new HomePreviewSelection(8);
        state.selectCategory(2);
        state.selectSecondary(3);
        assertEquals(2, state.category());
        assertEquals(3, state.secondary());
        state.selectSecondary(2);
        assertEquals(2, state.secondary());
        state.selectSecondary(2);
        assertEquals(0, state.secondary());
        state.selectSecondary(0);
        assertEquals(0, state.secondary());
    }

    @Test public void secondaryIsScopedToPrimaryAndSwitchingFixtureSourceClearsOldState() {
        HomePreviewSelection state = new HomePreviewSelection(8);
        state.selectCategory(1);
        state.selectSecondary(3);
        state.selectCategory(4);
        assertEquals(0, state.secondary());
        state.selectCategory(1);
        assertEquals(3, state.secondary());
        state.selectSource(2);
        assertEquals(2, state.source());
        assertEquals(0, state.category());
        state.selectCategory(1);
        assertEquals(0, state.secondary());
    }

    @Test public void restoringClampsInvalidFixtureState() {
        HomePreviewSelection state = new HomePreviewSelection(8);
        state.restore(999, -5, new int[]{2, 999, -1});
        assertEquals(0, state.source());
        assertEquals(0, state.category());
        assertEquals(2, state.secondary());
        state.selectCategory(1);
        assertEquals(0, state.secondary());
        int[] snapshot = state.secondarySnapshot();
        snapshot[1] = 4;
        assertEquals(0, state.secondary());
    }

    @Test public void viewportGeometryKeepsScaleInsideRailAndStableHeroSlot() {
        for (int[] size : new int[][]{{960, 540}, {1920, 1080}, {3840, 2160}, {1280, 582}, {800, 450}}) {
            float density = size[0] / 960f;
            HomePreviewGeometry g = HomePreviewGeometry.calculate(size[0], size[1], density);
            assertTrue(g.heroHeight() > 0);
            assertTrue(g.heroTop() + g.heroHeight() <= g.categoryTop());
            assertEquals(g.categoryTop() + g.categoryHeight(), g.railTop());
            assertTrue(g.railTop() + g.railHeight() <= size[1]);
            assertTrue(g.focusSpace() >= (g.posterHeight() + g.captionHeight()) * .02f);
            assertEquals(2f / 3, (float) g.posterWidth() / g.posterHeight(), .006f);
            assertTrue(g.gutter() > g.posterWidth() * .02f);
        }
    }
}
