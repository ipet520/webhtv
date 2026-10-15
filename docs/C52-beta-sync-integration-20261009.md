# C52：将固定 beta 增量语义整合到 WebHTV 本地基线

## Recovery anchor and scope

- 任务 ID：`C52-beta-sync-dev1-20261009`；分支：`sync/upstream-beta-20261009`；lane：`upstream`。
- 本地起点：`14966667cac105d95fae87fa6f4ed2265fc99be1`。上游旧点：`cb82755ae8fc671f19e26a7771939c7bce0b0580`。固定目标：`29d52ab2385702a9350c3099cd94dd4e54c1240f`，Release `v5.6.0-beta-202610091148`。
- 仅处理 `cb82755ae8fc671f19e26a7771939c7bce0b0580..29d52ab2385702a9350c3099cd94dd4e54c1240f` 的净变化；本地对象图显示两端共同祖先是 `59d481e72649e1e0cd9a7ae1082d413df0f581db`。因此不用普通 merge-base 语义合并（它会把旧范围纳入计算），而以固定旧点生成精确差异并对本地基线做三方语义整合。
- 固定范围当前有 77 commits（44 non-merge），净树差异 277 paths、`+11043/-25581`。净删除主要是用户已明确接受的音频指纹/语音广告识别移除。docs-only 与 merge-only 提交不作为功能同步目标。
- 原 App checkout `E:\GitHub\webhtv-suite\app` 是保护现场：分支 `sync/upstream-beta-bugfixes-20261006`、HEAD `14966667...`、29 个 status-only entries 与 `scripts/build-arm64.ps1` 保持不动。实现只在本隔离 worktree。
- 修改路径限于固定差异涉及的 `app/`、`gradle/`、`scripts/` 中实际适用项，以及本任务唯一记录和索引；不恢复 PG 相关代码，不更新 Suite gitlink。

## Decision-ready integration review

### 方案比较

| 方案 | 判断 |
| --- | --- |
| 不同步 | 保留本地行为，但缺少用户明确要求的 TV Light/Dark、Dialog、播放生命周期、episode concurrency、站点搜索、T3→T4 和短剧开关等上游增量，不能满足本任务。 |
| 直接取上游树/整文件 ours-theirs | 拒绝。共同祖先早于固定 `cb82755a`，整树合并可能带入固定范围以前的变化；整文件替换还会丢失本地 theme/focus、Appearance/DoH/About、播放 owner/recovery、Following、popup 和 TMDB concurrency 行为。 |
| 固定 `cb..target` 差异 + 三方语义组合（采用） | 只把锁定范围的实际生产变化带到 `14966667`；非重叠路径采用上游，重叠路径保留本地可验证合同并合入上游独立行为与测试。针对主题、TV/mobile、TMDB、播放、设置 UI 做专项交叉复核。 |

### 已确定的行为适配边界

- **Theme / TV UI**：采纳显式 Light/Dark 模式持久化、TV 浅色资源路径、Dialog theme overlay 与可读性修复；保留本地独立 Focus 角色、`legacyInteractionTokens()` 兼容调用、当前 Appearance / DoH / About 已验证状态、Mobile 自有色彩语义。不会照搬上游对 `ThemeController` 的整文件状态。
- **Episode concurrency**：保留本地 generation 校验、主线程排序完成边界和 `VideoActivity.showEpisodeSegment()` snapshot。上游 `TmdbEpisodeSorter` 的原位 `set()` 不增加 `ArrayList.modCount`；只有在不移除主线程 authority、不扩大共享可变窗口且合同测试通过时才合入。
- **播放生命周期**：引入 loading/seek/Surface 生命周期修复时，保留本地 owner、same-session recovery、source switch、segment retry 与已提交 playback invariants；不根据同名类冲突做整文件选择。
- **广告移除**：接受音频指纹及语音广告识别删除，连同专属 runtime/settings/tests/diagnostics/依赖与过期功能设计材料；保留普通广告拦截、URL/规则、统计、片头片尾、实时字幕及共享媒体信号基础设施。
- **新增能力**：接入站点注入搜索及空状态/排序回归修复、手机缓存布局、T3→T4 本机 API 网关、实验室配置源弹窗修复、短剧禁用语义与其测试；保持 TV / Mobile 各自 layout 分支。
- **构建**：审查上游 Gradle daemon Java 21 供给修复与依赖移除，仅保留跨机器可复现且与本机 JDK 21 Temurin/项目 wrapper 兼容的内容。不引入/推送本机绝对路径脚本。
- **文档**：不批量导入 beta merge-review / delivery-coordinate 文件；保留与被删除功能直接相关的删除说明、被接入的功能契约记录，以及本 C52 任务记录。

### 证据与适用性

| 证据 | 等级/日期 | 支持的判断及局限 |
| --- | --- | --- |
| 固定上游源码、tests、commit history：`git diff cb82755a..29d52ab`；重点 commits `35a97894a294057db8acf6f6eaf2c72b13bd9da2`、`10daccd631071a16d73b362cc88b74edf857735c`、`9555806b70b66d19132785fd757b0b3e2b990400`、`5096941a40304d9aae27c19d706df1454ba4e1e2`、`23e5280f37d90e62aa6b20bf3d3fc15cef62f010`、`56ebd07f10529ce014a20c43289ee34feb3b5892`。 | A：目标对象库，2026-10-09 | 直接证明文件行为、测试和目标 tree；上游实现不自动证明与本地扩展相容，必须适配本地保护合同。 |
| upstream public PR descriptions #419–#426；repo task records `docs/C45-remove-ad-audio-fingerprint-speech.md`、`docs/C45-t4-api-gateway.md`、`docs/C51-beta-merge-review-dev2-20261009.md` 及 C47/C50/C49 记录。 | A：GitHub API/repo history，2026-10-09 | 记录上游验证、已知范围与维护者决策。CLI 不可用；使用只读公开 PR API 与仓库内提交记录。个别设备证据属于 upstream，不替代本任务设备验收。 |
| Android Developers, [Better performance through threading](https://developer.android.com/topic/performance/threads) 与 [Processes and threads](https://developer.android.com/guide/components/processes-and-threads)。 | A：官方平台文档，检索 2026-10-09 | View hierarchy 操作归 UI thread；支持将共享 UI list 的结构重排留在主线程。文档不决定 TMDB 数据模型，行为边界仍由本地/上游测试决定。 |
| Android Developers, [Styles and themes](https://developer.android.com/develop/ui/views/theming/themes)、[Dark theme](https://developer.android.com/develop/ui/views/theming/darktheme)、[App resources](https://developer.android.com/guide/topics/resources/providing-resources)。 | A：官方平台文档，检索 2026-10-09 | 支持用语义 theme attributes 与 day/night qualified resources，不硬编码跨模式颜色；需服从 WebHTV 自有 runtime profile authority。 |
| Android Developers TV, [TV navigation](https://developer.android.com/training/tv/get-started/navigation) 与 [Focus system](https://developer.android.com/design/ui/tv/guides/styles/focus-system)。 | A：官方平台设计文档，检索 2026-10-09 | 明确 focused 与 selected/pressed 是不同 UI 状态，并要求可读、一致的焦点提示；不会强制所有 TV control 用同一种 fill/ring。 |
| WebHTV 当前基线及固定上游相关 unit/source-contract tests；目标 PR 的 in-repo regression tests。 | A：本仓库代码，2026-10-09 | 成熟实现和合同直接覆盖 WebHTV 的 flavor 与协议，优先于外部 app 的假设。没有独立相关项目能比本项目固定协议 tests 更决定 T3→T4/API、Spider、TMDB 语义。 |
| 论文、性能基准或泛化博客。 | 不适用 | 本范围为定向 UI、并发所有权、既有 HTTP 合同修复和功能删除；没有新的媒体解码算法/容量承诺/性能目标，外部 benchmark 不会决定语义选择。 |

## Acceptance criteria

1. 上游适用行为按上表准确进入，不混入 `cb82755a` 以前的树变化或大批仅交付/合并文档。
2. 本地 UI/theme/focus、Mobile/Leanback 分化、episode concurrency、playback owner/recovery 语义保留；冲突区按行为组合，不整文件覆盖。
3. PG remains absent. Audio fingerprint/speech recognition专属代码、设置、tests、diagnostics 与依赖无悬空引用；普通拦截、规则、统计、字幕管线保留。
4. 相关 Mobile/Leanback 定向测试通过；`Theme / Focus / TMDB episode / playback / settings-dialog` 合同无新增失败；`compileMobileArm64_v8aDebugJavaWithJavac` 和 `compileLeanbackArm64_v8aDebugJavaWithJavac` 通过。
5. `git diff --check`、构建路径审阅与固定范围文件清单核对通过。任务未要求/不能声称本次实机验证通过。
6. 单个正常本地 commit；生成本地 annotated recovery tag；只正常 push `sync/upstream-beta-20261009` 到 `origin`，不 push tag、不改 main、不更新 Suite pointer。

## Rollback

完成的 C52 是单一本地提交，回滚边界为 `git revert <C52 commit>`；提交前 HEAD `14966667cac105d95fae87fa6f4ed2265fc99be1` 与 `recovery/C52-beta-sync-integration-20261009/<timestamp>-<commit>` 作为恢复锚点。原工作目录不参与回滚或同步。

## Current status

- 固定范围整合与定向验证已完成，准备由 task guard 进行单次正常提交、创建本地 recovery tag，并推送集成分支。
- 本任务没有进行真机验收；不声称已完成设备验证。
## Final integration dispositions and verification

### Fixed-range commit ledger

The pinned range contains 77 commits: 33 merge transports and 44 single-parent commits. The latter include 17 documentation-only commits and 27 production or mixed commits. Each commit is recorded below; merges are classified as transport because their product effects are attributed to the range's leaf changes.

| Upstream commit | Subject | Disposition | Evidence / boundary |
| --- | --- | --- | --- |
| 57362eb4f2c0 | fix(tv): 修复外观与语言对话框行文字不可读 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 35a97894a294 | feat(theme): 真正打通 TV 浅色模式 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 663ac5e7da34 | fix(tv): 修复外观与语言二级选择弹窗标题不可读 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 8624f6c0d7e9 | refactor(tv): 删除 AppearanceRowTheme 中未被引用的三个辅助方法 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 990f8039d76d | merge: 合并 origin/beta 最新代码（PR#407-#409）并复评修复 TV 浅色迁移的日间白底白字回归 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 414babc1b5c9 | Merge pull request #410 from Silent1566/dev2 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 5c95aa19cde1 | fix(detail): 修复线路/选集 chip 无底色并恢复光影剧幕浅色白色中间层 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 8d03e86d82e9 | merge: 合并 origin/beta 最新代码（PR#410 真正打通 TV 浅色模式）并复评 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 92f9b020f275 | docs(c41): 订正净差异路径数为实测 6 并区分是否含本任务文档 | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 9b168a1f3805 | Merge pull request #411 from Silent1566/dev1 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 58ebb653ff93 | fix: repair mobile cache management layout | **NOT_APPLICABLE** | Adds cache-management UI to CacheManagementDialog, which has no implementation or entry point at local baseline 14966667; layout/test alone is not a valid port. |
| 009d70842246 | docs(tv): 记录最终提交后的设备重新验证结果 | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 5ff8037e58ea | docs(tv): 补充浅色模式路径的设备验证结果 | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 6414185aff91 | 修复短剧源规则无法禁用 | **SAFE_UPSTREAM** | Applicable independent production change; integrated without replacing protected local behavior. |
| f77c4a023692 | fix(tv): keep playback loading visible until new player starts | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 790d678c9eb5 | feat(crash): 崩溃页显示最后加载的蜘蛛源，便于定位外部 JAR 缺陷 | **SAFE_UPSTREAM** | Applicable independent production change; integrated without replacing protected local behavior. |
| 3374def64131 | merge: 合并 origin/beta 最新代码（PR#411）并复评修复播放加载圈守卫泄漏 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 53b54d1b62c3 | Merge pull request #412 from Silent1566/dev2 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 9555806b70b6 | fix: prevent episode list concurrent modification crash | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 12f2639b8d4f | merge: 合并 origin/beta 最新代码（PR#410/#411/#412）并复评修复 TV 固定深色面板对比度回归 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 2a2139bebc9c | Merge pull request #413 from Silent1566/dev3 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 84d76f4c592a | merge: 合并 origin/beta 最新代码（PR#412/#413）并复评补齐短剧源禁用契约 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| d1b7e2750e63 | docs(c42): 记录 dev1 交付坐标并订正合并树与提交树 | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 6ff6a4458cf7 | Merge pull request #414 from Silent1566/dev1 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 2b1082436136 | merge: 合并 origin/beta 最新代码并修复 episode 列表并发 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 056200cda0c3 | Merge pull request #415 from Silent1566/dev4 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 7596ebabb73c | fix(dialog): 弹窗改用主题覆盖层，阻止控件 tint 泄漏到系统编辑菜单 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| a10322a75ce2 | docs(danmaku): 记录提交 7596ebabb7 与恢复标签 | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 756f164ab366 | fix(tv): persist theme editor light and dark mode | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 7169eddc3516 | fix(tv): theme appearance dialog shell | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 10daccd63107 | fix(tv): honor explicit dark and light theme modes | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| fb85759b03a6 | fix(tv): 主题模式解析与选项弹窗跟随活动调色板 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| fb5f2a03df8d | merge: 合并 origin/beta 最新代码（PR#412-#415）并复评弹幕弹窗 tint 泄漏修复 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 2fafb302cc3b | Merge pull request #416 from Silent1566/dev2 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 7357b2c37d6b | feat: add site injection search | **SAFE_UPSTREAM** | Applicable independent production change; integrated without replacing protected local behavior. |
| f85398d35981 | merge: 合并 origin/beta 最新代码（PR#414-#416）并复评修复 LightDialog 外壳透明度双重施加 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 6e3037c569b9 | Merge pull request #417 from Silent1566/dev3 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 05469a967ad3 | fix: 修复站点注入搜索空状态与排序刷新回归 | **SAFE_UPSTREAM** | Applicable independent production change; integrated without replacing protected local behavior. |
| 6e667a236f1c | fix(ci): 修复守护进程 JVM 供给失败导致流水线在测试前中断 | **SAFE_UPSTREAM** | Applicable independent production change; integrated without replacing protected local behavior. |
| d8daedf86c57 | Merge pull request #418 from Silent1566/dev1 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| ce01b45c3c59 | merge: 合并上游 webhtv/webhtv Silent1566 站点注入搜索与手机缓存排版修复 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| c38861962929 | feat(server): add local T3-to-T4 gateway with HTTP contract coverage | **SAFE_UPSTREAM** | Applicable independent production change; integrated without replacing protected local behavior. |
| 56ebd07f1052 | 移除：删除音频指纹去广告与语音广告识别功能，向 Silent1566 上游靠齐 | **UPSTREAM_REMOVAL_ACCEPT** | Accept removal of audio fingerprint ad blocking and speech ad recognition stack, settings, runtime, tests, diagnostics, and rules. |
| 5d4a7e8e1897 | merge: 合并 origin/beta 最新代码（PR#417/#418）并复评 T3→T4 本机网关 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| cd88c907d912 | docs(c46): 复评 beta 合并后的 C45 交付并补齐两处遗留缺口 | **UPSTREAM_REMOVAL_ACCEPT** | Accept deletion of scripts/pull-ad-audio-log.sh only; C45/C46 delivery/review documentation from this commit is not imported. |
| 5e4cc37ba583 | docs(c46): 记录 Nano 资源回退分支的可证伪区分证据 | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| c2761b2ff693 | docs(c46): 记录交付坐标（提交 cd88c907d / tag / PR #420） | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 2283e15d1358 | docs(c46): 修正 revert 检测口径与净差异路径计数 | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 6d11b98128ef | Merge pull request #420 from Silent1566/dev1 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 24265785a33d | Merge pull request #419 from Silent1566/dev3 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 74c39d6dea36 | docs(c46): 记录 dev2 合并 beta 与站点注入搜索/手机缓存排版的评审与验证 | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 1a4d70410c84 | merge: 合并 origin/beta 最新代码（音频指纹移除与 T3→T4 本机网关） | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 46917be7965b | Merge pull request #421 from Silent1566/dev2 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 5096941a4030 | 修复自动下一集黑屏：Surface 未绑定到重建后的引擎 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| b8adf3cc1c9b | merge: 合并 origin/beta 最新代码（PR#421 站点注入搜索与手机缓存排版）并复评自动下一集黑屏修复 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 23e5280f37d9 | 修复：拖拽进度时加载中/网速大概率不显示 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 0582c722216a | fix(mobile): 修复历史记录集数行在壁纸上的对比度不足 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 87380b5cf76f | merge: 合并 origin/beta 最新代码（PR#421 站点注入搜索与手机缓存排版）并复评自动下一集黑屏修复 | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 0a0bed420895 | docs(c48): 记录交付坐标（提交 87380b5cf / tag / PR #422） | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 4174c65ea3dd | Merge pull request #422 from Silent1566/dev1 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 1a4110178fcc | docs(c47): 记录模拟器验收证据与精度边界 | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| a51e6f366f2f | merge: 合并 origin/beta 最新代码（PR#422 自动下一集黑屏修复）并复评 dev2 历史集数行对比度修复 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| a36a1e9c8f29 | docs(c49): 记录交付坐标（提交 a51e6f366f / tag / PR #423） | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| a7154d35e4f7 | fix(lab): 实验室配置源弹窗内容与面板同源，修复默认主题下白字看不清并让主题色生效 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 765dce2e2a4e | Merge pull request #423 from Silent1566/dev2 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 60e657c07484 | fix(lab): 配置源下拉列表改用弹窗主题上下文与语义 item 布局，修复白字浅底 | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| a8b30e09dcc7 | merge: 合并 origin/beta 最新代码（PR#423 历史集数行对比度修复）并复评实验室配置源弹窗修复 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 1e83bdc25a68 | docs(c50): 复评实验室配置源弹窗修复并记录 beta 合并评审，修正下拉列表主题跟随的不实描述 | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| cb8022bf777b | docs(c50): 记录交付坐标（提交 a8b30e09d / 1e83bdc25 / tag / PR #424） | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 7df98107befd | Merge pull request #424 from Silent1566/dev1 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 8dd441b1ccc3 | merge: 合并 origin/beta 最新代码（PR#424 实验室配置源弹窗）并复评 C47 seek 加载圈窗口 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 61e483011ed4 | docs(c49): 记录 dev4 交付坐标（合并提交 8dd441b1ccc / recovery tag / PR #425） | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 75735b58f6f6 | Merge pull request #425 from Silent1566/dev4 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| c5912111a4ab | fix(tv): 首页菜单键弹窗焦点条目文字改用 colorOnPrimary | **MERGE_BOTH** | Overlaps local product contracts; integrated upstream behavior while preserving local theme/focus/playback/concurrency semantics. |
| 1f8fc83369db | merge: 合并 origin/beta 最新代码（PR#425 C47 seek 加载圈窗口）并复评 dev2 首页菜单键弹窗对比度修复 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |
| 7543959f3e0a | docs(c51): 记录 dev2 交付坐标（合并提交 1f8fc83369 / recovery tag / PR #426） | **NOT_APPLICABLE** | Documentation/delivery record only; not part of product implementation. |
| 29d52ab23857 | Merge pull request #426 from Silent1566/dev2 | **NOT_APPLICABLE** | Merge transport; net behavior assessed from the fixed-range leaf changes. |

### Disposition totals and product boundaries

- `SAFE_UPSTREAM`: 6 commits — `6414185aff91` (short-drama rule disable), `790d678c9e` (crash-page Spider source), `7357b2c37d` and `05469a967a` (site-injection search plus regression fix), `6e667a236f` (CI daemon JVM provisioning), `c38861962929` (T3→T4 local gateway).
- `MERGE_BOTH`: 18 commits — `57362eb4f2`, `35a97894a2`, `663ac5e7da`, `8624f6c0d7`, `5c95aa19cd`, `f77c4a0236`, `9555806b70`, `7596ebabb7`, `756f164ab3`, `7169eddc35`, `10daccd631`, `fb85759b03`, `5096941a40`, `23e5280f37`, `0582c72221`, `a7154d35e4`, `60e657c074`, `c5912111a4`. These preserve local UI/theme/focus ownership, episode main-thread sorting/snapshot, and playback lifecycle while integrating the upstream changes.
- `UPSTREAM_SUPERSEDES_LOCAL`: none. No overlapping local behavior was replaced wholesale on the basis of upstream similarity.
- `UPSTREAM_REMOVAL_ACCEPT`: `56ebd07f10` removes the audio-fingerprint and speech-ad-recognition product stack; `cd88c907d9` removes its dedicated `scripts/pull-ad-audio-log.sh` diagnostic. Related delivery/review documents in `cd88c907d9` were not imported.
- `NOT_APPLICABLE`: `58ebb653ff` is a cache-management layout/test change for `CacheManagementDialog`; that class, its layout, and an entry point are absent from local baseline `14966667`, so importing only the layout/test would leave an invalid or unreachable feature. The 17 documentation-only commits and 33 merge transport commits do not add independent product behavior and are not copied as standalone changes.

The 27 production/mixed commits therefore resolve to 6 `SAFE_UPSTREAM`, 18 `MERGE_BOTH`, 2 accepted removal commits, and 1 `NOT_APPLICABLE`. The full 77-commit ledger above covers the entire requested range and no commit outside it.

### Final contract checks

- TV Light/Dark mode persistence, dialog shell/readability, Home menu focused text, and palette-mode resolution are integrated without replacing local runtime ThemeProfile authority or the local Focus contract.
- Theme/Focus state remains semantic: the local Appearance row helper and DoH/About behavior remain; no global `Focused = Focus` policy is introduced. Mobile Appearance retains `legacyInteractionTokens()`.
- TMDB episode title enrichment is generation-checked and structurally sorted on the main-thread completion boundary; Leanback sends adapter-owned episode segment snapshots rather than live `subList` views.
- Playback loading, rebuilt Surface binding, seek feedback, source/line terminal ownership, segment retry, same-session recovery, manual switching, zero-byte recovery, HTTP 417 recovery, binary segment handling, and automatic source recovery remain covered by the retained local code/contracts.
- No PG-only package/player/live/recovery path was reintroduced. Audio fingerprint and speech-recognition classes, settings, rules, tests, and diagnostics were removed. Ordinary URL/rule ad blocking and statistics remain (`HlsAdblockPipeline`, `AdBlockStatsStore`, and `AdBlockChartView` are present).
- Validation: Mobile targeted source/theme/TMDB/playback set, 47 selected test classes, passed; Leanback focused theme/focus/dialog/TMDB/episode/playback set, 144 tests, passed; Mobile and Leanback Debug Java compile tasks passed; `git diff --check` passed. No clean task or true-device verification was run.
- Scope exception: only task record and the existing assessment index were changed under `docs/`; no bulk upstream delivery/review docs were imported.
- Commit and branch push are the final task-guard steps; no App main or Suite gitlink update is part of this task.