package com.fongmi.android.tv.ui.home;

import java.util.*;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
import static com.fongmi.android.tv.ui.home.NativeHomeBrowseController.LoadState.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class)
public class NativeHomeLoadingTest {
    NativeHomeBusinessTest.Gateway gateway;NativeHomeBrowseController controller;
    @Before public void setup(){gateway=new NativeHomeBusinessTest.Gateway();controller=new NativeHomeBrowseController(gateway,r->{});controller.attach("site",c->{});}
    @After public void close(){controller.detach();}
    @Test public void coldHomeFallbackHasNoFalseReadyFrame(){
        List<NativeHomeBrowseController.LoadState> seen=new ArrayList<>();controller.attach("site",c->seen.add(c.loadState()));
        assertEquals(COLD,controller.loadState());gateway.last().ok().accept(NativeHomeBusinessTest.result("{\"class\":[{\"type_id\":\"A\",\"type_name\":\"电影\"}]}"));NativeHomeBusinessTest.idle();
        assertEquals(COLD,controller.loadState());assertFalse(seen.contains(NONE));
        gateway.last().ok().accept(NativeHomeBusinessTest.page("fallback",1));NativeHomeBusinessTest.idle();assertEquals(NONE,controller.loadState());assertFalse(controller.state().items.isEmpty());
    }
    @Test public void cachedCategoryDoesNotWaitAndAppendRetainsCardsAndStaleCompletionCannotEndNewLoad(){
        gateway.last().ok().accept(NativeHomeBusinessTest.home());NativeHomeBusinessTest.idle();assertEquals(NONE,controller.loadState());
        controller.focusCategory("A");controller.flushCategory();assertEquals(RAIL,controller.loadState());var stale=gateway.last();
        controller.focusCategory("B");controller.flushCategory();stale.ok().accept(NativeHomeBusinessTest.page("stale",1));NativeHomeBusinessTest.idle();assertEquals(RAIL,controller.loadState());
        gateway.last().ok().accept(NativeHomeBusinessTest.page("b",2));NativeHomeBusinessTest.idle();assertEquals(NONE,controller.loadState());
        Object state=controller.state();int count=controller.state().items.size();controller.focused(count-1);assertEquals(APPEND,controller.loadState());assertEquals(count,controller.state().items.size());
        controller.focusCategory("home");controller.flushCategory();int requests=gateway.calls.size();controller.focusCategory("B");controller.flushCategory();assertSame(state,controller.state());assertEquals(NONE,controller.loadState());assertEquals(requests,gateway.calls.size());
    }
}
