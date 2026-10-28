# TMDB-DETAIL-02 页内自动悬浮播放

## Recovery anchor

目标：仅 Leanback Fusion，复用根层 playerPanel/PlayerView 和唯一滚动入口，自动浮动/恢复、遥控器移动、局部鼠标拖动、四边吸附及相对位置记忆；不改播放业务。当前：限定源码、Leanback/Mobile 自动化、ARM64 Debug 构建及保护核验完成；设备播放连续性未验。HEAD 030485b70c263621fe4575abe5758b28115832c3，初始304 dirty/5254文件，外部保护目录由 TEMP/tmdb-detail-02-current.txt 指向。唯一下一步：等待用户电视真机验收反馈；不主动继续施工。

## 设计依据（2026-10-27）

用户正式批准基于 docs/TMDB-DETAIL-02-FLOAT-AUDIT.md 的 PARTIAL_ADAPTATION 方案，增加移动及拖动要求。本地工作区源码为实际依据。

- 官方 SurfaceView API：https://developer.android.com/reference/android/view/SurfaceView 。证据等级平台文档；Surface 由独立生命周期和回调管理，尺寸变更不等同于播放器重建，设备表现仍需实测。不承诺零闪黑。
- 官方方向键导航：https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/navigation 。证据等级平台文档；可聚焦入口必须在实际方向链中可到达，采用原生 focusSearch + 末端桥接，不替换详情焦点系统。
- 官方移动手势：https://developer.android.com/develop/ui/views/touch-and-input/gestures/movement 。证据等级平台文档；使用 MotionEvent pointer 身份及 touch slop 区分点击/拖动，处理 UP/CANCEL，不全局捕获触摸。
- 项目固定 Media3 源码包 PlayerView.setRender/setPlayer（审计报告已有位置）和现有 SurfaceDiagnosticCollector：直接复用，禁止改库或改变渲染类型。既有 AudioMiniPlayer 是音频控件，不能拿来承载视频；可借鉴局部触摸/安全边界机制。证据等级当前源码。
- 没有适用的上游提交/PR/revert：本轮不是上游合并，也不采纳外部播放器方案；成熟实现以当前根层 panel/spacer 为基准。论文/基准不能决定此本地展示适配，性能结论需真机 trace，故不扩展该类研究。

方案比较：不改无法满足同页小窗；原系统 PiP 缩小整个 Activity 且 Leanback 门控不可达；另建 WindowManager/播放器违背实例连续性与权限边界；选择当前根层 panel 两种布局，原 spacer 保持占位。进入可见比例≤5%且已滚到视口上方，恢复≥30%，不以方向键/焦点离开触发。

持久化复用 Prefers，保存归一化 x/y，仅移动确认或拖动完成写入。播放器/服务/解析/音轨/字幕/历史生命周期不变。布局回退只需撤销本轮外部备份对照出的增量，不能 restore/reset 含既有修改的文件。验收：纯判定、布局实例/焦点/拖动及跨模式自动化、Mobile门控、完整ARM64 Debug；Surface和真实输入标 NOT TESTED，未验收不进入stable。

## 定向业务调用链

手动选集 renderFlagSelection → onPlay → modeController.play → playInline → startInlinePlayer；自动连播 NavigationCallback.onNext / ended → checkInlineNext → playAdjacentEpisode → onPlay。正常换集会按原业务解析新集并准备引擎，不能误报为“所有换集永不准备”。本轮仅禁止布局切换触发这些调用。

线路切换与 changeSource / 站内搜索 → switchSourceDetail → 当前Activity setIntent + resetDetailState + loadContent。需要保留当前小窗布局，并抑制 applyLoaded/focusInlinePlayerPanel/startInlinePlayer 的抢焦点。新源正常重载依原逻辑执行。阅读器协议和外部播放器按原路由离开Activity，不强行维持跨页小窗。

## 施工完成记录

状态：限定范围源码、自动化及 Debug 构建完成，**待电视真机验收**。未安装 APK、未连接电视、未采集实际解码/Surface/帧率数据。

### 实际文件

| 文件 | 作用 |
| --- | --- |
| app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java | 唯一修改的既有文件：Fusion/Leanback 门控、接入唯一滚动入口、换集/换源的展示状态保留、全屏互斥及焦点兼容。 |
| app/src/main/java/com/fongmi/android/tv/ui/detail/FusionFloatingPanel.java | 新增纯展示辅助：调整原 panel 的 LayoutParams/translation；一个无视频节点的移动代理、局部拖动及焦点隔离。 |
| app/src/main/java/com/fongmi/android/tv/ui/detail/FusionFloatingLayout.java | 新增纯几何：可见比例迟滞、安全边界、16:9、归一化位置和四边吸附。 |
| app/src/main/res/values/fusion_floating.xml | 移动入口及无障碍提示文本，无独立播放器控制面板。 |
| app/src/test/java/com/fongmi/android/tv/ui/detail/FusionFloatingLayoutTest.java | 4 项纯判定、迟滞、边缘、比例与尺寸变化测试，两种 flavor 共用。 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/detail/FusionFloatingPanelTest.java | 4 项真实 View/PlayerView 父容器、焦点、移动/取消/保存、合成指针事件、缩放边界及恢复测试。 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/activity/FusionFloatingIntegrationTest.java | 4 项：真实 Activity 按键入口、模板重绑、全屏往返与恢复、跨模式隔离、业务路径静态检查。 |
| app/src/testMobile/java/com/fongmi/android/tv/ui/activity/FusionFloatingIsolationTest.java | 1 项：Mobile 无法构造新展示宿主，现有 PiP 门控和 Manifest 支持保留。 |
| docs/TMDB-DETAIL-02.md | 本报告及本轮恢复锚点。 |

未修改 FusionDetailController、原布局 XML、PlaybackService、PlayerManager、PlaybackActivity、Media3、PiP helper、解析器、历史数据库、Mobile flavor UI、主页或历史页文件。

### 根层复用与两种布局

- 构造门控在 `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10281`：仅 Util.isLeanback() && modeController.isFusionMode()。同一 helper 只创建一次，沿用 binding.root / playerPanel / playerPanelSpacer / scroll。
- `app/src/main/java/com/fongmi/android/tv/ui/detail/FusionFloatingPanel.java:83` / `app/src/main/java/com/fongmi/android/tv/ui/detail/FusionFloatingPanel.java:143`：只快照、修改和恢复既有卡片的参数、位移、圆角、Z 和焦点属性；没有第二个 PlayerView，没有 reparent，没有 Service/媒体/解析调用。新增 TextView 是移动/拖动命中代理，不承载视频。
- 默认右上角，宽度取安全区域约 24%，以 180–480dp 和可用尺寸约束，16:9，并适配高度不足场景。小窗圆角设为 0，优先保持 SurfaceView 画面完整；退出后恢复原卡片圆角，未强制改 Surface 类型。
- `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:2082` / `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10301` 增加局部兼容：现有模板重绑不会覆盖小窗的边距/位置。原 spacer 保持原尺寸和流式占位。
- 不在同一状态下重建布局或刷新媒体；同一主题/焦点状态复用状态 Drawable。只有尺寸/边距确实变化才 setLayoutParams，移动只改 translation，无动画队列/逐帧引擎/第二滚动系统。

### 可见度、暂停与加载

- `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10387` 保留既有唯一滚动/布局同步路径，先评估 `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10414`，不另注册第二个 scroll listener。
- `app/src/main/java/com/fongmi/android/tv/ui/detail/FusionFloatingPanel.java:63` 计算 NestedScrollView 的真实可见矩形，扣除其 padding，结合根视图系统栏/刘海 Insets 与 12dp 安全留白；当前 Leanback headerBar 本就隐藏，不虚构额外固定顶部栏。
- 原 spacer 基本滚到视口上方、可见高度比例≤5%时进入；可见比例≥30%时恢复。始终用原 spacer，而非小窗位置。初始位于视口下方不触发；单次方向键、焦点离开或 scrollY>0 均非条件。
- 非 Fusion、无已建立视频、错误首播、全屏、PiP、退出/销毁状态不会新建小窗。用户暂停不以 isPlaying=false 退出；换集缓冲和换源重载保留已建立的小窗。
- 移动入口正在获焦、移动或拖动时不撤销用户当前操作；操作退出后继续按 spacer 判断。原生错误 UI/提示继续保留，不新增错误重试或绕过原播放链。

### 遥控器与局部鼠标操作

- `app/src/main/java/com/fongmi/android/tv/ui/detail/FusionFloatingPanel.java:165`：普通右键先保留同一可见横排的原生邻居；到横排末端、回绕或无合适邻居时进入小窗“移动”代理。实际 Activity 的 dispatchKeyEvent 已自动化验证该入口可到达。
- 代理获焦不自动进入移动；确定后才进入。四方向按16dp步长移动、支持系统按键重复；确定吸附并保存，返回恢复移动前位置。非移动态左/上/下返回最近的有效详情控件；右键保持代理焦点，避免长按来回跳转。
- 普通小窗态 block 原播放器控制子树，暂时禁用 spacer 可聚焦性，保留占位；退出时还原。原桥接函数也检查小窗状态，防止把焦点转向小窗播放器。
- 焦点记忆排除滚动容器、占位和不可见节点；读取 Window 焦点缺失时回退同一 root 的原生焦点图。没有自动恢复到顶部或主动把详情焦点移进小窗。
- 弹窗仍由原 Window 接收按键；原 LUT 快捷层优先于移动处理。返回流程保留原手势/锁定/控制层优先级，再处理移动取消，最后走原详情返回/全屏逻辑。失去窗口焦点时取消未确认的移动。
- `app/src/main/java/com/fongmi/android/tv/ui/detail/FusionFloatingPanel.java:198` 仅绑定小窗代理矩形；处理 pointer 身份、touch slop、DOWN/MOVE/UP/CANCEL，多指进入时取消。点击进入移动，拖动松开吸附并保存，取消恢复原位。没有全局 Activity 触摸拦截，也不会把拖动发给原播放按钮。

### 四边吸附与位置记忆

`app/src/main/java/com/fongmi/android/tv/ui/detail/FusionFloatingLayout.java:33` 支持左/右/上/下最近边缘，沿边方向坐标自由，不限四角。`app/src/main/java/com/fongmi/android/tv/ui/detail/FusionFloatingLayout.java:42` 保存可移动区间的相对 x/y。

复用既有 Prefers，键 tmdb_fusion_float_x / tmdb_fusion_float_y。没有记录时(1,0)为右上；只在用户确认或拖动完成写入，自动进入/恢复、换集和换影片不覆盖记录。根尺寸或 Insets 改变后重新计算安全位置；移动未完成时尺寸变化会取消该次操作。

### 全屏互斥与生命周期

- `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10139` 先暂停页内小窗展示，使用原铺满根层/控件/方向流程，未新建视频显示面。
- `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10485`：来自小窗时跳过原 scrollTo(0,0)，按 spacer 再评估；若仍在换集/换源加载，保留此前已经建立的小窗状态。回到详情焦点仅属于主动全屏操作，不用于自动布局恢复。
- 恢复前先重建正常卡片的圆角和 Z 基线，避免把全屏的 0 圆角/高 Z 保存成将来内嵌恢复值。回归覆盖“浮动→全屏→浮动→内嵌”的位置、父容器、圆角及 Z。
- Mobile、非 Fusion 的正常全屏跳顶/焦点行为保持原语义；系统 PiP 不新增入口、不改 Manifest。
- `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:11401` 只补充移除移动代理及 focus listener；原历史保存、字幕会话、播放所有权、Service 绑定/释放、后台策略不变。不实现跨 Activity 小窗。

### 手动选集、连播、线路和换源证据

| 原路径 | 接入边界 |
| --- | --- |
| `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:6631`、renderFlagSelection 的选集确认 | 原 onPlay → modeController.play → playInline → startInlinePlayer 不变；小窗起播完毕不再强制聚焦 playerPanel。 |
| `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10619` | 手动上下集、自动 ended/NavigationCallback.next 继续汇聚到该函数与 onPlay，不另建队列。 |
| `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:7711` | 原 preparePlayer、字幕恢复、音轨/速度、startPlayer 和历史进度窗口保留；只为最终 requestFocus 加小窗条件。 |
| `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:754`、switchSourceDetail | 原当前 Activity setIntent/reset/loadContent，不新开详情。以 fusionFloatingReload 保留小窗，READY 后释放加载保留；模板重绑只修复展示参数。 |
| changeInlineParse、changeInlineQuality 等资源线路 | 继续原解析或 startInlinePlayer，未改资源决策；缓冲不退小窗。 |
| NovelRouter / 外部播放器 / 全局搜索 | 原逻辑允许离开此 Activity。小窗不跨页跟随，不为维持布局强行改成内嵌播放；属于明确的原业务边界。 |

静态前后对照：SiteApi.playerContent 调用点 2→2，player.preparePlayer 1→1，stop/clear 各4→4，startPlayer 1→1；没有新增 setRender 或 reattachVideoSurfaceAfterReparent 调用。证据在外部 playback-call-counts.json。**这些是源码调用点数量，不是实测网络/启动次数。** 正常换集/换源仍应按原业务解析新资源及准备媒体，不能声称这些操作永不重建底层引擎。

### Surface 与连续性证据分级

| 项目 | 实际证据 |
| --- | --- |
| 同一 PlayerView / 同一父容器 / 同一根层 panel | PASS：真实 View 及 Activity Robolectric 测试对象身份保持，布局切换与全屏往返均覆盖。 |
| 不额外解析、prepare、setPlayer(null)、setRender、detach、reparent | PASS（静态）：展示辅助类及新入口不调用这些函数，关键原业务调用点数量不变。 |
| PlayerManager 与底层 Player 的运行身份 | NOT TESTED：没有真实解码播放会话；源码结构复用原 Service/manager，不据此冒充实测。 |
| Surface created/changed/destroyed 与闪黑 | NOT TESTED：控制器关闭的测试 PlayerView 使用 surface_type=none，不能证明硬件 Surface 连续。 |
| 进度、字幕、音轨、丢帧、声音连续 | NOT TESTED：未运行实际资源及电视解码。 |

`app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java:10432` 复用现有 SpiderDebug/playback-lifecycle 日志及 trace，状态切换时记录 manager/player/view/surface 对象身份、进度和焦点。既有 PlaybackActivity 已绑定 SurfaceDiagnosticCollector，继续记录 surface.created/changed/destroyed，不再创建第二套 collector。

真机应使用原诊断开关（PlaybackDiagnosticCollector.enabled 走 DebugLogStore）及已有播放 trace，比较 INLINE→FLOATING→INLINE 的事件、画面、进度和丢帧；允许正常 resize 的 surface.changed，但 created/destroyed、闪黑或音画中断必须结合实际 trace 调查。未声称零重建、零缓冲、零掉帧或具体改善幅度。

### 自动化、构建与产物

- Leanback：**162 tests，0 failure/error，0 skipped**。包括12项小窗执行用例，及响应式布局、原控制器分派、主页/历史页相关回归。
- Mobile：**24 tests，0 failure/error，0 skipped**。包含小窗门控、共享纯几何、PipExitDecision 和 DetailModeController 回归。不等于手机真机测试。
- PASS：完整 :app:assembleLeanbackArm64_v8aDebug，APK 已签名。未构建 Release 或 Mobile APK，未安装、未推送。
- 调试中修复了加载中全屏退出的布局保留、Window/root 焦点暴露差异、错误恢复容器焦点、模板重绑边距，以及全屏后的卡片样式快照问题。测试场景明确先让可见详情控件获焦，再模拟小窗和全屏；没有用延迟滚回掩盖焦点滚动。
- minSdk=24；新增 setDefaultFocusHighlightEnabled 按 API≥26 保护，避免 Android 7 调用新方法；已加入静态门控断言，未执行 API24 真机测试。
- 原 Activity 保持施工前的 LF 行尾；未整文件格式化。

```powershell
./gradlew.bat :app:testLeanbackArm64_v8aDebugUnitTest --tests '*FusionFloating*' --tests '*TmdbDetailResponsiveLayoutTest' --tests '*TmdbDetailChipFillTest' --tests '*VideoActivityDetailShellSourceTest' --tests '*NativeHistory*' --tests '*NativeHome*' :app:testMobileArm64_v8aDebugUnitTest --tests '*FusionFloating*' --tests '*PipExitDecisionTest' --tests '*DetailModeControllerTest' :app:assembleLeanbackArm64_v8aDebug -PchaquopyBuildPython=E:/DevTools/python-3.10/python.exe --offline --no-daemon --console=plain
```

APK 完整路径：`E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`

- 大小：**209,261,474 bytes**。
- SHA-256：`30aeb06f6cb4869350bf52ab427459ac229bf7452c6b07a88aa39ffda0b81371`。
- ABI：`arm64-v8a`。
- 签名/Manifest 最终检查见外部 apk-signature.txt / apk-manifest.xml：Debug v2、包名 com.silent.android.webhtv、原 HomeActivityCurrent Launcher alias 指向 HomeActivity；VideoActivity/TmdbDetailActivity 未新增 supportsPictureInPicture。
- Debug 证书 SHA-256：`97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`。

### 工作区保护

施工前后 HEAD：`030485b70c263621fe4575abe5758b28115832c3`，未切分支。初始304 dirty/5,254已有文件；结束312 dirty。仅1个授权既有文件发生增量，其余5,253个原文件 SHA-256 不变，无文件缺失；8个新文件全部在本轮9条声明路径内。303条范围外初始 dirty 由 task guard 保护，最终范围检查 PASS。

WebHome 114 文件、WOGG 106 文件及其 HEAD/status 不变；Suite gitlink 不变。原主页、历史、实验室、Mihomo、第二字幕、播放器依赖和其他全部未提交成果保留。未 commit/push/tag、未更新stable、未 reset/clean/stash/checkout、未创建分支/worktree。

外部证据目录：`C:\Users\ADMINI~1\AppData\Local\Temp\tmdb-detail-02-20261027-160917`，含 before.json、originals、activity-incremental.diff、guard日志、protection-after.json、各次测试诊断及最终 build-test.log、final-results.json、APK 签名和Manifest。

### 电视验收清单（全部 NOT TESTED）

1. 有效视频向下浏览，播放器仍可见时不悬浮，基本离屏后浮动；临界往返不抖动；暂停仍保留布局；无视频/首播错误不错误进入。
2. 小窗时继续选集、自动下一集、换线路及换源；缓冲/失败仍按原业务处理，详情焦点不被起播回调抢走。
3. 当前横排末端右键进入“移动”；确定开始、四向连续移动、确定保存、返回取消；退出移动后详情方向键及返回正常。
4. 物理鼠标/空鼠拖动，松手四边吸附，取消还原；不能误暂停或点进原播放控件。自动化仅覆盖合成 MotionEvent，不包含实际空鼠设备。
5. 换影片/重新进入恢复位置，1080P/4K 与不同安全区下不越界；移动中改变尺寸安全取消。
6. 主动全屏往返保持原机制；回到仍在浏览的位置继续浮动，向上回观看区域恢复居中尺寸和圆角/Z，页面不跳顶。
7. SurfaceView、TextureView、硬件解码、HDR/LUT、字幕/ASS/弹幕、音轨、进度和音画连续性；用既有 trace 检查 Surface 事件及资源解析/启动次数。
8. 原弹窗/控制层/返回/生命周期、其他详情模式、原主页及历史页、Mobile/PiP 真机无回退。

本轮停止，等待电视真机反馈。不自行进入下一阶段。
