# NATIVE-HOME-04 — native buttons, theme, automatic local categories

## Recovery anchor

Objective: adopt existing HomeButton/Func dispatch and ThemeController tokens in the accepted immersive layout; add the user's automatic local-category focus behavior without real source/category/TMDB migration. One Debug APK delivery, no commit/push/Suite update. User reports NATIVE-HOME-03 visual/focus acceptance; that does not replace this build's device validation.

Baseline: HEAD 030485b70c263621fe4575abe5758b28115832c3, 245 dirty paths, 5,201 pre-existing App files. Snapshot/original adopted files and isolated guard reside outside the repository under the directory recorded in TEMP/native-home-04-current.txt. Guard protects 226 other original dirty paths. Preserve all original R2/R3/C55/NATIVE-HOME results.

Status: menu-key correction complete. Immersive calls existing onHomeMenuKey instead of forcing Settings; 52 tests/full Debug build and v2 signature passed. Next action: wait for user television acceptance.

## Local evidence / decision before implementation

- HomeButton.getVisibleButtons reads the existing home_button order, filters unavailable Live and disabled Following. HomeButtonDialog saves enabled items in sorted order via HomeButton.save/saveSorted. No new preference keys or function registry.
- HomeActivity.setFunc creates Func per HomeButton; Func owns dynamic text (Following unread and CustomCsp status). FuncPresenter delegates click and long-click to HomeActivity. Reuse those callbacks; immersive VOD remains explicitly pending because classic VOD needs real category context. Do not call getVideo(true) into an unimplemented immersive category route.
- ThemeProfileStore -> ThemeResolver -> ThemeController.current is the palette authority. Tokens include surface/onSurface/primary/focus/scrim/dialog/overlay roles. ThemeBinder excludes artwork and unknown colors and cannot infer the prototype's arbitrary constants; use existing tokens explicitly for owned drawables, mark content images ignored, no new palette store. Dynamic updates refresh color/state objects and attached holders only, never notify the poster adapter.
- BaseActivity currently recreates on THEME events. Narrow HomeActivity override consumes THEME in the immersive host and refreshes tokens in place. Debug-only Manifest adds uiMode handling; HomeActivity handles that configuration in place only when immersive, preserves classic/WebHome recreation and leaves Release/Mobile manifests unchanged.
- HomeActivity.scheduleTypeSwitch uses removeCallbacks + postDelayed(100ms). Factor only that exact replacement primitive/constant into a shared helper, retaining classic semantics. The immersive local category state uses the same primitive, one pending request, generation checks, cached immutable local lists and remembered card position. No second scrolling engine or real SiteViewModel load.

Official evidence (2026-10-26): https://developer.android.com/develop/ui/views/theming/darktheme describes runtime uiMode handling and warns against fixed colors. AndroidX DiffUtil local dependency/API provides identity-based list updates. https://developer.android.com/reference/androidx/recyclerview/widget/DiffUtil fetch timed out; use actual cached API for compatibility. Existing App code/tests are primary behavior authority. Upstream merges/PRs, other projects and performance papers cannot determine this local configuration/dispatch contract and are inapplicable; no upstream/dependency work.

Alternatives: no change leaves mock buttons/fixed colors; copying classic presenters/layout would change the accepted appearance and introduce their own focus wrap; narrow list/callback/token adoption preserves geometry and Grid authority. Category filtering remains local fixtures, not a second source business system. Acceptance: stable function identity after reorder, actual dispatch parity except explicit pending VOD, dynamic theme drawable assertions in light/dark/custom, last-request-only categories/A-B-A cache and unchanged host, prior mode/WebHome/focus regression, Debug APK metadata/signature/ABI, baseline fingerprints.

Scope: homepreview presentation files, NativeHomeModeHost, minimal HomeActivity glue, two Leanback Debug variant manifests, corresponding tests, and this document. Global HomeButton/Func/FuncPresenter/preferences/theme resolver, classic business, Mobile, HTML, player/history/TMDB and Suite remain protected. Rollback requires reviewing reverse deltas against external originals, never HEAD restore/reset.

## 接入结果

### 功能按钮

正式沉浸宿主 NativeHomeModeHost.refreshFunctions 消费 HomeButton.getVisibleButtons，新增 HomeBrowseFunctionAdapter 只保存呈现快照。HomeButton 的启用/排序键和直播、追更过滤规则不变；不增加持久化、注册表或分派清单。Func.create 保留追更未读数、站点注入开关等动态文案。

HomeActivity.setFunc 的既有触发（onResume、追更未读刷新、自定义站点状态回调）同步更新沉浸顶部。DiffUtil 以 HomeButton.id 比较，只有变化时发送顶部差量；以功能ID恢复 selected position，而非用旧排列索引作为身份。隐藏当前项时选择邻近仍可用项；零项回到独立房子，分类继续可达。相同列表不请求焦点、不更新整页。离开顶部再返回也使用保留的功能身份/原生 Grid 位置。

按用户后续纠正，沉浸主页菜单键调用原有 onHomeMenuKey：读取 Setting.getHomeMenuKey，0为既有快捷菜单，其他值沿用 onHomeMenuItem 的功能快捷分派。不得将菜单键固定覆盖为设置；设置入口仍服从原有按钮及菜单配置。经典/WebHome菜单键分派不变，设备验证仍需实际电视。

直播、搜索、收藏、追更、推送、投屏、历史、设置、站点注入9项均通过同一个 FuncPresenter.OnClickListener 委托 HomeActivity.onItemClick/onLongClick。搜索长按仍为当前站源搜索，其他长按返回既有 false。房子继续独立模拟换源，不属于十项配置。

**明确待接入：点播。** 原业务需要真实首页分类和 Folder/Vod 页面上下文，本阶段没有迁移。沉浸模式点击该项提示“点播分类尚未接入沉浸主页”，不误调其他功能、不调用 getVideo(true) 进入空循环。经典点播分派未改。

独立 ADB NativeHomePreviewActivity 仍是显式数量压力测试夹具（0..24项，取 HomeButton.all 的目录名称，操作标为本地演示），不作为真实功能验收入口。正常 HomeActivity 沉浸宿主以0项夹具初始化后立即安装真实列表，屏幕上始终仅有一个顶部 Adapter。原固定 nhp_functions 数组已删除；没有参考图按钮排序硬编码。

### 主题

ThemeController.applyFromPreferences 沿用既有 ThemeProfileStore/ThemeResolver；NativeHomeBrowseView.refreshTheme 对同一 token 快照幂等。HomePreviewStyles、源抽屉及附着海报 Holder 的颜色全部读取该快照，替换拥有的 StateListDrawable/GradientDrawable；后续附着/回收复用的 Holder 在绑定时取最新 token。

| 区域 | 使用的现有语义 |
| --- | --- |
| 顶部文字、房子图标 | OnSurface / OnSurfaceVariant，房子只设置图标 tint |
| 普通焦点轮廓与控件背景 | Focus / SurfaceContainerHigh |
| 一级分类及三行筛选 | Primary / PrimaryContainer及对应On色，Focus轮廓；选中且聚焦时容器色与较粗轮廓保证两种状态可辨，不改变尺寸 |
| 站源抽屉 | SurfaceContainer、dialogOpacity、Scrim透明度，Primary/OnPrimary及Focus状态 |
| Hero与空状态 | OnSurface / OnSurfaceVariant / Shadow |
| 海报标题、评分、画质及轮廓 | OnSurface、Warning、Surface及Focus |
| 背景与渐变 | Surface及Scrim透明度，浅色遮罩较强；左侧文本区保留最低可读遮罩强度，不增加不透明信息面板 |

内容海报/剧照/Logo明确标记 webhtv:ignore，且不设置 tint。评分等属于原生文字控件，按本次授权消费 token。布局、字体大小、120ms/1.04缩放、Grid中心线与边界算法保持。

HomeActivity 仅在沉浸层可见时拦截 THEME 刷新事件做颜色更新；经典/WebHome继续原有事件行为。两个完整 Leanback Debug Manifest 为 HomeActivity 合并 uiMode 处理，夜间模式变化时沉浸层原位刷新，经典/WebHome仍 recreate 保持旧语义。Release Manifest不增加该声明，Mobile不变。返回经典时用原有 ThemeBinder 刷新此前隐藏的经典树和工具栏，避免旧颜色残留。设置返回的 onResume再次解析并幂等刷新，不新建主页。独立 Debug 预览也改用现有 AppCompat/ThemeController 解析，退出路由不变。

### 一级分类自动切换（用户追加要求）

HomeCategorySwitchScheduler 仅提取原有 removeCallbacks/postDelayed(100ms) 原语及常量，经典 HomeActivity仍使用同一 mTypeSwitch 和原有业务判断。沉浸模式显示时取消经典待切换任务；经典回调也守卫沉浸可见状态，两种展示不同时执行业务切换。

一级分类获焦启动至多一个本地待切换；相同已激活、已排队或在途分类不重复排队。新目标提升 generation，快速A-B-C只执行最终目标。向下或确认时先完成当前本地待切换，确认仅打开现有三行筛选。焦点留在分类，内容切换不主动 requestFocus。

HomePreviewData.categoryCards生成不同的演示标题、元信息、海报顺序/素材和背景；不是只改变高亮。每个分类缓存同一不可变列表、海报位置和Hero索引，A-B-A复用。仍是同一海报 View 和 Adapter，类别差量使用 stable card IDs；打开/关闭筛选不刷新内容。模拟站源变更清理旧分类缓存。

CategoryContentSource是未连接生产服务的数据完成接口；本轮默认仅同步本地夹具。已有在途类别结果可缓存复用，只有当前目标/代次可呈现；源 epoch或释放后结果直接丢弃。测试注入延后完成以验证旧结果不会覆盖当前类别。接口要求主线程完成；SiteViewModel/SiteApi、真实失败/分页/网络取消策略均留给后续明确授权，当前没有这些请求。

## 验证记录

开发中已通过基础37项，随后加入既有ThemeBinder回归、异步完成/主题状态/10项窄屏导航断言。一次最终编译因测试源码使用本仓JVM Color替身缺少的 alpha 方法失败，已改为整数alpha提取；不是生产Color API问题。最终结果待下方填写。

自动化使用 Robolectric Android Views 和已有状态/路由契约。方向回落使用 focusSearch/requestFocus模拟 ViewRoot 对未处理方向键的默认行为，不能替代电视真实输入队列、绘制或长按性能验收。没有全仓测试、Mobile构建、Release APK或设备安装。

### 最终命令与结果

```text
JAVA_HOME=E:/DevTools/jdk-21
ANDROID_HOME=E:/Android/Sdk
GRADLE_USER_HOME=E:/DevTools/GradleCache
gradlew.bat :app:testLeanbackArm64_v8aDebugUnitTest
 --tests com.fongmi.android.tv.ui.home.*
 --tests com.fongmi.android.tv.ui.homepreview.*
 --tests com.fongmi.android.tv.ui.activity.HomeActivityDeferredFragmentWorkTest
 --tests com.fongmi.android.tv.ui.activity.HomeSiteSwitchClearsTypesTest
 --tests com.fongmi.android.tv.theme.ThemeBinderRuntimeTest
 --tests com.fongmi.android.tv.ui.dialog.HomeMenuDialog*
 :app:assembleLeanbackArm64_v8aDebug
 :app:processLeanbackArm64_v8aReleaseManifest
 -PchaquopyBuildPython=E:/DevTools/python-3.10/python.exe --offline --no-daemon --console=plain
```

菜单纠正后的最终结果 BUILD SUCCESSFUL（40s，142 tasks，19 executed / 123 up-to-date）。52 tests、0 failures/errors：原33项回归、6项按钮/动态主题/分类集成、5项既有ThemeBinder运行回归、8项既有HomeMenuDialog规则/对比度检查。此轮不再重复处理未变化的Release Manifest。既有资源namespace/格式及Gradle弃用警告未扩大修复。diff --check通过。

| 产物检查 | 实际结果 |
| --- | --- |
| APK | E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk |
| 大小 | 209,259,042 bytes，199.56 MiB |
| SHA-256 | 8C626FCEAE3AC0727B046435B1F0AF67E9347860A2F0AD7C8B8A94D71FDEAB6E |
| 包/版本 | com.silent.android.webhtv，5.6.0 / 560，debuggable=true |
| ABI | aapt badging：仅arm64-v8a |
| 签名 | apksigner verify通过，v2，Android Debug；证书SHA-256：97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5 |
| 默认入口 | HomeActivityCurrent alias → HomeActivity；没有新增桌面图标 |
| Debug配置 | HomeActivity configChanges=0xf80（原0xd80加uiMode）；预览Activity仍显式extra门控，无Launcher filter |
| Release配置 | merged Manifest不含NativeHomePreviewActivity；HomeActivity保留原screenSize/smallestScreenSize/screenLayout/orientation，没有uiMode；仅处理Manifest，不生成Release APK |

### 实际修改文件

以下均相对App仓库，在施工前文件基础上补丁更新，未从HEAD覆盖。

| 既有文件 | 修改原因 |
| --- | --- |
| app/src/leanback/java/com/fongmi/android/tv/ui/activity/HomeActivity.java | 既有功能回调接线、刷新顶部、原位主题事件/夜间配置处理、恢复隐藏经典树主题、复用原有菜单快捷分派、共享分类延时原语 |
| app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeHost.java | 使用真实按钮配置/回调，刷新顶部及主题，不重建宿主 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java | 稳定功能ID、幂等主题刷新、100ms本地分类及代次/缓存/内容接口；原布局及Grid保留 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewData.java | 不同模拟分类的真实本地内容上下文 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewFocusCoordinator.java | 跨区返回顶部遵守更新后的功能选择身份；无新滚动机制 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewMenuDialog.java | 原站源抽屉色彩及状态跟随实时token |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewPosterAdapter.java | 本地分类列表差量、附着/复用Holder消费最新主题；主题不通知Adapter |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewStyles.java | 用现有token替换固定配色，保持原形状/布局 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomePreviewActivity.java | 独立Debug夹具也使用既有AppCompat/ThemeController主题解析 |
| app/src/leanback/res/layout/view_native_home_preview.xml | 初始文字色使用现有主题属性；布局数值不变 |
| app/src/leanback/res/values/native_home_preview.xml | 删除固定顶部功能数组，新增点播待接入提示 |
| app/src/leanbackArm64_v8aDebug/AndroidManifest.xml | 仅Debug HomeActivity处理uiMode |
| app/src/leanbackArmeabi_v7aDebug/AndroidManifest.xml | 同上；未构建32位APK |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeTest.java | 真实按钮回调与模式往返回归 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeVisualContractTest.java | 保留布局/轨道检查，按新授权改为分类获焦自动更新内容 |

新增4文件：

- app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomeBrowseFunctionAdapter.java：真实按钮快照/差量呈现，复用Func与同一回调。
- app/src/leanback/java/com/fongmi/android/tv/ui/home/HomeCategorySwitchScheduler.java：提取既有100ms可撤销延时原语，两个展示复用。
- app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeIntegrationTest.java：6项集成测试。
- docs/NATIVE-HOME-04.md：唯一施工/验收记录。

### 工作区保护

施工前后 HEAD 均为030485b70c263621fe4575abe5758b28115832c3。dirty（包含所有未跟踪路径）245→249，仅增加上述4文件。施工前5,201文件重新SHA-256对照：5,186完全不变、15为上述预期修改、0丢失。隔离guard保护226条其他原有dirty路径；其余已采纳但未修改文件也保持指纹。原始副本及逐路径前后指纹在仓库外 TEMP/native-home-04-20261026-214144。

HomeButton、Func、FuncPresenter、个性设置存储、ThemeProfile/Resolver/Binder/Controller、共享Setting、原几何计算、滚动/缩放常量、Mobile、历史/详情/播放器/TMDB均未修改。WebHome114文件、WOGG106文件及各自HEAD/status不变；Suite gitlink索引项逐字一致。不commit、不push、不更新Suite，不创建分支/worktree或破坏性清理。

## 真机验收清单 / 未完成事项

当前ADB已知3端点全部offline，没有在线电视；本轮未安装运行APK。下列项目全部 **NOT TESTED**，自动化不代替设备接受：

1. 个性设置全部10项/部分/禁用/排序，返回即时生效，退出重进保持；顶部溢出、焦点身份、零项后房子及分类可用。
2. 9项真实功能实际页面启动，搜索长按当前站源语义；点播仅待接入提示。遥控器MENU遵守原有快捷菜单/自定义功能配置，不固定打开设置。
3. 浅→深、深→浅、主题色、自定义主题，选中与聚焦区别，抽屉/筛选打开期间的色彩刷新，模式往返；确认HomeActivity不因这些主题改变而重建。
4. 一级分类无需确认切换内容，100ms快速连移只显示最后目标，向下进入当前轨道、确认筛选、Back恢复、A-B-A位置与背景缓存。
5. 1080P/4K视觉层次、渐变可读性、主题焦点对比度、长按延迟/回弹/裁切，以及WebHome优先级与错误回退。

验收请从正常桌面入口进入HomeActivity并选择沉浸模式，不使用独立压力预览作为业务验收。可用ADB安装命令：

```powershell
adb -s <电视序列号> install -r "E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk"
adb -s <电视序列号> shell am start -n com.silent.android.webhtv/com.fongmi.android.tv.ui.activity.HomeActivityCurrent
```

真实推荐/分类分页、TMDB、真实站源切换及点播上下文仍待后续授权；历史存储和播放链不改。本轮停止，等待用户真机验收。
