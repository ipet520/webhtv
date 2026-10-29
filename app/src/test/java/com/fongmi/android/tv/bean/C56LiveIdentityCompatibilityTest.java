package com.fongmi.android.tv.bean;

import com.github.catvod.utils.Trans;
import com.google.gson.Gson;
import org.junit.After;
import org.junit.Test;
import static org.junit.Assert.*;

public class C56LiveIdentityCompatibilityTest {
    @After public void resetLanguage() { Trans.setTraditional(null); }

    @Test public void oldTranslatedSourcePreferencesAreCopiedWithoutChangingRawIdentity() {
        Gson gson = new Gson();
        Live saved = gson.fromJson("{\"name\":\"電視源\",\"pass\":true,\"boot\":true,\"keep\":\"新聞@@@CCTV1@@@0\"}", Live.class);
        Live source = gson.fromJson("{\"name\":\"电视源\"}", Live.class);
        source.sync(saved);
        for (boolean traditional : new boolean[]{true, false, true}) {
            Trans.setTraditional(traditional);
            source.trans();
            assertEquals("电视源", source.getName());
            assertEquals(traditional ? "電視源" : "电视源", source.getDisplayName());
            assertEquals("新聞@@@CCTV1@@@0", source.getKeep());
            assertTrue(source.isPass());
            assertTrue(source.isBoot());
        }
        assertFalse(gson.toJson(source).contains("displayName"));
    }

    @Test public void groupIdentityAndLegacyTranslationRemainReachable() {
        Group group = new Gson().fromJson("{\"name\":\"新闻\"}", Group.class);
        for (boolean traditional : new boolean[]{false, true, false}) {
            Trans.setTraditional(traditional);
            group.trans();
            assertEquals("新闻", group.getName());
            assertEquals(traditional ? "新聞" : "新闻", group.getDisplayName());
            assertTrue(group.matchesName("新聞"));
            assertTrue(group.matchesName("新闻"));
            assertFalse(group.matchesName("体育"));
        }
    }
}
