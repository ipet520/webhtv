# H-INTEGRATED-20261014 — WebHome Native history contract v1

## Completion and boundaries

Deliver one matched App/HTML history loop: native read → display/select → exact resume → exact deletion → native readback → focus restore. Preserve database schema, default playback/aggregation, episode cache, tombstones, detail Continue Gate and existing TV/Mobile layout. Protected initial path: `scripts/build-arm64.ps1` (untracked, never stage).

App base `7995846a608da02ace7ab3f3eabe249fc0b56720`, branch `sync/upstream-beta-bugfixes-20261006`; WebHome base `bfd746f6d82d3f3a1d5e6948d466351d8483fe7f`, branch `codex/mobile-aggregate-filter-layout`. Target WebHome origin/main; App remote target must be established by remote ancestry before any push. Suite pointers remain untouched.

## Decision-ready design review (2026-10-14)

- Local audited source/test authority: HistoryDao, HistoryDisplayPolicy, HistoryResumePayload, TmdbSeasonProgressStore, PlaybackProgressWriter and their existing tests at App base above. Reuse native scopes, provenance validation and transaction lock; do not create another history store.
- Official Android RoomDatabase reference, accessed 2026-10-14: https://developer.android.com/reference/androidx/room/RoomDatabase — transaction isolates validation and mutation. Grade A; applicable to strict deletion race, not proof of real-device behavior.
- Official Android WebView native bridge guidance, accessed 2026-10-14: https://developer.android.com/privacy-and-security/risks/insecure-webview-native-bridges — browser arguments need native validation, existing bridge session boundary retained. Grade A; not an instruction to replace the project's bridge transport.
- AndroidX Room implementation: https://android.googlesource.com/platform/frameworks/support/+/15ccca2bd51eab204fbee3c140a3076621e8ea61/room/room-runtime/src/androidMain/kotlin/androidx/room/RoomDatabase.android.kt — mature transaction ownership, related project source, Grade A.
- No upstream adoption/dependency change. Upstream PR/revert search and performance papers do not change this bounded identity/scoping decision; no algorithm or concurrency tuning is introduced. Native progress throttling and queued-write issues are explicitly deferred.

Alternatives: no change leaves display/management/resume/delete identities inconsistent; reuse unmodified aggregated getter cannot expose individual routes; HTML-managed history duplicates persistence authority. Selected narrow adaptation: native versioned record reference over existing SDK options, independent authorized read, source-validated exact resume, writer-owned strict delete plus authoritative readback. Default no-option calls remain unchanged.

## Contract

`fm.history({mode:'independent',version:1})` returns `{version:1,mode:'independent',records:[...]}`. No argument keeps legacy array semantics. Each record includes immutable-by-convention `historyRecordRef` with version, kind ITEM/SEASON, historyKey, cid, siteKey, vodId, typed TMDB/season where applicable, and stable displayId. Unknown season is -1; season 0 is valid. Native determines accessible CID scope from Global History. Cross-CID entries have explicit non-resumable capability until safe playback context is available.

Physical rows remain ITEM references; trustworthy extra quarterly projections are SEASON references, excluding an equivalent current physical row. Projection creation/restore uses shared source provenance, never invents per-source seasonal snapshots. Only positive progress with valid duration is exposed as played/restorable state.

`fm.vod(...,{intent:'history_exact',historyRecordRef:ref})` validates reference on Native, reads actual native state, and sends exact payload into existing VideoActivity; no supplied position is trusted. ITEM uses exact persistent key, SEASON uses HistoryResumePayload provenance validation. Invalid, changed or inaccessible references fail without default fuzzy matching. No intent continues default findResumeHistory behavior. Future detail-session intents reserved but not implemented.

`fm.historyDelete({version:1,refs:[...]})` returns per-reference DELETED/NOT_FOUND/FAILED/UNSUPPORTED and a native independent readback. Local API permissions are required; no setting is enabled automatically. ITEM removes only the exact physical row, with missing-key fallback disabled inside the native transaction. SEASON means the native shared quarterly scope, explicitly presented to users; native validates source and update revision and preserves other seasons. GROUP is an explicitly confirmed frozen list of references, never a rolling loop over aggregated winners. UI single/batch actions do not implicitly request GROUP.

Legacy SDK responses remain displayable as legacy records; new exact operations are disabled without versioned references/capability. Resource context remains available and is cleaned only when its native reference can be tied to the successful operation; unbound legacy resource metadata is not erased by TMDB group.

## Acceptance and rollback

E01–E28 isolated native/Bridge/HTML tests, legacy Native defaults, TV click/double-confirm/management/focus, Mobile and build parity. Android build or integration failures block production-ready claims. Native real-device validation remains user-owned. Rollback both child candidate commits together; default SDK call and schema remain compatible. Do not push either side without matched contract verification and verified fast-forward targets.

## Recovery anchor

Status: App provider and HTML consumer implemented in matching worktrees. New native files: HistoryRecordRef, WebHomeHistoryService; existing Bridge/SDK and TV/Mobile exact-intent entry adapted; writer exact validation remains inside native transaction. HTML independent reads preserve a separate default/legacy detail-history array so the detail Continue Gate remains unchanged. Management/press/delete/focus use record IDs; obsolete projected-read deletion loop removed. Default old SDK remains readable, exact new actions require versioned references.

Native TV/Mobile first focused test pass succeeded, then final identity/GROUP/line checks were added. Native final focused tests before the last audio/unknown-season boundary adjustments also succeeded. HTML actual-SDK-expression fixture integration passed (11 checks). Chromium history suite passed after localized failure-text correction, before no further interaction changes. No formal HTML build yet. No commit/push. Protected script unchanged.

Known safe limitations: cross-CID precise playback is disabled pending native config-source activation contract; audio-specific sources are explicitly non-resumable via this video-history contract; ITEM routes with other visible quarterly snapshots require SEASON or confirmed GROUP, never broad fallback. GROUP supports an explicitly confirmed complete frozen set of accessible displayed native history references under the writer/Room transaction lock; ordinary UI batches are not GROUP. Scoped fingerprints reject concurrent foreign updates; batch-owned prior deletions are accounted for inside the same transaction. Unbound old resource contexts remain intact.

Latest user publishing decision: both repositories retain local commits only; no push, no Suite pointer update. App origin/main is `edf04f9b5048b6b5bc38c53333ac3c7d62f00265`, divergent from the local self-maintained branch (6 remote-only / 348 local-only at the preflight); WebHome origin/main matched its initial local HEAD. Neither remote is changed by this task.

## Final verification, 2026-10-14

Final focused Native verification: Leanback and Mobile each 31 tests, zero failures/errors: WebHomeHistoryServiceTest 15, HistoryRecordRefTest 4, existing HistoryResumePayloadTest 5, existing TmdbSeasonProgressStoreTest 7. In-memory Room and fixture configurations only, no real history writes/deletes. Both ARM64 Release candidates built with the existing offline toolchain and `-PfastRelease=true` (minification disabled); final build completed in 1m33s. These APKs were built before closure commits and retain build-time dirty/parent metadata; their code is the final tested source.

HTML contract fixture: 13 checks using the actual SDK expressions extracted from HomeWebController plus isolated transport. Chromium history interaction: 26 scenarios passed, including actual DOM keyboard/keyup/click deduplication, held key, same-work separate selection/deletion, failed deletion, management and return focus. The 120-record fixture's eight DPAD steps took 777ms including test-driver waits; no Native read was added per poster focus. This is not a TCL latency measurement or an exhaustive large-database benchmark. Existing tv-immersive-browse regression passed, including TV filter/Back, Detail/Search returns and Mobile Sidebar/Search.

Formal WebHome Build / Verify and both child whitespace checks passed. Generated HTML: 1,810,735 bytes; SHA-256 `05282873e8753a6397723cefbab5ac3a3fac8bc1f6993a8da71e725b7c2c7047`. No CSS, database schema, dependency, Suite source or protected build-script change. Protected script SHA-256 remains `17D2A8F424B210F4888E89A68C08DE0464A2AF38B66A9FAB516A2E2501B4D859`.

Logs: repository-external `E:\Codex\.codex\visualizations\2026\10\06\01a11157-fe32-7b32-878f-376d6c27dd54\history-integrated\`. Final Native log: native-final-validation.log; final history browser log: html-history-validated.log; shared browser regression: html-shared-regression.log. The initial incorrectly filtered all-Leanback run was interrupted and exposed unrelated font/HLS/live test failures; no complete repository-suite pass is claimed.

### E01–E28 acceptance matrix

PASS here means the stated isolated test/static contract evidence, never real-device acceptance. PENDING identifies the remaining live integration or performance evidence.

| ID | Result | Evidence / boundary |
|---|---|---|
| E01 | PASS | In-memory Native read/resolve and HTML fixture keep A/B distinct. |
| E02 | PASS | Native fixture changing line/episode retains one physical row. |
| E03 | PASS | Native valid owned quarterly projection and HTML tuple identity, including season 0. |
| E04 | PASS | Current/global CID fixture; cross-CID exact video resume explicitly refused. |
| E05 | PASS | Independent DAO read bypasses display aggregation; original legacy getter retained. Static plus A/B fixture. |
| E06 | PASS | Delete/readback reads DAO, no local history synthesis; aggregation never supplies independent records. Static plus isolated deletion. |
| E07 | PASS | Native exact A resolve/selection and actual SDK identity transport. Full device playback pending. |
| E08 | PASS | Newer B does not change selected A's source/key/position in Native and HTML fixtures. |
| E09 | PASS | Native source-owner changed test and existing quarterly payload provenance tests. |
| E10 | PASS | Missing Native reference rejected; no ordinary fuzzy playback fallback. |
| E11 | PASS | Native and HTML no-TMDB identity fixtures. |
| E12 | PASS | Actual Chromium same-work select B leaves A unselected. |
| E13 | PASS | Chromium select/toggle/select-all/cancel and selected count. |
| E14 | PASS | In-memory strict B delete plus Native readback preserves A; Chromium same-work deletion. |
| E15 | PASS | Native SEASON delete preserves other quarter and physical route. Shared quarter scope is explicit. |
| E16 | PASS | Native explicitly confirmed complete frozen GROUP, no rolling aggregate deletion; ordinary UI does not request GROUP. |
| E17 | PASS | Added member or changed scope fingerprint rejects frozen request before mutation. |
| E18 | PASS | HTML isolated per-item failure and Chromium failure preservation; Native results returned per ref. |
| E19 | PASS | Native local-write permission denial leaves data untouched; no setting bypass. |
| E20 | PASS | Old SDK arrays still display, exact actions disabled with explanation; no false precision. |
| E21 | PASS | Chromium physical key cycle/click-only simulated single invokes once. TCL pending. |
| E22 | PASS | Chromium double cancels single; third confirms existing delete; held repeat ignored. TCL pending. |
| E23 | PASS | Chromium management individual toggle/all/delete/Back/focus. TCL pending. |
| E24 | PASS | Chromium adjacent/empty restore; existing shared return path retained. |
| E25 | PENDING | 120-record Chromium DPAD/no-reread check passed; TCL performance and Native large-history read cost not measured. |
| E26 | PENDING | HTML projection→ITEM source focus/progress fixture passed; actual player-return refresh still requires paired APK/WebView. |
| E27 | PENDING | PUSH URL/key transport and Native same-flag selection checked; actual PUSH/Direct playback provider not exercised. |
| E28 | PENDING | TV/Mobile Native builds and common history regression tests pass; actual native history pages not run. |

### Remaining boundaries and next round

ITEM follows the existing physical key/CID; line/episode changes do not invent new rows. SEASON represents existing shared quarterly progress with owner validation. ITEM with other-quarter snapshots is non-deletable via ordinary single-record UI to avoid removing unrelated projections; use a legitimate quarterly reference or separately confirmed complete GROUP contract. Cross-CID video playback and audio-specific playback are explicitly non-resumable through this v1 video contract. No new GROUP UI was added.

Default detail playback still calls Native without history_exact and receives the original legacy history view. The history intent extension is available for a later, separately approved detail-session intent, but that session lifecycle is not implemented here. Native progress throttle and stale asynchronous progress-write findings remain deferred. No real TV/APK installation, history write/delete or user-data test occurred.

Next action: atomic local child closure (App guard commit plus annotated recovery tag, WebHome normal commit). Do not push; hand off matched APK/HTML candidates for user integration acceptance.
