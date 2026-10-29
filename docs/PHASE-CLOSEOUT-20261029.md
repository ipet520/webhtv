# PHASE-CLOSEOUT-20261029

## 范围与状态

PHASE-CLOSEOUT-01：只整理既有成果、提交/正常推送App main、Suite本地指针收敛、旧APK清理，不开发新功能、不整合C57。用户本轮声明当前成果已真机验收；此前各任务报告中的NOT_TESTED为当时交付状态，予以保留，不篡改历史记录。本轮未新增设备测试。

App施工前 main=`d48e68823ae283fd9c7286ea221b0ef75a620eed`；origin/main=`ad46fe5da81eacbfbd0465de6b0b85450cf3780a`，fetch后关系0 behind/14 ahead，无远端新增。origin为用户仓库https://github.com/ipet520/webhtv.git。

Suite施工前 main=`1f225767f1fa9a9c3c223aba6c57c137093fd597`，没有remote。按用户补充，**SUITE_PUSHED=SKIPPED_NO_REMOTE**，不创建remote，不构成本轮失败。Suite App指针原为74af2c5137a9380cd454c78098668806b1b27e6a。

WebHome HEAD=`05018ca8d68ff536c39cba2ff55e404f9d440c08`，当前checkout与Suite指针a62b29aca6139b2fe47203cc2d4cb1f95657e8a8不同；本轮不更新。WOGG HEAD=`be71724d3a168ba69dbc2c925d1ac71ac720d355`，是独立Git仓库，有其自身未完成修改，无remote；不纳入Suite gitlink、不提交、不推送。

Suite原有AGENTS.md、docs/BASELINE.md的WOGG增量、docs/WOGG_INTEGRATION.md、APP-MIHOMO-TRAFFIC-ROUTING-AUDIT.md及截图/附件均予以保护。只对BASELINE中App分支/HEAD做必要最小更新，WOGG文字不借机清理。根工作区允许继续显示这些独立未完成项，不谎报全局clean。

## 外部保护与恢复

外部快照：`C:\Users\ADMINI~1\AppData\Local\Temp\phase-closeout-01-20261029-1330`。包含四仓HEAD/分支/remotes/完整v2状态、App及Suite原文件副本、工作区/暂存binary diff、新增清单和SHA-256；App5300文件、Suite57文件的副本逐项SHA通过。WebHome/WOGG114/106文件也记录哈希。

App/Suite `bundle --all` 已创建并 `bundle verify` PASS，可恢复既有提交历史；工作区副本补齐未提交实现。bundle不包含未提交文件，所以恢复必须结合files及patch。没有reset/clean/stash/覆盖源码。

## 成果索引

| 阶段 | 最终实现/归属 | 报告 |
|---|---|---|
| C54/C55及原有定制 | main既有14个未推送提交+阶段前基线，完整随祖先推送 | 原docs及d48e68823a基线 |
| C56 | 简繁身份/缓存/语言、线路回声防护、Python/WebView资源等选择性接入 | [C56](C56-UPSTREAM-SELECTIVE-INTEGRATION.md) |
| HOME01～04 | 原生骨架、双主页、视觉/焦点、按钮/主题 | NATIVE-HOME-01-P1.md、02/03/04.md |
| HOME05～08 | 真实业务、TMDB、背景收敛、性能/焦点修复 | NATIVE-HOME-05/06/07/08.md |
| HOME09～11 | 背景准备、返回/安全区/标题、Release开放 | NATIVE-HOME-09/10.md、NATIVE-HOME-11-RELEASE.md |
| HOME12 | Logo共享准备队列、缓存及标题时序 | [HOME12](NATIVE-HOME-12-REPORT.md) |
| SOURCE01～03 | 原生分组健康、窄型下拉、返回优先级/材质 | NATIVE-SOURCE-01-REPORT.md、02-FOCUS-REPORT.md、03.md |
| NATIVE-HISTORY | 原生历史沉浸展示、删除复用、全局主题/首卡返回 | NATIVE-HISTORY-01.md、NATIVE-HISTORY-01-UI-FIX.md |
| TMDB DETAIL01/02 | TV响应布局、同播放器内嵌/页内悬浮 | TMDB-DETAIL-01.md、TMDB-DETAIL-02.md及FLOAT-AUDIT |
| TMDB INLINE CRASH01 | 空MediaController安全状态 | NATIVE-SOURCE-02-TMDB-INLINE-CRASH-01.md |
| UI OPT01/02 | 主页模式值、下拉、屏显、弹幕动态开启和历史返回 | UI-OPT-01-REPORT.md、UI-OPT-02-REPORT.md |
| Mihomo/Lab/第二字幕等 | 现有主线保留，不重构；SecondarySubtitleCues、ExoSubtitleSession、PlayerManager等在树内 | 既有实验室/Mihomo/字幕设计报告 |

大量文件交叉依赖，采用一个完整阶段整合提交，不拆旧实现。当前99个实际增量路径（源码/资源/测试/报告）全数审核纳入，加本报告共100；未混入APK、缓存、密钥或本地配置。高置信私钥/Token签名扫描无命中；不是完整安全审计。逐路径分类、大小、SHA见外部submission-inventory.json，所有采用文件均小于2MiB且无意外二进制。原文档不移动不删。

## 分支和提交追溯

只有main与self-maintained两个本地分支。self-maintained=74af2c5137a9380cd454c78098668806b1b27e6a，是origin/self-maintained跟踪的已验证独立基线；虽然是main祖先，不是临时任务分支，**保留**。无需合并/删除分支；LOCAL_BRANCHES_MERGED=0，LOCAL_BRANCHES_REMOVED=0。

App整合提交信息：`chore: consolidate validated native UI and playback phase through HOME-12`。本报告随该提交原子归档；精确App坐标由 `git log -1 --format=%H -- docs/PHASE-CLOSEOUT-20261029.md` 查询，发布后Suite同名收尾报告及外部final-status.json记录完整App远端SHA与Suite本地SHA（避免把commit自身SHA写进自身内容的循环）。全部正常提交，禁止amend/force push；不推送临时恢复tag。

## 验证与构建复用

- 本轮生产源码变化=0；5300个原文件与收尾开始快照保持。与HOME12施工前快照比较，范围外不变，HOME12范围增量和当时构建前保存diff一致。
- 复用HOME12完整Debug+R8 Release成功构建，不无意义重建。签名本轮重新apksigner verify PASS；Manifest/ABI/Launcher沿用同哈希审计证据：com.silent.android.webhtv，5.6.0/560，仅arm64-v8a，30个ELF AArch64库；HomeActivityCurrent→HomeActivity。Release debuggable=false。个人授权Debug证书未改。
- HOME12影响回归185/185 PASS（29套）；UI-OPT02 Leanback226/Mobile23 PASS；C56定向Leanback1396/Mobile1236/Python49 PASS。
- **C56历史全量仍FAIL**：Leanback4631中1失败2跳过，Mobile5236中6失败2跳过，既有冻结基线复现；不改成全量PASS。本轮无代码修改，不重跑矩阵、不修复范围外断言。
- 本轮执行提交范围/暂存一致性、冲突/敏感文件/二进制/完整性核验；最终证据记录在Suite收尾和外部目录。

## APK清理

APK_COUNT_BEFORE=29；APK_COUNT_DELETED=27；APK_COUNT_RETAINED=2；SPACE_RECOVERED=4965966415 bytes（约4.62 GiB，仅文件长度）。只保留指定最新Leanback Release/Debug，历史Mobile测试包按用户双包保留策略移除，**不表示Leanback包能够替代Mobile安装**；Mobile源码和构建能力完整保留。

删除前逐个检查绝对路径在Release/apk内、大小/哈希与预演清单一致；无Git跟踪APK，无构建/脚本依赖旧交付包，历史docs引用保留为归档坐标。使用精确路径Remove-Item，未删整个Release。删3个已空旧目录；其他含证据的旧目录保留，尤其最终R8 mapping、诊断材料不删。

| 路径（均位于E:/GitHub/webhtv-suite/app下） | bytes | SHA-256 | 决定 |
|---|---:|---|---|
| `Release/apk/leanback-arm64_v8a-native-home-12-20261029.apk` | 174127046 | `1cef3cfe327b1e84121654350f08a388751b53c28f86e745105f7296b7b86abc` | DELETE |
| `Release/apk/leanback-arm64_v8a-native-source-01-20261028.apk` | 174126170 | `690ce659626e4a20cf46616743c2bd24d5231c0c2e8ae0595708ad980e65af05` | DELETE |
| `Release/apk/leanback-arm64_v8a-native-source-03-20261028.apk` | 174126170 | `e45aeaa40b91ec78c07e9500c16688ed587362ccb84bf4d36a1ac3d142087212` | DELETE |
| `Release/apk/leanback-arm64_v8a-native-source02-inline-crash01-20261028.apk` | 174109786 | `7fc6cb7497790ff5f19f99b16c5ad8feb1f1a1a765c7f518e8fd43acb44f2832` | DELETE |
| `Release/apk/leanback-arm64_v8a-ui-opt-01-20261028.apk` | 174127034 | `88f334df1d995d73952953aed1bbdd2bd1575eb9783c3cd4dc0aa5aacb8641ce` | DELETE |
| `Release/apk/leanback-arm64_v8a-ui-opt-02-20261029.apk` | 174127034 | `8d70b2d4f678ed8d3e0a3c1e50a94eb5d2e6a7dd1bf1e29fa4dc2d3017cc0513` | DELETE |
| `Release/apk/leanback-arm64_v8a.apk` | 174125530 | `8dcc4ae6dbaaf9b8be7d82adec20ff9303166c7f82ca4f59e0ca86f3d405236c` | DELETE |
| `Release/apk/leanback-arm64_v8ac56-20261028.apk` | 174125530 | `6a9de7aac5569d5717bbb2298418532f1c8bf1f706e48173e80831c4ee4552af` | DELETE |
| `Release/apk/mobile-arm64_v8a.apk` | 172265627 | `7db55f18496a2c67eacd71ca24ee266778f078a2d7f5de069f95c22704b2f58b` | DELETE |
| `Release/apk/APP-R2-before-APP-R3-20261025/leanback-arm64_v8a.apk` | 171267700 | `d1600989ccacec02b280e4bf144702c935bd11f034bd25b34a54318c3bc50afa` | DELETE |
| `Release/apk/APP-R2-before-APP-R3-20261025/mobile-arm64_v8a.apk` | 171556791 | `dc775af9e6ddcc2275203e3d8cf473f63568aa3db86ccc7a49ccc78719ca31ca` | DELETE |
| `Release/apk/APP-R3-before-C55-20261026/leanback-arm64_v8a.apk` | 171267700 | `e46ae59b719d199af1e69794e15954367bc13b48770d4c96120f3eb68e98ba5d` | DELETE |
| `Release/apk/APP-R3-before-C55-20261026/mobile-arm64_v8a.apk` | 171556791 | `502db189849d7a74a1b0ce8c03dcc1d79efe57f403f7f5565abec827a5be7698` | DELETE |
| `Release/apk/C56-20261028/leanback-debug-arm64-c56-20261028.apk` | 209261474 | `ab102b5a969cb3e9948d139b24cc03345cb086f66d06dbfb4a5f100f6a187233` | DELETE |
| `Release/apk/C56-20261028/leanback-release-arm64-c56-20261028.apk` | 174125530 | `6a9de7aac5569d5717bbb2298418532f1c8bf1f706e48173e80831c4ee4552af` | DELETE |
| `Release/apk/C56-20261028/mobile-debug-arm64-c56-20261028.apk` | 205479072 | `a728a3f3e7ca5c6ad84a022b8c443c221abc0d7c5f7f4db79ab9727ec8349313` | DELETE |
| `Release/apk/NATIVE-HOME-12-20261029/leanback-debug-arm64-native-home-12-20261029.apk` | 209263110 | `a628df1fde6869eb892f63fbbe630a6a0705035d51e511f589efbd6951aa2668` | KEEP |
| `Release/apk/NATIVE-HOME-12-20261029/leanback-release-arm64-native-home-12-20261029.apk` | 174127046 | `1cef3cfe327b1e84121654350f08a388751b53c28f86e745105f7296b7b86abc` | KEEP |
| `Release/apk/NATIVE-SOURCE-01-20261028/leanback-debug-arm64-native-source-01-20261028.apk` | 209262254 | `c553fa37935cec2f14abe500808d9771bffd77f3de7ed85c5defca6e4456f1eb` | DELETE |
| `Release/apk/NATIVE-SOURCE-01-20261028/leanback-release-arm64-native-source-01-20261028.apk` | 174126170 | `690ce659626e4a20cf46616743c2bd24d5231c0c2e8ae0595708ad980e65af05` | DELETE |
| `Release/apk/NATIVE-SOURCE-02-20261028/leanback-debug-arm64-native-source-02-20261028.apk` | 209262254 | `14396ef7089722a9c2f533e1f717903602a3905b88102934ecd5d2014f5fb6d6` | DELETE |
| `Release/apk/NATIVE-SOURCE-02-TMDB-INLINE-CRASH-01-20261028/leanback-debug-arm64-native-source02-inline-crash01-20261028.apk` | 209262254 | `41c1c0a09123968507794cc908020fd0ebaaf82c9aa2a2a6c3adf2fa0ff1c6c9` | DELETE |
| `Release/apk/NATIVE-SOURCE-02-TMDB-INLINE-CRASH-01-20261028/leanback-release-arm64-native-source02-inline-crash01-20261028.apk` | 174109786 | `7fc6cb7497790ff5f19f99b16c5ad8feb1f1a1a765c7f518e8fd43acb44f2832` | DELETE |
| `Release/apk/NATIVE-SOURCE-03-20261028/leanback-debug-arm64-native-source-03-20261028.apk` | 209262254 | `2144974b7e803daa17b5fbd915047b34ae52cfc72707856d10e6891b1140d96c` | DELETE |
| `Release/apk/NATIVE-SOURCE-03-20261028/leanback-release-arm64-native-source-03-20261028.apk` | 174126170 | `e45aeaa40b91ec78c07e9500c16688ed587362ccb84bf4d36a1ac3d142087212` | DELETE |
| `Release/apk/UI-OPT-01-20261028/leanback-debug-arm64-ui-opt-01-20261028.apk` | 209263110 | `a899f6e8604a3b4e248ee819ee8a0a93bda54efa5578e009ce2c7dc9f912614f` | DELETE |
| `Release/apk/UI-OPT-01-20261028/leanback-release-arm64-ui-opt-01-20261028.apk` | 174127034 | `88f334df1d995d73952953aed1bbdd2bd1575eb9783c3cd4dc0aa5aacb8641ce` | DELETE |
| `Release/apk/UI-OPT-02-20261029/leanback-debug-arm64-ui-opt-02-20261029.apk` | 209263110 | `2ca356f9876377797b05f05b3a779b0d0c7e7aa675bf4e43f417c5aeb4f87f4f` | DELETE |
| `Release/apk/UI-OPT-02-20261029/leanback-release-arm64-ui-opt-02-20261029.apk` | 174127034 | `8d70b2d4f678ed8d3e0a3c1e50a94eb5d2e6a7dd1bf1e29fa4dc2d3017cc0513` | DELETE |

最终保留包：Release174127046 bytes / SHA1cef3cfe327b1e84121654350f08a388751b53c28f86e745105f7296b7b86abc；Debug209263110 bytes / SHAa628df1fde6869eb892f63fbbe630a6a0705035d51e511f589efbd6951aa2668。完整路径见上表，交付材料保留在NATIVE-HOME-12-20261029/evidence。

## 收尾边界

App按现有origin/main正常推送；确认远端包含目标commit后，Suite只更新App gitlink、App基线坐标及本轮协调报告，本地main提交，不配置/推送Suite remote。独立WebHome/WOGG和Suite既有未完成项保持。最终实际坐标与Git状态写入Suite收尾报告及外部final-status.json。禁止C57；完成后停止。
