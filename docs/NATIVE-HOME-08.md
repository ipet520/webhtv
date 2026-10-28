# NATIVE-HOME-08 — bounded native fixes

## Scope / protection / decision (2026-10-27 09:45 Asia/Shanghai)

Complete background preparation and ready-image transition, short top focus marker, initial
appearance dialog geometry, and evidence-based Leanback source-search repair; one ARM64 Debug
APK. No commit/push/Release APK, branches/worktrees, Suite pointer, Mobile/HTML/player edits.
App main HEAD 030485b70c263621fe4575abe5758b28115832c3; 269 dirty paths; 5222 original App
files fingerprinted outside repo. See %TEMP%/native-home-08-current.txt for snapshots, original
target content/diffs and logs. WebHome/WOGG fingerprints and gitlinks protected. Guard started
with exact allowed paths and 258 protected pre-existing dirty paths.

## Evidence and design review

Current local sources: NativeHomeHero.focus merges only in-flight requests, loses completed
Info; ExistingTmdb uses persisted match/manual cache and TmdbService.detail disk cache.
TmdbService.requestJson already logs cache/network/stale-cache independently. Glide has its
default caches; repeated API calls are NOT proof of repeated network. focusSource currently
calls clear(), cancelling and resetting native wall for every focus. First business render
already binds the default Hero without needing poster focus, but applies 180ms dwell to it.
There is no native directional neighbor preparation. CustomWallView has one image host and
no transition. Top underline currently measures text width; WOGG tv-browse.css uses a fixed
18px centered marker. WOGG tv-browse.js prepares bounded direction-aware upcoming visuals;
do not copy its visible-rail batch into native TMDB searches.

Upstream main pinned read-only: Silent1566/webhtv
edf04f9b5048b6b5bc38c53333ac3c7d62f00265 (GitHub API, 2026-10-27).
Original upstream files saved outside repo. No fetch/merge/replacement of local content.
Upstream and local ordinary VideoActivity.onChange both enter checkSearch(true), which
starts initSearch(...,true) if no candidates. Local results reach the popup only when
!isInitAuto(); auto completion with zero candidates calls showEmpty(). Mobile has a distinct
inline quick row consuming results. TMDB detail is separate: changeSource searches a match
in detailTasks, inline search uses the flavor QuickSearchDialog. Do not conflate these paths.
The reported device's exact Activity still requires runtime evidence; instrument counts.

Official evidence read (2026-10-27, primary sources):
- https://bumptech.github.io/glide/doc/caching.html : active/memory/resource/data cache;
  model, dimensions, transform and options must agree. Select matching preload/display
  requests, no second disk cache; record DataSource separately from request count.
- https://bumptech.github.io/glide/int/recyclerview.html : ahead-of-position preparation;
  adapt to existing focus direction, not another scroll authority.
- https://bumptech.github.io/glide/doc/resourcereuse.html : retained drawables must retain
  their Glide resource ownership. Never keep bitmap references after clearing their target.
- https://developer.android.com/reference/android/graphics/drawable/TransitionDrawable :
  platform two-Drawable fade inside one ImageView; no second image host. Keep only newest
  transition, release previous Glide request only after no longer drawn.
- https://developer.android.com/reference/android/app/Dialog#show() : configure window
  before show/first layout. LightDialog.createInternal currently changes width in OnShow,
  after window addition; fix only Leanback custom dialog initialization, retain Mobile path.

Alternatives: no change retains clears, dwell and manual/auto coupling. Unmodified upstream
shares the manual switch coupling and OnShow sizing, so copying it is rejected. Narrow native
adaptation retains existing TMDB/client/disk caches, bounded short-lived metadata memo and
in-flight merge, current-first + one known-identity neighbor only; Glide retains decoded
resources in existing targets; native wall performs one short Drawable transition. Fix
Leanback manual entry independently of automatic switching; test incremental result draining
and candidate selection before any popup lifecycle change. No new business repository.
PR/issue/paper searches are inapplicable to a new algorithm/dependency: platform lifecycle,
exact upstream source and existing cache/ownership contracts decide this bounded repair.

Acceptance: executable state/data-consumption tests, A-B-A counts, stale/scope/owner guards,
actual cached image/transition pixels, first-show window attrs, incremental quick candidates,
existing regression, full Debug APK and protection comparison. Physical TV and real server
counts remain NOT TESTED without an available device/config. Rollback only task inverse
patches against external originals; never reset/restore dirty baseline.

## Six outcomes and exact boundaries

1. Cache reuse: previous code already reused TMDB match/manual storage, seven-day detail
   cache and Glide caches. It did not memoize completed Hero Info and cleared the wall on
   every focus. Now a 24-entry, two-minute presentation memo avoids repeated match/detail
   consumer work; negative results have a 15-second cooldown. No new persistent cache.
   Memo/in-flight identity includes config/credential/subscription scope, source, VOD identity,
   name/year/remarks/embedded payload, with manual binding revalidated OFF MAIN before a hit.
   A-B-A-B-A test invokes the metadata backend twice instead of five times. This is backend
   invocation reduction, NOT proof that the previous version made five TMDB HTTP requests.
   Existing service cache/network/stale-cache logs remain authoritative for real TMDB traffic.
2. First/nearby preparation: existing first render already selected a Hero without poster
   focus; it previously waited 180ms. Initial/restored non-rail Hero now prepares immediately,
   without blocking cards/categories. Normal fast rail movement retains cancellable 180ms
   dwell; visited identities skip it. After current completion, prepare at most ONE neighbor
   in the current direction, using completed metadata or known/manual/embedded identity only.
   Unknown neighbors never invoke the matcher. Current work has scheduling priority, total
   active metadata work bounded at two, latest pending focus replaces older pending focus;
   already running shared work is retained, stale UI subscriptions cannot publish. Known-only
   misses do not suppress a subsequent real focused match. Glide preload/display use identical
   URL/header model, <=1280x720 size, centerCrop and ALL disk strategy; only priority differs.
3. Ready-image transition: single existing CustomWallView ImageView, platform TransitionDrawable
   for 160ms. No new ImageViews/scroll engine/animation task queue. Superseded fades collapse
   to their newest image rather than nesting. Existing Glide CustomTargets retain current and
   retiring resources until no longer drawn, so clearing a target cannot recycle a bitmap still
   visible in the fade. New focus retains a genuinely drawn previous image while preparation
   runs. Missing/failed metadata/art restores neutral wallpaper + current portrait. A resource
   that has never been drawn cannot suppress the next film's fallback. Text/focus update first.
4. Top focus: local current WOGG uses a fixed centered 18px line. Native uses a compact 12dp
   marker capped below 45% of the rendered text width: longer titles do not grow the marker.
   Existing bright/muted text, 1.08 focus scale, theme color, house circle and configured
   function identities/order/clicks stay intact. No fill/border/layout-size changes.
5. Appearance: reproduced initial Window width=-2 (WRAP_CONTENT) versus final 998px in the
   test viewport. LightDialog originally applied final geometry in OnShow, after window add.
   Leanback now receives final attrs before show; OnShow only binds theme/touch. Mobile keeps
   its original OnShow geometry path. Upstream pinned main has the same late sizing sequence;
   this was not unique to immersive routing and is not proven to be a recent theme regression.
   Tests establish the lifecycle defect/fix; physical compressed-frame elimination is pending.
6. Ordinary Leanback detail manual switch: onChange previously called checkSearch(true),
   initSearch(...,true), enabling autoMode/initAuto. In that state setSearch suppresses the
   manual popup; startSearch/setSearch hide the inline quick row, and empty auto completion
   can show the empty resource page. Manual eligibility also incorrectly used auto-change
   restrictions. onChange now enters existing manual search state, reuses valid cached
   candidates or starts ONE existing ViewModel request, and shows incremental QuickSearchDialog.
   Repeated click with an open popup is ignored. Automatic checkSearch/nextSite, line switching,
   item selection -> getDetail, player and history remain unchanged. Existing popup consumption
   tests pass before the fix (early/late/pending/close/retry), so no speculative adapter rewrite.
   QuickSearchDialog only gains opt-in count traces. Tests execute actual VideoActivity search
   methods and real popup/adapter with a playback-surface-free binding fixture; no Media3 JNI
   or real playback is simulated as a pass.

### Ordinary / TMDB / Mobile / upstream comparison

Pinned upstream main and local HEAD share manual onChange -> auto checkSearch coupling.
Current local Leanback hides the inline quick row and only feeds popup when !initAuto; Mobile
has its own visible quick row and separately feeds its active popup. The narrow correction
is Leanback-only, not a Mobile UI transplant or upstream merge.

Native VideoActivity can also present TMDB styling; it uses the same corrected onChange.
The independent shared TmdbDetailActivity has a DIFFERENT path: changeSource -> detailTasks ->
searchChangeSource -> switchSourceDetail; its player search uses openInlineSourceSearch ->
SiteViewModel -> flavor QuickSearchDialog. Its no-match branch reveals the default loading
page. This shared path was reviewed but NOT modified: no device evidence ties the reported
TV issue to it, and ordinary-entry results do not prove it fixed. Its runtime source switching
remains NOT TESTED / unconfirmed. Need the exact failing Activity from the user's TV before
claiming the whole detail system resolved. No second search or UI mask was added to hide it.

## Measured verification

- Final Leanback: **161 tests, 0 failures/errors, 0 skipped**. Includes business/category/filter/
  pagination/focus/config/theme/menu regressions, wall ownership/motion, stale callbacks,
  metadata manual/config invalidation/expiry, first preparation, bounded known-neighbor work,
  negative-cache isolation, real Glide PNG and transition, actual manual source entry/late
  results/empty-retry/manual-vs-auto eligibility, and existing TMDB identity/credential/cache tests.
- Mobile: **12 tests, 0 failures/errors, 0 skipped** (CustomWallViewSourceTest,
  MobileThemePhase1Test, SourceEpisodeSeasonCacheTest). Mobile source fingerprints unchanged.
  These are selected regressions, NOT Mobile device playback acceptance.
- Real local HTTP + Glide: A-B-A-B-A with neighbor preload => **2 HTTP requests, 2 decodes**
  when controlled existing-Glide memory capacity is 24MiB. Production Glide capacity is NOT
  changed. Earlier small-heap test => 2 HTTP requests, 5 decodes (cache pressure eviction),
  explicitly demonstrating that a load call, a decode and a download are different events.
  Test-only PNG decoder bypasses the unavailable Android AVIF JNI probe on Windows; real
  Glide HTTP/model/options/transform/target/cache pipeline remains exercised. No TV timings
  or percentage speedup measured. Real external TMDB HTTP A-B-A counts: NOT TESTED.
- Window test before patch: expected 998, initial -2; after patch: final width before show,
  unchanged after show. Existing actual popup early/late/empty/retry tests pass.
- Full `:app:assembleLeanbackArm64_v8aDebug` PASS; Release manifest only PASS. No Release APK.
- Build environment: pip TLS failure resolved using existing cached wheels extracted to the
  external stage and PIP_NO_INDEX/PIP_FIND_LINKS. No dependency/build-script change or upgrade.
- APK v2 signature PASS; Debug certificate SHA-256
  `97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`.
- Manifest: package com.silent.android.webhtv, debuggable=true, ABI only arm64-v8a; enabled
  HomeActivityCurrent alias still targets HomeActivity, no extra Launcher. Debug preview
  exists only in Debug manifest, absent in Release. Existing Setting debug + leanback gate
  unchanged; classic/immersive/WebHome route tests passed. APK DEX contains this round's
  metadata-memo, image-source and NativeSourceSearch/manual-entry code.

APK full path:
`E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`

Size: **209259698 bytes**.

SHA-256: `d74afa98eeb3eef13b7d8d2273dce20dba3ff713c8ae8497985713fc6221660e`.

## Actual changed files and reasons

Paths relative to App root:

- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeHero.java`: scoped bounded
  completed metadata memo, immediate initial work, one known neighbor, shared background work.
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeBrowseImages.java`: compatible
  existing Glide preload/display, resource lifetime, avoid focus-driven neutral resets, diagnostics.
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeHost.java`: default/current
  and directional-neighbor preparation wiring, fallback on unavailable metadata.
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java`:
  expose current direction/neighbor and guarded visual callbacks, no new focus/scroll authority.
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewStyles.java`: short marker.
- `app/src/main/java/com/fongmi/android/tv/ui/custom/CustomWallView.java`: opt-in single-host
  Drawable fade and completion; default classic/Mobile wallpaper path unchanged.
- `app/src/main/java/com/fongmi/android/tv/ui/dialog/LightDialog.java`: Leanback pre-show sizing.
- `app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java`: manual source
  entry uses existing manual state/search/candidates, opt-in count traces; original local
  R2/R3/C55/playback modifications retained.
- `app/src/leanback/java/com/fongmi/android/tv/ui/dialog/QuickSearchDialog.java`: receive/render
  count traces only; existing pending/drain/selection behavior retained.
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeHeroTest.java`: memo,
  expiry, binding/config invalidation and preparation state tests.
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomePolishTest.java`:
  short fixed marker rendering, existing portrait/drawer tests retained.
- New `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomePreparationTest.java`:
  real network/decode/cache counts, no-reset transition and fallback tests.
- New `app/src/testLeanback/java/com/fongmi/android/tv/ui/dialog/NativeHome08DialogsTest.java`:
  first sizing and early/late/empty/closed popup consumption.
- New `app/src/testLeanback/java/com/fongmi/android/tv/ui/activity/NativeHome08SourceSearchTest.java`:
  actual manual entry/result consumption, repeated click, eligibility, retry and detail identity.
- New `docs/NATIVE-HOME-08.md`: this evidence and handoff.

## Protection / device handoff

PASS: HEAD before/after 030485b70c263621fe4575abe5758b28115832c3; original 5222 files,
11 scoped original files changed, remaining **5211 byte-identical**, zero missing, four new
scoped files; dirty 269 -> 275. Task guard PASS for branch/HEAD, protected dirty paths, scope
and staged paths. WebHome 114 files and WOGG 106 files byte-identical, HEAD/status unchanged;
Suite gitlinks unchanged. No commit, push, destructive clean/reset, branches or worktrees.

ADB read-only inventory found an IN2025 phone via two connections, not an online TV.
No APK installed or user app state changed on that phone. Physical TV checklist NOT TESTED:
first/restored category Hero preparation; actual TMDB/Glide cache/network/frame timings;
rapid direction changes; no-art/failed-art portrait; smooth fade without flash; marker/house/
function ordering; appearance first/repeated show in classic/immersive and all themes;
ordinary source selection and actual play; legacy TmdbDetailActivity switch and exact failing
Activity identification; automatic failover/manual line switching/play progress preservation.

Optional TV Debug count capture (no titles/URLs/credentials in the added traces):

```text
adb -s <TV_SERIAL> shell setprop log.tag.NativeHomeHero DEBUG
adb -s <TV_SERIAL> shell setprop log.tag.NativeSourceSearch DEBUG
adb -s <TV_SERIAL> logcat -v threadtime NativeHomeHero:D NativeSourceSearch:D *:S
```

Existing TmdbService logs distinguish requestJson cache/network/stale-cache. Added Hero logs
distinguish metadata memo and Glide DataSource/ready time from display draw. Source logs show
manual entry, requested site count, returned/accepted item counts and popup rendered count.
Capture exact top Activity with `adb -s <TV_SERIAL> shell dumpsys activity activities` on the
TV when reproducing, rather than inferring ordinary/TMDB routing from an empty screen.

## Recovery anchor

Implementation and full scoped validation complete; 161 Leanback + 12 Mobile tests passed,
ARM64 Debug APK built and verified, original-work protection PASS. TV performance/visual
acceptance and independent legacy TMDB detail switch remain unverified; no global fix claim.
Next action: await user's television test feedback, particularly exact detail Activity if failing.
