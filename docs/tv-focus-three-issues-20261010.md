# Leanback TV Focus Fixes (2026-10-10)

## Recovery anchor

- Objective: fix the ad statistics child-row focus loss, playback-detail Follow button focus loss, and native VOD card focus-outline theme binding.
- Lane/scope: `standard`, guarded by `tv-focus-three-issues-20261010`; only the declared Leanback dialog/activity, ThemeBinder, VOD-card layouts, focused regression tests, and this task record may change.
- Baseline: branch `sync/upstream-beta-bugfixes-20261006`, HEAD `dafffd5a96075fa9bb8be87803ca2f93bffc9df1`.
- Protected initial dirty path: untracked `scripts/build-arm64.ps1`; preserve and exclude it.
- Acceptance: retain current tab/expanded/selection semantics; keep Follow business and selected visuals; only recolor opted-in ordinary card focus strokes from current `ThemeTokens.colorFocus()` while preserving drawable geometry and unrelated states.
- Verification: focused Leanback regression tests, `git diff --check`, then exactly one Leanback ARM64 Release build; finish with a local task-guard commit and annotated recovery tag, no push.
- Rollback anchor: baseline HEAD above; task changes are confined to the declared task-guard scope.
- Status: implementation and automated validation complete; awaiting user real-device confirmation after handoff.

## Findings and execution record

### Confirmed causes and fixes

- **APP_01 / Ad-block statistics:** both active grouped adapters rebuilt their visible rows and called `notifyDataSetChanged()` after expansion changes. Their posted focus restoration was a one-shot lookup which could run before the target holder returned from layout; when the list relinquished focus, the tab focus listener selected Overview. Expansion sets and filters remained intact. The grouped and rule adapters now notify the changed group plus only the inserted/removed contiguous log rows, preserving the focused holder and the existing expanded-state model. No statistics or category behavior changed.
- **APP_03 / playback Follow:** `onFollowing()` disabled the currently focused button before starting the async lookup. A disabled Leanback view cannot retain DPAD focus. The existing `followingActionPending` guard already blocks repeated clicks, so the view is no longer disabled while the request runs. Button instance, selected state, text, visibility, and add/cancel business paths are unchanged.
- **APP_05 / native VOD card focus:** ordinary VOD/search/file cards draw their focus outline from `selector_vod` in the item **foreground**, with a fixed-white `shape_vod_focused`; the oval layout uses a background selector. Runtime `ThemeBinder` previously bound tagged backgrounds and Material channels but did not bind foreground selectors, so the white foreground stroke bypassed the custom Focus token. The binder now clones and tracks only opted-in foreground state-list selectors marked `webhtv:focus-stroke:<dp>`, recoloring focused-state strokes from the active `ThemeTokens.colorFocus()` while preserving source drawables and non-focused states. Existing background binding handles the oval selector. The tag is applied only to the seven Leanback file/search/VOD card layouts. `selector_video_item` playback controls were not changed.

### Changed paths

- `app/src/leanback/java/com/fongmi/android/tv/ui/dialog/AdBlockStatsDialog.java`
- `app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java`
- `app/src/main/java/com/fongmi/android/tv/theme/ThemeBinder.java`
- `app/src/leanback/res/layout/adapter_file.xml`
- `app/src/leanback/res/layout/adapter_search.xml`
- `app/src/leanback/res/layout/adapter_search_list.xml`
- `app/src/leanback/res/layout/adapter_vod.xml`
- `app/src/leanback/res/layout/adapter_vod_list.xml`
- `app/src/leanback/res/layout/adapter_vod_rect.xml`
- `app/src/leanback/res/layout/adapter_vod_oval.xml`
- `app/src/testLeanback/java/com/fongmi/android/tv/theme/ThemeBinderRuntimeTest.java`
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/activity/VideoActivityFollowingFocusTest.java`
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/dialog/AdBlockStatsDialogFocusTest.java`

### Verification

- Leanback targeted unit tests: `:app:testLeanbackArm64_v8aDebugUnitTest` filtered to `AdBlockStatsDialogFocusTest`, `VideoActivityFollowingFocusTest`, and `ThemeBinderRuntimeTest`; **7 tests, 0 failures**.
- The VOD runtime contract inflated all seven tagged layouts, rebound each selector across two distinct custom Focus palettes, checked focused-stroke token changes, and verified the source drawable and focus geometry remained unchanged. An unmarked foreground selector remains outside the new binding path.
- `git diff --check`: passed (Git emitted only its existing LF/CRLF advisory warnings).
- Leanback ARM64 Release: `:app:assembleLeanbackArm64_v8aRelease` **BUILD SUCCESSFUL**.
- APK: `app/build/outputs/apk/leanbackArm64_v8a/release/leanback-arm64_v8a.apk`; size `171144583` bytes; SHA-256 `5249b98923e867dbc0a261f5e290e5d9a062c41b2252219b228aae7fa470105d`.

### Device follow-up

Real-device validation remains pending for: (1) expanding/collapsing source and rule statistics while retaining DPAD focus on the active row; (2) adding and canceling Follow on the playback detail page while confirming visible/reusable focus and unchanged state visuals; (3) ordinary poster cards across all seven layouts following distinct custom Focus colors in Light and Dark modes, without changing playback-chip, selection, current-playback, zoom, corner, stroke-width, or focus-navigation behavior.

The protected untracked local build script `scripts/build-arm64.ps1` remains outside this task’s commit scope.
