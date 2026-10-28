# NATIVE-HOME-11 三项收尾与个人 Release 候选

## A. 施工结果

### 01 顶部功能按钮

实际黑底来自 HomePreviewStyles.function 原有 colorPlayerScrim 的 72% 填充。改为现有全局 ThemeTokens.colorOverlayLight 的浅色半透明圆角背景，聚焦提高同一 Token 的透明度；文字、弱化文字、阴影、下划线分别继续消费 colorPlayerControl、colorPlayerControlMuted、colorShadow、colorFocus。没有新增专用主题或固定蓝/黑配色。保留原尺寸、位置、顺序、主题色短下划线及 1.08 倍/120ms 聚焦动画。沉浸历史页删除、观影报告复用同一 function 样式，经典主页不消费该样式。

浅色与深色全局主题、不同主题色、明暗模拟影视背景均通过 Drawable/像素和本地渲染检查；本地状态对照图已检查，不能代替电视最终视觉验收。使用适量主题阴影保持文字可辨，没有整排底板。

### 02 跨分类预热

实际执行链：NativeHomeBrowseView.renderBusiness → schedulePreparationWindow/publishPreparationWindow → NativeHomeModeHost 的 setPreparationWindowListener → NativeHomePreparationQueue.window/kick → NativeHomeHero.prepareWindow → NativeHomeBrowseImages.prefetch/GlideLoader.prefetch。Grid 的滚动、子项 attach/detach、布局变化都会重新调度窗口；发布任务合并，窗口上下文与候选去重。不是只在首次进入时启动。

源码位置：NativeHomeBrowseView.java:174、179、185、648、749、756；NativeHomeModeHost.java:54、92；NativeHomeHero.java:119；NativeHomeBrowseImages.java:84、192；NativeHomePreparationQueue.java:33、71。

已确认的窄缺陷：A 图片尚在解码时切 B 再返回 A，当前 A 的元数据确认相同 URL 后，旧 A 图片完成记录没有被新 A 订阅及时消费，可能再预取一次并错误判为未准备。新增 lateSameImageCompletionIsReusedAfterABAReturnWithoutDuplicatePrefetch 在修改前明确失败，日志 late-image-baseline.log；修复后通过。

最小修复：原队列 kick 在启动图片前，复用原有 ready 表中同身份、当前已确认相同 URL、未过期的完成记录；异步图片回调捕获实际请求 URL，仅对仍处于 IMAGE 且 URL 相同的 Entry 写 READY/FAILED。旧 URL 成功或失败均不得覆盖替换后的新 URL 状态。没有增加缓存系统、候选数、并发或任务引擎。

保留 MAX_WINDOW=10、1 元数据＋1 图片、750ms 元数据间隔、原 24 条/120 秒准备记录、现有 TMDB 身份/凭据作用域与 Glide。旧 metadata generation 不符时丢弃；旧图片完成仅记录自身身份/URL并唤醒当前队列，不直接更新当前视觉。旧任务占用期间不额外提高并发，完成后当前候选继续调度。

受控测试覆盖首次/第二/第三分类、A→B→C、A→B→A、A→B→C→A、缓存返回、旧任务在途、延迟布局、已知/未知身份、无横图、窗口移动及旧 URL 回调。保留原真实 Glide 本地 HTTP 测试，网络请求/解码次数及共享 key 检查通过。window-focus prepared=true 只表示近期解码完成记录，不证明电视上仍驻留内存；真实 DataSource、最终绘制时刻和电视命中率 NOT TESTED，无改善百分比声明。

### 03 分类四状态

仅调整 HomePreviewStyles 的一级分类 StateListDrawable 和现有 ChoiceAdapter 焦点视觉：普通为主题 surface 30% 填充；选择为主题 primary 28% 柔和填充；聚焦独立叠加主题 overlay 前景明亮 2dp 描边及细暗对比边、1.05 倍/120ms 缩放；选择＋聚焦同时保留两者。明亮描边来自全局 colorPlayerControl，暗对比边来自 colorShadow，没有独立配色。

选择态由既有业务 selected 控制，焦点由 Android focused 控制；失焦只取消放大和聚焦描边，不清除选择态。缩放不改变测量尺寸，分类列表已有 clipChildren/clipToPadding=false 与间距继续保留。没有修改 100ms 分类防抖、筛选、海报滚动、业务状态机或重新创建轨道。

### 用户追加授权的 Release 门控

Setting.isLeanbackHomeStyleAvailable/resolveLeanbackHomeMode 允许 Leanback Release 使用现有 CLASSIC/IMMERSIVE 存储与选择入口，默认 CLASSIC，Mobile 始终 CLASSIC。AppearanceDialog 原 GONE 隐藏的是 addRow 返回的摘要 TextView，菜单行本来可见，无需改动。WebHome 优先级未改。未增加桌面图标或 Debug 测试入口。

用户明确授权个人使用沿用现有 Debug keystore：产物是真正优化 Release，不将 Debug 签名冒称正式发布证书，不生成新密钥。

## B. 修改清单

以下路径均相对 E:/GitHub/webhtv-suite/app：

| 文件 | 原因 |
|---|---|
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewStyles.java | 顶部共享轻量全局主题样式、分类四状态 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java | 一级分类现有焦点动画及回收时复位 |
| app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomePreparationQueue.java | 复用晚到的相同图片完成记录，隔离替换 URL 回调 |
| app/src/main/java/com/fongmi/android/tv/setting/Setting.java | 已授权的 Leanback Release 双主页门控 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeReleaseVisualTest.java（新增） | 四状态/明暗主题像素及渲染、跨分类真实 View 窗口发布测试 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHistoryUiFixTest.java | 共享按钮轻量主题、可读性辅助与既有删除 UI 回归 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeVisualRepairTest.java | 更新轻量共享样式断言 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomePreparationQueueTest.java | ABC/ABA、未知及无图、晚到图片与 URL 替换成功/失败回调 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeTest.java | Release 模式、默认经典、Mobile 隔离及测试 Application |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeIntegrationTest.java | 分类柔和填充、可撤销 100ms 防抖及测试时钟隔离 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/activity/NativeHistoryModeTest.java | Release 跟随双主页模式预期 |
| docs/NATIVE-HOME-11-RELEASE.md（新增） | 本报告 |

NativeHomeModeHost、NativeHomePreparationTest、Hero、Images、CustomWallView、TMDB/播放器、浮窗、字幕、Mobile 源码均保持原内容；不处理长标题滚动和个别缺横图保留问题。

## C. 测试与边界

最终回归 BUILD SUCCESSFUL：Leanback 206、Mobile 48；失败/错误/跳过均 0。覆盖 NativeHome*、NativeHistory*、FusionFloating*、详情响应式/ChipFill/播放壳、ThemeResolver/Overrides/Presets，Mobile 含浮窗、PiP 退出决策和详情模式。

测试发现与归因：原 Mode/Integration 的 Application fixture 无 App 单例，调用 Func 时失败，原方法在施工前未改；测试改用项目已有 FixtureApp。Robolectric 焦点动画使 idle 99ms 实际推进到 135ms，导致旧 100ms 断言失真；仅在该测试暂停 Choreographer 并 finally 恢复，99/100ms 断言保留，生产调度没有改。新晚到 ABA 用例修改前失败、生产修复后通过，不归为旧失败。

本地验证 PASS：四种分类状态及失焦保留选择；顶部主题状态、尺寸/位置；分类切换不重建 Grid；模式默认、持久化解析及 Mobile 隔离；历史删除/详情/报告入口；WebHome 路由相关回归；页内浮窗控制/吸附/恢复状态相关测试。自动化不等同设备实际播放。

电视 NOT TESTED：浅/深/自定义主题实际观感与浅背景可读性；顶部/历史按钮、分类四状态长期按键；真实站源跨分类预热 DataSource/绘制/内存命中率；安全区、背景 160ms 过渡和海报流畅度；历史续播与报告真机操作；TMDB 各详情模式、Surface 连续性、真实选集自动连播、音轨字幕、物理空鼠及浮窗焦点。无 ADB 安装或覆盖安装验证。

## D. Release APK

完整构建命令：

```powershell
./gradlew.bat :app:assembleLeanbackArm64_v8aRelease -PfastRelease=false -PchaquopyBuildPython=E:/DevTools/python-3.10/python.exe --offline --no-daemon --console=plain
```

BUILD SUCCESSFUL，2m25s。保留现有 Release R8、资源压缩/优化、lintVital 与签名检查；没有修改 build.gradle、ProGuard 或关闭安全门控。不是 Debug APK 重命名。

- APK：`E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\release\leanback-arm64_v8a.apk`
- 大小：174,125,530 bytes（约 166.06 MiB）。
- SHA-256：`8dcc4ae6dbaaf9b8be7d82adec20ff9303166c7f82ca4f59e0ca86f3d405236c`。
- variant：`leanbackArm64_v8aRelease`；applicationId：`com.silent.android.webhtv`。
- versionCode：560；versionName：5.6.0；`debuggable=false`（Manifest 与 apkanalyzer 双重核查）。
- apksigner verify 成功；签名证书为既有 Android Debug，SHA-256：`97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`。这是用户批准的个人使用签名，不是正式发布证书。
- 与前次交付 Debug APK 的证书、包名一致，versionCode 同为560，满足同版本 replace 的版本关系；没有连接设备检查实际已安装版本或执行覆盖安装。如设备安装了更高版本或不同证书，仍须另行核验，不能据此宣称已安装成功。
- ABI 仅 `arm64-v8a`；30 个 native ELF 全部验证 Class64、Machine183/AArch64。
- 唯一启用 Launcher：`com.fongmi.android.tv.ui.activity.HomeActivityCurrent` → `com.fongmi.android.tv.ui.activity.HomeActivity`。History alias 未启用；没有新增桌面图标，Manifest 没有 NativeHomePreviewActivity。
- 直接读取产物 Setting DEX：Leanback 的主页样式可用，resolve 方法按 flavor/stored 判定，Release 不再被 DEBUG 强制经典；Mobile 分支仍 CLASSIC。默认 CLASSIC 的源码与测试保持。
- DEX 确认包含 NativeHomeBrowseView、NativeHistoryBrowseView、NativeHomePreparationQueue、TmdbDetailActivity、FusionFloatingPanel；证明功能代码纳入，不宣称 R8 后真实 Surface/播放效果已验收。
- 原有 BuildConfig.DEBUG 调试日志门控保留；只开放已授权的原有双主页入口，没有加入预览/测试按钮。

证据：外部目录 final-release-build.log、release-verification.json、release-signature.txt、release-manifest.xml、release-debuggable.txt、release-home-gate.txt、release-home-mode.txt。此前候选包 SHA 已失效，以本节为准。


## E. 工作区保护

OLD_HEAD = NEW_HEAD = 030485b70c263621fe4575abe5758b28115832c3。

施工前 312 条 dirty，5262 个现有 App 文件逐路径 SHA-256；施工后 314 条 dirty。原文件中 10 个为本轮必要修改、5252 个完全不变，0 缺失；新增测试与报告共 2 个。各必要脏文件基于当前内容最小补丁并保留原换行，未用 HEAD 覆盖。其余原有成果未覆盖。

外部保护证据位于 TEMP/native-home-11-current.txt 指向目录，含 before.json、originals、protection-after.json、scope.txt、日志、主题渲染图及产物校验结果。独立 task_guard scope 记录保护原有范围外 300 条 dirty 路径；最终 check PASS（branch/HEAD、protected dirty paths、scope、staged paths）。不运行 finish（其会提交）。

WebHome 114 文件、WOGG 106 文件逐路径内容完全不变，二者 HEAD/status 不变。Suite gitlink 未变化：App 74af2c5137a9380cd454c78098668806b1b27e6a，WebHome a62b29aca6139b2fe47203cc2d4cb1f95657e8a8。

没有 commit/push/tag、没有更新 Suite、没有创建分支/worktree、没有 reset/clean/stash/破坏性 checkout。完成后停止，等待电视真机验收。
