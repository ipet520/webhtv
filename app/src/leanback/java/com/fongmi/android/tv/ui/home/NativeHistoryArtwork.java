package com.fongmi.android.tv.ui.home;


import com.fongmi.android.tv.setting.Setting;

import com.fongmi.android.tv.bean.*;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.utils.TmdbLanguagePolicy;
import com.google.gson.JsonObject;
import java.util.*;
import java.util.function.Consumer;

/** Presentation-only bridge. Known identities only: never searches/matches the history collection. */
public final class NativeHistoryArtwork {
    private final NativeHomeHero hero;
    private final Map<Vod,Site> sites=new IdentityHashMap<>();
    private final Map<Vod,String> walls=new IdentityHashMap<>();
    public NativeHistoryArtwork(){
        this(new NativeHomeHero.ExistingTmdb());
    }
    NativeHistoryArtwork(NativeHomeHero.Backend known){
        hero=new NativeHomeHero(new NativeHomeHero.Backend(){
            public NativeHomeHero.Info load(Site site,Vod vod,TmdbConfig config)throws Exception{return known.loadKnown(site,vod,config);}
            public NativeHomeHero.Info loadKnown(Site site,Vod vod,TmdbConfig config)throws Exception{return known.loadKnown(site,vod,config);}
        });
    }
    public void reset(){hero.clearFocus();sites.clear();walls.clear();}
    public String scope(){return NativeHomeHero.scope(TmdbConfig.effectiveCurrent());}
    public Vod item(History history){
        JsonObject data=new JsonObject();data.addProperty("vod_id",history.getVodId());data.addProperty("vod_name",history.getVodName());
        data.addProperty("vod_pic",history.getVodPic());data.addProperty("vod_year",history.getYear());data.addProperty("vod_remarks",history.getVodRemarks());
        Vod vod=Vod.objectFrom(data.toString());
        if(history.getTmdbId()>0&&Set.of("movie","tv").contains(history.getMediaType())){
            TmdbSourcePayload payload=new TmdbSourcePayload();payload.setSchema(1);payload.setTmdbId(history.getTmdbId());payload.setMediaType(history.getMediaType());
            payload.setLanguage(TmdbLanguagePolicy.requestLanguage(TmdbConfig.effectiveCurrent(), Setting.getLanguage()));
            JsonObject detail=new JsonObject();detail.addProperty("id",history.getTmdbId());detail.addProperty("title",history.getVodName());detail.addProperty("name",history.getVodName());
            payload.setDetailJson(detail.toString());vod.setTmdb(payload);
        }
        // A record from another config must not consult today's same-named source binding.
        String site=history.getCid()==VodConfig.getCid()?history.getSiteKey():"history-config:"+history.getCid()+":"+history.getSiteKey();
        sites.put(vod,Site.get(site,history.getSiteName()));
        if(!history.getWallPic().isEmpty()&&!history.getWallPic().equals(history.getVodPic()))walls.put(vod,history.getWallPic());
        return vod;
    }
    public void focus(Vod vod,Consumer<NativeHomeHero.Info> ready,Runnable settled,Runnable unavailable,boolean immediate){
        Site site=sites.get(vod);if(site==null){unavailable.run();return;}hero.focus(site,vod,ready,settled,unavailable,immediate);
    }
    public boolean prepare(Vod vod,Consumer<NativeHomeHero.Info> ready){if(walls.containsKey(vod)){ready.accept(new NativeHomeHero.Info("","",walls.get(vod),""));return true;}Site site=sites.get(vod);if(site==null){ready.accept(null);return true;}return hero.prepareWindow(site,vod,ready);}
    public void pause(){hero.clearFocus();}
    public void available(Runnable callback){hero.setAvailabilityListener(callback);}
}
