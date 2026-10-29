package com.fongmi.android.tv.title;

import com.fongmi.android.tv.bean.TmdbConfig;
import com.fongmi.android.tv.bean.TmdbItem;
import com.fongmi.android.tv.ui.helper.TmdbMatcher;
import com.github.catvod.utils.Trans;
import org.junit.After;
import org.junit.Test;
import java.lang.reflect.Method;
import java.util.List;
import static org.junit.Assert.*;

public class C56TitleIdentityTest {
    @After public void resetLanguage() { Trans.setTraditional(null); }

    @Test public void comparisonIsCanonicalButActualQueryKeepsTheSourceScript() throws Exception {
        TmdbMatcher matcher = new TmdbMatcher(null, TmdbConfig.objectFrom("{}"));
        Method compare = method("compareKey", String.class);
        Method clean = method("cleanVideoName", String.class);
        for (String title : List.of("战斗之魂", "超巡！超条前辈", "归路", "弹珠汽水瓶里的千岁同学")) {
            String hant = Trans.s2t(false, title);
            for (boolean traditional : new boolean[]{false, true}) {
                Trans.setTraditional(traditional);
                assertEquals(compare.invoke(matcher, title), compare.invoke(matcher, hant));
                assertEquals(title, clean.invoke(matcher, title));
                assertEquals(hant, clean.invoke(matcher, hant));
            }
        }
    }

    @Test public void canonicalizationDoesNotEraseYearOrMediaType() throws Exception {
        TmdbMatcher matcher = new TmdbMatcher(null, TmdbConfig.objectFrom("{}"));
        Method compare = method("compareKey", String.class);
        assertNotEquals(compare.invoke(matcher, "归路2023"), compare.invoke(matcher, "歸路2024"));
        Method filter = method("filterByMediaType", List.class, String.class);
        TmdbItem tv = new TmdbItem(1, "tv", "归路", "", "", "", "");
        TmdbItem movie = new TmdbItem(2, "movie", "歸路", "", "", "", "");
        assertEquals(List.of(tv), filter.invoke(matcher, List.of(tv, movie), "tv"));
        assertEquals(List.of(movie), filter.invoke(matcher, List.of(tv, movie), "movie"));
    }

    private static Method method(String name, Class<?>... types) throws Exception {
        Method method = TmdbMatcher.class.getDeclaredMethod(name, types);
        method.setAccessible(true);
        return method;
    }
}
