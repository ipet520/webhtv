package com.fongmi.android.tv.ui.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.fongmi.android.tv.Product;
import com.fongmi.android.tv.R;
import com.fongmi.android.tv.bean.History;
import com.fongmi.android.tv.databinding.ActivityHistoryBinding;
import com.fongmi.android.tv.event.ConfigEvent;
import com.fongmi.android.tv.event.RefreshEvent;
import com.fongmi.android.tv.setting.Setting;
import com.fongmi.android.tv.theme.WebHtvAlertDialogBuilder;
import com.fongmi.android.tv.ui.adapter.HistoryAdapter;
import com.fongmi.android.tv.ui.base.BaseActivity;
import com.fongmi.android.tv.ui.custom.FocusRestorer;
import com.fongmi.android.tv.ui.custom.SpaceItemDecoration;
import com.fongmi.android.tv.ui.dialog.ViewingReportRangeDialog;
import com.fongmi.android.tv.ui.homepreview.NativeHistoryBrowseView;
import com.fongmi.android.tv.ui.custom.CustomWallView;
import android.view.ViewGroup;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HistoryActivity extends BaseActivity implements HistoryAdapter.OnClickListener {

    private ActivityHistoryBinding mBinding;
    private HistoryAdapter mAdapter;
    private NativeHistoryBrowseView mImmersive;
    private final FocusRestorer mFocusRestorer = new FocusRestorer();

    public static void start(Activity activity) {
        activity.startActivity(new Intent(activity, HistoryActivity.class));
    }

    @Override
    protected ViewBinding getBinding() {
        if (Setting.getLeanbackHomeMode() == Setting.LeanbackHomeMode.IMMERSIVE) {
            mImmersive = new NativeHistoryBrowseView(this, new NativeHistoryBrowseView.Actions() {
                public void open(History item) { onItemClick(item); }
                public void details(History item) { onItemDetails(item); }
                public void delete(History item, int position) { onItemDelete(item, position); }
                public boolean longClick() { return onLongClick(); }
                public void manage() { onDelete(); }
                public void report() { onReport(); }
            });
            return () -> mImmersive;
        }
        return mBinding = ActivityHistoryBinding.inflate(getLayoutInflater());
    }

   @Override
   protected void initView(Bundle savedInstanceState) {
       if (mImmersive != null) {
           mAdapter = new HistoryAdapter(this);
           ViewGroup content = findViewById(android.R.id.content);
           for (int i = 0; i < content.getChildCount(); i++)
               if (content.getChildAt(i) instanceof CustomWallView wall) mImmersive.attachBackground(wall);
           return;
       }
       setRecyclerView();
       getHistory();
        mBinding.deleteButton.setOnClickListener(v -> onDelete());
       mBinding.reportButton.setOnClickListener(v -> onReport());
   }

   private void onReport() {
       ViewingReportRangeDialog.create(this)
               .callback(range -> ViewingReportActivity.start(this, range))
               .show();
   }

    static int focusTopInset(int height, float density) {
        return (int) Math.ceil(height * 0.05f) + (int) Math.ceil(8 * density);
    }

    private void onDelete() {
        if (mAdapter.isDelete()) {
            new WebHtvAlertDialogBuilder(this)
                .setTitle(R.string.dialog_delete_record)
                .setMessage(Setting.isGlobalHistoryEnabled() ? R.string.dialog_delete_global_history : R.string.dialog_delete_history)
                .setNegativeButton(R.string.dialog_negative, null)
                .setPositiveButton(R.string.dialog_positive, (dialog, which) -> {
                    if (mImmersive != null) {
                        // Existing no-argument adapter clear owns History.deleteForDisplay().
                        mAdapter.clear();
                        getHistory();
                        return;
                    }
                    mAdapter.clear(() -> {
                    mBinding.progressLayout.showContent(true, mAdapter.getItemCount());
                    if (mAdapter.getItemCount() == 0) mAdapter.setDelete(false);
                    });
                })
                .show();
        } else if (mAdapter.getItemCount() > 0) {
            mAdapter.setDelete(true);
            if (mImmersive != null) mImmersive.submit(mAdapter.getItems(), true);
        }
    }

   private void setRecyclerView() {
        mBinding.recycler.setHasFixedSize(true);
        mBinding.recycler.setItemAnimator(null);
        mBinding.recycler.setAdapter(mAdapter = new HistoryAdapter(this));
        mBinding.recycler.setLayoutManager(new GridLayoutManager(this, Product.getColumn()));
        // Reserve the 1.1x focus scale overhang inside the clipping ProgressLayout.
        mBinding.recycler.addOnLayoutChangeListener((view, l, t, r, b, ol, ot, or, ob) -> {
            View first = mBinding.recycler.getChildAt(0);
            if (first == null || first.getHeight() == 0) return;
            int inset = focusTopInset(first.getHeight(), getResources().getDisplayMetrics().density);
            if (mBinding.recycler.getPaddingTop() != inset) {
                mBinding.recycler.setPadding(mBinding.recycler.getPaddingLeft(), inset,
                        mBinding.recycler.getPaddingRight(), mBinding.recycler.getPaddingBottom());
            }
        });
        mBinding.recycler.addItemDecoration(new SpaceItemDecoration(Product.getColumn(), 16));
    }

    private void getHistory() {
        int position = focusedPosition();
        History focused = position < 0 || position >= mAdapter.getItemCount() ? null : mAdapter.getItem(position);
        mAdapter.setItems(History.getForDisplay(), () -> {
            if (mImmersive != null) {
                if (mAdapter.getItemCount() == 0) mAdapter.setDelete(false);
                mImmersive.submit(mAdapter.getItems(), mAdapter.isDelete());
                return;
            }
            mBinding.progressLayout.showContent(true, mAdapter.getItemCount());
            if (mAdapter.getItemCount() == 0) mAdapter.setDelete(false);
            else if (focused != null) restoreFocus(reposition(focused, position));
        });
    }

    // 刷新会按 createTime 重排，优先按条目身份重新定位；条目已不在新列表里（被别处删掉）
    // 就退回原位置，让焦点留在附近而不是掉给右上角的删除按钮
    private int reposition(History focused, int position) {
        int found = mAdapter.getItems().indexOf(focused);
        if (found >= 0) return found;
        return FocusRestorer.nextPosition(position, mAdapter.getItemCount(), Product.getColumn());
    }

    private int focusedPosition() {
        if (mImmersive != null) return mImmersive.focusedPosition();
        if (isFinishing() || isDestroyed() || !mBinding.recycler.hasFocus()) return RecyclerView.NO_POSITION;
        View focus = mBinding.recycler.getFocusedChild();
        return focus == null ? RecyclerView.NO_POSITION : mBinding.recycler.getChildAdapterPosition(focus);
    }

    // 只在列表原本持有焦点时恢复：条目被移除时 RecyclerView 已 clearFocus，
    // 焦点被窗口兜底给了右上角的删除按钮，需要主动交回列表
    private void restoreFocus(int position) {
        if (isFinishing() || isDestroyed()) return;
        if (mImmersive != null) { mImmersive.restore(position); return; }
        mFocusRestorer.restore(mBinding.recycler, position);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onConfigEvent(ConfigEvent event) {
        if (event.isVod() && mAdapter != null) {
            mAdapter.notifyDataSetChanged();
            if (mImmersive != null) mImmersive.submit(mAdapter.getItems(), mAdapter.isDelete());
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onRefreshEvent(RefreshEvent event) {
        if (event.getType() == RefreshEvent.Type.HISTORY) getHistory();
    }

    @Override
    public void onItemClick(History item) {
        HistoryResumeCoordinator.open(this, item);
    }

    void onItemDetails(History item) {
        if (item.isCurrentSourceAvailable()) {
            VideoActivity.start(this, item.getSiteKey(), item.getVodId(), item.getVodName(),
                    item.getVodPic(), null, item.getWallPic());
        } else {
            // Ordinary source search, without a history-resume payload or forced config switch.
            SearchActivity.start(this, item.getVodName(), null, item.getVodPic(), item.getWallPic());
        }
    }

    @Override
    public void onItemDelete(History item, int position) {
        // diff 挂起时 holder 的绑定下标会失效，退回当前聚焦位置（遥控下点击必然带焦点）
        int target = position >= 0 ? position : focusedPosition();
        mAdapter.remove(item.deleteDisplayItem(), () -> {
            if (mImmersive != null) {
                if (mAdapter.getItemCount() == 0) mAdapter.setDelete(false);
                mImmersive.submit(mAdapter.getItems(), mAdapter.isDelete());
                return;
            }
            mBinding.progressLayout.showContent(true, mAdapter.getItemCount());
            if (mAdapter.getItemCount() == 0) mAdapter.setDelete(false);
            else restoreFocus(FocusRestorer.nextPosition(target, mAdapter.getItemCount(), Product.getColumn()));
        });
    }

    @Override
    public boolean onLongClick() {
        mFocusRestorer.cancel();
        mAdapter.setDelete(!mAdapter.isDelete());
        if (mImmersive != null) mImmersive.submit(mAdapter.getItems(), mAdapter.isDelete());
        return true;
    }

    @Override
    protected void onStop() {
        mFocusRestorer.cancel();
        if (mImmersive != null) mImmersive.active(false);
        super.onStop();
    }

    @Override protected void onResume() {
        super.onResume();
        if (mImmersive != null) { mImmersive.refreshTheme(); mImmersive.enter(); getHistory(); }
    }

    @Override protected void onDestroy() {
        if (mImmersive != null) mImmersive.release();
        super.onDestroy();
    }

    @Override protected boolean customWallMotion() { return mImmersive == null; }

    @Override
    protected void onBackInvoked() {
        mFocusRestorer.cancel();
        if (mAdapter.isDelete()) {
            mAdapter.setDelete(false);
            if (mImmersive != null) mImmersive.submit(mAdapter.getItems(), false);
        }
        else if (mImmersive == null || !mImmersive.handleBack()) super.onBackInvoked();
    }

}
