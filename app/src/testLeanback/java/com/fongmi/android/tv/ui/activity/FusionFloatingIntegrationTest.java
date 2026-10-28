package com.fongmi.android.tv.ui.activity;
import java.nio.file.*;
import org.junit.Test;
import static org.junit.Assert.*;
@org.junit.runner.RunWith(org.robolectric.RobolectricTestRunner.class)
@org.robolectric.annotation.Config(sdk=28,application=com.fongmi.android.tv.ui.home.NativeHomeBusinessTest.FixtureApp.class,qualifiers="w960dp-h540dp-land-xhdpi")
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class FusionFloatingIntegrationTest {
    static String source(String path)throws Exception{return Files.readString(Path.of("src/main/java/com/fongmi/android/tv/"+path));}
    static String method(String s,String signature){int a=s.indexOf(signature),start=s.indexOf('{',a),depth=1,b=start+1;while(depth>0){char c=s.charAt(b++);if(c=='{')depth++;if(c=='}')depth--;}return s.substring(a,b);}
    @Test public void existingSingleScrollEntryOwnsLayoutAndAutomaticPathNeverRestartsPlayback()throws Exception{
        String s=source("ui/activity/TmdbDetailActivity.java");assertEquals(1,s.split("binding.scroll.setOnScrollChangeListener",-1).length-1);
        String update=method(s,"private boolean updateFusionFloatingLayout()");assertTrue(update.contains("Util.isLeanback() && modeController.isFusionMode()"));assertTrue(update.contains("!inlineFullscreen"));assertTrue(update.contains("!isPlaybackExiting()"));
        String panel=source("ui/detail/FusionFloatingPanel.java");assertTrue(panel.contains("if(android.os.Build.VERSION.SDK_INT>=26)handle.setDefaultFocusHighlightEnabled(false)"));for(String forbidden:new String[]{"new PlayerView","setPlayer(","setRender(","detachSurface(","preparePlayer(","parse(","scrollTo(","removeView(panel)","removeView(video)"})assertFalse(forbidden,panel.contains(forbidden));
        String sync=method(s,"private void syncInlinePlayerToSpacer()");assertTrue(sync.contains("if (updateFusionFloatingLayout()) return;"));
    }
    @Test public void episodeSourceAndFullscreenPathsKeepTheirOriginalBusinessAndAddOnlyDisplayGuards()throws Exception{
        String s=source("ui/activity/TmdbDetailActivity.java");assertTrue(method(s,"private void startInlinePlayer(Result result, long resumePosition)").contains("if (!isFusionFloating()) binding.playerPanel.requestFocus();"));
        assertTrue(method(s,"private void focusInlinePlayerPanel()").contains("|| isFusionFloating()"));assertTrue(method(s,"private void resetDetailState()").contains("fusionFloatingReload = true"));
        assertTrue(method(s,"private void enterInlineFullscreen()").contains("fusionFloating.suspend()"));
        assertTrue(method(s,"private void restoreInlineDetailScrollAfterOverlay()").contains("fusionFullscreenFromFloating"));
        assertTrue(method(s,"private void playAdjacentEpisode(int direction)").contains("playAdjacentEpisode(direction, true)"));
        assertTrue(method(s,"private void playInline(long resumePosition, String failedUrl, String failureMessage)").contains("SiteApi.playerContent"));
    }
    @Test public void actualActivityKeepsOriginalPanelAndScrollAcrossExplicitFullscreen()throws Exception{
        var f=new TmdbDetailResponsiveLayoutTest();f.create();f.mode(true,false);
        try {
            f.b.contentPanel.setVisibility(android.view.View.VISIBLE);
            f.b.contentPanel.getLayoutParams().height=3600;
            var episode=new android.widget.Button(f.activity);episode.setText("Episode under player");episode.setFocusableInTouchMode(true);
            var episodeParams=new android.widget.LinearLayout.LayoutParams(300,100);episodeParams.topMargin=400;
            f.b.contentInner.addView(episode,0,episodeParams);
            f.call("applyDetailTemplate");f.layout(1920,1080);f.call("setupInlinePlayerSpacerSync");f.layout(1920,1080);
            var presentation=(com.fongmi.android.tv.ui.detail.FusionFloatingPanel)org.robolectric.util.ReflectionHelpers.getField(f.activity,"fusionFloating");
            assertNotNull(presentation);
            org.robolectric.util.ReflectionHelpers.setField(f.activity,"fusionFloatingReload",true);
            f.b.scroll.scrollTo(0,1200);episode.requestFocus();
            assertTrue("browsing must start from a visible detail control",episode.getGlobalVisibleRect(new android.graphics.Rect()));
            assertTrue(episode.hasFocus());
            assertTrue(presentation.update(true,true,true));
            assertTrue(f.activity.dispatchKeyEvent(new android.view.KeyEvent(0,android.view.KeyEvent.KEYCODE_DPAD_RIGHT)));
            f.activity.dispatchKeyEvent(new android.view.KeyEvent(1,android.view.KeyEvent.KEYCODE_DPAD_RIGHT));
            assertTrue("actual Activity must expose the move entry",presentation.handle().hasFocus());
            f.activity.dispatchKeyEvent(new android.view.KeyEvent(0,android.view.KeyEvent.KEYCODE_DPAD_LEFT));
            f.activity.dispatchKeyEvent(new android.view.KeyEvent(1,android.view.KeyEvent.KEYCODE_DPAD_LEFT));
            assertTrue(episode.hasFocus());
            Object video=f.b.exo,parent=f.b.exo.getParent(),panelParent=f.b.playerPanel.getParent();int scrollY=f.b.scroll.getScrollY();
            f.call("applyDetailTemplate");f.layout(1920,1080);
            assertEquals(0,((android.widget.FrameLayout.LayoutParams)f.b.playerPanel.getLayoutParams()).leftMargin);
            assertEquals(scrollY,f.b.scroll.getScrollY());
            f.call("enterInlineFullscreen");assertEquals("enter fullscreen",scrollY,f.b.scroll.getScrollY());assertFalse(presentation.isFloating());assertTrue((boolean)org.robolectric.util.ReflectionHelpers.getField(f.activity,"inlineFullscreen"));
            f.layout(1920,1080);assertEquals("fullscreen layout",scrollY,f.b.scroll.getScrollY());f.call("exitInlineFullscreen");assertEquals("exit method",scrollY,f.b.scroll.getScrollY());f.layout(1920,1080);
            assertTrue(presentation.isFloating());assertEquals("after exit layout focus="+f.b.root.findFocus()+" remembered="+org.robolectric.util.ReflectionHelpers.getField(presentation,"detailFocus")+" child="+f.b.scroll.getChildAt(0).getHeight(),scrollY,f.b.scroll.getScrollY());assertSame(video,f.b.exo);assertSame(parent,f.b.exo.getParent());assertSame(panelParent,f.b.playerPanel.getParent());
            assertEquals(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS,f.b.playerPanel.getDescendantFocusability());
            // Restore to the watching region after a fullscreen round-trip, including card chrome.
            f.b.scroll.scrollTo(0,0);f.call("syncInlinePlayerToSpacer");f.layout(1920,1080);
            assertFalse(presentation.isFloating());
            assertEquals(0f,f.b.playerPanel.getTranslationZ(),.01f);
            assertEquals(com.fongmi.android.tv.utils.ResUtil.dp2px(20),f.b.playerPanel.getRadius(),.01f);
        } finally {f.activity.finish();}
    }
    @Test public void nonFusionActivityDoesNotCreatePresentationHelper()throws Exception{
        var f=new TmdbDetailResponsiveLayoutTest();f.create();f.mode(false,false);
        try {f.call("setupInlinePlayerSpacerSync");assertNull(org.robolectric.util.ReflectionHelpers.getField(f.activity,"fusionFloating"));}
        finally {f.activity.finish();}
    }
}
