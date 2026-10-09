package com.fongmi.android.tv.ui.activity;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Architecture contracts for episode-list ownership across TMDB workers and the TV UI. */
public class EpisodeListConcurrencyContractTest {

    @Test
    public void segmentAdaptersReceiveASnapshotInsteadOfALiveSubList() throws Exception {
        String source = read("app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java");
        String method = method(source, "private void showEpisodeSegment(int position)", "private boolean shouldEnterFullscreen");

        int snapshot = method.indexOf("new ArrayList<>(episodes.subList(start, end))");
        int episodeAdapter = method.indexOf("mEpisodeAdapter.addAll(items);");
        int gridAdapter = method.indexOf("mEpisodeGridAdapter.addAll(items);");
        assertTrue("the segment must be copied before either adapter consumes it", snapshot >= 0 && episodeAdapter > snapshot && gridAdapter > episodeAdapter);
        assertFalse("adapters must not receive the live parent-backed view", method.contains("List<Episode> items = episodes.subList(start, end);"));
    }

    @Test
    public void episodeMetadataWorkerDefersStructuralSortToMainCompletion() throws Exception {
        String source = read("app/src/main/java/com/fongmi/android/tv/ui/helper/TmdbUIAdapter.java");
        String worker = method(source, "private void loadEpisodeTitlesAsync(", "private void loadRelatedRecommendationsAsync(");
        String singleSeason = method(source, "private boolean applyEpisodeTitles(", "private boolean applyEpisodeTitlesForSlices(");
        String slices = method(source, "private boolean applyEpisodeTitlesForSlices(", "private boolean applyEpisodeMetadata(");
        String completion = method(source, "private void completeEpisodeMetadataOnMainThread(", "private List<TmdbEpisode> seasonEpisodes(");

        int asyncLog = worker.indexOf("SpiderDebug.log(\"tmdb\", \"episode titles async cost=");
        int finalGenerationCheck = worker.lastIndexOf("if (!isCurrentEpisodeMetadataRequest(generation, metadataGeneration, selectedSeason)) return;", asyncLog);
        int handoff = worker.indexOf("completeEpisodeMetadataOnMainThread(vod, generation, metadataGeneration, selectedSeason);");
        assertTrue("the worker must check generation before logging and handing completion to main",
                finalGenerationCheck >= 0 && finalGenerationCheck < asyncLog && handoff > asyncLog);
        assertTrue("the worker completion must cross the Activity main-thread boundary", worker.contains("completeEpisodeMetadataOnMainThread(vod, generation, metadataGeneration, selectedSeason);"));
        assertFalse("the worker itself must not structurally sort a live Flag list", worker.contains("TmdbEpisodeSorter.sort"));
        assertFalse("single-season mapping must leave ordering to main completion", singleSeason.contains("TmdbEpisodeSorter.sort"));
        assertFalse("multi-slice mapping must leave ordering to main completion", slices.contains("TmdbEpisodeSorter.sort"));

        int dispatch = completion.indexOf("activity.runOnUiThread(() -> {");
        int firstGenerationCheck = completion.indexOf("if (!isCurrentEpisodeMetadataRequest(generation, metadataGeneration, selectedSeason)) return;", dispatch);
        int secondGenerationCheck = completion.indexOf("if (!isCurrentEpisodeMetadataRequest(generation, metadataGeneration, selectedSeason)) return;", firstGenerationCheck + 1);
        int target = completion.indexOf("Flag target = activeFlagFor(vod);", secondGenerationCheck);
        int sort = completion.indexOf("TmdbEpisodeSorter.sort(target);", target);
        int finish = completion.indexOf("finishEpisodeMetadataLoad(vod, generation, metadataGeneration, selectedSeason);", sort);
        assertTrue("main completion must recheck generation, resolve the active target, sort, then refresh",
                dispatch >= 0 && firstGenerationCheck > dispatch && secondGenerationCheck > firstGenerationCheck
                        && target > secondGenerationCheck && sort > target && finish > sort);
    }

    private static String method(String source, String startMarker, String endMarker) {
        int start = source.indexOf(startMarker);
        int end = source.indexOf(endMarker, start);
        assertTrue("missing method contract: " + startMarker, start >= 0 && end > start);
        return source.substring(start, end);
    }

    private static String read(String relativePath) throws Exception {
        Path root = Files.exists(Path.of("app", "src", "main")) ? Path.of(".") : Path.of("..");
        return Files.readString(root.resolve(relativePath), StandardCharsets.UTF_8);
    }
}
