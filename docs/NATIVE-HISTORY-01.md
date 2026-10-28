# NATIVE-HISTORY-01

## 目标、范围与施工基线

在现有 HistoryActivity 内增加 Leanback Debug 沉浸展示，经典布局不替换；继续播放按钮及进度组合参照用户电视照片和只读 WebHome 实现。History.getForDisplay、HistoryResumeCoordinator、删除范围和观影报告仍为唯一业务来源。禁止提交、推送和 Suite pointer 更新。

2026-10-27 14:18 +08:00 基线：HEAD `030485b70c263621fe4575abe5758b28115832c3`，291 dirty、5241 原有文件；仓库外 `%TEMP%/native-history-01-20261027-141817` 保存逐文件 SHA-256 和目标原件。guard 已启动，保护 290 其他 dirty 路径，显式采用当前 HistoryActivity 原有未提交内容后最小补丁。

范围：HistoryActivity、新增 NativeHistoryBrowseView / NativeHistoryPosterAdapter / NativeHistoryArtwork、三份相关 Leanback 测试、本报告；用户将按钮改为「查看详情」后，补充三份 Leanback 本地化资源并更新同任务 guard 范围。其余原有文件保护不变。

## 施工前审计与选择

- `HistoryActivity.getHistory` 经 `History.getForDisplay()` 取得聚合/全局/排序后的记录；`HistoryAdapter` 的 differ 保存列表、删除状态，原页负责删除确认与焦点恢复。
- 卡片点击 `HistoryResumeCoordinator.open` 已处理原站源、跨源恢复、小说/视频分派，UI 不重写。
- `History.getDisplayIdentity()` / equals 为焦点身份；不重新定义持久化身份。
- `History` 已有 wallPic、集数、position/duration、TMDB 身份及年份类型；观看状态必须保持该对象，不由 TMDB 覆盖。
- 原有 `HistoryAdapter.clear()` 调用 `History.deleteForDisplay()`；`clear(Runnable)` 来自 BaseDiffAdapter 只清 UI。新沉浸路径调用现有无参 clear 再读取，保证既有删除范围，不修改经典路径或数据库。
- `HomePreviewGeometry`、`HomePreviewPosterAdapter.Holder`、`HomePreviewStyles.function/functionFocus`、`HomeHeroTitleView`、`NativeHomeBrowseImages`、`NativeHomeHero` / known-only backend、`NativeHomePreparationQueue`、Activity 原生 CustomWallView 可直接复用。不修改已验收主页文件。

## 视觉与交互依据（2026-10-27）

用户照片：继续播放胶囊左置，旁边集数与时间同一行，细进度条在标签下方；底部单海报轨道；没有整块不透明 Hero 面板。

用户补充：背景兜底与主页一致，使用右上竖版卡片封面。直接复用 NativeHomeBrowseImages 的加载/失败/实际绘制交接，圆角 8dp、2:3；右上尺寸与位置采用主页现有 `.39h / max300dp`、右侧8%、顶部14.5% 规则。

用户最新纠正优先：进入历史页（含详情/播放返回）焦点始终落在首卡，不保存或恢复上次历史焦点。仅本页内刷新、删除采用稳定身份或邻近位置保焦；没有复制主页“Back 回首卡”规则。沉浸模式在 onResume 读取同一原生历史列表并标记本次首卡进入，避免恢复旧卡片；经典模式不改。

用户后续视觉精简：移除海报卡片内进度条和播放时间，避免与独立续播区重复；卡片只保留片名、集数、来源。原始 History 的 position/duration 完整保留，继续播放区仍展示真实时间及进度。

用户最新功能调整：胶囊按钮改为「查看详情」，不再调用续播协调器；可用原站源调用既有普通 VideoActivity.start，遵循既有内容分派及详情模式，不携带 resume_from_history / exact 续播参数。失效/其他配置站源不能错误借用当前同名站源，转既有普通搜索入口供选源，不强行自动续播或切配置。海报点击仍为 HistoryResumeCoordinator.open 原生续播。为准确文案补充三种语言的 Leanback 历史页资源，并在更新后的同任务 guard 中声明。

主题无独立设计/配置：所有色彩取现有全局 ThemeController/ThemeTokens；删除/报告使用已有 HomePreviewStyles.function/functionFocus；胶囊使用已有 control 状态配色，仅形状适配为圆角胶囊。海报/背景/Logo 不染主题色。

本地 WebHome `src/css/05-home-tv.css:1209-1217`、`src/js/11e-tv-browse.js:853-887`，HEAD `05018ca8d68ff536c39cba2ff55e404f9d440c08`：resume 为横向组合、按钮胶囊、标签左右排列、细进度条、未知时长不伪造百分比。只借鉴布局，原生颜色使用 ThemeController token，续播使用原生入口，不复制 DOM/JS 执行机制。

官方 [Android 方向键导航](https://developer.android.com/develop/ui/views/touch-and-input/keyboard-input/navigation) 支持在系统默认焦点关系不正确时显式指定跨区关系；本轮仅协调顶部/续播/轨道，左右滚动全部由 HorizontalGridView 管理。官方 HorizontalGridView 文档抓取超时，以本地已验证组件及测试为直接依据。没有升级依赖/合并上游，相关 PR、论文及上游迁移方案不适用。

不改原状无法实现目标；复制完整历史业务或整套主页会形成双权威，不采用。选择现有 Activity 按模式分流，保留旧 XML 和所有原业务方法，只加展示适配。回退只撤销本轮声明补丁/新增文件，不能恢复 HEAD 覆盖既有修改。

## 最终实现

### 双模式与业务入口

`HistoryActivity.getBinding()` 只在现有 `Setting.getLeanbackHomeMode()==IMMERSIVE` 时实例化新展示；否则继续原有 ActivityHistoryBinding / GridLayoutManager / HistoryAdapter。模式仍由现有 Debug+Leanback 门控决定，Release/Mobile 不获得新开关。没有增加 Activity、桌面图标、数据库、Repository 或样式偏好。

`History.getForDisplay()` 和原有 HistoryAdapter 是数据/管理来源；沉浸 Adapter 只展示传入的 History 引用，不读库、不排序、不聚合。`getHistory()` 的原结果提交到展示层；`onRefreshEvent(HISTORY)`、`onConfigEvent` 仍经原 Activity。

海报点击为原有 `HistoryResumeCoordinator.open(this,item)`；长按仍切换原有 delete 状态；单删调用 `item.deleteDisplayItem()` 后原 adapter.remove；清空经原确认弹窗、原 adapter.clear() / History.deleteForDisplay 范围执行；观影报告仍调用 ViewingReportRangeDialog → ViewingReportActivity。经典路径的 XML、Adapter/Presenter、续播、确认内容、清空分支和返回分支不替换。

「查看详情」有独立 UI 回调，但没有独立业务引擎：原源可用时调用普通 `VideoActivity.start`，不携带历史 exact/resume 参数；失效/其他配置原源走既有普通 SearchActivity，避免把旧记录错误套到当前配置。海报仍支持原有全局/跨源续播。因此，原源失效时按钮需要先选有效来源，不能承诺直接打开不存在的原源详情。

### 布局、焦点与进度

NativeHistoryBrowseView 为一个透明原生 FrameLayout：全屏左右/底部渐变、固定 Hero 槽、顶部历史标题与两个管理按钮、独立「查看详情＋集数/时间/细进度条」、单 HorizontalGridView。没有 WebView。

卡片直接复用 HomePreviewPosterAdapter.Holder 和 HomePreviewGeometry：2:3、原圆角、120ms/1.04 缩放及主题焦点描边，不改变布局尺寸。按用户最新反馈，**卡片没有进度条或时间文本**；仅保留片名、集数、来源。观看进度只在 Hero 下方独立区域显示。已知时长按原生毫秒夹紧到 0–100%；未知时长显示 `--:--` 并隐藏条，不伪造百分比。

HorizontalGridView 是唯一横向选择和滚动权威，沿用 BOTH_EDGE、居中 keyline、原生平滑滚动及边缘约束。只协调「顶部按钮 ↔ 详情按钮 ↔ 轨道」上下导航，左右边界不跳出轨道；没有 scrollBy、第二套 SmoothScroller 或逐帧滚动引擎。进入/返回页面都首卡；只在本页内删除/刷新时按原 displayIdentity 保焦或选邻近记录。Back 沿用 HistoryActivity：先退出删除模式，再返回，不复制主页 Back 回首卡。空列表清 Hero/背景/进度并禁用管理，焦点可到报告。

### 背景和 Hero 复用

复用 BaseActivity 已有 CustomWallView，不创建另一个横图宿主；NativeHomeBrowseImages 负责当前影片图片订阅、Glide、160ms 过渡与实际绘制确认。优先已有可信 wallPic；没有横图、失败或等待时右上 2:3 圆角封面持续兜底，真正绘制横图后才隐藏。相同焦点身份不反复清图/加载，过期 UI 回调有 artIdentity 与既有 generation 双重隔离。

NativeHistoryArtwork 是对 NativeHomeHero known-only 通道的薄适配；不调用 searchAndMatch、不创建 TMDB 新客户端或持久缓存。已有 History TMDB 身份转换为现有 payload；无身份仅查询现有绑定，不能对全历史搜索匹配。跨配置记录使用隔离展示站源键，避免借用当前配置同名站源的绑定。所有增强工作仍经现有 executor/service/cache。元数据不修改传入 History 的 position、duration、集数或身份。

NativeHomePreparationQueue 复用既有限额/间隔，在可见窗口及邻近方向范围内准备已知背景；同源窗口受原 MAX_WINDOW=10 上限、单元数据槽和单图片槽约束，已有可信 wallPic 可直接准备，不重新匹配全部历史。离开页面暂停视觉订阅，销毁释放 wall lease 与 Glide。

标题直接使用 NATIVE-HOME-10 HomeHeroTitleView.bindTitle：同片同文本幂等，换片重置，Logo 优先，页面隐藏停跑马灯。没有新标题动画。主题只使用全局 ThemeTokens：管理按钮精确调用 HomePreviewStyles.function/functionFocus；主题不变时不重绑轨道。背景/海报/Logo 标记内容素材，不染色。安全区沿用真实 system/cutout inset，只移动内容，背景和遮罩全屏，不添加固定手机挖孔边。

## 文件清单

修改唯一原有文件：

- `app/src/leanback/java/com/fongmi/android/tv/ui/activity/HistoryActivity.java`：模式分流、展示回调、首卡进入、生命周期释放、普通详情入口。

新增：

- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHistoryBrowseView.java`：页面原生布局、独立进度区、区域焦点和安全区。
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHistoryPosterAdapter.java`：复用主页卡片的 History 展示和删除状态。
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHistoryArtwork.java`：历史对象到既有 known-only Hero/预加载的展示适配。
- `app/src/leanback/res/values/native_history.xml`、`values-zh-rCN/native_history.xml`、`values-zh-rTW/native_history.xml`：查看详情本地化文案。
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHistoryBrowseTest.java`：布局、数据回调、首卡进入、删除、进度、边界与快照。
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHistoryArtworkTest.java`：known-only、不改历史状态、实际原生绘制/封面交接。
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/activity/NativeHistoryModeTest.java`：模式与 Debug/Mobile 隔离、普通详情无续播标记、失效原源不误分派。
- `docs/NATIVE-HISTORY-01.md`：本报告。

## 自动化验证

PASS：历史业务/既有主页/新历史定向回归 **237/237**；用户最后改为查看详情后，新历史相关重新执行 **15/15**（含原 GlobalHistoryResumeSourceTest），0 失败；Mobile 历史 Adapter、首页配置恢复、主题定向回归 **9/9**。这些是分阶段执行结果，不相加冒充全量测试。

覆盖：原 displayIdentity、原对象传递、删除及空列表、首卡重入、顶部/详情/轨道方向导航、详情与续播分派隔离、未知/越界进度、卡片无重复进度、全屏 shade 与实际 inset、known-only 元数据、竖版失败保留、native wall 真正绘制再隐藏封面、旧主页和历史/跨源/删除范围既有用例。

已导出并检查 1920×1080 原生 Robolectric 位图：`%TEMP%/native-history-01-20261027-141817/native-history-layout.png`，仅测试本地素材和测试历史；未接入生产数据，不冒充电视截图。最终快照已是「查看详情」，卡片无进度条和时间。全局浅色主题下展示浅色遮罩/深色文字，非固定深色方案。

开发过程中修正了本地 Leanback 不支持的焦点 setter、程序化复用标题时的初始空缓冲、原生绘制夹具模式/窗口尺寸和 Robolectric 重载编译问题；最终所有上述定向测试均通过，没有通过删除失败断言或修改既有业务测试绕过问题。

## 构建与 APK

完整任务 `:app:assembleLeanbackArm64_v8aDebug`：PASS。另处理 Release Manifest，未构建 Release APK。

- APK：`E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`
- 大小：**209260154 bytes**。
- SHA-256：`579fbc286aa031bd32ab3c8fc699c7fc87e3dd425564af38f97a34182d5bf41c`。
- applicationId：`com.silent.android.webhtv`，debuggable=true，ABI 仅 arm64-v8a。
- apksigner：PASS，APK v2，Debug 证书 SHA-256 `97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`。
- Manifest 保留原 HistoryActivity，未新增历史 Activity；默认 enabled Launcher 仍为 HomeActivityCurrent → HomeActivity；Debug 预览 Activity 不在 Release Manifest。
- 模式入口继续调用既有 Setting 门控，Release/非 Leanback 即便保存 IMMERSIVE 仍解析为 CLASSIC，测试通过。

## 工作区保护

HEAD 前后一致：`030485b70c263621fe4575abe5758b28115832c3`。5241 个原有文件中仅声明的 HistoryActivity 有本轮最小变化，**其他 5240 个逐内容一致，无丢失**；10 个本轮新增文件，dirty 291→301。

WebHome 114 文件、WOGG 106 文件及各自 HEAD/status 全部一致；Suite gitlinks 一致。NATIVE-HOME-01～10、TMDB-DETAIL-01、历史模型/续播协调器、Mobile、播放器等原有成果未修改。限定原有文件 diff --check 通过。更新范围后的 guard 终检 **PASS**：branch/HEAD、protected dirty、scope、staged paths 安全。

不 commit、不 push、不建分支/worktree、不更新 Suite；未安装设备、未操作电视数据。

## 电视真机待验证（NOT TESTED）

- 真实数据库历史列表、全局/跨源续播、观看返回后的最新进度；普通详情受各详情模式配置的最终设备行为。
- 首次与从播放/详情/报告返回均首卡；实际遥控器长按、快速连续左右和轨道首尾。
- 1080P/4K 实际安全区、卡片裁切、艺术 Logo/长标题持续帧表现。
- 真实壁纸/横图慢请求、缺图/失败、A→B→A、右上封面交接。
- 全局浅色/深色、自定义主题色的电视可读性和聚焦反馈。
- 真正执行单删、全部清空确认、全局删除范围及观影报告弹窗焦点。
- 大量真实历史及图片缓存冷/热情况下的帧率与按键延迟；没有实测电视毫秒或帧率，不宣称原生流畅度已真机通过。

功能接入已完成；原源失效时查看详情使用普通搜索选源是明确回退边界，其余上述为真实设备验收门槛。

## Recovery anchor

代码、最新用户反馈、定向测试、Debug APK/签名检查、逐文件保护与 guard 终检全部完成；停止施工，交付用户电视验收。无下一阶段施工。
