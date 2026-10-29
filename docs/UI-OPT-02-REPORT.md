# UI-OPT-02：弹幕动态开启与分组条目居中

## 施工前审计

基线main / d48e68823ae283fd9c7286ea221b0ef75a620eed；87条dirty，5294个原文件、23个归档APK已在TEMP/ui-opt-02-20261029逐路径SHA备份。仅接纳清单文件，原PlaybackActivity、PlayerManager、PlaySpec、Result、TMDB/普通点播播放流程、播放器内核、字幕/第二字幕、历史存储全部只读。

A已确认断点：Result.getDanmaku在load关闭时返回新空列表；PlaySpec.from/fromParse把此列表存入danmakus，却通过setSource保留原Result。PlayerManager.getCurrentResult→Result.playbackSnapshot→copy会复制原始danmaku字段。UI-OPT-01的restoreDanmakuSource只读PlaySpec.getDanmakus，开启load后没有重新读取仍可恢复的当前Result。因此有站源数据也会走“无源”，auto关闭时没有setDataSource。不是已经证明渲染器加载成功后不显示。

绑定/时间轴只读核查：VideoActivity.onControllerReady及TmdbDetailActivity.ensureInlineDanmakuController把现有PlayerView的DanmakuController交给PlayerManager；PlaybackActivity.attachSurface在更换实际Player时调用PlayerView.setPlayer。当前本地media3-ui字节码调用DanmakuController.setPlayer；media3-ui-danmaku的setDataSource读取Player.getCurrentPosition，setEnabled(true)调用syncToPlayer(true)并restartPositionPolling。已有加载器存在有效的动态加载和时间轴同步路径，本轮不改Media3/内核或Surface，也不把“URL存在”当成已显示。读取字节码证据保留于evidence。

异步缺口：原DanmakuApi.cancel仅取消OkHttp，没有阻止已排队的缓存回调，取消后的失败回退也可能继续。需要在既有API逻辑中增加取消代次，而非新增客户端/缓存。Dialog还需拒绝本次开启已取消、换片/切集、释放及用户已手选源的回调。

B已确认断点：展开项addGroup显式设置TEXT_ALIGNMENT_VIEW_START；之后styleGroup只改gravity=CENTER，不能覆盖该对齐模式。左右padding本来对称，背景没有不对称inset。只改展开项的gravity和textAlignment，顶部选择器保持START。

## 修改与验证

### A. 修复结果

`DanmakuDialog.onLoadChanged`仍使用既有load/show设置，并取消上一次弹幕搜索。开启后：

1. 检查实际PlayerManager未释放、当前spec非空且有播放key。
2. 读取现有getDanmakus，优先已选源，再选可用首源。
3. 无源时，在load已开启的前提下读取`player.getCurrentResult().getDanmaku()`，恢复先前被过滤但仍保存在当前会话Result里的站源信息。
4. 有源走原`PlayerManager.setDanmaku`；同URL沿用原去重策略，不重新解析视频、重建Player或重新选线。原Controller在setEnabled(true)同步当前位置，setDataSource按当前Player位置建立弹幕窗口。
5. 真无源时才使用原`DanmakuApi.canSearch()`门槛（load && auto &&有效API）。auto关闭/无API时原手动搜索、选文件入口保持，不擅自改auto，不将空源认作加载成功。

`Result`、`PlaySpec`、`PlayerManager`、`PlaybackActivity`、两种VideoActivity、`TmdbDetailActivity`及Media3均未修改。没有新增弹幕缓存、客户端或配置；只读现有会话快照。对真实TCL具体站源的端到端原因关联尚无本轮日志，因此不将单元测试等同真机显示验收。

#### 加载、显示与会话保护

load/show仍沿用原开关语义；仅显示关闭再开由已有setEnabled同步，load关闭再开现在可以补齐缺失数据。每次明确load切换提升Dialog局部sourceGeneration，并调用原API取消入口。

影片级`player.getKey()`在同一影片切集时可能不变；因此除了key，还比较原有`player.getPlaybackTraceId()`。该身份由PlayerManager.beginPlaybackTrace在start/parse/switch-player时生成，本轮不创建第二套播放会话。旧影片、同key的新集数/新会话、释放、关闭后重开，以及已有用户手选源，都不能被旧自动结果覆盖。

原DanmakuApi使用一个全局OkHttp TAG；本轮在这个既有权威内增加AtomicLong取消代次。缓存回调、网络回调和标题回退链都校验同一代次；cancel不仅停止网络，也使已经排队的App.post结果失效。public newCall保留手动请求的原取消行为，内部fallback使用同一逻辑请求，不自己创建新代次。缓存、手动记忆、原始→清理→AI标题顺序、API配置和结果解析不改。

#### 自动化证据与边界

新增`DanmakuDynamicEnableTest`在Leanback和Mobile各7项：使用真实Result、PlaySpec、PlayerManager、DanmakuDialog及DanmakuApi缓存/取消实现。为了不启动真实视频内核，测试绕过PlayerManager构造器并注入同一个Player代理；仅Renderer数据IO被shadow，记录实际到达setDataSource的URI和所读当前位置。

- 播放前load开启：PlaySpec直接有站源，原setDanmaku到加载入口。
- 播放时load关闭：PlaySpec列表确为空；动态开启后恢复原Result站源，相同URI到同一加载入口。
- 设置播放位置120000ms，动态加载读取120000ms；0次seek、0次setMediaItem，Player、PlaySpec、URL/headers和播放速度保持。
- 关闭/重开复用已备源，不重复加载相同URL；新会话重进可以再次恢复原始源。
- 无站源、auto开启、有效API时复用实际匹配缓存；auto关闭或无API不制造源，show/load及auto独立。
- 真实API缓存已排队→cancel后不交付；后一个查询使前一个结果失效。
- 同一影片key、不同PlaybackTrace的切集，释放Player，以及用户已选手动源时，迟到自动结果被拒绝。
- 现有TMDB内联/全屏/浮窗布局及控制隔离回归通过；播放器及渲染视图源码保持，不把布局回归当成真实视频连续性证明。

NOT_TESTED：TCL真实网络弹幕下载/解析、画面实际显示、弹幕与声音的真机同步、真实服务端错误后的重试、设备上的跨内联/全屏/浮窗连续播放。本轮确认“无源断点已补到加载入口”，不声称实际网络/渲染已验收。

### B. 展开分组列表对齐

仅`HomePreviewMenuDialog.addGroup`改变：gravity=CENTER、textAlignment=TEXT_ALIGNMENT_CENTER，保持singleLine/END省略，同时关闭单行TextView的虚拟超宽横向排版，使文字按条目实际内容宽度居中。这不是改变菜单ScrollView或焦点滚动机制。

原左右12dp、上下0 padding不变；40dp条目高度、背景描边/inset、菜单宽高、右侧12dp定位、overlay、主题及顶部选择器均不变。顶部仍为VIEW_START，不被本轮修改。排序“↕ 名称”按同一内容区域居中，不改排序语义。

NativeSourceDrawerTest新增实际Layout中心检查（含compound padding），短/长名正常态与聚焦态、基线不变、ellipsisCount、排序及取消；原长列表滚动、筛选、上下/返回/左右快捷关闭矩阵继续通过。逐方法对照15个窗口、材质、尺寸、定位、焦点、排序和恢复方法，与施工前完全一致。

本地Native Graphics截图`evidence/native-source-dropdown.png`已检查：展开项居中，顶部选择器保持左对齐。该图不是电视截图。

### C. 测试、保护与文件清单

- Leanback：226 tests / 39 suites，0 failures/errors/skips；Mobile：23 tests / 7 suites，0 failures/errors/skips。先执行完整影响回归，再对新增PlaybackTrace身份保护重跑12项/变体定向测试，合并最新XML结果。原UI-OPT-01、菜单筛选/排序/返回矩阵、历史首卡及TMDB浮窗/全屏、原空Controller状态保护均通过。
- 两包一次最终联合构建：assembleLeanbackArm64_v8aDebug + assembleLeanbackArm64_v8aRelease，fastRelease=false，2m30s / 346 tasks；执行R8、资源收缩/优化、Release lint。
- Manifest：com.silent.android.webhtv，versionCode=560 / 5.6.0；默认Launcher HomeActivityCurrent → HomeActivity，旧历史alias仍禁用。仅arm64-v8a / 30个AArch64 ELF；Debug debuggable=true，Release=false。
- 签名证书SHA-256：97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5，沿用已授权个人Debug keystore，未创建新证书。
- 原HEAD与最终HEAD都为d48e68823ae283fd9c7286ea221b0ef75a620eed，main。初始87条dirty，最终91条；index未暂存。不提交、不推送、不创建tag、不更新Suite gitlink。
- 5294个原文件逐路径核验，仅6个授权既有路径改变；新增2份测试及本报告。23个旧归档APK保持哈希不变。WebHome/WOGG HEAD和完整Git状态保持，Suite gitlink保持。
- 独立guard保护82条未接纳的原dirty路径；菜单15个窗口、宽高/定位、主题、焦点、排序、返回恢复函数逐方法与原件一致。仅addGroup的文字对齐参数改变。
- 原始快照/原差异/日志在`%TEMP%/ui-opt-02-20261029`。独立交付目录evidence保存最终测试XML、原始尝试、构建、APK身份校验、保护核验、任务增量、字节码审计和Release mapping。

实际修改：

1. `app/src/main/java/com/fongmi/android/tv/ui/dialog/DanmakuDialog.java`：当前会话原始源恢复，load切换代次、key+PlaybackTrace及手选源保护。
2. `app/src/main/java/com/fongmi/android/tv/api/DanmakuApi.java`：既有逻辑请求的取消代次，防缓存/网络旧结果与取消后的回退链。
3. `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewMenuDialog.java`：仅展开项的gravity、textAlignment及文字实际排版宽度。
4. `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeSourceDrawerTest.java`：实际文字中心、长名、聚焦和排序对齐测试。
5. `app/src/testLeanback/java/com/fongmi/android/tv/ui/dialog/UIOptimizationSharedTest.java`。
6. `app/src/testMobile/java/com/fongmi/android/tv/ui/dialog/UIOptimizationSharedTest.java`：以上两项仅给原PlayerManager shadow补已有trace getter，保持UI-OPT-01回归兼容。
7. `app/src/testLeanback/java/com/fongmi/android/tv/ui/dialog/DanmakuDynamicEnableTest.java`（新增）。
8. `app/src/testMobile/java/com/fongmi/android/tv/ui/dialog/DanmakuDynamicEnableTest.java`（新增）：真实会话数据、加载分派及API取消回归。
9. `docs/UI-OPT-02-REPORT.md`（新增）。

首次测试的导入重名、无无参构造器测试分配以及单行超宽排版测量问题均保留于尝试日志；没有修改生产PlayerManager构造器来迁就测试。最终通过，不将此前失败掩盖为PASS。

### 真机复核顺序

1. 同一影片先开弹幕再播放，与先播放再在原面板开启load，确认站源相同且能显示。
2. 已播放到明显中段后开启，确认弹幕跟随当前位置，视频无跳进度/重载。
3. 关闭再开启、退出重进；auto开/关及无API分别检查，空源时应保留手动入口而非假称已加载。
4. 切集、手动选源、内联/全屏/浮窗切换，确认迟到结果不覆盖。
5. 分组短/长名称、上下滚动、聚焦和排序模式检查实际电视居中；确认顶部按钮与菜单宽高、右侧位置、返回/左右关闭仍正常。

TV_ACCEPTANCE = NOT_TESTED。完成后停止，等待真机验收。



## DEBUG APK

`E:\GitHub\webhtv-suite\app\Release\apk\UI-OPT-02-20261029\leanback-debug-arm64-ui-opt-02-20261029.apk`

大小：209,263,110 bytes / 199.57 MiB

SHA-256：`2ca356f9876377797b05f05b3a779b0d0c7e7aa675bf4e43f417c5aeb4f87f4f`

## RELEASE APK

`E:\GitHub\webhtv-suite\app\Release\apk\UI-OPT-02-20261029\leanback-release-arm64-ui-opt-02-20261029.apk`

大小：174,127,034 bytes / 166.06 MiB

SHA-256：`8d70b2d4f678ed8d3e0a3c1e50a94eb5d2e6a7dd1bf1e29fa4dc2d3017cc0513`
