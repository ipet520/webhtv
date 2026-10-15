# D-SESSION: detail playback session

## Scope and baseline

- App: 3401edeb6b4dea85ea79c8f2f4946ebbaafd20f4, sync/upstream-beta-bugfixes-20261006.
- WebHome: ddcd10c7b17d61deadb1c779c10b064b68eafe8a, codex/mobile-aggregate-filter-layout.
- App pre-existing untracked scripts/build-arm64.ps1 is protected.
- Local commits only; no push, Suite pointer, Native history schema, UI or player engine changes.

## Audit and authority

```text
openDetail -> syncDetailMode(latest matching history)
           -> prepareDetailPlayback(continueGate)
           -> verifyDetailNativeHistory -> CONTINUE_READY (no provider race)
prepared handoff -> history-derived target -> pan.play
Native legacy pan.play -> VideoActivity -> History.findPlayback(title/source)
return -> rearm preparation + reconcile latest work history
close -> cancel run; UI snapshot/context survive separately
```

The old run governs preparation/provider commit. detailMode/detailHistory govern
the button and can also override its playback resource/episode. detailHistoryHandoff
is the old history resource handoff gate. detailEpisodeTarget is the UI target.
historyResume represents explicit historic playback intent. pan.playbackReturn and
detailReturn restore controls/scroll/focus. watch is preference sampling; it is
not a progress writer. fm_pan_playing denotes a return opportunity, not success.
Native Search marker owns its separate return. UI snapshot/context are auxiliary
recovery data, not proof that a new detail has prepared a playable resource.

Three HTML takeover sites are the initial Continue Gate, history-derived target
at provider handoff, and return winner reconciliation. Native legacy VOD and PAN
also auto-match persisted history; changing HTML alone cannot exclude takeover.

## Design decision before implementation

Keep provider races and health admission. Add one short-lived detail session
identity; the existing detailPlayback run remains its preparation generation.
detailMode/history become derived UI views, never independent resource winners.

Native owns a bounded in-memory lease containing only the launched route and the
History object used by that session's player. It stores no second persistent
progress/history. Version 1 player.detailSession provides play/read/close.
First play creates player history normally without automatic cross-record matching.
Replay uses only that lease's exact source/episode and persisted Native record;
missing/deleted/unverified progress cannot select another historical source.
Native source/episode changes update the bound object. No HistoryRecordRef change.

HTML records session ID, preparation generation, selected candidate/target,
request ID, acceptance and return intent. REQUESTED, ACCEPTED and actual playback
UNKNOWN are distinct. A successful Bridge reply proves queued acceptance only.
Same request ID is idempotent; closed/expired/CID-mismatched leases reject.

On return, retain candidate and read only the owned Native lease, never a title
winner. On close/switch, invalidate async work and close the lease. Reload recovery
uses the existing UI snapshot with a bounded return record and Native validation;
expired/corrupt/mismatched/native-process-lost records cannot take over detail.
Old SDKs prepare resources but reject unsupported session handoff explicitly.

| Event | Old behavior | New behavior |
|---|---|---|
| Open with A history | A can suppress race | New session and race |
| Prepared B | A can modify target | This session's B |
| Return from B | Latest work history wins | Bound Native B/session choices |
| Close/reopen | Auxiliary history can restore old READY | New identity and health admission |
| Explicit history A | Exact native A | Unchanged |

Lifecycle: OPEN/PREPARING -> READY -> REQUESTED -> ACCEPTED (actual UNKNOWN)
-> RETURNED -> REQUESTED; failures are recoverable; CLOSE terminates the identity.
Preparation and handoff generation must reject callbacks from previous targets.

## Best-practice evidence and alternatives

Accessed 2026-10-14. Android activity lifecycle documentation (primary):
https://developer.android.com/guide/components/activities/activity-lifecycle
Backgrounding is distinct from destruction; process loss must safely invalidate
in-memory state. MDN sessionStorage documentation (platform reference):
https://developer.mozilla.org/en-US/docs/Web/API/Window/sessionStorage
Reload persistence is page-session scoped, so recovery must not assume every newly
created WebView shares it. Use existing snapshot storage plus an expiring Native lease.

Exact local source/tests at the baselines above: existing HistoryRecordRef,
WebHomeHistoryService and stale provider-run patterns are mature project mechanisms
to reuse. No upstream patch is being integrated; upstream PR/revert evidence and
papers/benchmarks cannot establish this private Bridge contract and are inapplicable.

- No change preserves confirmed takeover and fails S02-S05.
- HTML-only suppression leaves Native History.findPlayback takeover possible.
- Unmodified legacy Native auto-resume is preserved for ordinary calls, but cannot
  express detail lifetime ownership.
- Narrow versioned lease is selected: isolated, reversible, compatible default
  calls; bounded memory and no extra content queries. Risks are lifecycle/reload,
  async source changes and legacy downgrade; test them explicitly.

Acceptance: S01-S26 isolated matrix, actual SDK expressions, existing provider/PAN,
history and TV/Mobile checks; Native tests and both ARM64 candidates if App changes.
Chromium, Native unit tests, builds and TCL verification reported separately.
Rollback: revert each local D-SESSION commit to the two baseline parents together;
no database migration or history deletion, no generated parallel UI.

## Implementation and removed conflicts

- Native: new WebHomeDetailSessionService; HomeWebBridge dispatch and full SDK
  expression; both VideoActivity variants receive the explicit session Intent.
- Fresh session Intent bypasses legacy History.findPlayback and late TMDB rematch
  only for that Intent. Ordinary VOD/PAN and history_exact remain unchanged.
- Owned replay uses the physical ITEM key, never a seasonal aggregate payload.
  Source/episode identity, current CID, positive progress and duration are checked.
- HTML: new 07a-detail-session; existing race, target setter, rendering wrapper,
  handoff/close/return and UI snapshot consume the same session/generation.
- Removed old detail Continue Gate verification, old history-first handoff and
  latest-history return rearm/Native Search retirement. Provider race and health
  admission are reused. Invalid cached candidates become failed, never false READY.
- Close-animation callbacks and late Bridge acceptance cannot overwrite a new
  generation or returned progress. Explicit retry after failure/no point prepares
  a new candidate; unsupported SDK refuses without calling legacy playback.

No persistent history schema, HistoryRecordRef, progress writer, delete service,
CSS, App engine, dependency or protected build script changes.

## Verification matrix (2026-10-14)

C = Chromium actual rendered source/SDK expression with isolated transport;
N = Native Robolectric + in-memory Room, both player variants;
R = existing regression tests; S = unchanged call path reviewed in source.
All TCL end-to-end cases remain PENDING; these labels never mean real-device PASS.

| Case | Evidence | Result |
|---|---|---|
| S01 fresh no history | C, provider race | PASS |
| S02 old A does not suppress preparation | C, N | PASS |
| S03 selected B wins over old A | C, N | PASS |
| S04 return/replay owns B | C, N | PASS |
| S05 progress refresh without latest A takeover | C, N | PASS |
| S06 repeated replay one accepted handoff | C, N idempotency | PASS |
| S07 close/reopen new preparation identity | C | PASS |
| S08 late old provider cannot overwrite B | C | PASS |
| S09 repeated close/animation/return isolation | C | PASS |
| S10 Curated-first and Direct-first single commit | C | PASS |
| S11 invalid candidate no handoff, explicit fresh retry | C | PASS |
| S12 rejection no false acceptance | C | PASS |
| S13 same WebView return, late acceptance isolation | C | PASS |
| S14 reload with valid Native lease | C | PASS |
| S15 damaged/expired/work/source/lease mismatch | C, N | PASS |
| S16 explicit history A stays exact | R history-record-contract, inherited N | PASS |
| S17 no-point history remains listed, cannot exact resume | R, inherited N | PASS |
| S18 Native Search cannot retire unrelated prepared session | C | PASS |
| S19 PAN/PUSH identity versus ordinary VOD | C PAN route; N VOD Intent; R PUSH exact; S ordinary paths | PASS in isolated tests/source |
| S20 season/episode/line target generation | C, N owned episode mutation | PASS |
| S21 held Enter/repeated operation one handoff | C | PASS |
| S22 TV detail focus/scroll and Home return | C, R tv-immersive-browse | PASS |
| S23 Mobile detail/history/Sidebar/return | C lifetime, R immersive Mobile | PASS |
| S24 missing SDK safely refuses | C | PASS |
| S25 closing lease preserves Native history | C, N | PASS |
| S26 cached invalid candidate cannot bypass health admission | C | PASS |

Commands/results:

- tests/detail-session.mjs: PASS; deterministic providers/health/network fixtures,
  actual SDK expression and actual DOM/keyboard. Live resource availability not tested.
- tests/history-record-contract.mjs: PASS (14 groups).
- tests/tv-history-interaction.mjs: PASS (remote/key/click/delete/manage isolation).
- tests/tv-immersive-browse.mjs: PASS including TV filters/search/Home return and
  Mobile paths. First concurrent run failed an unchanged navigation animation
  subpixel DOMRect equality; baseline passed and isolated current rerun passed.
  No test expectation or navigation style changed.
- tests/detail-fixed-viewport.mjs: PASS static/synthetic contract; real TV reveal
  explicitly NOT_SIMULATED.
- tests/tv-detail-title-layout.mjs: PASS six Chromium viewports including 1280x400.
- Native WebHomeDetailSessionServiceTest: 28 tests per variant, zero failures/errors,
  including 17 inherited independent-history tests per variant. Expanded Intent test
  initially failed test compilation on Shadows overload API; typed ShadowActivity
  resolved it, assertions unchanged.
- Gradle offline fastRelease: Leanback/Mobile ARM64 Release assemble PASS.
- HTML build once, verify PASS, both repositories diff --check PASS.
- Generated index.html: 1,804,476 bytes; SHA-256
  9e679b343db4374c0b090e7094c554b74d18594d0c489ddd113fda3aaf00d918.
- Protected scripts/build-arm64.ps1 SHA-256 unchanged:
  17d2a8f424b210f4888e89a68c08de0464a2af38b66a9fab516a2e2501b4d859.

## Capability limits and real-device acceptance

Bridge acceptance proves the request was queued, not that playback started;
actualPlayback remains UNKNOWN. Only persisted, validated owned Native progress
permits continuation. No point means explicit fresh retry, not borrowed history.
In-memory leases expire after two hours and disappear on Native process loss;
recovery then prepares fresh. Reload requires the existing UI snapshot and a live,
identity/source-matching Native lease. No restoration from title guessing.
Unknown cross-source media identity is refused; verified same TMDB work can bind.
Missing SDK requires the paired App candidate. These are deliberate safe limits.

TCL acceptance must check real Curated/Direct qualification, failed PAN auth/play,
actual progress persistence, same-detail replay, season/episode changes, WebView
rebuild and process loss, repeated remote confirmation, exact independent history,
and Mobile playback/return. No user history was written/deleted by these tests.

Reusable Native capability: explicit play intent, bounded ownership lease, request
idempotency and owned physical-history resume; no HTML-only matching algorithm is
required for a future native UI consumer.

## Recovery anchor

Implementation and isolated verification complete. Next action: atomic local App
guard commit/recovery tag, normal local WebHome commit, report exact IDs and stop.
Do not push or update Suite/stable pointers. Protected untracked build-arm64.ps1
must remain untracked and unchanged. TCL_REAL_DEVICE_STATUS = PENDING.
