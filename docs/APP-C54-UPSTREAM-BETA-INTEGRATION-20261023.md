# C54 上游 beta 增量同步与 Release 前稳定性整合

状态：上游审计已完成，选择性移植与验证进行中。目标固定为 `d9da14dcb77e84d4beb158e83efa74124a1116f7`。

## 固定范围与施工基线

| 项目 | 核验值 |
| --- | --- |
| 仓库 | `E:\GitHub\webhtv-suite\app` |
| 施工分支 | `main` |
| 施工前 HEAD | `ad46fe5da81eacbfbd0465de6b0b85450cf3780a` |
| C53 来源同步提交 | `f5f26b30855638a84aef3bbd5be40e97ec8b0ce1`，是施工前 `main` 的祖先 |
| C53 上游基线 | `93ea56a61df9fbe3acf0d345234f0a97c3bcfab3` |
| C54 固定目标 | `d9da14dcb77e84d4beb158e83efa74124a1116f7` |

`93ea56a` 是上游来源基线，不是本地 `main` 的祖先；C53 采用选择性同步，因此本轮用这个 SHA 与固定目标计算上游候选，不把 beta 整体合并进 App。目标对象、完整父链及其与来源基线的祖先关系均已在本地 Git 对象库核实；本轮不追踪 beta 后续移动。

## 施工前工作区保护快照

施工前 HEAD 为 `ad46fe5...`。工作区含 18 个已跟踪文件修改和 15 个未跟踪文件；没有已暂存变更。已跟踪差异共 673 行增加、38 行删除，`git diff --binary HEAD` 为 70,848 字节、1,290 行，SHA-256：`da0a966ffe8ff490e79fa528ca1c16c954df21023627d4494c7b0bc522df56fc`。这些路径由 C54 task guard 登记为保护基线，C54 移植不会覆盖、暂存或提交它们。未跟踪文件的逐文件大小和 SHA-256 已在施工前快照中核对。

既有内容归属如下：

- TV 实验室布局、列表、命令、终端与焦点修复：`app/src/main/java/com/fongmi/android/tv/lab/`、`app/src/main/res/layout/adapter_lab_*`、`app/src/main/res/layout/dialog_lab_*`、`app/src/main/res/drawable/selector_lab_*`、`app/src/test/java/com/fongmi/android/tv/lab/Lab*`、`docs/APP-LAB-UI-*.md`。
- Mihomo 自动代理、停止恢复与分流：`LabMihomoProxy.java`、`MihomoProxyConfig.java`、`MihomoProxyMonitor.java`、`catvod/.../OkHttp.java`、`catvod/.../OkProxySelector.java`、相应单元测试及 `docs/APP-MIHOMO-AUTO-PROXY-REPAIR.md`。
- 第二字幕既有控件背景及回归断言：`app/src/leanback/res/layout/dialog_track.xml`、`app/src/testMobile/java/com/fongmi/android/tv/ui/dialog/TrackDialogTest.java`。
- 用户维护的构建入口：`scripts/build-arm64.ps1`。

受保护路径完整集合：

```text
app/src/leanback/res/layout/dialog_track.xml
app/src/main/java/com/fongmi/android/tv/lab/LabActions.java
app/src/main/java/com/fongmi/android/tv/lab/LabCommandAdapter.java
app/src/main/java/com/fongmi/android/tv/lab/LabCommandSheet.java
app/src/main/java/com/fongmi/android/tv/lab/LabDetailActivity.java
app/src/main/java/com/fongmi/android/tv/lab/LabOutputDialog.java
app/src/main/java/com/fongmi/android/tv/lab/LabProcManager.java
app/src/main/java/com/fongmi/android/tv/lab/LabRunner.java
app/src/main/java/com/fongmi/android/tv/lab/LabTerminalActivity.java
app/src/main/java/com/fongmi/android/tv/web/WebCall.java
app/src/main/res/layout/adapter_lab_command_compact.xml
app/src/main/res/layout/adapter_lab_package.xml
app/src/main/res/layout/dialog_lab_command_sheet.xml
app/src/main/res/layout/dialog_lab_webview.xml
app/src/test/java/com/fongmi/android/tv/lab/LabTvFocusSourceTest.java
app/src/testMobile/java/com/fongmi/android/tv/ui/dialog/TrackDialogTest.java
catvod/src/main/java/com/github/catvod/net/OkHttp.java
catvod/src/main/java/com/github/catvod/net/OkProxySelector.java
app/src/main/java/com/fongmi/android/tv/lab/LabMihomoProxy.java
app/src/main/java/com/fongmi/android/tv/lab/MihomoProxyConfig.java
app/src/main/java/com/fongmi/android/tv/lab/MihomoProxyMonitor.java
app/src/main/res/drawable/selector_lab_command_row.xml
app/src/main/res/drawable/selector_lab_package_row.xml
app/src/main/res/drawable/selector_lab_terminal_history.xml
app/src/main/res/drawable/selector_lab_terminal_shortcut.xml
app/src/test/java/com/fongmi/android/tv/lab/LabUiDeviceFailureFollowupTest.java
app/src/test/java/com/fongmi/android/tv/lab/MihomoProxyConfigTest.java
app/src/test/java/com/fongmi/android/tv/lab/MihomoProxyMonitorTest.java
app/src/test/java/com/fongmi/android/tv/lab/MihomoRoutingIntegrationTest.java
docs/APP-LAB-UI-DEVICE-FAILURE-FOLLOWUP.md
docs/APP-LAB-UI-LANDSCAPE-UNIFIED-REPAIR.md
docs/APP-MIHOMO-AUTO-PROXY-REPAIR.md
scripts/build-arm64.ps1
```

## 上游增量审计口径

固定区间 `93ea56a..d9da14d` 含 134 个提交：51 个合并提交和 83 个非合并提交。非合并提交中 32 个仅改文档、3 个仅改测试、48 个改源码或源码与测试/文档混合。最终树相对来源基线净改 272 个路径（167 个 App 源码/资源、56 个 App 测试、3 个 Catvod 源码、45 个文档、1 个其他文件）。未发现 Gradle 依赖、Manifest、Room 数据库结构或 schema 变更；完整的逐提交台账及其处理结论见本报告附录。

采用最终目标树审计：重复 PR merge wrapper 不机械重放；同一修复以目标 SHA 上最终保留的实现为准；只在最终树仍存在、且能与排除功能安全分离时移植。局部提交如果被后续评审修正或回退，以最终父链/树为准。

## 候选决策台账

状态说明：`同步` 表示已选入本轮实现；`本地等效` 表示保留现有本地实现；`延期` 表示此处列明具体风险；`排除` 表示新增独立功能或超出本轮发布范围。每条同步记录对应上游来源、目标行为、移植路径、定向测试和验证结果。

| 上游来源 / PR | 目标行为与本地现状 | 决定 |
| --- | --- | --- |
| `cae6e4b6e9405ce34b1b29391aa4406a7c82acc4`、`bccb5b5a35dd7fc5a74cf3d8a38be844c6ba1416`、PR `#477` | 外观/语言重建期间，进程级 VodConfig 回调可能在旧 Leanback Activity 已保存 Fragment 状态后触发。当前已有若干事务守卫，但 `showHomeContent`、分类清理与内容刷新仍有未保存状态窗口。 | 同步最终评审版：内存状态先更新，允许提交时再提交事务；存活 Activity 延迟清理/刷新并在回前台补做；不使用 `commitAllowingStateLoss`。 |
| `30324abe36f4f11baf3e74b8f1659f9c0c0c4a02`、`c5ef7ec829b75ad3291b6ae5b11d26378d68090a`、PR `#475` | 现有崩溃页只显示 CAOC 异常文本；诊断日志在调试关闭时无崩溃前上下文。目标增加私有崩溃报告、PID 限定日志尾部、脱敏、临时内存日志及页面存活期 LAN 服务。 | 同步，保留本地主题化错误详情弹窗；不接入上游 EXO 原始异常 logcat 输出。验证日志大小/新鲜度/脱敏、处理器链与服务生命周期。 |
| `e79ed9058aab6cfce6f9433116e673cb89aeb140` | 硬解模式平台没有支持编码时，视频轨可能被丢弃而音频继续播放。当前 EXO 的 FFmpeg 视频扩展在硬解模式完全关闭。 | 同步最终实现，但仅在已有“解码器回退”开关允许时，把 FFmpeg 放到平台 MediaCodec 之后；关闭开关仍保持硬解语义。 |
| `48f59db5ae8e577cd0fba8c25be787876882d022`、`203bb7d567ec56960aa2276e865db680b983b020`、`8d47a5dd34618af520ba8f01c2d6aaa6a28f78e7`、PR `#466` / review `977ddf4343882cffe3623a126e76bc490f884c29` | 本地已有 HLS 广告改写、错误分类、预加载和自动换源链；目标分别处理空/坏 manifest 重试、网络失败不伪装换核、URL 为 `.m3u8` 的 TS 分片透传。 | 同步最小兼容组合：只对 manifest 放宽重试；分片内容类型优先于 URL 后缀；重试回调校验 `prepareSeq` 与 `PlaySpec` 身份；解码/媒体格式不支持仍允许既有内核回退；保留本地 EOF/预加载与手动换源流程。 |
| `2d403b2a31fe96adfd4eb6fb9d848168d598d433` | Release 下无条件把完整异常消息及 Throwable 写入 logcat，可能泄露播放 URL、请求信息或用户路径；现有 `PlaybackTrace` 已记录脱敏分类信息。 | 排除原始日志写入；继续使用现有脱敏诊断链。 |
| `da7ac6440a9d1da247bcabdb4f2ed291bf29001c`、`41f55daa17d272b0a51602a9998df1956a862d5d`、PR `#476` | 自动下一集使用 `VE:` 选集键，而页面持有 history key，字符串比较导致 Surface 重绑被跳过。当前服务/Activity 有本地生命周期和 AV/ASS 特化逻辑。 | 同步会话键等价与纯判定 Surface 绑定；只在 RESUMED 且未结束 Activity 的 BUFFERING/READY 路径校验/补绑。保留本地停止、释放、字幕和音视频生命周期实现，不采用上游 `stopPlayback` 改动。 |
| `1a16fd163c38e698616fba28dff64a71d879f194`、PR `#479` / review `389a9a4148eaf64440ecb3b3ccf9f73d62d0bdad` | TMDB 自动消歧当前最多读取六个候选完整详情，并发及流量开销较大；完整详情渲染需要图像与附加字段。 | 同步 lite 消歧详情和最多 3 路并发，按原候选顺序计分并对赢家继续取完整详情；保留高分辨率详情图片与现有 C16 凭据/来源协议，不带入全局图片降档。 |
| `7fe44116aafc02c580d7ddf80ba82708878257d2` | 本地 `TmdbDetailActivity` 已通过 `detailTasks.submitCallable(Task.largeExecutor(), this::loadTmdbResult)` 将 TMDB 等待移出共享 5 线程池。 | 本地等效，保留现有实现并补回归确认。 |
| `fa2f31812e55d2f79b175c6d0c579cefe6ab3300` | 预取可减少已知 identity 页面等待，但上游在站点详情返回之前先发 TMDB 请求；源携带 T3/T4 payload 时会多发一条网络请求，并扩大 C16 预热请求契约。 | 延期；本轮不增加额外 TMDB 请求。后续若要吸收，需在确认无源内嵌 payload 后启动并证明取消与凭据 epoch 隔离。 |
| `1bb216efec156709ca3d1b027701c6331749d986`、`29733de98d32760a4e5a743d4e420b48f5d59f61`、`631877c7d4869b070b835e46d0c6e1cefd3d2f49`、PR `#480` | 本地 ThemeDialog 已有系统栏安全尺寸算法；SiteHealthReport 及其他全屏/近全屏 dialog 仍有以原始 display bounds 尺寸窗口或内容上限的路径。 | 同步安全区 helper 与被截切按钮的 dialog 路径，复用现有 ThemeDialog 算法；避免覆盖同文件的本地主题 token、焦点和背景实现。 |
| `4c00f2dae8bab42af799a05fd104afdd78bf2c8a` | 自适应视频轨可能同时处于 selected，TV 轨道菜单无法指出正在解码的档位；本地 TrackDialog 当前未用 active video format。 | 同步唯一活动轨识别和焦点优先于 selected 的文字状态；不改受保护的 `dialog_track.xml` / 第二字幕测试，验证副字幕选择状态仍保持原语义。 |
| `541220ac7cdb5472cd6570ccd5c0ca6031551727` | 一键同步异步上传/取消回调没有操作代际，旧回调可能重置新请求 UI 或覆盖活动 Call。 | 只移植代际与销毁守卫；不采用 `b6f8e0...` 的两段式方向 UI。 |
| `e1cbbb77353f438bb96d081041183a502a0833b9`、`fbd31c11f4fe73b3f3377d46b68bf94841e89142` | Leanback 无配置空态会隐藏首页入口，首次安装后用户无法进入其他设置；本地显示“添加接口”单一空态。 | 同步保持导航/设置可达的最小行为，沿用既有配置管理入口；不新增第二个配置入口。 |
| `f5d08659fcd9b0e3195359168a98a185c71ab56b` | TV 更新弹窗焦点环与 About 齿轮图标可见性问题。 | 同步只修复缺失的控件状态，不替换本地主题绑定与 Focus 色语义。 |
| `ad21137c0fac31f6ac2154670900b55f977bb753`、`b414bc3444db0386d68a1d921689443b792a6e5a` | 同步/观影记录弹窗暗色面板文字使用本地 `ThemeDialogSemantics` / `ThemeTokens`，能解析自定义面板语义；上游改为直接读取 ThemeController。 | 本地等效并保留本地方案；不将直接颜色替换成上游默认颜色。 |
| `2711f89eb25ff7ce32786429973ac7095771b165`、`7eb229f50aaaf131b230d59bf011aa267d6b5f56`、`231bbea99bab2f11f9644e02459e14a860bcfed2` | 搜索范围与播放器显示项含焦点、选中、按下等重叠状态；当前 Selector 的 pressed 分支可覆盖焦点，键盘键底对比度偏低。 | 同步状态优先级和经目标测试核对的 TV 键底对比度；保留本地主题色与焦点 Token，不全局统一 Focus/Selected/Pressed。 |
| `49b28bd91f9e74b43d1963df84c1f56f8731bf80` | 上游把多个 dialog 的主按钮统一改成 `primary_container/on_primary_container`，覆盖范围 46 个布局。 | 不整体同步；按上面具体失效控件做窄修复，保留本地 Focus 环和用户自定义主题色。 |
| `29d357d882d4fde85a2ae97fa2fd5921a5479fd4` | 新增第 17 个 `playerScrimOpacity` 主题槽。 | 排除：新增独立主题能力。 |
| `b6f8e0fda7f92e24e9f8a2ea045d1eee55fd26e5`、`3f30bebe7ea97dff6ffcdc8e82649e8a8d970e1a` | 一键同步改为两段式推/拉方向选择。 | 排除：新增交互；独立代际并发修复按 `541220ac` 移植。 |
| `ad7f5834de429a35fb729c27744110226d800c0c`、`0a530b82ae48d1b9a91b6aa7ad8f2360c313de08` | 新增“切换线路不自动播放”选项及与之绑定的播放流保留逻辑。 | 排除：新增设置与行为；不移植依赖该设置的补丁。 |
| `ba711e721f1ae504dfed49e210e3a37046011757`、`befa6c60ed97321c4aad327ea926e8f0fe435b3e`、`15e8f6af48c97fd3078b472b5d9735e91fab5d74` | 新增缓存管理页面/手势与策略。 | 排除：独立缓存管理能力及其布局、文案改动。 |
| `26e77bb7280e80c8bdea6fdb6001febbf6b2d6e0`、`d9a0cc5c69cd2aca44b5f4627fb67ad584b9121b`、`4e8fa44848361f0eab2b9c4b1534f74b1df49539` | 直播源名称按钮、播放器按钮设置联动、平板旋转和轨道重算修复。 | 排除：依赖新增直播按钮/UI 设置，不扩展 Release 功能。 |
| `760f387de3ce2b24060828a41f323b51affb1b77` | 片尾向上键快捷焦点。 | 延期：属于新增遥控器捷径；需真机验证与现有片头焦点记忆交互。 |
| `ae0c82e7b66fbd452803525f1f0f85dd68c3e740` | 全屏菜单键新增下一集动作。 | 排除：新增遥控器动作。 |
| `759860e70b640dbd7b178145f0c10e1aafd79966`、`eaf0ba87c2cca1f32e6f780b9224f5c13ae67da7` | 长按分类键重置筛选。 | 排除：新增低频遥控器交互，且本轮明确不继续实验室/TV 低频遥控优化。 |
| `7faf6cea93026f477f0fefc29e0a20555126db9b`、`36f5ba4f8de75a8627a496ad6bcc6fb35a709c7c`、`4ba8f431c0c16218756e6a4a36662914b4b93886`、`14a45844fd6700f3d3cb932d68e97cd127609a28`、`fd5bcf109ee4e5de5fbeaa7d31242806bc76b5e3`、`36c6ed5324c2da61fbdef90748376bc95d7fa521` | Exo 帧迟到阈值由 5ms 调到 15ms，并扩展输入丢帧/诊断统计。 | 延期：改变丢帧调度与诊断口径；需要同设备视频负载 A/B，静态测试不能证明播放流畅度或掉帧改善。 |
| `e90de26e630b83ca85c84423a6029e2949bfc45a`、`d6b5d4cf4f0dc357c11da422a681851ce608951c` | 全局 BaseActivity 改为 TV 内容铺满窗口、修复特定屏幕 180px 黑带。 | 延期：全局窗口/insets 影响所有页面，无法从现有静态证据确认当前 TV 兼容；先完成局部 dialog 安全区修复并真机测量。 |

## 阶段记录

### 阶段 0：施工前核验与全量审计

- `main`、HEAD、C53 祖先关系与固定上游范围已核实。
- 134 个提交与 272 个最终净改路径均已入台账；合并 wrapper 不重放。
- 本地未提交修改已登记 33 路径并纳入 task guard 保护；C54 目前没有触碰这些路径。
- 施工、测试、构建与提交记录继续追加于本报告。

### 阶段 1：Leanback 外观/配置重建事务守卫

- 对应上游：`cae6e4b6e9405ce34b1b29391aa4406a7c82acc4`，最终评审 `bccb5b5a35dd7fc5a74cf3d8a38be844c6ba1416`，PR #477 / merge `d72134c5aa42047612ebd5525e6d82cbbb8f1f4b`。
- 修改：`HomeActivity` 在 FragmentManager 已保存、Activity 正在结束或已销毁时不再提交 hide/remove 事务；存活且仅状态已保存的实例会记录分类清理与首页刷新，回到前台后先清理旧站点分类 Fragment，再合并执行刷新。多次刷新折叠成一次，并用 OR 保留 `forceNative` 请求。未使用 `commitAllowingStateLoss`；TV 分类选择、WebHome/原生来源选择及本地焦点恢复结构保留。
- 回归：新增 `HomeActivityDeferredFragmentWorkTest` 3 项，验证事务守卫、延迟刷新合并、回前台清理/刷新顺序；命令 `:app:testLeanbackArm64_v8aDebugUnitTest --tests com.fongmi.android.tv.ui.activity.HomeActivityDeferredFragmentWorkTest --no-daemon` 通过，3/3，无失败。该任务同时编译 `compileLeanbackArm64_v8aDebugJavaWithJavac` 成功。
- 设备验证：未执行；主题切换与旧回调竞态仍需 Leanback 真机/模拟器验收。

### 阶段 2：崩溃报告与临时诊断会话

- 对应上游：`30324abe36f4f11baf3e74b8f1659f9c0c0c4a02`，最终评审 `c5ef7ec829b75ad3291b6ae5b11d26378d68090a`，PR #475 / merge `2f83884f2145259b740d7f8840d3a257b3a7144d`。
- 修改：`Startup` 在 CAOC 崩溃处理器安装后包裹异常处理器，崩溃进程以原子写入保存最多 3 份、每份有界且脱敏的报告；`:error_activity` 只读取 5 分钟内的新报告，并只保留崩溃 PID 的 logcat 行，PID 不明时导出空尾部。调试日志偏好关闭时强制使用内存持久层，关闭旧 writer 且不写回用户偏好；修复现有缓冲已停用时仍可能沿用磁盘持久层的边界。现有调试页在临时会话中标注状态并隐藏清空/开关采集操作。崩溃 Activity 存活期间提供本机及局域网地址、复制/打开动作，结束时关闭 Server；报告失败不替代 CAOC 原处理器。
- 本地兼容：将日志入口最小加入现有 Mobile / Leanback 崩溃页，保留两端原有按钮类型与视觉样式，并加可滚动容器；错误详情继续走本地 `WebHtvAlertDialogBuilder`。未引入上游 EXO 原始 Throwable logcat 输出。现有 `/debug/logs` LAN HTTP 端点不新增认证，用户应只在可信局域网使用并避免转发地址。
- 修改路径：`Startup.java`、`DebugLogs.java`、`CrashActivity.java`、`CrashLogSession.java`、`CrashLogText.java`、`CrashReport.java`、`DebugLogStore.java`、`InMemoryDiagnosticFile.java`、两套 `activity_crash.xml`、三份 crash strings 资源，以及 `CrashLogTextTest` / `CrashLogWiringTest`。
- 回归：两端各运行 `CrashLogTextTest` 10 项与 `CrashLogWiringTest` 8 项，均通过；两端 ARM64 Debug Java 编译通过。Leanback 全量任务共 4,287 项，3 项失败均在受保护的 `MihomoRoutingIntegrationTest`：`loopbackControllerAndLocalInterfaceNeverLoopThroughProxyOrWildcard`、`explicitWildcardStillWinsOverAutomaticDefault`、`appAndSiteExplicitRulesRetainPriorityAndPreferencesRemainUnchanged`，错误为 Gson 无法反射访问 JDK `java.net.Proxy#type`（`InaccessibleObjectException`，测试位置 `MihomoRoutingIntegrationTest.java:85`）。这些测试/代理文件未被 C54 修改；未在基线单独复跑，因此不把它归因为 C54，也不宣称已证明环境根因。
- 设备验证：未执行；需在 Mobile 与 Leanback 验收崩溃后日志服务启动、复制/打开地址、地址在前后台流程中的可用性及 Activity 结束后端口关闭。

### 后续阶段

播放器、TMDB、Dialog/UI 尚未施工；按领域分别记录文件、兼容适配、定向用例和结果。C54 不推送、不更新 Suite gitlink、不改 WOGG 或 WebHome。

## 最终交付状态

```ini
BRANCH = main
C53_MERGED = YES
LOCAL_BASE_HEAD = ad46fe5da81eacbfbd0465de6b0b85450cf3780a
UPSTREAM_BASE = 93ea56a61df9fbe3acf0d345234f0a97c3bcfab3
UPSTREAM_TARGET = d9da14dcb77e84d4beb158e83efa74124a1116f7
C54_INTEGRATED = PENDING
C54_DEFERRED = SEE CANDIDATE DECISIONS
THEME_CRASH_FIX = PENDING
MIHOMO_CRASH_STATUS = UNCHANGED; INDEPENDENT DIAGNOSIS REQUIRED
PLAYER_REGRESSION = PENDING
MOBILE_DEBUG_BUILD = PENDING
LEANBACK_DEBUG_BUILD = PENDING
FINAL_MAIN_HEAD = PENDING
WORKTREE_STATUS = 33 PRE-EXISTING PROTECTED PATHS; C54 CHANGES PENDING
PUSHED = NO
READY_FOR_RELEASE_FINALIZATION = PENDING
```

## 附录：固定范围完整 134 提交账本

下表逐条列出 93ea56a..d9da14d 的 Git 提交图，包含 51 个 merge、32 个文档提交、3 个测试专用提交和 48 个源码/混合提交。merge 记录保留完整 SHA 与主题，但不机械重放；其有效代码按上方功能组的最终目标树实现审查。文档提交不改变 App 运行行为；测试专用提交作为对应行为的验收证据，不单独搬运实现。每个源码/混合提交均给出本轮处置。

| # | 提交 | 类型 | 主题 | 处置 |
| ---: | --- | --- | --- | --- |
| 1 | b6f8e0fda7f92e24e9f8a2ea045d1eee55fd26e5 | 源码 | feat(sync): 一键同步改用推送/拉取两段式方向选择 | 排除：新增两段式同步方向界面。 |
| 2 | ad21137c0fac31f6ac2154670900b55f977bb753 | 源码/混合 | fix(ui): 观影记录同步弹窗深色模式改用语义 token | 本地等效：保留 ThemeDialogSemantics/ThemeTokens。 |
| 3 | 3f30bebe7ea97dff6ffcdc8e82649e8a8d970e1a | 源码 | fix(sync): 修复方向选择器的评审缺陷 | 排除：两段式同步界面评审。 |
| 4 | b414bc3444db0386d68a1d921689443b792a6e5a | 源码/混合 | review(ui): 复评观影记录同步弹窗深色模式改动并加固守卫 | 本地等效：继续使用本地主题语义实现。 |
| 5 | f5d08659fcd9b0e3195359168a98a185c71ab56b | 源码/混合 | 修复电视版版本更新弹窗的焦点环缺失与关于弹窗齿轮图标不可见 | 同步：TV 更新弹窗焦点环/About 齿轮可见性。 |
| 6 | 4f3e942bb3cc50b64bc7d3f47e3e7e0bdac1e11d | 文档 | 记录 TV-FOCUS-RING-DIALOG-20261019 交付坐标 | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 7 | 0e81bae1c7d1a91cd229dc5e1663d62f202548b3 | 测试专用 | review(ui): 补全观影记录同步守卫契约并消除配平器静默失准 | 不单独移植实现；保留其约束并在对应最终功能组补做本地回归。 |
| 8 | 0bb2601e3871f58b727873837112abe86784d61f | merge | Merge pull request #461 from Silent1566/dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 9 | f6e122c03847e1fc8c5ef83acdad0cbc3e024609 | merge | merge(C81): 合并上游 Silent1566 一键同步两段式方向选择器并保留主题 token 化 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 10 | c88f2896ac00d4ea663d7a56b4c16dfa4274a1cf | 文档 | docs(C81): 回填评估索引交付坐标与验证结论 | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 11 | 49b28bd91f9e74b43d1963df84c1f56f8731bf80 | 源码/混合 | fix(tv): 主按钮聚焦态改用 container 配对，焦点环不再与填充撞色 | 暂缓：46 个布局的主按钮焦点/配色大改不整体套用。 |
| 12 | 1b35b6e3e80554e79654455a3d8a986bb1236b66 | 文档 | docs(tv): 记录第二轮 container 配对方案的交付坐标与真机像素证据 | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 13 | ad7f5834de429a35fb729c27744110226d800c0c | 源码/混合 | feat(player): 新增「切换线路不自动播放」开关，手动切线路只刷选集列表不起播 | 排除：新增线路切换不自动播放设置。 |
| 14 | c0b1ffd3c2d048e7e975a85cb480dc7589f18c9b | merge | merge: 合并 origin/beta 并复评 TV 焦点环改动 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 15 | 4076b78a11aff097ebb48cc834f4fc5ace1a0736 | 文档 | docs(C83): 记录合并提交、recovery tag 与 PR #462 交付坐标 | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 16 | 13a093ad3745b85f6cece8e501d45cb76762b917 | merge | merge+review(C84): 合并 origin/beta 0bb2601e38 并复评合并带入改动与 dev3 未推送 4 提交，修复 10 项 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 17 | a004e4a18ec3d3cb903283f3f77a7e1dfb1e1581 | 文档 | docs(C84): 回填交付坐标（合并提交 13a093ad37、recovery tag、PR #463） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 18 | e0cb0ea2d53a48cb2ed463ba169e2712c405c238 | merge | Merge pull request #462 from Silent1566/dev4 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 19 | c5323524c1b3c2cb27314e7365abfefc5f726876 | merge | merge: 合并 origin/beta e0cb0ea2d5（PR #460-#462）并复评 dev2 未推送提交 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 20 | 70a3fe757fb8ae78866d35e0b9a0ec7389d7c6f0 | 测试专用 | review(C84): 修正 beta 焦点环契约在 CRLF 检出下恒空转并复评 dev2 未推送提交 | 不单独移植实现；保留其约束并在对应最终功能组补做本地回归。 |
| 21 | 5591f92612990c779a0bc8d7d082ac22e01388c0 | 文档 | docs(C84): 记录交付坐标（合并提交 c5323524c1 / 复评修复 70a3fe757f / recovery tag / 推送 dev2 / PR #464） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 22 | f289207bcee7b4bdc978262f775197a641e520c6 | merge | Merge pull request #464 from Silent1566/dev2 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 23 | 4e7b24b5f7562e22b7306bc4417eff3cc249ead1 | merge | merge+review(C84): 合并 origin/beta e0cb0ea2d5（TV 焦点环第二轮）并解决 2 处冲突，修复 beta 侧 CRLF 契约缺陷 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 24 | f70fa8c5fd2ebb289f2c161b6502ff5ce9e9d1a2 | merge | merge+review(C84): 合并 origin/beta f289207bce（dev2 线路切换开关）并解决 2 处冲突（CRLF 测试取 beta 版、C84 三次让位改 C86） | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 25 | ee8b058a972577e282ed556ed9f0c4defec1869c | merge | Merge pull request #463 from Silent1566/dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 26 | e1cbbb77353f438bb96d081041183a502a0833b9 | 源码/混合 | fix(home): 无配置时保留导航，引导并入导航栏，不再全屏接管首页 | 同步：Leanback 无配置时保留导航可达。 |
| 27 | fbd31c11f4fe73b3f3377d46b68bf94841e89142 | 源码/混合 | fix(home): 去掉无配置时额外插入的「添加接口」入口 | 同步：移植无配置导航修正，不新增重复入口。 |
| 28 | 2b098a691b6b6a36a8b64984c6673bcc2674b315 | merge | merge: 合并 origin/beta ee8b058a97（PR #463-#464）并复评 dev2 未推送的 2 个首页导航提交 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 29 | e79ed9058aab6cfce6f9433116e673cb89aeb140 | 源码 | fix(player): 硬解保留 ffmpeg 视频回退，避免 AVS3/无平台解码器的源只有声音没有画面 | 同步：按现有回退开关启用 Exo FFmpeg 视频后端。 |
| 30 | 541220ac7cdb5472cd6570ccd5c0ca6031551727 | 源码/混合 | review(C87): 修正首页导航交付文档失真并给一键同步陈旧回调补代际守卫 | 同步：只移植 OneKeySync 异步代际守卫。 |
| 31 | 4a85e49fd2b9b6064dc41a4ee4b7be49d08e6dcd | 文档 | docs(C87): 记录交付坐标（合并 2b098a691b / 修复 541220ac7c / recovery tag / 推送 dev2 / PR #465） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 32 | 77e2fbefdc30c501c3646b066826a714f54b3f38 | merge | Merge pull request #465 from Silent1566/dev2 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 33 | 48f59db5ae8e577cd0fba8c25be787876882d022 | 源码 | fix(player): 直播 manifest 空体/解析失败按临时失败重试，避免 EXO 一次空响应就整会话失败 | 同步：限制 HLS manifest 暂时失败重试。 |
| 34 | 2d403b2a31fe96adfd4eb6fb9d848168d598d433 | 源码 | fix(player): EXO 播放失败原因无条件写 logcat（release 下 SpiderDebug 关闭导致排障无证据） | 排除：原始异常写入 logcat 有 URL/路径泄露风险。 |
| 35 | 203bb7d567ec56960aa2276e865db680b983b020 | 源码 | fix(player): 取流失败(网络/解析/本机端点)不再伪造换核，改为有限次重试本项 | 同步：仅对短暂网络/本机端点错误重试当前源。 |
| 36 | 8d47a5dd34618af520ba8f01c2d6aaa6a28f78e7 | 源码 | fix(player): 分片伪装成 .m3u8 时按内容判定，不再当播放列表改写（EXO 直播黑屏根因） | 同步：以分片内容类型/内容嗅探优先于 .m3u8 后缀。 |
| 37 | 760f387de3ce2b24060828a41f323b51affb1b77 | 源码/混合 | fix(tv): 恢复片尾区间上键快捷聚焦，片头记忆焦点语义不变 | 暂缓：新片尾上键快捷操作，等待 TV 设备验证。 |
| 38 | 977ddf4343882cffe3623a126e76bc490f884c29 | merge | merge+review(C88): 合并 origin/beta 77e2fbefdc 并复评未推送 5 个播放器修复，修复 10 项 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 39 | cf828bf8a70150597f0c33418329d66c7d3d86db | 文档 | docs(C88): 回填交付坐标（合并 977ddf4343 / recovery tag / 推送 dev3 / PR #466 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 40 | b89af5e548930563757c41059562fd5029c3c160 | merge | Merge pull request #466 from Silent1566/dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 41 | 26e77bb7280e80c8bdea6fdb6001febbf6b2d6e0 | 源码/混合 | fix(live): 控制栏直播源按钮显示当前直播源名称 | 排除：新增直播源名称按钮。 |
| 42 | 5351ade5be4c2ca4b59b770311a49cb36759fd3b | merge | merge+review(C89): 合并 origin/beta b89af5e548 并复评未推送 26e77bb728，修复直播源名称按钮的溢出与无障碍语义 2 项 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 43 | 3a170afeec85df5340bad9c92bd02a9d370da4bb | 文档 | docs(C89): 回填交付坐标（合并 5351ade5be / recovery tag / 推送 dev3 / PR #467 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 44 | 687a6b65da9597b10a27ed2cb4c356283cd5f1f2 | merge | Merge pull request #467 from Silent1566/dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 45 | 944dd6419fee496b1e3237d93e2b9b0f0c84f0fd | 文档 | docs(TV-ENDING-SHORTCUT-20261021): 记录 5557 真机实测证据（片头/片尾/片头语义三例） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 46 | dbed4ad5439c8986df0588209c0726dc5398f01c | merge | merge+review(C90): 合并 origin/beta 687a6b65da 并复评 dev2 未推送的片尾快捷聚焦修复 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 47 | 7faf6cea93026f477f0fefc29e0a20555126db9b | 源码/混合 | fix(exo): 对齐上游帧调度默认值并暴露解码器排除状态 | 暂缓：帧调度阈值与诊断口径需要设备 A/B。 |
| 48 | 35d6dae6b69bf2f117c895942156df42842583f6 | 文档 | docs(C90): 回填交付坐标（合并 dbed4ad543 / recovery tag / 推送 dev2 / PR #468） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 49 | 2c3af2f009430db2e595941de7b53fed37f62718 | merge | Merge pull request #468 from Silent1566/dev2 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 50 | 0a530b82ae48d1b9a91b6aa7ad8f2360c313de08 | 源码/混合 | fix(player): 切换线路不自动播放时保留当前播放流 | 排除：依赖新增设置的线路切换行为。 |
| 51 | 2711f89eb25ff7ce32786429973ac7095771b165 | 源码/混合 | fix(tv): 修复搜索范围下拉焦点行白字压白底并新增焦点状态对对比度守卫 | 同步：搜索范围焦点文字对比度窄修。 |
| 52 | a8ae5e8602afd47c5a2079a9f1a79d215f1a08f7 | merge | merge+review(C91): 合并 origin/beta 2c3af2f009 并复评未推送 7faf6cea93，修复丢帧阈值开关失效等 4 项 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 53 | 8a58112b53dc27fb668fe7e6cd51db6c2c8af018 | 文档 | docs(C91): 回填交付坐标（合并 a8ae5e8602 / recovery tag / 推送 dev3 / PR #469 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 54 | a0641ef0e22e6b1353ecfbc5f86c93c34f91c051 | merge | Merge pull request #469 from Silent1566/dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 55 | 7eb229f50aaaf131b230d59bf011aa267d6b5f56 | 源码 | fix(tv): 搜索键盘键底上调到 black_40 达 4.5:1，并记录真机像素采样结果与采样脚本 | 同步：搜索键盘按键底色对比度修复。 |
| 56 | 08618b49ec1177344f9f389f47e596cdef022962 | merge | merge+review(C92): 合并 origin/beta a0641ef0e2 并复评 dev2 未推送 0a530b82ae，修复抑制分支漏作废在途取流 1 项 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 57 | fa7392a371572e85a5e2239630bd3784b1ee89d4 | merge | Merge remote-tracking branch 'origin/beta' into dev4 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 58 | 84c6d3bf1544dc184c842825caaae0dc838cdc61 | 文档 | docs(C92): 回填交付坐标（合并 08618b49ec / recovery tag / 推送 dev2 / PR #470 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 59 | 1983d649324c823348085acaedc3cebd402c4177 | merge | Merge pull request #470 from Silent1566/dev2 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 60 | 231bbea99bab2f11f9644e02459e14a860bcfed2 | 源码 | review(C92): 复评 TV 焦点对比度两提交，修正屏显芯片按下态顶掉焦点环、采样脚本空跑即通过 | 同步：修正焦点/按下态优先级与对比度断言。 |
| 61 | 0f7e833b59f602847a09bf3e539e976013f9d94b | 文档 | docs(C92): 回填交付坐标（合并 fa7392a3715 / recovery tag / 推送 dev4 / PR #471 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 62 | 3964eeae7ccf4f10f5a2e3696a2909c7a92b3952 | merge | Merge pull request #471 from Silent1566/dev4 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 63 | ba711e721f1ae504dfed49e210e3a37046011757 | 源码/混合 | feat(cache): 短按直接清理、长按打开缓存管理 | 排除：新增缓存管理手势/能力。 |
| 64 | befa6c60ed97321c4aad327ea926e8f0fe435b3e | 源码 | fix(cache): 缓存行去掉两行提示，恢复与相邻行对称的单行布局 | 排除：缓存管理页面布局。 |
| 65 | 15e8f6af48c97fd3078b472b5d9735e91fab5d74 | 源码 | docs(cache): 同步缓存行手势反转后遗留的陈旧注释 | 排除：只涉及缓存功能说明/陈旧注释，不改运行行为。 |
| 66 | d9a0cc5c69cd2aca44b5f4627fb67ad584b9121b | 源码/混合 | fix(live): 直播按钮跟随播放器按钮设置并支持平板全屏旋转 | 排除：依赖新增直播按钮/设置和旋转行为。 |
| 67 | 560d5e626315069050e5b20721e92a2810cf3207 | merge | Merge remote-tracking branch 'origin/beta' into dev1 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 68 | 4e8fa44848361f0eab2b9c4b1534f74b1df49539 | 源码/混合 | merge+review(C93): 合并 origin/beta 3964eeae7c 并复评 LIVE-UI-01，修复唤出控制栏未重算轨道按钮 | 排除：评审修复属于被排除的新增直播 UI 组合。 |
| 69 | 225d2bbcce3d2f947c191ff83bd400d10b55c0e4 | 文档 | docs(C93): 回填交付坐标（合并 560d5e6263 / recovery tag / 推送 dev1 / PR #472 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 70 | 8f7acb519c4cf91df0441c7d61253e6d1c87e6cd | merge | Merge pull request #472 from Silent1566/dev1 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 71 | 5262c92e3fa925a5a84bdfcf27665325422bd11d | merge | merge(C93): 合并上游 webhtv/webhtv Silent1566 缓存行手势反转（短按全清/长按管理面板） | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 72 | 3e73bd375378bf60023644f770191e25f70669b8 | 文档 | docs(C93): 回填交付坐标（合并提交 5262c92e / recovery tag / 上游 tip 15e8f6af） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 73 | bf0566640cee3e1790a9ab019a3404b6be6fc4be | merge | merge+review(C94): 合并 origin/beta 8f7acb519c 并复评 C93 缓存行手势，修正反向后遗留的两处陈旧注释 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 74 | 9c7b3ce1fbe5839d977aef3adf46eb91ca1ec92b | 文档 | docs(C94): 回填交付坐标（合并提交 bf0566640c / recovery tag / 推送 dev2 / PR #473 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 75 | 590c424568d5d1eaa713fe8a06c6c57cd013f42c | merge | Merge pull request #473 from Silent1566/dev2 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 76 | 1a16fd163c38e698616fba28dff64a71d879f194 | 源码/混合 | perf(tmdb): 消歧改拉 lite 详情并限并发，详情页图片按官方档位降档 | 同步：TMDB lite 消歧；保留本地图像质量。 |
| 77 | fa2f31812e55d2f79b175c6d0c579cefe6ab3300 | 源码/混合 | feat(tmdb): identity 已知时并行预取详情，并按裁决放宽 C16 预热请求契约 | 暂缓：预取会扩大 C16 请求契约并在源 payload 前发请求。 |
| 78 | 30324abe36f4f11baf3e74b8f1659f9c0c0c4a02 | 源码/混合 | feat(crash): 崩溃界面改为可复制的局域网日志地址，未开启调试日志时自动建立临时崩溃会话 | 同步：崩溃诊断会话、脱敏日志与 LAN 地址。 |
| 79 | 0c2d6d9ac6d7d030d2d54237f3bf1808448102e6 | merge | Merge remote-tracking branch 'origin/beta' into dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 80 | ae0c82e7b66fbd452803525f1f0f85dd68c3e740 | 源码/混合 | feat(tv): 全屏菜单键新增下一集动作 | 排除：新增下一集菜单动作。 |
| 81 | 573c8e5114d988c2285416785d2ea651f04916df | merge | merge+review(C95): 合并 origin/beta 590c424568 并复评全屏菜单键「下一集」动作 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 82 | b8fdbee6f6e8a16bb2236ee7b347b80fef79156f | 文档 | docs(C95): 回填交付坐标（合并提交 573c8e5114 / recovery tag / 推送 dev2 / PR #474 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 83 | c5ef7ec829b75ad3291b6ae5b11d26378d68090a | 源码/混合 | merge+review(C95): 合并 origin/beta 590c424568 并评审崩溃日志地址功能，修复小屏崩溃界面不可滚动等 4 项 | 同步：崩溃诊断最终评审与小屏滚动修复。 |
| 84 | 5fa1a242569fe12506e2e31ddaabeb62148bf462 | 文档 | docs(C95): 回填交付坐标（合并 0c2d6d9ac6 / recovery tag / 推送 dev3 / PR #475 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 85 | 67e1f6330ac3ef11e8ede56a81fea4efcd6597fc | 文档 | docs(tmdb): 回填详情页设备实测 A/B 证据（W1 成对 −399/−508/−1378ms） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 86 | 2f83884f2145259b740d7f8840d3a257b3a7144d | merge | Merge pull request #475 from Silent1566/dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 87 | 4dc58561bb1e27c8baa6e54be18287af0b5616dc | merge | Merge pull request #474 from Silent1566/dev2 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 88 | da7ac6440a9d1da247bcabdb4f2ed291bf29001c | 源码/混合 | fix(auto-next): 自动连播有声音没画面 —— 归属键按会话等价、Surface 绑定判定可测化并补绑 | 同步：会话键等价与安全 Surface 重绑。 |
| 89 | c9b2e769a87ad969b0972aa6fb11b33e72b0a38c | 文档 | docs(tmdb): 记录 W5 主线程渲染去重的实测结论（中位 164→202ms，已回滚） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 90 | cae6e4b6e9405ce34b1b29391aa4406a7c82acc4 | 源码/混合 | fix(home): 首页配置回调在新实例状态已保存后不再提交 Fragment 事务 | 同步：首页 Fragment 状态保存事务守卫。 |
| 91 | 7fe44116aafc02c580d7ddf80ba82708878257d2 | 源码/混合 | perf(tmdb): 详情页等待 TMDB 结果的阻塞段移出共享 5 线程池 | 本地等效：TMDB 等待已移出共享 5 线程池。 |
| 92 | b5d87c16b5fb51cbe5e79c504592a0fba7d89c74 | 文档 | docs(E-SP11): 记录 10-21 测试版同文件掉帧回归证据、设备分组操作单与上游口径复核 | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 93 | 41f55daa17d272b0a51602a9998df1956a862d5d | merge | merge+review(C96): 合并 origin/beta 4dc58561bb 并评审 AUTONEXT-02 自动连播修复 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 94 | d29f8563b3c0b4f57ab51237d5b2a54ae66c1d10 | 文档 | docs(C96): 回填交付坐标（合并提交 41f55daa17 / recovery tag / 推送 dev1 / PR #476 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 95 | 3203ca9da087c42cb569770140c9d6c2e0837078 | merge | Merge pull request #476 from Silent1566/dev1 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 96 | 36f5ba4f8de75a8627a496ad6bcc6fb35a709c7c | 源码/混合 | feat(E-SP11): 帧调度生效配置写入诊断日志并统一迟到阈值取值口 | 暂缓：帧调度诊断扩展依赖设备 A/B。 |
| 97 | 4ba8f431c0c16218756e6a4a36662914b4b93886 | 源码/混合 | feat(E-SP11): 补齐掉帧归因证据（输入侧丢帧计数、每帧迟到分布、吞吐口径标注） | 暂缓：新增帧丢失归因统计非当前 Release 必要项。 |
| 98 | 14a45844fd6700f3d3cb932d68e97cd127609a28 | 源码/混合 | fix(E-SP11): 迟到分布随诊断 tick 周期输出，并更正帧回调频率表述 | 暂缓：诊断统计输出时序需设备确认。 |
| 99 | 6a36571e51be6ab5fff30e413eb63761ce40ec6d | merge | Merge remote-tracking branch 'origin/beta' into dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 100 | bccb5b5a35dd7fc5a74cf3d8a38be844c6ba1416 | 源码/混合 | merge+review(C97): 合并 origin/beta 3203ca9da0 并评审首页延迟配置回调的事务守卫，存活宿主改为回前台补做 | 同步：吸收首页延迟回调最终评审修正。 |
| 101 | fbcd47fbc1a49b004a9f4d2026a91cb3a1b56c75 | 文档 | docs(C97): 回填交付坐标（合并 6a36571e51 / recovery tag / 推送 dev3 / PR #477 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 102 | d72134c5aa42047612ebd5525e6d82cbbb8f1f4b | merge | Merge pull request #477 from Silent1566/dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 103 | ae923e13125c536f06f940409661fead798e1dfe | merge | merge+review(C97): 合并 origin/beta 3203ca9da0 并复评 E-SP11 掉帧归因，修复输入侧丢帧计数未进诊断白名单导致整条 video.output.summary 被丢弃 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 104 | 20f37e87be501b2ac573689a82659f938c78c8f6 | 文档 | docs(C97): 回填交付坐标（合并 ae923e1312 / recovery tag / 推送 dev2 / PR #478 / 拉取最新）并在评估索引补检查点 64 | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 105 | dec93c43bf85b42c6920a8ce262f5632e63bd9ae | merge | Merge pull request #478 from Silent1566/dev2 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 106 | 1bb216efec156709ca3d1b027701c6331749d986 | 源码/混合 | fix(site-health): 报表弹窗按系统栏安全区定窗口尺寸，修复「取消」被导航栏裁切 | 同步：SiteHealth 报表按系统栏安全区定尺寸。 |
| 107 | fb7028f7a42dce5f6848807a7044727fee9feb07 | merge | Merge remote-tracking branch 'origin/beta' into dev4 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 108 | 4c00f2dae8bab42af799a05fd104afdd78bf2c8a | 源码/混合 | fix(tv-track): 焦点与选中视觉分离，视轨只高亮正在解码的档位 | 同步：焦点与视频轨活动/选中语义分离。 |
| 109 | 389a9a4148eaf64440ecb3b3ccf9f73d62d0bdad | 文档 | review(C98): 合并 origin/beta dec93c43bf8 并复评 5 个未推送 TMDB 详情性能提交，修正任务文档与实现矛盾 6 项 | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 110 | ca24fe1d0cdad87d3a6ba14afb6e89888aee88fd | 文档 | docs(C98): 回填交付坐标（合并 fb7028f7a42 / recovery tag / 推送 dev4 / PR #479 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 111 | 275c225edc5dd555d17bfaf13a44f64cdd3a6f95 | merge | Merge pull request #479 from Silent1566/dev4 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 112 | 29733de98d32760a4e5a743d4e420b48f5d59f61 | 源码/混合 | fix(dialog): 全屏/近全屏弹窗统一按系统栏安全区定尺寸，修复底部按钮被导航栏裁切 | 同步：近全屏 Dialog 底部按安全区约束。 |
| 113 | e90de26e630b83ca85c84423a6029e2949bfc45a | 源码/混合 | fix(tv-ui): 电视端内容区铺满整块窗口，修掉 overscan 屏底部 180px 黑带 | 暂缓：全局窗口铺满改动有跨页面 overscan/insets 风险。 |
| 114 | 39f44ccd1cf1360ce77314616f8be5b3d13397c7 | merge | Merge remote-tracking branch 'origin/beta' into dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 115 | 631877c7d4869b070b835e46d0c6e1cefd3d2f49 | 测试专用 | review(C99): 合并 origin/beta 275c225edc 并复评全屏弹窗安全区两提交，修正风险表述与契约测试覆盖 | 不单独移植实现；保留其约束并在对应最终功能组补做本地回归。 |
| 116 | fe9be226040df5cb1fc8b544a2d6efdf1bc30914 | 文档 | docs(C99): 回填交付坐标（合并 39f44ccd1c / recovery tag / 推送 dev3 / PR #480 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 117 | b9c39f85ec85ecf3522f91bbe469aac450638ef9 | merge | Merge pull request #480 from Silent1566/dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 118 | d6b5d4cf4f0dc357c11da422a681851ce608951c | merge | merge+review(C99): 合并 origin/beta 275c225edc 并评审两个未推送 TV 改动（视轨焦点/选中分离、内容区铺满窗口），修正 inset 守卫空转与注释比值 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 119 | 0f297ce6da00a92d56002965524aa5156b712e52 | 文档 | docs(C99): 回填交付坐标（合并提交 d6b5d4cf4f / recovery tag / 推送 dev1 / PR #481 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 120 | 4dda4b56b8d67e0cb78e6617f7b0f776591383a9 | merge | Merge pull request #481 from Silent1566/dev1 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 121 | 29d357d882d4fde85a2ae97fa2fd5921a5479fd4 | 源码/混合 | feat(theme): 播放菜单遮盖层强度接入主题色彩（第17槽 playerScrimOpacity） | 排除：新增第 17 个主题槽。 |
| 122 | 759860e70b640dbd7b178145f0c10e1aafd79966 | 源码/混合 | feat(tv): reset category filters on long press | 排除：新增分类长按重置操作。 |
| 123 | fd5bcf109ee4e5de5fbeaa7d31242806bc76b5e3 | 源码/混合 | fix(E-SP12): 掉帧按播放时长归一，暂停结算不再伪装成突发 | 暂缓：帧丢失归一及诊断统计语义需设备对照。 |
| 124 | 82a2731f4efb469b42d5f107bee647a38231e82e | 文档 | docs(E-SP12): 补记模拟器设备实测证据（暂停结算标注与 OSD 速率文案） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 125 | eaf0ba87c2cca1f32e6f780b9224f5c13ae67da7 | 源码/混合 | review(C100): 复评 TV 分类长按重置筛选并修 2 项（长按 latch 残留 / 无变化时重建面板） | 排除：评审的是被排除的分类长按新操作。 |
| 126 | 3b02e02c07f30a5ee9361f548d94cd4486a64749 | 文档 | docs(C100): 回填交付坐标（评审提交 / recovery tag / 推送 / PR #482 / 拉取最新） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 127 | ea678c0fd4401464826f45895846f9f9d2c2eeef | merge | Merge pull request #482 from Silent1566/dev3 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 128 | dcf9828cba616f07ea7b21ebc4053b691522a208 | merge | merge: 拉取远端beta最新代码 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 129 | 7443e1e474226f722980991b1e5b7a1d243d4864 | merge | merge: 合并远端 beta（ea678c0fd44）到 dev4 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 130 | db4a96cec32c51ad68138ed7c8b4c106e71fa529 | merge | Merge pull request #483 from Silent1566/dev2 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 131 | 36c6ed5324c2da61fbdef90748376bc95d7fa521 | 源码 | review(E-SP12): 复评 E-SP12 未推送提交，修 PlayerOsdController javadoc 错位并补复核记录 | 排除：只修诊断文档/Javadoc，无用户可见运行修复。 |
| 132 | e377db6cf20493d5b8b607ca4f2eed74ed3c4468 | merge | merge: 合并远端 beta（db4a96cec32，PR #483 播放菜单遮盖层主题槽）到 dev4 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
| 133 | 0bccba71c5f86818c99a44114d3f0d58e7989481 | 文档 | review(C101): 合并 origin/beta db4a96cec32 到 dev4 并复评播放菜单遮盖层主题槽（20 路径 / 0 必修项） | 不移植到 App 运行代码；作为审计/发布说明记录。 |
| 134 | d9da14dcb77e84d4beb158e83efa74124a1116f7 | merge | Merge pull request #484 from Silent1566/dev4 | 保留合并历史；不单独 cherry-pick，依最终树和对应修复组审查。 |
