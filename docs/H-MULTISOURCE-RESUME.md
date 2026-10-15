# H-MULTISOURCE-RESUME

## Scope, baseline and evidence

App eecf6e16d74ffe3a5ccb8d355734e6ce71eea6b2; WebHome
b2fe7b92f13d5ae566928a6c850f62b23934a26b. Local commits only.
Protected pre-existing untracked scripts/build-arm64.ps1 is excluded.

AudioUtil.isAudioSiteEnabled (24-27) returns true for push_agent unconditionally:
this is eligibility for audio handling, not a video History row's media type.
AudioActivity.startSite (158) excludes PUSH; actual PUSH audio routing checks
AudioUtil.isAudio(result) (187-196). AudioHistory is the separate audio store.
Normal HomeWebBridge.playVod finds the exact source/id key and launches legacy
startDirect. New history_exact first calls WebHomeHistoryService.resolve.
HomeWebBridge.playUrl (256-270) and playPan (512-527) both launch SiteApi.PUSH:
ordinary URL playback, disk search and PUSH video share this proven failed predicate.
Previously record() used audio eligibility to set canResume=false and
capabilityReason=audio; resolve() repeated that rejection before any player launch.
HTML normalizeHistoryItem copies canResume, and playNativeHistoryExact rejects
historyCanResumeExact=false before invoking SDK. Thus no playback callback can fix it.

## Contract and minimal correction

No new API or identity contract. PUSH video rows from the existing History table
are not classified as separate audio histories merely because PUSH supports audio.
One private predicate supplies both record capability and resolve admission.
Native identity/CID, positive position/duration, source availability, exact player
line/episode validation and existing configured audio-site guard remain unchanged.
No fallback, title matching, player/D-SESSION or HTML gate changes.

Native tests use ordinary video URL, disk-share URL and PUSH URL rows, verify
capability, exact resolved source/key/episode/progress and both variants' actual
startHistoryExact Intent. Export real service records for the HTML test, which
checks normalization and actual SDK expression without relaxing its gate.

## Verification and handoff

- Native test results: Leanback and Mobile each 19 history-service tests plus
  30 detail-session tests (the latter includes inherited history cases), zero
  failures/errors. New cases verify three video routes' original keys, URLs,
  episode identities, positions and actual startHistoryExact Intent extras.
  Existing deletion/season/CID/missing-progress tests remain green.
- Positive capability and resolve are verified using real in-memory Room rows.
  The old predicate would reject them because audio eligibility is true; tests
  assert that prerequisite explicitly. Existing configured audio-source, no-point
  and cross-CID refusals are retained and tested.
- Exported Native records consumed through actual HTML SDK history expression:
  normalization -> state.recent.items -> historyCanResumeExact -> history_exact
  handoff preserves the complete HistoryRecordRef including scopeToken. PASS.
  Zero-progress/canResume=false stays rejected before SDK invocation. PASS.
- HTML history-record-contract: 15 groups PASS; D-SESSION Chromium matrix PASS.
  The first added fixture test accidentally used the old load() fixture builder,
  replacing the Native scopeToken; fixed the test to read the actual SDK response,
  without weakening the exact-reference assertion or changing production HTML.
- Both ARM64 Release builds PASS (offline fastRelease).
- HTML verify PASS; generated HTML unchanged, no rebuild needed for test-only
  WebHome diff. Both repositories diff --check PASS.
- Protected build-arm64.ps1 SHA256 unchanged:
  17d2a8f424b210f4888e89a68c08de0464a2af38b66a9fab516a2e2501b4d859.

Production diff is only the private history-service predicate and its two callers.
WebHome diff is only regression coverage. No HTML gate, list, deletion, player,
D-SESSION, schema or Suite gitlink changes. Real user history never touched.
TCL real playback, authenticated disk resource resolution and remote-key behavior
remain PENDING; isolated tests/builds do not prove those device outcomes.

## Recovery anchor

Root cause correction and targeted regressions complete. Next: normal local App
guard commit/recovery tag and WebHome test commit, then stop for TCL acceptance.
PUSHED = NO. Rollback is the respective local commit parent; no database migration.
