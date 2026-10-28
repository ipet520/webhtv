package com.fongmi.android.tv.ui.home;

import android.app.Application;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.github.catvod.Init;
import java.util.*;
import java.util.function.Consumer;
import java.util.concurrent.TimeUnit;
import org.junit.*;
import org.junit.runner.RunWith;
import org.robolectric.*;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class) @Config(sdk=28,application=NativeHomeBusinessTest.FixtureApp.class)
public class NativeHomeBusinessTest {
    public static class FixtureApp extends com.fongmi.android.tv.App { @Override public void onCreate(){Init.set(this);} }
    record Call(String site,String type,int page,HashMap<String,String> extend,Consumer<Result> ok,Consumer<Throwable> error) {}
    static class Gateway implements NativeHomeBrowseController.Gateway {
        final List<Call> calls=new ArrayList<>();int cancels;
        public void home(Consumer<Result> ok,Consumer<Throwable> error){calls.add(new Call("","home",1,new HashMap<>(),ok,error));}
        public void category(String site,String type,int page,HashMap<String,String> extend,Consumer<Result> ok,Consumer<Throwable> error){calls.add(new Call(site,type,page,extend,ok,error));}
        public void cancel(){cancels++;}
        Call last(){return calls.get(calls.size()-1);}
    }
    Gateway gateway;NativeHomeBrowseController controller;int publishes;
    @Before public void setup(){Init.set(RuntimeEnvironment.getApplication());gateway=new Gateway();controller=new NativeHomeBrowseController(gateway,r->{});controller.attach("site-A",c->publishes++);}
    @After public void close(){controller.detach();}
    static Result result(String json){return Result.objectFrom(json);}
    static Result home(){return result("{\"class\":[{\"type_id\":\"A\",\"type_name\":\"电影\",\"filters\":[{\"key\":\"year\",\"init\":\"\",\"value\":[{\"n\":\"全部\",\"v\":\"\"},{\"n\":\"2026\",\"v\":\"2026\"}]}]},{\"type_id\":\"B\",\"type_name\":\"剧集\"}],\"list\":[{\"vod_id\":\"home1\",\"vod_name\":\"推荐\"}]}");}
    static Result page(String id,int pages){return result("{\"pagecount\":"+pages+",\"list\":[{\"vod_id\":\""+id+"\",\"vod_name\":\""+id+"\"}]}");}
    static void idle(){ShadowLooper.idleMainLooper(101,TimeUnit.MILLISECONDS);}
    void homeReady(){gateway.last().ok.accept(home());idle();}
    void activate(String type){controller.focusCategory(type);idle();}
    @Test public void realRecommendationIdentityOrderAndNoDuplicateAttach(){homeReady();assertEquals("home1",controller.state().items.get(0).getId());controller.attach("site-A",c->{});assertEquals(1,gateway.calls.size());assertEquals(3,controller.types().size());}
    @Test public void debounceOnlyFinalFocusAndConfirmDoesNotReload(){homeReady();controller.focusCategory("A");controller.focusCategory("B");controller.focusCategory("A");assertEquals(1,gateway.calls.size());idle();assertEquals(2,gateway.calls.size());assertEquals("A",gateway.last().type);controller.flushCategory();controller.focusCategory("A");idle();assertEquals(2,gateway.calls.size());}
    @Test public void paginationIsExclusiveDeduplicatedAndStopsAtPageCount(){homeReady();activate("A");gateway.last().ok.accept(page("a",2));idle();controller.focused(0);Call next=gateway.last();assertEquals(2,next.page);controller.focused(0);assertSame(next,gateway.last());next.ok.accept(result("{\"pagecount\":2,\"list\":[{\"vod_id\":\"a\",\"vod_name\":\"a\"},{\"vod_id\":\"b\",\"vod_name\":\"b\"}]}"));idle();assertEquals(2,controller.state().items.size());controller.focused(1);assertSame(next,gateway.last());}
    @Test public void cachedABARestoresPositionAndDoesNotRepeatFirstPage(){homeReady();activate("A");gateway.last().ok.accept(page("a",1));idle();var state=controller.state();state.position=0;activate("B");gateway.last().ok.accept(page("b",1));idle();int count=gateway.calls.size();activate("A");assertSame(state,controller.state());assertEquals(count,gateway.calls.size());assertEquals("a",controller.state().items.get(0).getId());}
    @Test public void lateCategoryAndSourceResultsCannotOverwrite(){homeReady();activate("A");Call a=gateway.last();activate("B");Call b=gateway.last();b.ok.accept(page("b",1));a.ok.accept(page("old-a",1));idle();assertEquals("b",controller.state().items.get(0).getId());controller.reload("site-B");Call homeB=gateway.last();a.ok.accept(page("old-site",1));homeB.ok.accept(home());idle();assertEquals("home1",controller.state().items.get(0).getId());assertEquals("site-B",controller.siteKey());}
    @Test public void filtersUseActualKeysDefaultsAndToggleBackToDefault(){homeReady();activate("A");gateway.last().ok.accept(page("default",1));idle();controller.selectFilter(0,1);assertEquals("2026",gateway.last().extend.get("year"));gateway.last().ok.accept(page("year",1));idle();int count=gateway.calls.size();controller.selectFilter(0,1);assertEquals("",controller.state().extend.get("year"));assertEquals(count,gateway.calls.size());assertEquals("default",controller.state().items.get(0).getId());}
    @Test public void failedNextPageKeepsCardsAndRetriesSamePage(){homeReady();activate("A");gateway.last().ok.accept(page("a",0));idle();controller.focused(0);gateway.last().error.accept(new RuntimeException("offline"));idle();assertEquals(1,controller.state().items.size());assertEquals(1,controller.state().page);assertFalse(controller.state().error.isEmpty());controller.focused(0);assertEquals(2,gateway.last().page);}
    @Test public void emptyHomeUsesNativeFirstClassFallbackWithoutFilterExtends(){gateway.last().ok.accept(result("{\"class\":[{\"type_id\":\"A\",\"type_name\":\"电影\"}],\"list\":[]}"));idle();assertEquals("A",gateway.last().type);assertTrue(gateway.last().extend.isEmpty());gateway.last().ok.accept(page("fallback",1));idle();assertEquals("home",controller.activeType());assertEquals("fallback",controller.state().items.get(0).getId());}
    @Test public void emptyAndFailureAreDistinctAndNoFixtureAppears(){gateway.last().ok.accept(result("{\"list\":[]}"));idle();assertTrue(controller.state().items.isEmpty());assertEquals("",controller.state().error);controller.reload("site-A");gateway.last().error.accept(new RuntimeException());idle();assertFalse(controller.state().error.isEmpty());}
    @Test public void folderBackAndDetachPreserveContextWithoutRequest(){homeReady();activate("A");gateway.last().ok.accept(page("a",1));idle();var original=controller.state();Vod folder=new Vod();folder.setId("directory");controller.openFolder(folder);assertEquals("directory",gateway.last().type);gateway.last().ok.accept(page("child",1));idle();assertTrue(controller.state().folder);assertTrue(controller.backFolder());assertSame(original,controller.state());int count=gateway.calls.size();controller.detach();controller.attach("site-A",c->{});assertEquals(count,gateway.calls.size());assertSame(original,controller.state());}
    @Test public void vodDuringInitialLoadActivatesRealFirstClass(){controller.openFirstCategory();homeReady();assertEquals("A",controller.activeType());assertEquals("A",gateway.last().type);}
    @Test public void declaredSiteFailureIsNotNormalEmpty(){gateway.last().ok.accept(Result.error("站源错误"));idle();assertEquals("站源错误",controller.state().error);assertTrue(controller.state().items.isEmpty());}
    @Test public void sharedViewModelCancellationRejectsAlreadyQueuedUiPublication() throws Exception {
        var model=new com.fongmi.android.tv.model.SiteViewModel();var store=new androidx.lifecycle.ViewModelStore();store.put("model",model);
        List<Result> observed=new ArrayList<>();androidx.lifecycle.Observer<Result> observer=observed::add;model.getResult().observeForever(observer);
        java.lang.Class<?> type=java.lang.Class.forName("com.fongmi.android.tv.model.SiteViewModel$TaskType");
        Object resultType=java.lang.Enum.valueOf((java.lang.Class)type,"RESULT");
        var execute=com.fongmi.android.tv.model.SiteViewModel.class.getDeclaredMethod("execute",type,androidx.lifecycle.MutableLiveData.class,java.util.concurrent.Callable.class,Consumer.class,Consumer.class);execute.setAccessible(true);
        var done=new java.util.concurrent.CountDownLatch(1);
        execute.invoke(model,resultType,model.getResult(),(java.util.concurrent.Callable<Result>)()->page("stale",1),(Consumer<Result>)r->done.countDown(),null);
        assertTrue(done.await(3,TimeUnit.SECONDS));Thread.sleep(30);model.cancelHomeContent();ShadowLooper.idleMainLooper();assertTrue(observed.isEmpty());
        model.getResult().removeObserver(observer);store.clear();
    }
    @Test public void hiddenClassicPageResumesOnlyOnceAndKeepsItsPageParameter() throws Exception {
        class Model extends com.fongmi.android.tv.model.SiteViewModel {
            int calls,cancels;String requestedPage;
            @Override public void categoryContent(String site,String type,String page,boolean filter,HashMap<String,String> extend){calls++;requestedPage=page;assertEquals("A",type);}
            @Override public void cancelHomeContent(){cancels++;super.cancelHomeContent();}
        }
        var type=com.fongmi.android.tv.ui.fragment.TypeFragment.newInstance("site-A","A",com.fongmi.android.tv.bean.Style.rect(),new HashMap<>(),false);
        Model model=new Model();
        for(var entry:Map.of("mViewModel",(Object)model,"mExtends",new HashMap<String,String>(),"presentationPendingPage","2").entrySet()){
            var field=type.getClass().getDeclaredField(entry.getKey());field.setAccessible(true);field.set(type,entry.getValue());
        }
        type.setHomePresentationActive(false);assertEquals(1,model.cancels);assertEquals(0,model.calls);assertFalse(type.onLoadMore("3"));
        type.setHomePresentationActive(true);type.setHomePresentationActive(true);assertEquals(1,model.calls);assertEquals("2",model.requestedPage);
        var store=new androidx.lifecycle.ViewModelStore();store.put("model",model);store.clear();
    }
}
