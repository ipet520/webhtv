package com.fongmi.android.tv.ui.activity;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TmdbDisambiguationConcurrencySourceTest {

    private static final Path SOURCE = Path.of(
            "src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java");

    @Test
    public void candidateDetailsAreLiteBoundedAndOnlyWinnerLoadsFullBundle() throws Exception {
        String source = Files.readString(SOURCE, StandardCharsets.UTF_8);
        assertTrue(source.contains("private static final int DISAMBIGUATION_CONCURRENCY = 3;"));

        String fetch = methodBody(source, "private JsonObject[] fetchDisambiguationDetails(");
        assertFalse(fetch.isEmpty());
        assertTrue(fetch.contains("start += DISAMBIGUATION_CONCURRENCY"));
        assertTrue(fetch.contains("detailForDisambiguation(item, tmdbConfig)"));
        assertTrue(fetch.contains("futures.get(i - start).get()"));
        assertFalse(fetch.contains("loadTmdbBundle("));

        String strict = methodBody(source, "private TmdbBundle chooseTmdbBundle(");
        assertFalse(strict.isEmpty());
        assertTrue(strict.contains("fetchDisambiguationDetails(candidates, count)"));
        assertTrue(strict.contains("scoreTmdbPeople(detail, sourceVod)"));
        assertTrue(strict.contains("loadDisambiguationWinner(best.item)"));
        assertFalse(strict.contains("loadTmdbBundle("));

        String smart = methodBody(source, "private TmdbBundle chooseSmartTmdbBundle(");
        assertFalse(smart.isEmpty());
        assertTrue(smart.contains("fetchDisambiguationDetails(candidates, count)"));
        assertTrue(smart.contains("scoreTmdbPeople(detail, sourceVod)"));
        assertTrue(smart.contains("loadDisambiguationWinner(best.item)"));
        assertFalse(smart.contains("loadTmdbBundle("));

        String winner = methodBody(source, "private TmdbBundle loadDisambiguationWinner(");
        assertTrue(winner.contains("loadTmdbBundle(item)"));
    }

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        if (start < 0) return "";
        int brace = source.indexOf('{', start);
        int depth = 0;
        for (int i = brace; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            if (c == '}' && --depth == 0) return source.substring(start, i + 1);
        }
        return "";
    }
}
