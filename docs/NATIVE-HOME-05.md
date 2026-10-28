# NATIVE-HOME-05

Scope: native Leanback immersive presentation adapter, same HomeActivity/SiteViewModel authority. Preserve classic, WebHome, Mobile, player and storage. No commit, push or Suite change.

Baseline: 030485b70c263621fe4575abe5758b28115832c3. External full fingerprints and originals: `%TEMP%/native-home-05-20261026-225608`. Guard native-home-05-valid protects 229 pre-existing dirty paths; adopted files have independent original copies.

Decision: retain the accepted view and one HorizontalGridView. Add presentation context state and a callback seam to the existing RESULT request slot, not a second repository. Native site home/category/filter/page semantics remain authoritative. HomeActivity owns dispatch and site selection. TMDB enhancement uses existing matcher, match cache, service disk cache and image selector; no new persisted cache or HTTP client.

Evidence reviewed 2026-10-26: HomeActivity home/category fallback and dispatch; TypeFragment extend/filter/folder/page rules; SiteAdapter hide/sort rules; SiteViewModel task cancellation; TmdbUIAdapter manual match precedence; TmdbService credential scope and disk cache. These current local sources are the maintained authority. No upstream integration is involved. External issue histories, benchmarks and research papers cannot decide these local wiring contracts and are not applicable. Android WindowManager.LayoutParams dimAmount documentation (https://developer.android.com/reference/android/view/WindowManager.LayoutParams#dimAmount) establishes 1 means fully dimmed. Glide target documentation (https://bumptech.github.io/glide/doc/targets.html) establishes clearing targets and explicit image bounds. Official documentation is normative; device-specific performance remains unverified.

Alternatives: no change leaves fixtures and a black drawer backdrop; copying detail/HTML logic duplicates authority. Chosen narrow native adaptation preserves UI and business ownership, at the cost of lifecycle and generation tests. Rollback is removal of task-owned new files and inverse task patch only, retaining every adopted original; never reset/restore the worktree.

Acceptance: actual home/types/filters/pages, source selection and native Vod dispatch; cached context return; stale source/category/focus rejection; TMDB optional; mild overlay with focus trap; scoped tests and complete ARM64 Debug APK. Device checks explicitly NOT TESTED without a connected TV.

## Recovery anchor

Implementation connected: NativeHomeBrowseController uses the existing SiteViewModel RESULT slot; HomeActivity retains site/menu/dispatch authority. NativeHomeModeHost attaches the same accepted view to real data; fixtures remain only in the explicitly enabled standalone stress preview. TMDB uses the existing service/matcher/match cache and source-payload adapter, with 180 ms dwell, two concurrent enhancement jobs at most, scoped in-flight coalescing and generation guards. No adjacent batch prefetch.

Protection check passed: 5,205 original App files fingerprinted; only 10 adopted/authorized originals differ, no originals missing. WebHome 114 and WOGG 106 original files unchanged; heads and Suite gitlinks unchanged. Seven task-owned new files. External originals remain the rollback anchor.

Validation: initial 73-test pass preceded additional lifecycle/identity cases. A later focused view test exposed missing App Gson initialization in the test sandbox; new tests now initialize App independently and assert parsed fixture cardinality. Final regression/build passed below. No device validation claimed.

Drawer evidence: openSourceMenu never hides/clears the underlying home. The former Dialog inherited the default window theme and directly used the full theme scrim alpha as dimAmount. The custom theme editor caps scrim opacity at .85, so 1.0 is not an editor-reachable reproduction. The fix explicitly uses a translucent overlay window and bounds dim to .10-.28, theme-dependent; an .85 profile case verifies mild dim, unchanged Hero/rail/request count and focus restoration. Actual TV preference state and compositor behavior remain unmeasured.

Final regression and complete Debug build passed: 80 tests, zero failures/errors/skips. APK inspection passed (v2 signature, arm64-v8a only, unchanged enabled launcher alias targeting HomeActivity; standalone pressure preview absent from Release manifest). Three ADB devices are offline; no installation or device acceptance performed.

Correction 2026-10-27: the earlier report incorrectly interpreted `addRow(...).setVisibility(GONE)` as hiding the home-style entry. `AppearanceDialog.addRow` returns the right-hand summary TextView (line 160), not the row. GONE correctly hides only the unwanted auxiliary text; the title-only entry remains visible, focusable and clickable. The user's TV result was correct. No setting or other production source was changed in the proposed entry follow-up, and no APK rebuild is needed. Strengthened NativeHomeAppearanceTest explicitly asserts row/title visibility, focusability, actual focus acquisition, clickability and GONE summary, retaining the existing two-choice/persistence tests.

Correction closed: strengthened appearance and routing tests passed (5 tests, zero failures/errors/skips; Gradle BUILD SUCCESSFUL in 46 seconds). Follow-up fingerprints confirm that only NativeHomeAppearanceTest and this report changed; all production files, WebHome/WOGG, HEAD and Suite gitlinks are unchanged. Delivered APK SHA-256 remains e4267817099dda0e546bccaf1bc9acc4d92b88338ad2ce045004c9501b2df239. No production fix or APK rebuild was performed. Next action: wait for the user's TV acceptance of NATIVE-HOME-05.

## 业务接线

| 链路 | 实际复用及必要新增边界 |
| --- | --- |
| 首页 | HomeActivity 的同一个 SiteViewModel，homeContent → SiteApi.homeContent。Result.list 按站源顺序成为同一轨道卡片，保留原 Vod 对象；空推荐使用原生首分类第一页、空 extend 兜底。空结果与异常/Result.msg 分开。 |
| 分类与筛选 | Result.types、Class.filters / Result.filters。100 ms 最后焦点生效；确认仅打开当前分类筛选。按实际 key/init/value 生成 extend，重复选择回默认，三行视口内可纵向访问额外维度。 |
| 分页 | 唯一触发点 NativeHomeBrowseController.focused：接近尾部请求 page+1。loading/ended 防重复，原 pagecount=0 未知页数语义保留，空页终止，失败保留已加载卡片并允许重试。追加按 Vod 身份去重。 |
| 上下文 | 站源 + type + 有序 extend + 目录状态区分展示上下文；保存影片位置和已加载页。A→B→A 使用已有内容；目录返回恢复父上下文；generation、attached 和同一 ContextState 身份共同拒绝旧结果。 |
| 换源 | 使用原 SiteAdapter 的隐藏规则及健康排序生成抽屉项目；确认复用 HomeActivity.setSite → VodConfig.setHome → 原 RefreshEvent.home。手动站源选择保留现有语义，未把快速切源锁或 changeable 误当成新增手动禁用规则。取消抽屉不触发请求。 |
| 卡片分派 | 动作卡复用 ActionCardHelper；目录卡按 TypeFragment 的 extend/categoryContent 规则进入同一轨道的目录上下文；索引站复用 CollectActivity；普通影片及目录内影片复用 VideoActivity.start（包括原目录 mark 语义）。保留 Vod Style/Cate 及其他字段，不把它们转换为 TMDB 内容身份。长按沿用原生搜索行为，动作和目录不误作影片。 |
| 顶部及菜单 | 仍由 HomeButton.getVisibleButtons / Func 控制可见性、可用性和顺序。点播激活真实首分类，首页加载中点击会等待该次结果。其余功能及长按调用 HomeActivity 原分派；菜单键仍走原 onHomeMenuKey 个性设置快捷入口。 |
| 详情返回 | HomeActivity/轨道未重新创建，保留分类、筛选、页、焦点及 selected position。onPause 只停止视觉订阅，onResume 恢复当前 Hero；不重新请求站源首屏。 |
| 隐藏经典宿主 | 暂停其在途 RESULT 请求并记录待恢复页码；原分类重新显示时只恢复一次。根容器隐藏时不参与 checkMore / onLoadMore。没有第二个沉浸分页消费者。 |
| WebHome | getVideo 的既有 WebHome 判断仍先于原生模式选择；原 getVideo(true) 错误回退不变。进入 WebHome 时 detach 沉浸请求和视觉订阅，不拦截其路由。 |

## TMDB 边界

- NativeHomeHero 是展示订阅适配器：复用 TmdbConfig.effectiveCurrent、TmdbMatcher.searchAndMatch(title,Vod)、Setting 的 TmdbMatchCache、TmdbService.detail(...,false)、TmdbImageSelector 和源内嵌数据的 TmdbSourcePayloadParser / TmdbSourceAdapter。没有新的 HTTP 客户端实现、Repository、磁盘缓存或详情页网络流水线副本。
- 先显示站源标题、年份、地区、类型、备注和简介。180 ms 焦点停留后在现有 Task.executor 上读取匹配/详情缓存或异步匹配；同一作用域/站源/影片共享在途任务，最多两个在途任务，容量满时只保留最新焦点等待，不批量预取临近卡片。
- 手动匹配优先；语言兼容及电影/电视剧/年份消歧继续由现有匹配规则决定。自动写匹配缓存时持有 Setting.class 锁，不覆盖手动绑定；完成前复查新手动身份。凭据/订阅代次/完整配置 SHA-256 作用域、焦点 generation 阻止旧结果发布，自动选择的传输路由变化不会误作凭据变化。
- 没有匹配、凭据不可用、失败或非影视站点时，影片浏览/详情入口继续工作。可信背景来自现有选择器；图片 URL 和 Glide 解码尺寸均受限，原图及超大尺寸路径收敛至 w1280，实际解码不超过 1920 宽。Logo 缺失/失败继续用文字标题。新影片先移除上一部视觉内容，再使用站源图片或主题中性背景兜底。
- 两层 ImageView 的 Glide Target 各自保持到交叉淡入完成，再释放旧层；取消、宿主释放和过期回调均受代次检查。海报图片使用原 ImgUtil，内容图片不被主题染色。

## 实际文件清单

修改 11 个原有文件（以施工前内容为基准，不是相对 HEAD 的整个脏补丁；其中最后一项仅为本次报告纠错补充的测试）：

| 文件 | 本轮修改原因 |
| --- | --- |
| app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java | 同一 ViewModel 接线、原生站源/影片分派、真实点播、模式缓存复用与视觉生命周期；保留菜单及 WebHome 权威。 |
| app/src/main/java/com/fongmi/android/tv/model/SiteViewModel.java | 为同一 RESULT 请求槽增加展示回调重载/取消方法；主线程发布前再次验证任务代次。旧 API 保留，既有回调线程规则保留。 |
| app/src/leanback/java/com/fongmi/android/tv/ui/fragment/TypeFragment.java | 隐藏经典宿主不再请求/分页，保存被暂停的页码并按原规则恢复一次。 |
| app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeHost.java | 绑定业务展示、保留请求上下文、视觉订阅暂停/恢复/释放。 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java | 接收真实数据、动态分类和实际筛选维度、真实源抽屉、Hero 更新与卡片回调，不更换轨道。 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewData.java | 卡片保留原 Vod 和图片地址；原本地构造器仅继续支持独立压力预览。 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewPosterAdapter.java | ImgUtil 真实海报、点击/长按、轻量角标及资源回收；DiffUtil 与既有焦点缩放保留。 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewFocusCoordinator.java | 接受真实筛选维度数组及空筛选，不增加轨道滚动调度。 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewMenuDialog.java | 明确透明 Overlay、轻度主题暗化，空列表安全；焦点锁定和返回恢复保留。 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeIntegrationTest.java | 将抽屉 dim 断言更新为修复后的轻度主题窗口策略；其他既有断言不删减。 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeAppearanceTest.java | 报告纠错后显式验证主页样式入口可见、可聚焦和点击，只有 summary 为 GONE；不修改设置实现。 |

新增 7 个文件：

- app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeBrowseController.java：仅展示上下文、请求代次与分页接线。
- app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeHero.java：可取消订阅的现有 TMDB 增强适配。
- app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeBrowseImages.java：现有 Glide 的双背景/Logo 目标管理。
- app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeBusinessTest.java：真实 Result/Vod 语义、分页、缓存、旧结果及共享请求取消。
- app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeHeroTest.java：焦点抑制、在途去重、凭据作用域、失败兜底、内嵌 TMDB、手动身份和图片上限。
- app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeDrawerTest.java：真实数据视图、抽屉 Overlay/焦点/请求边界、筛选及重复推荐稳定 ID。
- docs/NATIVE-HOME-05.md：本轮唯一任务记录及交付报告。

## 构建与产物

执行 `:app:testLeanbackArm64_v8aDebugUnitTest` 的限定 home/homepreview、原主页延期事务/换源、ThemeBinderRuntime、HomeMenuDialog 和 SiteViewModelSubmitAfterClear 用例：**80 PASS，0 FAIL，0 ERROR，0 SKIP**。执行完整 `:app:assembleLeanbackArm64_v8aDebug` 及仅 `:app:processLeanbackArm64_v8aReleaseManifest`：**BUILD SUCCESSFUL，57 秒**。不是全仓测试，也没有构建 Release APK。

- APK：`E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`
- 大小：214,512,345 bytes。
- SHA-256：`e4267817099dda0e546bccaf1bc9acc4d92b88338ad2ce045004c9501b2df239`。
- applicationId：`com.silent.android.webhtv`；versionName 5.6.0 / versionCode 560；debuggable=true。
- APK v2 签名通过；证书 SHA-256：`97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`。
- APK lib/ 仅 arm64-v8a。启用的原 Launcher alias `com.fongmi.android.tv.ui.activity.HomeActivityCurrent` 仍指向 `com.fongmi.android.tv.ui.activity.HomeActivity`；历史 alias 仍禁用，没有新增桌面入口。
- 独立 NativeHomePreviewActivity 仍仅在 Debug Manifest 注册且无 Launcher。**该独立入口是本地压力预览，不用于本轮真实业务验收**。
- Release merged Manifest 无独立预览 Activity；Setting 的 DEBUG + Leanback 模式门控保留。未构建或声称验证 Release APK。

电视在线后可执行（本轮未安装）：

```powershell
adb -s <电视序列号> install -r "E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk"
adb -s <电视序列号> shell am start -n com.silent.android.webhtv/com.fongmi.android.tv.ui.activity.HomeActivityCurrent
```

覆盖安装保留已有模式偏好。模式入口仍为「设置 → 外观与语言 → 主页样式」，可选择「经典模式」或「沉浸模式」；`GONE` 仅隐藏辅助文字。不能把独立压力预览当成真实业务验收入口。

## 真机验收（全部 NOT TESTED）

1. 有推荐、无推荐有分类、仅推荐无分类、无数据及失败站源的首页结果。
2. 真实分类快速左右切换、A→B→A、全部实际筛选维度、默认值与重复选择；分页末端追加及失败后重试。
3. 抽屉打开仍看见背景、Hero、分类、轨道和顶部；遥控器不穿透，返回无闪烁/重载，焦点恢复房子；确认换源和失败恢复。
4. 普通影片实际详情与播放、动作卡、目录/Cate、索引站、不同 Style 站源；详情返回位置和影片焦点。
5. 十项顶部功能的实际站点可用性、点击/长按、点播首分类，以及菜单键全部原生个性快捷行为。
6. TMDB 命中、缺失、手动绑定、语言/凭据变化、快速 A→B→A、错误/离线兜底；真实网络次数与 TV GPU/帧耗时。
7. 1080P/4K 安全边界、真实长按焦点性能、图片交叉淡入；所有主题/筛选/抽屉状态。
8. 经典模式、WebHome 优先级/错误回退及模式往返实际设备行为。Mobile UI 文件未修改，但本轮未运行 Mobile APK 或真机测试。

没有改动真实历史存储、收藏、追更、详情及播放器底层；没有 HTML/WOGG 修改、提交、推送、Suite pointer 更新或实验 stable baseline 提升。
