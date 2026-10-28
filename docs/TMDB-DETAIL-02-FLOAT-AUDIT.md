# TMDB-DETAIL-02 原生悬浮播放专项审计

日期：2026-10-27。依据：当前本地源码及未提交内容、实际 Leanback ARM64 Debug APK Manifest、随项目分发的 Media3 源码包。本部分全程只读，未修改播放器、Fusion 控制器、详情页或 Manifest，未实施自动悬浮。

## 结论与八个问题

总体结论：**PARTIAL_ADAPTATION**。可直接复用播放核心、现有 PlayerView、根层 playerPanel、滚动占位及生命周期；目标所需的页内小窗布局状态尚不存在，需要局部展示适配。

现有 **系统 PiP 窗口机制本身为 NOT_DIRECTLY_COMPATIBLE**：它缩小整个 Activity，不会把视频单独分离后继续让同一个详情 Activity 全尺寸浏览。不能以“已有 PiP / 后台播放”推导目标已经具备。

| 问题 | 结论 |
| --- | --- |
| 1. 现有悬浮采用什么方式？ | 视频小窗是 Android 系统 PiP，Activity 调用 enterPictureInPictureMode；不是系统 WindowManager 视频悬浮窗。另有独立音频迷你条与桌面歌词。 |
| 2. 能否直接复用于融合详情？ | PiP 不能直接满足同 Activity 继续浏览；复用现有 playerPanel 做 Activity 内布局适配可行。 |
| 3. 能否保留同一播放器和状态？ | 源码结构支持：Service 持有 PlayerManager，panel 常驻根层。仅改布局不需要 parse/start/prepare/setPlayer 或切渲染器。连续性仍需真机确认；同 Service 不等于任何情形下引擎永不重建。 |
| 4. 能否向下自动浮起、向上恢复？ | 可局部适配现有唯一滚动同步入口；当前没有该状态机，未实现。须根据占位区域实际可见度和不同进出阈值判断。 |
| 5. 小窗时能否继续浏览？ | 推荐的页内根层容器可支持，需在小窗态撤销其焦点参与和自动抢焦点；现有系统 PiP 不能保留同 Activity 的全尺寸详情交互。 |
| 6. 有无 Surface/闪黑/焦点/生命周期风险？ | 有。尺寸改变仍可能重配 Surface/解码输出；现有恢复流程会 scrollTo(0,0) 并 requestFocus，不能原样复用。没有真机证据承诺零闪黑。 |
| 7. 哪些可复用、哪些文件需要改？ | 复用 Service/PlayerManager/PlayerView/字幕与 OSD、panel/spacer、全屏及详情生命周期；预计生产修改集中 TmdbDetailActivity，必要时 FusionDetailController 和现有布局，详见后文。 |
| 8. 最小适配及窗口承载是否要换？ | 保留当前 Activity 根层 panel，增加 Fusion + Leanback 专属页内浮动布局状态。相对系统 PiP 是换承载方式；相对当前内嵌播放器无需换父容器、Activity 或 PlayerView。不需新窗口/Service/播放器。 |

## 1. 已有能力逐一辨别

### 系统视频 PiP：入口、创建及退出

- 普通 Mobile 点播控制按钮绑定 onPiP：`app/src/mobile/java/com/fongmi/android/tv/ui/activity/VideoActivity.java:1659`；方法 `app/src/mobile/java/com/fongmi/android/tv/ui/activity/VideoActivity.java:3891` 调用 mPiP.enter(force=true)。自动离开页面入口 `app/src/mobile/java/com/fongmi/android/tv/ui/activity/VideoActivity.java:9756`，Android 12+ 还走 onPictureInPictureRequested，最终仍调用 PiP helper。
- Mobile 直播也有已有入口：`app/src/mobile/java/com/fongmi/android/tv/ui/activity/LiveActivity.java:630` onPiP，经 prepareLivePiPView / OneShotPreDrawListener，`:661` mPiP.enter；`:1968` onUserLeaveHint。它不是独立悬浮播放 Service。
- TMDB 内嵌的手动按钮绑定：`app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:1353`。实际入口 `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10086`；自动入口 `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:11170`。
- Leanback PiP helper `app/src/leanback/java/com/fongmi/android/tv/utils/PiP.java:33` 检查 API 26 和 FEATURE_PICTURE_IN_PICTURE；`app/src/leanback/java/com/fongmi/android/tv/utils/PiP.java:82` 设置比例、source rect、动作及系统 seamless resize 后调用 Activity.enterPictureInPictureMode。Mobile 同名 helper 是 flavor 实现，不是另一套播放器。
- 系统 PiP 不使用 SYSTEM_ALERT_WINDOW 运行时授权；需要系统 PiP 功能、Activity Manifest 支持及系统允许状态。helper 会捕获失败并返回 false，后台策略可能禁止自动进入。`app/src/main/java/com/fongmi/android/tv/setting/BackgroundPlaybackPolicy.java:19` 只在后台开启且非音频模式时允许自动 PiP。
- PiP 控制操作通过 RemoteAction → ActionReceiver → PlaybackService，`app/src/leanback/java/com/fongmi/android/tv/utils/PiP.java:65`，`app/src/main/java/com/fongmi/android/tv/receiver/ActionReceiver.java:18`。不是另建视频引擎。
- 系统提供展开/关闭；融合页 `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:11176` 进入时铺满 panel、隐藏控制，退出恢复；`app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:11195` 用 PipExitDecision 区分回前台与关闭，关闭会保存历史、结束同步和 finishPlayback。Mobile 普通点播回调位于 `app/src/mobile/java/com/fongmi/android/tv/ui/activity/VideoActivity.java:9801`。
- 不能声称 PiP 只在离开 Activity 后才能启动：手动按钮可在 Activity 仍可见时启动。但启动后整个 Activity 进入系统小窗，并非同 Activity 内与详情共存。

### Leanback 的实际边界

- **`app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10099` 明确返回 Util.isMobile() && !PiP.noPiP()**。`app/src/main/java/com/fongmi/android/tv/utils/Util.java:439` 按 BuildConfig.FLAVOR_mode 判定，Leanback APK 在手机上运行也不会变成 Mobile。
- `app/src/mobile/AndroidManifest.xml:151,164,171` 给 LiveActivity / VideoActivity / TmdbDetailActivity 开启 supportsPictureInPicture。
- `app/src/leanback/AndroidManifest.xml:155,163` 的 VideoActivity / TmdbDetailActivity 没有该属性；本轮构建 APK 的两项也确认为 absent。
- Leanback VideoActivity 的 mPiP 仅声明和初始化（`:487` / `:1524`），没有对应调用 enter 的入口；Leanback LiveActivity 检索不到 PiP 调用。helper 存在不代表 TV 入口可用。
- 结论：**当前 Leanback 融合页原生系统 PiP 路径不可达**；未在电视执行系统 PiP 测试，不把可编译或设备支持 FEATURE 当作真机可用。

### 后台音频、App 内迷你条与系统悬浮窗

- PlaybackService 是 MediaLibraryService：`app/src/main/java/com/fongmi/android/tv/service/PlaybackService.java:59`；它持有播放实例，不负责创建 WindowManager 视频窗口。
- `app/src/main/java/com/fongmi/android/tv/ui/audio/AudioMiniPlayer.java:149` 将 view_audio_mini_player 加到 Activity 的 android.R.id.content，内容是封面、标题和音频控制；无视频 Surface。`:116` activate / keepAlive 及 `:350` bindService 可复用的是音频业务，不能当作已有视频小窗直接移植。
- `app/src/main/java/com/fongmi/android/tv/ui/activity/AudioActivity.java:314` 的最小化是 moveTaskToBack(true)，属于后台音频。另 `:615` closeAudio 只有 Mobile 调用 `:620` minimizeToFloatingPlayer → AudioMiniPlayer.activate + finish；Leanback 关闭音频走 stopAndFinish。这个名为 FloatingPlayer 的路径仍不含视频渲染视图。
- Manifest `app/src/main/AndroidManifest.xml:16` 虽声明 SYSTEM_ALERT_WINDOW，但实际 WindowManager 系统悬浮使用定位到 `app/src/main/java/com/fongmi/android/tv/player/lyrics/DesktopLyricsWindow.java:169` buildParams、`:179` TYPE_APPLICATION_OVERLAY / TYPE_PHONE，内容为桌面歌词。它需要相应悬浮窗权限，不承载视频。
- 已检索 App Java/Kotlin 的 PiP、WindowManager overlay、悬浮入口及相关资源；没有发现独立视频悬浮 Service/Controller。播放器 mini progress 配置也是进度显示，不能据名称解释为小窗视频。

## 2. Fusion 实例、渲染与布局调用链

```text
TmdbDetailActivity → modeController(FusionDetailController)
  → showInlinePlayer / shouldBindPlaybackService
  → PlaybackActivity 绑定 PlaybackService
  → PlaybackService.player(): PlayerManager → 当前底层 Player
  → PlaybackActivity.attachSurface() → 原 binding.exo(PlayerView)

NestedScrollView → setupInlinePlayerSpacerSync() → syncInlinePlayerToSpacer()
  → root 常驻 playerPanel.translationY 与 scroll 内 playerPanelSpacer 对齐
```

- `app/src/main/java/com/fongmi/android/tv/ui/detail/FusionDetailController.java:24`、`app/src/main/java/com/fongmi/android/tv/ui/detail/FusionDetailController.java:29`、`app/src/main/java/com/fongmi/android/tv/ui/detail/FusionDetailController.java:44`：Fusion 开启内嵌 panel 和 spacer、隐藏 heroSpacer，并显示 fusionActions。`app/src/main/java/com/fongmi/android/tv/ui/detail/FusionDetailController.java:63` 仍调用 host.playInline，不是独立播放器。
- `app/src/main/java/com/fongmi/android/tv/ui/detail/BaseTmdbDetailModeController.java:55` shouldBindPlaybackService 由内嵌/自动播放决定；TmdbDetailActivity 的 `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:720` 委托它。
- `app/src/main/java/com/fongmi/android/tv/service/PlaybackService.java:141` 创建一次 PlayerManager 和 MediaLibrarySession；`app/src/main/java/com/fongmi/android/tv/ui/activity/PlaybackActivity.java:103` 获取 Service 同一 manager。`app/src/main/java/com/fongmi/android/tv/service/PlaybackService.java:687` 说明引擎也可能因正常解码/换媒体路径重建；新小窗不能调用此类启动路径。
- `app/src/main/java/com/fongmi/android/tv/ui/activity/PlaybackActivity.java:656` 按引擎实例身份比较再 setPlayer，维护 ASS 和外部字幕 session；`app/src/main/java/com/fongmi/android/tv/ui/activity/PlaybackActivity.java:809` 会 setPlayer(null) 并 detach 字幕。**自动小窗不应调用 detachSurface / setRender / reattachVideoSurfaceAfterReparent**。
- 布局 `app/src/main/res/layout/activity_tmdb_detail.xml:1002` 的 MaterialCardView 是 NestedScrollView 的根层兄弟；内有 PlayerView、弹幕和 OSD。`app/src/main/res/layout/activity_tmdb_detail.xml:1026` 表示初始不创建 Surface，实际由 attachSurface → getRender 决定，不能据 XML 推断一定是 TextureView。
- `app/src/main/java/com/fongmi/android/tv/ui/activity/PlaybackActivity.java:853`：原生引擎取 Surface render，LUT 等条件可能取 Texture，否则复用 PlayerSetting。两种都需覆盖验收，不为小窗强行切换默认渲染器。
- 本地库 `third_party/maven/androidx/media3/media3-ui/1.11.0-alpha01-fongmi/media3-ui-1.11.0-alpha01-fongmi-sources.jar`，内部 `androidx/media3/ui/PlayerView.java:618` setRender：类型改变时创建新 TextureView/SurfaceView、绑定新视频输出并清旧输出；`:704` setPlayer 会解绑旧输出和清字幕。库源码已只读提取到外部证据目录，未修改依赖。
- `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10270` 已有唯一 scroll listener；`app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10287` 注释及实现明确常驻 root、不 reparent，避免 Surface 销毁重建；`app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10368` 只同步位移，全屏/PiP 时跳过。
- `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10448` / `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10465` 均使用同一 panel；PiP 是把 Activity/panel 铺满小窗，不是再创建 Surface 或重新解析。播放进度/音轨本身仍在同一 Player，但状态最终受后台和销毁规则约束。

## 3. 目标行为与最小适配条件（尚未实施）

| 用户行为 | 现状 / 推荐条件 |
| --- | --- |
| 进入 Fusion | 沿用当前 inline 启动及自动播放设置。未起播、无视频轨、错误、非 Fusion 不自动浮起。 |
| 焦点移到下方按钮 | 保持 inline；不能仅按 DPAD_DOWN、焦点离开 panel 或 scrollY>0 触发。 |
| 播放区域完全滚出可视区 | 在现有 scroll/layout 同步入口计算 **spacer 相对可视 viewport 的矩形**；考虑系统 inset/顶部固定内容。进入与退出采用不同阈值和幂等状态，避免临界抖动。 |
| 浏览简介/演员/剧集 | 原根层 panel 调整为固定小窗尺寸/位置，保留 spacer 占位及原 exo，不隐藏/移除视频视图；停用小窗及控制子树的方向键焦点，不抢当前详情焦点。 |
| 向上回到观看区域 | 始终用原 spacer 而非小窗的位置判断；达到恢复阈值后恢复原布局、重新同步原 scroll 位移。保留当前 scroll/focus，不主动滚回0或 requestFocus。 |
| 主动全屏 | 继续现有 enter/exitInlineFullscreen；全屏期间禁止自动浮动评估，退出按当前几何状态重新判断，不能叠两套状态。 |
| 离开详情 | 不改现有保存历史、停止同步和所有权释放；不因为页内浮动开启 keepAlive 或跨 Activity 跟随。 |

建议的展示状态只是现有 INLINE / FULLSCREEN / SYSTEM_PIP 之外的 **Fusion 页内浮动布局标记**，不另建业务 Controller/播放器。复用原面板；转换时不调用 playInline、SiteApi、startPlayer、parse、prepare、seek、setRender 或 new Player。大小改变仍可能使 Surface resize，应通过现有诊断验证而不是宣称绝对无缓冲。

**恢复函数不能整套照搬：**`app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10390` 有 scrollTo(0,0)；`app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:2338` post(requestFocus)；exitInlinePiPLayout 也调用这些方法。自动切换需复用更细的尺寸/位移能力，不复用跳顶抢焦点语义。

## 4. 焦点、返回及生命周期风险

- `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:1509` 为 TV 配置 controls/时间条焦点链；spacer 获焦会立即转给 playerPanel（`:1551`）。小窗态必须局部禁用 panel 子树焦点并保留 spacer 返回观看区域的语义，不能让画面飞回旧位置或抢走详情焦点。
- `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:2316` 与 `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:2338`、`app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:11393` 都需要核对小窗态；Back 应继续先处理控制层/全屏/详情退出，不能错误触发 PiP 关闭或把小窗作为独立页面。
- scroll listener 只能保留一个，同步位置和浮动评估必须有统一次序；全屏、PiP、页面销毁、布局变化与异步回调均需 guard。不能在每次 scroll 重建 panel、post 一串过时任务或重绑 Player。
- `app/src/main/java/com/fongmi/android/tv/ui/activity/PlaybackActivity.java:1249` 按后台偏好暂停；`app/src/main/java/com/fongmi/android/tv/ui/activity/PlaybackActivity.java:864` 依 keepAlive/owner/其他消费者决定 release；`app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:11327` 保存历史、结束字幕/同步并按当前 owner 停播。页内模式不经过 Activity 后台生命周期是较小风险路径，不能从 Service 存在推导离开页也应持续播。
- 音轨、速度、进度保留的依据是相同 engine/session、不重新 setMediaItem；字幕/ASS/弹幕及输出尺寸还需真机覆盖。保持整个 panel 可见且不 detach 比 Surface reparent 风险小，但厂商 SurfaceView resize、圆角、z-order、HDR/LUT/字幕尺寸仍可能闪黑或裁切。
- 现有 `app/src/main/java/com/fongmi/android/tv/player/SurfaceDiagnosticCollector.java:70` 已记录 created/changed/destroyed；未来施工应复用它与现有 playback trace，对照播放器/Surface 身份、parse/start次数、position、丢帧和焦点归属，不再建第二套播放器诊断权威。

## 5. 预计文件范围与复用边界

以下是正式施工建议，**本轮未修改**：

| 文件 | 预计最小职责 |
| --- | --- |
| app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java | 必需：仅 Leanback Fusion 门控的布局状态、scroll可见性阈值、全屏互斥、focus子树启停和无跳顶恢复；不改解析/业务入口。 |
| app/src/main/java/com/fongmi/android/tv/ui/detail/FusionDetailController.java | 可选：若按既有职责收纳展示模式能力，仅暴露 Fusion 是否允许页内浮动，不迁移播放业务。能用现有 isFusionMode 则无需改。 |
| app/src/main/res/layout/activity_tmdb_detail.xml | 原结构已满足根层 panel+spacer，原则上不必改；若需最小窗尺寸资源/焦点声明再限定改动，不新增播放器节点。 |
| 现有 Leanback TMDB 布局/焦点测试及共享纯布局判定测试 | 新增阈值、状态幂等、全屏互斥、Surface/Player不重建、返回语义覆盖，范围待正式方案确认。 |

直接复用、预计无需修改：PlaybackActivity、PlaybackService、PlayerManager、PlayerView、PiP helper、播放器解析/缓存、字幕引擎和 Mobile。系统 overlay 权限/新 Service/新 Activity/新播放器均不需要。

## 6. 验证证据与限制

- PASS（静态）：上述源码调用链、flavor 门控、Manifest、现有库 Surface 处理已核对。
- PASS（APK）：本轮实际产物中 VideoActivity/TmdbDetailActivity 没有 supportsPictureInPicture；默认启用 HomeActivityCurrent alias 指向原 HomeActivity，无新增启动图标。
- 已阅读 `app/src/testMobile/java/com/fongmi/android/tv/ui/helper/PipExitDecisionTest.java` 和 `app/src/test/java/com/fongmi/android/tv/player/PlaybackServiceReleasePolicyTest.java`：覆盖关闭/前台及释放矩阵，不覆盖自动页内浮动、TV焦点或 Surface 连续性；本轮未单独执行这些测试，不能报为 PASS。
- A 部分 130 项定向回归和 Debug 完整构建通过，只证明现有代码编译/对应 UI 回归，不证明 B 的未来功能已可用。
- NOT TESTED：真实 TV PiP、页内自动浮动/恢复、SurfaceView/TextureView 连续画面、HDR/LUT/字幕/弹幕、长按浏览/焦点与全屏往返。自动页内浮动尚未实现，无运行性能改善幅度。

本报告交付后停止。等待用户确认 TMDB-DETAIL-02 正式施工方案。
