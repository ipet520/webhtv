package com.fongmi.android.tv.ui.home;

import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.fongmi.android.tv.BuildConfig;
import com.fongmi.android.tv.utils.ImgUtil;
import com.fongmi.android.tv.ui.custom.CustomWallView;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** View subscriptions only: existing Glide owns requests, decoding and caching. */
public final class NativeHomeBrowseImages {
    interface Loader {
        void load(ImageView target,String url,int width,int height,boolean crop,Consumer<Drawable> ready,Runnable failed);
        void clear(ImageView target);
        default void background(ImageView target,String url,int width,int height,Consumer<Drawable> ready,Runnable failed){load(target,url,width,height,true,ready,failed);}
        default Runnable releasePrevious(ImageView target){return ()->{};}
        default void prefetch(ImageView target,String url,int width,int height){}
        default void prefetch(ImageView target,String url,int width,int height,Consumer<Boolean> completed){prefetch(target,url,width,height);completed.accept(false);}
        default void cancelBackground(ImageView target){}
        default void cancelPrefetch(ImageView target){}
        default void clearBackground(ImageView target){clear(target);}
    }
    private static final class GlideLoader implements Loader {
        private static final class Slot {com.bumptech.glide.request.target.CustomTarget<Drawable> current,previous,pending;}
        private final Map<ImageView,Slot> slots=new IdentityHashMap<>();
        private com.bumptech.glide.request.target.Target<Drawable> warm;
        private ImageView warmOwner;
        private String warmKey="";
        private long warmGeneration;
        private Runnable warmCancel;
        private final android.os.Handler main=new android.os.Handler(android.os.Looper.getMainLooper());
        private void cancelWarm(ImageView view){
            warmGeneration++;Runnable cancel=warmCancel;warmCancel=null;
            if(warm!=null)Glide.with(view).clear(warm);warm=null;warmOwner=null;warmKey="";
            if(cancel!=null)main.post(cancel);
        }
        public void clear(ImageView target) {
            clearBackground(target);ImgUtil.clear(target);
        }
        public void clearBackground(ImageView target) {
            Slot slot=slots.remove(target);if(slot!=null){var manager=Glide.with(target);if(slot.current!=null)manager.clear(slot.current);if(slot.previous!=null)manager.clear(slot.previous);if(slot.pending!=null)manager.clear(slot.pending);}
        }
        public void cancelPrefetch(ImageView view){if(warmOwner==view)cancelWarm(view);}
        public void cancelBackground(ImageView view){Slot slot=slots.get(view);if(slot!=null&&slot.pending!=null){Glide.with(view).clear(slot.pending);slot.pending=null;}}
        private com.bumptech.glide.RequestBuilder<Drawable> backdrop(ImageView view,String url,int w,int h){
            return Glide.with(view).load(ImgUtil.getUrl(url)).override(w,h).centerCrop().diskCacheStrategy(com.bumptech.glide.load.engine.DiskCacheStrategy.ALL);
        }
        public void background(ImageView view,String url,int width,int height,Consumer<Drawable> ready,Runnable failed){
            Slot slot=slots.computeIfAbsent(view,v->new Slot());if(slot.pending!=null)Glide.with(view).clear(slot.pending);
            var target=new com.bumptech.glide.request.target.CustomTarget<Drawable>(width,height){
                @Override public void onResourceReady(Drawable resource,com.bumptech.glide.request.transition.Transition<? super Drawable> transition){
                    slot.pending=null;slot.previous=slot.current;slot.current=this;ready.accept(resource);
                }
                @Override public void onLoadCleared(@Nullable Drawable placeholder){}
                @Override public void onLoadFailed(@Nullable Drawable error){slot.pending=null;failed.run();}
            };
            slot.pending=target;long start=android.os.SystemClock.uptimeMillis();trace("image-request",0);
            backdrop(view,url,width,height).priority(com.bumptech.glide.Priority.HIGH).listener(new RequestListener<Drawable>(){
                public boolean onLoadFailed(@Nullable GlideException e,Object model,Target<Drawable> ignored,boolean first){
                    int status=-1;if(e!=null)for(Throwable cause:e.getRootCauses())if(cause instanceof com.bumptech.glide.load.HttpException)status=((com.bumptech.glide.load.HttpException)cause).getStatusCode();
                    NativeHome10Diagnostics.event("BACKDROP_REQUEST_FAILED","image="+Integer.toHexString(url.hashCode())+" http="+status);
                    trace("image-failed",0);return false;
                }
                public boolean onResourceReady(Drawable resource,Object model,Target<Drawable> ignored,DataSource source,boolean first){NativeHome10Diagnostics.event("BACKDROP_DECODED","image="+Integer.toHexString(url.hashCode())+" cache="+source+" readyMs="+(android.os.SystemClock.uptimeMillis()-start));trace("image-source="+source+" ready-ms="+(android.os.SystemClock.uptimeMillis()-start),0);return false;}
            }).into(target);
        }
        public Runnable releasePrevious(ImageView view){
            Slot slot=slots.get(view);var previous=slot==null?null:slot.previous;
            return ()->{if(previous!=null){if(slot.previous==previous)slot.previous=null;Glide.with(view).clear(previous);}};
        }
        public void prefetch(ImageView view,String url,int width,int height){
            prefetch(view,url,width,height,result->{});
        }
        public void prefetch(ImageView view,String url,int width,int height,Consumer<Boolean> completed){
            cancelWarm(view);long token=warmGeneration;warmOwner=view;warmKey=url+"|"+width+"x"+height;
            warmCancel=()->completed.accept(false);
            warm=backdrop(view,url,width,height).priority(com.bumptech.glide.Priority.LOW).listener(new RequestListener<Drawable>(){
                private void finish(boolean success){if(token!=warmGeneration)return;warmCancel=null;main.post(()->completed.accept(success&&token==warmGeneration));}
                public boolean onLoadFailed(@Nullable GlideException error,Object model,Target<Drawable> target,boolean first){finish(false);return false;}
                public boolean onResourceReady(Drawable resource,Object model,Target<Drawable> target,DataSource source,boolean first){trace("window-image-ready source="+source,token);finish(true);return false;}
            }).preload(width,height);
            trace("image-prefetch",0);
        }
        public void load(ImageView target,String url,int width,int height,boolean crop,Consumer<Drawable> ready,Runnable failed) {
            var request=Glide.with(target).load(ImgUtil.getUrl(url)).override(width,height).placeholder(target.getDrawable());
            if(crop)request=request.centerCrop();else request=request.fitCenter();
            request.listener(new RequestListener<Drawable>() {
                public boolean onLoadFailed(@Nullable GlideException e,Object model,Target<Drawable> ignored,boolean first){failed.run();return true;}
                public boolean onResourceReady(Drawable resource,Object model,Target<Drawable> ignored,DataSource source,boolean first){ready.accept(resource);return true;}
            }).into(target);
        }
    }
    private final CustomWallView wall;
    private final Object wallOwner;
    private final ImageView background;
    private final ImageView logo, poster;
    private final TextView title;
    private final Loader loader;
    private final Map<ImageView,View.OnLayoutChangeListener> awaiting=new IdentityHashMap<>();
    private long generation,backgroundRequest;
    private boolean landscape;
    private String posterUrl="";
    private String pendingSource="";
    private int posterDecodeWidth,posterDecodeHeight;
    public void setPosterDecodeSize(int width,int height) { posterDecodeWidth=width;posterDecodeHeight=height; }
    private String backdropUrl="";
    private boolean holdingBackground;
    private boolean hasVisibleBackdrop;
    public void focusSource(String url) { reset(false);pendingSource=url; }
    public void settleSource() { String url=pendingSource;pendingSource="";loadPoster(url); }
    public NativeHomeBrowseImages(CustomWallView wall,Object owner,ImageView logo,TextView title,ImageView poster) { this(wall,owner,logo,title,poster,new GlideLoader()); }
    NativeHomeBrowseImages(CustomWallView wall,Object owner,ImageView logo,TextView title,ImageView poster,Loader loader) {
        this.wall=wall;wallOwner=owner;this.background=wall==null?null:wall.acquireContentBackground(owner);
        this.logo=logo;this.title=title;this.poster=poster;this.loader=loader;
    }
    private static final java.util.concurrent.atomic.AtomicInteger traceBudget=new java.util.concurrent.atomic.AtomicInteger(1200);
    static void trace(String event,long token) {
        if(BuildConfig.DEBUG&&android.util.Log.isLoggable("NativeHomeHero",android.util.Log.DEBUG)&&traceBudget.getAndDecrement()>0)
            android.util.Log.d("NativeHomeHero",event+" t="+android.os.SystemClock.uptimeMillis()+" generation="+token);
    }
    public void source(String url) {
        clear();title.setVisibility(View.VISIBLE);logo.setVisibility(View.INVISIBLE);
        // A source poster is a portrait fallback, never an untrusted full-screen backdrop.
        loadPoster(url);
    }
    public void enrich(String backdrop,String logoUrl) { enrich(backdrop,logoUrl,""); }
    public void enrich(String backdrop,String logoUrl,String fallback) {
        if(backdrop==null||backdrop.isEmpty())noBackdrop();
        if(fallback!=null&&!fallback.isEmpty())loadPoster(fallback);
        if(backdrop!=null&&!backdrop.isEmpty())loadBackground(backdrop);
        if(logoUrl==null||logoUrl.isEmpty())return;
        long token=generation;
        measured(logo,()->loader.load(logo,logoUrl,Math.min(1280,logo.getWidth()),Math.min(720,logo.getHeight()),false,resource->{
            if(token!=generation){trace("logo-stale",token);return;}
            logo.setImageDrawable(resource);logo.setVisibility(View.VISIBLE);title.setVisibility(View.INVISIBLE);trace("logo-visible",token);
        },()->{if(token==generation){logo.setVisibility(View.INVISIBLE);title.setVisibility(View.VISIBLE);trace("logo-failed",token);}}));
    }
    private void loadPoster(String url) {
        if(poster==null||url==null||url.isEmpty()||url.equals(posterUrl))return;
        posterUrl=url;long token=generation;
        // Keep the current film's source poster while a richer poster is being decoded.
        if(poster.getDrawable()==null)poster.setVisibility(View.INVISIBLE);
        measured(poster,()->loader.load(poster,url,posterDecodeWidth>0?posterDecodeWidth:Math.min(720,poster.getWidth()),posterDecodeHeight>0?posterDecodeHeight:Math.min(1080,poster.getHeight()),true,resource->{
            if(token!=generation)return;
            poster.setImageDrawable(resource);poster.setVisibility(landscape||holdingBackground?View.INVISIBLE:View.VISIBLE);trace("poster-ready",token);
        },()->{if(token==generation){if(poster.getDrawable()==null)poster.setVisibility(View.INVISIBLE);trace("poster-failed",token);}}));
    }
    private void measured(ImageView view,Runnable action) {
        View.OnLayoutChangeListener previous=awaiting.remove(view);if(previous!=null)view.removeOnLayoutChangeListener(previous);
        if(view.getWidth()>0&&view.getHeight()>0){action.run();return;}
        long token=generation;
        View.OnLayoutChangeListener listener=new View.OnLayoutChangeListener() {
            public void onLayoutChange(View v,int l,int t,int r,int b,int ol,int ot,int or,int ob) {
                if(r<=l||b<=t)return;
                view.removeOnLayoutChangeListener(this);awaiting.remove(view);
                if(token==generation)action.run();
            }
        };
        awaiting.put(view,listener);view.addOnLayoutChangeListener(listener);
    }
    private void loadBackground(String url) {
        if(background==null||!wall.ownsContentBackground(wallOwner)){trace("no-native-wall-host",generation);return;}
        if(url.equals(backdropUrl)&&hasVisibleBackdrop){landscape=true;holdingBackground=false;if(poster!=null)poster.setVisibility(View.INVISIBLE);return;}
        long token=generation,request=++backgroundRequest;
        measured(background,()->{
            int width=Math.min(1280,background.getWidth()),height=Math.min(720,background.getHeight());
            loader.background(background,url,width,height,resource->{
                if(token!=generation||request!=backgroundRequest){NativeHome10Diagnostics.event("BACKDROP_REJECTED","generation="+token+" current="+generation+" reason=stale");trace("backdrop-stale",token);return;}
                trace("backdrop-resource-ready",token);
                backdropUrl=url;Runnable release=loader.releasePrevious(background);
                wall.showContentBackground(wallOwner,resource,()->{
                    if(token!=generation||request!=backgroundRequest)return;
                    landscape=true;hasVisibleBackdrop=true;holdingBackground=false;
                    if(poster!=null)poster.setVisibility(View.INVISIBLE);
                    trace("native-backdrop-drawn",token);
                    NativeHome10Diagnostics.event("BACKDROP_DISPLAYED","generation="+token+" image="+Integer.toHexString(url.hashCode()));
                },release);
            },()->{if(token==generation&&request==backgroundRequest){trace("backdrop-failed",token);noBackdrop();}});
        });
    }
    public void prefetch(String url){prefetch(url,result->{});}
    public void prefetch(String url,Consumer<Boolean> completed){
        if(url==null||url.isEmpty()||background==null||!wall.ownsContentBackground(wallOwner)||background.getWidth()<=0||background.getHeight()<=0){completed.accept(false);return;}
        // A visible-window preload must never replace the foreground's pending layout listener.
        loader.prefetch(background,url,Math.min(1280,background.getWidth()),Math.min(720,background.getHeight()),completed);
    }
    public void noBackdrop(){landscape=false;hasVisibleBackdrop=false;holdingBackground=false;backdropUrl="";if(background!=null&&wall.ownsContentBackground(wallOwner)){wall.clearContentBackground(wallOwner);loader.clearBackground(background);}if(poster!=null&&poster.getDrawable()!=null){poster.setVisibility(View.VISIBLE);NativeHome10Diagnostics.event("POSTER_FALLBACK","generation="+generation);}}
    public void clear() {
        reset(true);
    }
    private void reset(boolean backgroundToo) {
        generation++;backgroundRequest++;holdingBackground=!backgroundToo&&hasVisibleBackdrop;landscape=false;posterUrl="";pendingSource="";
        for(var entry:awaiting.entrySet())entry.getKey().removeOnLayoutChangeListener(entry.getValue());awaiting.clear();
        if(background!=null&&wall.ownsContentBackground(wallOwner)){
            loader.cancelBackground(background);
            if(backgroundToo)loader.cancelPrefetch(background);
            if(backgroundToo||!hasVisibleBackdrop){hasVisibleBackdrop=false;backdropUrl="";wall.clearContentBackground(wallOwner);loader.clearBackground(background);}
        }
        loader.clear(logo);logo.setImageDrawable(null);logo.setVisibility(View.INVISIBLE);title.setVisibility(View.VISIBLE);
        if(poster!=null){loader.clear(poster);poster.setImageDrawable(null);poster.setVisibility(View.INVISIBLE);}
    }
}
