# NATIVE-HOME-07 — native background convergence and four TV repairs

## Approved scope and protection

Implement native-wall background ownership, rounded portrait fallback, text-width underline,
and source-title internal padding only. Preserve native business and all 01–06 work. No commit,
push, Suite pointer, Mobile/HTML/player edits. Shared CustomWallView receives only an opt-in
temporary content-background API; existing wallpaper behavior remains the default.

Before: App HEAD `030485b70c263621fe4575abe5758b28115832c3`, 264 dirty paths, 5218
original files. External before.json/full fingerprints and exact allowed originals are under
the directory in `%TEMP%/native-home-07-current.txt`. WebHome 114 files clean; WOGG 106 files,
104 dirty; their HEAD/status and Suite gitlinks are protected. Guard scope includes the
native wall, immersive image/view/host/preview adapters, style/menu/layout, focused tests and
this document. No HomeActivity or existing data/client edits are planned.

## Decision-ready evidence (2026-10-27)

- BaseActivity.setContentView/addCustomWall installs CustomWallView at content index 0.
  Its existing view_wall.xml ImageView is centerCrop; CustomWallView owns theme wallpaper,
  GIF/video stop/resume and original configuration. It has no film-art override API yet.
- Confirmed source and native-render root cause: old `ImageView.setAlpha(0)` selects the
  deprecated integer IMAGE alpha overload (0..255). `ViewPropertyAnimator.alpha(1)` changes
  VIEW alpha only. Image alpha stays zero permanently, despite the success callback hiding
  the portrait. A native-raster test reproduces transparent pixels with View alpha=1 and
  image alpha=0; restoring image alpha=255 paints the expected pixel. New wall explicitly
  sets View alpha=1f and image alpha=255. Physical TV display remains NOT TESTED.
- NativeHomeBrowseView independently owns two background views, front indices and animations.
  Both views, indices and animations are removed, including the separate fixture fade.
  Its root is transparent to expose the original native wall beneath it.
- Existing full-screen gradients use high midpoint opacity. Preserve readability but reduce
  midpoint cover; verify pixels in the right image region after composing the actual view tree.
- HomePreviewStyles.Underline uses button width minus padding. Font measure/layout width must
  replace that assumption, especially for minimum-width or overflow buttons.
- Drawer window geometry already correct. Only option text horizontal padding changes; outer
  content/window dimensions, gravity, text size/color/alignment and focus behavior stay intact.

Official references read:

- Glide Targets: https://bumptech.github.io/glide/doc/targets.html — request target reuse and
  clear ownership; keep existing Glide, bounded sizes and cancellation, no new cache/client.
- Android ViewGroup.dispatchDraw:
  https://developer.android.com/reference/android/view/ViewGroup#dispatchDraw(android.graphics.Canvas)
  — child draw dispatch can be overridden. Confirm after super.dispatchDraw; never add/remove
  child views during drawing. This confirms command submission, not physical TV presentation.

No change retains two background authorities and premature fallback exit. Another independent
Leanback BackgroundManager would introduce a third owner and is rejected. Selected adaptation:
reuse CustomWallView's existing ImageView with a per-view owner token, neutral original-wall
fallback and a one-shot draw acknowledgement. Suspend its normal motion/config refresh only
during this opt-in lease; restore original behavior on release. No persisted wallpaper/theme
selection changes and no movie-driven palette writes. The native wall alone draws film art.
Production uses the existing BaseActivity wall; explicit Debug fixture preview uses the same
component. No new movie background ImageView or crossfade engine.

Upstream merges/PRs/reverts and papers are inapplicable: this is a local ownership/draw fix,
not a dependency or algorithm upgrade. Local actual sources and Android/Glide contracts decide
the design. Remaining TV renderer uncertainty stays explicit.

## Acceptance and rollback

Executable tests must cover resource-ready-before-draw, failed/missing art retaining portrait,
actual composed image pixels, stale A-B-A/owner release, wallpaper/theme/config restoration,
rounded 2:3 portrait, measured text underline, unchanged drawer geometry and padded option
labels. Run native-home and shared-wall regression plus full ARM64 Debug assemble; inspect
actual APK signature/Manifest/ABI. No TV pass claims without a device.
Rollback uses task-only inverse patches against external originals; never reset/restore or
overwrite the dirty baseline. User has explicitly authorized this complete four-part stage.

## Latest portrait placement adjustment

User requested a slight upward move: top changes from 16% to 14.5% of viewport height
(about 16 pixels at 1080P). Width, height, horizontal placement and rounded corners stay intact.

## Changed files

All paths below are relative to App root.

- `app/src/main/java/com/fongmi/android/tv/ui/custom/CustomWallView.java`: opt-in temporary
  owner for existing image host; cancel stale acknowledgement, restore normal wallpaper on
  release. Default classic/Mobile wallpaper path remains unchanged.
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeBrowseImages.java`: existing
  Glide into borrowed native wall; portrait exits only after native wall draw; stale guards.
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeHost.java`: find existing
  BaseActivity wall, no duplicate background host.
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java`:
  transparent content, gradients, rounded and slightly raised portrait, background lease.
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomePreviewActivity.java`:
  standalone Debug fixture uses same native wall component.
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewStyles.java`:
  centered underline measures rendered text; existing theme/1.08 scale retained.
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewMenuDialog.java`:
  option/title horizontal internal padding 12dp; drawer outer geometry unchanged.
- `app/src/leanback/res/layout/view_native_home_preview.xml`: remove two movie background views.
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeImagesTest.java`:
  adapt existing image subscription tests to native wall and draw acknowledgement.
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeIntegrationTest.java`:
  transparent content and untinted content image assertions.
- New tests: `app/src/testLeanback/java/com/fongmi/android/tv/ui/custom/CustomWallContentBackgroundTest.java`,
  `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeBackgroundTest.java`,
  `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomePolishTest.java`:
  native rendering, actual Glide local PNG, owner lifecycle, stale/failure handoff, underline,
  portrait geometry and drawer padding.
- New report: `docs/NATIVE-HOME-07.md`.

## Final verification and artifact

- PASS: 119 tests, zero failures/errors. Covers home business/config/theme/menu regressions,
  original native wall lifecycle/motion and preferences, existing image generation/cancellation,
  old alpha failure reproduction, actual composed pixels, real Glide PNG decode, delayed
  portrait handoff, missing/failed/stale art, owner release, light/dark composed background,
  rounded/raised portrait, measured underline and drawer internal padding.
- Initial local Glide fixture failed because file URI is wrapped as GlideUrl by existing
  ImgUtil. Corrected test uses supported local PNG data URI. No production URL behavior changed.
- PASS: `:app:assembleLeanbackArm64_v8aDebug`; full Debug APK produced.
- PASS: `:app:processLeanbackArm64_v8aReleaseManifest`; no Release APK generated.
- PASS: APK v2 signature, Android Debug certificate SHA-256
  `97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`.
- APK package `com.silent.android.webhtv`, debuggable=true, ABI only `arm64-v8a`.
  Launcher remains enabled HomeActivityCurrent alias targeting HomeActivity; historical alias
  disabled. NativeHomePreviewActivity is Debug-only and absent from merged Release manifest.
- APK: `E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`
- Size: **209259698 bytes**.
- SHA-256: `e362c3ae769173c0b4e3a124a37dadb118ee93739be32c4185a29856d9ccc984`.
- Protection PASS: original App 5218 files, exactly 10 scoped files changed, zero missing,
  other 5208 byte-identical; four new scoped files. Dirty paths 264 -> 269. App HEAD unchanged
  `030485b70c263621fe4575abe5758b28115832c3`. Guard confirms protected dirty paths, scope,
  staged paths, branch and HEAD safe. WebHome/WOGG fingerprints, HEAD/status and Suite gitlinks
  unchanged. No Mobile source edit, commit, push, destructive cleanup or Suite update.

Physical TV / GPU presentation: **NOT TESTED** (no online ADB device). Must verify real TMDB
landscape visibility, missing/failing image portrait persistence, rapid A-B-A focus changes,
portrait placement/corners, underline appearance, drawer padding/overlay, theme changes and
classic/WebHome mode return. Automated native pixels establish actual draw output in the test
renderer, not physical television acceptance or quantified performance improvement.

## Recovery anchor

NATIVE-HOME-07 complete. Four repairs and the requested slight portrait raise implemented;
119 tests and full ARM64 Debug build passed; artifact and original-work protection verified.
No subsequent business phase started. Next action: await user's television acceptance feedback.
