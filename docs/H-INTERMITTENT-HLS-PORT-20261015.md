# H-INTERMITTENT-HLS-PORT-20261015 — HLS local endpoint port alignment

## Scope and approval

- Objective: keep App-generated HLS child-playlist and segment URLs on the Nano server instance that is currently listening.
- Scope: `app/src/main/java/com/fongmi/android/tv/server/process/M3u8.java` and `app/src/test/java/com/fongmi/android/tv/server/process/M3u8LegacyFallbackTest.java`.
- Protected initial dirty path: `scripts/build-arm64.ps1` (untracked; never stage or alter).
- User authorized root-cause fixes, local commit only, no push.
- Baseline: branch `sync/upstream-beta-bugfixes-20261006`, HEAD `b41075b7bb6463fe4b5d76ce2625bba971e41885`.
- Rollback: revert the task commit; this restores the previous fixed-port rewrite.

## Evidence and decision

### Confirmed source behavior

- `app/src/main/java/com/fongmi/android/tv/server/Server.java::start()` tries ports `9978..9998`, then stores the successfully bound port in `com.github.catvod.Proxy`.
- `Server::getAddress(String)` builds a loopback URL from that active port.
- `app/src/main/java/com/fongmi/android/tv/server/process/M3u8.java::proxy()` previously hard-coded `127.0.0.1:9978` for every rewritten child playlist/segment.
- Therefore, if Nano cannot bind `9978` and selects a later port, its own `/m3u8` rewrites point away from the active App server. A restart can change the selected port after the prior listener releases its port, explaining a failure that disappears after restart.

### Supplied runtime evidence and limit

- Failed traces show the resolved input route as `EXTERNAL_LOOPBACK_PROXY` / `UNREGISTERED_LOOPBACK_PORT`; the player then receives a 13-byte `text/plain` response, and IJK records HTTP 400. Exo HLS `#EXTM3U` parsing and MPV `unknown_format` are downstream symptoms of that response.
- The supplied log redacts the actual playback URL and does not record Nano's selected port, request path, response body, or a same-URL post-restart attempt. Thus the trace is consistent with the fixed-port mismatch but cannot prove that these exact requests used `/m3u8`.
- The fix is limited to a proven in-repository address-authority mismatch. Device-level attribution and resolution remain pending until a repeat test records the active Nano port and the failed URL's sanitized port/path.

### Alternatives

- No change: leaves the mismatch whenever Nano selects a fallback port.
- Force Nano to bind only port 9978: changes server startup/failure behavior and is unnecessary because the project already supports a port range.
- Use `Server.getAddress("/m3u8")` for generated children: reuses the existing active-server address authority and preserves the 9978 URL when 9978 is active. This is the selected minimal correction.

No upstream player or protocol behavior is being adopted or changed. HLS content, upstream requests, headers, cache policy, player engines, and playback decisions remain unchanged.

## Implementation and verification

- [x] Replace the hard-coded rewritten child URL with the current Nano server address.
- [x] Add a regression test that changes the active server port and verifies the generated URL follows it.
- [x] Run targeted App test: `:app:testLeanbackArm64_v8aDebugUnitTest --tests com.fongmi.android.tv.server.process.M3u8LegacyFallbackTest` passed.
- [x] Run WebHome history contract test and read-only `scripts/verify-build.mjs`; both passed, WebHome source and generated index were unchanged.
- [x] Review scoped diff and `git diff --check`.
- [ ] Local commit and recovery tag; no push.
- [ ] TCL same-resource before/after-restart verification remains user/device dependent.

The separate WebHome Playwright interaction test could not start in this sandbox: Node received `EPERM` while resolving the host `C:\Users\Administrator` profile path. This is an environment limitation, not a passing browser simulation. The Native/TCL playback path remains pending device verification.

## Recovery anchor

Current next action: create the scoped local commit after guard verification. Do not touch the protected untracked build script, other player code, WebHome sources, Suite gitlink, or remotes.
