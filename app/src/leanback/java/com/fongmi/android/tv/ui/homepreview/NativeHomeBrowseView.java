package com.fongmi.android.tv.ui.homepreview;

import android.content.Context;
import android.graphics.Color;
import android.content.res.ColorStateList;
import com.fongmi.android.tv.bean.HomeButton;
import com.fongmi.android.tv.ui.presenter.FuncPresenter;
import com.fongmi.android.tv.theme.ThemeController;
import com.fongmi.android.tv.theme.ThemeTokens;
import androidx.core.graphics.ColorUtils;
import com.fongmi.android.tv.ui.home.HomeCategorySwitchScheduler;
import java.util.HashMap;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import android.graphics.Canvas;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.leanback.widget.BaseGridView;
import androidx.leanback.widget.HorizontalGridView;
import androidx.recyclerview.widget.RecyclerView;
import com.fongmi.android.tv.R;
import java.util.List;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/** One native presentation, one poster rail; data remains an explicitly local fixture. */
public final class NativeHomeBrowseView extends FrameLayout {
    private final HorizontalGridView functions, categories, posters;
    private HorizontalGridView[] filters;
    private final ViewGroup top, categoryRow;
    private ViewGroup filterPanel;
    private com.fongmi.android.tv.ui.home.NativeHomeBrowseController business;
    private Object businessContext;
    private String businessCategories="";
    private List<com.fongmi.android.tv.bean.Filter> businessFilters=List.of();
    private java.util.function.Supplier<List<com.fongmi.android.tv.bean.Site>> sources;
    private Consumer<com.fongmi.android.tv.bean.Site> sourceApply;
    private Consumer<HomePreviewData.Card> heroFocus;
    private com.fongmi.android.tv.ui.home.NativeHomeBrowseImages images;
    private final Map<com.fongmi.android.tv.bean.Vod,HomePreviewData.Card> cardModels=new java.util.IdentityHashMap<>();
    private String renderedSite="";
    private Object renderedHome;
    private final ImageButton home;
    private final TextView title, meta, overview;
    private final ImageView logo;
    private com.fongmi.android.tv.ui.custom.CustomWallView nativeWall;
    private final HomeBrowseLoadingView centerLoading, railLoading;
    private Object pendingRailFocus;
    private final HomePreviewPosterAdapter posterAdapter;
    private List<HomePreviewData.Card> cards;
    private HomeBrowseFunctionAdapter nativeFunctions;
    private ThemeTokens appliedTokens;
    private long focusedFunctionId = RecyclerView.NO_ID;
    private final Map<Integer, CategoryState> categoryCache = new HashMap<>();
    private final int cardCount;
    private int requestedCategory, categoryLoads, categoryApplies;
    private long categoryGeneration;
    private long categoryEpoch;
    private final Set<Integer> categoryInFlight = new HashSet<>();
    /** Future native data integration seam. Completion must run on the main thread. No production loader is wired. */
    public interface CategoryContentSource {
        void load(int category, String label, int limit, Consumer<List<HomePreviewData.Card>> complete);
    }
    private CategoryContentSource categorySource = (category, label, limit, complete) -> complete.accept(HomePreviewData.categoryCards(category, label, limit));
    public void setCategoryContentSource(CategoryContentSource source) { categorySource = java.util.Objects.requireNonNull(source); }
    private boolean categoryPending;
    private final Runnable categorySwitch = this::applyRequestedCategory;
    private static final class CategoryState {
        final List<HomePreviewData.Card> cards; int position, hero;
        CategoryState(List<HomePreviewData.Card> cards) { this.cards = cards; }
    }
    private final HomePreviewSelection selection;
    private final HomePreviewFocusCoordinator focus;
    private final HomePreviewTiming timing = new HomePreviewTiming();
    private final String[] categoryLabels;
    private HomePreviewMenuDialog menu;
    private int sourceClosingKey=KeyEvent.KEYCODE_UNKNOWN;
    private long sourceClosingDownTime;
    private HomePreviewGeometry geometry;
    private boolean filterOpen, userNavigated;
    private int backdropResource, viewportWidth, viewportHeight, heroIndex;
    private long restoreGeneration;
    private Runnable pendingRestore;
    private HomePreviewData.Card boundHero;
    private int heroDirection=1;
    private int safeLeft,safeTop,safeRight,safeBottom;
    private boolean geometryDirty,backFromCards,preparationPosted;
    public record PreparationWindow(Object context,List<com.fongmi.android.tv.ui.home.NativeHomePreparationQueue.Candidate> candidates) {}
    private Consumer<PreparationWindow> preparationWindow;
    private Object lastPreparationContext;
    private List<com.fongmi.android.tv.ui.home.NativeHomePreparationQueue.Candidate> lastPreparationCandidates=List.of();
    private final Runnable publishPreparationWindow=()->{preparationPosted=false;publishPreparationWindow();};

    // Back/exit belongs to the owning Activity. The house never invokes unusedExit.
    public NativeHomeBrowseView(Context context, int cardCount, int functionCount, Runnable unusedExit) {
        super(context);
        this.cardCount = cardCount;
        setLayoutDirection(LAYOUT_DIRECTION_LTR);
        setClipChildren(false);
        setClipToPadding(false);
        LayoutInflater.from(context).inflate(R.layout.view_native_home_preview, this, true);
        top = findViewById(R.id.nhp_top);
        top.setFocusable(false);
        if(android.os.Build.VERSION.SDK_INT>=26)top.setDefaultFocusHighlightEnabled(false);
        categoryRow = findViewById(R.id.nhp_category_row);
        filterPanel = findViewById(R.id.nhp_filters);
        home = findViewById(R.id.nhp_home);
        title = findViewById(R.id.nhp_title);
        meta = findViewById(R.id.nhp_meta);
        overview = findViewById(R.id.nhp_overview);
        logo = findViewById(R.id.nhp_logo);
        centerLoading=new HomeBrowseLoadingView(context);railLoading=new HomeBrowseLoadingView(context);
        centerLoading.setVisibility(GONE);railLoading.setVisibility(GONE);
        addView(centerLoading);addView(railLoading);
        logo.setTag("webhtv:ignore");
        ImageView fallback=findViewById(R.id.nhp_fallback_poster);
        fallback.setTag("webhtv:ignore");fallback.setClipToOutline(true);
        fallback.setOutlineProvider(new android.view.ViewOutlineProvider(){
            @Override public void getOutline(View view,android.graphics.Outline outline){
                outline.setRoundRect(0,0,view.getWidth(),view.getHeight(),dp(8));
            }
        });
        functions = findViewById(R.id.nhp_functions);
        categories = findViewById(R.id.nhp_categories);
        posters = findViewById(R.id.nhp_posters);
        filters = new HorizontalGridView[]{findViewById(R.id.nhp_filter_genre), findViewById(R.id.nhp_filter_year), findViewById(R.id.nhp_filter_order)};
        for (HorizontalGridView grid : new HorizontalGridView[]{functions, categories, posters, filters[0], filters[1], filters[2]}) configureGrid(grid);
        categoryLabels = getResources().getStringArray(R.array.nhp_categories);
        selection = new HomePreviewSelection(categoryLabels.length);
        cards = HomePreviewData.cards(cardCount);
        categoryCache.put(0, new CategoryState(cards)); categoryLoads++;
        focus = new HomePreviewFocusCoordinator(top, categoryRow, home, functions, categories, posters, filters, () -> filterOpen);
        HomePreviewStyles.house(home);
        home.setOnClickListener(v -> openSourceMenu());
        home.setOnFocusChangeListener((v, hasFocus) -> { if (hasFocus) timing.focus("house", 0); });
        // Explicit standalone stress preview only; production host replaces this adapter with HomeButton configuration.
        String[] available = HomeButton.all().stream().map(b -> getResources().getString(b.getResId())).toArray(String[]::new);
        String[] labels = new String[Math.max(0, Math.min(24, functionCount))];
        for (int i = 0; i < labels.length; i++) labels[i] = available[i % available.length];
        functions.setAdapter(new ChoiceAdapter(labels, () -> -1, i -> timing.focus("functions", i), i -> {
            Toast.makeText(context, getResources().getString(R.string.nhp_placeholder, labels[i]), Toast.LENGTH_SHORT).show();
        }, 0));
        categories.setAdapter(new ChoiceAdapter(categoryLabels, selection::category, i -> { timing.focus("categories", i); scheduleCategory(i); }, i -> {
            // Confirm opens filters; focus already owns primary activation.
            flushCategory();
            openFilters();
        }, 1));
        for (int row = 0; row < filters.length; row++) {
            final int group = row;
            filters[row].setAdapter(new ChoiceAdapter(HomePreviewData.FILTERS[row], () -> selection.filter(group),
                    i -> timing.focus("filter" + group, i), i -> {
                selection.selectFilter(group, i);
                syncSelection(filters[group], selection.filter(group));
            }, 2));
        }
        posterAdapter = new HomePreviewPosterAdapter(cards, (card,position) -> {
            timing.focus("posters", (int) card.id());
            bindHero(card,position);
            if(business!=null) { business.focused(position); if(heroFocus!=null)heroFocus.accept(card); }
            schedulePreparationWindow();
        });
        posters.setAdapter(posterAdapter);
        posters.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int state) { timing.scroll(state); }
            @Override public void onScrolled(@NonNull RecyclerView recyclerView,int dx,int dy){schedulePreparationWindow();}
        });
        posters.addOnChildAttachStateChangeListener(new RecyclerView.OnChildAttachStateChangeListener(){
            public void onChildViewAttachedToWindow(@NonNull View child){schedulePreparationWindow();}
            public void onChildViewDetachedFromWindow(@NonNull View child){schedulePreparationWindow();}
        });
        posters.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->schedulePreparationWindow());
        if (cards.isEmpty()) {
            title.setText("本地演示");
            meta.setText("暂无测试影片");
            setBackdropResource(R.drawable.nhp_cinema_demo);
        } else bindHero(cards.get(0));
        refreshTheme();
        setFocusable(false);
        setOnApplyWindowInsetsListener((view, insets) -> {
            int left=insets.getSystemWindowInsetLeft(),top=insets.getSystemWindowInsetTop(),right=insets.getSystemWindowInsetRight(),bottom=insets.getSystemWindowInsetBottom();
            if(android.os.Build.VERSION.SDK_INT>=28&&insets.getDisplayCutout()!=null){var cutout=insets.getDisplayCutout();left=Math.max(left,cutout.getSafeInsetLeft());top=Math.max(top,cutout.getSafeInsetTop());right=Math.max(right,cutout.getSafeInsetRight());bottom=Math.max(bottom,cutout.getSafeInsetBottom());}
            if(left!=safeLeft||top!=safeTop||right!=safeRight||bottom!=safeBottom){safeLeft=left;safeTop=top;safeRight=right;safeBottom=bottom;geometryDirty=true;requestLayout();}
            return insets;
        });
    }

    private static void configureGrid(HorizontalGridView grid) {
        // Only child selectors draw focus; a transient container focus must not paint a rail-wide box.
        if(android.os.Build.VERSION.SDK_INT>=26)grid.setDefaultFocusHighlightEnabled(false);
        grid.setNumRows(1);
        grid.setWindowAlignment(BaseGridView.WINDOW_ALIGN_BOTH_EDGE);
        grid.setWindowAlignmentOffset(0);
        grid.setWindowAlignmentOffsetPercent(50);
        grid.setItemAlignmentOffset(0);
        grid.setItemAlignmentOffsetPercent(50);
        grid.setWindowAlignmentPreferKeyLineOverLowEdge(false);
        grid.setWindowAlignmentPreferKeyLineOverHighEdge(false);
        grid.setFocusScrollStrategy(BaseGridView.FOCUS_SCROLL_ALIGNED);
        grid.setSmoothScrollMaxPendingMoves(2);
        grid.setClipChildren(false);
        grid.setClipToPadding(false);
        grid.setItemAnimator(null);
        grid.setSaveChildrenPolicy(BaseGridView.SAVE_LIMITED_CHILD);
    }

    /** Production entry: list/configuration comes from the same HomeButton authority as classic. */
    public void setFunctions(List<HomeButton> buttons, FuncPresenter.OnClickListener listener) {
        boolean focused = functions.hasFocus();
        if (nativeFunctions == null) {
            nativeFunctions = new HomeBrowseFunctionAdapter(position -> {
                if (position >= 0 && position < nativeFunctions.getItemCount()) focusedFunctionId = nativeFunctions.getItemId(position);
                timing.focus("functions", position);
            });
            functions.setAdapter(nativeFunctions);
        }
        int oldPosition = Math.max(0, functions.getSelectedPosition());
        long identity = focusedFunctionId;
        if (identity == RecyclerView.NO_ID && oldPosition < nativeFunctions.getItemCount()) identity = nativeFunctions.getItemId(oldPosition);
        if (!nativeFunctions.submit(buttons, listener)) return;
        int target = nativeFunctions.positionOf(identity);
        if (target < 0 && nativeFunctions.getItemCount() > 0) target = Math.min(oldPosition, nativeFunctions.getItemCount() - 1);
        if (target >= 0) {
            if (functions.getSelectedPosition() != target) functions.setSelectedPosition(target);
            focusedFunctionId = nativeFunctions.getItemId(target);
        } else if (focused) home.requestFocus();
        focus.resetTopAnchor();
    }

    /** Colors only: no geometry, adapter notifications, focus or image-content rebinding. */
    public void refreshTheme() {
        ThemeTokens t = ThemeController.current();
        if (t.equals(appliedTokens)) return;
        appliedTokens = t;
        centerLoading.refreshTheme();railLoading.refreshTheme();
        // BaseActivity's CustomWallView is behind this content surface.
        setBackgroundColor(Color.TRANSPARENT);
        boolean light = ColorUtils.calculateLuminance(t.colorSurface()) > .5;
        float opacity = Math.min(.92f, Math.max(light ? .88f : .79f, Color.alpha(t.colorScrim()) / 255f));
        findViewById(R.id.nhp_shade_left).setBackground(new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT,
                new int[]{HomePreviewStyles.alpha(t.colorSurface(), opacity), HomePreviewStyles.alpha(t.colorSurface(), opacity * .45f), Color.TRANSPARENT}));
        findViewById(R.id.nhp_shade_bottom).setBackground(new GradientDrawable(GradientDrawable.Orientation.BOTTOM_TOP,
                new int[]{HomePreviewStyles.alpha(t.colorSurface(), .98f), HomePreviewStyles.alpha(t.colorSurface(), light ? .80f : .25f), Color.TRANSPARENT}));
        HomePreviewStyles.house(home, t);
        title.setTextColor(t.colorOnSurface()); meta.setTextColor(t.colorOnSurfaceVariant()); overview.setTextColor(t.colorOnSurface());
        for (TextView text : new TextView[]{title, meta, overview}) text.setShadowLayer(2, 0, 1, t.colorShadow());
        for (int i = 0; i < functions.getChildCount(); i++) HomePreviewStyles.function((TextView) functions.getChildAt(i), t);
        for (int i = 0; i < categories.getChildCount(); i++) HomePreviewStyles.pill((TextView) categories.getChildAt(i), true, t);
        for (HorizontalGridView grid : filters) for (int i = 0; i < grid.getChildCount(); i++) HomePreviewStyles.pill((TextView) grid.getChildAt(i), false, t);
        for (int i = 0; i < posters.getChildCount(); i++) {
            RecyclerView.ViewHolder holder = posters.getChildViewHolder(posters.getChildAt(i));
            if (holder instanceof HomePreviewPosterAdapter.Holder card) card.applyTheme(t);
        }
        if (menu != null && menu.isShowing()) menu.refreshTheme();
    }

    private void scheduleCategory(int category) {
        if (category == requestedCategory && (categoryPending || categoryInFlight.contains(category) || category == selection.category())) return;
        removeCallbacks(categorySwitch);
        categoryGeneration++;
        requestedCategory = category;
        categoryPending = category != selection.category();
        if (categoryPending) HomeCategorySwitchScheduler.replace(this, categorySwitch);
    }
    private void flushCategory() {
        if (!categoryPending) return;
        removeCallbacks(categorySwitch);
        applyRequestedCategory();
    }
    private void applyRequestedCategory() {
        categoryPending = false;
        int target = requestedCategory;
        long generation = categoryGeneration;
        if (target == selection.category()) return;
        CategoryState next = categoryCache.get(target);
        if (next == null) {
            if (!categoryInFlight.add(target)) return;
            categoryLoads++;
            long epoch = categoryEpoch;
            categorySource.load(target, categoryLabels[target], cardCount, result -> {
                // A changed source/released host invalidates even a completed old category.
                if (epoch != categoryEpoch) return;
                categoryInFlight.remove(target);
                CategoryState loaded = new CategoryState(List.copyOf(result));
                categoryCache.put(target, loaded);
                // Reused in-flight data may serve a later focus on the same identity;
                // it can never replace a different requested category.
                if (target == requestedCategory) publishCategory(target, categoryGeneration, loaded);
            });
            return;
        }
        publishCategory(target, generation, next);
    }
    private void publishCategory(int target, long generation, CategoryState next) {
        if (generation != categoryGeneration || target != requestedCategory) return;
        if (target == selection.category()) return;
        CategoryState previous = categoryCache.get(selection.category());
        if (previous != null) { previous.position = Math.max(0, posters.getSelectedPosition()); previous.hero = heroIndex; }
        selection.selectCategory(target); categoryApplies++;
        cards = next.cards; posterAdapter.submitCards(cards);
        syncCategorySelection();
        for (int row = 0; row < filters.length; row++) {
            filters[row].setSelectedPosition(selection.filter(row)); syncSelection(filters[row], selection.filter(row));
        }
        if (!cards.isEmpty()) {
            posters.setSelectedPosition(Math.min(next.position, cards.size() - 1));
            bindHero(cards.get(Math.min(next.hero, cards.size() - 1)));
        } else {
            boundHero = null; heroIndex = 0;
            title.setText(categoryLabels[target]); meta.setText("本地演示 · 暂无测试影片"); overview.setText("");
        }
    }

    @Override protected void onMeasure(int widthSpec, int heightSpec) {
        timing.measures++;
        int width = MeasureSpec.getSize(widthSpec) - getPaddingLeft() - getPaddingRight()-safeLeft-safeRight;
        int height = MeasureSpec.getSize(heightSpec) - getPaddingTop() - getPaddingBottom()-safeTop-safeBottom;
        if (width > 0 && height > 0 && (geometryDirty||width != viewportWidth || height != viewportHeight)) {
            geometryDirty=false;
            viewportWidth = width; viewportHeight = height;
            applyGeometry(HomePreviewGeometry.calculate(width, height, getResources().getDisplayMetrics().density));
        }
        super.onMeasure(widthSpec, heightSpec);
    }

    private void applyGeometry(HomePreviewGeometry g) {
        geometry = g;
        place(centerLoading,viewportWidth/2-dp(48),viewportHeight/2-dp(24),dp(96),dp(48));
        place(railLoading,viewportWidth/2-dp(48),g.railTop()+g.railHeight()/2-dp(24),dp(96),dp(48));
        int fallbackHeight=Math.round(Math.min(viewportHeight*.39f,dp(300)));
        int fallbackWidth=Math.round(fallbackHeight*2f/3);
        place(findViewById(R.id.nhp_fallback_poster),viewportWidth-Math.round(viewportWidth*.08f)-fallbackWidth,
            Math.round(viewportHeight*.145f),fallbackWidth,fallbackHeight);
        findViewById(R.id.nhp_fallback_poster).setTag("webhtv:ignore");
        timing.geometry++;
        place(top, g.gutter(), g.top(), viewportWidth - g.gutter() * 2, g.controlHeight());
        LinearLayout.LayoutParams homeParams = (LinearLayout.LayoutParams) home.getLayoutParams();
        homeParams.width = dp(32);
        homeParams.height = dp(32);
        home.setLayoutParams(homeParams);
        functions.setHorizontalSpacing(dp(24));
        place(categoryRow, g.gutter(), g.categoryTop(), viewportWidth - g.gutter() * 2, g.categoryHeight());
        categories.setHorizontalSpacing(dp(10));
        LinearLayout.LayoutParams categoryGridParams = (LinearLayout.LayoutParams) categories.getLayoutParams();
        categoryGridParams.height = Math.round(HomePreviewGeometry.clamp(viewportHeight * .045f, dp(24), dp(30)));
        categories.setLayoutParams(categoryGridParams);
        place(posters, 0, g.railTop(), viewportWidth, g.railHeight());
        posters.setPadding(g.gutter(), g.focusSpace(), g.gutter(), g.focusSpace());
        posters.setHorizontalSpacing(g.gap());
        posterAdapter.setGeometry(g);
        if(images!=null)images.setPosterDecodeSize(g.posterWidth(),g.posterHeight());
        int rowHeight = Math.round(HomePreviewGeometry.clamp(viewportHeight * .045f, dp(22), dp(29)));
        int gap = Math.round(viewportHeight * .012f);
        place(filterPanel, g.gutter(), Math.round(viewportHeight * .035f), viewportWidth - g.gutter() * 2, (rowHeight + gap) * 3);
        for (HorizontalGridView grid : filters) {
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) grid.getLayoutParams();
            lp.height = rowHeight; lp.bottomMargin = gap; grid.setLayoutParams(lp);
            grid.setHorizontalSpacing(dp(6));
        }
        float heightDp = viewportHeight / getResources().getDisplayMetrics().density;
        title.setTextSize(HomePreviewGeometry.clamp(heightDp * .074f, 24, 48));
        meta.setTextSize(HomePreviewGeometry.clamp(heightDp * .025f, 11, 16));
        overview.setTextSize(HomePreviewGeometry.clamp(heightDp * .027f, 12, 18));
        applyHeroLayout();
    }

    private void applyHeroLayout() {
        if (geometry == null) return;
        HomePreviewGeometry g = geometry;
        int heroTop = filterOpen ? Math.round(viewportHeight * .31f) : g.heroTop();
        int heroHeight = filterOpen ? Math.round(viewportHeight * .18f) : g.heroHeight();
        place(findViewById(R.id.nhp_hero), g.gutter(), heroTop, Math.round(viewportWidth * .50f), heroHeight);
        View slot = findViewById(R.id.nhp_title_slot);
        slot.setVisibility(filterOpen ? GONE : VISIBLE);
        LinearLayout.LayoutParams titleParams = (LinearLayout.LayoutParams) slot.getLayoutParams();
        // Reserve actual font line metrics, not a fraction that clips a two-line title.
        titleParams.height = Math.max(1, Math.round(title.getPaint().getFontSpacing() * 2));
        slot.setLayoutParams(titleParams);
        LinearLayout.LayoutParams metaParams = (LinearLayout.LayoutParams) meta.getLayoutParams();
        metaParams.height = Math.max(Math.round(meta.getPaint().getFontSpacing()) + meta.getCompoundPaddingTop() + meta.getCompoundPaddingBottom(), Math.round(viewportHeight * .037f));
        meta.setLayoutParams(metaParams);
        int overviewSpace = heroHeight - metaParams.height - (filterOpen ? 0 : titleParams.height);
        overview.setMaxLines(Math.max(1, Math.min(3, (int)(overviewSpace / (overview.getPaint().getFontSpacing() * 1.15f)))));
    }

    private void place(View view, int left, int top, int width, int height) {
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(width, height);
        lp.leftMargin = left+safeLeft; lp.topMargin = top+safeTop; view.setLayoutParams(lp);
    }

    private static void syncSelection(HorizontalGridView grid, int selected) {
        for (int i = 0; i < grid.getChildCount(); i++) {
            View child = grid.getChildAt(i);
            boolean next = grid.getChildAdapterPosition(child) == selected;
            if (child.isSelected() != next) child.setSelected(next);
        }
    }
    private void syncCategorySelection() { syncSelection(categories, selection.category()); }

    private void openSourceMenu() {
        if (filterOpen || menu != null && menu.isShowing()) return;
        if(business!=null) {
            List<com.fongmi.android.tv.bean.Site> sites=sources.get();
            if(sites.isEmpty())return;
            String key=com.fongmi.android.tv.api.config.VodConfig.get().getHome().getKey();
            int selected=0;for(int i=0;i<sites.size();i++)if(sites.get(i).getKey().equals(key))selected=i;
            final int current=selected;
            menu=new HomePreviewMenuDialog(getContext(),sites,()->current,sourceApply,home);
            menu.setShortcutCloseListener(event->{sourceClosingKey=event.getKeyCode();sourceClosingDownTime=event.getDownTime();});
            menu.show();return;
        }
        menu = new HomePreviewMenuDialog(getContext(), HomePreviewData.SOURCES, selection::source, index -> {
            int previous = selection.source();
            selection.selectSource(index);
            home.setContentDescription("站源：" + HomePreviewData.SOURCES[selection.source()]);
            if (previous != selection.source()) {
                removeCallbacks(categorySwitch); categoryPending = false; categoryGeneration++; requestedCategory = 0;
                categoryEpoch++; categoryInFlight.clear();
                categoryCache.clear(); cards = HomePreviewData.cards(cardCount); categoryCache.put(0, new CategoryState(cards)); categoryLoads++;
                posterAdapter.submitCards(cards);
                focus.resetCategoryAnchor();
                categories.setSelectedPosition(selection.category());
                syncCategorySelection();
                for (int row = 0; row < filters.length; row++) syncSelection(filters[row], selection.filter(row));
                if (!cards.isEmpty()) { posters.setSelectedPosition(0); bindHero(cards.get(0)); }
            }
        }, home);
        menu.show();
    }

    private void openFilters() {
        if(business!=null&&filters.length==0)return;
        pendingRailFocus=null;
        focus.remember(findFocus());
        if (!filterOpen) {
            filterOpen = true;
            top.setVisibility(GONE);
            categoryRow.setVisibility(INVISIBLE);
            filterPanel.setVisibility(VISIBLE);
            applyHeroLayout();
        }
        for (int row = 0; row < filters.length; row++) {
            int selected=business==null?selection.filter(row):business.selectedFilter(row);
            filters[row].setSelectedPosition(Math.max(0,selected));
            syncSelection(filters[row], selected);
        }
        filters[0].requestFocus();
    }

    public boolean isFilterOpen() { return filterOpen; }
    public boolean handleBack() {
        if(menu!=null&&menu.isShowing()){menu.dismiss();return true;}
        if(!filterOpen){
            if(posters.hasFocus()&&posters.getSelectedPosition()>0){
                if(posters.getAdapter()!=null&&posters.getAdapter().getItemCount()>0)
                    posters.setSelectedPosition(0,new androidx.leanback.widget.ViewHolderTask(){@Override public void run(RecyclerView.ViewHolder holder){holder.itemView.requestFocus();}});
                return true;
            }
            return business!=null&&business.backFolder();
        }
        filterOpen = false;
        filterPanel.setVisibility(GONE);
        top.setVisibility(VISIBLE);
        categoryRow.setVisibility(VISIBLE);
        applyHeroLayout();
        focus.focusCategories();
        return true;
    }

    private java.util.function.Function<com.fongmi.android.tv.bean.Vod,String> preparedLogo=vod->null;
    public void setPreparedLogoLookup(java.util.function.Function<com.fongmi.android.tv.bean.Vod,String> lookup){preparedLogo=lookup;}
    public void bindHero(HomePreviewData.Card card) {
        bindHero(card,Math.max(0,cards.indexOf(card)));
    }
    private void bindHero(HomePreviewData.Card card,int position) {
        if (card.equals(boundHero)) return;
        boundHero = card;
        timing.heroes++;
        if(position!=heroIndex)heroDirection=position>heroIndex?1:-1;
        heroIndex = position;
        ((HomeHeroTitleView)title).bindTitle(card.id(),card.title());
        if (!TextUtils.equals(meta.getText(), card.metadata())) meta.setText(card.metadata());
        if (!TextUtils.equals(overview.getText(), card.overview())) overview.setText(card.overview());
        if(card.vod()!=null&&images!=null){images.focusSource(card.image());images.prepareTitle(preparedLogo.apply(card.vod()));}
        else {
            title.setVisibility(card.logo() == 0 ? VISIBLE : INVISIBLE);logo.setVisibility(card.logo() == 0 ? INVISIBLE : VISIBLE);
            if(card.logo()!=0)logo.setImageResource(card.logo());else logo.setImageDrawable(null);
            setBackdropResource(card.backdrop());
        }
    }

    public void setBackdropResource(int resource) {
        if (resource == 0 || resource == backdropResource) return;
        backdropResource = resource; timing.backgrounds++;
        if(nativeWall!=null)nativeWall.showContentBackground(this,getResources().getDrawable(resource,getContext().getTheme()),()->{});
    }

    /** Borrow the Activity's existing native wall; content never creates a movie backdrop host. */
    public void setBackgroundHost(com.fongmi.android.tv.ui.custom.CustomWallView wall) {
        if(nativeWall==wall)return;
        if(nativeWall!=null)nativeWall.releaseContentBackground(this);
        nativeWall=wall;
        if(wall==null)return;
        wall.acquireContentBackground(this);
        if(backdropResource!=0)wall.showContentBackground(this,getResources().getDrawable(backdropResource,getContext().getTheme()),()->{});
    }

    @Override public boolean dispatchKeyEvent(KeyEvent event) {
        if(sourceClosingKey!=KeyEvent.KEYCODE_UNKNOWN){
            if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getRepeatCount()==0)sourceClosingKey=KeyEvent.KEYCODE_UNKNOWN;
            else if(event.getKeyCode()==sourceClosingKey&&event.getDownTime()==sourceClosingDownTime){
                if(event.getAction()==KeyEvent.ACTION_UP)sourceClosingKey=KeyEvent.KEYCODE_UNKNOWN;
                return true;
            }
        }
        long started = System.nanoTime();
        if(event.getAction()==KeyEvent.ACTION_DOWN&&event.getKeyCode()!=KeyEvent.KEYCODE_DPAD_DOWN)pendingRailFocus=null;
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DPAD_DOWN
                && focus.regionOf(findFocus()) == HomePreviewFocusCoordinator.Region.CATEGORIES) {
            if(business!=null)business.flushCategory();else flushCategory();
            if(business!=null&&cards.isEmpty()&&business.loading())pendingRailFocus=business.state();
        }
        if (event.getAction() == KeyEvent.ACTION_DOWN) {
            userNavigated = true;
            timing.key(event.getKeyCode(), event.getRepeatCount(), event.getEventTime());
        }
        if(event.getKeyCode()==KeyEvent.KEYCODE_BACK&&(backFromCards||filterOpen||(posters.hasFocus()&&posters.getSelectedPosition()>0))) {
            if(event.getAction()==KeyEvent.ACTION_DOWN)backFromCards=true;
            if (event.getAction() == KeyEvent.ACTION_UP) {backFromCards=false;if(!event.isCanceled())handleBack();}
            timing.dispatch(started); return true;
        }
        boolean handled = focus.handle(event, findFocus());
        if (!handled) handled = super.dispatchKeyEvent(event);
        timing.dispatch(started);
        return handled;
    }

    @Override protected void dispatchDraw(Canvas canvas) {
        long started = System.nanoTime();
        super.dispatchDraw(canvas);
        timing.draw(started);
    }

    public Bundle debugMetrics() {
        Bundle result = timing.snapshot();
        result.putInt("pendingRestores", pendingRestore == null ? 0 : 1);
        result.putInt("posterBinds",posterAdapter.binds);
        result.putLong("categoryGeneration", categoryGeneration); result.putInt("categoryPending", categoryPending ? 1 : 0);
        result.putInt("categoryLoads", categoryLoads); result.putInt("categoryApplies", categoryApplies);
        return result;
    }
    public void savePreviewState(Bundle state) {
        state.putInt("preview.source", selection.source()); state.putInt("preview.category", selection.category());
        state.putIntArray("preview.secondary", selection.secondarySnapshot());
        state.putIntArray("preview.year", selection.filterSnapshot(1)); state.putIntArray("preview.order", selection.filterSnapshot(2));
        state.putLong("preview.functionId", focusedFunctionId);
        state.putInt("preview.poster", posters.getSelectedPosition()); state.putInt("preview.function", functions.getSelectedPosition());
        state.putInt("preview.categoryFocus", categories.getSelectedPosition()); state.putInt("preview.hero", heroIndex);
        state.putString("preview.region", focus.regionOf(findFocus()).name());
        // A restored home starts in normal state; applied filter selections persist in the fixture.
    }

    public void restorePreviewState(Bundle state) {
        if(business!=null){home.requestFocus();return;}
        if (pendingRestore != null) removeCallbacks(pendingRestore);
        long generation = ++restoreGeneration;
        if (state != null) {
            selection.restore(state.getInt("preview.source"), state.getInt("preview.category"), state.getIntArray("preview.secondary"));
            int savedCategory = selection.category();
            CategoryState restored = categoryCache.get(savedCategory);
            if (restored == null) { restored = new CategoryState(HomePreviewData.categoryCards(savedCategory, categoryLabels[savedCategory], cardCount)); categoryCache.put(savedCategory, restored); categoryLoads++; }
            cards = restored.cards; posterAdapter.submitCards(cards); requestedCategory = savedCategory;
            focusedFunctionId = state.getLong("preview.functionId", RecyclerView.NO_ID);
            selection.restoreFilter(1, state.getIntArray("preview.year")); selection.restoreFilter(2, state.getIntArray("preview.order"));
        }
        home.setContentDescription("站源：" + HomePreviewData.SOURCES[selection.source()]);
        pendingRestore = () -> {
            if (generation != restoreGeneration) return;
            pendingRestore = null;
            if (userNavigated) return;
            categories.setSelectedPosition(state == null ? selection.category() : state.getInt("preview.categoryFocus", selection.category()));
            if (state != null) {
                if (functions.getAdapter().getItemCount() > 0) {
                    int restoredPosition = nativeFunctions == null ? -1 : nativeFunctions.positionOf(focusedFunctionId);
                    functions.setSelectedPosition(restoredPosition >= 0 ? restoredPosition : Math.max(0, Math.min(functions.getAdapter().getItemCount() - 1, state.getInt("preview.function"))));
                }
                if (!cards.isEmpty()) {
                    posters.setSelectedPosition(Math.max(0, Math.min(cards.size() - 1, state.getInt("preview.poster"))));
                    bindHero(cards.get(Math.max(0, Math.min(cards.size() - 1, state.getInt("preview.hero")))));
                }
            }
            String region = state == null ? "TOP" : state.getString("preview.region", "TOP");
            if ("POSTERS".equals(region) && !cards.isEmpty()) posters.requestFocus();
            else if ("CATEGORIES".equals(region) || "FILTERS".equals(region)) categories.requestFocus();
            else home.requestFocus();
            syncCategorySelection();
        };
        timing.restorePosts++;
        post(pendingRestore);
    }

    public void release() {
        removeCallbacks(publishPreparationWindow);preparationPosted=false;preparationWindow=null;setVisualsActive(false);
        pendingRailFocus=null;centerLoading.stop();railLoading.stop();
        if(images!=null)images.clear();
        categoryEpoch++; categoryInFlight.clear();
        removeCallbacks(categorySwitch); categoryPending = false; categoryGeneration++;
        restoreGeneration++;
        if (pendingRestore != null) removeCallbacks(pendingRestore);
        pendingRestore = null;
        if (menu != null) {
            // A departing presentation must not restore focus into its closing drawer's host.
            menu.setOnDismissListener(null);
            menu.dismiss();
            menu = null;
        }
        if(nativeWall!=null){nativeWall.releaseContentBackground(this);nativeWall=null;}
        for (int i = 0; i < posters.getChildCount(); i++) {
            View child=posters.getChildAt(i);child.animate().cancel();
            if(posters.getChildViewHolder(child) instanceof HomePreviewPosterAdapter.Holder holder)com.fongmi.android.tv.utils.ImgUtil.clear(holder.image);
        }
    }
    private int dp(float value) { return HomePreviewPosterAdapter.dp(getContext(), value); }

    /** Attach data ownership without replacing the accepted layout or poster host. */
    public void setBusiness(com.fongmi.android.tv.ui.home.NativeHomeBrowseController controller,
                            java.util.function.Supplier<List<com.fongmi.android.tv.bean.Site>> sources,
                            Consumer<com.fongmi.android.tv.bean.Site> sourceApply,
                            Consumer<com.fongmi.android.tv.bean.Vod> click, java.util.function.Predicate<com.fongmi.android.tv.bean.Vod> longClick,
                            Consumer<HomePreviewData.Card> heroFocus) {
        business=controller;this.sources=sources;this.sourceApply=sourceApply;this.heroFocus=heroFocus;
        categories.setAdapter(new ChoiceAdapter(new String[0],()->-1,i->{},i->{},1));
        focus.setActiveCategory(()->activeCategoryIndex(controller));
        businessFilters=null;
        images=new com.fongmi.android.tv.ui.home.NativeHomeBrowseImages(nativeWall,this,logo,title,findViewById(R.id.nhp_fallback_poster));
        images.manageTitle();
        if(geometry!=null)images.setPosterDecodeSize(geometry.posterWidth(),geometry.posterHeight());
        posterAdapter.click=c->{if(c.vod()!=null)click.accept(c.vod());};
        posterAdapter.longClick=c->c.vod()!=null&&longClick.test(c.vod());
        removeCallbacks(categorySwitch);categoryPending=false;
        title.setText("");meta.setText("");overview.setText("");
        backdropResource=0;images.source("");
    }
    public void renderBusiness(com.fongmi.android.tv.ui.home.NativeHomeBrowseController controller,String site) {
        schedulePreparationWindow();
        if(!site.equals(renderedSite)||renderedHome!=controller.homeResult()){cardModels.clear();renderedSite=site;renderedHome=controller.homeResult();}
        String signature=controller.types().stream().map(t->t.getTypeId()+":"+t.getTypeName()).collect(java.util.stream.Collectors.joining("|"));
        if(!signature.equals(businessCategories)) {
            businessCategories=signature;
            var categoryTypes=controller.types();
            categories.setAdapter(new ChoiceAdapter(categoryTypes.stream().map(t -> t.isHome() ? "更新推荐" : t.getTypeName()).toArray(String[]::new),
                ()->activeCategoryIndex(controller),i->{
                    if(!site.equals(controller.siteKey())||i>=controller.types().size()||!categoryTypes.get(i).getTypeId().equals(controller.types().get(i).getTypeId()))return;
                    pendingRailFocus=null;controller.focusCategory(categoryTypes.get(i).getTypeId());
                },
                i->{
                    if(!site.equals(controller.siteKey())||i>=controller.types().size()||!categoryTypes.get(i).getTypeId().equals(controller.types().get(i).getTypeId()))return;
                    controller.focusCategory(categoryTypes.get(i).getTypeId());controller.flushCategory();updateBusinessFilters(controller);openFilters();
                },1));
            focus.resetCategoryAnchor();
        }
        syncSelection(categories,activeCategoryIndex(controller));
        updateBusinessFilters(controller);
        var state=controller.state();
        List<HomePreviewData.Card> next=new java.util.ArrayList<>();Map<Long,Integer> occurrences=new HashMap<>();
        if(state!=null)for(com.fongmi.android.tv.bean.Vod vod:state.items) {
            HomePreviewData.Card card=cardModels.computeIfAbsent(vod,v->HomePreviewData.Card.fromVod(site,v));
            int occurrence=occurrences.merge(card.id(),1,Integer::sum)-1;
            if(occurrence>0)card=new HomePreviewData.Card(card.id()^(0x9e3779b97f4a7c15L*occurrence),card.title(),card.metadata(),card.overview(),card.rating(),card.quality(),card.poster(),card.backdrop(),card.logo(),card.vod(),card.image());
            next.add(card);
        }
        next=List.copyOf(next);
        boolean contextChanged=businessContext!=state;
        if(!next.equals(cards)) { cards=next;posterAdapter.submitCards(cards); }
        if(contextChanged) {
            businessContext=state;boundHero=null;
            if(!categories.hasFocus()){categories.setSelectedPosition(activeCategoryIndex(controller));focus.resetCategoryAnchor();}
            if(!cards.isEmpty())posters.setSelectedPosition(Math.min(state.position,cards.size()-1));
        }
        var loading=controller.loadState();
        boolean cold=loading==com.fongmi.android.tv.ui.home.NativeHomeBrowseController.LoadState.COLD;
        boolean railWait=loading==com.fongmi.android.tv.ui.home.NativeHomeBrowseController.LoadState.RAIL;
        centerLoading.setVisibility(cold?VISIBLE:GONE);railLoading.setVisibility(railWait?VISIBLE:GONE);
        categoryRow.setVisibility(cold||filterOpen?INVISIBLE:VISIBLE);
        findViewById(R.id.nhp_hero).setVisibility(cold?INVISIBLE:VISIBLE);
        posters.setVisibility(cold||railWait?INVISIBLE:VISIBLE);
        if(cards.isEmpty()) {
            if(boundHero!=null||contextChanged)images.source("");
            boundHero=null;logo.setVisibility(INVISIBLE);title.setVisibility(VISIBLE);
            title.setText(controller.loading()?"":state!=null&&!state.error.isEmpty()?"加载失败":"暂无内容");
            meta.setText(state==null?"":state.error);overview.setText("");
        } else if(contextChanged||boundHero==null) {
            HomePreviewData.Card card=cards.get(Math.min(state.position,cards.size()-1));bindHero(card);heroFocus.accept(card);
        }
        if(pendingRailFocus!=null&&pendingRailFocus!=state)pendingRailFocus=null;
        if(pendingRailFocus==state&&state!=null&&!cards.isEmpty()&&categories.hasFocus()) {
            pendingRailFocus=null;posters.requestFocus();
        }
    }
    private int activeCategoryIndex(com.fongmi.android.tv.ui.home.NativeHomeBrowseController controller) {
        for(int i=0;i<controller.types().size();i++)if(controller.types().get(i).getTypeId().equals(controller.activeType()))return i;
        return 0;
    }
    private void updateBusinessFilters(com.fongmi.android.tv.ui.home.NativeHomeBrowseController controller) {
        List<com.fongmi.android.tv.bean.Filter> next=controller.filters().stream().filter(f->!f.getValue().isEmpty()).toList();
        if(!next.equals(businessFilters)) {
            businessFilters=next;
            if(!(filterPanel instanceof android.widget.ScrollView)) {
                ViewGroup parent=(ViewGroup)filterPanel.getParent();int index=parent.indexOfChild(filterPanel);
                ViewGroup.LayoutParams lp=filterPanel.getLayoutParams();parent.removeView(filterPanel);
                android.widget.ScrollView scroll=new android.widget.ScrollView(getContext());scroll.setFillViewport(false);scroll.setVerticalScrollBarEnabled(false);scroll.setFocusable(false);scroll.setVisibility(GONE);
                parent.addView(scroll,index,lp);filterPanel=scroll;
            }
            filterPanel.removeAllViews();LinearLayout rows=new LinearLayout(getContext());rows.setOrientation(LinearLayout.VERTICAL);filterPanel.addView(rows,new ViewGroup.LayoutParams(-1,-2));
            filters=new HorizontalGridView[next.size()];
            for(int row=0;row<next.size();row++) {
                int group=row;HorizontalGridView grid=new HorizontalGridView(getContext());configureGrid(grid);filters[row]=grid;
                rows.addView(grid,new LinearLayout.LayoutParams(-1,dp(28)));
                grid.setAdapter(new ChoiceAdapter(next.get(row).getValue().stream().map(com.fongmi.android.tv.bean.Value::getN).toArray(String[]::new),
                    ()->controller.selectedFilter(group),i->{},i->controller.selectFilter(group,i),2));
            }
            focus.setFilters(filters);if(geometry!=null)applyGeometry(geometry);
            if(filterOpen)filterPanel.setVisibility(VISIBLE);
            if(filterOpen&&filters.length==0)handleBack();
        }
        for(int row=0;row<filters.length;row++)syncSelection(filters[row],controller.selectedFilter(row));
    }
    public void showHero(long id,String metadata,String synopsis,String backdrop,String logoUrl) {
        showHero(id,metadata,synopsis,backdrop,logoUrl,"");
    }
    public void showHero(long id,String metadata,String synopsis,String backdrop,String logoUrl,String posterUrl) {
        if(boundHero==null||boundHero.id()!=id)return;
        if(!metadata.isEmpty()&&!TextUtils.equals(meta.getText(),metadata))meta.setText(metadata);
        if(!synopsis.isEmpty()&&!TextUtils.equals(overview.getText(),synopsis))overview.setText(synopsis);
        images.enrich(backdrop,logoUrl,posterUrl);
    }
    public void refreshFocusedHero() { if(business!=null&&boundHero!=null&&heroFocus!=null)heroFocus.accept(boundHero); }
    public void settleFocusedArtwork(long id) { if(boundHero!=null&&boundHero.id()==id&&images!=null)images.settleSource(); }
    public boolean isPosterRailFocused(){return posters.hasFocus();}
    public HomePreviewData.Card neighborFor(long id){
        if(boundHero==null||boundHero.id()!=id)return null;int next=heroIndex+heroDirection;
        return next>=0&&next<cards.size()?cards.get(next):null;
    }
    public void prefetchArtwork(long id,String backdrop){if(boundHero!=null&&boundHero.id()==id&&images!=null)images.prefetch(backdrop);}
    public void noFocusedLogo(long id){if(boundHero!=null&&boundHero.id()==id&&images!=null)images.noLogo();}
    public void prefetchWindowLogo(String url,Consumer<Boolean> completed){if(images==null)completed.accept(false);else images.prefetchLogo(url,completed);}
    public void noFocusedBackdrop(long id){if(boundHero!=null&&boundHero.id()==id&&images!=null)images.noBackdrop();}
    public void setPreparationWindowListener(Consumer<PreparationWindow> listener){preparationWindow=listener;lastPreparationContext=null;lastPreparationCandidates=List.of();schedulePreparationWindow();}
    public void prefetchWindowArtwork(String url,Consumer<Boolean> completed){if(images==null)completed.accept(false);else images.prefetch(url,completed);}
    public void setVisualsActive(boolean active){
        if(images!=null)images.titleActive(active);
        if(title instanceof HomeHeroTitleView marquee)marquee.setMarqueeActive(active);
        if(active){lastPreparationContext=null;schedulePreparationWindow();}
        else {removeCallbacks(publishPreparationWindow);preparationPosted=false;}
    }
    private void schedulePreparationWindow(){if(preparationWindow!=null&&!preparationPosted){preparationPosted=true;post(publishPreparationWindow);}}
    private void publishPreparationWindow(){
        if(preparationWindow==null||business==null)return;
        if(!cards.isEmpty()&&(posters.getWidth()<=0||posters.getChildCount()==0||posters.getVisibility()!=VISIBLE))return;
        List<Integer> visible=new java.util.ArrayList<>();
        for(int i=0;i<posters.getChildCount();i++){
            View child=posters.getChildAt(i);int position=posters.getChildAdapterPosition(child);
            if(position>=0&&position<cards.size()&&child.getWidth()>0&&child.getRight()>posters.getPaddingLeft()&&child.getLeft()<posters.getWidth()-posters.getPaddingRight())visible.add(position);
        }
        if(!cards.isEmpty()&&visible.isEmpty())return;
        int first=visible.stream().mapToInt(Integer::intValue).min().orElse(0),last=visible.stream().mapToInt(Integer::intValue).max().orElse(0);
        visible.sort(java.util.Comparator.comparingInt(i->Math.abs(i-heroIndex)));
        Map<Integer,Integer> positions=new java.util.LinkedHashMap<>();
        if(!cards.isEmpty()){positions.put(heroIndex,0);int next=heroIndex+heroDirection;if(next>=0&&next<cards.size())positions.put(next,1);}
        for(int i=0;i<Math.min(8,visible.size());i++){int position=visible.get(i);positions.putIfAbsent(position,10+Math.abs(position-heroIndex));}
        for(int i=1;i<=2;i++){int position=heroDirection>0?last+i:first-i;if(position>=0&&position<cards.size())positions.putIfAbsent(position,100+i);}
        List<com.fongmi.android.tv.ui.home.NativeHomePreparationQueue.Candidate> candidates=positions.entrySet().stream()
            .sorted(Map.Entry.comparingByValue()).limit(com.fongmi.android.tv.ui.home.NativeHomePreparationQueue.MAX_WINDOW)
            .map(e->new com.fongmi.android.tv.ui.home.NativeHomePreparationQueue.Candidate(cards.get(e.getKey()).vod(),e.getValue())).toList();
        if(lastPreparationContext==businessContext&&lastPreparationCandidates.equals(candidates))return;
        lastPreparationContext=businessContext;lastPreparationCandidates=candidates;
        preparationWindow.accept(new PreparationWindow(businessContext,candidates));
    }

    private final class ChoiceAdapter extends RecyclerView.Adapter<ChoiceAdapter.Holder> {
        private final String[] labels;
        private final IntSupplier selected;
        private final IntConsumer onFocus, onClick;
        private final int role;
        ChoiceAdapter(String[] labels, IntSupplier selected, IntConsumer onFocus, IntConsumer onClick, int role) {
            this.labels = labels; this.selected = selected; this.onFocus = onFocus; this.onClick = onClick; this.role = role;
            setHasStableIds(true);
        }
        @Override public long getItemId(int position) { return position; }
        @Override public int getItemCount() { return labels.length; }
        @NonNull @Override public Holder onCreateViewHolder(@NonNull ViewGroup parent, int type) {
            Button button = new Button(getContext());
            button.setAllCaps(false); button.setTextSize(role == 0 ? 16 : role == 1 ? 14 : 12);
            button.setMinWidth(0); button.setMinimumWidth(0); button.setMinHeight(0); button.setMinimumHeight(0);
            button.setGravity(Gravity.CENTER); button.setPadding(dp(role == 2 ? 8 : 10), 0, dp(role == 2 ? 8 : 10), 0);
            if (role == 0) HomePreviewStyles.function(button); else HomePreviewStyles.pill(button, role == 1);
            button.setLayoutParams(new RecyclerView.LayoutParams(-2, -1));
            return new Holder(button);
        }
        @Override public void onBindViewHolder(@NonNull Holder holder, int position) {
            if (role == 0) HomePreviewStyles.function(holder.button); else HomePreviewStyles.pill(holder.button, role == 1);
            holder.button.setText(labels[position]); holder.button.setSelected(selected.getAsInt() == position);
            holder.button.setOnFocusChangeListener((v, hasFocus) -> { if(role==0)HomePreviewStyles.functionFocus(v,hasFocus);else if(role==1)HomePreviewStyles.categoryFocus(v,hasFocus);if (hasFocus) onFocus.accept(position); });
            holder.button.setOnClickListener(v -> onClick.accept(position));
        }
        @Override public void onViewRecycled(@NonNull Holder holder) {
            holder.button.setOnFocusChangeListener(null); holder.button.setOnClickListener(null);
            if(role==1){holder.button.animate().cancel();holder.button.setScaleX(1);holder.button.setScaleY(1);}
        }
        final class Holder extends RecyclerView.ViewHolder {
            final Button button;
            Holder(Button button) { super(button); this.button = button; }
        }
    }
}


