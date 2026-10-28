package com.fongmi.android.tv.ui.home;

import android.util.Log;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.bean.Vod;
import com.fongmi.android.tv.bean.TmdbItem;
import java.util.concurrent.atomic.AtomicInteger;

/** Opt-in, bounded diagnostics. Never log URLs, credentials or raw configuration. */
public final class NativeHome10Diagnostics {
    private static final AtomicInteger budget=new AtomicInteger(400);
    private NativeHome10Diagnostics(){}
    public static void event(String stage,String detail){
        if(BuildConfig.DEBUG&&Log.isLoggable("NativeHomeArt",Log.DEBUG)&&budget.getAndDecrement()>0)
            Log.d("NativeHomeArt",stage+" t="+android.os.SystemClock.uptimeMillis()+" "+detail);
    }
    static void identity(String stage,Site site,Vod vod,TmdbItem item){
        event(stage,"source="+Integer.toHexString((site.getKey()+"|"+vod.getId()).hashCode())
            +" title="+vod.getName().replace('\n',' ').replace('\r',' ')
            +(item==null?"":" tmdb="+item.getMediaType()+"/"+item.getTmdbId()));
    }
}
