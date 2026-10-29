# C56 上游选择性整合（2026-10-28）

## 授权、冻结与恢复

用户明确授权实际选择性施工、双端 ARM64 Debug 与有条件 Leanback Release 候选；不提交、不推送、不更新 Suite gitlink，不修改 WebHome/WOGG。当前 main 与初始 HEAD：`d48e68823ae283fd9c7286ea221b0ef75a620eed`。初始 tracked/index/untracked 均干净；此前 33 路径与 R2/R3 已在该提交固化。回滚以该提交对象和现有外部 bundle 为依据，不自动 reset。

备份目录 `E:\GitHub\_worktree_backups\webhtv-app\WORKTREE-BASELINE-01-20261028-101437` 已只读访问；01/02 报告、所有权与清理 CSV、snapshot/private/artifacts/manifests/restore-check 材料存在。02 报告记录独立恢复与 72,121 文件哈希校验，增量 bundle SHA256 `7c28af179bb0a1281b1bd07846349de65d942aa90f687be0adcb263c5de597e5`；本轮不重拷贝、不修改备份。

fetch 实际最新 beta 冻结为 `004b1f5d363ed32a48a040dd4497311d50179c23`。从 `d9da14dcb77e84d4beb158e83efa74124a1116f7` 完整历史计 72 提交、284 文件。C55 已覆盖至 `236a8fc02f5a2ce4ea54001c63fa0cfc0d66ffdd`；不能重复整合其 31 提交。

## 实施前决策与证据（先于业务源码修改建立）

已使用 upstream-integration-governor 工作流；用户本轮直接施工/禁止提交要求优先于默认暂停/提交闭环。旧 C55 guard 原样保留于 deferred/C55-before-C56-20261028，C56 scope 已登记，未触碰旧源码。

方案比较：不改动会保留繁体匹配失败、直播键漂移及线路回声；照搬上游会丢失本地详情/悬浮与凭据语义，并不能完整兼容旧显式 zh-CN 和旧简体缓存键；采用局部适配方案。验收：查询词保留原脚本、比较/缓存稳定归一，年份/类型/季/ID与手动优先不弱化；旧语言明确值保留；线路实例去重不吞点击；身份/显示分离不删数据库数据；JVM Error 不被依赖下载捕获。无依赖、签名、包名、ABI配置升级。

证据分级：A级为当前基线、冻结上游最终代码/测试和 Git diff；B级为官方文档与发布 API；静态推断与设备行为分开。

- 上游 PR491/493/486 与最终提交：本仓库对象完整读取（在线 PR 页面缓存不可用，不能把网页未读表述为已读讨论）；PR491 最终树 `6d0b3eb478e16cfcf1fcc093eba8a5d0ab248795` 隔离 C101 分帧改动。
- 官方 TMDB language 文档 https://developer.themoviedb.org/docs/languages （2026-10-28读取）：请求语言使用 language-country，不能把请求语言与标题身份或实际搜索词混为一谈。
- Android Context.createConfigurationContext 官方 API https://developer.android.com/reference/android/content/Context#createConfigurationContext(android.content.res.Configuration) （同日读取）：核对配置上下文关系；实际补丁继续使用 super.getResources 框架对象，只对其更新加锁和失败退回，不替换 WebView 所用 AssetManager。
- 成熟项目实现为当前项目的 strict TMDB 缓存、语言守卫、Task.Scope 和原生详情代际守卫，及上游完整回归；不借其他项目主题方案替换现有语义。此为 App 业务身份与兼容修复，论文/跨项目基准不适用于证明作品身份；不作性能数值承诺。Java 同步规范在线请求503，采用代码级类锁与实际测试，不宣称外部规范研究完整。

### 功能范围与兼容处理计划

| 分组 | 现状与收益 | 决策及风险边界 | 验证 |
|---|---|---|---|
| TMDB | 当前比较/旧键随脚本不同；本地额外 native home/history/lite 消费者 | ADAPT 最终 PR491；单向 s2t 仅比较；旧显式 zh-CN 保守保留；扩展旧缓存回读；所有请求与缓存消费者同源语言 | 双端 TMDB/历史/内联/首页定向测试、样本与消歧 |
| 线路 | C55 的 mPlaybackFlagKey 实际身份已具备；Listener 未持有已派发实例 | PRESENT PR489；INTEGRATE PR493，NO_POSITION 不开新窗口、点击不去重 | 最终实例测试、接线/历史/自动播放边界 |
| Python | 下载桥接/原子缓存/HTTP/重定向已具备，单依赖 catch Throwable 过宽 | PRESENT 下载契约，ADAPT Exception 边界；INTEGRATE Loader 类锁 | Python 下载套件与 Java 接线/初始化守卫 |
| 直播 | name 被翻译，持久化匹配可漂移 | ADAPT raw name + transient displayName；兼容旧译文；精确名称优先于归一回退 | 双端显示/旧收藏屏蔽分组/同键同步 |
| 资源与小优化 | App 语言资源并发无锁；重复内容动画与正则重复编译 | ADAPT App 锁与回退；INTEGRATE ProgressLayout/ImageHeaderPolicy，保留本地主题 | 资源/动画/图片策略及主题回归 |
| 徽标 | 127规则/127图、持久化/备份/OSD 已在 C55，入口后续本地修复 | ALREADY_PRESENT，不重放早期只随控制栏的中间状态 | C55/媒体徽标测试与资源哈希 |
| C101 生命周期 | 绑定延迟500ms、resize cache、详情分帧与本地悬浮/首屏预热重叠 | DEFER：缓存失效需覆盖 floating attach、reset、不同会话，分帧回调需补本地 stale generation 证明 | 不改变现有播放架构；未来以真机时序验证 |
| C101 字幕/身份 | provider懒初始化/预热、Device进程缓存 | DEFER：本地warm队列取消边界未证明；设备名/MAC权限变化缺失失效策略 | 不替换本地warm与设备身份 |
| C101 日志 | 单次脱敏/节流、ThreadLocal线程名缓存 | DEFER：需证明重命名线程与各脱敏分支等价；性能数据未建立 | 保留本地诊断，不声称安全缺陷已修复 |
| C101 Mobile集名缓存 | intent缓存减少解析，onNewIntent失效 | DEFER：本地动态详情/历史富集会修改来源，尚无全部失效路径证据 | 保留实时读取，不改Mobile UI |
| C101 ART profile | 上游31,712行设备生成profile | DEFER：来自其他类图/设备，需本地生成而非拷贝 |
| C107 主题/搜索 | 本地ThemeBinder已有递归状态元数据；搜索结构无keywordField | DEFER 白名单重构/新搜索表面，不整文件覆盖；Mobile编译冲突以本地实际构建确认 | 主题/焦点与原生首页回归 |

每组先定向测试，再双端本地保护回归。故障先对比冻结基线；不放宽断言。旧空白提示12处、基线检查退出2已保存，新增diff独立检查。基线 Leanback 定向：BUILD SUCCESSFUL，1m31s（TMDB缓存/语言、Flag、NativeHome/History、Mihomo、C55）。原始日志在 `%TEMP%\C56-20261028\baseline-leanback-tests.log`。测试 JVM 仅现有 TLS fixture 使用 sun.security.ssl opens，不开放 java.net。

## 完整72提交台账

以下按完整拓扑顺序列出，合并与文档亦不遗漏。状态为实施前裁决，最终验证在后文追加；混合提交只取表内明确范围。

| 上游完整提交 | 标题 | 裁决 | 依据/归组 |
|---|---|---|---|
| `4a55998f35e9d0710abe1d140254405cb00356f4` | feat(osd): show provable media info badges in player OSD | ALREADY_PRESENT | C55 最终本地等效处理：徽标解析与OSD基础；补已知SDR、MIME优先级及Mobile轮询 |
| `49e5b2c4bc05a5e1e05f2bc18327d1c02079b9f5` | feat(osd): add media-info chip to player display controls | ALREADY_PRESENT | C55 最终本地等效处理：控制弹窗媒体信息开关 |
| `7d4bee6ce1de549138ae3a2809334e5bc3c5c759` | feat(osd): show media info badges only while the control bar is visible | ALREADY_PRESENT | C55 最终本地等效处理：中间仅控制栏展示语义由192b最终常驻方案替代 |
| `7067d62b7b51bf413eacd9501a02ca65422b8d5e` | fix(update-dialog): paint version cards from the active palette so text and fill cannot diverge | ALREADY_PRESENT | C55 最终本地等效处理：本地状态递归binder及独立语义tag已处理更新卡片 |
| `35516a58ffc765da7b7326c8c0639a9db588bceb` | fix(py): 恢复 Spider 依赖下载桥接并加固契约 | ALREADY_PRESENT | C55 最终本地等效处理：Python依赖download桥接与失败处理 |
| `db553538b2896a0177ee6c2f6d0f138b9ccf7666` | merge+review(C102): 合并 origin/beta d9da14dcb7 到 dev3，修复版本卡片焦点与选中线索退化 | ALREADY_PRESENT | C55 最终本地等效处理：评审引入的聚焦/选中区分与本地方案比较后保留本地，不重放合并 |
| `17bd235c248ca3c40c7194bc3b09d57293465858` | docs(C102): 回填交付坐标（提交 db553538b2 / tag / PR #485）与拉取结果 | REJECT | C55 已排除的文档/拓扑：上游交付坐标文档 |
| `8ef4dbf34fd0694a8845b0d7cb776b34167a01c8` | review(PY-BRIDGE-DEPEND-01): 修复评审必修项并加固依赖下载契约 | ALREADY_PRESENT | C55 最终本地等效处理：Python桥接可选布尔/非HTTP来源/错误缓存守卫 |
| `762f5ae6ae8135383c6c279218dcb13f25e58b39` | docs(PY-BRIDGE-DEPEND-01): 收紧评审记录中的事实表述 | REJECT | C55 已排除的文档/拓扑：上游文档表述修订 |
| `e8551e321cef5d7d8c064c0b0cb0658956871e5a` | test(PY-BRIDGE-DEPEND-01): 补 Python 蜘蛛依赖失败日志双通道守卫 | ALREADY_PRESENT | C55 最终本地等效处理：Python异常结构化与Logcat双通道测试 |
| `1d80913f77d1348418d6fbbd4e5f50f46fa7dc88` | Merge pull request #486 from Silent1566/dev4 | REJECT | C55 已排除的文档/拓扑：PR486合并拓扑，代码按最终树提取 |
| `7c725e4d7ee43ca0a187e420b224b33bb0a39a6a` | Merge pull request #485 from Silent1566/dev3 | REJECT | C55 已排除的文档/拓扑：PR485合并拓扑 |
| `0dc165a9a2bd5c4d979acc1cc870fb89ee3672f7` | feat(osd): render media badges in one row with category-tinted plates | ALREADY_PRESENT | C55 最终本地等效处理：分组胶囊/单行优先呈现（窄屏可wrap） |
| `fe77e61cc51eb92a34aea4b799b3d5bd5c3f04ee` | C101: add loading jank probe script and task doc | REJECT | 任务文档/探针，无业务移植需求。 |
| `f0976cdb2499e481c421fa3afd00b20b214ed361` | C101-S1: defer playback service binding until after first frame | DEFER | 见功能分组风险：播放绑定/分帧/日志/字幕/ART/主题白名单与本地上下文不等价。 |
| `b829d104ef4480110f684003d37865e2af9e4aa6` | fix(tv): keep exit-confirm secondary labels readable on their fixed light fill | ALREADY_PRESENT | C55 最终本地等效处理：本地退出次级前景/背景已配对，不引入固定深色 |
| `442ee30e9ac3531581d288cc18e521e7d7128478` | C101-S2: chunk tmdb detail result application across frames | DEFER | 见功能分组风险：播放绑定/分帧/日志/字幕/ART/主题白名单与本地上下文不等价。 |
| `64ee84e68e5b036feb7a1ca2c581c3d996e82178` | feat(osd): ship the full media badge catalogue from the reference set | ALREADY_PRESENT | C55 最终本地等效处理：完整目录初版，按最终127条接入 |
| `800945de7c3e83373ca6746532ff5e32e04529b0` | perf(diagnostics): reuse one cleaned payload per log line and throttle range logs | DEFER | 见功能分组风险：播放绑定/分帧/日志/字幕/ART/主题白名单与本地上下文不等价。 |
| `13be53307caec31a6807e70ecbfeecfffa65d6f2` | perf(ui): make ProgressLayout content fade idempotent and cancel stale animators | INTEGRATE | 独立幂等动画/预编译正则；无布局及网络语义改变。 |
| `a5095df3869471e5f132c3c1d5080636ed2273cd` | perf(images): precompile the douban image host pattern | INTEGRATE | 独立幂等动画/预编译正则；无布局及网络语义改变。 |
| `1322505938cecc897fa2a60f9edffc9a75402bc6` | merge+review(C103): 合并 origin/beta 7c725e4d7e 并评审屏显媒体信息徽标 5 个未推送提交（0 冲突、0 必修项） | REJECT | C55 已排除的文档/拓扑：评审合并拓扑，最终有效实现已提取 |
| `39e1b4a0d47627dfc71eb27ffbfd0c84ee900e1e` | docs(C103): 回填交付坐标（合并提交 1322505938 / recovery tag / 推送 dev1 / PR #487 / 拉取最新） | REJECT | C55 已排除的文档/拓扑：上游交付文档 |
| `96891188510efa26803b290e1dc1192480ab2560` | fix(tv): pin palette-independent foregrounds on fixed light surfaces | ALREADY_PRESENT | C55 最终本地等效处理：大多数保留动态主题，仅修TV日志带及固定浅绿评分徽章 |
| `62f0b777d2a11fcd0891cb67474221956cb05c18` | merge: 合并远端 beta（7c725e4d7ee，PR #485/#486：版本卡片调色板修复与 PY-BRIDGE 补充守卫）到 dev4 | REJECT | C55 已排除的文档/拓扑：同一beta父树合并，不重复移植 |
| `192b55c2fca81cee7655590a4e8d79c1fa3dc74b` | feat(osd): keep media badges persistent when enabled and always show them with the control bar | ALREADY_PRESENT | C55 最终本地等效处理：控制栏必显+开关常驻最终语义；补首次缺元数据继续轮询 |
| `82ff8d4be9303a92c0b651035ef7ab20a29b9de4` | docs(C101): record acceptance retest, jank floor finding and S6 decision | REJECT | 任务文档/探针，无业务移植需求。 |
| `d638f6b9a9256e83250d96f874b1a8133bde070b` | fix(live): 直播源身份与界面语言解耦，修复切换繁体后收藏/屏蔽/上次频道丢失 | ADAPT | PR491：稳定身份/显示解耦及语言资源；保留本地凭据和详情生命周期，补旧缓存/显式中文语言兼容。 |
| `9b0a547eae202bb24ade60cfabc820c2fc500506` | fix(tv): repair the mobile ad-block log band the fixed-light sweep missed | ALREADY_PRESENT | C55 最终本地等效处理：Mobile日志标题带使用本地动态surface配对 |
| `306b823dc7b7340e13e999c09679eaccab0ee152` | docs(C103): 回填交付坐标（提交 9b0a547eae2 / recovery tag / PR #488）与拉取结果 | REJECT | C55 已排除的文档/拓扑：上游文档坐标 |
| `adcf52019ba35f74384375758136d481e0c8380c` | fix(live): 上次频道与频道同步按与语言无关的归一名匹配，兼容旧版译文主键 | ADAPT | PR491：稳定身份/显示解耦及语言资源；保留本地凭据和详情生命周期，补旧缓存/显式中文语言兼容。 |
| `8322438787d8f3e769ef244308d634dbd72d8307` | Merge pull request #488 from Silent1566/dev4 | REJECT | C55 已排除的文档/拓扑：PR488合并拓扑 |
| `c9fb2731b1a6ca60a54d3f4105d0c95090ef7483` | fix(tmdb): 匹配前先做简繁归一，修复繁体界面下 TMDB 匹配失败 | ADAPT | PR491：稳定身份/显示解耦及语言资源；保留本地凭据和详情生命周期，补旧缓存/显式中文语言兼容。 |
| `d1616410bb99fc30722044d0dcdda309ece49e2f` | fix(tmdb): 缓存键与标题清洗也不再受界面语言影响，补齐繁体噪声词表 | ADAPT | PR491：稳定身份/显示解耦及语言资源；保留本地凭据和详情生命周期，补旧缓存/显式中文语言兼容。 |
| `0363493c98097289133c0ad9605ed8c491f0f74c` | perf(C101-S7): cut first-frame main-thread work on the playback page | ADAPT | 只取诊断日志中无配置影响的音频硬件探测移除；绑定、resize、详情分帧等仍暂缓。 |
| `11be83da8b345f51a9615bbd111f84f420fe7b91` | feat(osd): merge the xinyue badge expansion and equalize badge row height | ALREADY_PRESENT | C55 最终本地等效处理：最终127条/127图、统一高度、AAC mp4a.40归一 |
| `2b405b4b7bd1a71ae1ebeb8b52427923303b8e71` | fix(live): 分组名与 keep 键不再随界面语言漂移，兼容旧译文组名 | ADAPT | PR491：稳定身份/显示解耦及语言资源；保留本地凭据和详情生命周期，补旧缓存/显式中文语言兼容。 |
| `3cfe93f389615bb32e00540afe23c66b1ec160e1` | merge+review(C104): 合并 origin/beta 8322438787 并评审徽标常驻语义与扩充包改动 | REJECT | C55 已排除的文档/拓扑：评审合并拓扑 |
| `539408b81d66a6dcb5990e0212af70886aa3e09d` | perf(C101-S9): warm subtitle providers off the playback first frame | DEFER | 见功能分组风险：播放绑定/分帧/日志/字幕/ART/主题白名单与本地上下文不等价。 |
| `02fc09ede42a762a9f9ef45aae24dfbd0405f5d4` | docs(C104): 回填交付坐标（合并提交 3cfe93f389 / recovery tag / 推送 dev1 / PR #487 就地更新 / 远端拉取） | REJECT | C55 已排除的文档/拓扑：上游文档坐标 |
| `07b9c20b609ed707dc452f49c95960711980fd1d` | fix(site): 站点名能力标签不再随界面语言变形，保住分组键与站点规则匹配 | ADAPT | PR491：稳定身份/显示解耦及语言资源；保留本地凭据和详情生命周期，补旧缓存/显式中文语言兼容。 |
| `024ac3f661ed390a2262d6d8916a5a8b5df83519` | fix(app): 语言资源入口加锁并容错，语言切换不再可能写坏 WebView 的 AssetManager | ADAPT | PR491：稳定身份/显示解耦及语言资源；保留本地凭据和详情生命周期，补旧缓存/显式中文语言兼容。 |
| `ee285f8e85c153caa7b5b0d758a9279a9d6ce88d` | Merge pull request #487 from Silent1566/dev1 | REJECT | C55 已排除的文档/拓扑：PR487合并拓扑 |
| `772a25a45486f72a6ff107eb1af5f9f2fbfd9c2d` | fix: 同名线路切换后按稳定键判定当前播放线路 | ALREADY_PRESENT | C55 最终本地等效处理：两端Video及TMDB内联稳定线路键，保留选中/起播/失效边界 |
| `db0b4483b35a1f77286acdb7892478aa15657b10` | merge: 合并 origin/beta ee285f8e85 并评审 dev4 未推送修复 | REJECT | C55 已排除的文档/拓扑：上游评审合并拓扑 |
| `4058fb7cf8ae1f1fad700bdb06891646a64e6d01` | docs(C105): 回填交付坐标与 PR #489 | REJECT | C55 已排除的文档/拓扑：上游文档坐标 |
| `236a8fc02f5a2ce4ea54001c63fa0cfc0d66ffdd` | Merge pull request #489 from Silent1566/dev4 | REJECT | C55 已排除的文档/拓扑：冻结目标PR489合并拓扑 |
| `f435ff4c0ca8f5bf9bfaf4f38321683e0f3c1698` | perf(C101-S10): ship a baseline profile generated from the device ART profile | DEFER | 见功能分组风险：播放绑定/分帧/日志/字幕/ART/主题白名单与本地上下文不等价。 |
| `6c2de578dc1f63b4901089987fbf52bb204e4f5a` | fix(tmdb): 详情页匹配与分季缓存键也做简繁归一，繁体界面下 TMDB 匹配不再失配 | ADAPT | PR491：稳定身份/显示解耦及语言资源；保留本地凭据和详情生命周期，补旧缓存/显式中文语言兼容。 |
| `abaef4f1b7b977100258d56730c6c7d6eba92945` | docs(tmdb): 回填繁体界面 TMDB 匹配修复的设备实测对照证据 | REJECT | 任务文档/探针，无业务移植需求。 |
| `9996d787ec0c23ddbec0b48ff714134e68801b83` | feat(tmdb): 未显式配置语言时请求语言跟随界面，并新增 languageConfigured 标记保护用户选择 | ADAPT | PR491：稳定身份/显示解耦及语言资源；保留本地凭据和详情生命周期，补旧缓存/显式中文语言兼容。 |
| `87d09dcd21fe29cacabccfcd75b77e5251aa542c` | docs(C101): record the profile-on A/B on a fresh install and fix the acceptance method | REJECT | 任务文档/探针，无业务移植需求。 |
| `056626b2e65f9211b91f9394ba0782b4f2765e12` | fix(chaquo): start CPython once under a class lock so concurrent loaders cannot crash | INTEGRATE | CPython 类锁，保留真实启动失败。 |
| `472a71ec7a8c3e87148fd44472f767aad203e589` | merge: 合并远端 beta 236a8fc02f（PR #483-#489）到 dev2 | REJECT | 合并拓扑或说明；运行时代码按最终功能分组核查，不重复重放。 |
| `3220d20d6c3d4a9f90b192340dae06d10e512c58` | merge+review(C106): 合并 origin/beta 236a8fc02f 到 dev3，评审 10 个未推送提交并修复 TMDB 语言同源、浏览树标题与旧配置兼容三项必修 | ADAPT | PR491：稳定身份/显示解耦及语言资源；保留本地凭据和详情生命周期，补旧缓存/显式中文语言兼容。 |
| `6d0b3eb478e16cfcf1fcc093eba8a5d0ab248795` | Merge pull request #491 from Silent1566/dev3 | ADAPT | PR491：稳定身份/显示解耦及语言资源；保留本地凭据和详情生命周期，补旧缓存/显式中文语言兼容。 |
| `0fc6878533108e4d48c341576206c4f39e758b14` | docs(C106): review beta merge into dev2 with no blocking findings | REJECT | 任务文档/探针，无业务移植需求。 |
| `ebcbeb9330016dde096e2f4575ab37cd0e25f08b` | merge: 合并远端 beta 6d0b3eb478（PR #491）到 dev2 | REJECT | 合并拓扑或说明；运行时代码按最终功能分组核查，不重复重放。 |
| `31b29f5c6dc4d16d83492bf9bcd476a25e2b6436` | docs(C106): record second beta merge 6d0b3eb478 and its review/verification | REJECT | 任务文档/探针，无业务移植需求。 |
| `6db0ebab9ff34bdae29936c0394c4c94e955a806` | Merge pull request #492 from Silent1566/dev2 | ADAPT | PR492混合整合：只取 Loader、ProgressLayout、ImageHeaderPolicy、Exo诊断；其余C101按分组暂缓。 |
| `a3b7817b3f18d588783d1da9b6413af5a829901e` | merge(C107): 合并 origin/beta 6db0ebab9f（PR #490-#492）到 dev1 | REJECT | 合并拓扑或说明；运行时代码按最终功能分组核查，不重复重放。 |
| `168a485514c6f92475db7ec0580022c324519ced` | fix(leanback): 线路行选中回声不再自激切换，刷新途中的 NO_POSITION 不再清回声窗口 | INTEGRATE | PR493：实例回声窗口和 NO_POSITION 边界及回归。 |
| `b1ef0aa03a1a16ea13142b277bf7003f87f1465e` | test(leanback): 强化线路回声守卫的回归断言（存在性+顺序、取消回调、同名身份、点击边界） | INTEGRATE | PR493：实例回声窗口和 NO_POSITION 边界及回归。 |
| `ad285c4ad2c8a21f8ab5f50d2aa22f871fdff5d7` | docs(C106): 回填线路回声修复的交付坐标（提交、PR #493、recovery tag、回滚与 beta 同步状态） | REJECT | 任务文档/探针，无业务移植需求。 |
| `bbb6d68ff2b8f91c25c75983d4687a58de5d4d9e` | test(leanback): 补上回声窗口清除与真实切换不被吞的回归用例（变异挖出的覆盖缺口） | INTEGRATE | PR493：实例回声窗口和 NO_POSITION 边界及回归。 |
| `09245faf8304442c46ee9e9bef7e9d1405c8f1bc` | docs(C106): 回填最终交付坐标（补测提交与 4 个 recovery tag） | REJECT | 任务文档/探针，无业务移植需求。 |
| `d7d64f4360e2cbee4cf6d58400a343971a647b11` | Merge pull request #493 from Silent1566/dev2 | INTEGRATE | PR493：实例回声窗口和 NO_POSITION 边界及回归。 |
| `c8e526321c4038f1058d164b40b72035ff7dc2b8` | fix(C107): 合并 beta 后评审修复——焦点环跟随主题收窄到应用表面白名单、搜索栏浅色可见、mobile 变体编译阻断 | DEFER | 见功能分组风险：播放绑定/分帧/日志/字幕/ART/主题白名单与本地上下文不等价。 |
| `c99ab92823c4dc3ca060c8101d24a7eeba7af5c0` | merge(C107): 合并 origin/beta d7d64f4360（PR #493）到 dev1 | REJECT | 合并拓扑或说明；运行时代码按最终功能分组核查，不重复重放。 |
| `36ef7a63bf23812a5e1349c6a2fea6cf63bec956` | fix(C107-2): 搜索栏焦点环真正点亮——#keywordField 改为合并子控件状态且不再自己可聚焦 | DEFER | 见功能分组风险：播放绑定/分帧/日志/字幕/ART/主题白名单与本地上下文不等价。 |
| `ce2fcc20957c7ff37a2667740425e3cc136bdcf4` | docs(C107-2): 回填交付坐标（四提交 + 两个 recovery 标签 + PR #494） | REJECT | 任务文档/探针，无业务移植需求。 |
| `004b1f5d363ed32a48a040dd4497311d50179c23` | Merge pull request #494 from Silent1566/dev1 | DEFER | 见功能分组风险：播放绑定/分帧/日志/字幕/ART/主题白名单与本地上下文不等价。 |

### 补充裁决：Exo 音频诊断

`0363493c98097289133c0ad9605ed8c491f0f74c` 的混合改动只抽取 ExoUtil 日志块：日志为查询能力同步触碰音频硬件，结果只用于打印，不参与 AudioSink 配置。INTEGRATE 用已有 passthrough/float/playbackParams/offload 配置打印，保持后续 directPolicy 和音频构建完全不变。无需引入首帧延迟绑定。来源最终 upstream ExoUtil 和 ExoUtilAudioCapabilityLogTest；风险是减少日志字段，收益是去掉仅为日志执行的硬件探测，不宣称本地实测节省了上游所述耗时。回滚只还原该日志块，不动解码策略。

C107 UI 独立核查：本地 Danmaku MaterialSwitch 本身仍 focusable，未采用 loadHost 包装，因此上游“包装承载焦点后点击转发”的修复无适用前提；不引入该结构。玻璃图标无独立 ring 是可见性候选，但新增统一矩形与本地字幕椭圆/原生主页状态规范重叠，当前暂缓视觉统一，后续需逐控件 TV 对比；不宣称已等效修复。

## 发布与 tag 核对

GitHub Releases REST 查询与 git ls-remote 一致，2026-10-28 实际读取：

| 发布 tag | 源码提交 | 发表 UTC / 北京时间 | 性质 |
|---|---|---|---|
| v5.6.0-beta-202610271903 | d7d64f4360e2cbee4cf6d58400a343971a647b11 | 2026-10-27 12:37:03 / 20:37:03 | beta prerelease |
| v5.6.0-beta-202610270920 | 6db0ebab9ff34bdae29936c0394c4c94e955a806 | 2026-10-27 03:38:49 / 11:38:49 | beta prerelease |
| v5.6.0-beta-202610251216 | 236a8fc02f5a2ce4ea54001c63fa0cfc0d66ffdd | 2026-10-25 16:59:29 / 26日00:59:29 | C55坐标 |
| v5.6.0-202609221328 | edf04f9b5048b6b5bc38c53333ac3c7d62f00265 | 2026-09-22 06:09:37 / 14:09:37 | 最新非prerelease |

两个新 beta tag 均为冻结 beta 的祖先。9月正式发布属于 main 发布合并历史，并非当前 beta 祖先；共同基点 `9d3dd9a73c1b4b09daf4efa6ad151c18bb8a6b3c`，不能误说所有 Release commit 都在 beta。它早于本次 d9da 基线，不扩大本轮同步范围。发布 JSON 的版本为5.6.0/code560，最新 beta 声明源码 d7d64f4。未下载上游完整 APK，因此仅验证 tag/API/发布JSON所声明身份，不声称逆向证明上游 APK 与源码完全一致；没有证据确认存在新 APK-only 代码。

旧候选实际保护：`E:\GitHub\_worktree_backups\webhtv-app\WORKTREE-BASELINE-01-20261028-101437\artifacts\leanback-arm64_v8a.apk`，174125530 bytes，重新计算 SHA256 `8dcc4ae6dbaaf9b8be7d82adec20ff9303166c7f82ca4f59e0ca86f3d405236c` 相符，只读未更改。新构建使用 `WEBHTV_APK_SUFFIX=c56-20261028` 区分输出，不覆盖旧名候选。

## 最终树运行时文件覆盖清单

完整284文件的name-status保存在本轮证据目录。下面展开全部非测试/非文档脚本/非徽标图片路径；其余分别是{'tests': 35, 'docs_scripts': 20}。127徽标资源和规则以C55测试与哈希检查另验；未以提交标题代替运行时代码核查。

| 路径 | 裁决 | 说明 |
|---|---|---|
| `app/src/leanback/java/com/fongmi/android/tv/ui/activity/LiveActivity.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/leanback/java/com/fongmi/android/tv/ui/activity/SettingPlayerActivity.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/leanback/java/com/fongmi/android/tv/ui/adapter/GroupAdapter.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/leanback/java/com/fongmi/android/tv/ui/adapter/LiveAdapter.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/leanback/java/com/fongmi/android/tv/ui/custom/FlagSelectionListener.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/leanback/java/com/fongmi/android/tv/ui/dialog/ControlDialog.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/leanback/java/com/fongmi/android/tv/ui/dialog/UpdateDialog.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/leanback/res/drawable/selector_exit_confirm_secondary.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/leanback/res/drawable/selector_glass_icon_action.xml` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/leanback/res/drawable/selector_tv_search_field.xml` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/leanback/res/drawable/shape_subtitle_focused.xml` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/leanback/res/drawable/shape_subtitle_pressed.xml` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/leanback/res/drawable/shape_tv_search_field.xml` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/leanback/res/drawable/shape_tv_search_field_focused.xml` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/leanback/res/layout/activity_search.xml` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/leanback/res/layout/activity_setting_enhance.xml` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/leanback/res/layout/activity_setting_player.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/leanback/res/layout/adapter_ad_block_log.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/leanback/res/layout/adapter_ad_rule.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/leanback/res/layout/adapter_ad_stats_item.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/leanback/res/layout/dialog_ad_block_stats.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/leanback/res/layout/dialog_control.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/leanback/res/layout/dialog_danmaku.xml` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/leanback/res/layout/dialog_exit_confirm.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/leanback/res/layout/dialog_track.xml` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/leanback/res/layout/dialog_update.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/baseline-prof.txt` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/main/java/com/fongmi/android/tv/App.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/api/config/LiveConfig.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/bean/Backup.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/java/com/fongmi/android/tv/bean/Device.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/main/java/com/fongmi/android/tv/bean/Group.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/bean/History.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/bean/Live.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/bean/Site.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/bean/TmdbConfig.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/bean/TmdbMatchCache.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/bean/TmdbSeasonMatchCache.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/browse/LiveBrowse.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/playback/PlaybackRecord.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/main/java/com/fongmi/android/tv/player/AudioPlaybackDiagnostics.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/java/com/fongmi/android/tv/player/MediaBadgeCatalog.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/java/com/fongmi/android/tv/player/MediaBadgeResolver.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/java/com/fongmi/android/tv/player/cache/PlaybackDiskBufferStore.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/main/java/com/fongmi/android/tv/player/exo/ExoUtil.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/service/TmdbService.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/setting/PlayerSetting.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/java/com/fongmi/android/tv/subtitle/SubtitleMatchService.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/main/java/com/fongmi/android/tv/subtitle/SubtitleTmdbResolver.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/theme/ThemeBinder.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/main/java/com/fongmi/android/tv/theme/UpdateVersionRowTheme.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/java/com/fongmi/android/tv/title/MediaTitleParser.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/title/MediaTitleResolver.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/ui/activity/PlaybackActivity.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/ui/custom/PlayerOsdController.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/java/com/fongmi/android/tv/ui/custom/ProgressLayout.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/ui/dialog/DanmakuDialog.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/main/java/com/fongmi/android/tv/ui/dialog/DisplayDialog.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/java/com/fongmi/android/tv/ui/dialog/TmdbSourceDialog.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/ui/helper/TmdbMatcher.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/ui/helper/TmdbSourceAdapter.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/ui/helper/TmdbUIAdapter.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/utils/ImageHeaderPolicy.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/java/com/fongmi/android/tv/utils/TmdbLanguagePolicy.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/main/res/color/fixed_light_focus_icon.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/color/fixed_light_focus_text.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_aac.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dolby_atmos.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dolby_atmos_digital_plus.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dolby_atmos_truehd.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dolby_digital.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dolby_digital_plus.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dolby_truehd.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dts.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dts_es.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dts_hd.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dts_hd_ma.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dts_x.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dts_x_hd.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_dts_x_hd_ma.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_flac.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_lossless.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_lpcm.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ga_opus.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gc_2_0.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gc_5_1.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gc_5_1_2.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gc_5_1_4.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gc_6_1.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gc_7_1.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gc_7_1_4.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ge_av1.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ge_avc.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ge_divx.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ge_hevc.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ge_vvc.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ge_xvid.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_allregion.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_arrow.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_cc.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_de.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_dieying.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_eu.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_fr.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_hk.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_jp.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_kr.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_tw.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_uk.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_ged_us.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gfr_hfr.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_blu_ray_1.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_blu_ray_2.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_blu_ray_3.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_blu_ray_4.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_blu_ray_5.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_blu_ray_6.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_blu_ray_7.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_blu_ray_8.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_high_bitrate.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_iso.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_remux_1.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_remux_2.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_remux_3.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_web_1.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_web_2.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_web_3.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_web_4.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_web_5.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gms_web_6.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_bluray.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_cam.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_dvdrip.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_hc_hdrip.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_hdrip.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_hdtv.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_remux.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_scr.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_tc.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_ts.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_web_dl.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gq_webrip.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gr_1080p.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gr_1440p.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gr_144p.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gr_240p.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gr_360p.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gr_480p.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gr_4k.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gr_576p.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gr_720p.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gr_8k.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gs_apple_tv_plus.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gs_crunchyroll.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gs_disney_plus.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gs_hbo_max.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gs_hulu.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gs_netflix.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gs_paramount_plus.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gs_peacock.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gs_prime_video.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_black_and_white.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_criterion.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_directors_cut.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_extended.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_hybrid.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_open_matte.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_proper.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_regraded.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_remastered.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_repack.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_seadex.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_theatrical.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_true_hue.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_uncensored.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gst_uncut.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_10bit.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_12bit.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_8bit.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_ai.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_dolby_vision.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_dolby_vision_hdr.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_dolby_vision_hdr10.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_dolby_vision_hdr10_plus.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_hdr.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_hdr10.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_hdr10_plus.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_hlg.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gv_sdr.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gvf_3d.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_gvf_hdrvivid.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_imax_imax.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable-nodpi/badge_imax_imax_enhanced.webp` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/drawable/shape_player_osd_badge.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/layout/adapter_custom_csp.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/layout/adapter_mpv_config_profile.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/layout/dialog_ai_recommendation_info.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/layout/dialog_custom_csp.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/layout/dialog_display.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/layout/dialog_mpv_config_create.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/layout/dialog_mpv_config_manager.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/layout/dialog_mpv_config_rename.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/layout/dialog_update_settings.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/layout/view_player_osd.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/values-zh-rCN/strings.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/values-zh-rTW/strings.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/values/colors.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/main/res/values/ids.xml` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/main/res/values/strings.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/mobile/java/com/fongmi/android/tv/ui/activity/LiveActivity.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/mobile/java/com/fongmi/android/tv/ui/activity/VideoActivity.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `app/src/mobile/java/com/fongmi/android/tv/ui/adapter/GroupAdapter.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/mobile/java/com/fongmi/android/tv/ui/adapter/LiveAdapter.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `app/src/mobile/java/com/fongmi/android/tv/ui/dialog/ControlDialog.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/mobile/java/com/fongmi/android/tv/ui/dialog/UpdateDialog.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/mobile/java/com/fongmi/android/tv/ui/fragment/SettingPlayerFragment.java` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/mobile/res/layout/adapter_github_proxy.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/mobile/res/layout/dialog_ad_block_stats.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/mobile/res/layout/dialog_control.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `app/src/mobile/res/layout/fragment_setting_player.xml` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |
| `catvod/src/main/java/com/github/catvod/crawler/DebugLogStore.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `catvod/src/main/java/com/github/catvod/crawler/SpiderDebug.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `catvod/src/main/java/com/github/catvod/crawler/diagnostics/DiagnosticLogBuffer.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `catvod/src/main/java/com/github/catvod/crawler/diagnostics/DiagnosticText.java` | DEFER | C101/C107 性能/主题分组，具体风险见前表；不覆盖本地契约。 |
| `chaquo/src/main/java/com/fongmi/chaquo/Loader.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `chaquo/src/main/java/com/fongmi/chaquo/Spider.java` | ADAPT / INTEGRATE | 本轮局部变更，具体兼容处理见功能分组与实际diff。 |
| `chaquo/src/main/python/app.py` | ALREADY_PRESENT | C55 已处理，保留本地适配；不重复早期中间状态。 |

### 旧分季缓存冲突补修（先复现）

新增 `conflictingLegacySeasonSpellingsMustNotPickAnArbitrarySeason` 以同一 site/vod、同一 TMDB ID、两种旧简繁键分别指向第1/2季。原上游回读返回 HashMap 第一个匹配 Entry，断言预期 null 实际非 null，定向复现 FAIL 已保存。最小适配：旧键内容匹配出现不止一个不同记录时拒绝回退，保留全部记录、不自动改季；精确新键仍优先，单条旧键仍兼容。不通过放宽季/ID断言取得命中。随后复测双端 TMDB/分季/手动匹配套件。

## 施工检查点（2026-10-28 11:45，北京时间）

- 当前 HEAD 仍为 d48e68823ae283fd9c7286ea221b0ef75a620eed，main，不提交、不暂存。
- 已实施 TMDB/语言缓存、本地额外消费者、线路实例回声、Python类锁/异常边界、直播身份/显示、资源锁、动画幂等、图片正则、诊断音频探测移除。
- 4处三方上下文冲突已局部解决：两端 LiveActivity 保留原有源按钮结构，只改展示宽度消费；TmdbDetailActivity 不恢复上游 detailPrefetch，保留本地会话/悬浮生命周期；TmdbUIAdapter 保留 seedSeasonEpisodes 交接。Leanback VideoActivity 最终只有 currentTmdbLanguage 一行必要修改，不引入延迟服务绑定。
- C55 mPlaybackFlagKey/inlinePlaybackFlagKey 的实际播放身份保护保持；Listener 的同实例去重与 NO_POSITION 防回声新增，不下沉到用户点击。DANMAKU-AUDIT-01 F4 风险的缺失 guard 得到补齐；没有建立/宣称弹幕、播放失败与历史恢复的共同根因，不改弹幕架构。
- Python 下载、HTTP/重定向/原子缓存等 49 tests PASS；依赖 Exception 继续打印诊断与Logcat，JVM Error 传播，不改cleanup方法的已有 Throwable 资源保护，也不升级Chaquopy。
- 完整测试首次 Leanback 在已输出4073项时 OOM（FAIL/环境），不能记PASS；改用 TEMP init 仅设定 Test maxHeapSize=2g、maxParallelForks=1、forkEvery=80，未改项目配置/断言/排除用例。
- 完整重跑 Leanback 4630项、1失败、2跳过；Mobile 5235项、6失败、2跳过。失败逐项基线源码对照见后文。分季冲突补修后 Leanback 全量再跑（已另存 full3 XML），只剩同一WebHome基线失败。
- 基线对照采用 git archive HEAD 的只读源文件 fixture（TEMP，无.git、无分支/worktree），用原测试类、相同JDK/依赖读取冻结源码。它是源码断言的实际基线复测，不是冻结基线完整APK/运行时测试。Mobile六项失败均已复现；对应源码断言不依赖当前生产对象。将补 Leanback 同一共享断言独立复测。
- 最终定向集会单独统计 C56 与受影响业务、本地保护契约；全量失败继续明确标 FAIL，不用定向绿色替代全量结论。TV/API真实网络与视觉性能均 NOT_VERIFIED。
- task_guard check PASS；Git index 无暂存；新增 diff check=0；基线12空白问题未清理。
- 下一步唯一动作：完成最终双端定向回归与基线共享断言核对，然后构建双端Debug及LeanbackRelease候选，验证Manifest/DEX/ABI/签名并完成交付。

## 最终实施与验收证据

### 已整合及本地适配

| 上游来源 | 实际修改与消费链 | 兼容处理/边界 | 自动化结果 |
|---|---|---|---|
| PR491 c9fb2731b1a6ca60a54d3f4105d0c95090ef7483、d1616410bb99fc30722044d0dcdda309ece49e2f、6c2de578dc1f63b4901089987fbf52bb204e4f5a | TmdbMatcher、Match/Season缓存、MediaTitleParser、详情匹配 | 比较键单向s2t，不改实际query脚本；保留年份/类型/季/ID；旧简体scoped/title键与手动别名可从繁体读回；歧义旧季缓存拒绝任选、保留全部旧数据；新精确键优先 | TMDB/手动/分季/详情/查询词不改写、新现场样本回归 PASS |
| PR491 9996d787ec0c23ddbec0b48ff714134e68801b83 与3220/6d0b评审最终树 | Config→LanguagePolicy→Service/Matcher→Source/UI/Detail/Subtitle/History | languageConfigured进入原配置JSON并copy/持久化；旧JSON非空语言（包括zh-CN）保守视为已有偏好；新配置/明确false可跟随中文UI。额外补 NativeHomeHero、NativeHistoryArtwork、PersonalRecommendationService 与视频排序/人物传记消费者，同源语言用于请求、响应标签、缓存键和守卫。保留凭据来源、subscription scope与本地lite/悬浮策略 | 双端语言偏好/反复切换/序列化/凭据与交接回归 PASS |
| PR493 168a485514c6f92475db7ec0580022c324519ced、b1ef0aa03a1a16ea13142b277bf7003f87f1465e、bbb6d68ff2b8f91c25c75983d4687a58de5d4d9e | Leanback FlagSelectionListener→FlagAdapter→VideoActivity真实切换 | dispatchedItem按实例；NO_POSITION不重开窗口；A-B-A/多步返回重新派发；待执行项过期/取消守卫保留；用户点击不走去重。原mPlaybackFlagKey/inlinePlaybackFlagKey及自动播放、续播判断不替换 | Leanback15、Mobile接线3项 PASS；真实遥控快速切线仍待设备 |
| PR486已有桥接 +056626b2e65f9211b91f9394ba0782b4f2765e12 | app.py→Spider依赖download；Loader构造→startPythonOnce | 下载契约不重复移植；仅把单依赖catch Throwable收窄为Exception，保留日志/finally；类锁守卫CPython启动，真启动失败继续抛出。未升级运行环境 | Python49/49 PASS；双端Java接线/启动源码守卫4/4 PASS，未冒充Android CPython真机并发验证 |
| PR491 d638f6b9a9256e83250d96f874b1a8133bde070b、adcf52019ba35f74384375758136d481e0c8380c、2b405b4b7bd1a71ae1ebeb8b52427923303b8e71、07b9c20b609ed707dc452f49c95960711980fd1d | Live/Group raw身份→LiveConfig兼容同步/keep→LiveBrowse稳定ID→两端Adapter显示；Site能力括号 | displayName为transient/Room Ignore，不新增SQL列/迁移，不清空收藏/屏蔽/观看记录；精确旧source名优先归一回退；重复简繁切换重置displayName，避免对象复用旧译文；能力标签不翻译；Channel原来已分开name/show，保留 | 双端24项 PASS；真实持久化/站源交互待设备 |
| 024ac3f661ed390a2262d6d8916a5a8b5df83519 | App.getResources/invalidateResources | 保留框架Resources/AssetManager，加锁、有限失败重试和恢复；不启用C101字幕预热 | App资源契约及主题回归 PASS；Android9 WebView/语言压力切换待设备 |
| 13be53307caec31a6807e70ecbfeecfffa65d6f2、a5095df3869471e5f132c3c1d5080636ed2273cd | ProgressLayout内容动画、ImageHeaderPolicy图片host匹配 | 重复显示/隐藏幂等，取消旧动画；等价正则预编译。不改变布局、路由、焦点、图片请求头语义 | 对应回归 PASS，不宣称真机帧耗改善 |
| 0363493c98097289133c0ad9605ed8c491f0f74c 的独立日志块 | ExoUtil.buildAudioSink | 只移除为日志同步探测AudioCapabilities；passthrough/directPolicy/offload/Sink装配保持。其余首帧/resize不移植 | 诊断及offload装配回归 PASS；本地耗时 NOT_VERIFIED |

### 已存在，无重复施工

媒体规格127规则/127张WebP、控制栏唤醒和常驻开关、位置与备份恢复均保留C55及后续本地修复。源码catalog/resolver/OSD和徽标资源本轮diff为零；三个新APK的127个资源逐一核对到当前源码SHA256一致。Release资源经过AAPT路径压缩，按resources.arsc导出的资源名→压缩路径映射核查，不能只按ZIP文件名找badge_就误判缺图。

PR489两端实际播放线路稳定键已在本地，未重新整合；更新弹窗/退出确认保留本地动态状态配对，TV与Mobile日志带/评分的C55局部适配也未覆盖。R2连接池后台清理/请求屏障、R3路由序列化/真实Home重启目标均无生产diff，Mihomo定向双端24项均PASS。

### 本地成果保护

原33路径/R2/R3和NATIVE-HOME等已固化于d48基线。本轮无初始dirty，不能再用旧030485b7或未提交指纹来覆盖现有源码。主题Token/ThemeBinder及原生首页布局/导航/历史UI、第二字幕、FFmpeg硬解回退、HLS恢复、PlaybackActivity、Mihomo/OkHttp、WebHome交接、构建签名/version/ABI配置、scripts/build-arm64.ps1均未改。

NativeHomeHero、NativeHistoryArtwork只新增必要语言消费；TmdbDetailActivity只做语言/比较/噪声词、保留代际守卫、季集交接和页内悬浮。没有新增Mobile主页/导航/历史UI，也没有修改实验室布局。当前Source diff与测试完整列于证据清单，0文件删除。

## 自动化结果（不是设备验收）

- 最终双端定向 Gradle BUILD SUCCESSFUL（2m12s）：Leanback1396项、Mobile1236项，0失败、0跳过。
- Python unittest49项，0失败（依赖下载/HTTP失败/重定向/中断/旧缓存保护/空多依赖/日志桥接）。
- 表内分组按类名归类，可重叠，不能相加当总数；0不表示失败，而是该flavor无对应平台测试。

| 验证分组 | Leanback定向 | Mobile定向 | 说明 |
|---|---|---|---|
| C56 | 11 / 0失败 | 11 / 0失败 | 自动化PASS；设备NOT_VERIFIED |
| TMDB | 103 / 0失败 | 110 / 0失败 | 自动化PASS；设备NOT_VERIFIED |
| Flag | 15 / 0失败 | 3 / 0失败 | 自动化PASS；设备NOT_VERIFIED |
| Python | 4 / 0失败 | 4 / 0失败 | 自动化PASS；设备NOT_VERIFIED |
| Live | 24 / 0失败 | 24 / 0失败 | 自动化PASS；设备NOT_VERIFIED |
| Mihomo | 24 / 0失败 | 24 / 0失败 | 自动化PASS；设备NOT_VERIFIED |
| Native | 140 / 0失败 | NOT_APPLICABLE（Leanback专属） | 自动化PASS；设备NOT_VERIFIED |
| Theme | 348 / 0失败 | 326 / 0失败 | 自动化PASS；设备NOT_VERIFIED |
| Playback | 618 / 0失败 | 627 / 0失败 | 自动化PASS；设备NOT_VERIFIED |
| Subtitles | 110 / 0失败 | 113 / 0失败 | 自动化PASS；设备NOT_VERIFIED |

全量测试不标绿：首次Leanback OOM保留FAIL记录；临时测试内存参数解决后完整执行。Mobile六个失败、Leanback共享的一个失败均以冻结HEAD源码fixture复现，不是凭早期checkpoint归因。对照只针对这些读取源文件的断言，未声称完成整套冻结基线运行时测试，也没有改、删或放宽这六项断言。

| 失败方法 | 冻结基线 | C56 | 实际失败条件 |
|---|---|---|---|
| HomeCategoryNavigationSourceTest.movingTypeFocusSwitchesInlineContentWithoutPressingOk | FAIL | FAIL(Mobile) | 源码不含旧postDelayed类别防抖表达式；C56未改HomeActivity |
| TmdbDetailActivityLayoutTest.manualTmdbEpisodeRebindWaitsForWindowFocusBeforeNextFrame | FAIL | FAIL(Mobile) | 断言旧窗口焦点回绑表达式缺失 |
| TmdbDetailActivityLayoutTest.tmdbEpisodeDataIsBoundBackToSourceEpisodesAndRefreshesByDataSeason | FAIL | FAIL(Mobile) | 断言旧单季首次强制fresh probe表达式缺失 |
| TmdbDetailActivityLayoutTest.inlineFullscreenExitRestoresEmbeddedPlayerLayout | FAIL | FAIL(Mobile) | 断言旧detail/fusion退出表达式缺失 |
| VideoActivityLayoutTest.leanbackEpisodeDialogLetsHeaderScrollWithEpisodes | FAIL | FAIL(Mobile) | 旧整页滚动源码断言与既有R1可视矩形方案不一致；未恢复旧算法 |
| WebHomeInteractionWiringTest.leanbackHidesStaleWebHomeUntilBridgeIsReady | FAIL(双端) | FAIL(双端) | 原生主页结构变更后的旧隐藏/bridge源码接线断言不满足 |

以上是基线源码契约不一致，不能仅凭它们静态失败或在基线存在就判定实际UI行为无问题；设备验收/后续测试契约修复仍独立保留。C56唯一调整的既有源码断言是TmdbDetailSourcePayloadTest：由原始getLanguage改为有效请求语言，仍严格验证完整内嵌视频不走网络；Python断言反向禁止吞JVM Error，未削弱业务要求。

## 三份新APK交付

Gradle实际执行 `assembleLeanbackArm64_v8aDebug`、`assembleMobileArm64_v8aDebug`、`assembleLeanbackArm64_v8aRelease`，BUILD SUCCESSFUL，3m29s，386tasks。Release保留R8/资源压缩/lintVital，未用fastRelease，未更换签名。Debug原生输出名由AGP固定，新候选额外复制到下列独立C56目录并逐份核对SHA；Release构建原生输出另有c56后缀。没有覆盖用户Release/apk中旧文件，没有安装。

| 候选 | 交付完整路径 | bytes | SHA256 |
|---|---|---|---|
| leanback-debug | `E:\GitHub\webhtv-suite\app\Release\apk\C56-20261028\leanback-debug-arm64-c56-20261028.apk` | 209261474 | `ab102b5a969cb3e9948d139b24cc03345cb086f66d06dbfb4a5f100f6a187233` |
| mobile-debug | `E:\GitHub\webhtv-suite\app\Release\apk\C56-20261028\mobile-debug-arm64-c56-20261028.apk` | 205479072 | `a728a3f3e7ca5c6ad84a022b8c443c221abc0d7c5f7f4db79ab9727ec8349313` |
| leanback-release | `E:\GitHub\webhtv-suite\app\Release\apk\C56-20261028\leanback-release-arm64-c56-20261028.apk` | 174125530 | `6a9de7aac5569d5717bbb2298418532f1c8bf1f706e48173e80831c4ee4552af` |

三份APK实际DEX读取：GIT_REVISION=d48e68823ae283fd9c7286ea221b0ef75a620eed、GIT_STATE=dirty、BUILD_TAG=C56-20261028、BUILD_TIME=202610281148（北京时间）；对应基线加本轮未提交差异，不是新commit。包名com.silent.android.webhtv、versionName5.6.0/code560、minSdk24/targetSdk28保持。

apksigner三份verify退出0；v2=true，证书SHA256 `97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`，与既有个人使用Android Debug证书一致，不是新正式发布证书。Release真实变体且Manifest与BuildConfig DEBUG均false；两份Debug为true。没有修改签名/密码/包名或版本策略。

ABI：三份APK `lib/`仅arm64-v8a，各30个ELF全部Class64、Machine183/AArch64；Release所有55个.so（含assets）逐文件哈希与旧NATIVE-HOME-11候选一致，0变更。Debug只libcnode.so与libexo_dovi_renderer.so和旧Release不同，属Debug/Release构建产物对比，不代表移植新原生版本。原生库完整清单及SHA存apk-verification.json。

既有风险：assets/libmitv.so仍为Class32/Machine40 ARM，未混进lib/arm64-v8a；不能据此宣布APK整体只支持32位，也不能宣称该动态加载能力在64位进程已兼容。特定MITV站源需设备验收，本轮不重建其原生依赖。构建CXX5202 armeabi-v7a警告如实保留，以上实际包检查优先于任务标签或警告概括。其他警告为既有Room查询字段差异、资源g命名空间、deprecated/unchecked及Gradle10弃用；未因通过构建而关闭检查。

Launcher：三份唯一启用桌面入口为HomeActivityCurrent→真实HomeActivity，HomeActivityHistory别名保留但默认未启用；Release无NativeHomePreviewActivity，错误页:error_activity仍存在。Release实际Setting DEX确认按flavor/stored解析CLASSIC/IMMERSIVE，Debug参数不强制CLASSIC；原生首页、历史、TMDB详情及FusionFloatingPanel等类实际在DEX。证明纳入产物，不代表其播放/Surface/导航已经真机通过。

## 待真机验收与发布边界

所有本轮设备项 NOT_RUN/NOT_VERIFIED：

1. 繁体界面四个现场片名与简体交叉匹配，核对真实TMDB ID/年份/类型/季；接口确无作品的无结果不能当程序故障。未使用用户密钥作在线TMDB真查询，自动化为fixture数据。
2. 反复切简繁，手动语言四种偏好、旧缓存/手动别名、分季冲突提示/重选及详情语言一致性。
3. 遥控器同名不同线路、同实例回声、NO_POSITION刷新、快速A-B-C-B-A、手动选集、不自动播放、历史续播与当前线路区分。
4. 真实Python站源依赖下载、坏依赖受控诊断与CPython并发初始化。
5. 直播旧收藏/屏蔽/上次频道与分组跨简繁切换、站源能力标签、MITV专项。
6. NATIVE-HOME双模式、Hero/顶部分类/焦点滚动，原生历史首排、TMDB详情季集/悬浮/全屏恢复，第二字幕、FFmpeg/HLS/Exo与Mihomo启停。
7. 已有徽标四角/控制栏/常驻状态与字幕弹幕遮挡；本轮未改OSD语义。
8. Android9 WebView语言切换、Android14后台恢复及语言压力切换；无真实设备日志不得宣称资源崩溃或性能已验收。

弹幕此前未复现异常独立保留。本轮产物可用于设备复测；全量历史源码断言仍FAIL、真机未验收、个人签名与既有MITV资产限制均保留，因此不标为正式发布就绪。完成后不提交、不推送，不更新Suite、不继续其他施工。

## 最终全量与工作区验收

最终源码全量：Leanback 4631项 / 1失败 / 2跳过；Mobile 5236项 / 6失败 / 2跳过。仅前述基线失败，未新增失败；最终Mobile全量是在分季冲突补修后独立完成。全量状态仍FAIL，不因已确认原有而改记PASS；定向1396/1236与Python49项PASS单独列明。

基线空白原始证据（本轮只读重现，exit2，不把它改成PASS；引用中的行尾空格用<SP>显示，原始证据文件不变）：

```text
app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewMenuDialog.java:126: new blank line at EOF.
app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewPosterAdapter.java:208: new blank line at EOF.
app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java:816: new blank line at EOF.
app/src/leanback/res/layout/view_native_home_preview.xml:34: new blank line at EOF.
app/src/leanback/res/values/native_home_preview.xml:14: new blank line at EOF.
app/src/leanbackArm64_v8aDebug/AndroidManifest.xml:14: new blank line at EOF.
app/src/leanbackArmeabi_v7aDebug/AndroidManifest.xml:13: new blank line at EOF.
app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeIntegrationTest.java:234: new blank line at EOF.
app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeTest.java:117: new blank line at EOF.
app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeVisualContractTest.java:205: new blank line at EOF.
docs/APP-LAB-UI-DEVICE-FAILURE-FOLLOWUP.md:3: trailing whitespace.
+日期：2026-10-23 <SP><SP>
docs/APP-LAB-UI-DEVICE-FAILURE-FOLLOWUP.md:4: trailing whitespace.
+范围：命令行 DPAD 焦点层级、实验室二级弹窗中的 WebView 加载与可视区域、横屏焦点滚动、实验室弹层统一退出，以及 `LabCommandSheet` 动态参数焦点回归补修。
```

本轮 tracked差异 `git diff --check HEAD` exit0；所有新增未跟踪源码/测试/报告逐项 `git diff --no-index --check /dev/null <path>` 均无空白问题。历史12处未全仓清理。

```text
LOCAL_BASELINE = d48e68823ae283fd9c7286ea221b0ef75a620eed
PREVIOUS_UPSTREAM_TARGET = d9da14dcb77e84d4beb158e83efa74124a1116f7
CURRENT_UPSTREAM_BETA = 004b1f5d363ed32a48a040dd4497311d50179c23
UPSTREAM_NEW_COMMITS = 72
UPSTREAM_CHANGED_FILES = 284
BRANCH = main
OLD_HEAD = d48e68823ae283fd9c7286ea221b0ef75a620eed
NEW_HEAD = d48e68823ae283fd9c7286ea221b0ef75a620eed
FILES_MODIFIED = 41
FILES_ADDED = 16
FILES_DELETED = 0
BASELINE_WHITESPACE_ISSUES = 12 (BASELINE_CHECK_EXIT=2)
NEW_WHITESPACE_ISSUES = 0
DIFF_CHECK_EXIT_CODE = 0
CODE_MODIFIED = YES (仅声明范围内选择性整合)
COMMIT = NO
PUSH = NO
SUITE_GITLINK_UPDATED = NO
STAGED_FILES = 0
WORKTREE_STATUS = DIRTY_EXPECTED_C56
WEBHOME_UNCHANGED = YES (HEAD/status与边界快照一致；未写该仓库)
WOGG_UNCHANGED = YES (HEAD/status与边界快照一致；未写该仓库)
SUITE_INDEX_UNCHANGED = YES
ORIGINAL_RELEASE_BACKUP_PRESERVED = YES (只读SHA核验)
DEVICE_VERIFICATION = NOT_RUN
READY_FOR_TV_RETEST = YES
READY_FOR_FORMAL_RELEASE = NO (基线测试契约与设备验收/已有MITV风险未闭环)
```

交付证据：`Release/apk/C56-20261028/evidence/`，包括完整72提交/284路径、独立源码和测试补丁、逐路径SHA256、失败XML/堆栈、基线对照、定向XML、构建日志、APK清单/DEX/资源映射/签名/ABI和边界检查。source-tests.patch包含本轮新增测试，不包含用户历史备份或凭据；报告单独保留。未运行会创建commit的task_guard finish。

### 精确修改清单

下列路径均相对 `E:\GitHub\webhtv-suite\app`：

| 路径 | 状态 |
|---|---|
| `app/src/leanback/java/com/fongmi/android/tv/ui/activity/LiveActivity.java` | MODIFIED |
| `app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java` | MODIFIED |
| `app/src/leanback/java/com/fongmi/android/tv/ui/adapter/GroupAdapter.java` | MODIFIED |
| `app/src/leanback/java/com/fongmi/android/tv/ui/adapter/LiveAdapter.java` | MODIFIED |
| `app/src/leanback/java/com/fongmi/android/tv/ui/custom/FlagSelectionListener.java` | MODIFIED |
| `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHistoryArtwork.java` | MODIFIED |
| `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeHero.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/App.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/api/config/LiveConfig.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/bean/Group.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/bean/History.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/bean/Live.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/bean/Site.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/bean/TmdbConfig.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/bean/TmdbMatchCache.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/bean/TmdbSeasonMatchCache.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/browse/LiveBrowse.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/player/exo/ExoUtil.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/service/PersonalRecommendationService.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/service/TmdbService.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/subtitle/SubtitleTmdbResolver.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/title/MediaTitleParser.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/title/MediaTitleResolver.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivity.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/ui/custom/ProgressLayout.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/ui/dialog/TmdbSourceDialog.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/ui/helper/TmdbMatcher.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/ui/helper/TmdbSourceAdapter.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/ui/helper/TmdbUIAdapter.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/utils/ImageHeaderPolicy.java` | MODIFIED |
| `app/src/main/java/com/fongmi/android/tv/utils/TmdbLanguagePolicy.java` | MODIFIED |
| `app/src/mobile/java/com/fongmi/android/tv/ui/activity/LiveActivity.java` | MODIFIED |
| `app/src/mobile/java/com/fongmi/android/tv/ui/adapter/GroupAdapter.java` | MODIFIED |
| `app/src/mobile/java/com/fongmi/android/tv/ui/adapter/LiveAdapter.java` | MODIFIED |
| `app/src/test/java/com/fongmi/android/tv/AppResourcesWebViewCompatibilityTest.java` | MODIFIED |
| `app/src/test/java/com/fongmi/android/tv/api/loader/PythonSpiderDiagnosticsWiringTest.java` | MODIFIED |
| `app/src/test/java/com/fongmi/android/tv/setting/FlagSwitchPlaybackWiringTest.java` | MODIFIED |
| `app/src/test/java/com/fongmi/android/tv/ui/activity/TmdbDetailSourcePayloadTest.java` | MODIFIED |
| `app/src/testLeanback/java/com/fongmi/android/tv/ui/custom/FlagSelectionListenerTest.java` | MODIFIED |
| `chaquo/src/main/java/com/fongmi/chaquo/Loader.java` | MODIFIED |
| `chaquo/src/main/java/com/fongmi/chaquo/Spider.java` | MODIFIED |
| `app/src/test/java/com/fongmi/android/tv/bean/C56LiveIdentityCompatibilityTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/bean/C56TmdbLanguageCompatibilityTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/bean/GroupDisplayNameTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/bean/LiveDisplayNameTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/bean/SiteTransBracketTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/bean/TmdbMatchCacheHantTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/bean/TmdbSeasonMatchCacheHantTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/chaquo/PythonStartIdempotenceSourceTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/player/exo/ExoUtilAudioCapabilityLogTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/title/C56TitleIdentityTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/title/MediaTitleParserHantTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/ui/activity/TmdbDetailActivityHantMatchTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/ui/custom/ProgressLayoutContractTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/ui/helper/TmdbMatcherHantTest.java` | ADDED |
| `app/src/test/java/com/fongmi/android/tv/utils/TmdbLanguageFollowUiTest.java` | ADDED |
| `docs/C56-UPSTREAM-SELECTIVE-INTEGRATION.md` | ADDED |
