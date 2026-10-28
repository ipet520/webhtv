package com.fongmi.android.tv.ui.home;

import android.os.Handler;
import android.os.Looper;
import com.fongmi.android.tv.bean.Result;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.bean.Filter;
import com.fongmi.android.tv.bean.Value;
import com.fongmi.android.tv.bean.Class;
import com.fongmi.android.tv.model.SiteViewModel;
import java.util.*;
import java.util.function.Consumer;

/** Main-thread presentation contexts, not a second data repository. One ViewModel RESULT slot. */
public final class NativeHomeBrowseController {
    public enum LoadState { NONE, COLD, RAIL, APPEND }
    public interface Gateway {
        void home(Consumer<Result> ok, Consumer<Throwable> error);
        void category(String site, String type, int page, HashMap<String,String> extend, Consumer<Result> ok, Consumer<Throwable> error);
        void cancel();
    }
    public static Gateway gateway(SiteViewModel model) {
        return new Gateway() {
            public void home(Consumer<Result> ok, Consumer<Throwable> error) { model.homeContent(ok, error); }
            public void category(String site, String type, int page, HashMap<String,String> extend, Consumer<Result> ok, Consumer<Throwable> error) {
                model.categoryContent(site, type, String.valueOf(page), true, extend, ok, error);
            }
            public void cancel() { model.cancelHomeContent(); }
        };
    }
    public static final class ContextState {
        public final String type;
        public final HashMap<String,String> extend;
        public final boolean folder;
        public final List<Vod> items = new ArrayList<>();
        public int page, pageCount, position;
        public boolean loaded, loading, ended, fallbackPending;
        public String error = "";
        ContextState(String type, HashMap<String,String> extend, boolean folder) { this.type=type; this.extend=new HashMap<>(extend); this.folder=folder; }
    }
    private final Gateway gateway;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Map<String,ContextState> contexts = new HashMap<>();
    private final Deque<ContextState> folders = new ArrayDeque<>();
    private final Map<String,HashMap<String,String>> choices = new HashMap<>();
    private Consumer<NativeHomeBrowseController> listener;
    private final Consumer<Result> homeReady;
    private List<Class> types = List.of();
    private Result homeResult;
    private ContextState current;
    private String site = "", requested = "home", active = "home";
    private long generation;
    private boolean attached, homeLoading;
    private boolean initialLoading;
    private boolean pendingFirstCategory;
    private final Runnable switchTask = () -> activate(requested);
    public NativeHomeBrowseController(Gateway gateway, Consumer<Result> homeReady) { this.gateway=gateway; this.homeReady=homeReady; }
    public void attach(String key, Consumer<NativeHomeBrowseController> listener) {
        this.listener=listener; attached=true;
        if (!site.equals(key)) reset(key);
        if (homeResult == null && !homeLoading) loadHome(); else {
            publish(); if(current!=null&&current.fallbackPending)loadHomeFallback();
            else if(current!=null&&!current.loaded&&!current.loading) load(current,1);
        }
    }
    public void detach() { attached=false; listener=null; handler.removeCallbacks(switchTask); cancel(); }
    private void cancel() { generation++; gateway.cancel(); homeLoading=false; for(ContextState state:contexts.values()) state.loading=false; }
    private void reset(String key) { cancel(); site=key; contexts.clear(); choices.clear(); folders.clear(); types=List.of(); homeResult=null; current=null; active=requested="home"; pendingFirstCategory=false; initialLoading=true; }
    public void reload(String key) { reset(key); if(attached) loadHome(); }
    public void seed(String key, Result result) { reset(key); acceptHome(result); }
    public List<Class> types() { return types; }
    public String activeType() { return active; }
    public ContextState state() { return current; }
    public Result homeResult() { return homeResult; }
    public String siteKey() { return site; }
    public boolean loading() { return homeLoading || current!=null&&current.loading; }
    public LoadState loadState() {
        if(!loading())return LoadState.NONE;
        if(initialLoading)return LoadState.COLD;
        return current==null||current.items.isEmpty()?LoadState.RAIL:LoadState.APPEND;
    }
    public List<Filter> filters() {
        for(Class type:types) if(type.getTypeId().equals(active)) {
            List<Filter> values=type.getFilters();
            if(values.isEmpty()&&homeResult!=null) values=homeResult.getFilters().getOrDefault(active,List.of());
            return values.stream().filter(f->!f.getValue().isEmpty()).toList();
        }
        return List.of();
    }
    public int selectedFilter(int row) {
        Filter f=filters().get(row); String selected=extension().get(f.getKey());
        for(int i=0;i<f.getValue().size();i++) if(Objects.equals(f.getValue().get(i).getV(),selected)) return i;
        return -1;
    }
    private HashMap<String,String> extension() {
        return choices.computeIfAbsent(active, key -> {
            HashMap<String,String> values=new HashMap<>();
            for(Filter f:filters()) if(f.getInit()!=null) values.put(f.getKey(),f.getInit());
            return values;
        });
    }
    public void focusCategory(String type) {
        requested=type; handler.removeCallbacks(switchTask);
        if(!type.equals(active)) handler.postDelayed(switchTask,HomeCategorySwitchScheduler.DELAY_MS);
    }
    public void flushCategory() { handler.removeCallbacks(switchTask); if(!requested.equals(active)) activate(requested); }
    public void openFirstCategory() { if(types.size()>1) { requested=types.get(1).getTypeId(); flushCategory(); } else if(homeLoading)pendingFirstCategory=true; }
    private void activate(String type) {
        if(!attached||type.equals(active)) return;
        cancel(); active=type; requested=type; folders.clear();
        current=context(type, extension(), types.stream().anyMatch(t->t.getTypeId().equals(type)&&t.isFolder()));
        if(current.fallbackPending)loadHomeFallback();else if(!current.loaded) load(current,1);else publish();
    }
    public void selectFilter(int row,int index) {
        List<Filter> filters=filters(); if(row<0||row>=filters.size())return;
        Filter filter=filters.get(row); if(index<0||index>=filter.getValue().size())return;
        Value value=filter.getValue().get(index); HashMap<String,String> extend=extension();
        if(Objects.equals(extend.get(filter.getKey()),value.getV())) { if(filter.getInit()==null)extend.remove(filter.getKey());else extend.put(filter.getKey(),filter.getInit()); }
        else extend.put(filter.getKey(),value.getV());
        ContextState next=context(active,extend,false); if(next==current){publish();return;}
        cancel();folders.clear();current=next;if(!next.loaded)load(next,1);else publish();
    }
    private ContextState context(String type,HashMap<String,String> extend,boolean folder) {
        String key=type+"|"+folder+"|"+new TreeMap<>(extend);
        return contexts.computeIfAbsent(key,k->new ContextState(type,extend,folder));
    }
    public void focused(int position) {
        if(current==null)return; current.position=Math.max(0,position);
        // The sole immersive pagination trigger; no hidden classic scroller participates.
        if(!current.type.equals("home")&&current.loaded&&!current.loading&&!current.ended&&position>=current.items.size()-4) load(current,current.page+1);
    }
    public void openFolder(Vod vod) {
        if(current==null)return; folders.push(current); cancel(); current=context(vod.getId(),current.extend,true);if(!current.loaded)load(current,1);else publish();
    }
    public boolean backFolder() { if(folders.isEmpty())return false;cancel();current=folders.pop();publish();return true; }
    private void loadHome() {
        homeLoading=true;long token=++generation;publish();
        gateway.home(result->handler.post(()->{if(!valid(token))return;homeLoading=false;acceptHome(result);homeReady.accept(result);}),
            error->handler.post(()->{if(!valid(token))return;homeLoading=false;initialLoading=false;current=context("home",new HashMap<>(),false);current.error="加载失败，请重新选择站源后重试";publish();}));
    }
    private void acceptHome(Result result) {
        if(result.hasMsg()&&result.getList().isEmpty()&&result.getTypes().isEmpty()) {
            initialLoading=false;current=context("home",new HashMap<>(),false);current.error=result.getMsg();publish();return;
        }
        homeResult=result;
        List<Class> list=new ArrayList<>();Class home=new Class();home.setTypeId("home");home.setTypeName("首页");list.add(home);
        for(Class type:result.getTypes())if(!type.isHome())list.add(type);types=List.copyOf(list);
        active=requested="home";current=context("home",new HashMap<>(),false);current.items.clear();current.items.addAll(result.getList());current.loaded=true;current.ended=true;
        current.fallbackPending=current.items.isEmpty()&&types.size()>1;
        initialLoading=current.fallbackPending;
        if(pendingFirstCategory){pendingFirstCategory=false;if(types.size()>1){openFirstCategory();return;}}
        // Same first-class page-one fallback as classic; initial fallback intentionally has no extends.
        if(current.fallbackPending&&attached)loadHomeFallback();else publish();
    }
    private void loadHomeFallback() {
        if(current==null||!current.fallbackPending||current.loading||!attached)return;
            ContextState fallback=current;long token=++generation;fallback.loading=true;publish();
            gateway.category(site,types.get(1).getTypeId(),1,new HashMap<>(), r->handler.post(()->{
                if(!valid(token)||current!=fallback)return;fallback.loading=false;initialLoading=false;
                if(r.hasMsg()&&r.getList().isEmpty())fallback.error=r.getMsg();
                else {fallback.fallbackPending=false;fallback.items.addAll(r.getList());}publish();
            }), e->handler.post(()->{if(!valid(token))return;fallback.loading=false;initialLoading=false;fallback.error="加载失败，请重试";publish();}));
    }
    private void load(ContextState state,int page) {
        if(!attached||state.loading||state.ended)return;
        state.loading=true;state.error="";long token=++generation;publish();
        gateway.category(site,state.type,page,new HashMap<>(state.extend), result->handler.post(()->{
            if(!valid(token)||state!=current)return;
            if(result.hasMsg()&&result.getList().isEmpty()){state.loading=false;initialLoading=false;state.error=result.getMsg();publish();return;}
            state.loading=false;initialLoading=false;state.loaded=true;state.page=page;state.pageCount=result.getPageCount();
            Set<String> seen=new HashSet<>();for(Vod v:state.items)seen.add(identity(v));
            for(Vod v:result.getList())if(seen.add(identity(v)))state.items.add(v);
            state.ended=result.getList().isEmpty() || state.pageCount>0&&page>=state.pageCount;publish();
        }),error->handler.post(()->{if(!valid(token)||state!=current)return;state.loading=false;initialLoading=false;state.error="加载失败，请重试";publish();}));
    }
    public static String identity(Vod vod) { return (vod.getId().isEmpty()?vod.getName():vod.getId())+"|"+vod.getAction(); }
    private boolean valid(long token) { return attached&&token==generation; }
    private void publish() { if(attached&&listener!=null)listener.accept(this); }
}
