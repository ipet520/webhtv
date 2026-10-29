# UI-OPT-01：五项原生界面定点优化

基线：main / d48e68823ae283fd9c7286ea221b0ef75a620eed。初始72条dirty，5291个原文件、20个归档APK已在仓库外TEMP/ui-opt-01-20261028保存逐路径SHA与原差异。仅按清单最小补丁，不提交/推送/更新Suite。

## UI-04 修改前审计（先审计，后施工）

- PlayerButtonSetting：用户隐藏列表、顺序分别持久化；applyVisibility只隐藏配置项，不主动恢复被业务预先GONE的按钮。实际可见性必须先恢复模式支持的按钮，再套用户配置。
- Leanback VideoActivity.setVideoView:1914、Mobile VideoActivity.setVideoView:1925 / refreshDanmakuControls:5569：直接以DanmakuSetting.isLoad决定GONE。这是普通点播退出后按钮消失的直接代码条件，与isShow无关。
- TMDB：VodPlayerControlController.hasDanmakuControl要求player非空且(load || haveDanmaku)。TmdbDetailActivity.updateInlineButtons/updateDetailActionButtons将其用于内联、原生全屏、Fusion浮窗同会话按钮/快捷按钮的能力判断，然后叠加PlayerButtonSetting。关闭加载且无已选源时会失去入口；并非真实播放模式限制。
- DanmakuSetting三个独立键：danmaku_load默认false，danmaku_show默认true，danmaku_auto默认false。设置页仅切load或auto；播放器DanmakuDialog.onLoadChanged同时写load/show，并调用PlayerManager.setDanmakuEnabled。长按普通点播/TMDB仅切show。
- DanmakuDialog关闭load后，下一次普通VOD进入被isLoad隐藏；TMDB无已选源也被能力条件隐藏。仅关闭show且load仍true不应导致此问题。按钮配置本身没有被这些开关改写。
- Result.getDanmaku受load过滤；DanmakuApi.canAutoSearch要求load、auto及有效API；PlayerManager.getDanmakus来自现有PlaySpec，setDanmaku/reloadDanmaku使用已有URL、去重与加载策略，setDanmakuEnabled仅管理显示与直播会话，不负责搜索缺失源。
- 普通点播点击onDanmaku打开现有DanmakuDialog，长按onDanmakuToggle切show；TMDB点击经VodPlayerControlController（无源时打开同一Dialog），长按经onPlayerDanmakuLongClick。全屏/浮窗仅显示状态，不另建弹幕配置或引擎。
- DanmakuDialog原开启load仅setEnabled，没有在缺失弹幕源时补已有加载流程。因此本轮同时处理入口与重新开启：保留load/show/auto独立语义；无源且auto允许时复用DanmakuApi；auto关闭或无API时保留原搜索/选文件入口，不擅自打开auto。
- 根因为以上已确认代码条件。尚无本轮电视调用日志；最终实际网络/解码/字幕等验收不由静态结论代替。

## UI-01 / UI-02 / UI-03 / UI-05 审计与方向

- UI-01：addRow返回右侧summary TextView，原setVisibility(GONE)只隐藏值，不是隐藏主页样式整行。使用既有Setting模式，加右侧“经典/沉浸”，选择后原位更新；不改模式路由。
- UI-02：搜索scopePopupWidth先量文字，菜单右侧锚定且独立宽度。现分组FrameLayout overlay采用anchor.width()。只借鉴测量/右对齐，将独立宽度约束为96～144dp及锚宽82%以内，保留同一overlay/滚动和焦点状态机。
- UI-03：DisplayControl背景普通/聚焦都使用player_control_muted，文字普通也用muted、焦点用player_control；selected分支先于focused，且缺disabled/selected+focused组合，能出现同色浅底浅字及焦点被选中覆盖。只在DisplayDialog绑定成对ThemeTokens及完整状态，不统一其他控制面板。
- UI-05：主页handleBack使用HorizontalGridView.setSelectedPosition(0,ViewHolderTask.requestFocus)，首卡时不消费，交回原页面返回。历史页同法，删除模式及上层Dialog优先；不重建历史Adapter，不改记录及续播。

## 施工、验证与联合交付

### UI-01

AppearanceDialog:120、228–241 直接读取Setting.getLeanbackHomeMode，右侧summary显示“经典/沉浸”，沿用同一addRow、字号14sp和主题绑定；确认选择后原位更新，重进读取持久值。仅新增两个Leanback短标签，不改主页模式、WebHome优先级或Release/Mobile隔离。原“经典模式/沉浸模式”选择项不改，未增加小字说明。NativeHomeAppearanceTest与RoutingContractTest已更新对应值预期并通过。

### UI-02

HomePreviewMenuDialog:228–231：只新增4行尺寸/定位计算。保留原FrameLayout悬浮overlay、内部ScrollView和焦点逻辑，按15sp量分组名，宽度受96～144dp及锚宽82%约束，右侧留12dp；极窄窗口优先不超可用锚宽。按钮/面板宽度、站源列表位置不变。分组选择、长按排序、返回各层级、左右关闭、当前源及浏览焦点恢复原矩阵全部通过。截图evidence/native-source-dropdown.png为本地Native Graphics结果，不是真机截图。

### UI-03

DisplayDialog:144–174 是唯一状态样式修改位置；没有修改全局Control、Player OSD、主题配置或其他面板。背景使用ThemeTokens.surfaceContainer，普通surfaceContainerHigh/onSurface、聚焦secondaryContainer/onSecondaryContainer、选中primaryContainer/onPrimaryContainer；选中+聚焦保持选中填充并增加3dp colorFocus描边。禁用分支优先，使用原全局onSurfaceVariant半透明文字及弱描边，避免与普通态混淆。配对状态随全局主题重建，onStart/onResume消费最新token，同一快照不重复分配。局部根标记沿用ThemeBinder的webhtv:ignore，使通用推断不拆散成对状态，仍由唯一ThemeController提供颜色。

实际Dialog在浅/深/自定义主题下测试普通、聚焦、选中、选中+聚焦的文字/背景对比度均≥4.5；聚焦时真实GradientDrawable ConstantState描边色等于全局focus；禁用态单独验证。确认点击仍写原PlayerSetting并执行原callback。首轮测试使用Robolectric描边影子字段，克隆后读到0；改读实际GradientDrawable状态后通过，生产逻辑未因该影子观测问题改动。

### UI-04

修改范围为原有弹幕入口与DanmakuDialog，不改PlayerManager、PlaySpec、Result、Media3或弹幕请求实现：

- 两种VideoActivity的可见性使用PlayerButtonSetting.isVisible(DANMAKU)，其余按钮、原排序/模式限制仍由既有配置管理；不强制显示所有按钮。
- VodPlayerControlController.hasDanmakuControl保留hasPlayer真实会话限制，去除load/是否已有源作为能力门槛；TMDB内联、全屏与Fusion浮窗继续复用同一控制器和PlayerButtonSetting过滤。
- 关闭load后普通VOD/TMDB的长按会进入既有DanmakuDialog以重新开启load；load仍开启时原长按show切换语义保持。短按无源或load关闭也打开原Dialog，不另建开关。
- DanmakuDialog.onLoadChanged仍写原load/show，并更新PlayerManager显示；从关闭恢复开启时先使用会话原有已选源，否则复用已有首个可用源。已有源走原setDanmaku去重/加载，不强制重解析或重复建播放器。
- 缺源时，仅当现有DanmakuApi.canSearch（load && auto &&有效API）成立才用原MediaTitleRequest/DanmakuApi补源；结果检查当前播放key、释放状态、load/show，以及是否已被用户手动选择源，旧结果不能覆盖新会话或手动选择。
- 没源且auto关闭/无有效API，保持原手动搜索及选文件入口。不能凭空产生源，也不会把auto强制改成true。load关闭期间Result.getDanmaku原过滤保留；若本会话没保存源，需要上述既有搜源/选文件流程，而非重新解析视频或强开自动搜索。
- 设置页原getLoad/getShow/getAuto读取一致，不新增配置。播放器按钮隐藏配置从未被弹幕load/show写入；隐藏按钮不改变弹幕功能状态。

UIOptimizationSharedTest在Leanback和Mobile实际打开DanmakuDialog，覆盖关闭→销毁弹窗→重开→开启的原设置持久值、已有源setDanmaku调用、无源auto门槛、无API、用户隐藏按钮、不同会话及手动选源的过期回调拒绝。PlayerManager和API使用Robolectric shadow，验证的是实际UI/控制器到既有加载器的分派及状态，不是网络、弹幕解析或解码渲染实测。自动搜索及真实弹幕服务可用性NOT_TESTED；未宣称电视显示已通过。

### UI-05

NativeHistoryBrowseView:130–145复用主页同一个Grid API：setSelectedPosition(0,ViewHolderTask)，在有效代次、未释放且holder位置仍0时requestFocus。只在真实rail.hasFocus且当前非首卡、非删除模式时消费Back；DOWN/repeat/UP成对处理，取消UP不跳转。HistoryActivity:243系统返回委托同一handleBack，原删除分支始终在前；首卡时不消费，沿用原HistoryActivity退出。

弹窗本来拥有独立窗口，先处理其Back；未新增全局返回拦截。覆盖管理/删除确认弹窗只关闭自身、删除模式先取消、空/单卡不拦截。原history读取/排序/删除/续播、卡片视觉和主页Back代码完全未改。

测试第二张、第十张和80张列表末尾返回后首卡实际hasFocus、完全进入可视范围；Adapter同一实例、数量不变、能继续上下导航。另测试返回过程列表被更新/删除时不会访问失效holder。实际HistoryActivity系统返回集成测试验证非首卡→首卡、删除优先、首卡再按Back才走原退出。

### 验证汇总

- Leanback：218 tests / 38 suites，0 failure/error/skip。
- Mobile：16 tests / 6 suites，0 failure/error/skip。共享屏显/弹幕、原弹幕入口空值、按钮配置、API来源契约及既有播放器崩溃/浮窗隔离回归。
- 只做一轮最终联合构建：assembleLeanbackArm64_v8aDebug + assembleLeanbackArm64_v8aRelease，fastRelease=false，2m43s / 346 tasks。R8、资源收缩和优化、Release lint均执行。
- 两包com.silent.android.webhtv，versionCode=560、versionName=5.6.0，仅arm64-v8a / 30个AArch64 ELF。Launcher仍HomeActivityCurrent → HomeActivity，旧历史alias保持禁用。Release debuggable=false，Debug=true。
- 证书SHA-256：97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5，与已授权个人Debug签名及之前候选一致。
- 未执行电视安装与真实播放验收。源码/测试/构建通过不等于电视通过。

### 修改文件清单

生产（11）：

1. leanback/ui/dialog/AppearanceDialog.java（UI-01）。
2. leanback/res/values/native_home_mode.xml（UI-01短值）。
3. leanback/ui/homepreview/HomePreviewMenuDialog.java（UI-02尺寸）。
4. main/ui/dialog/DisplayDialog.java（UI-03局部主题）。
5. main/ui/dialog/DanmakuDialog.java（UI-04恢复已有加载流程）。
6. main/ui/controller/VodPlayerControlController.java（UI-04真实会话能力/入口）。
7. leanback/ui/activity/VideoActivity.java（UI-04可见性/长按）。
8. mobile/ui/activity/VideoActivity.java（UI-04同源门槛兼容性）。
9. main/ui/activity/TmdbDetailActivity.java（UI-04长按入口，仅加一行，无播放流程改动）。
10. leanback/ui/homepreview/NativeHistoryBrowseView.java（UI-05）。
11. leanback/ui/activity/HistoryActivity.java（UI-05系统返回最小接入）。

以上Java路径均位于app/src/<flavor>/java/com/fongmi/android/tv/下。测试更新5份：NativeHomeAppearanceTest、NativeHomeRoutingContractTest、NativeSourceDrawerTest、NativeHistoryBrowseTest、NativeHistoryModeTest；新增Leanback/Mobile各一份UIOptimizationSharedTest。本报告为第三个新增文件。共16个既有文件最小修改、3个新增文件。

### 保护结果与待真机事项

5291个原文件逐路径SHA核验，仅上述授权路径改变；其他原文件完全保持。C56原有脏文件中仅接纳Leanback VideoActivity、TmdbDetailActivity及原换源类/测试做本轮增量，已有原件和原差异均保存在仓库外，task-only.patch与HEAD差异分离。原PlayerManager、PlaybackActivity（TMDB-INLINE-CRASH-01）、FusionDetailController/浮窗核心、Mihomo/实验室/第二字幕未改。

20个原归档APK SHA不变。WebHome/WOGG HEAD及完整状态不变，Suite gitlink不变，App HEAD仍d48e68823ae283fd9c7286ea221b0ef75a620eed。原72条dirty，最终87条；index保持未暂存。独立task guard保护68条未接纳的原dirty路径。

NOT_TESTED：TCL电视五项视觉/遥控器体验、真实弹幕源/网络请求与字幕渲染、真实应用进程重启、TMDB内联/全屏/浮窗中的实际弹幕连续性。auto关闭且无源需要现有手动入口；不能将“load已开启”视为一定有可播放弹幕。管理菜单/确认弹窗及删除模式按原窗口/分支处理，等待真机复核。


### DEBUG APK

`E:\GitHub\webhtv-suite\app\Release\apk\UI-OPT-01-20261028\leanback-debug-arm64-ui-opt-01-20261028.apk`

大小：209,263,110 bytes / 199.57 MiB

SHA-256：`a899f6e8604a3b4e248ee819ee8a0a93bda54efa5578e009ce2c7dc9f912614f`

### RELEASE APK

`E:\GitHub\webhtv-suite\app\Release\apk\UI-OPT-01-20261028\leanback-release-arm64-ui-opt-01-20261028.apk`

大小：174,127,034 bytes / 166.06 MiB

SHA-256：`88f334df1d995d73952953aed1bbdd2bd1575eb9783c3cd4dc0aa5aacb8641ce`

## 交付状态（PASS指代码/自动化，不代替真机验收）

```ini
TASK = UI-OPT-01
UI01_HOME_MODE_VALUE = PASS
UI02_SOURCE_DROPDOWN = PASS
UI03_OSD_FOCUS_COLORS = PASS
UI04_BUTTON_VISIBILITY_AUDITED = PASS
UI04_ROOT_CAUSE_CONFIRMED = YES
UI04_BUTTON_VISIBILITY_FIXED = PASS
UI04_PLAYER_REENABLE = PASS (UI-to-existing-loader dispatch; real rendering NOT_TESTED)
UI04_SETTINGS_CONSISTENCY = PASS
UI05_HISTORY_BACK_TO_FIRST = PASS
LEANBACK_TESTS = 218 PASS
MOBILE_TESTS = 16 PASS
DEBUG_BUILD = PASS
RELEASE_BUILD = PASS
RELEASE_SHA256 = 88f334df1d995d73952953aed1bbdd2bd1575eb9783c3cd4dc0aa5aacb8641ce
PREVIOUS_CHANGES_PRESERVED = PASS
TV_ACCEPTANCE = NOT_TESTED
COMMIT = NO
PUSH = NO
SUITE_GITLINK_UPDATED = NO
```

完成五项联合交付后停止，等待电视验收。证据位于独立交付目录evidence，包含原始测试日志、最终XML、保护结果、源码增量及Release mapping；未启动其他任务。
