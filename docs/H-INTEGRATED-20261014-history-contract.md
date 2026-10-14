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

Physical rows remain ITEM references; trustworthy extra quarterly projections are SEASON references, excluding an equivalent current played physical row. Projection creation/restore uses shared source provenance, never invents per-source seasonal snapshots. Accessible valid physical records remain listable without a resume point; positive progress with valid duration determines played/restorable capability, not physical record existence. Quarterly projection generation still requires valid progress.

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

## H-INTEGRATED-LOSS-20261014 regression closure evidence

### Baseline and scope

App `e6eee3f4482959a59ce076570b0e89a905298fd2`, branch `sync/upstream-beta-bugfixes-20261006`; WebHome `4cacae61f823a4c2d571b920f3b60fe00c510a62`, branch `codex/mobile-aggregate-filter-layout`. Both tracked worktrees were clean; App's original untracked `scripts/build-arm64.ps1` remains protected. Task paused and resumed without discarding or resetting work. No fetch/pull, branch move, push or Suite pointer update.

User real-device evidence: on the same new App, Native and old HTML show two different works; new HTML shows one. One Native record has no valid progress. The user further identified a failed playback after successful resource retrieval without Pan login as a way that this record was created. This task does not change login, resource retrieval or playback failure behavior.

### Confirmed root cause and first loss

`WebHomeHistoryService.read()` gated physical ITEM creation on `played(route)` (`position > 0 && duration > 0`). This incorrectly promoted resume-point validity into list admission. `HistoryDao.findAll(currentCid)` and the default `History.get()` have no corresponding progress exclusion. The first loss occurs before Bridge serialization, SDK handling, HTML normalization or rendering.

The unconditional new Native test `differentWorksRemainVisibleAcrossProgressAndDurationBoundaries` first ran against the unchanged provider. Its actual HomeWebBridge private history handler on in-memory Room recorded `dao=2, legacy=2, independent=1` for B position=0/duration>0 and failed with expected 2/actual 1. Evidence: external loss-before.log and loss-before.xml. Fixture identity summaries are synthetic; no actual user records, URLs or tokens were logged.

TCL exact runtime DAO/API/state/DOM counts were not collected: **UNVERIFIED**. The root-cause mechanism is test-confirmed and agrees with the supplied observation; no claim of device instrumentation or post-fix TCL acceptance is made.

### Targeted fix

- `read()`: create ITEM from accessible valid persistent identity even when played=false. Same current/global CID and incognito authorization boundaries remain.
- `record()`: canResume now explicitly requires played, source availability and non-audio route; no valid point gets `尚未建立有效续播点` capability reason. Original progress values are returned unchanged.
- `resolve()`: played remains mandatory for playback=true. It is no longer a precondition for deletion identity resolution. CID, key/source match, quarterly provenance, local-write permission, scope fingerprint, ITEM other-quarter guard, exact Writer fallback prohibition and tombstones remain unchanged. This permits deleting an existing no-progress ITEM without granting playback capability.
- SEASON generation retains existing valid season/episode/progress and owner checks. No fabricated row, TMDB/name grouping, extra CID access or fallback to legacy aggregated read.
- No production WebHome JS/CSS/HTML change was necessary: current HTML admission checks identity/title/product visibility separately from canResume. Only HTML regression tests changed. Existing title/explicit-expired product filters and default detail Continue Gate remain untouched.
- The previous `metadataOnlyRowsAreNotPretendPlaybackHistory` test expectation of an empty list was itself the mistaken coupling. It is replaced with assertions that the physical row stays visible, progress is not invented, exact playback refuses it and exact deletion succeeds. Identity/scope protections were not relaxed.

### Counts and boundary coverage

Environment: Robolectric, in-memory Room, actual HomeWebBridge history method; exported synthetic responses consumed by current SDK expressions/production HTML functions and Chromium DOM. Same CID7, synthetic works A/B, A valid throughout. B positions {0,-1,50000} × durations {500000,0,Native TIME_UNSET}: nine combinations. TIME_UNSET represents Native unknown/missing time state; separate HTML cases omit position/duration fields entirely. Additional Native/HTML cases remove TMDB metadata while retaining a valid physical identity.

| Layer | Before, B position0/duration>0 | After, all nine combinations |
|---|---:|---:|
| Authorized DAO rows | 2 | 2 |
| Actual Legacy Bridge/API | 2 | 2 |
| Actual Independent Bridge/API | 1 | 2 |
| HTML normalized records | Not measured before fix | 2 |
| state.recent.items | Not measured before fix | 2 |
| TV record cards | Not measured before fix | 2 |
| Mobile record cards | Not measured before fix | 2 |

Both identities remain distinct. Mobile's existing More control is excluded from record-card counts. Invalid-progress B is displayed/selectable, Continue is disabled, poster activation never issues fm.vod for B or A. Played/restorable values remain separate from physical presence. Native strict deletion of no-TMDB/no-progress B preserves A and returns authoritative readback.

### Verification

- Native Leanback/Mobile: each 33 isolated tests (WebHomeHistoryServiceTest17, HistoryRecordRefTest4, HistoryResumePayloadTest5, TmdbSeasonProgressStoreTest7), zero failures/errors. Covers same-work independent routes, same-row line/episode changes, current/global CID, season0/unknown, missing/changed identity, frozen GROUP/new member, SEASON scope, exact line selection and permission failures.
- HTML actual SDK expression contract: 15 checks with Native fixture export enabled; standalone missing-progress regression remains active even without an export.
- Chromium history: original 26 scenarios plus TV and Mobile nine-case Native-response pipeline scenarios, all pass. Single/double/third-confirm, held key, management/cancel/all/delete, failure handling, neighboring focus, player-return simulation and 120-record DPAD remain covered.
- Shared immersive browser regression passed: TV filter/Back, Detail/Search/category return and Mobile Sidebar/Search. E01–E24 matrix above retains isolated/static PASS evidence; E25–E28 retain real performance/player/provider/native-page PENDING boundaries. No full TCL playback, Pan login failure or Native UI test occurred.
- Both ARM64 Release candidates built in 1m19s using existing offline toolchain and fastRelease=true. The later change only added a Native test; final production APK code is unchanged and is not rebuilt unnecessarily.
- One formal WebHome Build/Verify passed; generated index unchanged: 1,810,735 bytes, SHA-256 `05282873e8753a6397723cefbab5ac3a3fac8bc1f6993a8da71e725b7c2c7047`. Child diff checks passed.
- External evidence directory remains history-integrated. Current logs: loss-native-final.log (builds), loss-native-tests.log (final33 tests), loss-native-fixtures.json (actual synthetic Bridge responses), loss-html-contract.log, loss-browser.log, loss-shared-regression.log.

### Recovery anchor

Production changes limited to WebHomeHistoryService; Native/HTML tests and this existing contract document updated. Completed: first-loss reproduction, targeted fix, Native33 tests per flavor, actual-provider fixture→HTML/state/DOM proof, TV/Mobile regressions, both APK builds and HTML Build/Verify. Remaining acceptance: install this updated App with current HTML and confirm both TCL records, no-point capability, individual management and exact resume for valid A. Native progress throttling, queued writes and future detail-session work remain deferred. Next action: one normal local commit per modified child, with App guard recovery tag; **PUSHED=NO**.
