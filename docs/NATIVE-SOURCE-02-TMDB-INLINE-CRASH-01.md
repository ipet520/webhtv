# NATIVE-SOURCE-02＋TMDB-INLINE-CRASH-01

## 范围与基线

2026-10-28，App main / d48e68823ae283fd9c7286ea221b0ef75a620eed。不提交、不推送、不更新 Suite gitlink。任务 A/B 分别定位和测试，最终联合构建。C56、NATIVE-SOURCE-01 原有差异及归档 APK 受保护。

## A：TMDB 内联状态查询崩溃

用户日志确认空对象 getPlaybackState 调用；本地直接来源是 PlaybackActivity.mController（MediaController），不是 PlayerManager 或底层解码器。PlaybackActivity.onServiceConnected 先赋 mService，再 buildControllerAsync；handleControllerConnected 才通过 Future 赋 mController。releaseController 也会清空它。TmdbDetailActivity.updateInlineLoading 原先仅以 player()!=null 判断，服务/PlayerManager 存在并不保证 MediaController 已连接。由此可以形成日志中的空引用窗口。具体真机是初始化、断开或释放阶段：UNVERIFIED，不能归因于 C56、换源或解码器。

最小修复只在 PlaybackActivity 内收敛状态读取：捕获同一个 controller 局部引用；为空按 STATE_IDLE / 未就绪处理。isIdle=true，isEnded/isBuffering/isPaused=false；isPaused 使用单次快照。非空时仍按原四种状态判断，保留旧 isPaused 的实际语义（非 IDLE、非 BUFFERING），没有重新定义为 !isPlaying。没有 try/catch 吞异常、固定等待、重建播放器或改变快速全屏。updateInlineLoading 的 pending / manager loading 规则未改；无控制器并不被判断为暂停/缓冲完成。

未修改 TmdbDetailActivity、FusionDetailController、FusionFloatingPanel、Service、Media3、资源解析、字幕、音轨或播放进度逻辑。不会在全屏切换时创建/释放实例。自动测试范围与真机限制在最终结果中列明。

## B：换源焦点最短路径

打开默认真实焦点为分组选择器。原实际选中源的视觉独立保留，利用原 ScrollView 可见性机制定位，不新增滚动引擎。下键首次进当前分组中的实际源；没有则第一条，无结果则留在分组。

站源 Back → 分组，分组 Back → 关闭。浏览位置通过站源 key 记忆：返回分组后，下键恢复仍有效的浏览条目；筛选重建后只保留结果中仍有效的 key，失效则退回实际源/第一条。浏览不会切源，不改变实际选中标记。分组确认后回到选择器，不自动切源。

普通选择器、列表及展开下拉的左右 DOWN 直接关闭；Dialog 及主页配对消费重复键和 UP，避免关闭后穿透。新 DOWN repeat=0 清理旧手势，缺失 UP 不影响下一新按键。返回 DOWN/repeat/UP 全部消费，仅在有效 UP 执行一次。下拉 Back 只收起，排序 Back 只取消；排序期间左右不能抢占。

NATIVE-SOURCE-01 的布局、无标题、宽度、颜色、胶囊、健康点、原生分组排序/持久化和真实切源业务保持。顶部和卡片焦点系统不改。

## 修改范围

- app/src/main/java/com/fongmi/android/tv/ui/activity/PlaybackActivity.java：状态查询空控制器保护。
- app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewMenuDialog.java：分组/列表按键路径和浏览身份恢复。
- app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java：消费关闭手势尾部，不穿透主页。
- app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeSourceDrawerTest.java：实际 Dialog 焦点矩阵与浏览身份回归。
- app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeDrawerTest.java：关闭恢复及事件尾部回归。
- app/src/testLeanback/java/com/fongmi/android/tv/ui/activity/PlaybackControllerStateTest.java：空、连接、四状态、释放重连状态测试。
- app/src/testMobile/java/com/fongmi/android/tv/ui/activity/PlaybackControllerStateTest.java：同一共享修复在 Mobile 变体回归。
- docs/NATIVE-SOURCE-02-FOCUS-REPORT.md：原焦点任务报告。
- 本报告。

## 执行记录

首轮 combined 测试命令只给最后一个 Gradle 任务附加 --tests，导致 Leanback 误跑全套；在发现无关 Mihomo SSL/JDK 模块访问失败后停止，不能算本轮定向结果。已改为每个任务分别指定过滤器。历史 C56 全套已有失败，不宣称全量绿色。

## 真机待验证

NOT TESTED：TCL Android14 初始化阶段快速确定进入全屏；正常 Exo/其他引擎真实解码；反复进出全屏与浮窗、字幕/音轨/进度连续；电视长按/快速左右和抽屉关闭焦点。当前 ADB 仅有 offline 手机，无可用 TCL。自动状态及布局测试不能替代上述设备验收。

## 联合交付结果

### 源码证据位置

- PlaybackActivity.java:253–275：统一读取及四项状态语义。
- PlaybackActivity.java:620–637：异步 controller 成功绑定。
- PlaybackActivity.java:893–900：释放清空 controller。
- PlaybackActivity.java:1198–1219：先连接 Service，再异步建立 controller。
- TmdbDetailActivity.java:8406–8425：updateInlineDisplayPanel → updateInlineLoading 调用链（本轮未改）。
- HomePreviewMenuDialog.java:155、173–210、237、360–409：默认焦点、浏览 key、可见定位、分组确认及按键消费。
- NativeHomeBrowseView.java:424、522–528：普通关闭手势尾部消费。

### 自动化与构建

| 范围 | 结果 | 边界 |
|---|---|---|
| Leanback 影响回归 | PASS：232 tests / 40 suites，0 failure/error/skip | 合并上一轮未失败的影响回归与本轮最终定向结果；不是全套测试 |
| Mobile 共享回归 | PASS：25 tests / 5 suites，0 failure/error/skip | 状态、控制器 READY 对齐、播放来源契约、音频对齐、浮窗隔离 |
| NativeSourceDrawerTest | PASS：27 | 真实 Dialog 测量/焦点、分组筛选、健康排序、Back/左右、排序保护、浏览恢复 |
| NativeHomeDrawerTest | PASS：6 | 关闭后主页焦点、长按事件尾部、新手势不被旧状态吞掉 |
| PlaybackControllerStateTest | PASS：Leanback 4 / Mobile 3 | null、四种状态单次读取、释放重连；controller 用 Robolectric shadow |
| 初始化前重复全屏 | PASS：Robolectric 实际详情布局重复 5 轮进出，重复 enter 幂等、同一 PlayerView/父容器 | 没有实际 Service/解码，不能等同 TCL 快速确定键实测 |
| FusionFloating 集成与布局 | PASS（包含在232内） | 自动悬浮/全屏互斥、容器/滚动和渲染视图保留；真实 Surface/音轨待设备 |
| Leanback ARM64 Debug＋Release | PASS：单次最终联合构建，2m35s，346 tasks | fastRelease=false，执行 R8、资源收缩及优化、Release lint |
| 任务范围 git diff --check | PASS | 原有其他差异未清理 |
| TCL / Exo / MPV 解码、字幕音轨进度连续 | NOT TESTED | 当前 ADB 手机 offline，无在线 TCL |

影响回归首轮为231项、1条旧焦点断言失败；按本次用户新增浏览恢复语义更新该断言后，对相应两套测试重跑，并加入一项全屏进出测试；最终合并结果232项全部通过。全部原始日志保留，不隐去失败尝试。

### APK 身份

两包 applicationId=com.silent.android.webhtv，versionCode=560，versionName=5.6.0。仅 arm64-v8a、30个 AArch64 ELF；Manifest 的已启用 Launcher 为 HomeActivityCurrent → HomeActivity，历史 alias 仍禁用。

签名证书 SHA-256：97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5，与既有个人 Debug 签名一致。Release 是实际 leanbackArm64_v8aRelease，debuggable=false；Debug=true。没有重命名 Debug 冒充 Release。



**DEBUG**

路径：`E:\GitHub\webhtv-suite\app\Release\apk\NATIVE-SOURCE-02-TMDB-INLINE-CRASH-01-20261028\leanback-debug-arm64-native-source02-inline-crash01-20261028.apk`

大小：209,262,254 bytes（199.57 MiB）

SHA-256：`41c1c0a09123968507794cc908020fd0ebaaf82c9aa2a2a6c3adf2fa0ff1c6c9`

**RELEASE**

路径：`E:\GitHub\webhtv-suite\app\Release\apk\NATIVE-SOURCE-02-TMDB-INLINE-CRASH-01-20261028\leanback-release-arm64-native-source02-inline-crash01-20261028.apk`

大小：174,109,786 bytes（166.04 MiB）

SHA-256：`7fc6cb7497790ff5f19f99b16c5ad8feb1f1a1a765c7f518e8fd43acb44f2832`

### 既有成果保护

- 前后 App HEAD 都是 d48e68823ae283fd9c7286ea221b0ef75a620eed，main；无提交/推送/tag，index未改。
- 初始66条dirty，最终71条；本轮改5个既有文件，新增2份测试及2份报告。原有 C56 / Source01 文件没有被 HEAD 版本替换。
- 5286个原文件逐路径 SHA 核对，仅授权的5条现有文件内容改变：PlaybackActivity、HomePreviewMenuDialog、NativeHomeBrowseView及两份既有焦点测试。
- Source01 的11个窗口、主题、胶囊、健康和分组排序核心方法与施工前逐方法一致。C56 原有脏路径内容没有被修改；尤其原有 dirty TmdbDetailActivity 未改。
- 2038个 C56 / Source01 归档文件全部 SHA不变。旧APK目录没有覆盖。
- WebHome114个、WOGG106个原文件 SHA不变，HEAD/状态不变；Suite gitlink未改。
- 独立 task guard check PASS，保护62个未接纳的既有 dirty 路径。未执行 finish/commit/tag。
- 快照与原始日志：`%TEMP%/native-source-02-20261028-185647`（原焦点任务）及 `%TEMP%/native-source02-inline-crash01`（合并任务）。交付目录 evidence 保留测试、构建、APK校验、保护结果和增量补丁。

### 电视复核顺序

1. TCL Android14：播放初始化时快速连续确定；进入/退出全屏后再进入；确认不崩溃。
2. 实际 Exo/其他引擎：内嵌/浮窗/全屏往返，确认画面、字幕、音轨及进度连续。两种正常播放状态语义未改，但不能用 shadow 测试证明实际解码。
3. 抽屉初始分组 → 下到当前源；浏览另一源 → Back到分组 → 下回刚浏览源；实际选中视觉仍在原源。
4. 分组筛选、长列表和健康排序；Back层级；左右快捷关闭及长按事件不穿透；排序时返回只取消排序。
5. 经典主页、沉浸主页、WebHome和菜单键既有行为复核。

完成本轮联合交付后停止，等待真机复核；不启动其他任务。
