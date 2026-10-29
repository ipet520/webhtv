package com.fongmi.android.tv.ui.home;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.ui.homepreview.NativeHomeBrowseView;
import com.fongmi.android.tv.bean.HomeButton;
import com.fongmi.android.tv.ui.presenter.FuncPresenter;

/** Presentation only. HomeActivity retains WebHome, configuration, lifecycle and business authority. */
public final class NativeHomeModeHost {
    private final Context context;
    private final FrameLayout root;
    private final ViewGroup classic;
    private final View toolbar;
    private final Runnable exit;
    private final FuncPresenter.OnClickListener functions;
    private NativeHomeBrowseView immersive;
    private Bundle viewState;
    private int classicFocusability;
    private int toolbarVisibility;
    private NativeHomeBrowseController business;
    private final NativeHomeHero hero=new NativeHomeHero();
    private java.util.function.Supplier<com.fongmi.android.tv.bean.Site> site;
    private java.util.function.Supplier<java.util.List<com.fongmi.android.tv.bean.Site>> sources;
    private java.util.function.Consumer<com.fongmi.android.tv.bean.Site> sourceApply;
    private java.util.function.Consumer<com.fongmi.android.tv.bean.Vod> click;
    private java.util.function.Predicate<com.fongmi.android.tv.bean.Vod> longClick;
    private boolean visualsPaused;
    private final NativeHomePreparationQueue preparation;
    public void setBusiness(NativeHomeBrowseController business,
            java.util.function.Supplier<com.fongmi.android.tv.bean.Site> site,
            java.util.function.Supplier<java.util.List<com.fongmi.android.tv.bean.Site>> sources,
            java.util.function.Consumer<com.fongmi.android.tv.bean.Site> sourceApply,
            java.util.function.Consumer<com.fongmi.android.tv.bean.Vod> click,
            java.util.function.Predicate<com.fongmi.android.tv.bean.Vod> longClick) {
        this.business=business;this.site=site;this.sources=sources;this.sourceApply=sourceApply;this.click=click;this.longClick=longClick;
    }
    public void reload() { hero.clearFocus();if(business!=null)business.reload(site.get().getKey()); }

    public NativeHomeModeHost(Context context, FrameLayout root, ViewGroup classic, View toolbar,
                              Runnable exit, FuncPresenter.OnClickListener functions, Bundle savedState) {
        this.context = context;
        this.root = root;
        this.classic = classic;
        this.toolbar = toolbar;
        this.exit = exit;
        this.functions = functions;
        viewState = savedState;
        preparation=new NativeHomePreparationQueue((vod,completed)->site!=null&&hero.prepareWindow(site.get(),vod,completed),
            new NativeHomePreparationQueue.Images(){
                public void start(String url,java.util.function.Consumer<Boolean> completed){if(immersive==null||visualsPaused)completed.accept(false);else immersive.prefetchWindowArtwork(url,completed);}
                public void logo(String url,java.util.function.Consumer<Boolean> completed){if(immersive==null||visualsPaused)completed.accept(false);else immersive.prefetchWindowLogo(url,completed);}
            },true);
        hero.setAvailabilityListener(preparation::kick);
    }

    public boolean isVisible() { return immersive != null; }

    public void show() {
        show(false);
    }
    public void show(boolean refresh) {
        if (immersive != null) { if(business!=null&&(refresh||!business.siteKey().equals(site.get().getKey())))reload();return; }
        if(refresh&&business!=null)business.reload(site.get().getKey());
        classicFocusability = classic.getDescendantFocusability();
        toolbarVisibility = toolbar.getVisibility();
        classic.clearFocus();
        toolbar.clearFocus();
        classic.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        classic.setVisibility(View.GONE);
        toolbar.setVisibility(View.GONE);
        immersive = new NativeHomeBrowseView(context, business==null?18:0, 0, exit);
        if(root.getParent() instanceof ViewGroup content) {
            for(int i=0;i<content.getChildCount();i++) {
                if(content.getChildAt(i) instanceof com.fongmi.android.tv.ui.custom.CustomWallView wall) {
                    immersive.setBackgroundHost(wall);break;
                }
            }
        }
        if(business!=null)immersive.setBusiness(business,sources,sourceApply,click,longClick,card->{
            if(visualsPaused)return;
            if(immersive.isPosterRailFocused())preparation.focus(card.vod());
            hero.focus(site.get(),card.vod(),info->{
                if(immersive==null)return;
                immersive.showHero(card.id(),info.metadata(),info.overview(),info.backdrop(),info.logo(),info.poster());
                preparation.currentMetadata(card.vod(),info);
            },()->{if(immersive!=null)immersive.settleFocusedArtwork(card.id());},
                ()->{if(immersive!=null){immersive.noFocusedBackdrop(card.id());immersive.noFocusedLogo(card.id());}},!immersive.isPosterRailFocused());
        });
        immersive.setPreparedLogoLookup(preparation::logo);
        if(business!=null)immersive.setPreparationWindowListener(window->preparation.window(window.context(),site.get().getKey()+"|"+NativeHomeHero.scope(com.fongmi.android.tv.bean.TmdbConfig.effectiveCurrent()),window.candidates()));
        preparation.resume();
        refreshFunctions();
        root.addView(immersive, new FrameLayout.LayoutParams(-1, -1));
        if(business!=null)business.attach(site.get().getKey(),controller-> {
            if(controller.state()==null||controller.state().items.isEmpty()){hero.clearFocus();preparation.clearWindow();}
            immersive.renderBusiness(controller,site.get().getKey());
        });
        immersive.restorePreviewState(viewState);
    }

    public void hide() {
        if (immersive == null) return;
        preparation.pause();preparation.clearWindow();
        hero.clearFocus();if(business!=null)business.detach();
        viewState = new Bundle();
        immersive.savePreviewState(viewState);
        immersive.release();
        root.removeView(immersive);
        immersive = null;
        classic.setDescendantFocusability(classicFocusability);
        classic.setVisibility(View.VISIBLE);
        toolbar.setVisibility(toolbarVisibility);
    }

    public Bundle saveState() {
        if (immersive == null) return viewState;
        Bundle state = new Bundle();
        immersive.savePreviewState(state);
        return state;
    }

    public boolean handleBack() { return immersive != null && immersive.handleBack(); }
    public void refreshFunctions() {
        if (immersive != null) immersive.setFunctions(HomeButton.getVisibleButtons(), functions);
    }
    public void refreshTheme() { if (immersive != null) immersive.refreshTheme(); }
    public void pauseVisuals() { visualsPaused=true;hero.clearFocus();preparation.pause();preparation.clearWindow();if(immersive!=null)immersive.setVisualsActive(false); }
    public void resumeVisuals() { visualsPaused=false;preparation.resume();if(immersive!=null){immersive.setVisualsActive(true);immersive.refreshFocusedHero();} }

    public void release() { hide(); }
}
