package com.fongmi.android.tv.ui.homepreview;

import android.app.Dialog;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.Rect;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.FrameLayout;
import android.widget.TextView;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.api.config.VodConfig;
import com.fongmi.android.tv.bean.Site;
import com.fongmi.android.tv.setting.SiteGroupOrderStore;
import com.fongmi.android.tv.setting.SiteHealthStore;
import com.fongmi.android.tv.ui.dialog.SiteDialog;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.theme.ThemeTokens;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Consumer;

/** Source drawer only. Secondary filters live inside NativeHomeBrowseView. */
final class HomePreviewMenuDialog extends Dialog {
    private final List<Button> options = new ArrayList<>();
    private final ScrollView scroller;
    private final LinearLayout content;
    private final FrameLayout root;
    private LinearLayout body, groupContent, groupHeader;
    private FrameLayout groupOverlay;
    private ScrollView groupScroll;
    private Button groupSelector;
    private TextView empty;
    private List<Site> sites;
    private final List<Site> visibleSites = new ArrayList<>();
    private final List<Button> groupOptions = new ArrayList<>();
    private List<String> groups, originalGroups;
    private String currentSiteKey, reorderingGroup;
    private String browsedSiteKey;
    private Consumer<Site> applySite;
    private boolean suppressEnterUp;
    private boolean siteRevealPending;
    private int closingKey=KeyEvent.KEYCODE_UNKNOWN;
    private long closingDownTime;
    private long enteringSiteDownTime=-1;
    private Consumer<KeyEvent> shortcutClose;

    void setShortcutCloseListener(Consumer<KeyEvent> listener){shortcutClose=listener;}

    HomePreviewMenuDialog(Context context, String[] labels, IntSupplier selected,
                          IntConsumer apply, View returnTarget) {
        super(context, android.R.style.Theme_Translucent_NoTitleBar);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setCanceledOnTouchOutside(true);
        scroller = new ScrollView(context);
        scroller.setFocusable(false);
        scroller.setVerticalScrollBarEnabled(false);

        content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(20), dp(20), dp(20), dp(20));
        scroller.addView(content, new ScrollView.LayoutParams(-1, -2));
        for (int i = 0; i < labels.length; i++) {
            final int index = i;
            Button option = new Button(context);
            option.setId(View.generateViewId());
            option.setText(labels[i]);
            option.setTextSize(15);
            option.setAllCaps(false);
            option.setMinHeight(0);
            option.setMinimumHeight(0);
            HomePreviewStyles.control(option);
            option.setSelected(i == selected.getAsInt());
            option.setOnClickListener(v -> { apply.accept(index); dismiss(); });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(40));
            lp.bottomMargin = dp(9);
            content.addView(option, lp);
            options.add(option);
        }
        for (int i = 0; i < options.size(); i++) {
            Button option = options.get(i);
            option.setNextFocusUpId(options.get(Math.max(0, i - 1)).getId());
            option.setNextFocusDownId(options.get(Math.min(options.size() - 1, i + 1)).getId());
            option.setNextFocusLeftId(option.getId());
            option.setNextFocusRightId(option.getId());
        }
        root = new FrameLayout(context);
        setContentView(scroller);
        // Configure before WindowManager can draw the first frame. onShow is too late:
        // a selected MATCH_PARENT option otherwise inherits the theme's initial width.
        prepareWindow();
        setOnShowListener(dialog -> {
            if(!options.isEmpty()) options.get(Math.max(0, Math.min(options.size() - 1, selected.getAsInt()))).requestFocus();
        });
        setOnDismissListener(dialog -> {
            if (returnTarget.isAttachedToWindow() && returnTarget.isShown() && !returnTarget.hasFocus()) returnTarget.requestFocus();
        });
    }

    /** Input already comes from HomeActivity's native SiteAdapter (visibility/health order). */
    HomePreviewMenuDialog(Context context, List<Site> sites, IntSupplier selected,
                          Consumer<Site> apply, View returnTarget) {
        this(context, sites.stream().map(Site::getDisplayName).toArray(String[]::new), selected, i -> {}, returnTarget);
        this.sites = new ArrayList<>(sites);
        this.applySite = apply;
        currentSiteKey = sites.isEmpty() ? "" : sites.get(Math.max(0, Math.min(sites.size()-1, selected.getAsInt()))).getKey();
        groups = SiteGroupOrderStore.sort(Site.getGroups(sites));
        SiteDialog.rememberedGroup(groups);
        if(scroller.getParent() instanceof android.view.ViewGroup)((android.view.ViewGroup)scroller.getParent()).removeView(scroller);
        body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        LinearLayout header = new LinearLayout(context);
        groupHeader=header;
        header.setPadding(dp(20), dp(20), dp(20), dp(9));
        groupSelector = button();
        groupSelector.setTextSize(13);
        groupSelector.setGravity(Gravity.START|Gravity.CENTER_VERTICAL);
        groupSelector.setTextAlignment(View.TEXT_ALIGNMENT_VIEW_START);
        groupSelector.setSingleLine(true);
        groupSelector.setEllipsize(TextUtils.TruncateAt.END);
        GradientDrawable arrow = new GradientDrawable() {
            private final android.graphics.Paint paint = new android.graphics.Paint(3);
            @Override public void draw(android.graphics.Canvas canvas) {
                paint.setColor(ThemeController.current().colorOnSurface());
                Rect b = getBounds();
                android.graphics.Path path = new android.graphics.Path();
                path.moveTo(b.left,b.centerY()-dp(2));path.lineTo(b.right,b.centerY()-dp(2));path.lineTo(b.centerX(),b.centerY()+dp(3));path.close();canvas.drawPath(path,paint);
            }
        };
        arrow.setBounds(0,0,dp(10),dp(10));
        groupSelector.setCompoundDrawablesRelative(null,null,arrow,null);
        groupSelector.setCompoundDrawablePadding(dp(8));
        groupSelector.setOnClickListener(v -> openGroups());
        header.addView(groupSelector,new LinearLayout.LayoutParams(-1,dp(32)));
        body.addView(header,new LinearLayout.LayoutParams(-1,-2));
        content.setPadding(dp(20),0,dp(20),dp(20));
        body.addView(scroller,new LinearLayout.LayoutParams(-1,0,1));
        root.addView(body,new FrameLayout.LayoutParams(-1,-1));
        setContentView(root);
        empty = new TextView(context);
        empty.setText(R.string.error_empty);empty.setTextSize(15);empty.setGravity(Gravity.CENTER);
        rebuildSites();
        setOnShowListener(d -> {groupSelector.requestFocus();siteRevealPending=!options.isEmpty();revealCurrentSite();});
    }

    private Button button() {
        Button b = new Button(getContext());b.setId(View.generateViewId());b.setTextSize(15);b.setAllCaps(false);
        b.setMinHeight(0);b.setMinimumHeight(0);HomePreviewStyles.control(b);return b;
    }

    private void rebuildSites() {
        content.removeAllViews();options.clear();visibleSites.clear();
        String group = SiteDialog.rememberedGroup(groups);
        groupSelector.setText(group.isEmpty()?getContext().getString(R.string.site_group_all):group);
        for (Site site : sites) {
            if (!site.inGroup(group)) continue;
            visibleSites.add(site);
            Button option = button();option.setText(site.getDisplayName());
            option.setSelected(site.getKey().equals(currentSiteKey));
            option.setOnClickListener(v -> {applySite.accept(site);dismiss();});
            option.setOnFocusChangeListener((v,focused) -> {if(focused) {browsedSiteKey=site.getKey();groupSelector.setNextFocusDownId(option.getId());health(option,site);}});
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1,dp(40));lp.bottomMargin=dp(9);
            content.addView(option,lp);options.add(option);
        }
        if(options.isEmpty())content.addView(empty,new LinearLayout.LayoutParams(-1,dp(40)));
        for(int i=0;i<options.size();i++){
            Button b=options.get(i);b.setNextFocusUpId(i==0?groupSelector.getId():options.get(i-1).getId());
            b.setNextFocusDownId(options.get(Math.min(i+1,options.size()-1)).getId());b.setNextFocusLeftId(b.getId());b.setNextFocusRightId(b.getId());
        }
        if(visibleSites.stream().noneMatch(site -> site.getKey().equals(browsedSiteKey)))browsedSiteKey=null;
        groupSelector.setNextFocusUpId(groupSelector.getId());
        groupSelector.setNextFocusDownId(options.isEmpty()?groupSelector.getId():currentSiteOption().getId());
        groupSelector.setNextFocusLeftId(groupSelector.getId());groupSelector.setNextFocusRightId(groupSelector.getId());
        siteRevealPending=!options.isEmpty();
        if(siteRevealPending)currentSiteOption().addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->revealCurrentSite());
        refreshTheme();
    }

    private Button currentSiteOption(){
        if(options.isEmpty())return null;
        String targetKey=browsedSiteKey==null?currentSiteKey:browsedSiteKey;
        int selected=0;for(int i=0;i<visibleSites.size();i++)if(visibleSites.get(i).getKey().equals(targetKey))selected=i;
        return options.get(Math.max(0,Math.min(options.size()-1,selected)));
    }

    private void revealCurrentSite(){
        Button target=currentSiteOption();
        if(!siteRevealPending||!isShowing()||target==null||target.getWidth()==0||target.getHeight()==0)return;
        siteRevealPending=false;
        // Native ScrollView visibility handling, independent of keyboard focus. No new scroll engine.
        target.requestRectangleOnScreen(new Rect(0,0,target.getWidth(),target.getHeight()),true);
    }

    private void focusCurrentSite() {
        Button target=currentSiteOption();
        if(target==null){groupSelector.requestFocus();return;}
        // Same focus request and ScrollView reveal as the existing drawer; only map the filtered identity.
        target.requestFocus();
    }

    private void health(Button option,Site site){
        GradientDrawable dot=new GradientDrawable();dot.setShape(GradientDrawable.OVAL);dot.setColor(SiteHealthStore.getColor(site));dot.setBounds(0,0,dp(8),dp(8));
        option.setCompoundDrawablesRelative(dot,null,null,null);option.setCompoundDrawablePadding(dp(8));
    }

    private void openGroups(){
        if(groupOverlay!=null)return;
        groupOverlay=new FrameLayout(getContext());groupOverlay.setClickable(true);groupOverlay.setFocusable(false);groupOverlay.setDefaultFocusHighlightEnabled(false);
        groupOverlay.setOnClickListener(v->{if(reorderingGroup!=null)cancelReorder();else closeGroups(true);});
        groupScroll=new ScrollView(getContext());groupScroll.setVerticalScrollBarEnabled(false);groupScroll.setFocusable(false);
        groupContent=new LinearLayout(getContext());groupContent.setOrientation(LinearLayout.VERTICAL);groupContent.setPadding(dp(4),dp(4),dp(4),dp(4));
        groupScroll.addView(groupContent,new ScrollView.LayoutParams(-1,-2));
        Rect anchor=new Rect(0,0,groupSelector.getWidth(),groupSelector.getHeight());root.offsetDescendantRectToMyCoords(groupSelector,anchor);
        int available=Math.max(dp(40),root.getHeight()-anchor.bottom-dp(24));
        int height=Math.min(dp(40)*(groups.size()+1)+dp(8),Math.min(available,Math.max(dp(40),Math.round(root.getHeight()*.55f))));
        android.text.TextPaint measure=new android.text.TextPaint(groupSelector.getPaint());measure.setTextSize(groupSelector.getTextSize()*15f/13f);
        float textWidth=measure.measureText(getContext().getString(R.string.site_group_all));for(String group:groups)textWidth=Math.max(textWidth,measure.measureText(group));
        int popupWidth=Math.min(Math.round(anchor.width()*.82f),Math.min(dp(144),Math.max(dp(96),(int)Math.ceil(textWidth)+dp(40))));
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(popupWidth,height);lp.leftMargin=Math.max(0,root.getWidth()-dp(12)-popupWidth);lp.topMargin=anchor.bottom+dp(4);
        groupOverlay.addView(groupScroll,lp);root.addView(groupOverlay,new FrameLayout.LayoutParams(-1,-1));
        body.setDescendantFocusability(android.view.ViewGroup.FOCUS_BLOCK_DESCENDANTS);
        groupOptions.clear();addGroup("");for(String g:groups)addGroup(g);linkGroupFocus();refreshTheme();
        String selected=SiteDialog.rememberedGroup(groups);for(Button b:groupOptions)if(b.getTag().equals(selected)){b.requestFocus();break;}
    }

    private void addGroup(String group){
        Button b=button();b.setTag(group);b.setTextSize(13);b.setGravity(Gravity.CENTER);b.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);b.setSingleLine(true);b.setHorizontallyScrolling(false);b.setEllipsize(TextUtils.TruncateAt.END);
        b.setOnClickListener(v->{if(reorderingGroup!=null){if(group.equals(reorderingGroup))saveReorder();return;}SiteDialog.rememberGroup(group);closeGroups(false);rebuildSites();groupSelector.requestFocus();});
        if(!group.isEmpty())b.setOnLongClickListener(v->startReorder(group));
        groupContent.addView(b,new LinearLayout.LayoutParams(-1,dp(40)));groupOptions.add(b);
    }

    private boolean startReorder(String group){
        if(groups.size()<2||group.isEmpty())return false;
        if(reorderingGroup==null){originalGroups=new ArrayList<>(groups);reorderingGroup=group;}
        suppressEnterUp=true;refreshGroups();return true;
    }

    private void moveGroup(int direction){
        if(!SiteGroupOrderStore.move(groups,reorderingGroup,direction))return;
        Button active=null;for(Button b:groupOptions)if(b.getTag().equals(reorderingGroup))active=b;
        groupOptions.remove(active);groupOptions.add(groups.indexOf(reorderingGroup)+1,active);
        groupContent.removeView(active);groupContent.addView(active,groups.indexOf(reorderingGroup)+1);
        linkGroupFocus();active.requestFocus();
    }

    private void linkGroupFocus(){
        for(int i=0;i<groupOptions.size();i++){
            Button b=groupOptions.get(i);b.setNextFocusUpId(groupOptions.get(Math.max(0,i-1)).getId());b.setNextFocusDownId(groupOptions.get(Math.min(i+1,groupOptions.size()-1)).getId());
            b.setNextFocusLeftId(b.getId());b.setNextFocusRightId(b.getId());
        }
    }

    private void saveReorder(){
        SiteGroupOrderStore.save(Site.getGroups(VodConfig.get().getSites()),groups);
        reorderingGroup=null;originalGroups=null;refreshGroups();
    }

    private void cancelReorder(){
        if(reorderingGroup==null)return;
        String active=reorderingGroup;groups=new ArrayList<>(originalGroups);reorderingGroup=null;originalGroups=null;
        List<Button> ordered=new ArrayList<>();ordered.add(groupOptions.get(0));for(String g:groups)for(Button b:groupOptions)if(b.getTag().equals(g))ordered.add(b);
        groupOptions.clear();groupOptions.addAll(ordered);groupContent.removeAllViews();for(Button b:groupOptions)groupContent.addView(b);
        linkGroupFocus();refreshGroups();for(Button b:groupOptions)if(b.getTag().equals(active))b.requestFocus();
    }

    private void refreshGroups(){
        String selected=SiteDialog.rememberedGroup(groups);
        for(Button b:groupOptions){
            String g=(String)b.getTag();boolean moving=g.equals(reorderingGroup);String text=g.isEmpty()?getContext().getString(R.string.site_group_all):g;
            b.setText(moving?"↕ "+text:text);b.setSelected(g.equals(selected));b.setAlpha(reorderingGroup!=null&&!moving?.55f:1);
            b.setFocusable(reorderingGroup==null||moving);
            b.setContentDescription(moving?getContext().getString(R.string.native_source_reorder_description,text):text);styleGroup(b,false);
        }
    }

    private void closeGroups(boolean restoreFocus){
        if(groupOverlay==null)return;
        root.removeView(groupOverlay);groupOverlay=null;groupOptions.clear();groupScroll=null;groupContent=null;
        body.setDescendantFocusability(android.view.ViewGroup.FOCUS_AFTER_DESCENDANTS);
        if(restoreFocus)groupSelector.requestFocus();
    }

    @Override public void dismiss(){
        siteRevealPending=false;
        if(reorderingGroup!=null){groups=originalGroups;reorderingGroup=null;originalGroups=null;}
        closeGroups(false);super.dismiss();
    }

    private void prepareWindow() {
        Window window = getWindow();
        if (window == null) return;
        int width = getContext().getResources().getDisplayMetrics().widthPixels;
        window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        window.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND | WindowManager.LayoutParams.FLAG_FULLSCREEN);
        window.setGravity(Gravity.END | Gravity.TOP);
        window.setLayout(Math.min(Math.round(width * .22f), Math.max(dp(180), Math.round(width * .20f))), -1);
        window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        refreshTheme();
    }

    @Override public void show() {
        closingKey=KeyEvent.KEYCODE_UNKNOWN;
        prepareWindow();
        super.show();
    }

    void refreshTheme() {
        ThemeTokens t = ThemeController.current();
        // WOGG's current drawer uses ~.42 glass fill. Keep native theme hue/opacity,
        // with a restrained highlight gradient rather than an opaque themed slab.
        float opacity=Math.max(.30f,Math.min(.50f,t.dialogOpacity()*.47f));
        int background=HomePreviewStyles.alpha(t.colorSurfaceContainer(),opacity);
        GradientDrawable glass=new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{
            androidx.core.graphics.ColorUtils.compositeColors(HomePreviewStyles.alpha(t.colorOverlayLight(),.08f),background),
            background,
            androidx.core.graphics.ColorUtils.compositeColors(HomePreviewStyles.alpha(t.colorShadow(),.06f),background)});
        root.setBackgroundColor(Color.TRANSPARENT);
        if(body!=null){body.setBackground(glass);scroller.setBackgroundColor(Color.TRANSPARENT);}
        else scroller.setBackgroundColor(HomePreviewStyles.alpha(t.colorSurfaceContainer(),t.dialogOpacity()));
        for (Button option : options) styleOption(option);
        if(sites!=null){
            groupHeader.setBackgroundColor(Color.TRANSPARENT);
            styleGroup(groupSelector,true);empty.setTextColor(t.colorOnSurface());
            for(int i=0;i<options.size();i++)health(options.get(i),visibleSites.get(i));
        }
        if(groupOverlay!=null){
            groupScroll.setBackground(HomePreviewStyles.shape(groupScroll,t.colorSurfaceContainerHigh(),t.colorOutline(),12));
            groupScroll.setElevation(dp(6));refreshGroups();
        }
        // Theme scrim is a color token, not a window opacity policy. Never black out the home.
        if (getWindow() != null) getWindow().setDimAmount(Math.max(.10f, Math.min(.28f, Color.alpha(t.colorScrim()) / 255f * .28f)));
    }

    private void styleGroup(Button view,boolean selector){
        ThemeTokens t=ThemeController.current();
        android.graphics.drawable.StateListDrawable states=new android.graphics.drawable.StateListDrawable();
        states.addState(new int[]{android.R.attr.state_selected,android.R.attr.state_focused},HomePreviewStyles.shape(view,HomePreviewStyles.alpha(t.colorPrimary(),.16f),t.colorFocus(),8));
        states.addState(new int[]{android.R.attr.state_focused},HomePreviewStyles.shape(view,t.colorSurfaceContainerHigh(),t.colorFocus(),8));
        states.addState(new int[]{android.R.attr.state_selected},HomePreviewStyles.shape(view,HomePreviewStyles.alpha(t.colorPrimary(),.16f),Color.TRANSPARENT,8));
        states.addState(new int[]{},HomePreviewStyles.shape(view,selector?HomePreviewStyles.alpha(t.colorSurfaceContainerHigh(),.65f):Color.TRANSPARENT,selector?t.colorOutlineVariant():Color.TRANSPARENT,8));
        view.setBackground(states);view.setTextColor(t.colorOnSurface());centerText(view,selector?Gravity.START|Gravity.CENTER_VERTICAL:Gravity.CENTER);
    }

    private void styleOption(Button option){
            ThemeTokens t=ThemeController.current();
            android.graphics.drawable.StateListDrawable states = new android.graphics.drawable.StateListDrawable();
            GradientDrawable selectedFocus = HomePreviewStyles.shape(option, t.colorPrimaryContainer(), t.colorFocus(), 24);
            selectedFocus.setStroke(dp(2), t.colorFocus());
            states.addState(new int[]{android.R.attr.state_selected, android.R.attr.state_focused}, selectedFocus);
            states.addState(new int[]{android.R.attr.state_selected}, HomePreviewStyles.shape(option, t.colorPrimary(), Color.TRANSPARENT, 24));
            states.addState(new int[]{android.R.attr.state_focused}, HomePreviewStyles.shape(option, t.colorSurfaceContainerHigh(), t.colorFocus(), 24));
            states.addState(new int[]{}, new ColorDrawable(Color.TRANSPARENT));
            option.setBackground(states);
            centerText(option,Gravity.CENTER);
            option.setTextColor(new ColorStateList(new int[][]{{android.R.attr.state_selected, android.R.attr.state_focused}, {android.R.attr.state_selected}, {}},
                    new int[]{t.colorOnPrimaryContainer(), t.colorOnPrimary(), t.colorOnSurface()}));
    }
    private void centerText(Button view,int gravity){
        // Match the compact category rhythm without inheriting Button theme font/insets.
        view.setMinHeight(0);view.setMinimumHeight(0);
        view.setIncludeFontPadding(false);view.setGravity(gravity);
        view.setPadding(dp(12),0,dp(12),0);
    }

    @Override public void onBackPressed(){
        if(sites==null){super.onBackPressed();return;}
        if(!isShowing())return;
        // Android 13+ Dialog's system Back callback calls this directly, bypassing KeyEvent.
        if(reorderingGroup!=null){cancelReorder();return;}
        if(groupOverlay!=null){closeGroups(true);return;}
        if(getCurrentFocus()==groupSelector){dismiss();return;}
        groupSelector.requestFocus();
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        int key=event.getKeyCode();
        if(key==KeyEvent.KEYCODE_DPAD_DOWN&&event.getAction()==KeyEvent.ACTION_UP&&event.getDownTime()==enteringSiteDownTime){enteringSiteDownTime=-1;return true;}
        if(!isShowing()&&key==closingKey&&event.getDownTime()==closingDownTime)return true;
        if(sites!=null&&key==KeyEvent.KEYCODE_BACK){
            if(event.getAction()==KeyEvent.ACTION_UP&&!event.isCanceled())onBackPressed();
            return true;
        }
        if(sites!=null&&reorderingGroup==null&&(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)){
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0){
                closingKey=key;closingDownTime=event.getDownTime();
                if(shortcutClose!=null)shortcutClose.accept(event);
                dismiss();
            }
            return true;
        }
        if(groupOverlay!=null){
            if(key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT)return true;
            if(key==KeyEvent.KEYCODE_DPAD_CENTER||key==KeyEvent.KEYCODE_ENTER||key==KeyEvent.KEYCODE_NUMPAD_ENTER){
                if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)suppressEnterUp=false;
                if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()>0){
                    View focused=getCurrentFocus();if(focused!=null&&groupOptions.contains(focused))startReorder((String)focused.getTag());suppressEnterUp=true;return true;
                }
                if(reorderingGroup!=null||suppressEnterUp){
                    if(event.getAction()==KeyEvent.ACTION_UP){if(suppressEnterUp)suppressEnterUp=false;else saveReorder();}return true;
                }
            }
            if(reorderingGroup!=null&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN)){
                if(event.getAction()==KeyEvent.ACTION_DOWN)moveGroup(key==KeyEvent.KEYCODE_DPAD_UP?-1:1);return true;
            }
            int groupIndex=groupOptions.indexOf(getCurrentFocus());
            if(groupIndex>=0&&((groupIndex==0&&key==KeyEvent.KEYCODE_DPAD_UP)||(groupIndex==groupOptions.size()-1&&key==KeyEvent.KEYCODE_DPAD_DOWN)))return true;
            return super.dispatchKeyEvent(event);
        }
        int index = options.indexOf(getCurrentFocus());
        if(sites!=null&&getCurrentFocus()==groupSelector&&key==KeyEvent.KEYCODE_DPAD_DOWN){
            if(event.getAction()==KeyEvent.ACTION_DOWN){enteringSiteDownTime=event.getDownTime();focusCurrentSite();}return true;
        }
        if(sites!=null&&index==0&&key==KeyEvent.KEYCODE_DPAD_UP){if(event.getAction()==KeyEvent.ACTION_DOWN)groupSelector.requestFocus();return true;}
        if(sites!=null&&getCurrentFocus()==groupSelector&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_LEFT||key==KeyEvent.KEYCODE_DPAD_RIGHT||(options.isEmpty()&&key==KeyEvent.KEYCODE_DPAD_DOWN)))return true;
        if (index >= 0 && ((key == KeyEvent.KEYCODE_DPAD_UP && index == 0)
                || (key == KeyEvent.KEYCODE_DPAD_DOWN && index == options.size() - 1)
                || key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_DPAD_RIGHT)) return true;
        if ((key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER) && event.getRepeatCount() > 0) return true;
        return super.dispatchKeyEvent(event);
    }

    private int dp(float value) { return HomePreviewPosterAdapter.dp(getContext(), value); }
}

