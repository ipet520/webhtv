# DANMAKU-AUDIT-01：TV 弹幕异常与播放线路失效专项审计

审计日期：2026-10-28，Asia/Shanghai。对象：`E:\GitHub\webhtv-suite\app` 的实际工作区，包含未提交修改。性质：只读故障审计；本文件是本轮唯一仓库内交付，不代表批准修复。

## 1. 结论与证据边界

**尚未建立“开启弹幕 → 成功 Toast 循环 → 播放线路失效 → 历史续播失败”的共同根因链。** 已找到可操作的源码风险和外部参考实现，但没有用户故障会话日志、实际弹幕响应、History 前后快照、安装包身份及 TV 渲染证据，不能把风险直接填写为现场根因。

用户本轮补充确认：使用 TV，进入播放界面后，底部短暂提示“弹幕加载成功”反复出现，随后此前可播放的资源也无法播放。尚未确认：安装 APK 的 flavor/revision、普通点播还是 TMDB 页内/全屏、站源、内核、是否自动切线、提示的实际调用栈。“找不到线路”仍按原反馈保留，不替换为源码里的近似文案。

| 问题 | 本轮结论 | 等级 |
| --- | --- | --- |
| “找不到线路”的实际发出模块 | 当前受审 App 文本源码和弹幕组件中未定位该原文；近似资源 `error_play_flag` 为“暂无线路数据”，且未发现 Java/Kotlin 引用。站源 `Result.msg` 可直接进入播放错误链，但没有现场响应证明该原文来自站源 | NOT_VERIFIED |
| “弹幕加载成功”循环 | 当前 App 的弹幕完成回调和所查 Media3 弹幕源码没有此 Toast。不能将完成回调次数当作 Toast 次数；外部 Spider、实际 APK 差异、其他运行组件均待查 | NOT_VERIFIED |
| 成功却无弹幕 | 已确认组件可以在零条数据时调用 `onLoadCompleted`；加载开关与显示开关可分离；自动搜索缺少取消代次保护。但现场是否零条、隐藏、错集或失去时间轴绑定未确认 | CONFIRMED（代码语义）；现场根因 NOT_VERIFIED |
| 旧搜索影响新会话 | 普通点播自动搜索回调仅检查 `player() != null`；共享 TAG 取消后仍继续候选搜索，且主线程投递没有代次校验 | HIGH_CONFIDENCE |
| Leanback 线路选中自回声 | 本地缺少上游 beta #493 的同实例回声拦截；本地未起播线路的守卫可能失效。上游原故障还有“切线不自动播放”条件，本地不能直接套用其复现结论 | POSSIBLE（现场）；缺失差异 CONFIRMED |
| 删除历史后恢复 | 删除会改变恢复选线、集数、进度、内核偏好及相关轨道/聚合进度，重新进入还重建内存会话。没有证据证明弹幕写坏 History | POSSIBLE（机制）；现场因果 NOT_VERIFIED |
| 本地新增回归 | 弹幕 API 与 Silent main 相同；现有 dirty 线路守卫有相关变化，但不能证明三症状由它引入 | NOT_VERIFIED |

等级按用户定义使用：CONFIRMED 只确认可明确追踪的代码事实；HIGH_CONFIDENCE 表示明确风险、缺少现场事件；POSSIBLE 为有条件候选；RULED_OUT 仅在注明的直接路径范围内排除；NOT_VERIFIED 为证据不足。

## 2. 版本、工作区及外部证据

### 2.1 冻结坐标

| 版本 | 仓库、实际分支 | 完整 SHA | Committer 时间 |
| --- | --- | --- | --- |
| A 当前本地 | `https://github.com/ipet520/webhtv.git`，`main`，加全部工作区改动 | `030485b70c263621fe4575abe5758b28115832c3` | 2026-10-24 01:33:39 +08:00 |
| B Silent 正式线 | `https://github.com/Silent1566/webhtv`，`main` | `edf04f9b5048b6b5bc38c53333ac3c7d62f00265` | 2026-09-22 13:28:17 +08:00 |
| B2 Silent 最新相关线 | 同仓库，`beta` | `d7d64f4360e2cbee4cf6d58400a343971a647b11` | 2026-10-27 19:03:51 +08:00 |
| C 蜂蜜 | `https://github.com/FongMi/TV`，`fongmi` | `123cc086159b0f13bf54bdf64bacbc259ca47c88` | 2026-10-27 01:10:47 +08:00 |

B/B2/C 分支头通过 GitHub GET 实时确认，时间为提交时间，不等于每个模块最后更新时间。B 的 Git 对象已存在于本地，直接 `git show SHA:path` 读取；C/B2 缺少对象的文件通过 GitHub 连接器按固定 SHA 只读取得，没有向 App fetch 对象或修改 remote/ref。

Suite HEAD 为 `1f225767f1fa9a9c3c223aba6c57c137093fd597`；其中 App gitlink 为 `74af2c5137a9380cd454c78098668806b1b27e6a`，与当前 App checkout 不同。用户声明的 stable 真机验收不自动覆盖当前 HEAD + dirty 工作区，也不证明 TV 安装的是本次源码。

审计开始已保存 App/Suite 的完整 porcelain 状态与 `app/src`、`catvod/src`、`chaquo/src` 文件 SHA-256 清单，存于仓库外临时目录：

`C:\Users\Administrator\AppData\Local\Temp\DANMAKU-AUDIT-01-20261028-0844`（工具输出可能显示等价短路径 `ADMINI~1`）。

外部源码快照在该目录的 `Silent-main/`、`Silent-beta/`、`FongMi/`；组件 sources JAR 解包在 `media3-danmaku/`、`media3-ui/`。临时快照没有复制进 Suite 根或 App 源码；可持续引用以下固定 SHA 链接。

已有 dirty 文件全部保护，包括两 flavor `VideoActivity`、`TmdbDetailActivity`、`SiteViewModel`、设置/控制面板、原生历史入口、页内悬浮播放与 Mihomo 文件。未 stash/reset、切分支、合并、cherry-pick、修改 gitlink、构建 APK、读写用户数据库或删除缓存。

### 2.2 组件身份及限制

本地 `gradle/libs.versions.toml:50,135,167` 使用 `androidx.media3:media3-ui-danmaku:1.11.0-alpha01-fongmi`；`settings.gradle:18` 优先本地 Maven。`third_party/media-lock.json:5` 的基础源为 `e3e922d5c01bc0b564849940fe589daf37360d15`，弹幕 live patch 登记于第 9–10 行。

| 本地制品 | SHA-256 |
| --- | --- |
| `third_party/maven/androidx/media3/media3-ui-danmaku/1.11.0-alpha01-fongmi/media3-ui-danmaku-1.11.0-alpha01-fongmi.aar` | `fc789ae689c05e6724ac37fb7a818eb9e1c3175447399e48f9df0e7f806fe21d` |
| 同目录 `media3-ui-danmaku-1.11.0-alpha01-fongmi-sources.jar` | `12f4f4072ba2fff9e8990b5b7f5cb428f5363ccf88ba3f52ed91d153a7c1056e` |

下文组件行号来自该 sources JAR，**没有验证 sources 与二进制逐类一致，也没有验证设备实际打包制品**。C 最新 App 已使用 `DanmakuPlayerViewController` 等不同 API；不能用同一名称推断 A/C 组件兼容，更不能只替换 AAR。蜂蜜 [issue #764](https://github.com/FongMi/TV/issues/764) 也讨论公开 media 源码与 App 依赖不同步，属于外部报告，不作为本机故障证明。

## 3. 本地完整调用链

本节路径均相对 App 仓库 `E:\GitHub\webhtv-suite\app`；`main/`、`leanback/`、`mobile/` 分别简写 `app/src/<sourceSet>/java/com/fongmi/android/tv/`。组件路径均为解包后 `androidx/media3/ui/`。

### 3.1 入口、加载与渲染

| 步骤 | 实际文件、方法、行号 | 数据和调用关系 |
| --- | --- | --- |
| 总设置加载开关 | `leanback/ui/activity/SettingDanmakuActivity.java:57–60`；`mobile/ui/fragment/SettingDanmakuFragment.java:56–59` | 调 `DanmakuSetting.putLoad`；`main/setting/DanmakuSetting.java:14–20` 只改 `danmaku_load`，不会统一开启显示 |
| 播放面板入口 | Leanback `VideoActivity.java:1635–1636,5046–5048` | 点击打开 `DanmakuDialog`；长按走显示开关 |
| 显示开关 | Leanback `VideoActivity.java:5151–5161`；`main/ui/dialog/DanmakuDialog.java:120–125` | 长按只改 show 并提示开/关；面板 load 同时改 load/show，调用 `PlayerManager.setDanmakuEnabled`。没有调用资源解析、选线或历史写入 |
| 自动加载触发 | Leanback `VideoActivity.java:3145–3168`；Mobile 同文件 `3147–3161` | 在 `startPlayer` 分派后自动搜索。不是每次进度更新触发，也不由“加载完成”回调再次触发 |
| 自动搜索资格 | `main/api/DanmakuApi.java:34–40` | load、auto、有效 API 地址；`canAutoSearch(siteDanmakus)` 不看 show，也不因站源已有弹幕而跳过搜索 |
| 搜索请求 | `DanmakuApi.java:42–82,111–144,165–199` | 命中手动集级缓存就投递；否则手动标题、原始/清洗/AI 候选逐级查询。占位 URL GET 或 name/episode POST；共享 TAG `DanmakuApi` |
| 搜索结果解析 | `DanmakuApi.java:84–100`；`main/bean/Danmaku.java:34–84` | 解析的是弹幕源描述（name/url/source），非画面弹幕条目；URL 非空不等于内容有效；自动链没有 HTTP 成功状态校验 |
| 手动搜索 | `main/ui/dialog/DanmakuSearchDialog.java:353–369,422–440,609–631`；`DanmakuSearchInputDialog.java:373–387,573–594` | 调同一 `newCall`；使用 activeCall + SearchIntent 校验；选择时只调 setDanmaku 并写独立匹配记忆，不写 History |
| 绑定源 | `main/player/PlayerManager.java:6838–6892`；`main/player/engine/PlaySpec.java:201–210` | 修改 spec 的 danmakus 列表及 selected；规范化/分类 URL，同 URL 去重，force 重载有防抖；没有把弹幕 URL 赋给播放 URL |
| 静态加载 | 组件 `danmaku/DanmakuController.java:340–388,808–885` | 独立 loader、DataSource、fetcher/parser；用 loadGeneration 拒绝组件旧加载结果 |
| 时间轴 | `main/ui/activity/PlaybackActivity.java:652–685`；组件 `PlayerView.java:568–570,726,782–790`；`DanmakuController.java:409–442,614–649` | PlayerView 把实际 Player 交给控制器；根据 position、speed、isPlaying seek/start/pause/resume；组件 position polling 扩展窗口，不重新跑 App 标题搜索 |
| 渲染 | 组件 `DanmakuView.java:454–510,808` | drawEnabled、时间偏移、窗口及透明度共同决定是否能看见；transparency=1 会透明。实际尺寸、Surface 层级和设备画面未验证 |
| 完成/错误 | `PlayerManager.java:1250–1267,6902–6907,7029–7036` | 单 listener；完成只日志 count/耗时并结束 loading；错误同样只日志并收尾。没有成功 Toast、没有调用播放 onError/startFlow |
| 局域网注入 | `main/server/process/Action.java:150,171–175`；Leanback `VideoActivity.java:6344`；Mobile `7980–7982`；TMDB `11328–11335` | refresh 弹幕事件只重载/设置弹幕；发送文字只调用 sendDanmaku；没有借用 PLAYER 刷新事件重新解析媒体 |

**开启静态弹幕显示不必然启动一次新搜索。** 面板 load/show 只设置开关；若开始播放时 load/auto 未开、无已选源，播放中再开启也可能没有数据可渲染。这是确定的入口行为，不是已证明的用户故障。

### 3.2 四个实际模式

| 模式 | 实际链路与区别 | 仍待设备确认 |
| --- | --- | --- |
| Leanback 普通点播 | `VideoActivity:1443–1444` 绑定控制器；`3154–3167` 自动搜索回调只判 player 非空；`8762` 销毁取消搜索 | Toast 来源、是否错集、控制器/Player 是否同会话 |
| TMDB 沉浸融合页内 | `main/ui/activity/TmdbDetailActivity.java:7770–7799,11151–11154`；`applyInlineDanmaku` 检查 destroyed/service/inlineStarted/result==currentInlineResult/isOwner | 当前影片切集期间旧结果与 currentInlineResult 失效顺序，尺寸与图层 |
| TMDB 全屏/悬浮 | 同 Activity 使用页内控制器和模式切换；显示入口 `9225–9235`，布局调整 `10247,10268,10458`；不能仅因出现 Fusion 名称认定 FusionDetailController 有错 | 页面重挂、全屏/悬浮恢复后实际 PlayerView 和画面 |
| Mobile 原生/Live | Mobile 普通回调同样缺少会话核验；短剧另有专用队列身份检查。Live 两 flavor `LiveActivity:183/227` 绑定控制器，WS 有独立 generation | 用户仅确认 TV，未确认 Mobile/Live 受影响；不能外推 |

`activity_tmdb_detail.xml:1028` 的旧 `master.flame...DanmakuView` 对应本地兼容占位类，其 prepare/start 不绘制真实弹幕。**实际数据加载绑定的是 binding.exo 的 Media3 控制器**，不能看到占位类就认定整个弹幕渲染缺失，也不建议为此替换 Fusion 控制器。

### 3.3 “成功”必须分层

1. HTTP 请求结束：不保证状态成功；本地自动/手动搜索未显式检查 `response.isSuccessful()`。
2. 搜索解析得到源 URL：不保证该 URL 返回可解析弹幕，也不保证正确集数。
3. 内容解析完成：组件 `DanmakuController:843–852` 在没有任何 parser 命中时用空列表，仍回调 count=0；合法空列表也会如此。
4. 数据进入控制器：`setLoadedItems/applySortedItems` 与渲染窗口处理独立于 Toast。
5. 渲染启用：`syncToPlayer:615` 必须 enabled、view、player 都有值；load 不等于 show。
6. 实际可见：时间范围、偏移、当前集、透明度、尺寸、前后台、Surface 层级仍需 TV 证据。

Bili XML parser (`parser/BiliParser.java:35–69`) 读取 `<i>/<d p>`；TXT (`TxtParser.java:37–87`) 读取 `[p]text`；`ParserUtil.parsePAttr` 把秒换成毫秒并过滤不支持 mode。当前没有响应样本，不能判定时间单位错误或零条。

## 4. 线路错误的真实边界

### 4.1 本轮没有定位到原提示发出点

检索当前 App Java/XML、相关 Python/Java 支持代码及本地弹幕组件 sources，未找到“找不到线路”“弹幕加载成功”的固定字面量。`main/res/values-zh-rCN/strings.xml:1627` 的 `error_play_flag` 是“暂无线路数据”，默认语言第 1658 行为 `No flag data`；未找到实际 Java/Kotlin 消费者，因此不能把用户原文等同于此资源。

`main/utils/Notify.java:56–57,90–93` 可以显示任意非空文本，但不会自行重试、定时重复或拼出“弹幕加载成功”。自动搜索、完成 listener、手动搜索成功页均没有调用此成功提示。手动错误提示接受异常消息（`DanmakuSearchInputDialog:430–439`、`DanmakuSearchDialog:543`），与“加载成功”固定文案不同。

尚存的文案来源候选：设备安装的是其他 revision；动态 JAR/Python/JS/远端服务返回或主动提示；未取得的实际运行组件。`main/api/loader/JarLoader.java:153–159` 把 App Context 传给动态初始化方法，故不能用“本地没有字符串”排除外部提示。**上述都是 POSSIBLE，未定位到具体 Spider，更没有把责任归给它。**

### 4.2 可追踪的播放错误链

```text
VideoActivity.getPlayer(flag, episode) [3068–3086]
  -> SiteViewModel.playerContent [181]
  -> SiteApi.playerContent [292–350]
       -> type=3 Spider.playerContent / type=4 服务响应
       -> Result.fromJson / Source.fetch
  -> VideoActivity.setPlayer [3090–3107]
       -> result.hasMsg() 或 realUrl 为空
       -> onError(msg) [6081–6099]
       -> reset/stop/重建内核、showError、startFlow
  -> startFlow/checkParse/checkFlag [7962–7997]
       -> 下一解析/线路/站源（受设置与站源可切换条件限制）
```

`Result.hasMsg` (`main/bean/Result.java:376–377`) 只判断 msg 非空，不判断“成功/失败”语义。**即使第三方在有效播放结果中填“成功”信息，也会被普通点播 setPlayer 当错误处理**；当前没有这样的现场 Result，不能把它写成本次原因。

`SiteViewModel` 按任务类型维护 requestId，PLAYER 分派及异常投递均再检查 ID（`333–380`）；DanmakuApi 不使用 PLAYER LiveData 或其 taskId。解析失败的 `ParseJob` 进入 `PlayerManager.onParseError` (`7077–7086`)；弹幕 `onLoadError` 不进入该链。

所以原提示**尚不能回答为**“播放列表为空 / 站源失效 / 解析失败 / 历史索引越界”。四者均需实际事件或响应分支才能判定。源码确有空 flags 分支（Leanback `checkFlag(Vod):5398–5415`），但它调用 fallback，不发上述原文。

## 5. 异步、重复请求与共享状态

### F1：自动搜索缺少取消代次和播放会话隔离

等级：HIGH_CONFIDENCE；现场相关性 NOT_VERIFIED。

- `DanmakuApi.newCall:45` 每次取消同 TAG 所有调用；`cancel:201–203` 只取消网络，不使已投递的主线程结果失效。
- `searchFirst.onFailure:193–195` 无条件继续下一个标题，不检查 `call.isCanceled()` 或请求代次。旧链被新链/手动搜索取消后，可以继续发请求，再取消新的同 TAG 请求。
- 两条自动搜索链都有有限标题候选，不应称作“单链无限重试”。**多链竞争可产生额外请求及互相取消，仍不等于一次请求触发多个成功回调。**
- `App.post(found)` 和缓存命中投递没有 ID 校验。普通 Leanback/Mobile 回调仅判 player 非空，旧片/旧集结果可以提交给仍存活的共享播放器。组件 loadGeneration 只隔离组件加载，无法辨认这个迟到 URL 属于旧影片；它会把迟到请求当作新的当前源。
- 普通回调没有播放解析、线路选择、History 赋值。直接可预期影响为错集弹幕/覆盖新源/多请求；尚无代码链证明它必然令媒体线路失效。

手动对话框已有 activeCall/SearchIntent 和回调二次核验（见 3.1），不能笼统说“所有弹幕请求都没有保护”。但它与自动搜索共享 TAG，仍可能被旧自动链取消。短剧专用回调（Leanback `3422–3433`）有队列/集数保护，不能把普通入口风险直接套到短剧入口。

### F2：完成回调不代表有效/显示成功

等级：CONFIRMED（语义）；现场对应 NOT_VERIFIED。

组件零条仍完成的代码见 3.3。没有任何一个本轮审阅的完成回调向用户提示“弹幕加载成功”。静态内容一次加载末尾通知一次；分段 fetcher 按 segment 回调 progress，全部完成才调用 completion（`DanmakuController:981–1001`）。换控制器恢复源、切集、媒体重建、force refresh 都可能带来新的加载；不能据此宣称“一次网络请求无限回调”。

`PlayerManager:1235–1267` 是 setListener 替换，不是累加注册；同控制器只重新配置；不同控制器先解除旧 listener 并清数据。组件 `setPlayer:409–432` 解除旧 Player listener 后再绑定。未找到成功回调自调用加载的环。

### F3：加载、显示开关不一致

等级：CONFIRMED（代码行为）；现场对应 POSSIBLE。

设置页 `putLoad(true)` 不改 show；自动搜索资格也不看 show。因此此前隐藏过弹幕时，设置页打开加载仍可能在后台加载而不显示；播放页 load 面板则同步 show。播放中仅打开 show，在无源时也不会主动补一次自动搜索。应先确认用户实际操作入口与 load/auto/show 三值，不能凭提示判断开关状态。

### F4：Leanback 线路选中回声差异

等级：CONFIRMED（外部差异）；本机可触发性/现场因果 POSSIBLE。

本地 `leanback/ui/custom/FlagSelectionListener.java:26–50` 将每次有效选中 post 回 `listener.onItemClick`，没有“同一实例已派发”记录。`VideoActivity:3585` 只在选中且 `isPlaybackFlag` 为真时短路；回调自身又 setSelectedPosition/notify/setEpisodeAdapter/updateFocus (`3589–3594,3674–3678`)，可再产生选中事件。

此处 **HEAD→dirty** 将旧守卫 `resolved.isSelected() && !initialBinding` 改成 `resolved.isSelected() && isPlaybackFlag(resolved) && !initialBinding`，新增 `mPlaybackFlagKey` (`3513–3525`)；本地监听器自身未改。若 UI 选中线路尚未成为实际播放线路，例如 `seamless:3752–3754` 没匹配到历史集而返回，则现守卫不再挡同项回声。这是需要定向验证的风险，不是已复现循环。

Silent beta #493 修复的明确条件还包括“切线不自动播放”；**本地 `onRefresh:4844–4855` 没有该 defer 分支，且真正请求 `getPlayer:3083` 会更新播放线路键，所以不能照搬上游复现结论**。上游报告的 200ms/934 次等计数来自其自己的日志，未当作本用户日志。

该差异可以造成 UI/选线反复进入，但缺少它触发弹幕成功 Toast 的调用链；尤其未找到 Toast 文案，不能统一归因。

### F5：资源身份直接串写的排除范围

等级：RULED_OUT（受审直接赋值路径）。

自动/手动弹幕搜索读取 siteKey/vodId/title/episode/TMDB 身份；`PlaySpec.setDanmaku/addDanmaku:201–210` 只写 danmakus 和 selected，不写 key、媒体 url、播放 Flag、History 或 `mViewModel`。手动记忆写 `DanmakuMatchCache/MediaTitleLearningStore`，没有把搜索结果变成线路权威数据。

`PlaySpec` 会共享弹幕列表引用，但该列表不是 flags/episodes 列表。不能据此认定没有任何间接网络竞争；OkHttp、代理、外部 provider 与 Player 仍处于同一进程/网络环境，资源压力或外部共享状态影响属 NOT_VERIFIED。

## 6. 删除历史为什么可能恢复

### 6.1 实际持久化内容

`main/bean/History.java:50–152` 是 Room Entity；站源/影片身份由 key 中的组成部分加 cid 恢复；包含：

| 字段/状态 | 是否 History 持久化 | 语义 |
| --- | --- | --- |
| key、cid | 是 | 站源影片及配置身份；不是弹幕标题 |
| vodFlag、sourceBindingKey | 是 | 线路名称和 `Flag.stableKey` 绑定键，后者包含用于区分线路的索引信息；不是单独持久化的裸线路整数 |
| vodRemarks、episodeUrl | 是 | 集名/恢复资源身份；episodeUrl 是源站集 URL/ID，不保证是最终解析媒体 URL |
| position、duration、opening、ending、speed、scale、player | 是 | 续播位置、跳过/倍速/比例及记忆内核 |
| tmdbId、mediaType、season/episode | 是 | 聚合身份与季集恢复 |
| subtitleSource、revSort、revPlay 等 | 是 | 字幕偏好、排序与播放顺序 |
| playbackSourceKey、displayIdentity 等 transient 状态 | 否/Ignore | 内存来源/展示辅助，不可当数据库字段 |
| 弹幕开关、URL、弹幕请求 ID、解析后弹幕条目 | 否 | 设置/独立匹配缓存/控制器内存中；未见写入 History |
| 当前最终解析媒体 URL、独立线路索引字段 | 未见对应 History 字段 | 不能按假设宣称保存了失效最终 URL 或越界裸索引 |

### 6.2 恢复与失败写入

普通 Leanback `checkHistory:5419–5458` 使用显式历史 payload，或 `History.findPlayback`，否则新建。`History.findPlayback:256–269` 还可能按 TMDB 聚合、同 key、同名候选恢复，故只删除一个展示项不一定等于完全没有可恢复记录。

`resolveHistoryPlaybackFlag:1268–1275` 调 `TmdbUIAdapter.selectPlaybackFlag:221–254`：绑定键与 URL校验、唯一 URL 候选、同名键保留、旧式 URL/名称回退；不匹配就选第一条。**过期历史通常有回退，不存在一概“历史索引越界必失败”的证据。** WebHome exact 独立入口会拒绝不精确匹配 (`5422–5431`)，本用户没指认该入口，不能混为普通原生恢复。

重要时序：`getPlayer:3084–3086` 发取流请求后立即 `updateHistory`，不等待 READY；`updateHistory:5699–5703` 先 `setHistoryFlag`，后更新 episodeUrl/remarks。`setHistoryFlag:1256–1265` 会先 `syncHistory()` 拷贝异步保存；所以**存在新线路配旧集数的中间快照，以及未成功线路被记住的可能性**。常规 tick/退出 `saveHistory` 可再次保存完整状态；不能把中间快照认定为永久污染。

写入 `History.save:1157–1173` 有事务和删除墓碑保护，但没有“这条线路刚刚播放成功”的条件。播放失败 `onError:6081–6099` 可继续自动换线；若后续仍失败，重新进入可能恢复最后尝试过的线路/内核/集数，而删除后回到初始选择。这是符合源码的候选机制，尚无故障前后字段比较。

普通点播 `updatePlaybackHistoryPosition:6301–6308` 只检查 history/pending-resume/光盘时间轴，不以本集 READY 为前提；切集时旧引擎的正 position/duration 读数是否会影响新记录也需日志。TMDB 页内代码另有 `inlinePlayerMediaReady=false` 防护（`7755–7760`），不能跨入口替代验证。

### 6.3 删除的真实副作用与四类机制

`History.delete/deleteDisplayItem/deleteRelated:1179+` 进入 `PlaybackProgressWriter`，可按聚合身份处理关联记录；writer `444–457` 删除相关季进度快照、History、Track 并协调聚合进度。它不是单纯删一行，也不是“清空弹幕缓存”。`DanmakuMatchCache` 无 History 删除调用；`EpisodePositionCache` 也不能假定随此删除而清空。

| 机制 | 删除后为什么可能改善 | 当前证据 |
| --- | --- | --- |
| 数据库持久化异常 | 清除错误线路/集/内核偏好或中间快照，首次选择改变 | POSSIBLE，缺少字段 diff |
| 内存状态异常 | 退出重进、列表更新、重新查询令旧引用/绑定不再参与 | POSSIBLE，没有删除本身直接清 Player 的证据 |
| 会话生命周期异常 | 新 Activity/Player 绑定或旧异步任务时序改变 | POSSIBLE；取消不等于彻底阻断旧回调，见 F1 |
| 恢复策略选择异常 | 由记忆线路转为第一条，避开已失效的源站路径/进度/内核 | POSSIBLE，源码支持，但无同资源对照 |

轨道删除也可能改变恢复行为，不过正常 `onError:6085` 本身已删除当前 Player key 的 Track，故不能优先宣称“删除历史修好了轨道”。本轮没有读取或清空任何用户历史、配置、缓存，也没有执行删除恢复实验。

## 7. 三方代码对照与历史修复

表内 B 固定为 main SHA，C 固定为 fongmi SHA；外部路径与本地相同前缀时直接写仓库相对全路径。C 重构后的调用顺序不同，差异不等于本地缺陷。

| 模块 | A 本地默影视 | B Silent main | C FongMi/TV | 差异与影响 |
| --- | --- | --- | --- | --- |
| 弹幕请求 | `main/api/DanmakuApi:42–82,111–203`，候选/AI/手动记忆、全局 TAG | 同路径同内容；与 A `git diff B -- file` 为空。最后相关提交 `97a7dd8898c0635fe13b696402c25c57acbfb3be`（09-05 11:10:17 +08） | `app/src/main/java/com/fongmi/android/tv/api/DanmakuApi.java:28–79`，AtomicInteger REQUEST_ID、HTTP/空响应检查 | C 的旧请求失效保护可局部借鉴；不能丢弃本地多标题、语言清洗、缓存逻辑 |
| 加载回调 | `VideoActivity` 两 flavor 见 3.1；普通仅判 player；TMDB `7795–7799` 校验 Result/owner | Leanback `app/src/leanback/.../VideoActivity.java:2994–3007` 同类普通回调 | `playback/vod/VodPlaybackController.java:149–165` 调 renderPlaybackResult → loadDanmaku，再 `156` startPlayback；`VodPlaybackMedia.java:25–32` 有 site.danmaku==0 门控 | A 在 startPlayer 后搜，C 在分派启动前搜；不能认定任一顺序必有错。C API 在主线程消费时检验请求序号 |
| 弹幕渲染 | `PlayerManager:1235–1277,6850–6923` 直接持 PlayerView controller，静态/live 分流、去重、防抖 | `app/src/main/java/com/fongmi/android/tv/player/PlayerManager.java:1308–1350` 同类绑定/完成日志，无成功 Toast | `app/src/main/java/com/fongmi/android/tv/player/PlayerManager.java:940–965` 只选源并 callback；`PlaybackActivity.java:825–856,1007–1025,1149–1150` 持 DanmakuPlayerViewController | C 将视图生命周期归宿主；不能直接替换 A Manager 或组件，避免损伤内核回退/字幕/浮窗 |
| 线路选择 | Leanback `VideoActivity:3578–3599` + `FlagSelectionListener:26–50`；dirty 新增实际线路键守卫 | `app/src/leanback/java/com/fongmi/android/tv/ui/activity/VideoActivity.java:3398–3419,3571–3576` 的 onItemClick/seamless 尚使用旧 selected 守卫 | `VodPlaybackController.java:193–213,527–554`，显式 state/request，selected 幂等；`VodFallbackPolicy.java:60–77` 下一线路或源 | B2 #493 专门修复选中回声；A 缺少。C 大重构不能整体移植以解决单一风险 |
| 历史恢复 | `History:256–269`、Leanback `checkHistory:5419–5458`、`TmdbUIAdapter:221–254`；TMDB/稳定键/原生精确恢复 | `app/src/leanback/.../VideoActivity.java:5136–5160`、`resolveHistoryPlaybackFlag:1203+`，已含同类本地增强 | `playback/vod/VodHistoryPolicy.java:18–34,90–110`、`VodPlaybackController:527–554`；`bean/History.java:36–68,278–285,378–380`，模型更简单 | C 无 A 全套 TMDB 聚合/稳定绑定/记忆内核字段，不能回退 History；两者均会在发请求阶段更新集身份，不是“成功后才写”现成解法 |
| 生命周期 | `PlaybackActivity:652–685,1206,1252`、`PlayerManager:1235–1277`；TMDB Result/owner 防护 | 同类 Manager 绑定设计，普通自动回调缺少播放身份保护 | `api/DanmakuApi:35–53` cancel 增序号；`impl/ApiCallback.java:16–36` UI消费再核验；`PlaybackActivity:1149–1150` close | 借鉴代次+宿主身份校验即可；无需整体改播放架构 |

### 7.1 已实际查看的外部提交

| 提交、时间、源码证据 | 修复内容 | 对本轮的处置 |
| --- | --- | --- |
| [FongMi `179cd159266121932ab570b5188b1f9fccf65461`](https://github.com/FongMi/TV/commit/179cd159266121932ab570b5188b1f9fccf65461)，2026-07-29 01:33:17 +08 | `DanmakuApi` 增加 REQUEST_ID/cancel invalidation、HTTP/空响应检查；新增 `impl/ApiCallback:16–36` 消费时核验并关闭 response | **REFERENCE_CANDIDATE**；A/B main/B2 当前 API 均没有该保护。确认是相关成熟设计参考，未证明修复本用户 Toast 循环 |
| [FongMi `86f1393cfe5a7b3f38e81d513c40e3f71648b8e2`](https://github.com/FongMi/TV/commit/86f1393cfe5a7b3f38e81d513c40e3f71648b8e2)，2026-07-29 01:33:14 +08 | `DanmakuSetting.putLoad(true)` 同时 putShow(true)，show 默认 true | **PARTIALLY_EQUIVALENT**；A 已有默认 true、播放面板 load/show 同步，但总设置 putLoad 没同步；只在明确入口复现后补齐 |
| [FongMi `4c71955fd6912b233aa4acc50d9a6ab635afdd72`](https://github.com/FongMi/TV/commit/4c71955fd6912b233aa4acc50d9a6ab635afdd72)，2026-07-29 01:33:12 +08 | 当时 Manager 持有 controller 时在换 controller/release 时 release | **NOT_DIRECTLY_PORTABLE**；A 当前对 PlayerView 所有权不同，盲目 release 可能释放仍活着的视图控制器；最新 C 又转宿主 close |
| [FongMi `bc20bfebea398946393530178ecab2e3762e5567`](https://github.com/FongMi/TV/commit/bc20bfebea398946393530178ecab2e3762e5567)，2026-04-21 13:01:07 +08 | 旧 `DanPlayer` 在换数据时 release 再 prepare | **SUPERSEDED/INAPPLICABLE**；A 已用 Media3 控制器，不采用旧渲染器补丁 |
| [FongMi `0addfec68454b588c5c77cfbdaaf230714cbece8`](https://github.com/FongMi/TV/commit/0addfec68454b588c5c77cfbdaaf230714cbece8)，2026-08-28 15:56:35 +08 | `PlaybackActivity` 引入 DanmakuPlayerViewController bind/close，整理字幕与弹幕宿主所有权 | **ARCHITECTURAL_REFERENCE_ONLY**；不为本次故障同步整个宿主/字幕架构 |
| [Silent `168a485514c6f92475db7ec0580022c324519ced`](https://github.com/Silent1566/webhtv/commit/168a485514c6f92475db7ec0580022c324519ced)，2026-10-27 16:57:31 +08；[PR #493](https://github.com/Silent1566/webhtv/pull/493) | `FlagSelectionListener:18,29–33,55–61` 记录 dispatchedItem；同实例回声忽略；NO_POSITION 不清窗口，真实切换清窗口 | **REFERENCE_CANDIDATE**；beta 已含，本地缺少；先验证 A 的实际触发条件，不当作弹幕故障既定根因 |
| Silent `b1ef0aa03a1a16ea13142b277bf7003f87f1465e`、`bbb6d68ff2b8f91c25c75983d4687a58de5d4d9e`（同 PR） | 结构断言、同名不同实例、快速 A→B→A、真实 Leanback 选中路径测试强化 | **TEST_REFERENCE_ONLY**；这些提交的测试记录属于上游，不计作本地本轮 PASS |

检索了两仓库弹幕相关 commit、issues 和关闭项；Silent 弹幕关键词搜索无匹配，不能等价于“仓库历史从无修复”，所以另核查最新 beta/PR #493。FongMi [#765](https://github.com/FongMi/TV/issues/765) 是开关入口不一致，[#654](https://github.com/FongMi/TV/issues/654) 是搜索超时，[#707](https://github.com/FongMi/TV/issues/707) 是内容去重诉求；均没有证明“成功 Toast 循环+线路失效+删历史恢复”的同一问题。没有找到可确认针对该完整三症状的现成修复。

### 7.2 固定源码入口

- [Silent main DanmakuApi](https://github.com/Silent1566/webhtv/blob/edf04f9b5048b6b5bc38c53333ac3c7d62f00265/app/src/main/java/com/fongmi/android/tv/api/DanmakuApi.java#L111)
- [Silent main PlayerManager listener](https://github.com/Silent1566/webhtv/blob/edf04f9b5048b6b5bc38c53333ac3c7d62f00265/app/src/main/java/com/fongmi/android/tv/player/PlayerManager.java#L1323)
- [Silent beta FlagSelectionListener](https://github.com/Silent1566/webhtv/blob/d7d64f4360e2cbee4cf6d58400a343971a647b11/app/src/leanback/java/com/fongmi/android/tv/ui/custom/FlagSelectionListener.java#L26)
- [FongMi DanmakuApi](https://github.com/FongMi/TV/blob/123cc086159b0f13bf54bdf64bacbc259ca47c88/app/src/main/java/com/fongmi/android/tv/api/DanmakuApi.java#L28)
- [FongMi VodPlaybackController](https://github.com/FongMi/TV/blob/123cc086159b0f13bf54bdf64bacbc259ca47c88/app/src/main/java/com/fongmi/android/tv/playback/vod/VodPlaybackController.java#L149)
- [FongMi VodHistoryPolicy](https://github.com/FongMi/TV/blob/123cc086159b0f13bf54bdf64bacbc259ca47c88/app/src/main/java/com/fongmi/android/tv/playback/vod/VodHistoryPolicy.java#L90)
- [FongMi PlaybackActivity](https://github.com/FongMi/TV/blob/123cc086159b0f13bf54bdf64bacbc259ca47c88/app/src/main/java/com/fongmi/android/tv/ui/activity/PlaybackActivity.java#L825)

## 8. 验证记录、缺口与最短取证方案

| 项目 | 本轮状态 | 限制 |
| --- | --- | --- |
| 当前 HEAD/dirty、Suite gitlink、固定外部 branch SHA | PASS | 实时源码坐标，不替代安装包坐标 |
| 只读保护核验 | PASS | 2026-10-28 08:59:48 +08:00：4,022 个受保护源码文件 SHA-256 前后一致，新增/删除/内容变化为 0；App porcelain 前后仅新增本报告 |
| 三方源码读取、API/回调/线路/历史/生命周期比较 | PASS（静态） | 含本地打包组件 sources 阅读，没有设备关联 |
| 现有日志 | NOT_VERIFIED | 检查项目文档及附件目录未取得该故障播放会话日志；不使用上游报告日志冒充用户证据 |
| 现有测试覆盖阅读 | PASS（静态） | `DanmakuApiSourceTest` 只测标题顺序；`VideoActivityDanmakuNullSafetyTest` 只测入口空值；现有 FlagSelectionListenerTest 未覆盖 beta 新增回声场景 |
| Gradle/JVM/Robolectric 运行 | NOT_RUN | 本轮未构建或运行；已有结构测试也不能证明同会话因果链 |
| TV 渲染/Surface/网络响应/请求回调计数 | NOT_RUN | 无对应会话及安装包身份 |
| History 前后字段/删历史恢复实验 | NOT_RUN | 保护用户数据；后续仅独立测试环境/测试资源 |

七个场景均为 **NOT_RUN**：①关闭弹幕正常播；②播放中开启；③保持开启重进；④换集；⑤换线；⑥有历史重进；⑦独立数据无历史首次播放。用户反馈提供现象依据，但未变成受控测试 PASS。

最短下一轮取证应在**同一 TV、同一 APK、同一测试资源**上完成：

1. 先记录 APK revision/hash、flavor、播放器内核、普通/TMDB 页内/全屏及站源身份；避免把 TV 设备类型等同于 Leanback APK。
2. 关联已有 `playback-flow` / `video-flow` / `playback-action` / `danmaku` / `player` / `source` 日志，以单调时间与 playbackTraceId 对齐。现有弹幕日志没有完整请求 generation/解析器/显示身份，不能假装已满足关联。
3. 在故障发生的一次播放中，记录 input→load/auto/show→搜索请求开始/结束→候选源→条目解析 count→controller/view/player 标识→绘制启用→媒体错误→fallback→历史保存。先取得成功 Toast 与线路提示的实际调用来源（栈或 provider 响应）；若现有日志无法定位，再单独申请最小诊断改动。
4. 分开计数 search HTTP、弹幕内容/分段 HTTP、callback、Toast、播放器重建/选线次数。对照是否先出现 from==to 回声，还是先有播放错误。正常分段请求不算重复故障。
5. 使用独立历史测试数据记录上述表内恢复字段前后值；先做有/无历史对照，再分别控制“仅新建 Activity/Player”“仅忽略恢复选择”“清测试历史”，不能同时改变多个因素后归因。真实用户历史不删。

记录 URL 时仅保留脱敏 host/path 类别及不可逆摘要；不输出鉴权、完整查询参数、带令牌播放 URL 或配置密钥。本报告无此类数据。

## 9. 推荐最小修复范围（方案，未施工）

### 9.1 可优先评审：自动搜索隔离

候选文件：`main/api/DanmakuApi.java`，两 flavor `ui/activity/VideoActivity.java` 的普通自动回调，以及所需定向测试。借鉴 C 的 REQUEST_ID + `ApiCallback.post` 设计，**保留本地标题清洗、手动匹配记忆、TMDB 季身份、AI fallback、URL 规范化与 Spider 优先**。

- 每次顶层搜索创建 epoch；cancel 递增 epoch；候选链全程携带 epoch，缓存命中与主线程 found 也核验。
- 已取消/过时代次的 onFailure 立即终止，不再 nextCandidate；不同候选属于同一搜索 epoch，不要把内部候选切换误作新顶层任务。
- 普通入口回调校验 owner、Activity 生命周期、影片/集/Result 或 playback generation；借用本地 `applyInlineDanmaku` 思路，不能只加 player 非空判断。
- HTTP 失败/空响应/无有效源应作为搜索失败或耗尽，不让任意错误 body 冒充源；保持现有候选有限回退。
- 如已开播而首次启用 load 时没有源，应明确是否补一次搜索；总设置同步 show 是另一个入口行为选择，先对照用户操作，不混进无关重构。

回归：A 搜索迟到不覆盖 B；取消后零后续请求；旧失败不取消新手动请求；cached post 也失效；快速换集/换线/退出；空结果/HTTP 非 2xx；原始→清洗→AI 顺序、手动集/剧/季记忆、Spider 优先；Exo/MPV 时间轴及 TV 实际可见。通过代码/单测仍不等于现场三症状已修复。

### 9.2 独立候选：线路回声

只有采到本地同实例自刷新重复进入，或先用定向 Leanback 测试证明实际路径后，才移植 beta #493 的 `FlagSelectionListener` 同实例回声守卫。范围可限一个生产文件与测试，不改线路解析器、不回退 `mPlaybackFlagKey` 的实际播放线路识别。

必须覆盖同名不同实例、NO_POSITION、A→B→A、连续真实用户切线、点击线路不被吞、切线后能继续选集，以及本地没有 defer 模式的真实场景。外部 PR 的测试结果不得替代本地运行。

### 9.3 历史与渲染暂不动

不修改 History schema/清库、不删除缓存、不切换渲染器/内核、不改 FusionDetailController。若字段快照证明了中间组合或失败选线被恢复，再考虑把线路/集数更新聚合到同一完整快照、或约束失败恢复选择；这是取证后方案，不预先重构历史系统。

若只有零条解析/错集/隐藏，修相应数据与显示入口；若确认控制器失去实际 Player 绑定，再只修该宿主绑定点。不要无证据同步 FongMi 最新整个播放架构或 AAR。

需保护 C55、FFmpeg 硬解回退、HLS 恢复、原生历史与 WebHome exact 入口、TMDB 分派、第二字幕、最新页内悬浮播放、Mihomo 代理。后续施工涉及本轮前已 dirty 文件，必须先隔离现有工作，不能覆盖用户改动。

## 10. 最终状态

```text
DANMAKU_LOAD_ROOT_CAUSE = NOT_CONFIRMED
DANMAKU_TOAST_LOOP_ROOT_CAUSE = NOT_CONFIRMED
PLAYBACK_LINE_ERROR_ROOT_CAUSE = NOT_CONFIRMED
HISTORY_RECOVERY_RELATION = NOT_CONFIRMED
UPSTREAM_DIFF_FOUND = YES (beta #493 selection echo; main API equals local)
FONGMI_REFERENCE_FIX_FOUND = YES (request generation, cancellation, HTTP validation; not a proven fix for this incident)
LOCAL_REGRESSION_CONFIRMED = NOT_CONFIRMED
RECOMMENDED_FIX_SCOPE = DanmakuApi cancellation/session isolation first; conditional Leanback selection echo; no History schema or player architecture changes
CODE_MODIFIED = NO
COMMIT = NO
PUSH = NO
```

CODE_MODIFIED 指业务源码；仅新增本审计报告。未更新 Suite pointer、未构建 APK、未修改用户历史。完成静态审计后停止，等待用户确认取证/正式最小修复范围。

## Recovery anchor

- 目标：只读追踪三项症状是否同源，给三方固定版本证据与最小方案。
- 当前状态：源码审计完成；现场根因没有确认，不以猜测补齐。
- 本轮文件：仅 `docs/DANMAKU-AUDIT-01.md`。
- 关键风险：普通自动搜索无 epoch；组件零条仍完成；本地 dirty 线路幂等条件与缺失回声守卫；历史先保存线路再更新集的中间快照。
- 下一动作：等待用户确认后，针对同 TV 故障会话取得 Toast/线路提示来源及历史前后快照，再决定施工。
