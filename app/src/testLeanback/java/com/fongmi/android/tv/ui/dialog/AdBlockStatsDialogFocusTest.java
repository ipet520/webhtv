package com.fongmi.android.tv.ui.dialog;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Architecture contract: expanding a statistics group must not invalidate the focused row. */
public class AdBlockStatsDialogFocusTest {

    @Test
    public void groupedAndRuleExpansionNotifyOnlyTheChangedRows() throws Exception {
        String source = read("src/leanback/java/com/fongmi/android/tv/ui/dialog/AdBlockStatsDialog.java");
        String grouped = section(source, "private static class GroupedLogAdapter", "private static class RuleGroupedLogAdapter");
        String rules = section(source, "private static class RuleGroupedLogAdapter", "private static class ExpandableLogAdapter");
        String rowCounter = method(source, "private static int countExpandedLogRowsAfter(", "private enum GroupType");

        assertExpansionUsesPreciseNotifications(grouped, "group");
        assertExpansionUsesPreciseNotifications(rules, "rule");
        assertTrue(rowCounter.contains("items.get(index) instanceof AdBlockLog"));
        assertFalse("the active grouped paths must not invalidate all holders", grouped.contains("notifyDataSetChanged()"));
        assertFalse("the active rule path must not invalidate all holders", rules.contains("notifyDataSetChanged()"));
        assertFalse("focus must not be redirected through a posted position lookup", source.contains("restoreItemFocus("));
    }

    private static void assertExpansionUsesPreciseNotifications(String source, String kind) {
        assertTrue(kind + " expansion keeps its independent expanded-state set", source.contains("expanded.add("));
        assertTrue(kind + " expansion rebuilds the same visible data model", source.contains("rebuild();"));
        assertTrue(kind + " expansion rebinds the existing group row", source.contains("notifyItemChanged(adapterPosition)"));
        assertTrue(kind + " expansion inserts only its visible children", source.contains("notifyItemRangeInserted(adapterPosition + 1, newChildCount)"));
        assertTrue(kind + " collapse removes only its visible children", source.contains("notifyItemRangeRemoved(adapterPosition + 1, oldChildCount)"));
    }

    private static String section(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        if (from < 0 || to <= from) throw new AssertionError("Missing source section: " + start);
        return source.substring(from, to);
    }

    private static String method(String source, String start, String end) {
        return section(source, start, end);
    }

    private static String read(String relativePath) throws Exception {
        Path root = Files.exists(Path.of("src", "leanback")) ? Path.of("") : Path.of("app");
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
