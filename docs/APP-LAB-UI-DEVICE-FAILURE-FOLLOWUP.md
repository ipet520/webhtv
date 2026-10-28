# 默影视实验室 UI 真机失败后续核查与补修

日期：2026-10-23  
范围：命令行 DPAD 焦点层级、实验室二级弹窗中的 WebView 加载与可视区域、横屏焦点滚动、实验室弹层统一退出，以及 `LabCommandSheet` 动态参数焦点回归补修。  
施工状态：本次参数焦点补修、Leanback ARM64 Debug 构建完成；最新 APK 尚未安装，Android TV 真机验收待进行。

## 结论

本轮只处理实验室 UI：

1. 命令行主体现在是列表第一级焦点。右键进入本行操作按钮，左键按顺序回退，行内按钮的确定键只执行自己的操作；上下键从行主体或行内按钮进入相邻行主体。
2. WebView 次级弹窗修复的是 AlertDialog 内部 `parentPanel/customPanel` 未分配剩余高度的问题。外层窗口尺寸规则保持原样；内嵌 WebView 等到生产弹窗测出有效 viewport 后才调用 `loadUrl()`，加载与主框架错误均有日志和可见提示。
3. 详情页已有的 `NestedScrollView` 可以随聚焦子行滚动。本轮在 Adapter 中显式把 DPAD 上下导航落到相邻行主体，运行时测试确认连续向下导航后焦点行完整进入横屏可视区，因此不改详情页布局。
4. 所有共享 `LabCommandSheet` 命令详情增加统一标题栏关闭入口；其余弹层复用标准 BACK/取消机制。进度弹窗和输出弹窗之前显式禁用 BACK，本轮恢复 BACK 关闭，但不改变后台任务或命令的运行/停止状态。

用户已确认成功的两项保持原样：原生播放器「第二字幕」焦点背景没有在本轮改动；主 BottomSheet 自动展开逻辑没有改动。此前关闭按钮新增造成的参数焦点回归，已在本次补修中单独处理；主弹层自动展开逻辑仍未改动。

## 安装版本与运行路径

当前在线设备为 OnePlus 8 Pro（IN2025，Android 13，1440×3168），不是 Android TV。施工前设备装有 `com.silent.android.webhtv` 5.6.0（versionCode 560）Leanback ARM64 Debug，设备端 SHA-256 为 `AEC647837192A2D4CCE0FF8D78F1D7DADB8B7BA79C6898FD2EBA167E3E3C2ACB`。本轮按用户此前要求用 `adb install -r` 覆盖安装后，版本仍为 5.6.0 (560)，`lastUpdateTime=2026-10-23 14:48:43`；设备端 `base.apk` SHA-256 已核对为新 Leanback 产物 `91E18CEDD4BB4A3530B965F20FDE42248E33B26D29C993D2D74DF6312FE7DC3E`。手机安装不代替 Android TV 遥控器验收。

Lab 命令详情由 `LabDetailActivity.onOpen()` 打开 `LabCommandSheet`；共享列表使用 `LabCommandAdapter`，其布局和代码均位于 `src/main`，Mobile 与 Leanback 使用同一实现。此前确认的设备配置中，Mihomo「启动代理」的「面板1」是 `action=webview`、`style=dialog`，没有声明宽高，实际走 `LabActions.openWebView()` 默认尺寸。

## 已确认根因和修复

### 命令行两级 DPAD 焦点

原行主体已可聚焦并负责打开详情；`btnAction` 有点击监听，却没有明确的焦点进入路径。线性布局的父行可能先占据列表焦点，遥控器无法从行主体可靠地进入子按钮。此前代码也没有给行内按钮和相邻命令行定义 DPAD 路由。

共享 Adapter 现在在绑定每一行时设置 `FOCUS_BEFORE_DESCENDANTS`，保留行主体作为列表的默认焦点目标。行主体的 RIGHT 进入第一个可点击子控件；行内按钮的 LEFT/RIGHT 在同一行的可点击控件间移动，越过左边界回到行主体；行主体和子按钮的 UP/DOWN 都聚焦相邻行主体。目标行尚未附着时，Adapter 先滚动到该行，再请求焦点。方向键处理和按钮发现集中在 `LabCommandAdapter`，所以所有使用该共享 Adapter 的实验室命令行都生效。

触摸/鼠标监听仍分别绑定在父行和操作控件上。确定键落在父行时触发 `onOpen`，落在子按钮时触发 `onAction`；子按钮不会把自己的点击转成父行点击。状态圆点、命令文字和运行状态标签不是点击控件，不参与焦点路径。父行和按钮继续使用各自背景；按钮保留 `duplicateParentState=false`，聚焦按钮时父行不同时显示焦点态。

来源：

- `app/src/main/java/com/fongmi/android/tv/lab/LabCommandAdapter.java:96-190`：绑定监听、方向键路由、同行操作收集、相邻行焦点与未附着行处理。
- `app/src/main/res/layout/adapter_lab_command_compact.xml:2-55`：行主体 `focusable`、`beforeDescendants`、操作按钮 `focusable` 和独立父状态。
- `app/src/main/res/drawable/selector_lab_command_row.xml:9-14`、`app/src/main/res/drawable/shape_lab_run_btn.xml:16-23`、`app/src/main/res/drawable/shape_lab_stop_btn.xml:10-17`：行和按钮各自的聚焦视觉。

### WebView 次级弹层加载与可视区域

根因已由生产 AlertDialog 测量复现并修复。问题不是外层窗口高度不足：横屏测试窗口 decor 为 `720×414`，外框高度仍按既有配置；但 AppCompat 的 `parentPanel` 使用 `wrap_content`，测量只有 `700×204`，其中 `customPanel/root` 只有 `700×76`，WebView 只有 `700×73`。即使只把内容 root 的 layout height 设为 266px，父面板仍把它压回 76px，因而“root match_parent + WebView weight”本身不足以撑开真实 AlertDialog。

`LabActions.openWebView()` 现在按 decor 实际高度扣除标题、content、底部按钮、内外 padding 和 margins，令此 WebView 弹层自己的 `parentPanel` 填满当前窗口可用高度、`customPanel` 占据标题与按钮之间的剩余高度，再将 root 设置为对应内容高度。测试重新测量真实 Dialog 后得到 `parentPanel=700×394`、`customPanel/root=700×266`、WebView measured `700×263`。测试环境的 Dialog decor 没有附着到设备 WindowRoot，故 WebView `getLeft/Top/Right/Bottom` 仍是零；这不是实际设备矩形验收，设备矩形与遮挡情况仍待验证。

在 WebView 有非零 measured 宽高且可见后才调用 `loadUrl(url)`。内嵌页与“浏览器打开”使用同一个解析后的 `url`；生产 Dialog 测试确认 `WebView.getUrl()` 和外部 `ACTION_VIEW` Intent data 相同。`onPageStarted`、`onPageFinished`、主框架网络错误、HTTP 4xx/5xx、TLS 错误和 JavaScript console error 均记录到 `LabWebView`；主框架网络/HTTP/TLS 失败显示状态层。日志只记录 URL scheme/host/port，不记录路径或 query。页面实际回调、Chromium viewport、透明/遮挡和真机矩形仍需要设备日志确认。

外层窗口默认宽高、`fullscreen` 和 YAML/命令显式宽高规则均未在本轮扩大；没有给其他弹窗套用同一尺寸。

来源：

- `app/src/main/java/com/fongmi/android/tv/lab/LabActions.java:562-739`：WebView 设置、主框架回调、有效尺寸门控、AlertDialog 内部面板分配、错误提示和安全诊断。
- `app/src/main/res/layout/dialog_lab_webview.xml:2-35`：加权 WebView 区域及加载/错误状态覆盖层。
- `app/src/test/java/com/fongmi/android/tv/lab/LabUiDeviceFailureFollowupTest.java:293-343`：生产 Dialog measured viewport 与 Browser/WebView URL 相同检查。
- 自动化实际矩形局限：当前 Robolectric 窗口未附着 WindowRoot，报告 measured viewport，不将未附着测试的 bounds 当作真机矩形。

### 横屏焦点跟随滚动

详情页 `activity_lab_detail.xml:21-26,177-186` 使用 `NestedScrollView` 包含 `wrap_content` 的 `RecyclerView`，列表自身关闭 nested scrolling。新测试在生产详情页布局中通过 DPAD 连续向下移动 12 行，断言父滚动容器滚动，且最后聚焦行的完整矩形落在滚动视口内。Leanback 与 Mobile 测试均通过，因此没有增加第二套手动滚动监听，避免与 `NestedScrollView` 的焦点可见性处理重复。

真实电视的安全区、遥控器固件以及滚动动画时序尚未验证；见文末状态。

## 实验室次级窗口核查

| 窗口 | 当前实现 | 本轮处理 |
|---|---|---|
| `LabCommandSheet` 命令详情 | `BottomSheetDialog`；详情由 `LabDetailActivity.onOpen()` 打开 | 共享标题栏的 close ImageButton 可聚焦；DOWN 进入第一个动态参数，参数区按模型顺序导航，确定关闭只 dismiss、不触发 run/stop；BACK 保持默认可关闭，关闭后原命令行恢复焦点。底栏 UP 使用实际内容区焦点搜索，不再与关闭键形成往返环 |
| `LabCommandEditDialog` | AlertDialog；取消、保存，编辑时另有删除 | 标准 BACK/取消可用；从命令详情打开后 BACK 只关闭编辑框，父 Sheet 仍显示；未另加工具级按钮 |
| `LabVariableDialog` | AlertDialog；取消、运行；内容在 ScrollView | 标准 BACK 只关闭参数框；生产实例测试覆盖父 Sheet 保留 |
| `LabFileBrowserDialog` | AlertDialog；取消；目录/文件 ListView | 标准 BACK 只关闭文件浏览；父 Sheet 保留 |
| `LabRunningDialog` | AlertDialog；关闭、停止全部；命令 ListView | 标准 BACK 只关闭列表，不调用停止操作；父 Sheet 保留 |
| `LabOutputDialog` / `LabOutputActivity` | 前者 AlertDialog，后者独立 Activity | `LabOutputDialog` 原来 `setCancelable(false)`，现在允许 BACK 关闭；外部点击仍不关闭；关闭输出不会停止后台命令。Activity 路径仍按 Activity 返回栈工作 |
| `LabActions.openWebView()` | 标准 AlertDialog；关闭、浏览器打开 | BACK 只关闭最上层 WebView Dialog，父 Sheet 保留；URL 与外部浏览器一致；不加每个 WebView 工具的独立关闭实现 |
| 下载、安装、依赖准备、导出进度 | 多个 AlertDialog 在进度中隐藏正向“关闭”按钮 | 原来 `setCancelable(false)` 使按钮隐藏期间 BACK 无效；改为可 BACK dismiss、禁止外点击。任务本身继续异步运行，完成/错误回调不变；失败时仍显示原有显式关闭按钮 |
| 实验室设置、确认、导入提示、日志、二维码和动态操作 | 各自 `WebHtvAlertDialogBuilder` AlertDialog | 默认 BACK/取消机制不变；没有证据显示需要改尺寸或另加关闭按钮 |
| `LabFilePicker` 系统选择器 | Android ACTION_OPEN_DOCUMENT / ACTION_OPEN_DOCUMENT_TREE Activity | 由系统 Activity 处理 BACK，再返回参数编辑器；不是 App 内自建 Dialog |

分类型覆盖边界：生产 Dialog 测试实例化并覆盖命令编辑、变量、文件浏览、运行列表、输出、内嵌 WebView 六类子 Dialog 的 BACK 回调和父 Sheet 保留；另覆盖主 Sheet BACK 回调后的命令行焦点恢复、共享标题关闭键 DPAD、上述子 Dialog 标准按钮的聚焦和点击关闭。下载/安装/依赖/导出进度、设置与确认类动态弹窗、系统文件选择器和独立 `LabOutputActivity` 本轮只做源码路径核对，未声称有自动化 UI 事件覆盖。标准 AlertDialog 按钮的遥控器 CENTER 和物理 BACK 仍需真机验收。

### 统一退出实现与覆盖边界

修改集中在共享 `LabCommandSheet`，没有按 Mihomo、Python 或模板命令逐个加按钮。close ImageButton 使用当前 Lab 次级按钮焦点背景；未调整全局焦点 Drawable、ThemeBinder、弹窗尺寸或播放器。

Android 标准可取消 Dialog 的 BACK 关闭当前顶层 Dialog，嵌套 AlertDialog dismiss 后父 Sheet 仍显示。测试真实构造 `BottomSheetDialog` 与各类 AlertDialog，调用生产 `Dialog.onBackPressed()` 入口并确认 child dismiss、parent 仍 showing。关闭主 Sheet 后，生产详情 Sheet 的 dismiss 回调将焦点恢复到原命令行。Robolectric 下对 BottomSheet 的物理 KEYCODE_BACK 分派未稳定关闭窗口，因此父 Sheet 覆盖的是生产回调；设备级 BACK 仍待确认。命令详情标题关闭键的 DPAD CENTER 只触发 close；测试确认 run/action 回调计数仍为零。

进度/输出窗口的 BACK 只关闭窗口，不取消异步下载/安装/导出或停止命令；这保留既有业务运行语义，同时避免不可退出的模态遮罩。外部点击仍禁用，避免误触关闭。普通 AlertDialog 的取消/关闭按钮沿用平台按钮组件，测试确认按钮可聚焦且执行点击后关闭正确子层；Robolectric 没有把合成 DPAD_CENTER 分派成标准按钮点击，因此标准按钮的真实遥控器确认键仍列为设备待验，报告不将其记作自动化 DPAD 通过。共享 BottomSheet 的自定义标题关闭键已有实际 DPAD 事件测试。

## 为什么上一轮测试没有发现真机失败

上一轮报告中的 59 项实验室测试和 10 项 `TrackDialogTest` 通过，不代表这些场景完成了电视遥控验收。59 项按实际验证对象分组如下：

| 上轮测试组 | 数量 | 实际覆盖 | 未覆盖的失败条件 |
|---|---:|---|---|
| `LabTvFocusSourceTest` | 17 | 读取 Java/XML 文本并以字符串、正则和方法片段断言焦点属性、Drawable 引用、展开参数、滚动容器等；例如 `commandRowAndActionButtonHaveIndependentFocusVisuals()` 和 `commandSheetExpandsWithinViewportAndKeepsFocusableContentScrollable()` | 没有创建生产 RecyclerView 并发送 DPAD；没有证明父子焦点顺序、中心键监听隔离、窗口裁切、WebView viewport 或连续导航时的真实滚动 |
| `LabSettingsDialogThemeTest` | 8 | 设置弹窗的主题、色彩 token、对比度和布局表格 | 测的是设置弹窗，不是 `LabActions.openWebView()` 的 AlertDialog；不覆盖命令行 DPAD |
| `LabEnvTest`、`LabProcessIdentityTest`、`LabRunnerProcessCompatTest` | 11 | ABI/下载选择、进程身份及旧版本进程存活逻辑 | 无 UI 窗口、焦点搜索或 WebView 视口 |
| Mihomo Config、Monitor、RoutingIntegration 测试 | 23 | 代理端点/生命周期/共享客户端请求逻辑 | 无命令列表 DPAD，也不渲染实验室二级弹窗 |
| **实验室合计** | **59** | 上轮报告记录 59 项通过 | 没有覆盖本轮三类真机交互组合 |
| `TrackDialogTest` | **10** | 原生播放器字幕对话框的轨道状态与控件行为 | 路径属于播放器，不会执行实验室命令 Adapter、WebView 面板或详情滚动 |

上轮报告原记录见 `docs/APP-LAB-UI-LANDSCAPE-UNIFIED-REPAIR.md:50-57`。本轮新增测试使用 Robolectric 创建生产详情布局和生产 Adapter，实际派发 DPAD down/up 并断言焦点目标、监听回调、滚动矩形和 WebView 测量尺寸，不再用源码字符串证明界面行为。

## 自动化验证与构建

新增 `app/src/test/java/com/fongmi/android/tv/lab/LabUiDeviceFailureFollowupTest.java:95-413`，覆盖：

1. 行主体 RIGHT 进入按钮；多个行内控件左右切换；左边界回行主体；上下从按钮进入相邻行主体；中心键分别调用 `onOpen`/`onAction`；触摸点击仍调用各自监听。
2. 生产命令详情 BottomSheet 标题关闭键可经 DPAD UP 到达，DPAD CENTER 只关闭 Sheet、不运行/操作命令；关闭后恢复打开它的行焦点。
3. 在生产 Dialog 中向参数编辑、文件浏览、运行任务、命令输出和内嵌 WebView 逐类发送 BACK；每次只关闭最上层窗口并保留父 Sheet。各类型关闭按钮另验证可聚焦、可点击且点击只关闭对应子层。标准 AlertDialog 按钮的物理 DPAD CENTER 分派未被 Robolectric 模拟出来，此项待真机确认。
4. 12 行连续 DPAD DOWN 后父滚动容器滚动，最后整行仍在横屏可视区。
5. 省略高度的生产 WebView 动作使用横屏默认高度，受限内容区的 WebView 测量尺寸不溢出；嵌入 WebView 与“浏览器打开”使用相同 URL。

测试结果：

- Leanback 本轮聚焦 UI 测试：7 项通过，0 失败、0 跳过。
- Mobile 本轮聚焦 UI 测试：7 项通过，0 失败、0 跳过。
- 前序完整 Leanback 单元测试曾执行 4261 项，3 项失败、2 项跳过；失败在未修改的 `MihomoRoutingIntegrationTest`，Gson 因 JDK 模块访问限制无法反射 `java.net.Proxy#type`，抛出 `InaccessibleObjectException`。本轮没有重跑全量测试，不能把完整测试套件标为全绿。
- `:app:assembleMobileArm64_v8aDebug`：成功。
- `:app:assembleLeanbackArm64_v8aDebug`：成功。

## APK

`aapt dump badging` 确认两个产物均为包名 `com.silent.android.webhtv`、版本 `5.6.0 (560)`、原生 ABI `arm64-v8a`；Leanback 包含 `android.software.leanback` 特征。

| Variant | APK | 大小 | SHA-256 |
|---|---|---:|---|
| Mobile ARM64 Debug | `E:\GitHub\webhtv-suite\app\app\build\outputs\apk\mobileArm64_v8a\debug\app-mobile-arm64_v8a-debug.apk` | 209,064,352 bytes | `5A531CF9EF4CFF25F1DF91F453EA90B940D7C1C7DA884F6670AF466EF664AB8C` |
| Leanback ARM64 Debug | `E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk` | 209,213,151 bytes | `91E18CEDD4BB4A3530B965F20FDE42248E33B26D29C993D2D74DF6312FE7DC3E` |

## 真机验收与状态

本轮已将新 Leanback APK 覆盖安装到在线 OnePlus 8 Pro 手机，并核对设备端安装 APK 哈希与构建产物一致；没有连接 Android TV。手机安装不是电视焦点验收。此前用户真机确认的播放器第二字幕修复保持不变，主命令 Sheet 的自动展开逻辑保持不变；本轮只在共享 Sheet 加了关闭入口。

| 状态 | 结果 | 依据 |
|---|---|---|
| `LAB_COMMAND_ROW_DPAD` | `AUTOMATED_PASS / TV_PENDING` | 两个 flavor 的生产列表测试覆盖两级焦点、方向键、中心键动作隔离、多按钮和触摸；未在电视遥控器上确认 |
| `LAB_MODAL_EXIT_BACK` | `CALLBACK_AUTOMATED_PASS / DEVICE_BACK_PENDING` | 生产 BottomSheet/AlertDialog 的 `onBackPressed()` 回调测试确认只关闭顶层并保留父层；真实电视返回键仍待验 |
| `LAB_MODAL_CLOSE_DPAD` | `SHEET_DPAD_AUTOMATED_PASS / STANDARD_DIALOG_DPAD_PENDING` | Sheet 自定义关闭键通过 DPAD 测试；标准 AlertDialog 按钮通过聚焦/点击测试，合成 DPAD_CENTER 未被 Robolectric 路由，等待遥控器确认 |
| `LAB_WEBVIEW_SECONDARY_VIEWPORT` | `AUTOMATED_PASS / TV_PENDING` | 生产 AlertDialog 动作与 View 测量测试通过；真实 Chromium viewport 和电视安全区待验收 |
| `LANDSCAPE_FOCUS_SCROLL` | `AUTOMATED_PASS / TV_PENDING` | 生产详情布局中连续 DPAD 与视口边界测试通过；电视设备仍待确认 |
| `PLAYER_SECOND_SUBTITLE` | `USER_DEVICE_ACCEPTED / UNCHANGED` | 用户明确确认成功；本轮未改播放器源码 |
| `PRIMARY_COMMAND_SHEET` | `USER_DEVICE_ACCEPTED / EXPAND_LOGIC_UNCHANGED` | 用户明确确认自动展开成功；本轮只在共享 Sheet 增加退出入口 |
| `MOBILE_APK_BUILD` | `PASS` | ARM64 Debug 构建成功，产物已记录 SHA-256 |
| `LEANBACK_APK_BUILD` | `PASS` | ARM64 Debug 构建成功，产物已记录 SHA-256 |
| `PHONE_LEANBACK_INSTALL` | `PASS` | `adb install -r` 成功；设备端 SHA-256 与 Leanback 构建一致 |
| `TV_DEVICE_ACCEPTANCE` | `PENDING` | 当前无 Android TV；手机安装和 Robolectric 结果不替代电视遥控器验收 |

未提交、未推送。此前 Mihomo 自动代理成果、播放器验收成果及其他工作区未提交改动均保留。

## `LabCommandSheet` 参数焦点回归补修（2026-10-23）

### 回归根因与修复

用户本次给定的 Leanback APK SHA-256 为 `91E18CEDD4BB4A3530B965F20FDE42248E33B26D29C993D2D74DF6312FE7DC3E`。对照该版本的 `LabCommandSheet.syncRunning()` 变更，关闭按钮新增的强制焦点对形成了循环：底部主操作 UP 被 `OnKeyListener` 截获并 `requestFocus()` 到关闭按钮，关闭按钮 DOWN 又被截获并送回同一底部操作。真实生产 Sheet 的红灯测试复现为：从关闭键 DOWN 后焦点落在「编辑」，没有进入第一个 `TextInputEditText`。长参数表也无法进入。

仅删除这组循环后，测试又发现系统几何搜索会将焦点放在文件参数的 Material 尾部图标；单行输入框还会消费上下键，长列表从第 5 个字段之后提前跳到底栏。因此修复包含两个精确范围：

1. `LabCommandSheet.java:316-340` 从当前命令实际渲染结果建立参数焦点序列；关闭键 DOWN 指向第一个参数，首参数 UP 回关闭键，单行参数 UP/DOWN 在相邻动态字段间移动。下拉菜单打开时保留其自身上下键行为；多行参数不覆盖上下键，避免破坏文本光标移动。其它方向键不由这些监听器处理。
2. `LabCommandSheet.java:343-357` 仅为单行下载地址/命令预览字段允许上下键继续使用 View 焦点搜索。删除底栏操作 UP→关闭键及关闭键 DOWN→底栏的强制跳转；关闭键 CENTER/ENTER 仍只执行 dismiss。

没有改 `dialog_lab_command_sheet.xml`、`expandSheetToViewport()`、`requestSheetFocus()`、命令执行/参数缓存、Mihomo 代理或播放器代码。主 BottomSheet 自动展开和播放器「第二字幕」已通过的状态保持原样。

### 生产视图测试

更新 `app/src/test/java/com/fongmi/android/tv/lab/LabUiDeviceFailureFollowupTest.java:239-370`，通过生产 `LabCommandSheet`、`BottomSheetDialog` 和动态变量布局发送 DPAD 事件并检查实际 `findFocus()`：

- 短参数表以 Mihomo「配置文件 / 外部服务 / 服务密码」字段模型验证：关闭键 DOWN 到配置文件，连续 DOWN 进入其余参数、命令预览和底部编辑；反向 UP 回到参数及关闭键。
- 修改服务密码后，命令预览同步变化；焦点离开和返回后输入文本模式、光标位置保持。Robolectric 不模拟真实软键盘和物理光标移动，IME/真机键盘行为仍列为待验。
- 12 个动态单行参数逐一获得焦点，最后字段聚焦后断言 `NestedScrollView.scrollY > 0` 且焦点 View 有可见矩形；清空并重新构造两项参数后，再验证新焦点顺序没有沿用旧列表。
- 关闭键 CENTER 关闭 Sheet 且不触发命令。既有行焦点、弹层 BACK、WebView viewport 等生产视图回归也同次重跑。

补修前新增的两个行为测试为红：关闭键 DOWN 落在底栏编辑按钮，未到参数字段；补修后整个 `LabUiDeviceFailureFollowupTest` 为 **9 tests / 0 failures / 0 errors / 0 skipped**。此前报告的 7 项聚焦 UI 测试不能覆盖此回归，因为它们只验证关闭按钮可见/可点和关闭动作，旧测试反而断言底栏 UP 应跳到关闭键，没有构造动态变量并走关闭键→参数→底栏的连续焦点链。

### 最新构建和设备状态

命令：`:app:testLeanbackArm64_v8aDebugUnitTest --tests com.fongmi.android.tv.lab.LabUiDeviceFailureFollowupTest :app:assembleLeanbackArm64_v8aDebug --offline --no-daemon --console=plain`，结果 `BUILD SUCCESSFUL`。`aapt dump badging` 确认包名 `com.silent.android.webhtv`、版本 `5.6.0 (560)`；ARM64 Leanback Debug APK：

- `E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`
- 文件大小：209,213,151 bytes
- SHA-256：`360FE50F28A75260308E66D0168465764403601BB3B797EEED74CCD7208DEE64`

该 SHA 与用户给定的旧 APK `91E18C…` 不同。此次只生成并校验构建产物，没有安装到手机或 Android TV；真实遥控器、软键盘和电视窗口焦点链仍需验收。

| 状态 | 结果 | 依据 |
|---|---|---|
| `LAB_COMMAND_SHEET_PARAMETER_FOCUS` | `AUTOMATED_PASS / DEVICE_PENDING` | 生产 Sheet 短/长动态参数 DPAD、可见滚动、文本预览与光标状态测试 9/9；尚无新 APK 真机验收 |
| `CLOSE_TO_PARAMETER_TO_ACTION` | `AUTOMATED_PASS / DEVICE_PENDING` | 关闭键→参数→预览→底部编辑及反向返回的实际焦点断言通过 |
| `DYNAMIC_PARAMETER_REBUILD` | `AUTOMATED_PASS / DEVICE_PENDING` | 参数集合从 12 项重建为 2 项后，焦点序列按新集合更新 |
| `PRIMARY_BOTTOMSHEET_EXPANSION` | `USER_DEVICE_ACCEPTED / UNCHANGED` | 用户此前真机确认；本次未修改展开方法 |
| `PLAYER_SECOND_SUBTITLE` | `USER_DEVICE_ACCEPTED / UNCHANGED` | 用户此前真机确认；本次未修改播放器 |
| `LEANBACK_ARM64_DEBUG_APK` | `PASS` | APK 构建成功，SHA-256 如上 |
| `CURRENT_APK_DEVICE_INSTALL` | `NOT RUN` | 本次未安装新 APK |
| `ANDROID_TV_DPAD_IME_ACCEPTANCE` | `PENDING` | Robolectric 不等同于电视焦点搜索、硬件遥控器或真实软键盘 |

未提交、未推送；其它已有未提交改动保持不动。
