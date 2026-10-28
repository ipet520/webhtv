package com.fongmi.android.tv.ui.homepreview;

import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.fongmi.android.tv.bean.Func;
import com.fongmi.android.tv.bean.HomeButton;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.ui.presenter.FuncPresenter;
import java.util.List;
import java.util.function.IntConsumer;

/** Presentation adapter only. HomeButton owns visibility/order; HomeActivity owns dispatch. */
final class HomeBrowseFunctionAdapter extends RecyclerView.Adapter<HomeBrowseFunctionAdapter.Holder> {
    private record Entry(int id, Func func) {}
    private List<Entry> items = List.of();
    private FuncPresenter.OnClickListener listener;
    private final IntConsumer focus;
    HomeBrowseFunctionAdapter(IntConsumer focus) { this.focus = focus; setHasStableIds(true); }
    boolean submit(List<HomeButton> buttons, FuncPresenter.OnClickListener listener) {
        this.listener = listener;
        List<Entry> next = buttons.stream().map(b -> new Entry(b.getId(), Func.create(b.getResId()))).toList();
        List<Entry> old = items;
        boolean same = old.size() == next.size();
        for (int i = 0; same && i < old.size(); i++) same = old.get(i).id == next.get(i).id && old.get(i).func.isSameContent(next.get(i).func);
        if (same) return false;
        DiffUtil.DiffResult diff = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            public int getOldListSize() { return old.size(); }
            public int getNewListSize() { return next.size(); }
            public boolean areItemsTheSame(int a, int b) { return old.get(a).id == next.get(b).id; }
            public boolean areContentsTheSame(int a, int b) { return old.get(a).func.isSameContent(next.get(b).func); }
        });
        items = next; diff.dispatchUpdatesTo(this); return true;
    }
    int positionOf(long id) { for (int i = 0; i < items.size(); i++) if (items.get(i).id == id) return i; return -1; }
    @Override public long getItemId(int position) { return items.get(position).id; }
    @Override public int getItemCount() { return items.size(); }
    @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
        Button b = new Button(parent.getContext()); b.setAllCaps(false); b.setTextSize(16);
        b.setMinWidth(0); b.setMinimumWidth(0); b.setMinHeight(0); b.setMinimumHeight(0);
        b.setGravity(Gravity.CENTER); int padding = HomePreviewPosterAdapter.dp(parent.getContext(), 10);
        b.setPadding(padding, 0, padding, 0); b.setLayoutParams(new RecyclerView.LayoutParams(-2, -1));
        HomePreviewStyles.function(b); return new Holder(b);
    }
    @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
        Entry item = items.get(position); holder.button.setText(item.func.getText());
        HomePreviewStyles.function(holder.button, ThemeController.current());
        holder.button.setOnFocusChangeListener((v, focused) -> {
            HomePreviewStyles.functionFocus(v,focused);
            int positionNow=holder.getBindingAdapterPosition();
            if (focused&&positionNow!=RecyclerView.NO_POSITION) focus.accept(positionNow);
        });
        holder.button.setOnClickListener(v -> { if (listener != null) listener.onItemClick(item.func); });
        holder.button.setOnLongClickListener(v -> listener != null && listener.onLongClick(item.func));
    }
    @Override public void onViewRecycled(@NonNull Holder holder) {
        holder.button.animate().cancel();holder.button.setScaleX(1);holder.button.setScaleY(1);
        holder.button.setOnFocusChangeListener(null); holder.button.setOnClickListener(null); holder.button.setOnLongClickListener(null);
    }
    static final class Holder extends RecyclerView.ViewHolder {
        final Button button;
        Holder(Button b) { super(b); button = b; }
    }
}
