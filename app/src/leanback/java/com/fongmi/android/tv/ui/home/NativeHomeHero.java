package com.fongmi.android.tv.ui.home;


import android.os.Handler;
import android.os.Looper;
import com.fongmi.android.tv.bean.*;
import com.fongmi.android.tv.service.TmdbService;
import com.fongmi.android.tv.ui.helper.TmdbMatcher;
import com.fongmi.android.tv.ui.helper.TmdbSourceAdapter;
import com.fongmi.android.tv.ui.helper.TmdbSourcePayloadParser;
import com.fongmi.android.tv.ui.helper.TmdbBundle;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.utils.TmdbImageSelector;
import com.fongmi.android.tv.utils.TmdbLanguagePolicy;
import com.fongmi.android.tv.ui.helper.TmdbMatchPolicy;
import com.fongmi.android.tv.utils.Task;
import com.google.gson.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/** Optional focused-film enhancement using the existing TMDB service and persisted caches. */
public final class NativeHomeHero {
    public static final long FOCUS_SETTLE_MS=180;
    public record Info(String metadata,String overview,String backdrop,String logo,String poster) {
        public Info(String metadata,String overview,String backdrop,String logo) { this(metadata,overview,backdrop,logo,""); }
    }
    public interface Backend {
        Info load(Site site,Vod vod,TmdbConfig config) throws Exception;
        default Info loadKnown(Site site,Vod vod,TmdbConfig config) throws Exception { return null; }
    }
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Map<String,CompletableFuture<Info>> inFlight=new HashMap<>();
    private final Backend backend;
    private record Prepared(Info info,long expires,String binding) {}
    // Short-lived presentation memo only; existing TMDB and Glide own persistent caches.
    private final Map<String,Prepared> prepared=Collections.synchronizedMap(new LinkedHashMap<>(24,.75f,true));
    private final Set<String> visited=new LinkedHashSet<>();
    private final java.util.concurrent.ConcurrentHashMap<String,CompletableFuture<Info>> loads=new java.util.concurrent.ConcurrentHashMap<>();
    private Runnable dwell;
    private Runnable pendingFocus;
    private Runnable availability=()->{};
    private long generation;
    public NativeHomeHero() { this(new ExistingTmdb()); }
    public NativeHomeHero(Backend backend) { this.backend=backend; }
    public void setAvailabilityListener(Runnable listener){availability=listener==null?()->{}:listener;}
    public void focus(Site site,Vod vod,Consumer<Info> consumer) {
        focus(site,vod,consumer,()->{});
    }
    public void focus(Site site,Vod vod,Consumer<Info> consumer,Runnable settled) {
        focus(site,vod,consumer,settled,()->{},false);
    }
    public void focus(Site site,Vod vod,Consumer<Info> consumer,Runnable settled,Runnable unavailable,boolean immediate) {
        clearFocus();long token=generation;
        TmdbConfig initial=TmdbConfig.effectiveCurrent();
        String focusKey=key(site,vod,initial);
        dwell=()->{
            if(token!=generation)return;
            settled.run();
            if(vod==null||vod.isAction()||vod.isFolder()||site.isIndex()){unavailable.run();return;}
            TmdbConfig config=TmdbConfig.effectiveCurrent();
            if((!config.isReady()&&vod.getTmdb()==null)||!config.isSiteEnabled(site.getKey(),site.getName())){NativeHomeBrowseImages.trace("disabled-or-no-credentials",token);unavailable.run();return;}
            String scope=scope(config);String key=key(site,vod,config);
            CompletableFuture<Info> future=inFlight.get(key);
            if(future==null) {
                if(inFlight.size()+(preparing?1:0)>=2){pendingFocus=dwell;return;}
                future=new CompletableFuture<>();inFlight.put(key,future);CompletableFuture<Info> pending=future;
                Task.executor().submit(()->{try{pending.complete(loadPrepared(key,site,vod,config,false));}catch(Exception e){NativeHomeBrowseImages.trace("metadata-request-failed:"+e.getClass().getSimpleName(),token);pending.complete(null);}});
                future.whenComplete((info,error)->handler.post(()->{
                    inFlight.remove(key,pending);Runnable latest=pendingFocus;pendingFocus=null;if(latest!=null)latest.run();handler.post(availability);
                }));
            }
            CompletableFuture<Info> subscribed=future;
            future.whenComplete((info,error)->handler.post(()->{
                inFlight.remove(key,subscribed);
                if(token!=generation||!scope.equals(scope(TmdbConfig.effectiveCurrent())))NativeHomeBrowseImages.trace("metadata-stale",token);
                else if(info==null){NativeHomeBrowseImages.trace("no-match-or-no-detail",token);unavailable.run();}
                else {remember(key);NativeHomeBrowseImages.trace("metadata-ready backdrop="+!info.backdrop().isEmpty()+" logo="+!info.logo().isEmpty(),token);consumer.accept(info);}
            }));
        };
        if(immediate||visited.contains(focusKey))dwell.run();else handler.postDelayed(dwell,FOCUS_SETTLE_MS);
    }
    private static String key(Site site,Vod vod,TmdbConfig config) {
        return scope(config)+"|"+site.getKey()+"|"+(vod==null?"":NativeHomeBrowseController.identity(vod)+"|"+vod.getName()+"|"+vod.getYear()+"|"+vod.getRemarks()+"|"+vod.getTmdb());
    }
    // Called off main, including manual-binding validation (AppCache is disk-backed).
    private Info loadPrepared(String key,Site site,Vod vod,TmdbConfig config,boolean knownOnly) throws Exception {
        String language=TmdbLanguagePolicy.requestLanguage(config, Setting.getLanguage());
        TmdbItem manual=Setting.getTmdbMatchCache().findManual(site.getKey(),vod.getId(),vod.getName(),language);
        String binding=manual==null?"":manual.getMediaType()+":"+manual.getTmdbId();
        Prepared hit=prepared.get(key);long now=android.os.SystemClock.uptimeMillis();
        if(hit!=null&&hit.expires()>now&&hit.binding().equals(binding)){NativeHomeBrowseImages.trace("metadata-memo-hit",generation);return hit.info();}
        Prepared miss=knownOnly?prepared.get(key+"|known-miss"):null;
        if(miss!=null&&miss.expires()>now&&miss.binding().equals(binding))return null;
        String workKey=key+"|"+binding;CompletableFuture<Info> own=new CompletableFuture<>();
        CompletableFuture<Info> shared=loads.putIfAbsent(workKey,own);
        if(shared!=null){Info result=shared.join();if(result!=null||knownOnly)return result;loads.remove(workKey,shared);return loadPrepared(key,site,vod,config,false);}
        Info info;
        try {
            info=knownOnly?backend.loadKnown(site,vod,config):backend.load(site,vod,config);
            memo(knownOnly&&info==null?key+"|known-miss":key,new Prepared(info,now+(info==null?15000:120000),binding));
            own.complete(info);
        }
        catch(Exception error){memo(knownOnly?key+"|known-miss":key,new Prepared(null,now+15000,binding));own.complete(null);throw error;}
        finally {loads.remove(workKey,own);}
        return info;
    }
    private void memo(String key,Prepared value){synchronized(prepared){prepared.put(key,value);while(prepared.size()>24)prepared.remove(prepared.keySet().iterator().next());}}
    private boolean preparing;
    private void remember(String key){visited.remove(key);visited.add(key);while(visited.size()>24)visited.remove(visited.iterator().next());}
    public void prepareKnown(Site site,Vod vod,Consumer<Info> consumer) {
        if(preparing||!inFlight.isEmpty()||vod==null||vod.isAction()||vod.isFolder()||site.isIndex())return;
        TmdbConfig config=TmdbConfig.effectiveCurrent();if(!config.isSiteEnabled(site.getKey(),site.getName()))return;
        long token=generation;String scope=scope(config),key=key(site,vod,config);preparing=true;
        Task.executor().submit(()->{
            Info info=null;try{info=loadPrepared(key,site,vod,config,true);}catch(Exception ignored){}
            Info result=info;handler.post(()->{preparing=false;Runnable latest=pendingFocus;pendingFocus=null;if(latest!=null)latest.run();if(token==generation&&scope.equals(scope(TmdbConfig.effectiveCurrent()))&&result!=null){remember(key);consumer.accept(result);}});
        });
    }
    /** Window subscription survives focus moves; its owner validates category/source generation. */
    public boolean prepareWindow(Site site,Vod vod,Consumer<Info> consumer){
        if(preparing||!inFlight.isEmpty()||pendingFocus!=null)return false;
        TmdbConfig config=TmdbConfig.effectiveCurrent();
        if(vod==null||vod.isAction()||vod.isFolder()||site.isIndex()||(!config.isReady()&&vod.getTmdb()==null)||!config.isSiteEnabled(site.getKey(),site.getName())){handler.post(()->consumer.accept(null));return true;}
        String expectedScope=scope(config),key=key(site,vod,config);preparing=true;
        Task.executor().submit(()->{
            Info info=null;try{info=loadPrepared(key,site,vod,config,false);}catch(Exception error){NativeHomeBrowseImages.trace("window-metadata-failed:"+error.getClass().getSimpleName(),generation);}
            Info result=info;handler.post(()->{
                preparing=false;Runnable latest=pendingFocus;pendingFocus=null;if(latest!=null)latest.run();
                if(expectedScope.equals(scope(TmdbConfig.effectiveCurrent()))&&result!=null)remember(key);
                consumer.accept(expectedScope.equals(scope(TmdbConfig.effectiveCurrent()))?result:null);availability.run();
            });
        });return true;
    }
    public void clearFocus() { generation++;if(dwell!=null)handler.removeCallbacks(dwell);dwell=null;pendingFocus=null; }
    static String scope(TmdbConfig c) {
        try {
            // Configured scope, not a mutable automatically selected transport route. Never log credentials.
            byte[] digest=java.security.MessageDigest.getInstance("SHA-256").digest((c.toJson()+"\u0000"+c.getApiKey()+"\u0000"+c.getAccessToken()).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return c.getCredentialOrigin()+"|"+c.getCredentialSubscriptionKey()+"|"+c.getCredentialScopeEpoch()+"|"+android.util.Base64.encodeToString(digest,android.util.Base64.NO_WRAP);
        } catch(java.security.NoSuchAlgorithmException impossible) { throw new IllegalStateException(impossible); }
    }
    static final class ExistingTmdb implements Backend {
        private final TmdbService service;
        ExistingTmdb() { this(new TmdbService()); }
        ExistingTmdb(TmdbService service) { this.service=service; }
        public Info load(Site site,Vod vod,TmdbConfig config) throws Exception {
            return load(site,vod,config,true);
        }
        @Override public Info loadKnown(Site site,Vod vod,TmdbConfig config) throws Exception { return load(site,vod,config,false); }
        private Info load(Site site,Vod vod,TmdbConfig config,boolean allowSearch) throws Exception {
            String language=TmdbLanguagePolicy.requestLanguage(config, Setting.getLanguage());
            TmdbMatchCache cache=Setting.getTmdbMatchCache();
            TmdbItem manual=cache.findManual(site.getKey(),vod.getId(),vod.getName(),language);
            TmdbSourcePayload payload=TmdbSourcePayloadParser.parse(vod.getTmdb());
            TmdbBundle embedded=payload!=null&&TmdbLanguagePolicy.isDisplayLanguageCompatible(language,payload.getLanguage())?TmdbSourceAdapter.toBundle(payload,vod,config):null;
            TmdbItem item=manual!=null?manual:embedded!=null?embedded.item():cache.find(site.getKey(),vod.getId(),vod.getName(),language);
            NativeHome10Diagnostics.identity("SOURCE_WALLPIC_UNAVAILABLE",site,vod,null); // Vod has no wallPic field; raw payload is unknown.
            if(!config.isReady()&&(manual!=null||embedded==null))return null;
            JsonObject detail=manual==null&&embedded!=null?embedded.detail():item==null?null:service.detail(item,config,false);
            // Embedded station metadata often has identity/text but no image collection.
            // Enrich that identity through the SAME cached service, without another matcher.
            if(manual==null&&embedded!=null&&item!=null&&config.isReady()&&missingArtwork(detail)) {
                try { JsonObject enriched=service.detail(item,config,false);if(enriched!=null)detail=enriched; }
                catch(Exception error) { NativeHomeBrowseImages.trace("embedded-art-request-failed:"+error.getClass().getSimpleName(),0); }
            }
            if(manual==null&&detail!=null&&TmdbMatchPolicy.isUnwantedSplitSeasonVariant(vod.getName()+" "+vod.getRemarks(),detail)) {item=null;detail=null;}
            if(item==null) {
                if(!allowSearch)return null;
                if(!config.isReady())return null;
                item=new TmdbMatcher(service,config).searchAndMatch(vod.getName(),vod);
                if(item==null){NativeHome10Diagnostics.identity("TMDB_MATCH_MISSING",site,vod,null);return null;}
                detail=service.detail(item,config,false);
                if(TmdbMatchPolicy.isUnwantedSplitSeasonVariant(vod.getName()+" "+vod.getRemarks(),detail))return null;
                if(!scope(config).equals(scope(TmdbConfig.effectiveCurrent())))return null;
                synchronized(Setting.class) { TmdbMatchCache latest=Setting.getTmdbMatchCache();latest.put(site.getKey(),vod.getId(),vod.getName(),item,language);Setting.putTmdbMatchCache(latest); }
            }
            if(detail==null)return null;
            NativeHome10Diagnostics.identity("TMDB_MATCHED",site,vod,item);
            TmdbItem latestManual=Setting.getTmdbMatchCache().findManual(site.getKey(),vod.getId(),vod.getName(),language);
            if(latestManual!=null&&(latestManual.getTmdbId()!=item.getTmdbId()||!Objects.equals(latestManual.getMediaType(),item.getMediaType())))return null;
            List<String> backgrounds=TmdbImageSelector.backdrops(detail,bounded(config.getBackdropBase()),1);
            NativeHome10Diagnostics.identity(backgrounds.isEmpty()?"TMDB_BACKDROP_EMPTY":"BACKDROP_CANDIDATE_FOUND",site,vod,item);
            if(!backgrounds.isEmpty())NativeHome10Diagnostics.event("BACKDROP_CANDIDATE_FOUND","tmdb="+item.getMediaType()+"/"+item.getTmdbId()+" image="+Integer.toHexString(bounded(backgrounds.get(0)).hashCode()));
            List<String> metadata=new ArrayList<>();String date=text(detail,"release_date");if(date.isEmpty())date=text(detail,"first_air_date");if(date.length()>=4)metadata.add(date.substring(0,4));
            if(detail.has("genres")&&detail.get("genres").isJsonArray())for(JsonElement e:detail.getAsJsonArray("genres"))if(e.isJsonObject())metadata.add(text(e.getAsJsonObject(),"name"));
            if(detail.has("production_countries")&&detail.get("production_countries").isJsonArray())for(JsonElement e:detail.getAsJsonArray("production_countries"))if(e.isJsonObject())metadata.add(text(e.getAsJsonObject(),"name"));
            String score=text(detail,"vote_average");if(!score.isEmpty())metadata.add("★ "+score);
            if(!vod.getRemarks().isEmpty())metadata.add(vod.getRemarks());
            String logo=text(detail,"logo_path").isEmpty()?"":service.image(bounded(config.getImageBase()),text(detail,"logo_path"));
            if(detail.has("images")&&detail.get("images").isJsonObject()) {
                JsonObject images=detail.getAsJsonObject("images");
                if(images.has("logos")&&images.get("logos").isJsonArray())for(JsonElement e:images.getAsJsonArray("logos")) {
                    if(!e.isJsonObject())continue;JsonObject image=e.getAsJsonObject();String lang=text(image,"iso_639_1");
                    if(!text(image,"file_path").isEmpty()&&(lang.isEmpty()||language.startsWith(lang))){logo=service.image(bounded(config.getImageBase()),text(image,"file_path"));break;}
                }
            }
            return new Info(String.join(" · ",metadata.stream().filter(s->!s.isEmpty()).toList()),service.translatedOverview(detail,config),backgrounds.isEmpty()?"":bounded(backgrounds.get(0)),bounded(logo),bounded(TmdbImageSelector.poster(detail,config.getImageBase(),"")));
        }
        private static boolean missingArtwork(JsonObject detail) {
            if(detail==null)return true;
            return text(detail,"backdrop_path").isEmpty() || !detail.has("images");
        }
        static String bounded(String url) {
            var matcher=java.util.regex.Pattern.compile("/(original|w([0-9]+)|h([0-9]+))(?=/|$)").matcher(url);
            StringBuffer result=new StringBuffer();
            while(matcher.find()) {
                boolean large=matcher.group(1).equals("original");
                try {large|=matcher.group(2)!=null&&Long.parseLong(matcher.group(2))>1280 || matcher.group(3)!=null&&Long.parseLong(matcher.group(3))>720;}catch(NumberFormatException ignored){large=true;}
                matcher.appendReplacement(result,java.util.regex.Matcher.quoteReplacement(large?"/w1280":matcher.group()));
            }
            matcher.appendTail(result);return result.toString();
        }
        private static String text(JsonObject object,String key) { JsonElement value=object.get(key);return value==null||value.isJsonNull()||!value.isJsonPrimitive()?"":value.getAsString(); }
    }
}
