package com.fongmi.android.tv.ui.activity;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Architecture contract: an async Follow request must leave its DPAD-focused view eligible. */
public class VideoActivityFollowingFocusTest {

    @Test
    public void pendingFollowActionDoesNotDisableOrReplaceTheFocusedButton() throws Exception {
        String source = read("src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java");
        String onFollowing = section(source, "private void onFollowing()", "private void cancelFollowing(");
        String applyState = section(source, "private void applyFollowingButtonState(", "private void updateVod(");

        assertTrue("the existing pending guard prevents duplicate requests",
                onFollowing.contains("followingActionPending || !FollowingPlaybackBridge.isEligible(mHistory)"));
        assertTrue("the async action remains guarded while it runs", onFollowing.contains("followingActionPending = true;"));
        assertTrue("the existing asynchronous operation is preserved", onFollowing.contains("FollowingPlaybackBridge.findAsync(identityKey"));
        assertFalse("disabling the focused Leanback view caused the focus loss", onFollowing.contains("mBinding.following.setEnabled(false)"));
        assertFalse("the action must not replace the bound button view", onFollowing.contains("mBinding.following ="));

        assertTrue(applyState.contains("mBinding.following.setVisibility(eligible ? View.VISIBLE : View.GONE)"));
        assertTrue(applyState.contains("mBinding.following.setEnabled(true)"));
        assertTrue(applyState.contains("mBinding.following.setSelected(followed)"));
        assertTrue(applyState.contains("mBinding.following.setText(followed ? R.string.following_added : R.string.following_add)"));
    }

    private static String section(String source, String start, String end) {
        int from = source.indexOf(start);
        int to = source.indexOf(end, from);
        if (from < 0 || to <= from) throw new AssertionError("Missing source section: " + start);
        return source.substring(from, to);
    }

    private static String read(String relativePath) throws Exception {
        Path root = Files.exists(Path.of("src", "leanback")) ? Path.of("") : Path.of("app");
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
