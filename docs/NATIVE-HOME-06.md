# NATIVE-HOME-06 — immersive native repairs

## Scope and protection

Implement the seven requested native presentation repairs; preserve the existing business,
settings, source drawer overlay and single HorizontalGridView scroll authority. No commits,
pushes, Release APK, Mobile/HTML edits or Suite gitlink changes are authorized.
App HEAD: `030485b70c263621fe4575abe5758b28115832c3`; initial dirty paths: 258.
External content fingerprints and allowed-file originals:
`%TEMP%/native-home-06-20261027-010135` (before.json, originals, scope.txt).
The exact task guard started successfully and protects 240 unrelated dirty paths.

## Design decision and evidence

Use the existing native view, controller generation, TMDB client/matcher/cache and Glide.
No change leaves clipping, missing artwork and incorrect filter/loading transitions.
Transplanting WOGG DOM/JS scheduling would duplicate business/focus authorities and is rejected.
The narrow native correction changes only presentation geometry, image subscriptions,
loading state projection, cross-region focus and pre-show drawer bounds.

Evidence reviewed 2026-10-27:

- Local `NativeHomeBrowseView.applyHeroLayout` and `HomePreviewGeometry`: fixed title
  slot can be smaller than two current font lines; preserve font sizing, budget slots upward.
- Local `NativeHomeBrowseImages`: a GONE logo may request 1x1, and source posters are
  stretched into the full-screen background. Use measured targets and a separate right poster.
- Local `NativeHomeHero.ExistingTmdb`: embedded detail bypasses missing-art enrichment;
  reuse its existing detail client and maintain identity/scope checks.
- Android ValueAnimator documentation, https://developer.android.com/reference/android/animation/ValueAnimator
  supports lifecycle-cancelled native drawing animations; no Handler animation loop.
- Glide targets documentation, https://bumptech.github.io/glide/doc/targets.html
  supports measured targets, cancellation/clear and target ownership. Does not prove device rendering.
- WOGG current dirty `tv-browse.css` / `tv-browse.js`, HEAD
  `be71724d3a168ba69dbc2c925d1ac71ac720d355`: poster fallback, hidden category row during
  filters, three-dot loading and category/rail return anchors are interaction references only.
  Current dirty files differ from committed HEAD; their fingerprints are in before.json.
- No upstream merge or dependency change is involved; upstream patch/PR/revert/paper evidence
  is inapplicable to these local view defects. Device-specific root correlation remains pending.

## User evidence and constraints

The previous version already fixed drawer-open black screen. Preserve that fix.
The latest TV photo captures a thin long frame across the entire top row while the drawer
is visible and the underlying home remains visible. It occurs on every house activation.
This rules out treating it as an occasional-only symptom or claiming pre-show width alone
is proven. Inspect top focus/background bounds and window focus handover as well.
Current drawer constrains width only in onShow; move first-window geometry before display.
Disable only redundant platform focus decoration on controls that already own selectors,
and keep container decoration absent; validate bounds and repeated open/close without reload.

## Acceptance and rollback

Verify Hero slots and safe bounds, measured/stale-safe image loads and poster fallback,
three-dot loading states, category/rail restore, 更新推荐 label, hidden categories during inline
filters, drawer first-layout bounds and retained overlay. Run scoped Robolectric tests and
complete Leanback ARM64 Debug APK; inspect signature/Manifest/ABI and provide SHA-256.
No device result may be marked passed without actual TV execution.
Rollback only by inverse task patches against external originals; never reset/restore the
working tree or overwrite unrelated changes.

## Module 8 — native history performance control

Added by the user during execution; same task and final APK. Scope extended before edits to
`HomePreviewPosterAdapter.java`, `HomePreviewTiming.java` and one comparison test. Originals
of both existing files copied externally; refined guard protects 238 unrelated dirty paths.

Actual classic route: HomeActivity.setRecyclerView selects CustomRowPresenter(16,
ZOOM_FACTOR_SMALL, FOCUS_SCROLL_ALIGNED) for HistoryPresenter ListRow -> ItemBridgeAdapter
-> ListRowPresenter grid -> HistoryPresenter. The presenter focus listener only changes text
marquee; images are bound via ImgUtil when holders bind and cleared on unbind. No Hero path.
Immersive: NativeHomeBrowseView.dispatchKeyEvent -> region-only coordinator -> same native
Grid focus/layout -> PosterAdapter focus scale/stroke -> bindHero -> focused pagination and
NativeHomeHero. There are two linear cards.indexOf calls in the current focus path. Before 06,
source imagery is loaded as a full-screen decode on every focus. This is confirmed additional
work, not a measured device latency attribution. The new portrait fallback must share rail
decode dimensions and move cache-missing visual preparation out of the immediate focus path
using the EXISTING Hero dwell; no second timer or scrolling engine.

No-change retains this avoidable work. Copying ListRow wholesale would alter accepted layout
and focus semantics. Narrow adaptation: holder-position callback, shared Glide size and native
existing dwell, plus bounded timing counters. Preserve 1.04/120ms scale, center keyline and
edge limits; do not blindly change pending-move limits without device evidence. Existing local
CustomRowPresenter/HistoryPresenter and cached Leanback 1.2.0 / leanback-grid 1.0.0 artifacts
are the exact baseline. Compare real instantiated grids and equal fixed card counts offline;
Robolectric event/counter checks are not TV timing or frame-rate benchmarks.

User subsequently requested the top controls' final style in the same delivery: circular
theme-colored house badge; text-only function buttons with a theme-colored focus underline,
without boxed background/border. Only HomeBrowseFunctionAdapter and the existing house vector
are additionally scoped and externally snapshotted. Latest clarification: normal house icon
remains unchanged (the vector is therefore NOT edited); only its focused state is a circular
badge. Top text focus also scales to 1.08. Final clarification: ordinary text is the muted
OnSurfaceVariant color and focused text is bright OnSurface (white in dark themes, its readable
dark counterpart in light themes), rather than the accent color. The underline uses Focus.
Scale is a 120ms transform, not a layout-size change.
The house remains a source action;
visibility/order/dispatch and horizontal focus navigation are unchanged. Drawable painting
must stay inside each control's own bounds and runtime theme refresh must replace colors.

## Recovery anchor

All eight modules and final top-focus clarification implemented. Final scoped regression:
96 tests, zero failures/errors/skips. Full Leanback ARM64 Debug assemble passed (39s).
APK signature/Manifest/ABI checks passed; artifact below. Fingerprints and final guard passed.
No commit, push, Suite pointer, Mobile, WebHome or WOGG edit. No online device was available.
Next action: await user TV acceptance; do not start further migration.

## Final implementation and acceptance evidence

| Module | Result and boundary |
| --- | --- |
| Hero bounds | Normal top moved from 22.5% to 16% of usable height, below top controls. Title slot budgets two actual font lines; metadata budgets its font height; overview uses available space up to three lines. Safe-padding 1080P/4K tests pass; no font-size reduction. |
| Artwork | GONE/1px logo target removed; unmeasured targets wait for a cancellable layout callback. Existing embedded TMDB identity gets missing art through the same cached detail service, preserving source metadata on failure. Backdrop/Logo results remain generation/scope protected. |
| No backdrop | Source portrait is a separate right-upper 2:3 ImageView, not a full-screen crop. Existing themed left/bottom gradients remain; reliable landscape replaces it after successful loading. No film's old image is retained as the next film. |
| Drawer | Retains previous translucent overlay and mild dim fix. Narrow right-side window bounds set before show; framework's redundant focus highlight disabled on owned controls/containers. House normal icon unchanged; focused badge circular and theme-colored. TV long-frame disappearance NOT TESTED. |
| Loading | One native ValueAnimator per visible three-dot view; hidden/detached animators stop. Cold loads center dots and hide empty category/rail shell; category/filter misses show rail-area dots; cache hits do not wait; append keeps current cards and does not show a full-page loader. No new request scheduler. |
| Category/rail | Existing 100ms switch debounce retained. Poster UP selects active category identity, not a stale View. Explicit category DOWN during a missing result stores only that context's focus intent; another key/category invalidates it. A-B-A uses existing contexts and selected positions. |
| Label | Immersive adapter renders 更新推荐; underlying home type ID/name/cache/query and classic display remain unchanged. |
| Inline filters | Category row INVISIBLE while filters open, preserving rail bounds and excluding that row from focus. Poster UP returns last filter row; last filter DOWN enters posters. Closing restores current category without rebuilding the adapter. |
| Top functions | Existing HomeButton visibility/order and native click/long-click callbacks retained. Text has no box/fill; normal muted text, focused bright text (dark theme white), 1.08/120ms transform and Focus-token underline. Light theme retains readable counterpart. House action is still source selection only. |

### Performance comparison and limits

| Item | Classic recent-history rail | Immersive rail / repair |
| --- | --- | --- |
| Native path | ListRow -> CustomRowPresenter -> ItemBridgeAdapter -> HistoryPresenter | Single HorizontalGridView -> HomePreviewPosterAdapter; no ListRow replacement |
| Focus strategy | FOCUS_SCROLL_ALIGNED (0) | Same |
| Window / keyline / item | BOTH_EDGE (3), 50%, 50% | Same |
| Smooth speed | 1.0 | Same |
| Pending moves | Library default 10 | Existing accepted bound 2, deliberately retained without device evidence |
| Focus animation | Leanback FocusAnimator, SMALL zoom, 150ms in the resolved 1.2.0 artifact | Existing 1.04/120ms property transform, no layout-size change |
| Focus key handling | Native grid; history row does not install CustomRowPresenter edge interception | Region coordinator handles vertical transitions and horizontal boundaries; interior horizontal keys go to native grid |
| Focus content work | Marquee selection only; ImgUtil at holder bind | Immediate source text, required visual cancellation; image preparation moved into existing Hero dwell. TMDB network/matching remains executor work. |
| Position lookup | Native holder selection | Removed two cards.indexOf scans from rail focus callback; use binding-adapter position |
| Image sizing | Existing ImgUtil/ViewTarget | Before 06: source portrait requested as a full-screen background (e.g. 1920x1080 target). Now fallback shares the card override size and centerCrop transformation through the same Glide engine/cache. This is a request-size fact, not measured decoded heap or speed. |
| Data updates | History refresh binding separate from focus | Focus alone does not submit a new adapter/list; actual category/page changes still use existing state/DiffUtil. No extra focus timer or second scroll engine. |

Measured offline checks (Robolectric, **not TV latency measurements**):

- Both real grids with 24 local fixed items and the same DPAD sequence select
  `[1,2,1,2,1,0]`.
- 40 in-viewport focus transitions: zero extra holder binds, zero synchronous whole-view
  measures, zero extra restore tasks; scale does not move neighboring layout bounds.
- Actual 2000-item adapter: last focus callback reports position 1999 correctly. The standalone
  preview fixture itself caps at 40, so the test explicitly constructs 2000 items.
- Three quick source-focus updates start zero image loads before the existing dwell; settling
  starts one request for the final source. Fallback request override equals the rail size.
- Same-count **cached-image device timing**, input-to-focus delay, focus-to-scroll delay,
  completion time, dropped frames and live site/TMDB comparison: **NOT TESTED**.

Confirmed avoidable work: per-focus large source-background requests and linear position scans.
The user's TV latency's dominant cause and improvement percentage are **UNVERIFIED** without
device traces. The grid parameter comparison does not support blindly speeding up scrolling.
Bounded optional logs: `log.tag.NativeHomeFocus=DEBUG` reports key queue/dispatch/focus/draw and
scroll intervals; `log.tag.NativeHomeHero=DEBUG` reports request stages/stale rejection/missing
art/load failures/display. Logs omit URLs/credentials and have finite budgets. Classic timing
requires platform traces/gfxinfo; no classic production instrumentation was added.

### Actual changed files

All paths below are relative to the App repository; originals are preserved externally.

| Existing file | Reason |
| --- | --- |
| app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeBrowseController.java | Project existing request state into cold/rail/append loading; prevent transient false-ready home fallback. |
| app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeBrowseImages.java | Measured/cancelled image targets, portrait fallback, shared decode size, deferred artwork and bounded diagnostics. |
| app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeHero.java | Existing-detail art completion, source fallback on failure, poster result and reuse of existing dwell. |
| app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeHost.java | Pass poster art and existing dwell completion to the same view. |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomeBrowseFunctionAdapter.java | Final top focus style and recycle/invalid-position handling; native dispatch unchanged. |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewFocusCoordinator.java | Active-category restoration and hidden-category filter routes. |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewGeometry.java | Hero upward budget only; rail geometry retained. |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewMenuDialog.java | Set window bounds before first draw; preserve original overlay/dim/focus return. |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewPosterAdapter.java | Constant-time binding-position callback and bind counter. |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewStyles.java | Circular focused house and themed unboxed underline/scale text style. |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewTiming.java | Bounded focus-to-scroll/completion interval diagnostics. |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java | Wire the eight repairs without replacing its host/adapter; reject stale category callbacks while removing old items. |
| app/src/leanback/res/layout/view_native_home_preview.xml | Separate portrait slot; logo remains measured while invisible. |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeDrawerTest.java | Cold loading, real category DOWN/UP, drawer and filter preservation. |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeHeroTest.java | Embedded art completion and offline fallback. |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomePreviewViewTest.java | Updated three-line available overview contract, existing geometry/focus checks retained. |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeVisualContractTest.java | Updated Hero top and hidden-category filter contract, retaining adapter/position checks. |

New files:

- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomeBrowseLoadingView.java`
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeImagesTest.java`
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeLoadingTest.java`
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeRailPerformanceTest.java`
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeVisualRepairTest.java`
- `docs/NATIVE-HOME-06.md`

### Build, APK and protection

Final command: `:app:testLeanbackArm64_v8aDebugUnitTest` (home/homepreview, deferred-fragment,
site-switch, runtime-theme, menu and ViewModel-clear selected tests) plus
`:app:assembleLeanbackArm64_v8aDebug`; all 96 tests pass. Final clarification build 39s.
Release merged manifest processed only; no Release APK produced. Native build dependency
emits a `preMobile...` task name internally; no Mobile source or APK was changed/generated.

- APK: `E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`
- Size: **209259786 bytes**.
- SHA-256: `ad400452f80830bfeb3a1794758d7f03b7d727e01ce0392ba475bdaa942ea6d5`.
- Actual APK package: `com.silent.android.webhtv`, version 560 / 5.6.0; debuggable true.
- ABI: only `arm64-v8a` native libraries.
- `apksigner verify`: PASS, v2 signature. Certificate SHA-256:
  `97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`.
- Enabled Launcher alias: `com.fongmi.android.tv.ui.activity.HomeActivityCurrent` -> existing
  `HomeActivity`; historical alias remains disabled. No added Launcher icon.
- Standalone NativeHomePreviewActivity is Debug-only, absent from Release merged manifest;
  it still represents fixtures and is not the real-business acceptance entry.
- Setting still gates immersive mode on DEBUG and Leanback flavor; HomeActivity, AppearanceDialog,
  Setting and WebHome routing files are byte-identical to the start of this task.

Normal launch after installing on the intended TV:

```powershell
adb -s <TV_SERIAL> install -r "E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk"
adb -s <TV_SERIAL> shell am start -n com.silent.android.webhtv/com.fongmi.android.tv.ui.activity.HomeActivityCurrent
```

Select the existing 设置 → 外观与语言 → 主页样式 → 沉浸模式 if needed. No default
Launcher or WebHome-priority change. These commands are derived from the inspected APK;
installed launch is **NOT TESTED**: all three discovered ADB endpoints were offline phones.

Protection: App HEAD before/after remains `030485b70c263621fe4575abe5758b28115832c3`;
dirty paths 258 -> 264 (six new task files). Of 5212 original App files, only the 17 authorized
files listed above changed; 5195 other originals match fingerprints, none missing. All 114
WebHome and 106 WOGG originals, their HEAD/status and Suite gitlinks remain identical.
Final guard confirms branch/HEAD, protected dirty paths, declared scope and staging are safe.
External evidence: `before.json`, `protection-after.json`, `final-guard-check.log`,
`final-test-results/`, `final-test-summary.json`, `accepted-style-build.log`, `artifact.json`,
`apk-manifest.xml`, `apk-signature.txt` under the external snapshot directory noted above.

### TV acceptance still required

**NOT TESTED**: repeated house open/close long-frame disappearance; live theme contrast and
new top glyph/underline/zoom; 1080P/4K TV safe-area and long Hero text; real backdrop/Logo and
portrait fallback; cold/source/category/filter loader lifecycle; fast category changes,
pending DOWN and A-B-A restoration; filter-hide geometry; first/last card and long-press;
classic cached-history versus immersive cached/real/TMDB latency, dropped frames and memory;
real source/detail/playback returns and WebHome/mode-switch device regression.

Stop here for TV feedback. No further business migration, commit, push or Suite update.
