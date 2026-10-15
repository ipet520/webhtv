package com.fongmi.android.tv.theme;

import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Maps a concrete ARGB value back to the semantic role it played in the frozen
 * baseline palette.
 *
 * <p>The binder never guesses: a view color is only rewritten when it exactly
 * equals a baseline role value, and only when every role sharing that value
 * resolves to the same active color. Ambiguous or unknown colors are left alone.
 */
public final class ThemeColorIndex {

    private final Map<Integer, Set<ThemeRole>> rolesByColor;
    private final Map<Integer, Set<ThemeRole>> rolesByRgb;
    private final Map<ThemeRole, Integer> baselineByRole;

    private ThemeColorIndex(Map<Integer, Set<ThemeRole>> rolesByColor,
                            Map<Integer, Set<ThemeRole>> rolesByRgb,
                            Map<ThemeRole, Integer> baselineByRole) {
        this.rolesByColor = rolesByColor;
        this.rolesByRgb = rolesByRgb;
        this.baselineByRole = baselineByRole;
    }

    public static ThemeColorIndex of(ThemeTokens baseline) {
        Map<Integer, Set<ThemeRole>> byColor = new LinkedHashMap<>();
        Map<Integer, Set<ThemeRole>> byRgb = new LinkedHashMap<>();
        Map<ThemeRole, Integer> byRole = new EnumMap<>(ThemeRole.class);
        if (baseline != null) {
            for (ThemeRole role : ThemeRole.values()) {
                int color = role.colorOf(baseline);
                if (color == 0) continue;
                byRole.put(role, color);
                byColor.computeIfAbsent(color, ignored -> new LinkedHashSet<>()).add(role);
                byRgb.computeIfAbsent(color & 0x00FFFFFF, ignored -> new LinkedHashSet<>()).add(role);
            }
        }
        Map<Integer, Set<ThemeRole>> frozen = new LinkedHashMap<>();
        byColor.forEach((color, roles) -> frozen.put(color, Collections.unmodifiableSet(roles)));
        Map<Integer, Set<ThemeRole>> frozenRgb = new LinkedHashMap<>();
        byRgb.forEach((rgb, roles) -> frozenRgb.put(rgb, Collections.unmodifiableSet(roles)));
        return new ThemeColorIndex(Collections.unmodifiableMap(frozen),
                Collections.unmodifiableMap(frozenRgb), Collections.unmodifiableMap(byRole));
    }

    public boolean isEmpty() {
        return rolesByColor.isEmpty();
    }

    public Set<ThemeRole> rolesFor(int color) {
        Set<ThemeRole> roles = rolesByColor.get(color);
        return roles == null ? Set.of() : roles;
    }

    /**
     * The single role a baseline color belongs to, or {@code null} when the color
     * is unknown or shared by several roles.
     */
    public ThemeRole uniqueRoleFor(int color) {
        Set<ThemeRole> roles = rolesFor(color);
        return roles.size() == 1 ? roles.iterator().next() : null;
    }

    /**
     * The replacement color for a baseline view color, or {@code null} when the
     * color must be left untouched.
     *
     * <p>When several roles share the baseline color the replacement is only
     * returned if every one of those roles agrees on the active value; otherwise
     * the correct role cannot be determined and the view keeps its static color.
     */
    public Integer replacementFor(int color, ThemeTokens active) {
        if (active == null) return null;
        Set<ThemeRole> roles = rolesByColor.get(color);
        if (roles == null) roles = rolesByRgb.get(color & 0x00FFFFFF);
        if (roles == null || roles.isEmpty()) return null;
        Integer candidate = null;
        for (ThemeRole role : roles) {
            Integer resolved = resolvedReplacement(color, baselineByRole.get(role), role.colorOf(active));
            int effective = resolved == null ? color : resolved;
            if (candidate == null) candidate = effective;
            else if (candidate != effective) return null;
        }
        return candidate == null || candidate == color ? null : candidate;
    }

    /** Replaces a color when its semantic role is explicit, preserving selector alpha. */
    public Integer replacementForRole(int color, ThemeRole role, ThemeTokens active) {
        if (role == null || active == null) return null;
        return resolvedReplacement(color, baselineByRole.get(role), role.colorOf(active));
    }

    private static Integer resolvedReplacement(int source, Integer baseline, int active) {
        if (baseline == null || baseline == 0) return null;
        int baselineAlpha = (baseline >>> 24) & 0xFF;
        if (baselineAlpha == 0) return null;
        int sourceAlpha = (source >>> 24) & 0xFF;
        int activeAlpha = (active >>> 24) & 0xFF;
        int replacementAlpha = Math.max(0, Math.min(255,
                Math.round(sourceAlpha * (activeAlpha / (float) baselineAlpha))));
        int replacement = (active & 0x00FFFFFF) | (replacementAlpha << 24);
        return replacement == source ? null : replacement;
    }

    /** Baseline value of a role, used for explicit role tags. */
    public Integer baselineOf(ThemeRole role) {
        return baselineByRole.get(role);
    }

    /**
     * A practical, ordered set of common state combinations. Android exposes no
     * public accessor for a ColorStateList's original state array, so a stateful
     * list is rebuilt against this skeleton: every entry keeps the colour the
     * original list resolves to for that state, and unknown states therefore keep
     * their original value instead of being dropped.
     */
    public static final int[][] STATE_SKELETON = {
            {-android.R.attr.state_enabled},
            {android.R.attr.state_enabled, android.R.attr.state_pressed},
            {android.R.attr.state_enabled, android.R.attr.state_focused},
            {android.R.attr.state_enabled, android.R.attr.state_hovered},
            {android.R.attr.state_enabled, android.R.attr.state_selected},
            {android.R.attr.state_enabled, android.R.attr.state_activated},
            {android.R.attr.state_enabled, android.R.attr.state_checked},
            {android.R.attr.state_enabled, -android.R.attr.state_checked, android.R.attr.state_checkable},
            {android.R.attr.state_enabled, android.R.attr.state_expanded},
            {android.R.attr.state_enabled, -android.R.attr.state_expanded},
            {android.R.attr.state_enabled},
            {},
    };

    /** The ordered state skeleton used when a stateful list must be recolored. */
    public static int[][] stateSkeleton() {
        int[][] copy = new int[STATE_SKELETON.length][];
        for (int index = 0; index < STATE_SKELETON.length; index++) copy[index] = STATE_SKELETON[index].clone();
        return copy;
    }

}
