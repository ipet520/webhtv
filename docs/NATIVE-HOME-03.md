# NATIVE-HOME-03 — visual / interaction correction

## Scope / recovery

2026-10-26, initial HEAD `030485b70c263621fe4575abe5758b28115832c3`, 240 initial dirty paths, 5,196 existing App files. Full SHA-256 snapshot and original adopted files are outside the repository at `C:\Users\ADMINI~1\AppData\Local\Temp\native-home-03-20261026-203259`. Isolated guard `.codex/task-state/native-home-03` adopts only the 16 declared prototype/glue/test files; 224 other original dirty paths remain protected. No branch/worktree, reset/clean, commit/push, Release APK or Suite update is authorized.

Completion: one combined visual/interaction correction of existing Debug immersive home; preserve NATIVE-HOME-02 settings, mode persistence, same-Activity host, WebHome priority, Release/Mobile gate and all classic business logic. Do not migrate source/category/TMDB/history/player business.

## Reference states and decision (before implementation)

- A: user images 1 and 2 show NORMAL home. Image 1 is a direct 1280×720 reference. Image 2 is a perspective photograph: measure only the television content quadrilateral, not the surrounding wall/furniture. Top left house then Live/Search/History/Keep/Push/Settings; no clock or separate source glyph. Hero title about 23–37% of content height; metadata/synopsis beneath; primary row about 53–59%; poster imagery about 60–93%, 2:3 ratio, captions beneath. Horizontal inset about 2.5%; poster gap about 1.4% width. These are target proportions, not screenshot pixels interpreted as dp.
- B: user image 3 is FILTER state in the same TV content. Three upper-left pill rows (region, year, order); top function bar and title/logo absent; metadata/synopsis remain below filters; background, primary row and same poster rail remain. No modal rectangle, header, extra confirmation or dim layer. Preserve normal rail baseline.
- Source: user image 4 and read-only current WOGG `tv-browse.css:167-175`, `tv-browse.js:1544-1574,1580-1687`. Right-side full-height drawer, outside scrim, selected state independent of focused item, Back closes and returns to house. Secondary repeat-confirm restores default and continuous choices do not close. WOGG HEAD `be71724d3a168ba69dbc2c925d1ac71ac720d355` plus existing dirty CSS/JS; no claim this dirty version is device-stable. HTML is not copied or modified.
- Android BaseGridView official API: https://developer.android.com/reference/androidx/leanback/widget/BaseGridView ; rendering guidance https://developer.android.com/topic/performance/issues/render (accessed 2026-10-26, primary evidence). Grid remains the only poster scroller; no pixel scrolling/second SmoothScroller. Local counters and log timings do not establish real-TV frame rate. Keep scale at 1.04/120ms rather than disguising latency by shortening animations.
- Existing local NativeHomeBrowseView, coordinator, poster adapter and host are direct implementation evidence. Root dispatch invokes the coordinator once; the coordinator only diverts region transitions/hard edges. No custom pixel scroller was found. Existing restore posts lacked coalescing; Hero updates and background work run directly on focus; HomeActivity.getVideo hides/recreates the immersive view even when already on the same native mode. Audit/fix these specific paths and retain device uncertainty.
- Artwork: one original generated cinematic garden scene, stored as `nhp_cinema_demo.png`; no reference screenshot or film artwork extracted. Fixture metadata explicitly says local demo. Existing illustrative posters remain fixtures. No TMDB runtime requests.

Alternatives: preserving the centered filter dialog violates image B; porting WOGG's whole page violates visual authority; changing architecture would duplicate Phase 1/02 work. Chosen: update existing XML/geometry, make filter state inline, retain one native source drawer and coordinator, keep the same poster adapter/view across filter state and redundant native refresh. Upstream merges/PRs/papers are inapplicable to this bounded local UI repair; no dependency change.

## 实现结果及参考图对照

本轮复用原有 NativeHomeBrowseView / ModeHost / Coordinator / PosterAdapter，没有建立新页面或第二套轨道滚动机制。正常状态与筛选状态共用同一 View 和 Adapter。

| 区域 | 参考图及本轮实现 | 自动化证据 / 尚存差异 |
| --- | --- | --- |
| 顶部 | 从左向右：房子、直播、搜索、历史、收藏、推送、设置；去掉独立换源按钮、时钟及入口编号 | XML 中只有一个 ImageButton、没有 TextClock；6 个标签顺序断言通过。顶部仍使用原生白色焦点轮廓；未做电视截图逐像素比对 |
| 顶部溢出 | 现有横向 Grid 完成内部焦点/滚动，房子与第一项之间由协调器连接 | 0/1/12/24 项均可布局；24 项实际方向导航往返通过。6 项为本地原型清单，尚不接管 HomeButton.getVisibleButtons()；经典模式配置、排序和追更入口未改变 |
| Hero | 双层 CENTER_CROP 背景、左右/底部渐变；左侧固定标题槽、元信息和最多两行简介，右侧留出主体 | 1920×1080 本地布局测得 Hero 起点 y=243（22.5%H）、宽度50%W。使用原创静态演示剧照与文字标题，不是参考图影片 Logo；三个矢量演示海报重复使用，不声称还原了真实片库 |
| 分类 | 位于 Hero 和轨道之间；轻量透明胶囊，已选红色边缘、聚焦白色轮廓分别表达 | 分类行 y=554（51.3%H），行内垂直居中；左右获焦不会应用分类、重绑 Hero 或移动海报。参考图 A 的具体字体/颜色仍需实机截图核验 |
| 海报 | 单个 HorizontalGridView；2:3，标题居中、评分/画质轻量角标，白色轮廓、1.04/120ms | 1080P 测得海报顶部 y=648（60%H）、233×350；标题区56px、海报底部998（92.4%H），约7张完整可见。不同密度/安全显示区只做布局/几何测试，4K实体设备未测 |
| 筛选 B | 三行内联筛选位于左上；顶部入口和标题/Logo槽 GONE，背景、元信息、简介、一级分类和轨道保留 | 筛选行起点3.5%H、单行约4.5%H、间隙1.2%H；元信息区移至31%H。无 Dialog、标题、确认按钮或额外容器。轨道 View/Adapter/位置/几何更新次数均保持不变 |
| 筛选选择 | 地区、年份、排序可连续确认；再次确认当前项恢复默认，按一级分类分别保存本地选项 | 焦点与选中分离；筛选展开时确认另一一级分类会同步其选项，再次回到原分类恢复其选择。Back 先收起筛选，恢复最后确认的一级分类焦点。没有真实分类、筛选、分页请求 |
| 站源抽屉 | 唯一房子入口打开右侧全高抽屉，宽约20%W、外部暗化0.40、名称及已选白色胶囊 | 原生 Dialog 仅承载抽屉，不是居中选择框；原生 ScrollView 负责菜单滚动。上下边缘和左右锁在抽屉内，确认或返回关闭并回到房子；12个演示站源，不触碰真实锁定/切换业务 |

参考图 B 是拍摄电视的透视照片，本轮比例依据电视内容区域估读，不声称精确复原像素。实际演示素材、片名字体、Logo、海报内容、主题色与照片存在明确差异；最终视觉验收必须使用电视截图。

## 焦点迟缓排查：证据、修复与边界

- 实际 Leanback `leanback-grid-1.0.0` 的 GridLayoutManager 字节码默认 `mMaxPendingMoves=10`、`mSmoothScrollSpeedFactor=1`。允许积压移动是长按松手后继续追赶的候选因素，不足以证明电视延迟的唯一根因。本轮使用同一 Grid 的 `setSmoothScrollMaxPendingMoves(2)` 限制积压，滚动速度仍为1；未增加 SmoothScroller、scrollBy 或逐帧滚动。
- 原恢复路径每次都 post 新焦点恢复任务；现合并为至多一个待执行任务，代次校验并在已有用户输入时放弃旧位置/焦点恢复。测试两次恢复后 pending=1，用户输入后 pending=0，旧恢复不抢焦点。
- 整个呈现层释放时清空待恢复任务，并取消抽屉的入口恢复回调，避免向即将移除的主页发出额外 requestFocus；普通关闭抽屉仍恢复房子。
- 原一级分类获焦即更新选择；现焦点仅记录，确认才应用分类。连续5次分类右移，Hero、几何和恢复任务计数不增，轨道位置不变。
- 原 HomeActivity.getVideo 在选择原生呈现前无条件隐藏沉浸层，重复刷新会销毁并重建宿主。本轮仅在进入 WebHome 或确实返回经典模式时隐藏，重复原生路径复用 ModeHost.show 的已有幂等性。WebHome 判断仍在沉浸模式之前；错误回退仍 getVideo(true)。
- 相同 Hero 不重绑，相同背景资源不重复设置/交叉淡入；仅视口尺寸变化重算几何。缩放只改 scaleX/scaleY，保持卡片布局尺寸；原有120ms动画未缩短。
- 根 dispatch 对每个事件只调用一次协调器，未发现第二个像素滚动器。协调器只处理区域转移、区域硬边界及房子与顶部功能的衔接，其余交给原生焦点/滚动。
- 新增 Debug 有界日志 NativeHomeFocus：按键 repeat/queuedMs、焦点 afterKeyUs、dispatchUs、scrollState、drawUs。每个 View 最多120条，每个按键最多2条绘制日志；默认关闭，不安排延时、逐帧或焦点任务。drawUs 是 View 绘制调用耗时，不是 GPU 帧完成时间。
- 本地计数样例：2次按键、4次累计焦点（包括初始化）、geometry=1、heroes=1、backgrounds=1、pendingRestores=0。本地样例不是电视毫秒性能测量。

**结论：确定并修复了上述多余更新/重建/恢复路径；真机迟缓的主要贡献因素及改善幅度尚未闭环，不能宣称已达到原生流畅度。** maxPendingMoves=2 在电视长按下是否需要调整，也属于待验证项。

## 本轮文件清单（相对 App 仓库）

16个既有文件在施工前现有内容上修改，未用 HEAD 覆盖：

| 文件 | 原因 |
| --- | --- |
| app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java | 去除原生重复刷新时无条件拆卸；原生返回优先关闭内联筛选 |
| app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeHost.java | 默认6个顶部演示项，去掉错误房子描述，委托筛选返回 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java | 原组件布局、单一换源入口、内联筛选、选择与焦点分离、恢复合并、更新去重 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewFocusCoordinator.java | 只协调必要区域；内联三行筛选上下衔接 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewGeometry.java | 参考图比例与原有 dp 边界 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewMenuDialog.java | 原菜单组件改为唯一站源抽屉；筛选不再使用 Dialog |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewSelection.java | 本地地区/年份/排序的分类作用域、重复选择及恢复 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewData.java | 明确本地演示信息、演示站源和筛选数据、原创背景引用 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewStyles.java | 白色焦点反馈、独立胶囊选中状态 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewPosterAdapter.java | 相同几何不重复通知；原卡片/缩放系统保留 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomePreviewActivity.java | 默认顶部数量同步；独立 Debug 预览 Back 先收起筛选 |
| app/src/leanback/res/layout/view_native_home_preview.xml | 顶部左排、删除重复换源/时钟、加入内联筛选槽 |
| app/src/leanback/res/values/native_home_preview.xml | 正式视觉入口文字、本地分类和模拟操作文案 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeTest.java | 真实设置回调随顶部视觉排列移至第6项 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewStateTest.java | 更新本地筛选数据断言，保留状态/几何检查 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomePreviewViewTest.java | 将原居中菜单测试改为内联状态，保留轨道/焦点/恢复回归 |

5个新增文件：

- app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewTiming.java：默认关闭的有界 Debug 时序记录。
- app/src/leanback/res/drawable/nhp_home_icon.xml：唯一房子图标。
- app/src/leanback/res/drawable-nodpi/nhp_cinema_demo.png：原创本地演示背景，不含参考截图。
- app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeVisualContractTest.java：5项布局/交互/更新次数回归。
- docs/NATIVE-HOME-03.md：本任务唯一施工、验证及回退记录。

现有 Setting、AppearanceDialog、native_home_mode.xml、经典业务 Presenter、Mobile、WebHome、WOGG、TMDB、播放器和存储未修改。Release/Mobile继续由既有 DEBUG + leanback 门控返回 CLASSIC；预览资源/类仍在 Leanback source set，不能把“未暴露入口”写成“源码完全不参与 Release 编译”。

## 验证与构建

使用 JDK21、Android SDK E:/Android/Sdk、既有 Gradle 缓存及 Chaquopy Python3.10，离线执行：

```text
:app:testLeanbackArm64_v8aDebugUnitTest
  --tests com.fongmi.android.tv.ui.home.*
  --tests com.fongmi.android.tv.ui.homepreview.*
  --tests com.fongmi.android.tv.ui.activity.HomeActivityDeferredFragmentWorkTest
  --tests com.fongmi.android.tv.ui.activity.HomeSiteSwitchClearsTypesTest
:app:assembleLeanbackArm64_v8aDebug
:app:processLeanbackArm64_v8aReleaseManifest
-PchaquopyBuildPython=E:/DevTools/python-3.10/python.exe --offline --no-daemon --console=plain
```

33项限定测试通过（模式/设置8、隔离2、本地状态4、既有视图10、新视觉交互5、原主页延迟工作/切源4），不是全仓测试。Robolectric 直接 View dispatch 对未处理左右键使用 Android focusSearch/requestFocus 补足 ViewRoot 的标准方向回落；不模拟真实设备输入队列或 GPU。

完整 Debug APK 构建通过；Release仅处理 Manifest，没有生成 Release APK。既有资源 namespace/格式及 Gradle弃用警告保留，没有为消除警告扩大修改。隔离检查确认演示组件未新增真实站源/分类/TMDB调用；本轮没有设备抓包，不能以源码检查替代全App网络运行验证。

产物、最终签名/哈希、保护核验和真机清单见下方最终交付记录。

## 真机验收与时序采集

当前 ADB 三个已知端点均 offline，没有在线电视。本轮未安装、未启动新APK，以下全部 **NOT TESTED**：

1. 1080P/4K实体电视安全边界、主题色、文字与参考 A/B 截图对齐；标题槽、筛选元信息位置和真实可见张数。
2. 顶部各入口左右访问、设置进入及切回经典；房子仅打开抽屉、不退出；长列表抽屉滚动和返回恢复。
3. 筛选三行连续选择/再次选择默认、一级分类切换恢复、Back关闭后的焦点与海报位置。
4. 长按的实际焦点延迟、松键后追赶、首尾回弹、缩放/裁切和帧率；4K与少量海报场景。
5. 同一 HomeActivity 模式往返、App重启持久化、已有 WebHome 会话与错误回退。用户此前确认的四项双模式测试属于前一APK，不算本轮复测。

常规验收从现有桌面入口打开，再在「设置 → 外观与语言 → 主页样式」选择沉浸模式。独立 Debug 预览仅用于本地数据/极端数量，不代表模式路由验收：

```powershell
adb -s <电视序列号> install -r "E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk"
adb -s <电视序列号> shell am start -n com.silent.android.webhtv/com.fongmi.android.tv.ui.activity.HomeActivityCurrent
adb -s <电视序列号> shell am start -n com.silent.android.webhtv/com.fongmi.android.tv.ui.homepreview.NativeHomePreviewActivity --ez native_home_preview true --ei preview_cards 18 --ei preview_functions 6
```

独立预览不接通真实设置；正式双模式宿主的第6项「设置」保留既有 SettingActivity 连接。独立预览 Back 先退出筛选，再回原任务；冷启动退出仍使用既有 HomeActivity。

必要时采集一次有界日志（启用后重进沉浸主页），对比正常短按、持续长按、松手、筛选和抽屉往返：

```powershell
adb -s <电视序列号> shell setprop log.tag.NativeHomeFocus DEBUG
adb -s <电视序列号> logcat -v threadtime NativeHomeFocus:D '*:S'
# 结束采集后关闭；不清空设备日志。
adb -s <电视序列号> shell setprop log.tag.NativeHomeFocus INFO
```

## 最终交付记录

最终构建：BUILD SUCCESSFUL（39s，154 tasks：19 executed / 135 up-to-date）。33 tests，0 failures / 0 errors。局部 `git diff --check` 通过。

- APK：`E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`
- 大小：209,259,090 bytes（199.57 MiB）。
- SHA-256：`79CBCD70DB925599DD0DC63A271BA4EF2D4EA3324B767DFA4206577EDA846696`
- 实际 Manifest：applicationId `com.silent.android.webhtv`，5.6.0 / 560，debuggable=true；默认启用的 Launcher alias 仍是 `com.fongmi.android.tv.ui.activity.HomeActivityCurrent`，target=HomeActivity，MAIN + LAUNCHER + LEANBACK_LAUNCHER。独立 NativeHomePreviewActivity exported=true、无 Launcher filter，仅 Leanback Debug 变体声明并要求显式 extra。
- 签名：apksigner verify 通过，APK v2，Android Debug，证书 SHA-256 `97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`。
- ABI：最终 APK aapt badging 仅 `arm64-v8a`。实际 APK 资源含主页样式两个选项、顶部数组和房子图标。
- Release merged Manifest：NativeHomePreviewActivity=0；既有模式解析测试覆盖 Release/Mobile 强制 CLASSIC。没有生成 Release APK，也没有构建或运行 Mobile。

施工前后 App HEAD 均为 `030485b70c263621fe4575abe5758b28115832c3`。按 `status --porcelain=v1 --untracked-files=all`，240→245条脏路径，仅增加上述5个文件；16个既有原型/胶水/测试文件作本轮补丁。原有5,196文件重新SHA-256比较：5,180完全不变、16预期改变、0丢失；224条未采纳原有脏路径的 guard 指纹通过。原始副本和逐路径基线/结果保存在仓库外，可供审核补丁，不覆盖旧成果。R2/R3/C55及其他未采纳成果保持。

WebHome全部114文件、WOGG全部106文件 SHA-256及各自 HEAD/status 与施工前一致（含WOGG原有104条dirty路径）；Suite原有App/WebHome gitlink索引项逐字相同。未修改共享 Setting/Appearance、Mobile、真实站源/TMDB/历史/收藏/追更/播放逻辑。没有commit、push、Suite pointer更新或自动进入后续业务迁移。

回退仅针对本轮外部originals副本和5个新增路径审核后做反向补丁；不能使用 HEAD restore/reset，因为这些原型及HomeActivity在施工前已有未提交改动。当前交付不执行回退或删除文件。

**状态：源码和Debug产物交付完成，视觉/性能真机验收 PENDING。** 停止在 NATIVE-HOME-03；下一动作由用户电视测试反馈决定。
