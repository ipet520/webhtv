# NATIVE-HOME-02 — Leanback Debug native home selection

## Scope and recovery anchor

User authorization: add the exact title `主页样式` and choices `经典模式` / `沉浸模式` to the existing Leanback appearance dialog, persist through Setting/Prefers, route only the native presentation after existing WebHome selection, reuse the Phase 1 fixture surface, and deliver a full ARM64 Debug APK. No business migration, Mobile/HTML/player/history changes, Release APK, branch/worktree, commit, push, or Suite pointer update.

Base HEAD: `030485b70c263621fe4575abe5758b28115832c3`. Initial status: 231 dirty paths, 5,190 App files. External full-file SHA-256 snapshot and original copies of adopted Phase 1 files: `C:\Users\ADMINI~1\AppData\Local\Temp\native-home-02-20261026-192010`. Two explicitly adopted dirty files are `NativeHomeBrowseView.java` (optional settings callback only) and `HomePreviewIsolationTest.java` (update the former no-home-connection assertion for the now-authorized host connection). All other 229 initial dirty paths are protected by an isolated copy of task_guard, changing only its state directory to `.codex/task-state/native-home-02`. Original C55 and Phase 1 guard states remain untouched.

Completion: Debug mode selection and same-Activity native presentation switching, with persistent classic default, WebHome priority, an existing settings entry from immersive, automated scope/host/settings tests and full Debug APK verification. Rollback: reverse this task's minimal diffs using the external originals and remove only its new files; never reset the dirty repository.

## Decision evidence — 2026-10-26

- Existing local `SettingActivity.onAppearance` opens `AppearanceDialog`; `AppearanceDialog.addRow` owns row size, type, spacing and theme; `ChoiceDialog.showSingle` owns checked choice/focus/return semantics. Grade: direct source at the base HEAD/current worktree. Reuse both without a new settings UI. Hide the empty value view for this title-only row; add no summary text.
- Existing HomeActivity `getVideo(boolean)` resolves WebHome before native loading, `onWebError` uses `getVideo(true)`, and `openVod` explicitly forces native. Grade: direct local source. Keep that sequence; switch only the native display after the existing WebHome branch. Remember the last forced-native request when applying changed settings on resume. An active WebHome session only records the new preference, without being replaced or reloaded.
- Android Activity lifecycle: https://developer.android.com/guide/components/activities/activity-lifecycle (accessed 2026-10-26, primary platform documentation). Reconcile the selected mode on HomeActivity resume after settings; save fixture view state with instance state. No new Activity navigation router.
- Android build variant/source sets: https://developer.android.com/build/build-variants#sourcesets (accessed 2026-10-26, primary platform documentation). Gate availability and preference writes by Debug + leanback flavor; Release resolves classic even if the preference remains from Debug. Keep existing complete Debug preview manifests; do not add any launcher.
- Phase 1 view/state/focus code and 16 local tests: direct local implementation. Reuse all grids, focus coordination, backgrounds and modal fixtures. Optional settings callback replaces one generic fixture function only for the integrated host.
- Upstream merge source/PR/revert research and performance papers/benchmarks are inapplicable: no upstream/dependency/ABI/render engine change is proposed. Device acceptance is not inferred from local tests.

Alternatives: no change cannot supply mode selection; replacing the launcher or repeatedly starting a preview Activity would introduce avoidable task/back-stack loops. Chosen: retain HomeActivity as authority and insert a small native presentation host; classic content is hidden/non-focusable only while the fixture view is active. Preserve classic business code and WebHome priority. Existing settings Activity remains the only settings navigation target.

## Implemented behavior

Actual settings path: existing `SettingActivity.onAppearance()` → existing `AppearanceDialog` → a title-only row from the existing `addRow()` → existing `ChoiceDialog.showSingle()`. Text is exactly `主页样式`, `经典模式`, `沉浸模式`. The empty row value view is GONE; no helper text, description or new dialog styling is introduced. Choice confirmation immediately writes `Setting.putLeanbackHomeMode`; cancel leaves the value unchanged.

Storage: existing Prefers/SharedPreferences key `leanback_native_home_mode`, values `CLASSIC` and `IMMERSIVE`, default/invalid value `CLASSIC`. The availability and resolver require `BuildConfig.DEBUG` and flavor `leanback`; Release/Mobile resolve CLASSIC even if an IMMERSIVE value was retained from a prior Debug installation. No second preferences/database system.

Routing: original Launcher aliases still target HomeActivity. Its existing WebHome decision runs before native mode selection. A native immersive result creates one `NativeHomeModeHost` within the same HomeActivity, reusing the Phase 1 `NativeHomeBrowseView`. The old native content becomes GONE with descendants blocked from focus, and the classic toolbar is hidden. Returning to classic removes the immersive view and restores the old native content. No new Activity route, launcher, manifest or applicationId change.

HomeActivity onResume applies a changed preference after settings. An active WebHome session only records the new desired native preference and is neither replaced nor reloaded. The last explicit force-native flag is preserved, including `onWebError → getVideo(true)` and `openVod → getVideo(true)`. No-config classic fallback restores the existing no-config screen. Startup/deferred transactions keep their existing guard. Old async native results do not bind while the immersive fixture is active. Back/home in the integrated immersive surface uses the existing home exit flow, avoiding a bounce back into another home Activity.

Only one existing top function slot in the integrated surface is connected to `SettingActivity.start(this)`, labeled by the existing settings resource. Remaining functions, sources/categories/secondary choices, Hero and posters remain local Phase 1 simulation. The standalone explicit Phase 1 Debug Activity remains available, with its existing enable flag and manifests unchanged. It is not the normal-launch router.

## File inventory — 5 modified, 6 added

Paths relative to the App repository:

| Status | File | Reason |
| --- | --- | --- |
| Modified, previously clean | `app/src/main/java/com/fongmi/android/tv/setting/Setting.java` | Leanback Debug availability, CLASSIC/IMMERSIVE enum and existing Prefers access |
| Modified, previously clean | `app/src/leanback/java/com/fongmi/android/tv/ui/dialog/AppearanceDialog.java` | Title-only row and existing two-choice dialog |
| Modified, previously clean | `app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java` | Native presentation hook after WebHome, resume/state/cleanup and focus isolation |
| Modified, adopted Phase 1 file | `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java` | Optional callback replacing the first fixture top entry with existing settings navigation; no grid/focus rewrite |
| Modified, adopted Phase 1 test | `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewIsolationTest.java` | Allow the now-authorized host connection while retaining standalone Activity/network/scroll boundaries |
| Added | `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeHost.java` | One same-Activity presentation host, restoration and cleanup |
| Added | `app/src/leanback/res/values/native_home_mode.xml` | Exactly the three required strings |
| Added | `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeTest.java` | Preferences/build-flavor gate and executable repeated host switching |
| Added | `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeAppearanceTest.java` | Actual AppearanceDialog/ChoiceDialog row, choices, selected state, confirm/cancel and classic restoration |
| Added | `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeRoutingContractTest.java` | WebHome-before-native ordering, resume/native fallback, no new home Activity and Mobile source boundary |
| Added | `docs/NATIVE-HOME-02.md` | Unique implementation, recovery and delivery record |

## Verification — 2026-10-26, approximately 19:31 Asia/Shanghai

- **PASS** Leanback ARM64 Debug tests: **28 tests, 0 failures, 0 skipped**. New settings/host/route tests: 8; Phase 1: 16; existing HomeActivity deferred transaction/site-switch tests: 4. Actual settings View tests use existing AppearanceDialog/ChoiceDialog; actual host tests toggle the same Activity three times and check only one focusable home surface. WebHome ordering/fallback checks are source contracts, not end-to-end WebView sessions.
- **PASS** complete `:app:assembleLeanbackArm64_v8aDebug`, **BUILD SUCCESSFUL**, 30 seconds. No dependency/build script changes. Existing Gradle/Java warnings remain. External logs: `tests-1.log`, `build.log` in the protection directory above.
- **PASS** `:app:processLeanbackArm64_v8aReleaseManifest` only. No Release APK task run. Final merged Release manifest has no preview Activity. UI/runtime isolation is also enforced by the settings resolver; preview classes/resources live in the existing Leanback source set, so physical removal from Release DEX/resources is not asserted.
- **PASS** final Debug APK: applicationId `com.silent.android.webhtv`, debuggable=true, existing HomeActivityCurrent launcher enabled and targeting original HomeActivity; explicit NativeHomePreviewActivity exported with no launcher intent-filter; only arm64-v8a, 30 native `.so` files; all three setting resource names and exact strings are present in compiled resources; APK v2 signature verifies.
- **PASS** isolated task guard: unchanged branch/HEAD, 229 untouched protected initial dirty paths, declared/adopted scope and staged paths. The two adopted original Phase 1 files were backed up before patching; minimal changes are the optional callback and corresponding integration assertion. Neither original C55 nor Phase 1 guard state was overwritten.
- **PASS** full SHA-256 comparison of all 5,190 original App files: exactly the 5 declared files above changed; all other originals unchanged. WebHome 114 original files and WOGG 106 original files, their HEADs and statuses remain unchanged. Suite gitlinks unchanged. External machine-readable evidence: `verification.json` and `before.json` in the protection directory above.
- App HEAD before/after: `030485b70c263621fe4575abe5758b28115832c3`. No commit, tag, push, branch, worktree, destructive cleanup or Suite pointer change.

## Debug APK delivery

```text
E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk
Size: 209259203 bytes
SHA-256: 434A8FEACF077F87307494C2B0396C817B559F6EA9A620E609AA0DE72F87F32A
```

Normal validation route after installing this Debug APK: existing launcher → 设置 → 外观与语言 → 主页样式 → 沉浸模式 → return to HomeActivity. Immersive top `设置` → same appearance dialog → 经典模式 → return. If WebHome is configured/active, it retains priority; select the existing native VOD entry to exercise the native style.

Optional direct Phase 1 fixture entry remains:

```text
adb shell am start -W -n com.silent.android.webhtv/com.fongmi.android.tv.ui.homepreview.NativeHomePreviewActivity --ez native_home_preview true --ei preview_cards 18 --ei preview_functions 7
```

This command bypasses normal mode routing for isolated fixture inspection and is not proof of persisted style selection.

## Device gates and stop point

ADB inventory: three existing devices, all offline. No APK installed or launched on a device. **NOT TESTED**: TV cold start/restart persistence; actual classic↔immersive↔settings back-stack behavior; remote focus/theme; live WebHome priority, explicit native entry and error fallback on TV; 1080P/4K safe areas; continuous/long remote input; modal physical Back; GPU animation and memory. Mobile/Release full builds and Release APK are **NOT RUN**; Mobile sources remain byte-identical, with resolver policy tested against Mobile/Release inputs.

Recovery anchor: NATIVE-HOME-02 implementation/tests/Debug APK delivery complete and uncommitted. Next action: await real-TV acceptance. Do not migrate source/category/recommendation/TMDB business or begin Phase 2.
