# APP-MIHOMO-AUTO-PROXY-REPAIR

## 目标与授权

用户已批准仅将实验室 Mihomo 自动接入 App 共享 OkProxySelector 的远程 HTTP 请求。显式 App/站源规则优先，Mihomo RULE 模式保留，停机恢复原策略。无 VPN/TUN、WebView/native player 改造，无提交/推送。App 基点 `ad46fe5da81eacbfbd0465de6b0b85450cf3780a`，main；初始未跟踪 `scripts/build-arm64.ps1` 受保护。Suite 原有改动不属于本任务。

## 实施决策与依据（2026-10-23，Asia/Shanghai）

问题已由 Suite 根目录 `APP-MIHOMO-TRAFFIC-ROUTING-AUDIT.md` 定位：LabRunner 只启动核心，实验室 env proxy 不会改变 JVM 共享网络路由。本轮不重复架构审计。

| 证据 | 版本/等级 | 支持的决定及限制 |
|---|---|---|
| [Mihomo 代理端口文档](https://wiki.metacubex.one/en/config/inbound/port/) | 官方文档，访问 2026-10-23，A | mixed 同时支持 HTTP/SOCKS；不能由端口数字猜协议 |
| [Mihomo configs API 源码](https://github.com/MetaCubeX/mihomo/blob/Meta/hub/route/configs.go) | Meta 分支访问快照，A；不是本地核心版本证明 | GET /configs 返回 executor 当前 General；只读获取监听配置，不 PATCH mode/rules |
| [Mihomo HTTP listener](https://github.com/MetaCubeX/mihomo/blob/Meta/listener/http/server.go) | Meta 分支访问快照，A | 单独 TCP 存活不足以表示 HTTP/SOCKS 协议就绪，需协议握手 |
| [OkHttp RouteSelector](https://github.com/square/okhttp/blob/parent-5.4.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/connection/RouteSelector.kt) | 与本地依赖 5.4.0 对齐，A | 现有 client 持有 selector，新 route 会重新 select；连接复用仍需处理 |
| [OkHttp RealRoutePlanner](https://github.com/square/okhttp/blob/parent-5.4.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/connection/RealRoutePlanner.kt)、[ConnectPlan](https://github.com/square/okhttp/blob/parent-5.4.0/okhttp/src/commonJvmAndroid/kotlin/okhttp3/internal/connection/ConnectPlan.kt) | 5.4.0，访问 2026-10-23，A | 池复用可能早于 select；SOCKS Socket.connect 还包含目标握手，不能把目标拒绝误判成本地核心退出 |
| 本地 OkProxySelector、OkHttp.closeIdleConnections、ProxySetting.apply、LabRunner/ProcManager | 上述 App HEAD，A | 独立 volatile 运行期槽位位于显式规则之后；只清空闲连接，不取消活跃 call |

其他证据类别：本任务不是移植上游补丁，故没有待接收的 PR/issue/revert；没有 native/ABI/dependency 变化，论文和性能基准不决定此局部路由合同。成熟相关实现以 Mihomo 实际 API handler 和本地既有 App Proxy/连接池机制为直接依据；不扩大研究。

备选比较：不改保持故障；全局 ProxySelector 或通配显式规则会扩大覆盖或覆盖用户规则，拒绝；采用用户已指定的窄适配：Lab lifecycle → 独立监视器 → 当前配置与握手 → 共享 selector runtime default。无新增依赖或 UI。

验收：未就绪不绑定；mixed/HTTP/SOCKS 可识别且协议验证；显式规则和持久化设置不变；本机直连；停止/异常/重启/端口变化撤销或更新；真实本地代理模拟收包；相关测试及 Leanback ARM64 Debug 构建；真机状态单独记录。回滚为撤销本任务的代码 diff，用户配置无需恢复。

### 已授权接入范围内的连接池补充

公共 `OkHttp.getBuilder()` 以及 App `WebCall` 的独立池需要登记到选择器；否则启动前创建的客户端可能继续复用旧直连空闲连接。本任务据此增加这两处登记，task guard scope 同步加入 `catvod/.../OkHttp.java` 和 App `web/WebCall.java`，初始受保护文件及 HEAD 不变。仅调用 `ConnectionPool.evictAll()` 处理空闲连接，活动 exchange 不被取消。

## 原始原因与修改后的链路

原实现只有 `LabRunner` 启动外部进程，Mihomo 配置/就绪状态没有进入 App 的 `ProxySetting` / `OkProxySelector`。实验室 `global_proxy` 仍仅对实验室子进程注入环境变量。本次不改变其含义。

```text
Mihomo 启动命令（展开后的真实参数）
→ LabRunner 注册进程 / LabProcManager 保存恢复信息
→ LabMihomoProxy 单一后台监视器
→ 读取 controller /configs 的实际配置（或保守 YAML fallback）
→ 回环 listener + 对应代理协议就绪验证
→ OkProxySelector 独立运行期默认代理槽位
→ 无显式代理规则的远程 HTTP(S) 新选路进入 Mihomo
→ Mihomo 原有 RULE / DIRECT / 节点选择
```

`ProxySelector.setDefault()` 未新增调用。未覆盖、清空或写入 `Setting.shell_proxy`、代理 URL/host rules 或站源配置。`ProxySetting.apply()` 仍仅管理自己的显式规则，运行期槽位不在这些规则列表中。

## 修改文件与关键位置

路径均相对 App 仓库 `E:\GitHub\webhtv-suite\app`。

| 文件 | 行/函数 | 修改 |
|---|---|---|
| `app/src/main/java/com/fongmi/android/tv/lab/LabRunner.java` | 74、108、177、401、430 | 启停/退出钩子；旧进程清理不移除新进程 stdin 或恢复条目 |
| `app/src/main/java/com/fongmi/android/tv/lab/LabProcManager.java` | 54、92、113、144、162、235 | 恢复启动元信息；按 PID/进程组校验旧回调归属；停止恢复进程时撤销绑定 |
| `app/src/main/java/com/fongmi/android/tv/lab/LabMihomoProxy.java`（新增） | 33 `saveLaunch`、43 `recover`、59 `start`、69 `activate` | Lab 到监视器/共享选择器的窄适配；单一 daemon worker、1 秒轮询间隔、日志和本机地址 |
| `app/src/main/java/com/fongmi/android/tv/lab/MihomoProxyConfig.java`（新增） | 39 `Launch`、103 `Detector`、139 `detect`、205 `liveConfig`、220 `candidates`、259 `handshake` | 参数、配置、端口/协议识别，读取实际配置及就绪验证 |
| `app/src/main/java/com/fongmi/android/tv/lab/MihomoProxyMonitor.java`（新增） | 24 `start`、30 `stop`、41 `processExited`、47 `poll` | 运行期状态机，generation/session 身份隔离和失败恢复 |
| `catvod/src/main/java/com/github/catvod/net/OkProxySelector.java` | 36 `registerRuntimePool`、52 `setRuntimeProxy`、134 `select`、168 `connectFailed` | 保留显式优先级，新增运行期 default、本机例外、连接失败处理和空闲池清理 |
| `catvod/src/main/java/com/github/catvod/net/OkHttp.java` | 248 | 公共 builder 的池以弱引用登记，不改变原 client/鉴权/DNS/拦截器 |
| `app/src/main/java/com/fongmi/android/tv/web/WebCall.java` | 33 | Native Bridge 的独立池登记，保留原网络配置与请求流程 |
| `app/src/test/java/com/fongmi/android/tv/lab/MihomoProxyConfigTest.java`（新增） | 配置及实际 loopback 端口/协议用例 | 10 项 |
| `app/src/test/java/com/fongmi/android/tv/lab/MihomoProxyMonitorTest.java`（新增） | 状态机与竞争用例 | 4 项 |
| `app/src/test/java/com/fongmi/android/tv/lab/MihomoRoutingIntegrationTest.java`（新增） | 选择器与真实 HTTP/SOCKS socket 用例 | 最终结果见验证节 |
| 本报告 | 全文 | 设计决定、交付证据与待验收项 |

没有修改 App Proxy 持久化实现、YAML、站源解析、播放器业务、WebHome/WOGG 子仓库、构建脚本或依赖版本。

## 端口识别与就绪判据

1. 只识别实验室 `mihomo` 项目中的普通核心启动命令，支持可执行文件绝对路径、`exec`、引号/空格、空字符串参数及 `-f`/`-d`、`-ext-ctl`、`-secret`（含 `=` 形式）。版本/测试命令、未知 shell wrapper/展开不建立自动绑定；原命令启动行为不变。
2. 从实际展开命令确定配置路径和 controller/secret 覆盖。未提供 controller flag 时读取 YAML 顶层 `external-controller` / `secret`。没有 controller 的简单顶层 YAML 可以读取代理端口；不猜复杂 alias/merge/listeners，也不猜核心默认配置路径。
3. controller 存在时，以 **GET /configs 的当前值**为代理端口依据。controller 请求用独立、显式 `Proxy.NO_PROXY` 的短超时只读 client，不跟随重定向；controller 失败/未授权时不退回可能过时的 YAML 端口。
4. 按 `mixed-port`（HTTP）→ `port`（HTTP）→ `socks-port`（SOCKS5）选择有效可用候选。0 表示关闭，非法端口拒绝；与 controller 端口相同的候选排除。不硬编码 7890/9090，不从实验室 env proxy 的 port/port+1 推导。
5. 根据 `allow-lan` / `bind-address` 选择并验证回环目标；监听通配地址转换为 `127.0.0.1` / `::1` 候选，客户端不连接 `0.0.0.0`。仅绑定到非回环地址、不可达回环或未知地址时不激活。
6. 首次就绪需要 HTTP CONNECT 得到 200，或 SOCKS5 no-auth greeting 得到 `05 00`，不是仅看 PID/端口存在。HTTP 探测目标为 `127.0.0.1:1`，按 Mihomo HTTP handler 的语义验证入站协议已接受；**不把这个响应当作外网/节点连通证明**。Mihomo 记录中应把该探测与业务流量区分。
7. 后台按每轮探测结束后 1 秒再次检查进程/实际配置/监听。HTTP 已验证且配置未变化时，30 秒内做 TCP liveness，避免每秒生成一次 CONNECT 目标拨号；配置/端口变化、失效恢复或满 30 秒重新验证协议。SOCKS greeting 不发目标 CONNECT，每轮均可验证。文件 metadata 未变化时缓存其 scalar/指纹，避免每秒反复读取大订阅文件。
8. 本轮不会自动配置代理用户凭据；如果 loopback 监听要求认证且不接受 no-auth，本轮保持 UNREADY 并记录回退。用户原有带认证的显式代理规则保持原逻辑。

无 controller 的 YAML fallback 保存启动时 SHA-256，文件变化即撤绑，要求核心重启；不会把仅编辑、未由核心加载的新端口当成实际运行端口。有 controller 时以其已加载配置为准，不代替 Mihomo 执行 reload。controller 地址/secret 本身变化不匹配时先撤绑，直到配置与实际核心一致。

## 代理优先级与 RULE 模式

| 次序 | 条件 | 结果 |
|---|---|---|
| 1（运行期绑定有效时） | 127/8、localhost/.localhost、IPv6 回环/映射回环、通配本机目标、当前本机网卡 IP；包括核心 proxy/controller 和播放器 helper | 明确直连，防止代理循环；没有按国内外或私网段做额外分流 |
| 2 | 原有 App Proxy 或站源显式 host rule（包括 `*`）命中 | 继续原有规则顺序/代理列表；空 proxy 规则仍按原系统 fallback 语义处理 |
| 3 | 没有命中显式规则，Mihomo runtime ready，HTTP(S)/WS(S) | 仅返回本地 Mihomo；不附加 DIRECT 作为同次请求的备用 route |
| 4 | runtime 未激活/已撤销，或不适用协议 | 原系统 ProxySelector fallback 行为 |

已有显式规则指向其他代理的请求不会进入 Mihomo；显式通配规则可覆盖所有远程请求，这属于用户规则的优先级。原显式规则若本来就指向已停止的 Mihomo，其故障语义仍由该原规则决定，本修复只撤销自动 default，不擅自改写显式配置。

App 不查看国内/国外域名、不解析规则匹配结果、不 PATCH mode/rules。进入 Mihomo 后，DIRECT 与代理节点都属于核心处理；上游节点/目标失败不会被 App 当作需要重新实现规则分流的信号。模拟服务分别返回 DIRECT/NODE 标记，App 均通过同一本地代理收发；**本轮没有运行真实 Mihomo 规则引擎**。

## 启停、故障和竞争

| 状态/事件 | 运行期行为与记录 |
|---|---|
| 未启动/启动中 | default 为空；STARTING / UNREADY 日志说明继续原策略 |
| 真正 ready | READY 记录 source、协议和 loopback endpoint；安装独立 runtime default |
| 正常停止 / stopAll | 先撤销运行期绑定并清理空闲池，随后原有停止逻辑终止核心；恢复原策略 |
| 子进程退出 | 按 owner 身份立即撤销，记录 PROCESS_EXIT；如果 shell 实际 daemonize，只能在原进程组被确认存活并重新探测成功后恢复 |
| 进程/监听异常 | 撤销并记录 CORE_NOT_RUNNING / UNREADY，未来请求恢复原策略；属于 **fail-open 到原策略，不是代理 kill switch** |
| HTTP 本地端点 TCP 连接失败 | selector 同步撤销 runtime、清理空闲池，Logcat warning 明确未来请求恢复原策略；当前失败请求不伪装成功 |
| SOCKS 目标失败 | 不因一次目标 CONNECT 失败清除健康核心；独立本地 greeting/liveness 失败才撤销，避免目标被 RULE 拒绝时反而让后续请求绕过核心 |
| 核心重启/端口变化 | 新会话先撤销旧 default；新端口确认可用才替换；旧 probe/退出通知不能恢复或移除新会话绑定 |
| App 恢复已存活核心 | 已有私有 `lab_process_state.json` 条目附加启动命令、工作目录、配置指纹；恢复后重新探测，不恢复未经验证的 ready 状态。旧版本无元信息的存活核心需要重启 |

只有一个 active runtime session 和一个后台 worker；最近一次识别成功的核心启动替代先前 session，不堆叠通配规则。若用户另开多个核心，本轮不提供多核管理/自动回退到前一核心。命令中的 controller secret 可能存在于私有进程恢复元信息中；新增状态日志不打印命令/secret。

轮询不是实时断链通知：退出回调和 HTTP endpoint connectFailed 可立即撤绑；其他故障有 1 秒调度间隔加有界探测耗时。已有活跃 socket/播放会话不被 `cancelAll` 或强关；活动 HTTP/2 连接上的 exchange 仍可能沿原 route 继续到该连接回收。本轮不声称切换瞬间迁移所有活动流量。

## 覆盖与未覆盖

| 网络组件 | 本轮效果 |
|---|---|
| 共享 OkHttp.client 原生站源/API、图片/字幕等 | 没有显式规则时，新选路默认进入 ready Mihomo |
| OkHttp.player 的 Exo/Media3、现有 HLS helper 上游 | 同上；不修改播放器资源/解析/缓存决策 |
| WebHome Native Bridge `WebCall`、共享 client 的 RawAdapter/gateway | 同上；登记独立 Bridge 池，处理启动前已创建的客户端 |
| 自建 client 若使用公共 OkHttp.getBuilder | 共享 selector 与池登记；显式 `proxy(NO_PROXY)` 等仍优先 |
| WebView Chromium 页面导航/fetch/XHR | 未改造，不受此 runtime selector 接管 |
| MPV/IJK 未经既有 OkHttp helper 的 native 请求、系统 MediaPlayer | 未改造，不受此接入保证 |
| 显式 NO_PROXY、其他独立 selector/client、外部脚本/原生 socket、UDP/QUIC、全局 DNS | 未接管；没有 VPN/TUN/UID 全流量承诺 |

## 自动化验证与构建

最终源码已完成 **45 项 App 定向测试，0 失败、0 错误**；Leanback ARM64 Debug APK 构建成功。最终命令日志为 `build/mihomo-delivery-final.log`（BUILD SUCCESSFUL，34s），XML 结果在 `app/build/test-results/testLeanbackArm64_v8aDebugUnitTest/`，本地测试时间为 2026-10-23 00:56（Asia/Shanghai）。

| 验证组 | 真实结果 | 证明范围 |
|---|---|---|
| MihomoProxyConfigTest | 10/10 | controller 实际端口优先、mixed/HTTP/SOCKS、认证失败、不可用/非法端口、配置/端口变化、loopback 协议握手 |
| MihomoProxyMonitorTest | 4/4 | 就绪后绑定、正常停止、故障/重启、新旧 session 竞争及探测期间死亡 |
| MihomoRoutingIntegrationTest | 9/9 | 真实共享 client/player/Bridge 代理收包、用户规则优先及 prefs 不变、本机防循环、空闲池迁移、活动响应保留、HTTP 失效撤绑、真实 SOCKS 目标拒绝不误撤健康核心 |
| 既有相关回归 | 22/22 | Lab process 5、App Proxy 2、Bridge wiring 9、Exo datasource 4、弹幕 selector 2 |
| catvod 全量测试 | BLOCKED_BASELINE_TEST_COMPILATION | 以下已有 DebugLogStoreTest 编译阻碍；不宣称全量通过 |
| Leanback ARM64 Debug | BUILD SUCCESSFUL | 当前未提交源码可编译、打包并签名；不等于电视真机验收 |
| 最终工作区安全检查 | PASS | task_guard check 确认 branch/HEAD、初始脏文件指纹、声明范围及暂存区；git diff --check 无错误。登记路径的 Windows CRLF 已转为 Bash 使用的 LF |

- 新测试：端口/协议/控制接口、状态机、App/站源优先级、配置不写回、本地例外、真实 HTTP 请求、SOCKS 目标拒绝、空闲池切换、活跃响应保留。
- 已有相关回归：Lab 进程兼容/身份、App Proxy、WebHome Bridge wiring、Exo datasource 隔离、弹幕 selector。
- 首轮失败被确认是 Java 21 对 `java.net.Proxy` 反射的测试环境限制，以及 FixtureApp 未初始化；用 Test JVM `--add-opens=java.base/java.net=ALL-UNNAMED` 和最小 FixtureApp 修正，没有削弱断言或改生产 Gson 模型。
- 扩大执行 `:catvod:testDebugUnitTest`：**BLOCKED_BASELINE_TEST_COMPILATION**。`catvod/src/test/java/com/github/catvod/crawler/DebugLogStoreTest.java:22,34` 调用不存在的 `DebugLogStore.trimLines/readTail`。两个文件均与 HEAD 相同；未修复、未跳过其中用例并宣称全量通过。完整失败输出在 `build/mihomo-verify-apk.log`。

复现本轮 App 验证/构建（PowerShell，已有本地缓存，未运行 clean）：

```powershell
$env:JAVA_HOME='E:/DevTools/jdk-21'
$env:ANDROID_HOME='E:/Android/Sdk'
$env:ANDROID_SDK_ROOT='E:/Android/Sdk'
$env:GRADLE_USER_HOME='E:/DevTools/GradleCache'
$env:JAVA_TOOL_OPTIONS='-Djdk.net.unixdomain.tmpdir=C:/Windows/Temp --add-opens=java.base/java.net=ALL-UNNAMED'
./gradlew.bat :app:testLeanbackArm64_v8aDebugUnitTest --tests 'com.fongmi.android.tv.lab.Mihomo*' --tests 'com.fongmi.android.tv.lab.LabRunnerProcessCompatTest' --tests 'com.fongmi.android.tv.lab.LabProcessIdentityTest' --tests 'com.fongmi.android.tv.setting.ProxySettingTest' --tests 'com.fongmi.android.tv.web.WebHomeRemoteBridgeWiringTest' --tests 'com.fongmi.android.tv.player.exo.MediaSourceFactoryIsolationTest' --tests 'com.fongmi.android.tv.player.danmaku.LiveDanmakuProxySelectorTest' :app:assembleLeanbackArm64_v8aDebug --offline --no-daemon --console=plain
```

APK：`E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`

- 类型：Leanback / ARM64-v8a / Debug；205,441,490 bytes。
- SHA-256：`c2beb20bfc959ac88a968c4bf1283071af3becc4e4fc960e1d560a35829f7198`。
- 本地未提交实验构建，未安装、发布或升级 stable baseline；App HEAD 和 Suite gitlinks 不更新。

## 真机待验证

ADB 检查时 `192.168.31.123:42721` 与 `192.168.31.205:37217` 均 offline，未安装/启动此 APK，也未读取用户设备 YAML/偏好或实际 Mihomo connections。本轮仅有 JVM、Robolectric、真实 loopback socket 和 APK 构建证据。

电视验收至少包括：

1. 启动实验室 Mihomo，查看 `MihomoProxy` Logcat 的 READY 和实际 loopback 协议/端口；controller 只读 `/configs` 核对 `mode=rule`。日志 READY 只代表本地代理就绪。
2. 对原生 API 与 Exo/HLS 各发一个缓存未命中的可识别请求；将 host/时间/播放器类型与 Mihomo 入站 connections 对齐。HTTP readiness 的 `127.0.0.1:1` 不能当成业务流量。
3. 各选择真实 YAML 命中 DIRECT 和节点的目标，均须有对应 Mihomo 入站；DIRECT 不判绕行。另选一个显式 App/站源代理 host，验证仍走其原代理。
4. 单独测 WebHome Native Bridge；Chromium fetch、MPV/IJK native 作为未覆盖对照，记录其实际路径，不外推。
5. 正常停止、杀死核心、重启与实际 reload 更换端口，核对撤绑/恢复日志和后续请求选路。特别验证 SOCKS 上游目标失败不会错误撤掉仍健康的本地核心。
6. 正在播放时切换状态，确认本轮没有主动取消播放会话；核心本身被停止造成其现有 socket 失败属于核心停止的自然后果。

## 交付状态

```text
MIHOMO_READY_DETECTION=PASS_UNIT_AND_LOOPBACK_PROTOCOL; DEVICE_PENDING
AUTO_PROXY_BINDING=PASS_SHARED_OKHTTP_AND_NATIVE_BRIDGE_TESTS; DEVICE_PENDING
RULE_MODE_PRESERVED=PASS_NO_MODE_OR_RULE_WRITES; REAL_MIHOMO_DIRECT_NODE_VERIFICATION_PENDING
EXISTING_PROXY_RULES_PRESERVED=PASS_APP_SITE_PRIORITY_AND_PREFERENCES_TESTS
STOP_RESTORE=PASS_STATE_MACHINE_AND_HTTP_RESTORE_TESTS; ANDROID_PROCESS_PENDING
CORE_FAILURE_HANDLING=PASS_MONITOR_HTTP_AND_SOCKS_FAULT_TESTS; FAIL_OPEN_TO_ORIGINAL_POLICY; ANDROID_PROCESS_PENDING
OKHTTP_ROUTING_TESTS=45_PASSED_0_FAILED_0_ERRORS; CATVOD_FULL_SUITE_BLOCKED_BY_BASELINE_TEST_COMPILATION
APK_BUILD=SUCCESS_LEANBACK_ARM64_DEBUG
PENDING_DEVICE_VERIFICATION=YES
```

## Recovery anchor

- 状态：限定修复、45 项定向测试及最终 APK 已完成；不提交/推送。
- 基点：main / `ad46fe5da81eacbfbd0465de6b0b85450cf3780a`；`scripts/build-arm64.ps1` 原样保护。
- 文件：上表 8 个生产源文件、3 个测试文件及本报告；实验室 YAML、App Proxy 设置与其他子仓库不改。
- 已验证：最终 45 项定向测试、Debug 构建和上述 APK hash；catvod 全量测试被基线旧测试编译阻断。
- 未验证：Android 实际进程恢复/退出时序、用户 YAML 真实 DIRECT/节点入站关联、电视播放兼容性。源码/自动化不替代这些验收。
- guard：保留本地未提交任务状态；不得执行会提交的 finish，用户明确禁止提交/推送。
- 下一步：用该 Debug APK 按真机待验证清单采集 App / Bridge / Exo 与 Mihomo 入站关联。
