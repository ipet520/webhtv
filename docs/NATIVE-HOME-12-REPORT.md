# NATIVE-HOME-12｜艺术标题加载时序审计与定点优化

## 结论

根因 **A+D 已确认**：窗口队列原先仅准备背景图，虽然元数据可能已有 Logo URL，Logo 图片并未预加载/解码；主页绑定及图片 reset 又立即显示普通文字，资源返回后才隐藏文字。没有发现 Logo 专用额外渐入动画。

完成定点修复、185 项受影响 Leanback 测试、真实 Glide 本地 HTTP/解码缓存验证及 ARM64 Debug/优化 Release。**TCL 物理显示、真实 TMDB 网络及手感 NOT TESTED**；不以 onResourceReady 或模拟绘制宣称电视通过。

## 1. 实际调用链与源码依据

`NativeHomeBrowseView.bindHero → NativeHomeModeHost 的 heroFocus → NativeHomeHero.focus/loadPrepared → ExistingTmdb.load → TmdbService/TmdbMatcher/TmdbMatchCache → Info.logo → NativeHomeBrowseImages.enrich → Glide → nhp_logo`。

- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeHero.java:54-81`：已有焦点稳定 180ms、在途共享、scope/generation 交付。`:86-105`：离主线程手动绑定校验与短期元数据 memo；`:121-132`：窗口订阅；`:150-204`：手动优先、站源内嵌身份、原 Matcher/Service、语言作用域及 Logo URL 选择。规则未变。
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomePreparationQueue.java:80-150`：原唯一调度现增加 opt-in Logo 图片准备。修改前图片接口只接 Info.backdrop，没有 Logo 请求入口。
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java:486-506`：修改前真实 Vod 的 card.logo()==0 分支直接显示文字；现在直接消费当前窗口确认的 Logo。`:651` 仅真实主页启用新标题管理。
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeBrowseImages.java:89-111`：相同 Logo Glide request；`:134-191`：标题状态/代次/时序；`:216-229`：opt-in 主页面及历史旧默认分支。原 reset 无条件 title.VISIBLE 是另一处文字提前显示断点。
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomeHeroLogoView.java:15-21`：super.onDraw 后检查当前 Drawable、绑定、可见性、window、alpha、裁剪，记录一次 T4。只观察绘制，不建立显示权威。

情况区分：A/D 有源码和回归证据；B 的 TCL 解码延迟 NOT_OBSERVED，旧版本就没有 Logo 图片预加载；C 中缓存不能阻止原文字先显分支，但 TCL 的 T3→T4 时长 NOT_OBSERVED，没有凭空增加网络解决假定延迟。

## 2. 修复及预算

复用现有单一队列，NativeHomeModeHost 显式开启 Logo 准备，历史默认关闭。原窗口上限 10、后台元数据间隔 750ms、一个后台图片名额保持。Logo 和背景共享名额，按原候选优先级，平级背景优先；不取消当前 Hero 关键背景请求。没有新增 TMDB 客户端/数据库/独立调度器。

预加载/正式显示共用 URL、实际测量尺寸（上限1280×720）、Drawable、fitCenter、AUTOMATIC diskCacheStrategy、dontAnimate；优先级 LOW/NORMAL 不参与资源缓存键。仍由 Glide 管理资源，不另存 Drawable。已有最多24条/120秒就绪记录只扩展 Logo 标记，当前窗口确认元数据后再消费，不能绕过手动绑定/语言/凭据作用域。

| Logo 状态 | 显示 |
|---|---|
| 已缓存且 URL 已确认 | 首次绑定直接使用 Glide 资源，不先显示文字 |
| 未知/准备中 | 稳定空标题槽，不改大小位置 |
| 确认不存在或失败 | 立即显示原文字 |
| 超时 | 显示文字，本次展示锁定回退 |
| 回退后迟到 | Glide 保留缓存，本次不再替换文字；下次焦点可复用 |

等待边界 **930ms = 原焦点稳定180ms + 一个窗口调度间隔750ms**。不是给就绪 Logo 加延迟，也不阻塞焦点/背景；只防止未知状态无界留白。弱网可触发回退，边界感受待 TCL。

请求校验焦点 generation、标题生命周期 epoch、当前 URL。暂停取消展示订阅，恢复沿用原 Hero 刷新；重复同 URL 不再重复加载；失焦保留有价值的预加载，释放时取消。分类和源切换仍使用原代次。普通标题 Marquee 类、字体字号、Logo 几何、分类焦点、轨道、背景宿主和 **160ms背景过渡未改**。XML只替换同属性 ImageView 为绘制观察子类。

## 3. 时序证据

修改前没有采集 TCL 日志；不能倒推时间。保留旧默认分支的测试确认原先文字先出现，资源成功后消失。

| 指标 | 修改前 | 修改后：受控 Robolectric 展示测试 |
|---|---|---|
| T0 可见窗口 | 原 queued，仅背景；时间 NOT_OBSERVED | 原 window-queued；本展示测试时间 NOT_OBSERVED，队列测试独立验证入窗准备 |
| TMDB身份/Logo URL | 既有 TMDB_MATCHED/metadata-ready；真实时间 NOT_OBSERVED | 原匹配规则；window-logo-url-known；真实时间 NOT_OBSERVED |
| T1 Hero焦点 | 立即文字，时间 NOT_OBSERVED | uptime145ms |
| T2 Logo请求 | enrich后，时间 NOT_OBSERVED | 145ms |
| T3 资源就绪 | 回调，时间 NOT_OBSERVED | 195ms（测试人为延后50ms） |
| T4 绘制确认 | 无专用观测，NOT_OBSERVED | 210ms，实际 Logo View.draw 后记录一次 |
| 普通文字 | 是（旧分支测试确认） | 此展示测试全程没有出现 |

上述时间为 Robolectric uptime，包含模拟 View 生命周期推进，**不能当作 TCL 毫秒性能**。T3 之前、T3之后未绘制时均没有T4；二次draw不重复T4。T4只证明Android View执行绘制，不证明GPU合成或电视物理呈现。

真实Glide对照：MockWebServer提供PNG，HTTP/Glide Engine/预加载/目标/缓存真实执行；Windows Robolectric不支持Android AVIF JNI探测，测试以ByteBuffer→BitmapFactory解码器替代探测，生产解码器不变。**1次预加载+3次正式显示，HTTP=1，解码=1，3次T3均MEMORY_CACHE**。首轮T1/T2/T3=145ms、T4=160ms，后两轮同测试uptime160ms；未先出现文字。原始事件见本交付 evidence/logo-timing-evidence.txt。

真实TMDB身份和Logo URL时刻仍 NOT_OBSERVED。调试可使用既有NativeHomeHero日志（window-queued、metadata-ready、logo-url-known、title-T1/T2/T3/T4、text、late-cached）及NativeHome10身份日志。仅记录图片hash，不输出凭据；T4不是电视拍摄验收。

## 4. 请求与性能边界

增加的是候选Logo图片准备，不增加Logo专用TMDB请求。队列测试2个候选仍2次metadata调用；一个图片后台名额，失败30秒冷却。Logo逻辑Glide请求增加，缓存复用测试没有重复HTTP/解码；不能承诺真实缓存淘汰后网络量永远不增加。没有测得TCL主线程阻塞/掉帧/改善幅度，不作流畅度结论。

## 5. 测试

- 首轮测试因包内同名Color类解析冲突编译失败；测试改整数颜色后通过，生产颜色未改。
- 最终 `:app:testLeanbackArm64_v8aDebugUnitTest --tests '*NativeHome*Test' --tests '*NativeHistory*Test' --tests '*HomeHero*Test' --tests '*NativeSourceDrawerTest'`：**185/185 PASS，29套，0失败/错误/跳过**。
- 新Logo时序5项、队列14项；覆盖准备中不闪字、失败/无Logo/超时/迟到、resource≠draw、重复回调、ABC/ABA旧结果、暂停恢复、历史旧路径、真实Glide缓存键、Logo-only影片及共享图片预算。
- 原HOME-09～11准备/背景/主题/焦点/Release视觉合约、Marquee、历史、站源相关套件通过；不声称全仓测试通过。
- Mobile NOT RUN：没有修改main/mobile生产代码；共享Leanback辅助类以默认关闭分支保护历史，历史回归通过。
- scoped git diff --check、task guard：PASS。JUnit和完整构建/测试日志在evidence目录。

## 6. 完整构建与产物

`assembleLeanbackArm64_v8aDebug assembleLeanbackArm64_v8aRelease -PfastRelease=false`：**BUILD SUCCESSFUL，2m59s**。Release实际执行R8，mapping已归档，没有重命名Debug冒充Release。

两包：com.silent.android.webhtv，versionName5.6.0/versionCode560；只有arm64-v8a，30个.so、ELF machine183。Launcher启用HomeActivityCurrent→HomeActivity，History alias禁用。apksigner verify PASS；证书SHA-256 `97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`，沿用已授权现有Debug keystore。Debug debuggable=true，Release **false**。

### Debug

- 路径：`E:\GitHub\webhtv-suite\app\Release\apk\NATIVE-HOME-12-20261029\leanback-debug-arm64-native-home-12-20261029.apk`
- 大小：209263110 bytes
- SHA-256：`a628df1fde6869eb892f63fbbe630a6a0705035d51e511f589efbd6951aa2668`

### Release

- 路径：`E:\GitHub\webhtv-suite\app\Release\apk\NATIVE-HOME-12-20261029\leanback-release-arm64-native-home-12-20261029.apk`
- 大小：174127046 bytes
- SHA-256：`1cef3cfe327b1e84121654350f08a388751b53c28f86e745105f7296b7b86abc`

## 7. 修改文件（7既有+3新增）

- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeHero.java`
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomePreparationQueue.java`
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeBrowseImages.java`
- `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeModeHost.java`
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java`
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomeHeroLogoView.java`
- `app/src/leanback/res/layout/view_native_home_preview.xml`
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomePreparationQueueTest.java`
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeLogoTimingTest.java`
- `docs/NATIVE-HOME-12-REPORT.md`

Hero仅提取180ms常量；Host连接原队列；View消费标题就绪信息；Images负责原图片订阅中的标题决策；Queue扩展同一图片名额；LogoView只记录绘制；XML属性不变；测试和报告补证据。

## 8. 保护结果

- 开始/结束HEAD `d48e68823ae283fd9c7286ea221b0ef75a620eed`，main。未commit/push/tag，index空。
- 施工前91条dirty，逐文件快照5297项；仅7个授权既有文件变化，缺失0、范围外变化0。新增3项均在scope内。
- 89个未采用dirty路径guard保护通过，2个已dirty授权文件基于现有内容补丁；其他既有成果未覆盖。
- 原26个归档APK哈希全部不变。独立后缀新产物及本轮目录保留。
- Suite gitlink（App74af2c5137a9380cd454c78098668806b1b27e6a）、WebHome/WOGG HEAD和逐路径完整Git状态与开始相同；未触及其他项目。
- 原始逐文件哈希/原dirty diff/授权原文件留在 `C:\Users\ADMINI~1\AppData\Local\Temp\native-home-12-20261029`。evidence/protection-final.json含最后核验。

## 9. 真机待验与停止点

NOT TESTED：TCL冷启动、缓存Logo首帧、无Logo文字、弱网超时/迟到不翻转、ABC/ABA/换类换源、前后台、Marquee、主题往返、背景160ms观感、焦点响应和内存压力。需要Debug时序结合实际拍摄验证，不能用Robolectric替代。

完成NATIVE-HOME-12后停止，等待TCL验收；不启动其他任务。
