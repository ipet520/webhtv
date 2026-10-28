# NATIVE-HOME-01 — Phase 1

## Scope and recovery anchor

Explicitly authorized on 2026-10-26: native Leanback layout/focus preview only. No business migration, network enrichment, Mobile/HTML changes, APK release, commit, push, branch, worktree, or Suite pointer update.

Base HEAD: `030485b70c263621fe4575abe5758b28115832c3`. All 209 initial dirty paths are protected. Before editing, SHA-256 fingerprints of all existing tracked and untracked App/WebHome/WOGG files were saved outside the repository at `C:\Users\ADMINI~1\AppData\Local\Temp\native-home-phase1-20261026-182956\before.json`. The existing active C55 guard is untouched. A copy of the guard, changing only its state directory to `.codex/task-state/native-home-phase1`, protects this task independently.

Completion: an explicitly launched, local-data native preview with one poster HorizontalGridView, independent region focus navigation, and return to the existing home/task. Normal HomeActivity and WebHome routing remain unchanged.

## Design evidence and decision (2026-10-26)

- Local authority: `app` at the HEAD above; current WOGG `index.html:22`, `tv-browse.css:99-110,281-288`, `tv-browse.js:350-425`. WOGG HEAD `be71724d3a168ba69dbc2c925d1ac71ac720d355` plus its current dirty working tree. Grade: direct source. Adopt the visual proportions, 2:3 posters, 1.04 scale/120ms, clipped edge/center anchor behavior; do not port DOM/JS/CSS mechanisms.
- User screenshot: 1280×582, attached in this task. Left Hero, upper controls, categories immediately above one poster row. Grade: direct visual reference. Use original local vector fixtures, not extracted film imagery or third-party source/assets.
- Android BaseGridView API: https://developer.android.com/reference/androidx/leanback/widget/BaseGridView . Grade: primary platform documentation, accessed 2026-10-26. BOTH_EDGE + center key line + edge preference implement bounded native alignment. Horizontal keys remain under Leanback control; no custom pixel scroll engine.
- Android build variant documentation: https://developer.android.com/build/build-variants#sourcesets . Grade: primary platform documentation, accessed 2026-10-26. Multiple flavor dimensions require complete variant source sets. Declare the preview only in `leanbackArm64_v8aDebug` and `leanbackArmeabi_v7aDebug` manifests; no Mobile or Release entry.
- Existing `CustomRowPresenter` demonstrates native focus-edge handling. Existing home/category adapters and details remain outside this phase. Upstream merge commits/PRs/reverts, benchmarks and papers are inapplicable: this is an approved isolated local UI prototype, with no upstream/dependency/ABI/player change. Device behavior/performance remains unverified until device acceptance.

Alternatives: no change cannot validate the requested prototype; copying WOGG runtime would violate native authority; replacing HomeActivity's adapter would risk existing business state. Chosen: a separate Activity and leanback-only reusable view, local fixtures, native grid alignment and a region-only coordinator. This adds no dependencies or network client. Lifecycle callbacks release view animations; no caches/business stores are added.

Acceptance: Debug compile/resources; executable region/state/layout tests covering empty/short/long rails and varying top counts; default HomeActivity and all pre-existing files byte-identical; preview source has no network/business loader dependency; no Release preview manifest. Remote-repeat focus stability and TV performance require real-device verification.

Rollback: remove only the task's newly added files and isolated guard state after review; do not reset or clean the dirty repository. No existing production file is modified.

## Supplemental category/source scope

The user additionally authorized local secondary filters and a simulated source menu. Current dirty WOGG source `tv-browse.js:1202-1234,1580-1668` applies primary selection on focus; confirming opens secondary choices. Secondary direction keys only move focus; confirmation applies in place, repeated confirmation of a non-default option restores the default, and Back closes without undoing the applied selection. The native fixture implements these semantics with a native modal Window and horizontal option row. The source fixture is a narrow right-side native Dialog with vertical scrolling; confirming applies only local state, closes and restores the source entry as explicitly requested for this phase.

Selection uses green text and the independent `selected` state; focus uses a white outline. No production Site/Vod/Class identity, preferences, lock policy or request is used. Primary/secondary simulation has one state owner, `HomePreviewSelection`; no second production authority is introduced. Popup focus is confined by Android's modal Window; the region coordinator does not dispatch into it.

Version boundaries: WOGG HEAD is a committed reference, not evidence of a verified stable TV artifact. The additional local CSS/JS/HTML changes are uncommitted. Their device-stable status is **UNVERIFIED**; the local secondary-filter follow-up also records TV acceptance as pending. No WOGG checkout, commit, or assets were copied. App stable authority remains the separately verified `74af2c5137a9380cd454c78098668806b1b27e6a`, not this prototype or the current dirty HEAD.

## All added files (22); no pre-existing file edited

Paths below are relative to the App repository:

| File | Reason |
| --- | --- |
| `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomePreviewActivity.java` | Explicit Debug gate, local fixture counts, return to original task/home, instance state and cleanup |
| `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java` | Layered native layout, Hero binding, dual backdrop crossfade, native grids and menu connections |
| `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewFocusCoordinator.java` | Region transitions and horizontal hard edges; no pixel scrolling |
| `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewGeometry.java` | Bounded viewport/density-based geometry and scale clearance |
| `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewPosterAdapter.java` | Stable local card identity, 2:3 posters, bounded badges, 1.04/120ms focus without relayout |
| `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewData.java` | Offline fixture cards/source/secondary labels |
| `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewSelection.java` | Single fixture selection owner and source-change reset |
| `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewStyles.java` | Independent focus outline and selected color |
| `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewMenuDialog.java` | Native modal source/secondary menus, continuous selection and entry restoration |
| `app/src/leanback/res/layout/view_native_home_preview.xml` | Layer slots and three independent regions; only one poster grid |
| `app/src/leanback/res/values/native_home_preview.xml` | Preview control labels and fixture categories |
| `app/src/leanback/res/drawable/nhp_backdrop_day.xml` | Original offline background fixture |
| `app/src/leanback/res/drawable/nhp_backdrop_night.xml` | Original offline alternate background fixture |
| `app/src/leanback/res/drawable/nhp_poster_amber.xml` | Original offline poster fixture |
| `app/src/leanback/res/drawable/nhp_poster_blue.xml` | Original offline poster fixture |
| `app/src/leanback/res/drawable/nhp_poster_green.xml` | Original offline poster fixture |
| `app/src/leanbackArm64_v8aDebug/AndroidManifest.xml` | Complete ARM64 Leanback Debug entry, no launcher filter |
| `app/src/leanbackArmeabi_v7aDebug/AndroidManifest.xml` | Complete ARMv7 Leanback Debug entry, no launcher filter |
| `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewStateTest.java` | Four state and geometry tests |
| `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomePreviewViewTest.java` | Ten executable native View/focus/layout/menu tests |
| `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewIsolationTest.java` | Two default-route, manifest and network/scroll dependency boundary tests |
| `docs/NATIVE-HOME-01-P1.md` | Scope, decision evidence, recovery and validation record |

## Validation — completed 2026-10-26, approximately 18:49 Asia/Shanghai

- **PASS** `:app:compileLeanbackArm64_v8aDebugJavaWithJavac`, including resource processing and complete Debug manifest merge. Existing Gradle deprecation/unchecked warnings remain; no build configuration or dependency change.
- **PASS** `:app:testLeanbackArm64_v8aDebugUnitTest --tests 'com.fongmi.android.tv.ui.homepreview.*'`: **16 tests, 0 failures, 0 skipped**. State 4, Android View 10, isolation 2. Final log: external protection directory `gradle-6.log`. JDK21, Android SDK and existing Gradle cache were used. No assemble/Release task was executed.
- Test harness limitation: direct Robolectric View key dispatch omits Android ViewRoot's unhandled-arrow fallback. The harness invokes the actual View/Leanback `focusSearch` and `requestFocus` fallback, then checks real grid positions and child coordinates. It does not implement a pixel scroller. This is not an end-to-end hardware input test.
- **PASS** overflow function traversal through 24 items; poster middle keyline, first/last edge coordinates; repeated arrows at last item remain bounded; empty/one/two-item rails; TOP→CATEGORIES→POSTERS and restoration; held vertical key does not traverse multiple regions; source modal hard edges/confirmation/cancel; filter focus independent of selection, continuous confirmation/reset; stable Hero/card layout; saved preview state.
- **PASS** new XML parsing, both Debug manifest declarations, absence from main/leanback/mobile/general-debug manifests, explicit enable flag + BuildConfig.DEBUG gate. ARMv7 full compilation **NOT RUN**.
- **PASS** isolated task guard: HEAD/branch, all 209 protected dirty paths, task scope and staged paths. Original C55 guard remains active and unchanged.
- **PASS** SHA-256 comparison of **all 5,168 pre-existing App paths**, **114 WebHome paths** and **106 WOGG paths**: **0 changed original paths**. Both HTML repository statuses and HEADs unchanged. Suite HEAD and gitlink index entries unchanged. External evidence: `before.json`, `protection-result.json`, `guard-check.log` in the protection directory above.
- **PASS (static scope)** no production network/business loader import or custom pixel-scroll dependency in preview code; all fixture images are local resources. **NOT TESTED** full-App runtime packet capture; pre-existing Application startup activity is outside this claim.
- **NOT TESTED** real TV/remote long holds, GPU animation smoothness, 1080P/4K display/overscan acceptance, physical Back/predictive behavior, old home/WebHome live-session return, process death on a device and memory peaks. No APK assembled, installed or released; no commit, tag, push or Suite pointer update.

## Explicit preview entry and behavior

After a future authorized Leanback Debug install:

```text
adb shell am start -n com.silent.android.webhtv/com.fongmi.android.tv.ui.homepreview.NativeHomePreviewActivity --ez native_home_preview true --ei preview_cards 18 --ei preview_functions 7
```

Counts accept 0–40 cards and 0–24 generic top entries, for empty/short/overflow cases. The home icon and Back leave the preview. Existing task: return to its prior page; cold preview task: launch the unchanged HomeActivity only on exit. Without the enable flag no preview view is created. There is no persistent preview switch, normal-launch redirect or production test button.

Top entries, source choices, primary/secondary categories and Hero are local simulation only. Real functional dispatch, source/lock rules, category requests/pagination, TMDB enrichment, history/keep/following and playback remain unconnected and unchanged.

Recovery anchor: Phase 1 code and local verification complete, left uncommitted as requested. Next action: await user review and a separately authorized device-verification stage. Do not begin business migration or Phase 2.
