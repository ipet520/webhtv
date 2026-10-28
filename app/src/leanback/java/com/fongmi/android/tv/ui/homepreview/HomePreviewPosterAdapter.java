package com.fongmi.android.tv.ui.homepreview;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Outline;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.fongmi.android.tv.R;

import java.util.List;
import java.util.function.Consumer;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.theme.ThemeTokens;
import androidx.recyclerview.widget.DiffUtil;

/** Preview-specific native cards; no dependency on business presenters or image/network loaders. */
final class HomePreviewPosterAdapter extends RecyclerView.Adapter<HomePreviewPosterAdapter.Holder> {
    private List<HomePreviewData.Card> cards;
    private final java.util.function.BiConsumer<HomePreviewData.Card,Integer> onFocus;
    int binds;
    private HomePreviewGeometry geometry;
    Consumer<HomePreviewData.Card> click = card -> {};
    java.util.function.Predicate<HomePreviewData.Card> longClick = card -> false;

    HomePreviewPosterAdapter(List<HomePreviewData.Card> cards, java.util.function.BiConsumer<HomePreviewData.Card,Integer> onFocus) {
        this.cards = cards;
        this.onFocus = onFocus;
        setHasStableIds(true);
    }

    void setGeometry(HomePreviewGeometry next) {
        if (next.equals(geometry)) return;
        geometry = next;
        notifyItemRangeChanged(0, getItemCount(), next);
    }

    void submitCards(List<HomePreviewData.Card> next) {
        if (cards == next) return;
        List<HomePreviewData.Card> previous = cards;
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            public int getOldListSize() { return previous.size(); }
            public int getNewListSize() { return next.size(); }
            public boolean areItemsTheSame(int a, int b) { return previous.get(a).id() == next.get(b).id(); }
            public boolean areContentsTheSame(int a, int b) { return previous.get(a).equals(next.get(b)); }
        });
        cards = next; diff.dispatchUpdatesTo(this);
    }

    @Override public long getItemId(int position) { return cards.get(position).id(); }
    @Override public int getItemCount() { return cards.size(); }

    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        return new Holder(parent.getContext());
    }

    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        binds++;
        holder.applyTheme(ThemeController.current());
        HomePreviewData.Card card = cards.get(position);
        holder.itemView.animate().cancel();
        holder.itemView.setScaleX(holder.itemView.hasFocus() ? 1.04f : 1);
        holder.itemView.setScaleY(holder.itemView.hasFocus() ? 1.04f : 1);
        if(card.vod()!=null) com.fongmi.android.tv.utils.ImgUtil.load(card.title(),card.image(),holder.image,true,
                geometry==null?240:geometry.posterWidth(),geometry==null?360:geometry.posterHeight());
        else holder.image.setImageResource(card.poster());
        holder.itemView.setOnClickListener(v->click.accept(card));
        holder.itemView.setOnLongClickListener(v->longClick.test(card));
        holder.title.setText(card.title());
        holder.rating.setText("★ " + card.rating());
        holder.rating.setVisibility(card.rating().isEmpty()?View.GONE:View.VISIBLE);
        holder.quality.setText(card.quality());
        holder.quality.setVisibility(card.quality().isEmpty()?View.GONE:View.VISIBLE);
        holder.itemView.setContentDescription(holder.itemView.getContext().getString(R.string.nhp_card_description, card.title(), card.rating(), card.quality()));
        holder.itemView.setOnFocusChangeListener((v, focused) -> {
            holder.stroke.setAlpha(focused ? 1 : 0);
            v.animate().cancel();
            v.animate().scaleX(focused ? 1.04f : 1).scaleY(focused ? 1.04f : 1).setDuration(120).start();
            int current=holder.getBindingAdapterPosition();
            if (focused&&current!=RecyclerView.NO_POSITION) onFocus.accept(card,current);
        });
        holder.stroke.setAlpha(holder.itemView.hasFocus() ? 1 : 0);
        if (geometry != null) {
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(geometry.posterWidth(), geometry.posterHeight() + geometry.captionHeight()));
            holder.poster.setLayoutParams(new LinearLayout.LayoutParams(geometry.posterWidth(), geometry.posterHeight()));
            holder.title.setLayoutParams(new LinearLayout.LayoutParams(geometry.posterWidth(), geometry.captionHeight()));
            float density = holder.itemView.getResources().getDisplayMetrics().density;
            float captionSp = HomePreviewGeometry.clamp(geometry.posterHeight() / density * .09f, 12, 17);
            holder.title.setTextSize(captionSp);
            holder.rating.setMaxWidth(Math.max(1, Math.round(geometry.posterWidth() * .43f) - dp(holder.itemView.getContext(), 8)));
            holder.quality.setMaxWidth(Math.max(1, Math.round(geometry.posterWidth() * .57f) - dp(holder.itemView.getContext(), 8)));
        }
    }

    @Override public void onViewRecycled(@NonNull Holder holder) {
        com.fongmi.android.tv.utils.ImgUtil.clear(holder.image);
        holder.itemView.setOnClickListener(null);holder.itemView.setOnLongClickListener(null);
        holder.itemView.animate().cancel();
        holder.itemView.setOnFocusChangeListener(null);
        holder.itemView.setScaleX(1);
        holder.itemView.setScaleY(1);
        holder.stroke.setAlpha(0);
        holder.image.setImageDrawable(null);
    }

    static final class Holder extends RecyclerView.ViewHolder {
        final FrameLayout poster;
        final ImageView image;
        final View stroke;
        final TextView title;
        final TextView rating;
        final TextView quality;

        Holder(Context context) {
            super(new LinearLayout(context));
            LinearLayout root = (LinearLayout) itemView;
            root.setOrientation(LinearLayout.VERTICAL);
            root.setFocusable(true);
            root.setFocusableInTouchMode(true);
            root.setClipChildren(false);
            root.setClipToPadding(false);
            root.setDescendantFocusability(ViewGroup.FOCUS_BLOCK_DESCENDANTS);
            root.setLayoutParams(new RecyclerView.LayoutParams(dp(context, 120), dp(context, 205)));
            poster = new FrameLayout(context);
            root.addView(poster, new LinearLayout.LayoutParams(dp(context, 120), dp(context, 180)));
            poster.setOutlineProvider(new ViewOutlineProvider() {
                @Override public void getOutline(View view, Outline outline) {
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), dp(context, 8));
                }
            });
            poster.setClipToOutline(true);
            image = new ImageView(context);
            image.setTag("webhtv:ignore");
            image.setScaleType(ImageView.ScaleType.CENTER_CROP);
            poster.addView(image, new FrameLayout.LayoutParams(-1, -1));
            rating = badge(context);
            quality = badge(context);
            rating.setTextColor(ThemeController.current().colorWarning());
            FrameLayout.LayoutParams left = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.START);
            left.setMargins(dp(context, 5), dp(context, 7), 0, 0);
            poster.addView(rating, left);
            FrameLayout.LayoutParams right = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
            right.setMargins(0, dp(context, 7), dp(context, 5), 0);
            poster.addView(quality, right);
            stroke = new View(context);
            GradientDrawable border = new GradientDrawable();
            border.setColor(Color.TRANSPARENT);
            border.setCornerRadius(dp(context, 8));
            border.setStroke(dp(context, 2), ThemeController.current().colorFocus());
            stroke.setBackground(border);
            stroke.setAlpha(0);
            poster.addView(stroke, new FrameLayout.LayoutParams(-1, -1));
            title = new TextView(context);
            title.setGravity(Gravity.CENTER);
            title.setTextColor(ThemeController.current().colorOnSurface());
            title.setSingleLine(true);
            title.setEllipsize(TextUtils.TruncateAt.END);
            title.setTextSize(14);
            root.addView(title, new LinearLayout.LayoutParams(-1, dp(context, 25)));
            applyTheme(ThemeController.current());
        }

        void applyTheme(ThemeTokens t) {
            title.setTextColor(t.colorOnSurface());
            rating.setTextColor(t.colorWarning()); quality.setTextColor(t.colorOnSurface());
            for (TextView badge : new TextView[]{rating, quality}) {
                GradientDrawable bg = new GradientDrawable();
                bg.setColor(HomePreviewStyles.alpha(t.colorSurface(), .96f)); bg.setCornerRadius(dp(itemView.getContext(), 5));
                badge.setBackground(bg);
            }
            GradientDrawable border = new GradientDrawable(); border.setColor(Color.TRANSPARENT);
            border.setCornerRadius(dp(itemView.getContext(), 8)); border.setStroke(dp(itemView.getContext(), 2), t.colorFocus());
            stroke.setBackground(border);
        }

        private static TextView badge(Context context) {
            TextView view = new TextView(context);
            view.setTextColor(ThemeController.current().colorOnSurface());
            view.setTextSize(9);
            view.setTypeface(null, Typeface.BOLD);
            view.setSingleLine(true);
            view.setEllipsize(TextUtils.TruncateAt.END);
            view.setPadding(dp(context, 4), dp(context, 2), dp(context, 4), dp(context, 2));
            GradientDrawable background = new GradientDrawable();
            background.setColor(HomePreviewStyles.alpha(ThemeController.current().colorSurface(), .96f));
            background.setCornerRadius(dp(context, 5));
            view.setBackground(background);
            return view;
        }
    }

    static int dp(Context context, float value) {
        return Math.round(context.getResources().getDisplayMetrics().density * value);
    }
}

