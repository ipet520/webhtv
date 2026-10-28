package com.fongmi.android.tv.setting;

import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/** Tests extracted only from upstream 772a25a4: stable playback-line identity. The older optional defer-playback feature is not installed locally and is outside C55. */
public class FlagSwitchPlaybackWiringTest {

    private static final String MOBILE = "app/src/mobile/java/com/fongmi/android/tv/ui/activity/VideoActivity.java";
    private static final String LEANBACK = "app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java";







    private static void assertProgrammatic(String source, String path, String method, String click) {
        String body = methodBody(source, method);
        int call = body.indexOf(click);
        assertTrue(path + " 的 " + method + " 必须仍调用 " + click, call >= 0);
        int arm = body.lastIndexOf("mProgrammaticFlagSwitch = true;", call);
        int reset = body.indexOf("mProgrammaticFlagSwitch = false;", call);
        assertTrue(path + " 的 " + method + " 必须在调用前置位程序化标记", arm >= 0);
        assertTrue(path + " 的 " + method + " 必须在调用后复位程序化标记", reset > call);
    }


    @Test
    public void bothPlayersIdentifyTheCurrentStreamByTheAppliedFlagKey() throws Exception {
        for (String path : new String[]{MOBILE, LEANBACK}) {
            String source = read(path);
            assertTrue(path + " 必须记住实际已应用的线路稳定键",
                    source.contains("private String mPlaybackFlagKey = \"\";")
                            && source.contains("private String playbackFlagKey(Flag flag) {")
                            && source.contains("private boolean isPlaybackFlag(Flag flag) {")
                            && source.contains("private boolean isPlayingContent(Episode episode) {"));
            String invalidate = methodBody(source, "private void invalidatePlayerContent()");
            assertTrue(path + " 作废在途取流时必须同时作废已应用线路键",
                    invalidate.contains("mPlaybackFlagKey = \"\";"));
            String getPlayer = methodBody(source, "private void getPlayer(Flag flag, Episode episode)");
            int begin = getPlayer.indexOf("beginPlayerContentRequest(getKey(), playFlag, episode.getUrl());");
            int remember = getPlayer.indexOf("mPlaybackFlagKey = playbackFlagKey(flag);");
            assertTrue(path + " 起播时必须记录线路键", begin >= 0 && remember > begin);
            assertTrue(path + " 只按标记不能认定该集正在播放",
                    methodBody(source, "private boolean shouldEnterFullscreen(Episode item)")
                            .contains("isPlayingContent(item)"));
            assertTrue(path + " 平滑换线路必须确认当前流确实属于该线路",
                    methodBody(source, "private boolean seamless(Flag flag)")
                            .contains("(isPlaybackFlag(flag) && episode.isSelected())"));
            assertTrue(path + " 线路点击早退必须以当前线路身份为前提",
                    methodBody(source, "public void onItemClick(Flag item)").contains("isPlaybackFlag(resolved)"));
        }
    }

    @Test
    public void detailInlinePlaybackAlsoComparesTheAppliedFlagKey() throws Exception {
        String source = read(DETAIL);
        assertTrue("详情页必须记住内联播放的线路稳定键",
                source.contains("private String inlinePlaybackFlagKey = \"\";"));
        String start = methodBody(source, "private void startInlinePlayer(Result result, long resumePosition)");
        assertTrue("内联起播必须记录线路稳定键",
                start.contains("inlinePlaybackFlagKey = selectedFlag == null ? \"\" : selectedSeasonFlagKey();"));
        String stop = methodBody(source, "private void stopInlinePlayerForReload()");
        assertTrue("内联停止必须清空线路稳定键", stop.contains("inlinePlaybackFlagKey = \"\";"));
        assertTrue("同名线路必须靠稳定键区分是否仍是当前播放",
                source.contains("&& TextUtils.equals(selectedSeasonFlagKey(), inlinePlaybackFlagKey)"));
        assertTrue("所有内联停止点都必须清空线路稳定键", occurrences(source, "inlinePlaybackFlagKey = \"\";") >= 2);
    }

    private static int occurrences(String source, String token) {
        int count = 0;
        int index = 0;
        while ((index = source.indexOf(token, index)) >= 0) {
            count++;
            index += token.length();
        }
        return count;
    }

    private static final String DETAIL = "app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java";

    private static String methodBody(String source, String signature) {
        int start = source.indexOf(signature);
        assertTrue("missing method " + signature, start >= 0);
        int open = source.indexOf('{', start);
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') depth++;
            else if (c == '}' && --depth == 0) return source.substring(start, i + 1);
        }
        throw new AssertionError("unbalanced method " + signature);
    }

    private static Path sourcePath(String sourceSet, String kind) {
        return root().resolve(Path.of("app", "src", sourceSet, kind));
    }

    /** Tests extracted only from upstream 772a25a4: stable playback-line identity. The older optional defer-playback feature is not installed locally and is outside C55. */
    private static Path root() {
        return Files.exists(Path.of("app", "src", "main")) ? Path.of(".") : Path.of("..");
    }

    private static String read(String path) throws Exception {
        return read(root().resolve(path));
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
