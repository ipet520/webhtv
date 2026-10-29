package com.fongmi.android.tv.ui.custom;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.util.TypedValue;

import androidx.core.content.ContextCompat;

import com.fongmi.android.tv.R;
import com.fongmi.android.tv.databinding.ViewEmptyBinding;
import com.fongmi.android.tv.theme.SettingsValuePresentation;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.databinding.ViewProgressBinding;

import java.util.ArrayList;
import java.util.List;

public class ProgressLayout extends RelativeLayout {

    private static final String TAG_PROGRESS = "ProgressLayout.TAG_PROGRESS";

    public enum State {
        CONTENT, PROGRESS, EMPTY, ERROR
    }

    private List<View> mContentViews;
    private View mProgressView;
    private View mEmptyView;
    private ViewEmptyBinding mEmptyBinding;
    private int mErrorTitleRes;
    private CharSequence mErrorReason;
    private int mErrorRetryRes;
    private OnClickListener mErrorRetryListener;
    private State mState;

    public ProgressLayout(Context context) {
        super(context);
    }

    public ProgressLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ProgressLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mState = State.CONTENT;
        mContentViews = new ArrayList<>();
    }

    private void ensureEmptyView() {
        if (mEmptyView != null) return;
        mEmptyBinding = ViewEmptyBinding.inflate(LayoutInflater.from(getContext()));
        mEmptyView = mEmptyBinding.getRoot();
        mEmptyView.setTag(TAG_PROGRESS);
        mEmptyView.setVisibility(GONE);
        addView(mEmptyView, centerParams());
    }

    private void ensureProgressView() {
        if (mProgressView != null) return;
        mProgressView = ViewProgressBinding.inflate(LayoutInflater.from(getContext())).getRoot();
        mProgressView.setTag(TAG_PROGRESS);
        mProgressView.setVisibility(GONE);
        addView(mProgressView, centerParams());
    }

    private LayoutParams centerParams() {
        LayoutParams params = new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(CENTER_IN_PARENT);
        return params;
    }

    @Override
    public void addView(View child, int index, ViewGroup.LayoutParams params) {
        super.addView(child, index, params);
        if (child.getTag() == null || !child.getTag().equals(TAG_PROGRESS)) {
            mContentViews.add(child);
        }
    }

    /** Adds a child that must not follow CONTENT/PROGRESS/EMPTY visibility changes. */
    public void addOverlayView(View child, ViewGroup.LayoutParams params) {
        addView(child, params);
        mContentViews.remove(child);
    }

    public void showProgress() {
        switchState(State.PROGRESS);
    }

    public void showEmpty() {
        resetEmptyContent();
        switchState(State.EMPTY);
    }

    public void showEmptyContent(int title, int description, int action, OnClickListener listener) {
        ensureEmptyView();
        restoreEmptyChrome();
        mEmptyBinding.text.setText(title);
        mEmptyBinding.description.setText(description);
        mEmptyBinding.description.setVisibility(VISIBLE);
        mEmptyBinding.action.setText(action);
        mEmptyBinding.action.setOnClickListener(listener);
        mEmptyBinding.action.setVisibility(listener == null ? GONE : VISIBLE);
        switchState(State.EMPTY);
        if (listener != null) mEmptyBinding.action.requestFocus();
    }

    public void showError(int titleRes, CharSequence reason, int retryRes, OnClickListener retry) {
        mErrorTitleRes = titleRes;
        mErrorReason = reason;
        mErrorRetryRes = retryRes;
        mErrorRetryListener = retry;
        if (mState == State.ERROR) applyErrorContent();
        else switchState(State.ERROR);
    }

    public boolean isError() {
        return mState == State.ERROR;
    }

    private void applyErrorContent() {
        if (mEmptyView == null) return;
        mEmptyBinding.illustration.setVisibility(GONE);
        SettingsValuePresentation.error(mEmptyBinding.text, ThemeController.current());
        if (mErrorTitleRes != 0) mEmptyBinding.text.setText(mErrorTitleRes);
        if (mErrorReason != null && mErrorReason.length() > 0) {
            mEmptyBinding.description.setText(mErrorReason);
            mEmptyBinding.description.setVisibility(VISIBLE);
        } else {
            mEmptyBinding.description.setVisibility(GONE);
        }
        if (mErrorRetryRes != 0 && mErrorRetryListener != null) {
            mEmptyBinding.action.setText(mErrorRetryRes);
            mEmptyBinding.action.setOnClickListener(mErrorRetryListener);
            mEmptyBinding.action.setVisibility(VISIBLE);
            mEmptyBinding.action.requestFocus();
        } else {
            mEmptyBinding.action.setOnClickListener(null);
            mEmptyBinding.action.setVisibility(GONE);
        }
    }

    private void restoreEmptyChrome() {
        mEmptyBinding.illustration.setVisibility(VISIBLE);
        mEmptyBinding.text.setTextColor(ContextCompat.getColor(getContext(), R.color.webhtv_on_wallpaper));
        mEmptyBinding.text.setBackground(null);
    }

    private void resetEmptyContent() {
        ensureEmptyView();
        restoreEmptyChrome();
        mEmptyBinding.text.setText(com.fongmi.android.tv.R.string.error_empty);
        mEmptyBinding.description.setText("");
        mEmptyBinding.description.setVisibility(GONE);
        mEmptyBinding.action.setText("");
        mEmptyBinding.action.setOnClickListener(null);
        mEmptyBinding.action.setVisibility(GONE);
    }

    public void showContent() {
        switchState(State.CONTENT);
    }

    // 强制把内容视图重新隐藏（仅在 PROGRESS/EMPTY 态有意义）：
    // 用于内容填充（如 setText）在 loading 期间把部分子视图改回 VISIBLE 后，重新压回隐藏，避免泄漏
    public void hideContent() {
        if (mState == State.CONTENT) return;
        setContentVisibility(false);
    }

    public void showContent(boolean flag, int size) {
        if (flag && size == 0) showEmpty();
        else showContent();
    }

    public boolean isProgress() {
        return mState == State.PROGRESS;
    }

    public boolean isContent() {
        return mState == State.CONTENT;
    }

    public boolean isEmpty() {
        return mState == State.EMPTY;
    }

    public void switchState(State state) {
        if (mState == state) return;
        State previous = mState;
        mState = state;
        if (state == State.EMPTY || state == State.ERROR) applyEmptyStateScrim();
        else if (previous == State.EMPTY || previous == State.ERROR) setBackground(null);
        switch (state) {
            case CONTENT:
                if (mEmptyView != null) mEmptyView.setVisibility(GONE);
                if (mProgressView != null) mProgressView.setVisibility(GONE);
                setContentVisibility(true);
                break;
            case PROGRESS:
                ensureProgressView();
                if (mEmptyView != null) mEmptyView.setVisibility(GONE);
                mProgressView.setVisibility(VISIBLE);
                setContentVisibility(false);
                break;
            case EMPTY:
                ensureEmptyView();
                restoreEmptyChrome();
                mEmptyView.setVisibility(VISIBLE);
                if (mProgressView != null) mProgressView.setVisibility(GONE);
                setContentVisibility(false);
                break;
            case ERROR:
                ensureEmptyView();
                applyErrorContent();
                mEmptyView.setVisibility(VISIBLE);
                if (mProgressView != null) mProgressView.setVisibility(GONE);
                setContentVisibility(false);
                break;
        }
    }

    /**
     * Empty-state text sits over the wallpaper. Use the theme dark overlay across the
     * ProgressLayout so the existing text palette remains readable over arbitrary images.
     * The overlay is cleared only when leaving EMPTY, preserving any host background in
     * CONTENT/PROGRESS states.
     */
    private void applyEmptyStateScrim() {
        TypedValue value = new TypedValue();
        if (!getContext().getTheme().resolveAttribute(R.attr.webhtvColorOverlayDark, value, true)) {
            setBackground(null);
            return;
        }
        int color = value.resourceId == 0
                ? value.data
                : ContextCompat.getColor(getContext(), value.resourceId);
        setBackgroundColor(color);
    }
    private void setContentVisibility(boolean visible) {
        for (View view : mContentViews) {
            if (visible) showView(view);
            else hideView(view);
        }
    }

    private void showView(View view) {
        // 幂等：已经完整可见时不再重放淡入。
        //
        // 为什么必须幂等。switchState(CONTENT) 会在 EMPTY/ERROR ↔ CONTENT 之间被反复调用，
        // 而每次切换都会走一遍 setContentVisibility(true)。旧实现每次都 setAlpha(0f) 再
        // new 一个 ViewPropertyAnimator，两步都会请求一次绘制；加载圈期间主线程本来就紧绷
        // （详情页/播放页要 inflate、解析、建 ExoPlayer），这些无效的 alpha 动画会把转圈
        // 的帧挤掉——用户看到的就是「转圈卡住不动再恢复」。
        if (view.getVisibility() == VISIBLE && view.getAlpha() == 1f) return;
        // 取消上一轮可能还在跑的淡入，避免多个 animator 同时写同一个 alpha。
        view.animate().cancel();
        view.setAlpha(0f);
        view.setVisibility(VISIBLE);
        view.animate().alpha(1f).setDuration(100);
    }

    private void hideView(View view) {
        if (view.getVisibility() == INVISIBLE) return;
        // 取消未完成的淡入：否则它仍会把 alpha 推到 1，下一次 show 又从 0 重来，
        // 同样是一帧白干的请求。
        view.animate().cancel();
        view.setVisibility(INVISIBLE);
    }
}
