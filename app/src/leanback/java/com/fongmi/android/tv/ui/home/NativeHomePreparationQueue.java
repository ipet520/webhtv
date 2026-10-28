package com.fongmi.android.tv.ui.home;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import com.fongmi.android.tv.bean.Vod;
import java.util.*;
import java.util.function.Consumer;

/** Presentation subscriptions only. Existing Hero/TMDB and Glide own all data/resources. */
public final class NativeHomePreparationQueue {
    public static final int MAX_WINDOW=10;
    public record Candidate(Vod vod,int priority) {}
    public interface Metadata { boolean start(Vod vod,Consumer<NativeHomeHero.Info> completed); }
    public interface Images { void start(String url,Consumer<Boolean> completed); }
    private enum Stage { QUEUED, METADATA, IMAGE, READY, FAILED }
    private static final class Entry {
        final String key;final Vod vod;int priority;Stage stage=Stage.QUEUED;String url="";long readyAt,retryAt;
        Entry(String key,Vod vod){this.key=key;this.vod=vod;}
    }
    private record Ready(String url,long at) {}
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Metadata metadata;
    private final Images images;
    private final Map<String,Entry> window=new LinkedHashMap<>();
    private final LinkedHashMap<String,Ready> ready=new LinkedHashMap<>(24,.75f,true);
    private Object context;
    private String scope="";
    private long generation,nextMetadataAt;
    private boolean active=true,metadataBusy,imageBusy,retryPosted;
    private int focuses,preparedFocuses;
    private final Runnable retry=()->{retryPosted=false;kick();};
    public NativeHomePreparationQueue(Metadata metadata,Images images){this.metadata=metadata;this.images=images;}
    private String key(Vod vod){return scope+"|"+NativeHomeBrowseController.identity(vod)+"|"+vod.getName()+"|"+vod.getYear()+"|"+vod.getRemarks();}
    public void window(Object context,String scope,List<Candidate> candidates){
        if(this.context!=context||!this.scope.equals(scope)){
            this.context=context;this.scope=scope;generation++;window.clear();handler.removeCallbacks(retry);retryPosted=false;
        }
        Set<String> desired=new HashSet<>();int count=0;
        for(Candidate candidate:candidates){
            Vod vod=candidate.vod();if(vod==null||vod.isAction()||vod.isFolder())continue;
            String key=key(vod);if(!desired.add(key))continue;if(count++>=MAX_WINDOW){desired.remove(key);break;}
            Entry entry=window.get(key);
            if(entry==null){
                entry=new Entry(key,vod);Ready hit=ready.get(key);
                if(hit!=null&&now()-hit.at()<120000){entry.url=hit.url();entry.readyAt=hit.at();entry.stage=Stage.READY;}
                window.put(key,entry);trace("queued",entry);
            }
            entry.priority=candidate.priority();
            if(entry.stage==Stage.FAILED&&now()>=entry.retryAt)entry.stage=Stage.QUEUED;
            if(entry.stage==Stage.READY&&now()-entry.readyAt>=120000)entry.stage=Stage.QUEUED;
        }
        // Reprioritize existing entries, do not recreate the queue or cancel useful active work.
        window.keySet().removeIf(key->!desired.contains(key));kick();
    }
    public void currentMetadata(Vod vod,NativeHomeHero.Info info){
        if(vod==null)return;Entry entry=window.get(key(vod));if(entry==null)return;
        String url=info==null?"":info.backdrop();
        if(url.isEmpty()){fail(entry);return;}
        if((entry.stage==Stage.READY||entry.stage==Stage.IMAGE)&&entry.url.equals(url))return;
        entry.url=url;entry.stage=Stage.METADATA;trace("metadata-ready",entry);kick();
    }
    public boolean focus(Vod vod){
        Entry entry=vod==null?null:window.get(key(vod));boolean prepared=entry!=null&&entry.stage==Stage.READY&&now()-entry.readyAt<120000;
        focuses++;if(prepared)preparedFocuses++;
        NativeHomeBrowseImages.trace("window-focus id="+(entry==null?"unlisted":Integer.toHexString(entry.key.hashCode()))+" prepared="+prepared+" lead-ms="+(prepared?now()-entry.readyAt:-1)+" hits="+preparedFocuses+"/"+focuses,generation);
        return prepared;
    }
    public void pause(){active=false;handler.removeCallbacks(retry);retryPosted=false;}
    public void resume(){active=true;kick();}
    public void clearWindow(){context=null;generation++;window.clear();handler.removeCallbacks(retry);retryPosted=false;}
    public void kick(){
        if(!active||context==null)return;
        if(!imageBusy){
            Entry entry=best(Stage.METADATA);
            if(entry!=null){
                // A category may return while the same identity's previous image is still
                // decoding. Reuse its completion only after current metadata confirms URL.
                Ready hit=ready.get(entry.key);
                if(hit!=null&&hit.url().equals(entry.url)&&now()-hit.at()<120000){
                    entry.stage=Stage.READY;entry.readyAt=hit.at();trace("image-ready-reused",entry);kick();return;
                }
                String requestedUrl=entry.url;
                imageBusy=true;entry.stage=Stage.IMAGE;trace("image-start",entry);
                images.start(requestedUrl,success->{
                    imageBusy=false;
                    if(success){
                        long completedAt=now();
                        if(entry.stage==Stage.IMAGE&&entry.url.equals(requestedUrl)){entry.stage=Stage.READY;entry.readyAt=completedAt;}
                        ready.put(entry.key,new Ready(requestedUrl,completedAt));
                        while(ready.size()>24)ready.remove(ready.keySet().iterator().next());trace("image-ready",entry);
                    } else if(entry.stage==Stage.IMAGE&&entry.url.equals(requestedUrl))fail(entry);
                    kick();
                });
            }
        }
        if(metadataBusy)return;
        Entry entry=best(Stage.QUEUED);if(entry==null)return;
        long wait=nextMetadataAt-now();
        if(wait>0){if(!retryPosted){retryPosted=true;handler.postDelayed(retry,wait);}return;}
        long token=generation,previousStart=nextMetadataAt;metadataBusy=true;nextMetadataAt=now()+750;
        boolean started=metadata.start(entry.vod,info->{
            metadataBusy=false;
            if(token==generation&&window.get(entry.key)==entry){
                if(info==null||info.backdrop().isEmpty()){if(entry.stage!=Stage.IMAGE&&entry.stage!=Stage.READY)fail(entry);}
                else if(entry.stage!=Stage.IMAGE&&entry.stage!=Stage.READY){entry.url=info.backdrop();entry.stage=Stage.METADATA;trace("metadata-ready",entry);}
            }
            kick();
        });
        if(started){trace("metadata-start",entry);}
        else {metadataBusy=false;nextMetadataAt=previousStart;} // Hero availability callback wakes us; no busy polling.
    }
    private Entry best(Stage stage){Entry best=null;for(Entry entry:window.values())if(entry.stage==stage&&(best==null||entry.priority<best.priority))best=entry;return best;}
    private void fail(Entry entry){entry.stage=Stage.FAILED;entry.retryAt=now()+30000;trace("failed",entry);}
    private void trace(String event,Entry entry){NativeHomeBrowseImages.trace("window-"+event+" id="+Integer.toHexString(entry.key.hashCode()),generation);}
    private static long now(){return SystemClock.uptimeMillis();}
    public int queuedCount(){return window.size();}
    public int focusCount(){return focuses;}
    public int preparedFocusCount(){return preparedFocuses;}
}
