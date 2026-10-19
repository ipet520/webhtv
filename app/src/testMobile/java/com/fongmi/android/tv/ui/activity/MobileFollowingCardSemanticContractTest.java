package com.fongmi.android.tv.ui.activity;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

/** Mobile and shared Following cards must use the foreground semantic paired with their fill. */
public class MobileFollowingCardSemanticContractTest {

    private static final String MOBILE = "app/src/mobile/res/layout/item_following.xml";
    private static final String MAIN = "app/src/main/res/layout/item_following.xml";
    private static final String BADGE_SHAPE = "app/src/main/res/drawable/shape_sync_direction.xml";

    @Test
    public void badgeForegroundMatchesTheExistingPrimaryContainerFill() throws Exception {
        String shape = read(BADGE_SHAPE);
        assertTrue("the local badge keeps its existing primary-container fill",
                shape.contains("?attr/colorPrimaryContainer"));
        for (String path : new String[]{MOBILE, MAIN}) {
            String source = read(path);
            assertTrue(path + " badge text must pair with the retained container fill",
                    tagBlock(source, "@+id/badge").contains("?attr/colorOnPrimaryContainer"));
        }
    }

    @Test
    public void errorTextUsesTheThemeErrorRoleInBothLayouts() throws Exception {
        for (String path : new String[]{MOBILE, MAIN}) {
            assertTrue(path + " error text must use the current theme error role",
                    tagBlock(read(path), "@+id/error").contains("?attr/colorError"));
        }
    }

    @Test
    public void mobileFollowingTextDoesNotUseTheTwoMismatchedPaletteConstants() throws Exception {
        String source = read(MOBILE);
        assertFalse(source.contains("#174EA6"));
        assertFalse(source.contains("#B3261E"));
    }

    private static String tagBlock(String source, String id) {
        int at = source.indexOf(id);
        assertTrue("missing " + id, at >= 0);
        int end = source.indexOf("/>", at);
        assertTrue("tag must be self-closing: " + id, end > at);
        return source.substring(at, end);
    }

    private static String read(String path) throws Exception {
        Path direct = Path.of(path);
        if (Files.exists(direct)) return Files.readString(direct, StandardCharsets.UTF_8);
        return Files.readString(Path.of("..").resolve(path), StandardCharsets.UTF_8);
    }
}