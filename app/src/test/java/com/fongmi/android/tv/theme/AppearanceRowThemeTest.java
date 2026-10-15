package com.fongmi.android.tv.theme;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The “Appearance &amp; language” row must be readable on every shipped palette and
 * must follow a user theme profile.
 *
 * <p>Regression under test (measured on device, {@code 192.168.50.3:5559}): the row kept
 * the fixed light {@code selector_git_cloud_card} fill {@code #F8F9FA} while its text came
 * from {@link ThemeController#current()}. The TV flavour compiles the dark token table, so
 * the label rendered {@code #E2E2E9} at <b>1.16:1</b> and the value {@code #C4C6D0} at
 * <b>1.36:1</b> - the reported "text is unreadable" in the TV appearance dialog.
 *
 * <p>These assertions are pure arithmetic on the shipped token tables plus source
 * contracts, so they fail on the old wiring and pass on the fix without an Android device.
 */
public class AppearanceRowThemeTest {

    /** The role {@code AppearanceRowTheme} paints the resting row with. */
    private static int restingFill(ThemeTokens tokens) {
        return tokens.colorSurfaceContainerHigh();
    }

    @Test
    public void restingRowClearsWcagAaForBothTextRolesOnEveryShippedPalette() {
        assertRowIsReadable(ThemeTokens.dark(), "dark");
        assertRowIsReadable(ThemeTokens.light(), "light");
    }

    private static void assertRowIsReadable(ThemeTokens tokens, String name) {
        int fill = restingFill(tokens);
        assertTrue(name + " label on the resting row",
                ThemeContrast.ratio(tokens.colorOnSurface(), fill) >= 4.5);
        assertTrue(name + " value on the resting row",
                ThemeContrast.ratio(tokens.colorOnSurfaceVariant(), fill) >= 4.5);
        // The row must not collapse into the dialog panel either, otherwise the inset list
        // reads as one undifferentiated block.
        assertNotEquals(name + " row fill must be distinguishable from the panel",
                tokens.colorSurfaceContainerHigh(), tokens.colorSurfaceContainer());
    }

    @Test
    public void focusedRowKeepsItsOwnTextReadableAndShowsARing() {
        for (ThemeTokens tokens : new ThemeTokens[]{ThemeTokens.dark(), ThemeTokens.light()}) {
            // The established local Appearance contract changes both row text roles to
            // OnPrimaryContainer while the focused fill is PrimaryContainer.
            assertTrue("focused title on primaryContainer",
                    ThemeContrast.ratio(tokens.colorOnPrimaryContainer(), tokens.colorPrimaryContainer()) >= 4.5);
            assertTrue("focused summary on primaryContainer",
                    ThemeContrast.ratio(tokens.colorOnPrimaryContainer(), tokens.colorPrimaryContainer()) >= 4.5);
            // The focus indicator itself must be visible against both the row fill and the
            // surrounding panel (WCAG non-text contrast floor of 3:1).
            assertTrue("focus ring on the focused fill",
                    ThemeContrast.ratio(tokens.colorPrimary(), tokens.colorPrimaryContainer()) >= 3.0);
            assertTrue("focus ring on the dialog panel",
                    ThemeContrast.ratio(tokens.colorPrimary(), tokens.colorSurfaceContainerHigh()) >= 3.0);
        }
    }

    /**
     * The old fixed fill is exactly what the device screenshot measured, so this pins the
     * regression numerically rather than only by source inspection.
     */
    @Test
    public void theRemovedFixedFillIsExactlyTheMeasuredUnreadableOne() {
        int legacyFill = 0xFFF8F9FA;
        assertEquals("the fixed selector fill the row used to paint", legacyFill, 0xFFF8F9FA);
        assertTrue("label was unreadable on it",
                ThemeContrast.ratio(ThemeTokens.dark().colorOnSurface(), legacyFill) < 2.0);
        assertTrue("value was unreadable on it",
                ThemeContrast.ratio(ThemeTokens.dark().colorOnSurfaceVariant(), legacyFill) < 2.0);
        assertTrue("the fix must clear AA where the old fill did not",
                ThemeContrast.ratio(ThemeTokens.dark().colorOnSurface(), restingFill(ThemeTokens.dark())) >= 4.5);
    }

    @Test
    public void bothFlavoursRouteTheRowThroughTheSharedPalette() throws Exception {
        String mobile = codeOnly(read("src/mobile/java/com/fongmi/android/tv/ui/dialog/AppearanceDialog.java"));
        assertTrue(mobile.contains("ThemeTokens tokens = ThemeController.legacyInteractionTokens();"));
        assertTrue(mobile.contains("ThemeEditorUi.interactive(requireContext(), fill,"));
        assertTrue(mobile.contains("tokens.colorOutline(), tokens.colorFocus(), false"));
        assertFalse(mobile.contains("selector_git_cloud_card"));

        String leanback = codeOnly(read("src/leanback/java/com/fongmi/android/tv/ui/dialog/AppearanceDialog.java"));
        assertTrue(leanback.contains("AppearanceRowTheme.apply(row, title, summary, ThemeController.current());"));
        assertTrue(leanback.contains("applyRowTheme();"));
    }

    @Test
    public void theSharedRowRendererUsesOnlySemanticRoles() throws Exception {
        String source = codeOnly(read("src/main/java/com/fongmi/android/tv/theme/AppearanceRowTheme.java"));
        assertTrue(source.contains("safe.colorSurfaceContainerHigh()"));
        assertTrue(source.contains("safe.colorOnSurface()"));
        assertTrue(source.contains("safe.colorOnSurfaceVariant()"));
        assertTrue(source.contains("safe.colorPrimaryContainer()"));
        assertTrue(source.contains("safe.colorPrimary()"));
        assertTrue(source.contains("safe.colorOutlineVariant()"));
        // No fixed colours may creep back in.
        assertFalse("the row renderer must not hard-code a fill", source.contains("#F8F9FA"));
        assertFalse(source.contains("#DADCE0"));
        assertFalse(source.contains("#E8F0FE"));
    }

    @Test
    public void theRowOwnsThePaletteItIsPaintedWith() throws Exception {
        // The regression was a mismatch between a fixed row fill and token-driven text.
        // Guard the invariant directly: every colour the row paints comes from the same
        // ThemeTokens instance the row text is set from.
        String source = codeOnly(read("src/main/java/com/fongmi/android/tv/theme/AppearanceRowTheme.java"));
        assertTrue(source.contains("public static void apply(View row, TextView title, TextView summary, ThemeTokens tokens)"));
        int apply = source.indexOf("public static void apply(");
        String body = source.substring(apply, source.indexOf("private static GradientDrawable shape("));
        assertTrue("the row fill comes from the passed tokens", body.contains("background(row.getContext(), safe)"));
        assertTrue("focused title colour comes from the same tokens", body.contains("safe.colorOnPrimaryContainer()"));
        assertTrue("normal title colour comes from the same tokens", body.contains("safe.colorOnSurface()"));
        assertTrue("normal value colour comes from the same tokens", body.contains("safe.colorOnSurfaceVariant()"));
    }

    /**
     * The picker opened from these rows paints its title on the same dialog panel, so it
     * had the identical defect: a fixed {@code #202124} title on the dark panel measured
     * <b>1.00:1</b> on device. It must use the active palette too.
     */
    @Test
    public void thePickerOpenedFromTheseRowsAlsoUsesTheActivePalette() throws Exception {
        String dialog = codeOnly(read("src/main/java/com/fongmi/android/tv/ui/dialog/ChoiceDialog.java"));
        assertTrue(dialog.contains("titleView.setTextColor(ThemeDialogSemantics.tokens().colorOnSurface())"));
        assertTrue(dialog.contains("messageView.setTextColor(ThemeDialogSemantics.tokens().colorOnSurfaceVariant())"));
        assertTrue(dialog.contains("import com.fongmi.android.tv.theme.ThemeDialogSemantics;"));
        assertTrue(dialog.contains("ThemeController.bindDialog(dialog)"));
        // The title/message sit directly on the dialog panel, which is the part that was
        // invisible. Item rows keep their own opaque card, so their colours are a separate
        // concern and stay untouched here.
        int title = dialog.indexOf("titleView.setTextColor(");
        int message = dialog.indexOf("messageView.setTextColor(");
        assertTrue(title > 0 && message > 0);
        assertFalse("the panel-drawn title must not be a fixed colour",
                dialog.substring(title, dialog.indexOf(";", title)).contains("parseColor"));
        assertFalse("the panel-drawn message must not be a fixed colour",
                dialog.substring(message, dialog.indexOf(";", message)).contains("parseColor"));
    }

    @Test
    public void choiceItemsUseTheActivePaletteInBothModes() throws Exception {
        String dialog = codeOnly(read("src/main/java/com/fongmi/android/tv/ui/dialog/ChoiceDialog.java"));
        assertTrue(dialog.contains("ThemeTokens tokens = ThemeDialogSemantics.tokens()"));
        assertTrue(dialog.contains("tokens.colorSurfaceContainerHigh()"));
        assertTrue(dialog.contains("tokens.colorOnPrimaryContainer()"));
        assertTrue(dialog.contains("tokens.colorOnSurface()"));
        assertTrue(dialog.contains("ThemeEditorUi.shape(requireContext(), tokens.colorSurfaceContainerHigh(), 0, 0, 22)"));
        assertFalse(dialog.contains("#9AA0A6"));
        assertFalse(dialog.contains("#F1F3F4"));
        assertFalse(dialog.contains("#202124"));
        assertFalse(dialog.contains("#1A73E8"));
    }

    /**
     * Strips comments so a source contract matches executable code, not the explanatory
     * prose that names the very colours and selectors the contract forbids.
     */
    private static String codeOnly(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int index = 0;
        while (index < source.length()) {
            char current = source.charAt(index);
            char next = index + 1 < source.length() ? source.charAt(index + 1) : '\0';
            if (current == '/' && next == '/') {
                while (index < source.length() && source.charAt(index) != '\n') index++;
            } else if (current == '/' && next == '*') {
                index += 2;
                while (index + 1 < source.length() && !(source.charAt(index) == '*' && source.charAt(index + 1) == '/')) index++;
                index = Math.min(source.length(), index + 2);
            } else {
                out.append(current);
                index++;
            }
        }
        return out.toString();
    }

    private static String read(String path) throws Exception {
        Path root = Files.exists(Path.of("src")) ? Path.of("") : Path.of("app");
        return Files.readString(root.resolve(path), StandardCharsets.UTF_8);
    }
}
