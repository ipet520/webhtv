package com.fongmi.android.tv.ui.dialog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/** Keeps the ad-rule ranking user-facing by resolving stored rule IDs to display names. */
public class SiteHealthReportDialogSourceTest {

    @Test
    public void ruleRankingUsesResolvedDisplayNames() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/fongmi/android/tv/ui/dialog/SiteHealthReportDialog.java"));

        assertTrue(source.contains("ruleDimensionSummary(report.adBlockedByRule)"));
        assertTrue(source.contains("AdBlockStatsStore.getRuleDisplayName(entry.getKey())"));
        assertFalse(source.contains("dimensionSummary(report.adBlockedByRule)"));
    }

    @Test
    public void generatedRowsUseResolvedThemeRolesInsteadOfFixedBlack() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/fongmi/android/tv/ui/dialog/SiteHealthReportDialog.java"));

        assertFalse("generated report text must not use fixed translucent black",
                source.contains("R.color.black"));
        assertTrue("generated text must use the active theme's semantic color roles",
                source.contains("ThemeController.current().colorOnSurface"));
        assertTrue("status accents must be corrected against the actual report panel",
                source.contains("readableOnPanel(color)"));
        assertTrue(source.contains("ThemeController.current().colorSurfaceContainerHigh()"));
    }

    @Test
    public void windowSizingUsesInsetsWithoutChangingReportTheme() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/fongmi/android/tv/ui/dialog/SiteHealthReportDialog.java"));

        assertTrue(source.contains("DialogSafeArea.follow(requireActivity(), window"));
        assertTrue(source.contains("DialogSafeArea.area(requireActivity(), window)"));
        assertTrue(source.contains("ThemeDialogLayout.width(area.width(), margin)"));
        assertTrue(source.contains("ThemeDialogLayout.height(area.height(), margin)"));
        assertFalse(source.contains("ResUtil.getScreenHeight(requireContext()) - margin * 2"));
        assertTrue(source.contains("ThemeController.current().colorOnSurface"));
    }
}
