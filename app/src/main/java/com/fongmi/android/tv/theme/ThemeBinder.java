package com.fongmi.android.tv.theme;

import android.app.Dialog;
import android.content.res.ColorStateList;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.InsetDrawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.RippleDrawable;
import android.graphics.drawable.ScaleDrawable;
import android.graphics.drawable.StateListDrawable;
import android.os.Looper;
import android.os.SystemClock;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import com.fongmi.android.tv.R;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.shape.MaterialShapeDrawable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Controlled runtime channel for user theme overrides.
 *
 * <p>Android offers no public API to rewrite a compiled {@code ?attr/color*}
 * value, so the binder walks the already-created view tree and swaps only colors
 * that provably came from a semantic role: a view color is rewritten when it
 * exactly equals a frozen baseline role value and is not stateful. Everything
 * else - player surfaces, media, posters, brand and health colors, focus
 * selectors, unknown colors - is left untouched.
 *
 * <p>This runs on the main thread only. Untagged views remain untouched when the
 * active tokens equal the baseline; explicit semantic role tags are still bound
 * from the active token snapshot. A view is never rebound more than once per token signature.
 */
public final class ThemeBinder {

    private static final String IGNORE_TAG = "webhtv:ignore";
    private static final String FOCUS_STROKE_TAG = "webhtv:focus-stroke";
    private static final BindingSpec EMPTY_SPEC = new BindingSpec(null, 0f, Map.of(), Map.of(), List.of("pressed", "focused", "selected", "activated"), "");
    private static final int[] STATE_ATTRIBUTES = {
            android.R.attr.state_enabled,
            android.R.attr.state_pressed,
            android.R.attr.state_focused,
            android.R.attr.state_hovered,
            android.R.attr.state_selected,
            android.R.attr.state_activated,
            android.R.attr.state_checked,
            android.R.attr.state_checkable,
            android.R.attr.state_expanded,
            android.R.attr.state_window_focused,
    };
    private static final int[][] STATE_COMBINATIONS = stateCombinations();
    private static final String[] EXEMPT_CLASS_MARKERS = {
            "surface", "texture", "video", "player", "danmaku", "subtitle",
            "karaoke", "wall", "logo", "rating", "media",
    };

    private static final Map<View, BindingState> BINDINGS = new WeakHashMap<>();
    private static final Map<RecyclerView, ChildWatcher> WATCHED = new WeakHashMap<>();
    private static final Map<View, RootWatcher> ROOTS = new WeakHashMap<>();
    private static final Map<Object, OwnedDrawableBinding> OWNED_BACKGROUNDS = new WeakHashMap<>();
    private static final Map<ColorStateList, Map<RewriteKey, ColorStateList>> REBUILT = new WeakHashMap<>();

    private static long cachedSignature;

    private static volatile long lastBindMillis;
    private static volatile int lastBoundViews;
    private static volatile int lastWalkedViews;

    private ThemeBinder() {
    }

    /** Elapsed time of the most recent non-trivial bind, for the performance gate. */
    public static long lastBindMillis() {
        return lastBindMillis;
    }

    /** Number of views rewritten by the most recent non-trivial bind. */
    public static int lastBoundViews() {
        return lastBoundViews;
    }

    /** Marks a known selector as having a Focus-role outline, preserving any app-owned tag. */
    public static void markFocusStroke(View view, float widthDp) {
        if (view == null || !Float.isFinite(widthDp) || widthDp <= 0f || widthDp > 16f) return;
        view.setTag(R.id.webhtv_theme_binding, FOCUS_STROKE_TAG + ":" + widthDp);
    }

    /** Attaches view-local semantic state metadata without replacing the app's unkeyed tag. */
    public static void markSemanticBinding(View view, String metadata) {
        if (view == null || metadata == null) return;
        BindingSpec spec = BindingSpec.from(metadata);
        if (spec.hasBinding()) view.setTag(R.id.webhtv_theme_binding, metadata);
    }

    public static void bind(View root, ThemeTokens baseline, ThemeTokens active) {
        if (root == null || baseline == null || active == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) return;
        if (baseline.equals(active) && !containsExplicitRole(root) && !containsBoundView(root)) {
            // Default profile without semantic tags remains a strict no-write path.
            lastBindMillis = 0L;
            lastBoundViews = 0;
            return;
        }
        long started = SystemClock.elapsedRealtimeNanos();
        long signature = signature(baseline, active);
        if (signature != cachedSignature) {
            REBUILT.clear();
            cachedSignature = signature;
        }
        BindingContext context = new BindingContext(ThemeColorIndex.of(baseline), active, signature);
        int bound = walk(root, context);
        watchRoot(root, context);
        lastBoundViews = bound;
        lastBindMillis = (SystemClock.elapsedRealtimeNanos() - started) / 1_000_000L;
    }

    /** Binds both the dialog view tree and its separate window-owned shell. */

    /** Binds both the dialog view tree and its separate window-owned shell. */
    public static void bindDialog(Dialog dialog, ThemeTokens baseline, ThemeTokens active) {
        if (dialog == null || baseline == null || active == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) return;
        Window window = dialog.getWindow();
        if (window == null) return;
        View decor = window.getDecorView();
        bindOwnedBackground(window, decor.getBackground(), decor.getResources(),
                window::setBackgroundDrawable, baseline, active, true);
        bind(decor, baseline, active);
    }

    private static int walk(View root, BindingContext context) {
        int bound = 0;
        int walked = 0;
        Deque<View> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            View view = queue.poll();
            if (view == null) continue;
            walked++;
            if (isExempt(view) && !hasThemeBinding(view)) continue;
            if (bindView(view, context)) bound++;
            if (view instanceof RecyclerView recyclerView) watchChildren(recyclerView, context);
            if (view instanceof ViewGroup group) {
                for (int child = 0; child < group.getChildCount(); child++) {
                    queue.add(group.getChildAt(child));
                }
            }
        }
        lastWalkedViews = walked;
        return bound;
    }

    /**
     * Fragments and async content attach after the Activity bind points. The root
     * is re-walked only when its descendant count actually changed, which keeps the
     * scroll path free of repeated traversals.
     */
    private static void watchRoot(View root, BindingContext context) {
        RootWatcher watcher = ROOTS.get(root);
        if (watcher != null) {
            watcher.context = context;
            return;
        }
        watcher = new RootWatcher(context, countDescendants(root));
        RootWatcher state = watcher;
        ROOTS.put(root, watcher);
        root.addOnLayoutChangeListener((view, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) -> {
            int count = countDescendants(view);
            if (state.descendants == count) return;
            state.descendants = count;
            walk(view, state.context);
        });
    }

    private static int countDescendants(View view) {
        if (!(view instanceof ViewGroup group)) return 1;
        int count = 1;
        for (int child = 0; child < group.getChildCount(); child++) count += countDescendants(group.getChildAt(child));
        return count;
    }

    private static boolean containsExplicitRole(View root) {
        Deque<View> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            View view = queue.poll();
            if (hasThemeBinding(view)) return true;
            if (view instanceof ViewGroup group) {
                for (int child = 0; child < group.getChildCount(); child++) queue.add(group.getChildAt(child));
            }
        }
        return false;
    }

    private static void watchChildren(RecyclerView recyclerView, BindingContext context) {
        ChildWatcher existing = WATCHED.get(recyclerView);
        if (existing != null) {
            existing.context = context;
            return;
        }
        ChildWatcher watcher = new ChildWatcher(context);
        RecyclerView.OnChildAttachStateChangeListener listener = new RecyclerView.OnChildAttachStateChangeListener() {
            @Override
            public void onChildViewAttachedToWindow(View view) {
                walk(view, watcher.context);
            }

            @Override
            public void onChildViewDetachedFromWindow(View view) {
                // Keep the per-view source snapshot while a holder is recycled so a
                // later palette bind can replace only the colors still owned by us.
            }
        };
        watcher.listener = listener;
        WATCHED.put(recyclerView, watcher);
        recyclerView.addOnChildAttachStateChangeListener(listener);
    }

    private static boolean bindView(View view, BindingContext context) {
        BindingState state = BINDINGS.computeIfAbsent(view, ignored -> new BindingState());
        BindingSpec spec = BindingSpec.from(themeTag(view));
        ThemeRole explicit = spec.role;
        boolean changed = false;
        boolean customStateBackground = hasStateListDrawable(view.getBackground(), new IdentityHashMap<>());
        ThemeRole foreground = foregroundRole(explicit);
        if (view instanceof TextView textView) changed |= bindText(textView, state, foreground, spec, context);
        if (view instanceof ImageView imageView) changed |= bindImageTint(imageView, state, foreground, spec, context);
        if (view instanceof MaterialCardView cardView && !customStateBackground) {
            changed |= bindCard(cardView, state, explicit, spec, context);
        }
        if (view instanceof MaterialButton button && !customStateBackground) {
            changed |= bindButton(button, state, spec, context);
        }
        changed |= bindBackgroundTint(view, state, explicit, spec, context);
        if (!(view instanceof MaterialButton || view instanceof MaterialCardView) || customStateBackground) {
            changed |= bindBackground(view, state, explicit, spec, context);
        }
        state.signature = context.signature;
        return changed;
    }

    private static boolean bindText(TextView view, BindingState state, ThemeRole explicit, BindingSpec spec,
                                    BindingContext context) {
        boolean changed = false;
        changed |= bindColorList(view.getTextColors(), state.text, explicit, false, spec, "text", context, view::setTextColor);
        changed |= bindColorList(view.getHintTextColors(), state.hint, explicit, false, spec, "text", context, view::setHintTextColor);
        return changed;
    }

    private static boolean bindImageTint(ImageView view, BindingState state, ThemeRole explicit, BindingSpec spec,
                                         BindingContext context) {
        return bindColorList(view.getImageTintList(), state.imageTint, explicit, false, spec, "icon", context,
                view::setImageTintList);
    }

    private static boolean bindCard(MaterialCardView view, BindingState state, ThemeRole explicit, BindingSpec spec,
                                    BindingContext context) {
        boolean changed = false;
        changed |= bindColorList(view.getCardBackgroundColor(), state.cardBackground, explicit, false,
                spec, "fill", context, view::setCardBackgroundColor);
        ColorStateList stroke = view.getStrokeColorStateList();
        changed |= bindColorList(stroke, state.cardStroke, null, false, spec, "stroke", context, view::setStrokeColor);
        return changed;
    }

    /**
     * Filled and outlined Material buttons keep their fill in a background tint and
     * their border in a stroke colour rather than in a GradientDrawable, so the
     * generic drawable path never reached them and a themed button stayed on the
     * compiled palette. Both channels are still resolved from the same semantic
     * attributes and obey the identical exact-match rule.
     */
    private static boolean bindButton(MaterialButton view, BindingState state, BindingSpec spec, BindingContext context) {
        boolean changed = false;
        changed |= bindColorList(view.getStrokeColor(), state.buttonStroke, null, spec.focusStrokeDp > 0f,
                spec, "stroke", context, view::setStrokeColor);
        changed |= bindColorList(view.getIconTint(), state.buttonIcon, null, false, spec, "icon", context, view::setIconTint);
        return changed;
    }

    /**
     * Covers every view that fills itself through {@code app:backgroundTint} - the
     * Material button family, chips, FABs and text-input boxes - with the same
     * "only an exact baseline role colour" guard the rest of the binder uses.
     */
    private static boolean bindBackgroundTint(View view, BindingState state, ThemeRole explicit, BindingSpec spec,
                                              BindingContext context) {
        return bindColorList(view.getBackgroundTintList(), state.backgroundTint, explicit, false,
                spec, "fill", context, view::setBackgroundTintList);
    }

    private static boolean bindBackground(View view, BindingState state, ThemeRole explicit, BindingSpec spec,
                                          BindingContext context) {
        Drawable current = view.getBackground();
        if (state.background.applied != null && current == state.background.applied
                && state.signature == context.signature) return false;
        Drawable source = state.background.applied != null && current == state.background.applied
                ? state.background.source : current;
        if (source == null) {
            state.background.clear();
            return false;
        }
        Drawable copy = cloneDrawable(source, view);
        if (copy == null) {
            state.background.clear();
            return false;
        }
        boolean changed = bindDrawable(copy, explicit, spec, context.index, context.active, context.signature, view);
        if (changed) {
            if (copy != current) view.setBackground(copy);
            state.background.source = source;
            state.background.applied = copy;
            state.background.signature = context.signature;
            return true;
        }
        if (current == state.background.applied && state.background.applied != null) {
            view.setBackground(source);
            state.background.clear();
            return true;
        }
        state.background.source = current;
        state.background.applied = null;
        return false;
    }

    private static boolean bindColorList(ColorStateList current, ColorListBinding state, ThemeRole explicit,
                                         boolean focusOnly, BindingSpec spec, String channel,
                                         BindingContext context, ColorListSetter setter) {
        if (current == null) {
            state.clear();
            return false;
        }
        boolean stillOwned = state.applied != null && current == state.applied;
        if (stillOwned && state.signature == context.signature) return false;
        ColorStateList source = stillOwned ? state.source : current;
        ColorStateList replacement = rewrite(source, explicit, spec, channel, context.index,
                context.active, context.signature, focusOnly);
        if (replacement != null) {
            if (replacement != current) setter.set(replacement);
            state.source = source;
            state.applied = replacement;
            state.signature = context.signature;
            return replacement != current;
        }
        if (stillOwned) {
            setter.set(source);
            state.clearApplied();
            return true;
        }
        state.source = current;
        state.applied = null;
        return false;
    }

    /**
     * Rewrites a window-level background drawable.
     *
     * <p>{@code MaterialAlertDialogBuilder} builds the dialog panel as a
     * {@link MaterialShapeDrawable} wrapped in an {@link InsetDrawable} and hands it to
     * {@code Window.setBackgroundDrawable}. That drawable belongs to the window, not to
     * any {@code View}, so the ordinary view-tree walk can never reach it - which is why
     * a themed dialog changed its text but kept the compiled panel colour. The builder
     * exposes the very same instance through {@code getBackground()}, so it is rewritten
     * here through one shared code path instead of a second colour model.
     */
    public static boolean bindWindowBackground(Drawable drawable, ThemeTokens baseline, ThemeTokens active) {
        if (drawable == null || baseline == null || active == null) return false;
        if (Looper.myLooper() != Looper.getMainLooper()) return false;
        if (baseline.equals(active)) return false;
        Drawable isolated = drawable.mutate();
        boolean changed = bindDrawable(isolated, null, EMPTY_SPEC, ThemeColorIndex.of(baseline), active,
                signature(baseline, active), null);
        // dialogOpacity is documented as "the dialog/BottomSheet shell only, never the text
        // alpha", so it is applied to this window background and nowhere else. Without this
        // the editor's dialog-opacity slider changed only the preview swatch and the Web
        // snapshot while real dialogs stayed fully opaque.
        changed |= applyShellOpacity(drawable, active.dialogOpacity());
        return changed;
    }

    /** Binds a PopupWindow shell without applying the dialog-only opacity slot. */
    /** Binds a PopupWindow's independent content tree and safely rebinds its shell. */
    public static void bindPopupWindow(PopupWindow popup, ThemeTokens baseline, ThemeTokens active) {
        if (popup == null || baseline == null || active == null) return;
        if (Looper.myLooper() != Looper.getMainLooper()) return;
        View content = popup.getContentView();
        if (content != null) bind(content, baseline, active);
        bindOwnedBackground(popup, popup.getBackground(), content == null ? null : content.getResources(),
                popup::setBackgroundDrawable, baseline, active, false);
    }

    private static boolean bindOwnedBackground(Object owner, Drawable current, android.content.res.Resources resources,
                                               DrawableSetter setter, ThemeTokens baseline, ThemeTokens active,
                                               boolean applyOpacity) {
        if (owner == null || current == null || baseline == null || active == null) return false;
        if (Looper.myLooper() != Looper.getMainLooper()) return false;
        long activeSignature = signature(baseline, active);
        OwnedDrawableBinding state = OWNED_BACKGROUNDS.computeIfAbsent(owner, ignored -> new OwnedDrawableBinding());
        Drawable applied = state.applied == null ? null : state.applied.get();
        if (applied != null && current == applied && state.signature == activeSignature) return false;

        Drawable source = applied != null && current == applied ? state.source : current;
        Drawable snapshot = cloneDrawable(source, resources);
        Drawable copy = snapshot == null ? null : cloneDrawable(snapshot, resources);
        if (snapshot == null || copy == null) {
            state.clear();
            OWNED_BACKGROUNDS.remove(owner);
            return false;
        }

        boolean changed = bindDrawable(copy, null, EMPTY_SPEC, ThemeColorIndex.of(baseline), active,
                activeSignature, null);
        if (applyOpacity) changed |= applyShellOpacity(copy, active.dialogOpacity());
        if (changed) {
            setter.set(copy);
            state.source = snapshot;
            state.applied = new java.lang.ref.WeakReference<>(copy);
            state.signature = activeSignature;
            return true;
        }
        if (applied != null && current == applied) {
            setter.set(snapshot);
            state.clear();
            OWNED_BACKGROUNDS.remove(owner);
            return true;
        }
        state.clear();
        OWNED_BACKGROUNDS.remove(owner);
        return false;
    }

    /**
     * Scales the alpha of a window background fill, preserving whatever transparency the
     * drawable already had. A shell opacity of 1.0 is the shipped default and must stay a
     * strict no-op so the frozen palette path remains byte-identical.
     */
    private static boolean applyShellOpacity(Drawable drawable, float opacity) {
        if (drawable == null || opacity >= 1f) return false;
        if (opacity < 0f) return false;
        if (drawable instanceof InsetDrawable inset) {
            return applyShellOpacity(inset.getDrawable(), opacity);
        }
        if (drawable instanceof LayerDrawable layers) {
            boolean changed = false;
            for (int index = 0; index < layers.getNumberOfLayers(); index++) {
                changed |= applyShellOpacity(layers.getDrawable(index), opacity);
            }
            return changed;
        }
        if (drawable instanceof RippleDrawable ripple) {
            boolean changed = false;
            for (int index = 0; index < ripple.getNumberOfLayers(); index++) {
                if (ripple.getId(index) != android.R.id.mask) {
                    changed |= applyShellOpacity(ripple.getDrawable(index), opacity);
                }
            }
            return changed;
        }
        if (drawable instanceof MaterialShapeDrawable shape) {
            ColorStateList fill = shape.getFillColor();
            if (fill == null) return false;
            int original = fill.getDefaultColor();
            int target = scaleAlpha(original, opacity);
            if (target == original) return false;
            shape.setFillColor(ColorStateList.valueOf(target));
            return true;
        }
        if (drawable instanceof GradientDrawable shape) {
            ColorStateList color = shape.getColor();
            if (color == null) return false;
            int original = color.getDefaultColor();
            int target = scaleAlpha(original, opacity);
            if (target == original) return false;
            shape.setColor(target);
            return true;
        }
        return false;
    }

    /** Package-private so the alpha arithmetic can be asserted without an Android device. */
    static int scaleAlpha(int color, float opacity) {
        int alpha = Math.round(((color >>> 24) & 0xFF) * Math.max(0f, Math.min(1f, opacity)));
        return (color & 0x00FFFFFF) | (Math.max(0, Math.min(255, alpha)) << 24);
    }

    /**
     * Rewrites the solid fill of a background drawable, unwrapping the transparent
     * wrappers Android and Material put around real panels.
     *
     * <p>Two families are covered by the same exact-match rule as everything else:
     * {@link GradientDrawable} (classic XML shapes) and
     * {@link MaterialShapeDrawable} (Chip, text-input box, bottom sheet and the
     * AlertDialog panel). Material's alert background is additionally wrapped in an
     * {@link InsetDrawable} carrying the window insets, so wrappers are unwrapped
     * recursively instead of being treated as an unknown drawable.
     */
    private static boolean bindDrawable(Drawable drawable, ThemeRole explicit, BindingSpec spec, ThemeColorIndex index,
                                        ThemeTokens active, long signature, View owner) {
        return bindDrawable(drawable, explicit, spec, index, active, signature, owner, new IdentityHashMap<>());
    }

    private static boolean bindDrawable(Drawable drawable, ThemeRole explicit, BindingSpec spec,
                                        ThemeColorIndex index, ThemeTokens active, long signature, View owner,
                                        IdentityHashMap<Drawable, Boolean> visited) {
        if (drawable == null) return false;
        if (visited.put(drawable, Boolean.TRUE) != null) return false;
        if (drawable instanceof InsetDrawable inset) {
            return bindDrawable(inset.getDrawable(), explicit, spec, index, active, signature, owner, visited);
        }
        if (drawable instanceof ScaleDrawable scale) {
            return bindDrawable(scale.getDrawable(), explicit, spec, index, active, signature, owner, visited);
        }
        if (drawable instanceof StateListDrawable states) {
            boolean changed = false;
            for (int position = 0; position < states.getStateCount(); position++) {
                Drawable child = states.getStateDrawable(position);
                int[] stateSet = states.getStateSet(position);
                ThemeRole stateStroke = spec.roleFor("stroke", stateSet);
                float stateStrokeDp = spec.strokeWidthFor(stateSet);
                int widthPx = Math.max(1, Math.round(stateStrokeDp * (owner == null
                        ? 1f : owner.getResources().getDisplayMetrics().density)));
                if (stateStroke != null && stateStrokeDp > 0f) {
                    changed |= setFocusStroke(child, widthPx, stateStroke.colorOf(active), new IdentityHashMap<>());
                } else if (hasState(stateSet, android.R.attr.state_focused) && spec.focusStrokeDp > 0f) {
                    changed |= setFocusStroke(child, widthPx, active.colorFocus(),
                            new IdentityHashMap<>());
                }
                ThemeRole stateFill = spec.roleFor("fill", stateSet);
                changed |= bindDrawable(child, stateFill == null ? explicit : stateFill, spec,
                        index, active, signature, owner, visited);
            }
            return changed;
        }
        if (drawable instanceof RippleDrawable ripple) {
            boolean changed = false;
            for (int position = 0; position < ripple.getNumberOfLayers(); position++) {
                if (ripple.getId(position) == android.R.id.mask) continue;
                changed |= bindDrawable(ripple.getDrawable(position), explicit, spec, index, active,
                        signature, owner, visited);
            }
            return changed;
        }
        if (drawable instanceof LayerDrawable layers) {
            boolean changed = false;
            for (int position = 0; position < layers.getNumberOfLayers(); position++) {
                if (layers.getId(position) == android.R.id.mask) continue;
                changed |= bindDrawable(layers.getDrawable(position), explicit, spec, index, active,
                        signature, owner, visited);
            }
            return changed;
        }
        if (drawable instanceof GradientDrawable shape) {
            boolean changed = false;
            ColorStateList color = rewrite(shape.getColor(), explicit, spec, "fill", index, active, signature, false);
            if (color != null) {
                shape.setColor(color);
                changed = true;
            }
            return changed;
        }
        if (drawable instanceof MaterialShapeDrawable shape) {
            ColorStateList color = rewrite(shape.getFillColor(), explicit, spec, "fill", index, active, signature, false);
            if (color == null) return false;
            shape.setFillColor(color);
            return true;
        }
        if (drawable instanceof ColorDrawable color) {
            Integer replacement = singleColor(color.getColor(), explicit, index, active);
            if (replacement == null) return false;
            color.setColor(replacement);
            return true;
        }
        return false;
    }

    private static boolean setFocusStroke(Drawable drawable, int widthPx, int color,
                                          IdentityHashMap<Drawable, Boolean> visited) {
        if (drawable == null || visited.put(drawable, Boolean.TRUE) != null) return false;
        if (drawable instanceof GradientDrawable shape) {
            shape.setStroke(Math.max(1, widthPx), color);
            return true;
        }
        if (drawable instanceof MaterialShapeDrawable shape) {
            shape.setStroke(Math.max(1, widthPx), ColorStateList.valueOf(color));
            return true;
        }
        if (drawable instanceof InsetDrawable inset) return setFocusStroke(inset.getDrawable(), widthPx, color, visited);
        if (drawable instanceof ScaleDrawable scale) return setFocusStroke(scale.getDrawable(), widthPx, color, visited);
        if (drawable instanceof LayerDrawable layers) {
            boolean changed = false;
            for (int index = 0; index < layers.getNumberOfLayers(); index++) {
                if (layers.getId(index) != android.R.id.mask) {
                    changed |= setFocusStroke(layers.getDrawable(index), widthPx, color, visited);
                }
            }
            return changed;
        }
        if (drawable instanceof RippleDrawable ripple) {
            boolean changed = false;
            for (int index = 0; index < ripple.getNumberOfLayers(); index++) {
                if (ripple.getId(index) != android.R.id.mask) {
                    changed |= setFocusStroke(ripple.getDrawable(index), widthPx, color, visited);
                }
            }
            return changed;
        }
        return false;
    }

    /**
     * Returns a recolored copy of the list, preserving its state behaviour, or null
     * when the colour is unknown/unchanged or the state shape cannot be rebuilt.
     */
    private static ColorStateList rewrite(ColorStateList source, ThemeRole explicit, BindingSpec spec,
                                          String channel, ThemeColorIndex index, ThemeTokens active,
                                          long signature, boolean focusOnly) {
        if (source == null) return null;
        if (!source.isStateful()) {
            if (focusOnly) return null;
            Integer replacement = singleColor(source.getDefaultColor(), explicit, index, active);
            return replacement == null ? null : ColorStateList.valueOf(replacement);
        }
        RewriteKey key = new RewriteKey(signature, explicit, spec.key, channel, focusOnly);
        Map<RewriteKey, ColorStateList> variants = REBUILT.computeIfAbsent(source, ignored -> new HashMap<>());
        ColorStateList cached = variants.get(key);
        if (cached != null) return cached;
        int[][] states = copyStateCombinations();
        int[] colors = new int[states.length];
        boolean changed = false;
        for (int position = 0; position < states.length; position++) {
            int original = resolvedColor(source, states[position]);
            boolean focused = hasState(states[position], android.R.attr.state_focused);
            ThemeRole stateRole = spec.roleFor(channel, states[position]);
            Integer replacement = focusOnly && focused ? index.replacementForRole(original, ThemeRole.FOCUS, active)
                    : stateRole != null ? index.replacementForRole(original, stateRole, active)
                    : singleColor(original, explicit, index, active);
            colors[position] = replacement == null ? original : replacement;
            changed |= replacement != null && replacement != original;
        }
        if (!changed) return null;
        ColorStateList rebuilt = new ColorStateList(states, colors);
        variants.put(key, rebuilt);
        return rebuilt;
    }



    private static int resolvedColor(ColorStateList source, int[] state) {
        return source.getColorForState(state, source.getDefaultColor());
    }

    private static Integer singleColor(int current, ThemeRole explicit, ThemeColorIndex index, ThemeTokens active) {
        if (explicit != null) return index.replacementForRole(current, explicit, active);
        return index.replacementFor(current, active);
    }

    private static ThemeRole foregroundRole(ThemeRole fill) {
        if (fill == null) return null;
        return switch (fill) {
            case PRIMARY -> ThemeRole.ON_PRIMARY;
            case PRIMARY_CONTAINER -> ThemeRole.ON_PRIMARY_CONTAINER;
            case SECONDARY_CONTAINER -> ThemeRole.ON_SECONDARY_CONTAINER;
            case ERROR -> ThemeRole.ON_ERROR;
            case ERROR_CONTAINER -> ThemeRole.ON_ERROR_CONTAINER;
            default -> fill;
        };
    }

    private static boolean isExempt(View view) {
        Object tag = view.getTag();
        if (tag instanceof String value && IGNORE_TAG.equals(value.trim().toLowerCase(Locale.ROOT))) return true;
        String name = view.getClass().getName().toLowerCase(Locale.ROOT);
        for (String marker : EXEMPT_CLASS_MARKERS) {
            if (name.contains(marker)) return true;
        }
        return false;
    }

    private static boolean hasThemeBinding(View view) {
        return BindingSpec.from(themeTag(view)).hasBinding();
    }

    private static Object themeTag(View view) {
        Object binding = view.getTag(R.id.webhtv_theme_binding);
        return binding == null ? view.getTag() : binding;
    }

    private static float focusStrokeWidth(Object tag) {
        return BindingSpec.from(tag).focusStrokeDp;
    }

    private static boolean containsBoundView(View root) {
        Deque<View> queue = new ArrayDeque<>();
        queue.add(root);
        while (!queue.isEmpty()) {
            View view = queue.poll();
            BindingState state = BINDINGS.get(view);
            if (state != null && state.hasApplied()) return true;
            if (view instanceof ViewGroup group) {
                for (int child = 0; child < group.getChildCount(); child++) queue.add(group.getChildAt(child));
            }
        }
        return false;
    }

    private static Drawable cloneDrawable(Drawable source, View owner) {
        return cloneDrawable(source, owner == null ? null : owner.getResources());
    }

    private static Drawable cloneDrawable(Drawable source, android.content.res.Resources resources) {
        if (source == null) return null;
        Drawable.ConstantState constantState = source.getConstantState();
        if (constantState == null) return null;
        Drawable copy = resources == null ? constantState.newDrawable() : constantState.newDrawable(resources);
        return copy.mutate();
    }

    private static boolean hasStateListDrawable(Drawable drawable, IdentityHashMap<Drawable, Boolean> visited) {
        if (drawable == null || visited.put(drawable, Boolean.TRUE) != null) return false;
        if (drawable instanceof StateListDrawable) return true;
        if (drawable instanceof InsetDrawable inset) return hasStateListDrawable(inset.getDrawable(), visited);
        if (drawable instanceof ScaleDrawable scale) return hasStateListDrawable(scale.getDrawable(), visited);
        if (drawable instanceof RippleDrawable ripple) {
            for (int index = 0; index < ripple.getNumberOfLayers(); index++) {
                if (ripple.getId(index) != android.R.id.mask && hasStateListDrawable(ripple.getDrawable(index), visited)) return true;
            }
        }
        if (drawable instanceof LayerDrawable layers) {
            for (int index = 0; index < layers.getNumberOfLayers(); index++) {
                if (layers.getId(index) != android.R.id.mask && hasStateListDrawable(layers.getDrawable(index), visited)) return true;
            }
        }
        return false;
    }

    private static boolean hasState(int[] stateSet, int stateAttribute) {
        if (stateSet == null) return false;
        for (int state : stateSet) {
            if (state == stateAttribute) return true;
        }
        return false;
    }

    private static int[][] stateCombinations() {
        int count = 1 << STATE_ATTRIBUTES.length;
        int[][] states = new int[count][STATE_ATTRIBUTES.length];
        int output = 0;
        int[] masks = new int[count];
        masks[output++] = 1; // Enabled, ordinary/default state is the ColorStateList default.
        masks[output++] = 0;
        for (int mask = 2; mask < count; mask++) masks[output++] = mask;
        for (int position = 0; position < count; position++) {
            int mask = masks[position];
            for (int bit = 0; bit < STATE_ATTRIBUTES.length; bit++) {
                states[position][bit] = (mask & (1 << bit)) != 0
                        ? STATE_ATTRIBUTES[bit] : -STATE_ATTRIBUTES[bit];
            }
        }
        return states;
    }

    private static long signature(ThemeTokens baseline, ThemeTokens active) {
        // ThemeTokens is a content-based record; its generated hashCode is stable
        // across the desugared Android runtime. Java-16 record reflection is
        // deliberately avoided because it does not exist on Android API < 33.
        return ((long) baseline.hashCode() << 32) ^ (active.hashCode() & 0xFFFFFFFFL);
    }

    private static int[][] copyStateCombinations() {
        int[][] copy = new int[STATE_COMBINATIONS.length][];
        for (int position = 0; position < STATE_COMBINATIONS.length; position++) {
            copy[position] = STATE_COMBINATIONS[position].clone();
        }
        return copy;
    }

    private static final class BindingContext {
        final ThemeColorIndex index;
        final ThemeTokens active;
        final long signature;

        BindingContext(ThemeColorIndex index, ThemeTokens active, long signature) {
            this.index = index;
            this.active = active;
            this.signature = signature;
        }
    }

    private static final class RootWatcher {
        BindingContext context;
        int descendants;

        RootWatcher(BindingContext context, int descendants) {
            this.context = context;
            this.descendants = descendants;
        }
    }

    private static final class ChildWatcher {
        BindingContext context;
        RecyclerView.OnChildAttachStateChangeListener listener;

        ChildWatcher(BindingContext context) {
            this.context = context;
        }
    }

    /** Small per-view adapter metadata; it does not own or store palette values. */
    private static final class BindingSpec {
        final ThemeRole role;
        final float focusStrokeDp;
        final Map<String, ThemeRole> stateRoles;
        final Map<String, Float> stateStrokeWidths;
        final List<String> statePriority;
        final String key;

        BindingSpec(ThemeRole role, float focusStrokeDp, Map<String, ThemeRole> stateRoles,
                    Map<String, Float> stateStrokeWidths, List<String> statePriority, String key) {
            this.statePriority = statePriority;
            this.role = role;
            this.focusStrokeDp = focusStrokeDp;
            this.stateRoles = stateRoles;
            this.stateStrokeWidths = stateStrokeWidths;
            this.key = key;
        }

        static BindingSpec from(Object rawTag) {
            if (!(rawTag instanceof String value)) return EMPTY_SPEC;
            String normalized = value.trim().toLowerCase(Locale.ROOT);
            if (normalized.isEmpty()) return EMPTY_SPEC;
            ThemeRole role = null;
            float focusStroke = 0f;
            Map<String, ThemeRole> stateRoles = new HashMap<>();
            Map<String, Float> stateStrokeWidths = new HashMap<>();
            List<String> statePriority = new ArrayList<>(List.of("pressed", "focused", "selected", "activated"));
            for (String rawClause : normalized.split("[;,]")) {
                String clause = rawClause.trim();
                ThemeRole taggedRole = ThemeRole.fromTag(clause);
                if (taggedRole != null) {
                    role = taggedRole;
                    continue;
                }
                if (clause.equals(FOCUS_STROKE_TAG)) {
                    focusStroke = 1f;
                    continue;
                }
                if (clause.startsWith(FOCUS_STROKE_TAG + ":")) {
                    String width = clause.substring(FOCUS_STROKE_TAG.length() + 1).replace("dp", "").trim();
                    try {
                        float parsed = Float.parseFloat(width);
                        if (Float.isFinite(parsed) && parsed > 0f && parsed <= 16f) focusStroke = parsed;
                    } catch (NumberFormatException ignored) {
                        // Invalid metadata is ignored; the original selector remains authoritative.
                    }
                    continue;
                }
                if (!clause.startsWith("webhtv:")) continue;
                String declaration = clause.substring("webhtv:".length());
                int separator = declaration.indexOf(':');
                if (separator <= 0 || separator == declaration.length() - 1) continue;
                String stateChannel = declaration.substring(0, separator);
                if (stateChannel.equals("state-priority")) {
                    List<String> parsed = new ArrayList<>();
                    for (String state : declaration.substring(separator + 1).split("\\+")) {
                        if (isSupportedState(state) && !parsed.contains(state)) parsed.add(state);
                    }
                    if (!parsed.isEmpty()) statePriority = parsed;
                    continue;
                }
                if (stateChannel.endsWith("-stroke-width")) {
                    String state = stateChannel.substring(0, stateChannel.length() - "-stroke-width".length());
                    String width = declaration.substring(separator + 1).replace("dp", "").trim();
                    try {
                        float parsed = Float.parseFloat(width);
                        if (isSupportedState(state) && Float.isFinite(parsed) && parsed > 0f && parsed <= 16f) {
                            stateStrokeWidths.put(state, parsed);
                        }
                    } catch (NumberFormatException ignored) {
                        // Invalid geometry metadata is ignored.
                    }
                    continue;
                }
                ThemeRole stateRole = ThemeRole.fromTag("webhtv:" + declaration.substring(separator + 1));
                if (stateRole == null) continue;
                stateRoles.put(stateChannel, stateRole);
            }
            if (role == null && focusStroke == 0f && stateRoles.isEmpty() && stateStrokeWidths.isEmpty()) return EMPTY_SPEC;
            return new BindingSpec(role, focusStroke, Map.copyOf(stateRoles), Map.copyOf(stateStrokeWidths), List.copyOf(statePriority), normalized);
        }

        ThemeRole roleFor(String channel, int[] states) {
            if (states == null) return null;
            if (hasState(states, -android.R.attr.state_enabled)) return stateRoles.get("disabled-" + channel);
            for (String state : statePriority) {
                if (!hasState(states, stateAttribute(state))) continue;
                ThemeRole role = stateRoles.get(state + "-" + channel);
                if (role != null) return role;
            }
            return null;
        }

        float strokeWidthFor(int[] states) {
            if (states == null) return 0f;
            if (hasState(states, -android.R.attr.state_enabled)) return stateStrokeWidths.getOrDefault("disabled", 0f);
            for (String state : statePriority) {
                if (!hasState(states, stateAttribute(state))) continue;
                Float width = stateStrokeWidths.get(state);
                if (width != null) return width;
            }
            return hasState(states, android.R.attr.state_focused) ? focusStrokeDp : 0f;
        }

        private static boolean isSupportedState(String state) {
            return state.equals("disabled") || state.equals("pressed") || state.equals("focused")
                    || state.equals("selected") || state.equals("activated");
        }

        private static int stateAttribute(String state) {
            return switch (state) {
                case "pressed" -> android.R.attr.state_pressed;
                case "focused" -> android.R.attr.state_focused;
                case "selected" -> android.R.attr.state_selected;
                case "activated" -> android.R.attr.state_activated;
                default -> 0;
            };
        }

        boolean hasBinding() {
            return role != null || focusStrokeDp > 0f || !stateRoles.isEmpty() || !stateStrokeWidths.isEmpty();
        }
    }

    private static final class BindingState {
        final ColorListBinding text = new ColorListBinding();
        final ColorListBinding hint = new ColorListBinding();
        final ColorListBinding imageTint = new ColorListBinding();
        final ColorListBinding cardBackground = new ColorListBinding();
        final ColorListBinding cardStroke = new ColorListBinding();
        final ColorListBinding buttonStroke = new ColorListBinding();
        final ColorListBinding buttonIcon = new ColorListBinding();
        final ColorListBinding backgroundTint = new ColorListBinding();
        final DrawableBinding background = new DrawableBinding();
        long signature;

        boolean hasApplied() {
            return text.applied != null || hint.applied != null || imageTint.applied != null
                    || cardBackground.applied != null || cardStroke.applied != null
                    || buttonStroke.applied != null || buttonIcon.applied != null
                    || backgroundTint.applied != null || background.applied != null;
        }
    }

    private static final class ColorListBinding {
        ColorStateList source;
        ColorStateList applied;
        long signature;

        void clearApplied() {
            source = null;
            applied = null;
            signature = 0L;
        }

        void clear() {
            clearApplied();
        }
    }

    private static final class DrawableBinding {
        Drawable source;
        Drawable applied;
        long signature;

        void clear() {
            source = null;
            applied = null;
            signature = 0L;
        }
    }

    private static final class OwnedDrawableBinding {
        Drawable source;
        java.lang.ref.WeakReference<Drawable> applied;
        long signature;

        void clear() {
            source = null;
            applied = null;
            signature = 0L;
        }
    }

    private interface DrawableSetter {
        void set(Drawable drawable);
    }

    private interface ColorListSetter {
        void set(ColorStateList value);
    }

    private record RewriteKey(long signature, ThemeRole role, String spec, String channel, boolean focusOnly) {
    }
}
