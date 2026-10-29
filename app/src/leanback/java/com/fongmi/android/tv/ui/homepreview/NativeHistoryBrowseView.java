package com.fongmi.android.tv.ui.homepreview;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.*;
import android.text.TextUtils;
import android.view.*;
import android.widget.*;
import androidx.leanback.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.graphics.ColorUtils;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.*;
import com.fongmi.android.tv.theme.*;
import com.fongmi.android.tv.ui.custom.CustomWallView;
import com.fongmi.android.tv.ui.home.*;
import java.util.*;

/** One native history presentation; no storage, resume, delete or report implementation. */
public final class NativeHistoryBrowseView extends FrameLayout {
    public interface Actions {void open(History item);void details(History item);void delete(History item,int position);boolean longClick();void manage();void report();}
    final HorizontalGridView rail;
    final TextView heading,deleteButton,reportButton,resume,meta,overview,episode,time,empty;
    final HomeHeroTitleView title;
    final ImageView logo,poster;
    final ProgressBar progress;
    final NativeHistoryPosterAdapter adapter;
    final LinearLayout top,hero,resumeRow,progressInfo,progressLabels;
    final View leftShade,bottomShade;
    private final Actions actions;
    private final NativeHistoryArtwork artwork=new NativeHistoryArtwork();
    private final NativeHomePreparationQueue preparation;
    private final Map<String,Vod> models=new HashMap<>();
    private NativeHomeBrowseImages images;
    private CustomWallView wall;
    private History current;
    private String selectedIdentity="",artIdentity="",prepareSource="";
    private Object prepareContext=new Object();
    private int selectedPosition,safeLeft,safeTop,safeRight,safeBottom,direction=1;
    private long revision;
    private boolean initialized,active=true,released,entryPending,backFromCards;
    private ThemeTokens appliedTokens;

    public NativeHistoryBrowseView(Context context,Actions actions){
        super(context);this.actions=actions;setClipChildren(false);setClipToPadding(false);setBackgroundColor(Color.TRANSPARENT);
        leftShade=new View(context);bottomShade=new View(context);addView(leftShade,new LayoutParams(-1,-1));addView(bottomShade,new LayoutParams(-1,-1));
        top=new LinearLayout(context);top.setGravity(Gravity.CENTER_VERTICAL);addView(top);
        heading=text(23);heading.setText(R.string.home_history);top.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        deleteButton=button(R.string.setting_delete);reportButton=button(R.string.menu_viewing_report);top.addView(deleteButton);top.addView(reportButton);
        deleteButton.setOnClickListener(v->actions.manage());reportButton.setOnClickListener(v->actions.report());
        hero=new LinearLayout(context);hero.setOrientation(LinearLayout.VERTICAL);hero.setClipChildren(false);addView(hero);
        FrameLayout titleSlot=new FrameLayout(context);hero.addView(titleSlot,new LinearLayout.LayoutParams(-1,dp(86)));
        title=new HomeHeroTitleView(context,null);title.bindTitle(Long.MIN_VALUE,"");title.setSingleLine();title.setEllipsize(TextUtils.TruncateAt.MARQUEE);title.setMarqueeRepeatLimit(-1);title.setGravity(Gravity.BOTTOM);titleSlot.addView(title,new LayoutParams(-1,-1));
        logo=new ImageView(context);logo.setScaleType(ImageView.ScaleType.FIT_START);logo.setTag("webhtv:ignore");titleSlot.addView(logo,new LayoutParams(-1,-1));
        meta=text(14);meta.setSingleLine();hero.addView(meta,new LinearLayout.LayoutParams(-1,dp(24)));
        overview=text(14);overview.setMaxLines(3);overview.setEllipsize(TextUtils.TruncateAt.END);hero.addView(overview,new LinearLayout.LayoutParams(-1,dp(68)));
        poster=new ImageView(context);poster.setScaleType(ImageView.ScaleType.CENTER_CROP);poster.setTag("webhtv:ignore");poster.setOutlineProvider(new ViewOutlineProvider(){public void getOutline(View v,android.graphics.Outline o){o.setRoundRect(0,0,v.getWidth(),v.getHeight(),dp(8));}});poster.setClipToOutline(true);addView(poster);
        resumeRow=new LinearLayout(context);resumeRow.setGravity(Gravity.CENTER_VERTICAL);resumeRow.setClipChildren(false);addView(resumeRow);
        resume=button(R.string.native_history_view_details);resumeRow.addView(resume,new LinearLayout.LayoutParams(dp(128),-1));
        resume.setOnClickListener(v->{if(current!=null)actions.details(current);});
        progressInfo=new LinearLayout(context);progressInfo.setOrientation(LinearLayout.VERTICAL);LinearLayout.LayoutParams info=new LinearLayout.LayoutParams(dp(220),-2);info.leftMargin=dp(16);resumeRow.addView(progressInfo,info);
        progressLabels=new LinearLayout(context);episode=text(12);episode.setSingleLine();episode.setEllipsize(TextUtils.TruncateAt.END);time=text(12);time.setSingleLine();progressLabels.addView(episode,new LinearLayout.LayoutParams(0,-2,1));progressLabels.addView(time);progressInfo.addView(progressLabels,new LinearLayout.LayoutParams(-1,dp(22)));
        progress=new ProgressBar(context,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(10000);progressInfo.addView(progress,new LinearLayout.LayoutParams(-1,dp(3)));
        rail=new HorizontalGridView(context);rail.setNumRows(1);rail.setWindowAlignment(BaseGridView.WINDOW_ALIGN_BOTH_EDGE);rail.setWindowAlignmentOffset(0);rail.setWindowAlignmentOffsetPercent(50);rail.setItemAlignmentOffset(0);rail.setItemAlignmentOffsetPercent(50);rail.setWindowAlignmentPreferKeyLineOverLowEdge(false);rail.setWindowAlignmentPreferKeyLineOverHighEdge(false);rail.setFocusScrollStrategy(BaseGridView.FOCUS_SCROLL_ALIGNED);rail.setClipChildren(false);rail.setClipToPadding(false);rail.setItemAnimator(null);rail.setHasFixedSize(true);rail.setHorizontalSpacing(dp(12));if(android.os.Build.VERSION.SDK_INT>=26)rail.setDefaultFocusHighlightEnabled(false);addView(rail);
        adapter=new NativeHistoryPosterAdapter(this::select,actions::open,actions::delete,actions::longClick);rail.setAdapter(adapter);
        rail.addOnScrollListener(new RecyclerView.OnScrollListener(){public void onScrollStateChanged(RecyclerView r,int state){if(state==RecyclerView.SCROLL_STATE_IDLE)prepareWindow();}});
        empty=text(20);empty.setGravity(Gravity.CENTER);empty.setText(R.string.error_empty);addView(empty);
        preparation=new NativeHomePreparationQueue(artwork::prepare,(url,done)->{if(images==null)done.accept(false);else images.prefetch(url,done);});artwork.available(preparation::kick);
        setOnApplyWindowInsetsListener((v,insets)->{int l=insets.getSystemWindowInsetLeft(),t=insets.getSystemWindowInsetTop(),r=insets.getSystemWindowInsetRight(),b=insets.getSystemWindowInsetBottom();if(android.os.Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){var c=insets.getDisplayCutout();l=Math.max(l,c.getSafeInsetLeft());t=Math.max(t,c.getSafeInsetTop());r=Math.max(r,c.getSafeInsetRight());b=Math.max(b,c.getSafeInsetBottom());}if(l!=safeLeft||t!=safeTop||r!=safeRight||b!=safeBottom){safeLeft=l;safeTop=t;safeRight=r;safeBottom=b;requestLayout();}return insets;});
        refreshTheme();showEmpty();
    }
    private TextView text(float size){TextView text=new TextView(getContext());text.setTextSize(size);return text;}
    private TextView button(int resource){TextView text=text(16);text.setText(resource);text.setGravity(Gravity.CENTER);text.setPadding(dp(16),dp(7),dp(16),dp(7));text.setOnFocusChangeListener((v,f)->HomePreviewStyles.functionFocus(v,f));return text;}
    private int dp(float value){return HomePreviewPosterAdapter.dp(getContext(),value);}
    private static void text(TextView view,String value){if(!TextUtils.equals(view.getText(),value))view.setText(value);}
    public void attachBackground(CustomWallView wall){this.wall=wall;images=new NativeHomeBrowseImages(wall,this,logo,title,poster);}
    public void refreshTheme(){
        ThemeTokens t=ThemeController.current();if(t.equals(appliedTokens))return;appliedTokens=t;float opacity=ColorUtils.calculateLuminance(t.colorSurface())>.5?.88f:.79f;
        leftShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,new int[]{HomePreviewStyles.alpha(t.colorSurface(),opacity),HomePreviewStyles.alpha(t.colorSurface(),opacity*.45f),Color.TRANSPARENT}));
        bottomShade.setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,new int[]{HomePreviewStyles.alpha(t.colorSurface(),.98f),HomePreviewStyles.alpha(t.colorSurface(),.25f),Color.TRANSPARENT}));
        for(TextView text:new TextView[]{heading,title,overview,episode,time,empty})text.setTextColor(t.colorOnSurface());meta.setTextColor(t.colorOnSurfaceVariant());
        for(TextView text:new TextView[]{title,meta,overview})text.setShadowLayer(2,0,1,t.colorShadow());
        for(TextView text:new TextView[]{deleteButton,reportButton})HomePreviewStyles.function(text,t);
        HomePreviewStyles.control(resume,t);if(resume.getBackground() instanceof StateListDrawable states)for(int i=0;i<states.getStateCount();i++)if(states.getStateDrawable(i) instanceof GradientDrawable shape){shape.setCornerRadius(dp(999));if(states.getStateSet(i).length==0)shape.setColor(HomePreviewStyles.alpha(t.colorSurfaceContainerHigh(),.60f));}
        progress.setProgressTintList(ColorStateList.valueOf(t.colorPrimary()));progress.setProgressBackgroundTintList(ColorStateList.valueOf(HomePreviewStyles.alpha(t.colorOnSurface(),.45f)));
        adapter.notifyItemRangeChanged(0,adapter.getItemCount());
    }
    public void submit(List<History> items,boolean deleting){
        long token=++revision;boolean restore=rail.hasFocus()||!initialized||entryPending;String identity=selectedIdentity;int oldPosition=selectedPosition;
        adapter.deleting(deleting);adapter.setItems(new ArrayList<>(items),()->{
            if(token!=revision||released)return;deleteButton.setEnabled(!items.isEmpty());
            if(adapter.getItemCount()==0){showEmpty();if(restore)reportButton.requestFocus();return;}
            empty.setVisibility(GONE);hero.setVisibility(VISIBLE);resumeRow.setVisibility(VISIBLE);resume.setEnabled(true);rail.setVisibility(VISIBLE);
            int position=Math.min(oldPosition,adapter.getItemCount()-1);for(int i=0;i<adapter.getItemCount();i++)if(adapter.getItem(i).getDisplayIdentity().equals(identity)){position=i;break;}
            if(entryPending){position=0;entryPending=false;}
            select(adapter.getItem(position),position);initialized=true;
            if(restore)rail.setSelectedPosition(position,new ViewHolderTask(){@Override public void run(RecyclerView.ViewHolder h){if(token==revision&&active&&!released)h.itemView.requestFocus();}});else rail.setSelectedPosition(position);
        });
    }
    private void showEmpty(){current=null;selectedIdentity="";selectedPosition=0;artIdentity="";artwork.pause();preparation.clearWindow();models.clear();title.setText("");text(meta,"");text(overview,"");text(episode,"");text(time,"");progress.setProgress(0);if(images!=null)images.clear();poster.setVisibility(INVISIBLE);logo.setVisibility(INVISIBLE);hero.setVisibility(INVISIBLE);resumeRow.setVisibility(INVISIBLE);resume.setEnabled(false);rail.setVisibility(INVISIBLE);empty.setVisibility(VISIBLE);deleteButton.setEnabled(false);}
    private Vod model(History item){return models.computeIfAbsent(item.getDisplayIdentity()+"|"+item.getTmdbId()+"|"+item.getVodPic(),key->artwork.item(item));}
    private void select(History item,int position){
        if(released)return;if(position!=0)entryPending=false;if(position!=selectedPosition)direction=position>selectedPosition?1:-1;selectedPosition=position;selectedIdentity=item.getDisplayIdentity();current=item;
        title.bindTitle(NativeHistoryPosterAdapter.identity(item),item.getVodName());
        text(meta,String.join(" · ",java.util.stream.Stream.of(item.getYear(),item.getTypeName(),item.getArea(),item.getSiteName()).filter(s->!s.isEmpty()).toList()));
        text(episode,item.getVodRemarks());text(time,item.hasPlaybackTime()?item.getPlaybackTimeText():com.fongmi.android.tv.utils.Util.timeMs(Math.max(0,item.getPosition()))+" / --:--");progress.setVisibility(item.hasPlaybackTime()?VISIBLE:INVISIBLE);progress.setProgress(NativeHistoryPosterAdapter.progress(item));
        String next=item.getDisplayIdentity()+"|"+item.getVodPic()+"|"+item.getWallPic()+"|"+item.getTmdbId();
        if(!next.equals(artIdentity)&&active){artIdentity=next;text(overview,"");if(images!=null){images.focusSource(item.getVodPic());images.settleSource();if(!item.getWallPic().isEmpty()&&!item.getWallPic().equals(item.getVodPic()))images.enrich(item.getWallPic(),"");}
            Vod vod=model(item);String expected=artIdentity;
            artwork.focus(vod,info->{if(!active||!expected.equals(artIdentity)||released)return;text(overview,info.overview());if(images!=null)images.enrich(info.backdrop().isEmpty()?trustedWall(item):info.backdrop(),info.logo(),info.poster());preparation.currentMetadata(vod,info);},()->{},()->{if(active&&expected.equals(artIdentity)&&images!=null&&trustedWall(item).isEmpty())images.noBackdrop();},!initialized);
        }
        prepareWindow();
    }
    private static String trustedWall(History item){return item.getWallPic().equals(item.getVodPic())?"":item.getWallPic();}
    private void prepareWindow(){
        if(!active||current==null||released)return;String source=current.getCid()+"|"+current.getSiteKey();if(!source.equals(prepareSource)){prepareSource=source;prepareContext=new Object();}
        List<NativeHomePreparationQueue.Candidate> candidates=new ArrayList<>();int first=selectedPosition,last=selectedPosition;
        for(int i=0;i<rail.getChildCount();i++){int p=rail.getChildAdapterPosition(rail.getChildAt(i));if(p>=0){first=Math.min(first,p);last=Math.max(last,p);}}
        first=Math.max(0,first-1);last=Math.min(adapter.getItemCount()-1,last+1);
        for(int p=first;p<=last&&candidates.size()<NativeHomePreparationQueue.MAX_WINDOW;p++){History item=adapter.getItem(p);if(item.getCid()!=current.getCid()||!item.getSiteKey().equals(current.getSiteKey()))continue;candidates.add(new NativeHomePreparationQueue.Candidate(model(item),p==selectedPosition?0:1+Math.abs(p-selectedPosition)+(Integer.signum(p-selectedPosition)==direction?0:10)));}
        preparation.window(prepareContext,artwork.scope(),candidates);
    }
    public int focusedPosition(){return rail.hasFocus()?selectedPosition:RecyclerView.NO_POSITION;}
    public void restore(int position){if(adapter.getItemCount()>0)rail.setSelectedPosition(Math.max(0,Math.min(position,adapter.getItemCount()-1)),new ViewHolderTask(){@Override public void run(RecyclerView.ViewHolder holder){if(active&&!released)holder.itemView.requestFocus();}});}
    /** Every Activity entry starts at the first record; in-page refresh/delete is separate. */
    public void enter(){if(released)return;if(!active)artIdentity="";active=true;entryPending=true;title.setMarqueeActive(true);preparation.resume();}
    public void active(boolean active){if(this.active==active)return;this.active=active;title.setMarqueeActive(active);if(active){preparation.resume();artIdentity="";if(current!=null)select(current,selectedPosition);}else{artwork.pause();preparation.pause();if(images!=null)images.clear();}}
    public void release(){active(false);released=true;revision++;preparation.clearWindow();artwork.reset();models.clear();if(wall!=null)wall.releaseContentBackground(this);for(int i=0;i<rail.getChildCount();i++)if(rail.getChildViewHolder(rail.getChildAt(i)) instanceof NativeHistoryPosterAdapter.Holder h)adapter.onViewRecycled(h);}
    public boolean handleBack(){
        if(released||!active||adapter.deleting||!rail.hasFocus()||rail.getSelectedPosition()<=0||adapter.getItemCount()==0)return false;
        long token=revision;
        rail.setSelectedPosition(0,new ViewHolderTask(){@Override public void run(RecyclerView.ViewHolder holder){
            if(token==revision&&active&&!released&&holder.getBindingAdapterPosition()==0)holder.itemView.requestFocus();
        }});
        return true;
    }
    @Override public boolean dispatchKeyEvent(KeyEvent event){
        if(event.getKeyCode()==KeyEvent.KEYCODE_BACK&&(backFromCards||(!adapter.deleting&&rail.hasFocus()&&rail.getSelectedPosition()>0))){
            if(event.getAction()==KeyEvent.ACTION_DOWN)backFromCards=true;
            if(event.getAction()==KeyEvent.ACTION_UP){backFromCards=false;if(!event.isCanceled())handleBack();}
            return true;
        }
        if(event.getAction()==KeyEvent.ACTION_DOWN){int key=event.getKeyCode();
            if(event.getRepeatCount()>0&&(key==KeyEvent.KEYCODE_DPAD_UP||key==KeyEvent.KEYCODE_DPAD_DOWN))return true;
            if(rail.hasFocus()&&((key==KeyEvent.KEYCODE_DPAD_LEFT&&selectedPosition==0)||(key==KeyEvent.KEYCODE_DPAD_RIGHT&&selectedPosition==adapter.getItemCount()-1)))return true;
            if(key==KeyEvent.KEYCODE_DPAD_UP&&rail.hasFocus()){resume.requestFocus();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_UP&&resume.hasFocus()){deleteButton.requestFocus();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_DOWN&&(deleteButton.hasFocus()||reportButton.hasFocus())){if(current!=null)resume.requestFocus();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_DOWN&&resume.hasFocus()){restore(selectedPosition);return true;}
            if(key==KeyEvent.KEYCODE_DPAD_RIGHT&&deleteButton.hasFocus()){reportButton.requestFocus();return true;}
            if(key==KeyEvent.KEYCODE_DPAD_LEFT&&reportButton.hasFocus()){deleteButton.requestFocus();return true;}
        }return super.dispatchKeyEvent(event); // First-card and delete-mode BACK still belong to HistoryActivity.
    }
    @Override protected void onMeasure(int widthSpec,int heightSpec){
        int width=MeasureSpec.getSize(widthSpec),height=MeasureSpec.getSize(heightSpec);setMeasuredDimension(width,height);
        int w=Math.max(1,width-safeLeft-safeRight),h=Math.max(1,height-safeTop-safeBottom);HomePreviewGeometry g=HomePreviewGeometry.calculate(w,h,getResources().getDisplayMetrics().density);adapter.geometry(g);
        place(leftShade,0,0,width,height,false);place(bottomShade,0,0,width,height,false);
        place(top,g.gutter(),g.top(),w-2*g.gutter(),g.controlHeight(),true);
        int resumeHeight=g.controlHeight(),resumeTop=g.railTop()-resumeHeight-dp(10);
        title.setTextSize(HomePreviewGeometry.clamp(h/getResources().getDisplayMetrics().density*.074f,24,48));
        place(hero,g.gutter(),Math.round(h*.16f),Math.round(w*.52f),Math.max(0,resumeTop-Math.round(h*.16f)-dp(8)),true);
        place(resumeRow,g.gutter(),resumeTop,Math.round(w*.55f),resumeHeight,true);
        int ph=Math.round(Math.min(h*.39f,dp(300))),pw=Math.round(ph*2f/3);place(poster,w-Math.round(w*.08f)-pw,Math.round(h*.145f),pw,ph,true);
        rail.setPadding(g.gutter(),g.focusSpace(),g.gutter(),g.focusSpace());place(rail,0,g.railTop(),w,g.railHeight(),true);
        place(empty,g.gutter(),Math.round(h*.35f),w-2*g.gutter(),dp(80),true);
    }
    private void place(View view,int x,int y,int width,int height,boolean safe){if(safe){x+=safeLeft;y+=safeTop;}view.setTag(R.id.nhp_hero,new int[]{x,y});view.measure(MeasureSpec.makeMeasureSpec(Math.max(0,width),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(0,height),MeasureSpec.EXACTLY));}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){for(int i=0;i<getChildCount();i++){View child=getChildAt(i);int[] pos=(int[])child.getTag(R.id.nhp_hero);child.layout(pos[0],pos[1],pos[0]+child.getMeasuredWidth(),pos[1]+child.getMeasuredHeight());}}
}
