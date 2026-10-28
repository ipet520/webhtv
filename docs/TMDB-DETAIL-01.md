# TMDB-DETAIL-01 — Leanback responsive standalone TMDB detail

## Scope and protection

Completion: compact existing detail/fusion actions and center the existing fusion player at
16:9 on Leanback only; preserve mode playback dispatch, player/session/surface identity,
themes, history/source logic and Mobile. User explicitly authorizes implementation/build,
no commit/push/Suite update. Standard lane; no upstream merge or dependency changes.

Start 2026-10-27 11:58 Asia/Shanghai; implementation/tests40–60min, build/handoff10–15min,
target12:50–13:15. App main HEAD030485b70c263621fe4575abe5758b28115832c3,280 dirty paths,
5231 original files fingerprinted outside repository. Exact original target contents/diffs
saved; guard adopts only pre-existing dirty TmdbDetailActivity.279 other dirty paths protected.
Allowed: Activity, existing TmdbDetailLayoutUtils,3 small resume-label resources, one Leanback
test, one Mobile isolation test and this document. Shared XML/controllers remain untouched.

## Source audit / decision-ready design

Local `detailActions` is a vertical LinearLayout: full-width play then a weighted button row.
`fusionActions` is already a wrapping FlexboxLayout but all children grow to fill available
width. Both templates reapply group widths; therefore apply one Leanback adapter AFTER the
existing template, not competing template-specific rules. Keep existing buttons/IDs/listeners
and outer containers; only flatten the detail button hierarchy into one Flexbox child on TV.
Do not reparent player/PlayerView/Surface/Texture/subtitle/danmaku.

Existing embedded player uses MATCH_PARENT x252dp; TV horizontal margins0. Existing fullscreen
sets root-sized MATCH_PARENT with zero margins; exit calls embedded layout. Existing spacer
reserves252dp plus22/20dp fusion margins and scroll listener translates the root player to it.
Keep this authority; adjust host LayoutParams and spacer height from the same computed result.

Read-only upstream comparison,2026-10-27:
- Stored upstream/beta93ea56a61df9fbe3acf0d345234f0a97c3bcfab3 (2026-10-19) is stale.
- `git ls-remote https://github.com/Silent1566/webhtv.git refs/heads/beta` reports
  **6db0ebab9ff34bdae29936c0394c4c94e955a806**. Downloaded exact target source at this revision
  into external stage, without fetch/merge/branch/ref/pointer changes.
- Shared XML, LayoutUtils and Enhanced/Player/Fusion controllers are text-identical to latest
  upstream. Activity has959 unified-diff lines (includes local R2/R3/C55/source fixes and later
  upstream work); targeted templates and full-width252dp embedded design remain equivalent.
  External full diff retained; never replace local Activity with upstream version.

Primary evidence (accessed2026-10-27):
- https://github.com/google/flexbox-layout (project source/README,grade A): existing native
  flexWrap supports dynamic children; disabling flexGrow/flexShrink prevents all-width fill
  and unreadable shrink. Reuse installed dependency; no upgrade or imported code.
- https://developer.android.com/reference/android/view/ViewGroup.LayoutParams (platform,A):
  measure available parent area, preserve existing host and adjust layout parameters.
- https://developer.android.com/media/media3/ui/surface (platform,A): preserve current output
  surface/type and lifecycle; this task changes only enclosing layout, never surface binding.
- Local Activity fullscreen/spacer/focus and Mobile tests are concrete implementation evidence.
  Research papers/performance benchmarks, unrelated PR/revert history and dependency/native
  changes are inapplicable to this localized, user-specified layout correction.

Alternatives: no change preserves observed oversized layout; latest unmodified upstream has
same layout problem; TV XML fork duplicates77KB and risks binding/Mobile drift. Choose narrow
runtime TV adapter after common templates, existing Flexbox dependency, measured content width
and same host/spacer. No new player architecture, theme or focus engine.

Design: visible buttons wrap naturally at measured width, no flex growth/shrink, consistent
height/gaps/focus room, green play untouched. TV resume label uses localized short text while
full label remains accessibility description and history/episode state unchanged. Fusion width
64% of safe content width minus existing side gutters, capped760dp; height=width*9/16. Content
insets affect scroll/content only; background full screen. Fullscreen bypasses compact sizing.
Responsive sizing responds to measured root changes and insets, without repeated listeners or
unconditional layout loops. Mobile branch retains original XML/252dp/full-width behavior.

Acceptance: actual native View measurement at varied dp/densities/themes,5/6 buttons and wraps,
dynamic visibility/text, clicks and focus order/bounds; panel/spacer synchronization and same
surface hierarchy through repeated fullscreen-layout transitions; mode dispatch/history source
contracts; Mobile regressions; full ARM64 Debug APK/signature/Manifest/ABI/protection. Hardware
decode/audio/subtitle/no-black-frame/actual progress continuity remain device NOT TESTED.
Rollback: task-only inverse against external originals; never reset/restore existing work.

## Recovery anchor

Implementation,31 Leanback tests,full ARM64 Debug build and artifact/protection checks complete.
Mobile full regression156:155 passed,1 pre-existing failure reproduced on saved pre-task source.
Next: await television validation; no optional follow-up implementation or commit/push/Suite update.

## Implementation and verification boundaries

Only the original Activity and existing LayoutUtils are production Java changes. Shared XML,
Enhanced/Player/Fusion controllers, playback service/engine/surface code remain untouched.
Leanback post-template adaptation keeps `detailActions` outer LinearLayout, moves the SAME
button instances into one Flexbox child and configures existing `fusionActions`. IDs, visibility,
click/long-click listeners and theme resources remain on those instances. Natural measured
widths, min112dp(play)/72dp(other),48dp consistent height (font metrics can enlarge),8dp gaps,
no flex growth/shrink; existing Flexbox wraps at actual parent width. TV outer action container
uses measured parent width after either template; children never stretch to fill that container.

Fusion player width=min(760dp,64%*(safe width-32dp side gutters)), height=width*9/16; root measured
width first, configuration dp only before first layout. Center inside left/right content insets,
including RTL margin resolution. Existing22/20dp vertical margins retained. Same computed height
reserves spacer; existing scroll and player layout callbacks keep translationY synchronized.
Width/height/inset updates do not restart playback or move PlayerView between parents. Fullscreen
continues original full-root layout and zero margins; exit recomputes compact embedded bounds.
Root-size changes reapply only on actual dimension change and only embedded Fusion state.
System bars/cutout inset only scroll content; root/hero/backdrop remain full size.

Localized Resume/继续播放/繼續播放 is TV-only. Full original episode label remains contentDescription;
selectedEpisode/history/position/identity state and continuation dispatch are unchanged. Mobile
still uses the original full label and original shared XML/252dp full-width embedded branch.

Testing notes: actual detail XML and original buttons are inflated. Windows Robolectric cannot
decode bundled Media3 exo_icon_vr.png; test factory uses a real PlayerView with a controller-free
layout (no decoding/playback service). A retained SurfaceView marker verifies parent identity;
this is not a running decoder continuity measurement. Host uses explicit Robolectric resources
because the fixture app omits AutoSize's startup provider; pure geometry tests independently
cover320/640/960/1920dp at density1/2/3. Production AutoSize is untouched.
API28 synthetic platform WindowInsets drops stable-bar metadata on rewrapping. The test invokes
the actual registered Compat handler with typed insets; OEM/system dispatch is NOT TESTED.
These are explicit environment limits, not production patches or weakened existing assertions.

## Measured layout / focus results

Actual native View measurements at density2, using the unchanged detail XML and TV adaptation:

| Root width | Logical width | Action area | Green play width | Six visible actions |
|---|---|---|---|---|
|1920px|960dp|1792px /896dp|224px /112dp|one row|
|1280px|640dp|1152px /576dp|224px /112dp|one row|
|960px|480dp|832px /416dp|224px /112dp|two rows|

No zero/negative widths or horizontal overflow; visible buttons have equal heights. Five actions
fit one row. Hiding Following restores the shorter group without replacing button/row identities.
Fusion four/five actions are compact and centered on the same axis as the player.

At1920x1080 physical pixels,density2 (960x540dp), fusion player measures **1188x668px**
(594x334dp,rounding tolerance<1px),left366px; spacer height668px. Pure geometry tests cover
320/640/960/1920dp at density1/2/3,including760dp cap and asymmetric safe left/right insets.

Theme tests exercise default dark/light and cinema dark/light through existing ThemeColors,
button styling and both templates: original green play tint, original button instances and
focused Keep survive; action layout remains compact. No theme resource/color definition changed.

Focus PASS: original requestDetailPlayFocus; left/right visual order through all buttons;
left/right boundary consumption; hidden Keep skipped; theme/template update preserves focus;
native Down reaches existing flag section; native Up from fusion actions reaches the existing
player/spacer bridge; player Down returns to fusion actions. Existing horizontal dispatcher is
unchanged; no additional focus engine or synthetic key delay introduced.

Three original controllers dispatch respectively to playDefaultPlayback/playDetailFullscreen/
playInline; enhanced/direct modes retain explicit play and fusion retains original autoplay
policy. External before/after method-body comparison confirms **11 existing methods identical**:
playDefaultPlayback,playDetailFullscreen,playInline,onPlay,enterInlineFullscreen,exitInlineFullscreen,
applyInlinePlayerFullscreenLayout,enterInlinePiPLayout,exitInlinePiPLayout,
restoreInlinePlayerPanelAfterOverlay,saveInlineHistory. No source/search/line/episode/history
or player-session implementation was changed. Repeated fullscreen-layout tests run three cycles:
full-root bounds then original compact bounds, same panel/PlayerView/SurfaceView-marker parent,
spacer position/height synchronized,no overlap with actions. This verifies layout/identity,
**not physical decoder playback,progress continuity or absence of black frames**.

## Automated results and existing failure

- PASS:31 Leanback tests,zero failures (`regression-leanback.log`),including12 new responsive
  native View tests and existing chip/theme/focus/controller/C55/home-route contracts.
- Mobile full regression:156 tests,**155 passed,1 failed** (`regression-mobile.log`). New flavor
  and shared-XML isolation tests pass; existing direct-fullscreen/mode/theme tests pass.
- Existing failing test:
  `TmdbDetailActivityLayoutTest.tmdbEpisodeDataIsBoundBackToSourceEpisodesAndRefreshesByDataSeason`,
  line2920: `stale split-season TMDB caches should trigger a one-shot fresh first-season probe
  for long single-season shows`.
- Baseline confirmation: copied the exact saved pre-task Activity plus unchanged TmdbService
  into external `baseline-source/src/main/java`,ran ONLY that original assertion with explicit
  test working directory and separate reports. **Same failure on pre-task source**. Log prints
  `BASELINE_WORKING_DIR=.../baseline-source`; baseline report preserves the original failure.
  No workspace files replaced and no test expectation edited/excluded. This is a pre-existing
  failure,not a claim that the underlying cache behavior has been diagnosed/fixed.
- Full Mobile run evidence is the saved156-test log; later isolated baseline invocation has
  separate final XML report. Do not interpret the latest single-test invocation as a new
 156-test run or describe Mobile as all-PASS.
- PASS full `:app:assembleLeanbackArm64_v8aDebug` (28s),Release Manifest processing only.
  No Release APK. Quota interruption delayed final handoff; no extra implementation scope.

## APK and worktree protection

APK:
`E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`

Size: **209259650 bytes**.

SHA-256: `988a71e910b51f4034e3e3dc32f26aad3e1c3a8c066a7ae51cbf400956d7f533`.

PASS APK v2 signature,Android Debug certificate SHA-256
`97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`.
Package com.silent.android.webhtv,debuggable=true,only arm64-v8a. Original enabled Launcher
HomeActivityCurrent still targets HomeActivity. TmdbDetailActivity registration unchanged.
NativeHome preview remains Debug-only/absent in Release Manifest; no new Launcher or package.
DEX contains compactDetailActions/applyTvFusionActionsWidth,confirming this APK contains the
layout changes. Existing mode gates are unchanged; this task adds no new experimental gate.

HEAD before/after: **030485b70c263621fe4575abe5758b28115832c3**.
Dirty paths280 ->287. Of5231 original files,only2 authorized Java files changed;**5229 byte-identical**,
zero missing;6 scoped new files. Guard PASS protects279 initially dirty paths,branch/HEAD,scope
and staged paths. WebHome114 and WOGG106 original files byte-identical,HEAD/status unchanged.
Suite gitlinks unchanged; NATIVE-HOME-01–09 files and classic/Mobile/player business preserved.
No commit,push,reset,clean,destructive checkout,new branch/worktree or Suite update.

External originals,full diffs,test/build/baseline/signature/Manifest/protection evidence:
`C:\Users\ADMINI~1\AppData\Local\Temp\tmdb-detail-01-20261027-120050`.

## Exact changed files

Modified:
- `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java`: Leanback post-template
  action adaptation,measured fusion host/spacer sizing,content-only insets,short TV resume label.
- `app/src/main/java/com/fongmi/android/tv/ui/utils/TmdbDetailLayoutUtils.java`: existing tool hosts
  compact native Flexbox setup and pure dp/density-based player geometry.

New:
- `app/src/main/res/values/tmdb_detail_tv.xml`: compact English Resume label.
- `app/src/main/res/values-zh-rCN/tmdb_detail_tv.xml`: 继续播放.
- `app/src/main/res/values-zh-rTW/tmdb_detail_tv.xml`: 繼續播放.
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/activity/TmdbDetailResponsiveLayoutTest.java`:
 12 native layout/focus/geometry/theme/dispatch/host-restoration tests.
- `app/src/testMobile/java/com/fongmi/android/tv/ui/activity/TmdbDetailResponsiveIsolationTest.java`:
 Mobile flavor cannot enter TV adapter; original shared XML geometry preserved.
- `docs/TMDB-DETAIL-01.md`: this audit/decision/implementation/evidence/handoff.

## Television acceptance — NOT TESTED

No APK was installed and no playback session was run on a device by this task.
Verify enhanced/direct modes:5/6 buttons,three actual detail themes,short Resume with correct
episode/progress,default focus and all directions,launch native playback/return/replay.
Verify fusion:compact16:9 view,actual video scaling,manual/autoplay,all actions,scroll alignment,
repeated fullscreen entry/exit,correct focus restoration,progress preserved,no black frame or
playback/audio/subtitle/danmaku interruption,real surface/decoder lifecycle unchanged.
Check TV UI scaling1080P/4K and asymmetric system bars/cutouts on phone-running-Leanback.
Mobile physical landscape/portrait/PiP/gestures remain NOT TESTED;source/layout isolation and
156-test regression outcomes above do not substitute for device acceptance.

Task stops here awaiting user review; do not expand into cache repair or further UI redesign.
