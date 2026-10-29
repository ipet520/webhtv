package com.fongmi.android.tv.ui.activity;

import androidx.media3.common.Player;
import androidx.media3.session.MediaController;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;
import org.robolectric.shadow.api.Shadow;
import org.robolectric.util.ReflectionHelpers;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk=28,application=android.app.Application.class,shadows=PlaybackControllerStateTest.ControllerShadow.class)
public class PlaybackControllerStateTest {
    @Implements(MediaController.class)
    public static class ControllerShadow {
        int state=Player.STATE_IDLE;
        int reads;
        @Implementation protected int getPlaybackState(){reads++;return state;}
    }
    private TmdbDetailActivity activity(){return Robolectric.buildActivity(TmdbDetailActivity.class).get();}
    private MediaController attach(TmdbDetailActivity activity){
        MediaController controller=Shadow.newInstanceOf(MediaController.class);
        ReflectionHelpers.setField(activity,"mController",controller);return controller;
    }
    private void idle(PlaybackActivity activity){assertTrue(activity.isIdle());assertFalse(activity.isBuffering());assertFalse(activity.isEnded());assertFalse(activity.isPaused());}
    @Test public void initializationAndRepeatedUiReadsWithoutControllerRemainNotReady(){
        TmdbDetailActivity activity=activity();for(int i=0;i<100;i++)idle(activity);
    }
    @Test public void connectedControllerPreservesEveryExistingStateAndPausedUsesOneSnapshot(){
        TmdbDetailActivity activity=activity();ControllerShadow controller=Shadow.extract(attach(activity));
        for(int state:new int[]{Player.STATE_IDLE,Player.STATE_BUFFERING,Player.STATE_READY,Player.STATE_ENDED}){
            controller.state=state;assertEquals(state==Player.STATE_IDLE,activity.isIdle());assertEquals(state==Player.STATE_BUFFERING,activity.isBuffering());assertEquals(state==Player.STATE_ENDED,activity.isEnded());
            int before=controller.reads;assertEquals(state!=Player.STATE_IDLE&&state!=Player.STATE_BUFFERING,activity.isPaused());assertEquals(before+1,controller.reads);
        }
    }
    @Test public void releaseAndRebindUiReadsDoNotRetainOldReadyOrEndedState(){
        TmdbDetailActivity activity=activity();ControllerShadow old=Shadow.extract(attach(activity));old.state=Player.STATE_READY;assertTrue(activity.isPaused());
        ReflectionHelpers.setField(activity,"mController",null);for(int i=0;i<100;i++)idle(activity);
        ControllerShadow next=Shadow.extract(attach(activity));next.state=Player.STATE_BUFFERING;assertTrue(activity.isBuffering());assertFalse(activity.isPaused());
        next.state=Player.STATE_READY;assertTrue(activity.isPaused());assertFalse(activity.isIdle());
        ReflectionHelpers.setField(activity,"mController",null);idle(activity);
    }
}
