package com.fongmi.android.tv.ui.dialog;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/** Keeps full and near-full dialog sizing inside the system-bar-safe area. */
public class DialogSafeAreaTest {

    private static final String DIALOGS = "app/src/main/java/com/fongmi/android/tv/ui/dialog/";

    @Test
    public void helperUsesWindowHostAndContentInsetsWithAChangeListener() throws Exception {
        String source = read(DIALOGS + "DialogSafeArea.java");

        assertTrue(source.contains("WindowInsetsCompat.Type.systemBars() | WindowInsetsCompat.Type.displayCutout()"));
        assertTrue(source.contains("WindowInsetsCompat.Type.systemGestures()"));
        assertTrue(source.contains("Math.max(bars.bottom, gestures.bottom)"));
        assertTrue(source.contains("ThemeDialogLayout.safeArea("));
        assertTrue(source.contains("insets.isEmpty()"));
        assertTrue(source.contains("ViewCompat.getRootWindowInsets("));
        assertTrue(source.contains("ThemeDialogLayout.Insets.ofContent("));
        assertTrue(source.contains("ViewCompat.setOnApplyWindowInsetsListener("));
        assertTrue(source.contains("ViewCompat.requestApplyInsets("));
        assertFalse(source.contains("screenHeightDp"));
    }

    @Test
    public void existingScreenSizedDialogsUseTheSharedAreaResolver() throws Exception {
        String[] dialogs = {
                "AiRecommendationInfoDialog", "CodecCapabilityDialog", "GitCloudDialog", "GroupRuleDialog",
                "LoginStateLearnDialog", "LoginStatePathDialog", "ManagePageDialog", "MpvConfigDialog",
                "MpvConfigEditorDialog", "MpvCustomButtonDialog", "MultiThreadProxyDialog",
                "PanNetworkDiagnosticDialog", "PlaybackPerformanceDialog", "PlaybackPerformanceModal",
                "PlaybackRemoteSyncDialog", "PlaybackWebhookDialog", "PlayerButtonConfigDialog",
                "RemoteTrustDialog", "ShellProxyDialog", "SiteHealthDialog", "SiteHealthReportDialog",
                "UpdateSettingsDialog", "ViewingRecordSyncDialog", "WebHomeExtensionDebugDialog",
                "WebHomeExtensionDialog"
        };
        for (String dialog : dialogs) {
            String source = read(DIALOGS + dialog + ".java");
            assertTrue(dialog + " must size content from DialogSafeArea", source.contains("DialogSafeArea."));
        }
        for (String flavor : new String[]{"mobile", "leanback"}) {
            String source = read("app/src/" + flavor
                    + "/java/com/fongmi/android/tv/ui/dialog/AdBlockStatsDialog.java");
            assertTrue(flavor + " AdBlockStatsDialog must use the safe width and height",
                    source.contains("DialogSafeArea.width(activity, window)")
                            && source.contains("DialogSafeArea.height(activity, window)"));
        }
    }

    @Test
    public void fullScreenHealthReportReappliesSizeWhenInsetsArrive() throws Exception {
        String source = read(DIALOGS + "SiteHealthReportDialog.java");
        assertTrue(source.contains("DialogSafeArea.follow(requireActivity(), window"));
        assertTrue(source.contains("DialogSafeArea.area(requireActivity(), window)"));
        assertTrue(source.contains("ThemeDialogLayout.width(area.width(), margin)"));
        assertTrue(source.contains("ThemeDialogLayout.height(area.height(), margin)"));
        assertTrue(source.contains("window.setLayout(params.width, params.height)"));
        assertFalse(source.contains("ResUtil.getScreenHeight(requireContext()) - margin * 2"));
        assertFalse(source.contains("ResUtil.getScreenWidth(requireContext()) - margin * 2"));
    }

    private static String read(String relative) throws Exception {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.isRegularFile(root.resolve(relative))) root = root.getParent();
        if (root == null) throw new IllegalStateException("Cannot locate App repository root for " + relative);
        return Files.readString(root.resolve(relative));
    }
}
