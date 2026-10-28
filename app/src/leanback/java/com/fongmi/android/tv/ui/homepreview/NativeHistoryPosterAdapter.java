package com.fongmi.android.tv.ui.homepreview;

import android.view.*;
import android.widget.*;
import androidx.recyclerview.widget.RecyclerView;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.AdapterVodBinding;
import com.fongmi.android.tv.ui.adapter.BaseDiffAdapter;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.utils.ImgUtil;
import java.util.*;
import java.util.function.*;

/** Uses the accepted home poster holder/geometry; History remains the bound business object. */
final class NativeHistoryPosterAdapter extends BaseDiffAdapter<History,NativeHistoryPosterAdapter.Holder> {
    HomePreviewGeometry geometry;
    boolean deleting;
    final BiConsumer<History,Integer> focus;
    final Consumer<History> click;
    final BiConsumer<History,Integer> delete;
    final BooleanSupplier longClick;
    NativeHistoryPosterAdapter(BiConsumer<History,Integer> focus,Consumer<History> click,BiConsumer<History,Integer> delete,BooleanSupplier longClick){this.focus=focus;this.click=click;this.delete=delete;this.longClick=longClick;setHasStableIds(true);}
    static long identity(History history){long hash=0xcbf29ce484222325L;for(char c:history.getDisplayIdentity().toCharArray())hash=(hash^c)*0x100000001b3L;return hash;}
    @Override public long getItemId(int position){return identity(getItem(position));}
    void geometry(HomePreviewGeometry next){if(next.equals(geometry))return;geometry=next;notifyItemRangeChanged(0,getItemCount(),"geometry");}
    void deleting(boolean next){if(deleting==next)return;deleting=next;notifyItemRangeChanged(0,getItemCount(),"delete");}
    @Override public Holder onCreateViewHolder(ViewGroup parent,int type){return new Holder(parent);}
    @Override public void onBindViewHolder(Holder h,int position,List<Object> payloads){if(payloads.contains("delete")){state(h,getItem(position));return;}onBindViewHolder(h,position);}
    @Override public void onBindViewHolder(Holder h,int position){
        History item=getItem(position);var v=h.card;v.applyTheme(ThemeController.current());v.title.setText(item.getVodName());
        v.rating.setVisibility(View.GONE);v.quality.setText(item.getSiteName());v.quality.setVisibility(item.getSiteVisible());
        h.details.setText(item.getVodRemarks());
        h.details.setTextColor(ThemeController.current().colorOnSurface());
        state(h,item);
        h.itemView.setOnClickListener(view->{int p=h.getBindingAdapterPosition();if(p<0)return;if(deleting)delete.accept(getItem(p),p);else click.accept(getItem(p));});
        h.itemView.setOnLongClickListener(view->longClick.getAsBoolean());
        h.itemView.setOnFocusChangeListener((view,focused)->{view.animate().cancel();view.animate().scaleX(focused?1.04f:1).scaleY(focused?1.04f:1).setDuration(120).start();v.stroke.setAlpha(focused?1:0);int p=h.getBindingAdapterPosition();if(focused&&p>=0)focus.accept(getItem(p),p);});
        h.itemView.setScaleX(h.itemView.hasFocus()?1.04f:1);h.itemView.setScaleY(h.itemView.hasFocus()?1.04f:1);v.stroke.setAlpha(h.itemView.hasFocus()?1:0);
        if(geometry!=null){h.itemView.setLayoutParams(new RecyclerView.LayoutParams(geometry.posterWidth(),geometry.posterHeight()+geometry.captionHeight()));v.poster.setLayoutParams(new LinearLayout.LayoutParams(geometry.posterWidth(),geometry.posterHeight()));v.title.setLayoutParams(new LinearLayout.LayoutParams(-1,geometry.captionHeight()));}
        ImgUtil.load(item.getVodName(),item.getVodPic(),v.image,true,geometry==null?240:geometry.posterWidth(),geometry==null?360:geometry.posterHeight());
        h.itemView.setContentDescription(item.getVodName()+" "+item.getVodRemarks()+" "+item.getPlaybackTimeText()+" "+item.getSiteName());
    }
    static int progress(History item){return item.getDuration()>0?(int)(Math.min(Math.max(0,item.getPosition()),item.getDuration())/(double)item.getDuration()*10000):0;}
    private void state(Holder h,History item){h.remove.setVisibility(deleting?View.VISIBLE:View.GONE);}
    @Override public void onViewRecycled(Holder h){ImgUtil.clear(h.card.image);h.itemView.animate().cancel();h.card.image.setImageDrawable(null);h.itemView.setScaleX(1);h.itemView.setScaleY(1);h.card.stroke.setAlpha(0);}
    static final class Holder extends RecyclerView.ViewHolder {
        final HomePreviewPosterAdapter.Holder card;
        final TextView details;
        final View remove;
        Holder(ViewGroup parent){this(new HomePreviewPosterAdapter.Holder(parent.getContext()));}
        private Holder(HomePreviewPosterAdapter.Holder card){super(card.itemView);this.card=card;var context=itemView.getContext();
            details=new TextView(context);details.setSingleLine();details.setTextSize(9);details.setEllipsize(android.text.TextUtils.TruncateAt.END);details.setPadding(6,3,6,3);details.setBackground(HomePreviewStyles.shape(details,HomePreviewStyles.alpha(ThemeController.current().colorSurface(),.82f),0,0));
            FrameLayout.LayoutParams info=new FrameLayout.LayoutParams(-1,-2,Gravity.BOTTOM);card.poster.addView(details,info);
            // Reuse the classic history delete view, including its trash icon, scrim and shape.
            remove=AdapterVodBinding.inflate(LayoutInflater.from(context)).delete;
            ((ViewGroup)remove.getParent()).removeView(remove);
            card.poster.addView(remove,new FrameLayout.LayoutParams(-1,-1));
        }
    }
}
