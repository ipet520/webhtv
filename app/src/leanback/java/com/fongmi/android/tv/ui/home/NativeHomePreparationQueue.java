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
    public static final long METADATA_INTERVAL_MS=750;
    public record Candidate(Vod vod,int priority) {}
    public interface Metadata { boolean start(Vod vod,Consumer<NativeHomeHero.Info> completed); }
    public interface Images { void start(String url,Consumer<Boolean> completed); default void logo(String url,Consumer<Boolean> completed){completed.accept(false);} }
    private enum Stage { QUEUED, METADATA, IMAGE, READY, FAILED }
    private static final class Entry {
        final String key;final Vod vod;int priority;Stage stage=Stage.QUEUED;String url="";long readyAt,retryAt;
        String logo="";boolean logoKnown,logoQueued,logoPrepared;long logoRetryAt;
        Entry(String key,Vod vod){this.key=key;this.vod=vod;}
    }
    private record Ready(String url,long at,String logo,long logoAt) { Ready(String url,long at){this(url,at,"",0);} }
    private final Handler handler=new Handler(Looper.getMainLooper());
    private final Metadata metadata;
    private final Images images;
    private final boolean logos;
    private final Map<String,Entry> window=new LinkedHashMap<>();
    private final LinkedHashMap<String,Ready> ready=new LinkedHashMap<>(24,.75f,true);
    private Object context;
    private String scope="";
    private long generation,nextMetadataAt;
    private boolean active=true,metadataBusy,imageBusy,retryPosted;
    private int focuses,preparedFocuses;
    private final Runnable retry=()->{retryPosted=false;kick();};
    public NativeHomePreparationQueue(Metadata metadata,Images images){this(metadata,images,false);}
    public NativeHomePreparationQueue(Metadata metadata,Images images,boolean logos){this.metadata=metadata;this.images=images;this.logos=logos;}
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
                if(hit!=null&&!hit.url().isEmpty()&&now()-hit.at()<120000){entry.url=hit.url();entry.readyAt=hit.at();entry.stage=Stage.READY;}
                window.put(key,entry);trace("queued",entry);
            }
            entry.priority=candidate.priority();
            if(logos&&entry.logoKnown&&!entry.logo.isEmpty()&&!entry.logoPrepared&&now()>=entry.logoRetryAt)entry.logoQueued=true;
            if(entry.stage==Stage.FAILED&&now()>=entry.retryAt)entry.stage=Stage.QUEUED;
            if(entry.stage==Stage.READY&&now()-entry.readyAt>=120000)entry.stage=Stage.QUEUED;
        }
        // Reprioritize existing entries, do not recreate the queue or cancel useful active work.
        window.keySet().removeIf(key->!desired.contains(key));kick();
    }
    public void currentMetadata(Vod vod,NativeHomeHero.Info info){
        if(vod==null)return;Entry entry=window.get(key(vod));if(entry==null)return;
        rememberLogo(entry,info);
        String url=info==null?"":info.backdrop();
        if(url.isEmpty()){fail(entry);kick();return;}
        if((entry.stage==Stage.READY||entry.stage==Stage.IMAGE)&&entry.url.equals(url)){kick();return;}
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
            Entry entry=best(Stage.METADATA),logo=bestLogo();
            if(logo!=null&&(entry==null||logo.priority<entry.priority))startLogo(logo);
            else if(entry!=null){
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
                        Ready previous=ready.get(entry.key);ready.put(entry.key,new Ready(requestedUrl,completedAt,previous==null?"":previous.logo(),previous==null?0:previous.logoAt()));
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
        long token=generation,previousStart=nextMetadataAt;metadataBusy=true;nextMetadataAt=now()+METADATA_INTERVAL_MS;
        boolean started=metadata.start(entry.vod,info->{
            metadataBusy=false;
            if(token==generation&&window.get(entry.key)==entry){
                rememberLogo(entry,info);
                if(info==null||info.backdrop().isEmpty()){if(entry.stage!=Stage.IMAGE&&entry.stage!=Stage.READY)fail(entry);}
                else if(entry.stage!=Stage.IMAGE&&entry.stage!=Stage.READY){entry.url=info.backdrop();entry.stage=Stage.METADATA;trace("metadata-ready",entry);}
            }
            kick();
        });
        if(started){trace("metadata-start",entry);}
        else {metadataBusy=false;nextMetadataAt=previousStart;} // Hero availability callback wakes us; no busy polling.
    }
    /** Only metadata confirmed in the active window is used for immediate title binding. */
    public String logo(Vod vod){Entry e=vod==null?null:window.get(key(vod));return logos&&e!=null&&e.logoKnown?e.logo:null;}
    private void rememberLogo(Entry e,NativeHomeHero.Info info){
        if(!logos)return;String url=info==null||info.logo()==null?"":info.logo();
        if(!e.logoKnown||!url.equals(e.logo)){e.logo=url;e.logoPrepared=false;e.logoRetryAt=0;}
        e.logoKnown=true;Ready hit=ready.get(e.key);
        if(hit!=null&&hit.logo().equals(url)&&now()-hit.logoAt()<120000)e.logoPrepared=true;
        e.logoQueued=!url.isEmpty()&&!e.logoPrepared&&now()>=e.logoRetryAt;
        trace("logo-url-known present="+!url.isEmpty(),e);
    }
    private Entry bestLogo(){Entry best=null;if(logos)for(Entry e:window.values())if(e.logoQueued&&(best==null||e.priority<best.priority))best=e;return best;}
    private void startLogo(Entry e){
        Ready hit=ready.get(e.key);
        if(hit!=null&&hit.logo().equals(e.logo)&&now()-hit.logoAt()<120000){e.logoQueued=false;e.logoPrepared=true;trace("logo-ready-reused",e);kick();return;}
        String requested=e.logo;e.logoQueued=false;imageBusy=true;trace("logo-image-start",e);
        images.logo(requested,success->{
            imageBusy=false;
            if(success){
                Entry current=window.get(e.key);
                if(current==null||!current.logoKnown||requested.equals(current.logo)){
                    Ready previous=ready.get(e.key);ready.put(e.key,new Ready(previous==null?"":previous.url(),previous==null?0:previous.at(),requested,now()));
                    while(ready.size()>24)ready.remove(ready.keySet().iterator().next());
                }
                if(requested.equals(e.logo)){e.logoPrepared=true;e.logoQueued=false;}trace("logo-image-ready",e);
            }else if(requested.equals(e.logo)){e.logoPrepared=false;e.logoQueued=false;e.logoRetryAt=now()+30000;trace("logo-image-failed",e);}
            kick();
        });
    }
    private Entry best(Stage stage){Entry best=null;for(Entry entry:window.values())if(entry.stage==stage&&(best==null||entry.priority<best.priority))best=entry;return best;}
    private void fail(Entry entry){entry.stage=Stage.FAILED;entry.retryAt=now()+30000;trace("failed",entry);}
    private void trace(String event,Entry entry){NativeHomeBrowseImages.trace("window-"+event+" id="+Integer.toHexString(entry.key.hashCode()),generation);}
    private static long now(){return SystemClock.uptimeMillis();}
    public int queuedCount(){return window.size();}
    public int focusCount(){return focuses;}
    public int preparedFocusCount(){return preparedFocuses;}
}
