# Nostr TV playback lifecycle audit and repair

## Recovery anchor

- Objective: repair the confirmed WebHome ↔ App playback lifecycle defects across Mobile and Leanback; preserve explicit Native History and do not restore D-SESSION.
- Acceptance: fresh preparation on every new detail entry; only a real same-detail player return can show executable exact resume; preserve a ready candidate on return; fix native empty terminal timing and `(siteKey, vodId)` fallback identity; verify History exact routing and builds; local App commit/tag, no push.
- Branch/base: `main` / `165519e02dd35cb66b7855f51986bae289d1119a`.
- Task guard: `nostr-playback-lifecycle-20261015`, active. Protected initial dirty path: `scripts/build-arm64.ps1` (untracked user file).
- User-owned Suite root attachments and changed App/WebHome gitlinks are outside App scope and must remain untouched.
- Shared contract: `../../docs/bridge-contracts/nostr-tv-playback-lifecycle.md`.

## Confirmed findings before edits

1. `openDetail()` in the ordinary path calls `prepareDetailPlaybackForDetail()`; snapshot/player-return restoration intentionally skips that fresh-entry call. This distinction must remain.
2. `updateDetailContinueButton()` currently uses any matching authoritative Native History to change the label to `续播`. Its click binding still reaches `handleDetailPrimaryAction()` → `requestDetailPlayback()`, which dispatches the prepared Curated/Direct resource without History. The label therefore does not represent an executable continuation.
3. `rearmDetailPlaybackAfterPlayerReturn()` refreshes Native History, then cancels a handed-off preparation run. The Pan return special path restores/clears its UI snapshot but skips that rearm. Neither path binds an exact current-return History record to the detail action.
4. `HomeWebBridge.playVod()` has an implicit `findResumeHistory(siteKey, vodId)` branch for ordinary requests. The explicit `intent=history_exact` branch is already reference-validated and must remain the exact-history authority.
5. Both `VideoActivity` flavors schedule `showEmpty()` after 10 seconds from `setEmpty()`, while `SiteViewModel` can still have per-site searches in flight. Mobile has no SearchProgress observer; Leanback ignores progress during `isInitAuto()`.
6. Both fallback filters compare/break on `vodId` alone, rejecting valid replacements from another source with the same ID. The WebHome `pan.play` payload contains no TMDB ID.
7. Runtime App source contains no D-SESSION coordinator/bridge. Confirmed unused detail residues are subject to removal only when exact references prove they have no live consumer.

## Decision, tradeoffs and rollback

- No change is rejected: it keeps the false `续播` label, discards the prepared run on return, prematurely ends unresolved search, and rejects cross-site fallback with a reused ID.
- There is no upstream implementation candidate for this task. Copying or inventing a session gate would conflict with the user's explicit D-SESSION rollback boundary; no external code proposal is applicable.
- Narrow WebHTV adaptation: use the existing exact `history_exact`/`HistoryRecordRef` contract only after an actual same-detail player return; treat a no-intent WebHome launch as a fresh native entry for its first resolved VOD; use `SearchProgress.finished` for auto-search terminal UI; identify failed candidates by the existing native `(siteKey, vodId)` pair.
- Compatibility: no public bridge schema or database/ABI change. Existing native History entry points and exact resume payloads remain untouched. Old clients without exact-play capability do not get a WebHome resume action. Playback and fallback costs remain unchanged apart from waiting for already-running search completion.
- Evidence used: current WebHome/App call paths, App `WebHomeHistoryService`, `HistoryRecordRef`, `HistoryResumePayload`, prior local rollback commits, and their tests. External/upstream comparisons are inapplicable because this fixes an existing local contract and imports no dependency, API or algorithm.
- Acceptance: root Bridge Contract acceptance list. Rollback: revert only this task's child commits; do not rewrite prior commits or move Suite pointers.

## Build and verification source

- WebHome: `node tests/<target>.mjs`, then `node scripts/build.mjs` and `node scripts/verify-build.mjs`.
- App: preserve and run the pre-existing `scripts/build-arm64.ps1`; it invokes `:app:assembleMobileArm64_v8aRelease` and `:app:assembleLeanbackArm64_v8aRelease` with the repository's configured Java/Python/SDK/cache paths. It is not task-owned and must never be staged.
- Device verification is unavailable in this local pass and will remain `NEEDS_DEVICE_TEST` unless actually performed.

## Progress

- App guard started before edits with base `165519e02dd35cb66b7855f51986bae289d1119a`. The initial untracked `scripts/build-arm64.ps1` remains protected and unstaged.
- `HomeWebBridge.playVod()` ordinary playback no longer resolves implicit Native History. Explicit `history_exact` still validates its History record and uses the native exact-history entry. Ordinary WebHome launches call `VideoActivity.startFromWebHome()` with a one-shot fresh-handoff marker; both variants skip implicit resume-history matching for that launch while retaining native progress recording.
- Mobile and Leanback fallback candidate/broken-source checks now key on `(siteKey, vodId)`, so a same-ID candidate from a different valid source remains eligible.
- The fixed 10-second empty terminal was removed. Both variants wait for site-search completion before showing the empty state; Mobile now observes the same `SearchProgress` completion used by Leanback.
- The final Leanback source keeps `checkSearch(false);` as a separate assignment before deciding whether to show the empty state; the search completion semantics are unchanged.
- No runtime D-SESSION coordinator, `detailSessionFor()`, or `sdk.detailSession` was reintroduced.

### Verification

- `STATIC_PASS`: ordinary WebHome bridge routing, explicit History routing, `(siteKey, vodId)` identity and wait-for-search empty-state contracts are covered by source/contract checks.
- `UNIT_PASS`: focused Mobile `WebHomePlaybackLifecycleContractTest`, `WebHomeRemoteBridgeWiringTest`, and `PlaybackSourceIdentityTest`; focused Leanback `VideoActivityDetailShellSourceTest.emptyDetailFromSearchKeepsPlaybackPageForRetry`, `WebHomePlaybackLifecycleContractTest`, and `PlaybackSourceIdentityTest` all pass on the final source.
- Full App unit suites ran before the final semantics-preserving empty-state source formatting adjustment: Mobile 4,903 tests, 5 failures; Leanback 4,105 tests, 6 failures. Both runs share five unrelated existing MPV font/HLS cache, Live fallback, and theme palette failures. Leanback's additional old source-shape assertion now passes in the focused final run. No failing unrelated assertion was weakened or removed.
- `BUILD_PASS`: serial `:app:assembleMobileArm64_v8aRelease :app:assembleLeanbackArm64_v8aRelease --no-parallel --max-workers=1` succeeded from the final App sources.
- Mobile APK: `app/build/outputs/apk/mobileArm64_v8a/release/mobile-arm64_v8a.apk`, 171,509,618 bytes, SHA-256 `B1DA20B7BA498A4ACD65A6EFF816B0A6FB0C8AE1A61340132F02C10689C0AB8E`.
- Leanback APK: `app/build/outputs/apk/leanbackArm64_v8a/release/leanback-arm64_v8a.apk`, 171,160,967 bytes, SHA-256 `01DB842CBF0F549741542844CBAC42FBDC679B785CEA1A3B25B60B58CDE70787`.
- `NEEDS_DEVICE_TEST`: real Mobile/Leanback playback, same-detail return timing, and the expired-share automatic fallback still require device confirmation. No device result is claimed.
- The local App commit and recovery tag are created by task-guard finish after scope verification. No remote push or Suite gitlink update is made in this pass.
