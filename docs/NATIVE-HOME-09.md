# NATIVE-HOME-09 — visible-window art preparation and bounded home fixes

## Scope / baseline

Four changes only: visible-window preparation, poster Back-to-first, full-screen visual insets,
single-line Hero marquee. Preserve 08 transition/theme/source/settings/player paths. No commit,
push, branches/worktrees, Suite pointer or HTML/Mobile/player edits. App HEAD before:
030485b70c263621fe4575abe5758b28115832c3, 275 dirty paths, 5226 original files. Fingerprints,
original scoped contents/diffs and logs outside repo at %TEMP%/native-home-09-current.txt.
Guard protects 268 dirty paths; allowed scope is immersive Hero/image/view/host adapters,
two small presentation classes, one XML, focused tests and this report. CustomWallView and
the accepted 160ms fade are protected, not edited.

Start 2026-10-27 11:03 Asia/Shanghai; target implementation 50–80min + validation 15–20min.

## Evidence / decision before implementation

08 starts one known-identity neighbor only after current Hero metadata callback; unknown
visible films never match early. prepareKnown uses focus generation, so moving focus drops
its callback even if still useful in the same visible window. Glide preload has no completion
callback and the caller cannot distinguish URL readiness from decoded-image readiness.
Existing Glide request keys/1280x720 bound/centerCrop/ALL strategy and metadata memo are sound.
The root currently puts system insets in padding, shrinking full-screen shade views. Back
does not consume the poster region before Activity exit. Hero XML allows two lines/end ellipsis.

Primary references read on 2026-10-27:
- https://bumptech.github.io/glide/int/recyclerview.html (window/ahead preparation, matching
  exact request options/dimensions; maxPreload must reflect image/device budget).
- https://developer.android.com/develop/ui/views/layout/edge-to-edge (visuals behind bars,
  apply safe system-bar/cutout insets to interactive content rather than every visual layer).
- https://developer.android.com/reference/android/widget/TextView#setMarqueeRepeatLimit(int)
  (native marquee, single-line ellipsize, indefinite repeat; lifecycle/selection owned locally).
Local WOGG is reference only; no JS/DOM or background renderer copied. No dependency/native
upgrade or new matching algorithm; upstream merges/PRs/papers are inapplicable to this bounded
presentation scheduling/lifecycle change. Existing TMDB Matcher/service/caches remain authority.

No change retains poor coverage. Copying WOGG's full visual batch or adding RecyclerViewPreloader
without metadata coordination would duplicate scheduling or trigger unbounded matching. Choose
one presentation queue fed by actual visible child bounds, preserving entries as focus changes.
Budget: 8 nearest visible + 2 directional lookahead (10 queued max), one background metadata
job with >=750ms start spacing, one background image job. Existing Hero retains one foreground
slot (two total metadata jobs including focused work), latest pending focused work first.
Use existing memo/manual/credential scopes and permit bounded unknown matching through the
SAME backend. No bitmaps kept by queue; at most 24 short-lived readiness records; Glide alone
owns memory/disk caches and existing foreground/transition references. No 4K source decode.

Queue stages: queued -> metadata-ready -> image-ready, with actual image completion callback;
failure cooldown and context/source/config generation isolate stale work. Focus diagnostics
distinguish prior preparation from actual Glide DataSource/readiness timing, not assumed hits.
Preserve current image fade and focus/scroll authority. Back uses Grid selection/ViewHolderTask;
insets offset only content coordinates; title uses native TextView marquee, no custom scroll.

Acceptance: executable priority/concurrency/failure/stale/cache/readiness tests, real Glide
resource completion and focus timing, Back priority/first-card real focus, inset geometry,
single-line marquee/logo/pause tests, existing regressions, Mobile selected regressions,
full ARM64 Debug APK and signature/Manifest/ABI/protection. TV coverage remains pending device
measurement. Rollback: task-only inverse patches against external originals, never reset/restore.

## Recovery anchor

Implementation and all scoped automated/build/artifact/protection checks complete.
150 Leanback +12 Mobile tests passed. No commit/push/Suite update.
TV readiness coverage and visual acceptance pending; final artifact recorded below.

## Implementation evidence (09)

- 08 limitations confirmed in source: preparation ran only after current Hero metadata; one
  known-identity neighbor; focus-generation guard discarded useful neighbor callbacks. Window
  queue now starts from actual measured/bound visible children, including before poster focus.
- Metadata uses existing Hero -> ExistingTmdb -> existing Matcher/Service/MatchCache. Unknown
  visible identities can take this same full path; no new client/repository/cache authority.
- At most 10 candidates (focused/current, next, nearest visible, up to two directional lookahead),
  one background metadata job, starts >=750ms apart, one LOW image preparation; existing HIGH
  focused request and two-total metadata-job cap remain. In-flight work coalesces in existing Hero.
- URL-ready != image-ready: real Glide RequestListener completion posts readiness after decode;
  matching model, override <=1280x720, centerCrop and disk strategy reuse display cache identity.
- Ordinary focus/no-art cleanup no longer cancels useful window image preparation. Release does.
  Pause invalidates window subscriptions, retains bounded readiness history/cache; resume derives
  a new actual window, so changed config cannot schedule an old-window request.
- Pending candidates reprioritize rather than recreate/cancel active work. No bitmap list is held;
  readiness ledger 24 records/120s, failed entry cooldown30s; actual resources stay Glide-owned.
  Readiness diagnostics mean a resource completed earlier, not guaranteed memory residency after
  eviction; actual focused Glide DataSource and draw events remain separate evidence.
- Back: overlay/dialog first, inline filter next, focused poster rail -> selected position0 with
  Leanback ViewHolderTask and actual focus. At first poster consumes without a new selection.
  Other-region folder/Activity Back unchanged; no adapter or category reload to return first.
- Previous inset listener put system-bar padding on the whole immersive root, shrinking shade
  coverage. Root now retains full bounds, masks remain match_parent; bars/cutout inset only
  content placement/usable geometry. Native CustomWallView is untouched and full-screen.
- Hero text is one-line native MARQUEE, full desired text width decides overflow (ellipsized
  layout width alone can be misleading). Selected state is independent of DPAD focus and stops
  on pause/hidden/detach. Film text update resets selection/scroll; Logo success hides text,
  failure makes the same title visible again. Existing two-font-line slot height is retained to
  preserve accepted Hero/metadata/overview/category geometry; no new scrolling engine.


Final review: foreground completion wakes window scheduling after its queued consumer has had
an opportunity to publish current metadata. This avoids spending another background start
interval rereading the just-completed Hero memo before moving on to visible neighbors.

## 09 validation and remaining device gates

PASS: 150 Leanback tests / zero failures / zero skipped, 12 selected Mobile tests / zero failures.
Leanback selection includes all home/homepreview tests, CustomWallView content/source contracts,
08 dialogs/detail-source tests, native menu, runtime theme, semantic/color contrast, C55 local UI
preservation and source episode-season cache. Mobile covers existing wall source contracts,
Mobile theme phase1 and source episode-season cache. No Mobile source changes.

Controlled queue result (Robolectric clock, not TV measurement):

| Focus event | Image resource completed earlier | Lead time |
|---|---|---|
| A at0ms | no | — |
| A at250ms | yes | 200ms |
| B at900ms | yes | 100ms |
| C at900ms | no | — |

2/4 prepared at focus in this explicit artificial schedule. This is a mechanism result, not a
claimed 50% television coverage rate or a before/after improvement percentage. Existing08 had
no directly comparable TV timing sample, so real before/after coverage is NOT TESTED.
Actual local HTTP+Glide A-B-A-B-A: 2 network requests, 2 decodes for two distinct resources;
B's real decoded completion is asserted before its focused display, with compatible display key.
Test uses explicitly sufficient24MiB Glide cache and PNG decoder adapter for Windows Robolectric;
production Glide cache and supported image decoders remain unchanged. External TMDB/device
network quality, cache eviction under actual television memory pressure remain NOT TESTED.

Executable tests cover measured visible-child publication before entering rail, priorities both
directions, 200-card cap, rapid reprioritization, stale category/scope rejection, unknown identity
through existing full backend, coalescing, cooldown, synchronous-cache completion and start
spacing. Back covers second/tenth/first and another category, real first-item focus, unchanged
adapter/category/business context, no repeat first-page request, filter/drawer/top priority.
Insets test dispatches actual WindowInsets: full1920x1080 masks, safe content offsets and bottom.
Existing geometry tests retain1080P/4K and reduced viewport checks. Native text tests cover
long overflow, short replacement, selected independent of focus, pause/resume and visibility
as used by Logo success/failure; existing image tests cover actual Logo callbacks/stale results.

TV/phone acceptance still NOT TESTED for this APK:
- First/restored categories: capture prepared-before-focus ratio for actual visible window;
  continuous right/left, rapid multi-card jumps, unknown identities, low-memory eviction.
- Verify ready-before-focus records alongside actual Glide DataSource/ready latency and native
  draw acknowledgment. Previously completed preload alone does not guarantee retained RAM.
- Check no extra blocking/frame drops and accepted160ms transition under real network pressure.
- Back at2/10/first/other category, drawer/filter priority and original top-region exit behavior.
- Phone landscape system bars/cutout and TV1080P/4K/display scaling: full masks/background,
  focus enlargement unclipped, content safe without excessive margins.
- Long/extreme/short titles, rapid film/category changes, Logo success/failure, pause/return:
  actual marquee visual motion and lifecycle. Native selected/layout tests do not establish
  physical rendering smoothness or OEM TextView behavior.

Optional focused Debug capture (no APK install performed by this task):

```text
adb -s <SERIAL> shell setprop log.tag.NativeHomeHero DEBUG
adb -s <SERIAL> logcat -v threadtime NativeHomeHero:D *:S
```

Correlate `window-queued`, `window-metadata-start`, `window-metadata-ready`, `window-image-start`,
`window-image-ready`, `window-focus` (uptime, generation, hashed identity; no credentials/URLs).
Use focused `image-source`, `backdrop-resource-ready`, `native-backdrop-drawn` to distinguish
historical preparation, actual cache residency/decode and display. Debug trace budget1200.

## Exact file scope and reasons

Modified existing files (relative to App):
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeHero.java`: existing backend
  background-window entry, same memo/work sharing, availability and focused priority.
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeBrowseImages.java`: actual Glide
  preparation completion/cancellation, keep useful warm work through ordinary focus cleanup.
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeHost.java`: queue subscriptions,
  pause/source/category lifecycle; existing one home presentation and native wall preserved.
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java`: measured
  visible window/direction, first-poster Back and content-only insets/title lifecycle.
- `app/src/leanback/res/layout/view_native_home_preview.xml`: native one-line marquee text class;
  existing Logo and title slot layout kept.
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeHeroTest.java`: unknown
  visible identity through existing backend, one background slot and focus-independent result.
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomePreparationTest.java`:
  actual decoded completion and focus-cleanup/preload release boundary.

New:
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomePreparationQueue.java`:
  bounded presentation scheduling/readiness state; no TMDB client, database or bitmap cache.
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomeHeroTitleView.java`:
  lifecycle selection for Android TextView native marquee, no custom scroll animation.
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomePreparationQueueTest.java`:
  eight scheduler/readiness/concurrency/context tests.
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHome09InteractionTest.java`:
  seven actual-view/business binding/focus/inset/title tests.
- `docs/NATIVE-HOME-09.md`: scope, implementation, evidence, artifact and handoff.

No CustomWallView, transition duration, Activity route, classic business, Mobile, player,
detail source switch, settings/theme authority, WebHome or WOGG source modified.

## Final build / artifact / protection

PASS final combined Leanback regression + complete Debug assembly (`final-build-tests.log`,58s).
PASS selected Mobile regression (`regression-mobile.log`,26s). Release manifest only checked;
no Release APK generated. APK v2 signature verified, Android Debug certificate SHA-256:
`97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`.

APK full path:
`E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`

Size: **209259698 bytes**.

SHA-256: `04f67740412857dadcfb105b7dc9c6864517745c55a3f8fc38e59aec88c33367`.

Manifest package `com.silent.android.webhtv`, debuggable=true, only arm64-v8a libraries.
Enabled Launcher alias `HomeActivityCurrent` targets original `HomeActivity`; no new Launcher.
Existing Debug preview present; Release preview absent. Existing Debug/Leanback style gate,
classic/immersive/WebHome routing and theme/menu behavior are covered by unchanged regressions.
APK DEX contains NativeHomePreparationQueue, HomeHeroTitleView and window image-ready/focus
traces, confirming this artifact contains09 code rather than the prior same-size08 artifact.

Protection PASS: App HEAD before/after
`030485b70c263621fe4575abe5758b28115832c3`.
275 ->280 dirty paths. All5226 original App files fingerprinted before work outside repository:
7 permitted original files modified; **5219 byte-identical**, zero missing;5 scoped new files.
Task guard PASS branch/HEAD,268 protected pre-existing dirty paths, exact scope and staged paths.
WebHome114 files and WOGG106 files byte-identical, HEAD/status unchanged. Suite gitlinks unchanged.
In particular CustomWallView and its160ms transition file remain byte-identical to pre09 state.
External evidence directory (not tracked): `C:\Users\ADMINI~1\AppData\Local\Temp\native-home-09-20261027-110928`.
No commit, push, reset, clean, destructive checkout, branch/worktree or Suite update performed.

## Final recovery anchor

NATIVE-HOME-09 four-item implementation,150 Leanback +12 Mobile tests, full ARM64 Debug APK,
signature/Manifest/ABI and original-file protection checks complete. Await user device review;
real-TV prepared-at-focus coverage, motion/frame timing and OEM safe-area rendering NOT TESTED.
Do not open NATIVE-HOME-10 automatically.
