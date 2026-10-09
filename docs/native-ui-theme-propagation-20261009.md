# Native UI theme propagation: design and recovery record

## Recovery anchor

- Objective: make existing ThemeProfile/ThemeTokens reach eligible native Mobile and Leanback views without a parallel theme engine or UI redesign; preserve Focus, Selected, Activated, Pressed, Disabled, Current, and independent player/media/business colors.
- Acceptance: ThemeController remains the sole runtime authority; compiled Android themes remain defaults; runtime binding safely updates proven semantic roles; Dialog, Popup, and RecyclerView descendants use current tokens; original selector state priority and drawable geometry remain; targeted behavior tests and both ARM64 Release builds pass. Device visuals remain pending user acceptance after local finish.
- Task: native-ui-theme-propagation-20261009; branch sync/upstream-beta-20261009; base HEAD c50fd87b1070d74089e3985cc1349b1fbe8e67c5; guard active; scope app/src and this record plus the visual-system index.
- Protected baseline: c50fd87 contains the Mobile player setting text-contrast fix, Leanback settings custom Focus binding, and their tests. The user confirmed both on device. Preserve without rework.
- Plan: repair stale root/RecyclerView watcher snapshots, preserve ColorStateList state specs during role replacement, constrain drawable adaptation to supported types, bind missing standalone windows/popups, and add state/lifecycle behavior contracts; then run focused verification and both ARM64 Release builds.
- Device acceptance has not happened. The user authorizes normal task_guard finish, local commit, and recovery tag immediately after source changes, required tests, and both ARM64 Release builds pass; do not push.

## Architecture decision

### No-change option

Android compiled theme attributes, styles, and selectors continue to supply system/light/dark defaults. They do not read the persisted user ThemeProfile, and existing Drawable instances do not automatically become a user-defined palette merely because their XML references a theme attribute.

### Platform-native option

Android's normal mechanism resolves Theme/Style attributes as Views are created and lets the application recreate themed Activities/Contexts for configuration changes. This fits compiled palettes. This app also supports runtime-editable profiles; using only Android resources would require every window and dynamic View to be created with a newly configured Context and would still not update already-created custom/nested Drawables.

### Narrow project adaptation

Retain ThemeProfile -> ThemeResolver -> ThemeTokens -> ThemeController -> ThemeBinder. ThemeBinder remains an adapter, not a store or second palette. It may update only View properties or explicitly supported Drawables whose semantic role is known. Preserve ColorStateList state specs and order. Isolate mutable Drawable instances. Do not infer distinct roles from colliding ARGB values. Existing player, media, brand, rating, wallpaper, and business-state exclusions remain.

## Local call-path evidence and known breaks

- Mobile and Leanback BaseActivity call ThemeController.bindTheme before and after view initialization. Leanback also uses SettingFocusTheme for the scoped settings selector; keep it.
- ThemeController.bindDialog sends the dialog decor to the binder. Shared helpers are already connected; About's main raw Dialog path needs explicit binding.
- PopupWindow content roots are separate View trees and are not guaranteed to be covered by an Activity root watcher.
- ThemeBinder watchRoot and RecyclerView attach listeners capture the first ThemeColorIndex, active tokens, and signature; after a profile change, newly attached descendants may receive stale tokens.
- ColorStateList rewriting currently rebuilds using ThemeColorIndex.stateSkeleton rather than each original state spec, which can merge states or alter priority.
- Drawable support is bounded to InsetDrawable, GradientDrawable, and MaterialShapeDrawable. StateListDrawable, LayerDrawable, and RippleDrawable are not recursively handled. Add only evidence-backed component/resource support and preserve the original graph.
- MaterialButton owns background tint, shape, stroke, icon tint, and ripple state. Custom backgrounds can bypass its managed path; bind through component properties while retaining stateful lists.

## Best-practice evidence (accessed 2026-10-09)

| Source | Revision / grade | Supported fact and decision |
|---|---|---|
| https://developer.android.com/develop/ui/views/theming/themes | Official Android docs, accessed 2026-10-09; A | Theme attributes are consumed by Views/styles that support them. Keep compiled themes as defaults; an XML attr is not proof of runtime Profile binding. |
| https://developer.android.com/reference/android/content/res/Resources.Theme | Official Android API, accessed 2026-10-09; A | Theme resolves resource values and applyStyle takes a style resource; it is not a general API for recoloring an existing View tree from user tokens. |
| https://developer.android.com/guide/topics/resources/drawable-resource.html | Official Android docs, accessed 2026-10-09; A | StateListDrawable selects the first matching state spec; states and order affect behavior, so preserve both when replacing colors. |
| https://developer.android.com/reference/android/graphics/drawable/Drawable#mutate() | Official Android API, accessed 2026-10-09; A | mutate isolates a drawable from shared ConstantState; isolate resource Drawables before applying per-view colors. |
| https://github.com/material-components/material-components-android/blob/1.14.0/lib/java/com/google/android/material/button/MaterialButton.java | Material Components source, tag 1.14.0; A | MaterialButton has component-owned shape/background tint/stroke/icon/ripple behavior; a generic background rewrite does not prove all channels are bound. |
| https://github.com/material-components/material-components-android/issues/1033 | Material Components issue; C, older alpha version only | Reports tint alpha blending in an older version. It is not evidence of a defect in this project's 1.14.0; use only as a reason to test stateful/alpha tint. |
| docs/THEME-CUSTOMIZATION-LAYERS-20260921.md, Layer 2B field report | Repository evidence; B | The no-role default path has a binder fast path. Keep runtime work bounded and avoid repeated full-tree walks during scrolling. |

## Test and risk boundary

- Behavior contracts cover state-spec preservation and role replacement, only the explicitly supported nested Drawable types, latest tokens after signature changes for root/RecyclerView children, repeated bind without wrapping/tint drift, preserved ignored/player colors, and Dialog/Popup window entry points.
- Tests should assert semantic roles, state arrays, Drawable types, and final View properties. If platform View behavior can only be protected with source-contract checks, label it an architecture contract.
- Risks: changed selector priority, shared Drawable state contamination, duplicate MaterialButton tint/stroke/ripple, stale watcher snapshots, accidental capture of player/business colors, and listener duplication after Activity recreation.
- Rollback boundary: changes stay in the existing ThemeBinder and necessary native window entry points. Do not rewrite SettingFocusTheme, ThemeProfile schema, or player/media code. After targeted tests and both ARM64 Release builds pass, finish the active task guard with a local commit and annotated recovery tag; device acceptance follows APK delivery. Do not push.

## Execution log

- 2026-10-09: closed old mobile-tv-focus-contrast-20261009 guard with local commit and recovery tag; started native-ui-theme-propagation-20261009.
- 2026-10-09: implemented bounded runtime theme propagation for supported selector/drawable state channels, latest-token rebinding for root and RecyclerView descendants, and owner-aware Dialog/PopupWindow binding. Added semantic role metadata at eligible native UI call sites while preserving existing selectors and excluded player/media/business colors.
- Verification: Mobile targeted theme/controller/player-sheet contracts passed; Leanback targeted theme, drawable runtime, Dialog/Popup, settings Focus, and player OSD contracts passed. Both `:app:assembleMobileArm64_v8aRelease` and `:app:assembleLeanbackArm64_v8aRelease` passed with the configured Java 21 / Android SDK / Gradle cache and no clean.
- Release APKs: Mobile `app/build/outputs/apk/mobileArm64_v8a/release/mobile-arm64_v8a.apk`, 171509618 bytes, SHA256 `3FF0B18286BA80A9B70AE7009E7277D2C1C93A83BE7A430889C1FAE0BDF97D7E`; Leanback `app/build/outputs/apk/leanbackArm64_v8a/release/leanback-arm64_v8a.apk`, 171144287 bytes, SHA256 `F4E2931E0D5F3C3024D42AE4C7CE84FEA0ED66987EF38147F1460EE83BF9DD79`.
- Device acceptance: pending user true-device validation after APK delivery. Normal local task-guard finish is authorized now; no push.
