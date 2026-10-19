package com.fongmi.android.tv.ui.activity;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Source-contract regression checks for the TV detail shell. */
public class VideoActivityDetailShellSourceTest {

    @Test
    public void emptyDetailFromSearchKeepsPlaybackPageForRetry() throws Exception {
        Path path = Path.of("src", "leanback", "java", "com", "fongmi", "android", "tv", "ui", "activity", "VideoActivity.java");
        if (!Files.isRegularFile(path)) path = Path.of("app").resolve(path);
        String source = Files.readString(path, StandardCharsets.UTF_8);
        int start = source.indexOf("private void setEmpty(boolean finish) {");
        int end = source.indexOf("\n    private void showEmpty()", start);

        assertTrue("VideoActivity must keep a dedicated empty-detail path", start >= 0 && end > start);
        String empty = source.substring(start, end);
        assertTrue("explicit detail errors must still close the failed playback page",
                empty.contains("if (finish) {"));
        assertFalse("a failed search-result detail must not close the playback page and reveal search results",
                empty.contains("isFromCollect() || finish"));
        assertTrue("the failed detail must retain the title and enter the existing retry path",
                empty.contains("mBinding.name.setText(getName());")
                        && empty.contains("checkSearch(false);"));
    }

    @Test
    public void directPlaybackKeepsSynopsisButtonVisibleByDefault() throws Exception {
        Path path = Path.of("src", "leanback", "res", "layout", "activity_video.xml");
        if (!Files.isRegularFile(path)) path = Path.of("app").resolve(path);
        String xml = Files.readString(path, StandardCharsets.UTF_8);
        int id = xml.indexOf("@+id/content\"");
        int end = xml.indexOf(">", id);

        assertTrue("the leanback playback page must keep its synopsis (简介) button", id >= 0 && end > id);
        String block = xml.substring(id, end);
        assertFalse("影视原生 (DIRECT) never runs the TMDB reveal that re-shows the button, so the layout "
                        + "must not default it to gone (regression from 934e831de)",
                block.contains("android:visibility=\"gone\""));

        Path source = Path.of("src", "leanback", "java", "com", "fongmi", "android", "tv", "ui", "activity", "VideoActivity.java");
        if (!Files.isRegularFile(source)) source = Path.of("app").resolve(source);
        String activity = Files.readString(source, StandardCharsets.UTF_8);
        int tag = activity.indexOf("mBinding.content.setTag(item.getContent());");
        assertTrue("setDetail must still feed the synopsis tag so the dialog can open", tag >= 0);
    }
    @Test
    public void directPlaybackRendersInlineSynopsisPreview() throws Exception {
        Path source = Path.of("src", "leanback", "java", "com", "fongmi", "android", "tv", "ui", "activity", "VideoActivity.java");
        if (!Files.isRegularFile(source)) source = Path.of("app").resolve(source);
        String activity = Files.readString(source, StandardCharsets.UTF_8);
        int helper = activity.indexOf("private void updateOverviewPreview()");
        int helperEnd = activity.indexOf("private void updateTmdbOverviewButton()", helper);
        assertTrue("VideoActivity must keep one shared synopsis preview writer", helper >= 0 && helperEnd > helper);
        String body = activity.substring(helper, helperEnd);
        assertTrue("the preview must be rendered into the synopsis node", body.contains("mBinding.tmdbOverview.setText(overviewText)") && body.contains("mBinding.tmdbOverview.setVisibility(View.VISIBLE)"));
        int setText = activity.indexOf("private void setText(Vod item) {");
        int setTextEnd = activity.indexOf("private void setText(TextView view, int resId, String text)", setText);
        assertTrue("DIRECT details must feed the preview without waiting for TMDB", setText >= 0 && setTextEnd > setText && activity.substring(setText, setTextEnd).contains("if (!isTmdbSourceEnabled()) updateOverviewPreview();"));
        int button = activity.indexOf("private void updateTmdbOverviewButton()");
        int buttonEnd = activity.indexOf("private int getTmdbOverviewMaxLines(", button);
        assertTrue("DIRECT must keep the full-synopsis button reachable", button >= 0 && buttonEnd > button && activity.substring(button, buttonEnd).contains("truncated || directMode"));
    }

    @Test
    public void directEpisodeChipsSeparateMarqueeAndCurrentState() throws Exception {
        Path source = Path.of("src", "leanback", "java", "com", "fongmi", "android", "tv", "ui", "adapter", "EpisodeAdapter.java");
        if (!Files.isRegularFile(source)) source = Path.of("app").resolve(source);
        String adapter = Files.readString(source, StandardCharsets.UTF_8);
        int helper = adapter.indexOf("private void applyTextActive(");
        int helperEnd = adapter.indexOf("private void bindCardView(", helper);
        assertTrue("text episode chips must use a shared state writer", helper >= 0 && helperEnd > helper);
        String body = adapter.substring(helper, helperEnd);
        assertTrue("selected must remain the marquee/focus state", body.contains("setSelected(active || hasFocus)"));
        assertTrue("activated must represent the currently playing episode", body.contains("setActivated(active)"));
        Path selectorPath = Path.of("src", "leanback", "res", "drawable", "selector_video_item.xml");
        if (!Files.isRegularFile(selectorPath)) selectorPath = Path.of("app").resolve(selectorPath);
        String selector = Files.readString(selectorPath, StandardCharsets.UTF_8);
        int focusedState = selector.indexOf("<item android:state_focused");
        int activatedState = selector.indexOf("<item android:state_activated");
        assertTrue("focus ring must take precedence over the persistent current-episode ring",
                focusedState >= 0 && activatedState > focusedState);
    }

    @Test
    public void synopsisPreviewIsAnchoredBelowActorAndHiddenWithAudioDetails() throws Exception {
        Path xmlPath = Path.of("src", "leanback", "res", "layout", "activity_video.xml");
        if (!Files.isRegularFile(xmlPath)) xmlPath = Path.of("app").resolve(xmlPath);
        String xml = Files.readString(xmlPath, StandardCharsets.UTF_8);
        int id = xml.indexOf("@+id/tmdbOverview\"");
        int end = xml.indexOf("/>", id);
        assertTrue("synopsis preview should be placed below actor row", id >= 0 && end > id && xml.substring(id, end).contains("android:layout_below=\"@+id/actor\""));
        Path source = Path.of("src", "leanback", "java", "com", "fongmi", "android", "tv", "ui", "activity", "VideoActivity.java");
        if (!Files.isRegularFile(source)) source = Path.of("app").resolve(source);
        String activity = Files.readString(source, StandardCharsets.UTF_8);
        int details = activity.indexOf("private void setVideoDetailsVisible(boolean visible)");
        int detailsEnd = activity.indexOf("private boolean shouldShowVideoDetailRemark(", details);
        assertTrue("audio mode must hide the synopsis with the other detail fields", details >= 0 && detailsEnd > details && activity.substring(details, detailsEnd).contains("visible && !TextUtils.isEmpty(mBinding.tmdbOverview.getText())"));
    }

}
