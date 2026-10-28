# NATIVE-HOME-10 — 四项定点修复

## 范围与保护

用户授权：首卡返回、安全区审计、原生 Hero 跑马灯、两部影片横图链路；不修改历史、独立 TMDB 详情页、经典业务、Mobile、WebHome、WOGG、播放器、预加载策略。禁止提交、推送、Suite gitlink 更新。

开始时间：2026-10-27 13:45:56 +08:00。施工前 App HEAD `030485b70c263621fe4575abe5758b28115832c3`，287 条 dirty 路径、5237 个文件。逐文件基线及 scoped 原件位于仓库外 `%TEMP%/native-home-10-20261027-134942`；task guard 保护其余 282 条既有 dirty 路径。终检待完成。

## 方案与源码依据

沿用已有 Android TextView marquee / Leanback 焦点 / CustomWallView。局部既定设计修复不引入新架构，不改布局尺寸。证据：`NativeHomeBrowseView.handleBack/dispatchKeyEvent/bindHero/showHero`、`HomeHeroTitleView`、`NativeHomeHero.ExistingTmdb`、`NativeHomeBrowseImages`、共享 `TmdbImageSelector`、`TmdbService`。不改动共享 TMDB 服务与缓存。

选择最小适配：首卡事件不消费；同一影片相同标题不重置，影片身份改变才重置原生 marquee；相同元数据不重复 setText。保留原状会吞掉首卡返回；替换跑马灯或滚动引擎没有真机帧证据，不采用。安全区若测试正确则零生产修改。回退仅恢复本轮 scoped 补丁，不覆盖原有内容。

## 背景现场证据（只读获取）

在线 `192.168.31.205:39573` 为 OnePlus 8 Pro **手机**，不是电视。仅通过 run-as 读取缓存，没有安装、导航、修改配置或清空缓存。原始归档保存在仓库外，不提交私有数据。

| 站源 / 影片 | 真实匹配缓存 | 详情 | 横图候选 |
| --- | --- | --- | --- |
| 玩偶 / 谜林 | `/voddetail/133262.html`，tv/336458，zh-CN，manual=false，backdropUrl 空 | first_air_date 2026-10-27，backdrop_path=null | images.backdrops=[]；有 Logo、Poster |
| 玩偶 / 谎日沉影 | `/voddetail/132282.html`，tv/336036，zh-CN，manual=false，backdropUrl 空 | first_air_date 2026-10-06，backdrop_path=null | images.backdrops=[]；有 Poster |

详情文件修改时间分别为当日 12:42:12 / 12:48:04 和 12:45:36，匹配缓存修改时间 13:25:44。`TmdbService` 详情 TTL 7 天，搜索 1 天；images 请求使用配置语言及 null 语言过滤。

进一步使用现有已配置凭据，只读访问 TMDB 当前 `/3/tv/336458/images`、`/3/tv/336036/images`：分别按 `include_image_language=zh,null` 和不限制语言查询，共 4 次，均 HTTP 200、backdrops=0。凭据仅留在进程内，日志和报告不保存鉴权信息。公开图片页面不可访问，但上述直接 API 请求成功。因此当前这两个正确 TMDB 身份确实没有横图，不是本地语言过滤遗漏。不会刷新或删除用户缓存，也不增加 App 请求策略。

正常元数据对照：同一手机缓存 tv/223911《仙逆》，`backdrop_path=/crn53sSGWRZ8wAtEGso52nepEkz.jpg`，images.backdrops 有 45 项；横图正常候选与两部问题片空候选可以区分。这是缓存元数据对照，不冒充该片已在电视上绘制成功。

共享 Vod 模型不声明 wallPic；不能将“模型没有该字段”写成“原始站源 wallPic 为空”。原始站源响应/vodPic/vodContent 未取得。Hero 初始文字来自 Vod；后续可由 TMDB 信息增强，有文字本身不能证明匹配成功。

当前这份缓存下选择器没有可用横图，因而不会发起横图 Glide 请求或进入 CustomWallView；应保持竖版兜底。不能借用搜索中另一个 movie/919609《奇巧计程车：扑朔谜林》的横图。电视是否使用相同缓存、原始站源是否另带未映射横图、正常对照影片的实际电视绘制：NOT TESTED。两部影片本轮没有“补图修复”，没有伪造 Glide HTTP 成功或最终显示结果。

## 当前实施与验证状态

- 返回：ROOT CAUSE CONFIRMED / FIXED / VERIFIED（自动化）。`handleBack` 仅在海报 selectedPosition>0 时归位并消费；`dispatchKeyEvent` 用已有 DOWN/UP 闩锁配对该次事件，首卡 DOWN/UP 交还上层，取消事件不归位。`HomeActivity.onBackPressed` 原分支仍为 host 未处理时调用 `exitHome()`；目录原有 backFolder 语义保留。没有新退出弹窗、计数或延迟。
- 安全区：VERIFIED（源码+自动化），电视 NOT TESTED。只消费当前设备真实 system/cutout inset 到内容；遮罩 match-parent，原生背景在独立宿主。根节点不加 inset padding；零 inset 会覆盖旧值。1280×720、1920×1080、3840×2160 遮罩边界全屏；左/右/系统栏/归零验证通过。2.5% 设计 gutter 与 cutout 不同，不把它算成挖孔留白。不修改安全区生产代码。手机实际物理屏幕之外的 Window 系统裁剪不等同于内容 View 自行加边，真机 Window 仍待复核。
- 标题：重复绑定路径 FIXED / VERIFIED（自动化）；真实电视帧卡顿 NOT REPRODUCED / NOT TESTED。`showHero` 相同元信息不重复 setText；`HomeHeroTitleView.bindTitle` 以影片身份+内容判断重置，普通相同文本 setter 幂等。同一影片 20 次重复绑定不触发新文本事件，换身份即使同名也重置；10 次相同 Hero 元数据刷新不触发新文本事件。保留原生 marquee，无新增滚动引擎；原生单行、selected、可见性与生命周期规则继续通过。该结果不是帧间隔、GPU 或电视流畅度测量，不能认定已解决持续帧卡顿。
- 背景：ROOT CAUSE CONFIRMED（当前 TMDB 无横图、手机缓存无候选）。原始站源及电视绘制未完整复现，不伪报显示修复。新增诊断只观测，不改变匹配、7 天缓存、预加载并发、160ms 过渡或绘制权威。

## Debug 定点诊断

使用实际 applicationId `com.silent.android.webhtv`；先安装本轮 APK 后，在目标电视执行：

```sh
adb shell setprop log.tag.NativeHomeArt DEBUG
adb shell setprop log.tag.NativeHomeHero DEBUG
adb logcat -v threadtime NativeHomeArt:D NativeHomeHero:D '*:S'
```

依次聚焦《谜林》《谎日沉影》及《仙逆》，并 A→B→A。`NativeHomeArt` 每进程最多 400 条、仅 Debug 且显式开启时记录。标签对应真实检查点：匹配缺失/成功、候选为空/存在、旧代次拒绝、Glide 失败（有 HttpException 才记录 HTTP 状态，否则 -1 未知）、解码成功及缓存来源、原生 wall 实际绘制确认、兜底可见。URL 只记录 hash；候选 hash 与请求/绘制 hash 可关联。`SOURCE_WALLPIC_UNAVAILABLE` 明确表示模型未承载字段，不冒充 `SOURCE_WALLPIC_EMPTY`。没有实际失败/解码/绘制时不会凭空打印成功标签。

## 最终验证 / APK / 文件清单

| 文件 | 本轮修改理由 |
| --- | --- |
| `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java` | 非首卡 Back 拦截、身份标题绑定、相同元信息避免重写 |
| `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomeHeroTitleView.java` | 相同影片/标题绑定幂等，影片变化重置 |
| `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeHero.java` | 现有匹配及选择节点的 Debug 诊断 |
| `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHomeBrowseImages.java` | 现有 Glide / wall 的失败、解码、绘制、过期及兜底诊断 |
| `app/src/leanback/java/com/fongmi/android/tv/ui/home/NativeHome10Diagnostics.java` | 新增有限、显式启用、不含 URL/鉴权的日志工具 |
| `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHome09InteractionTest.java` | 将旧“首卡吞 Back”断言更新为本轮正确行为 |
| `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHome10InteractionTest.java` | 新增返回、inset 方向/归零、多窗口、重复文本测试 |
| `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHome10BackgroundTest.java` | 两部真实缓存身份的空横图形态回归，明确为测试重放 |
| `docs/NATIVE-HOME-10.md` | 本报告 |

最终 Leanback 原生主页相关 22 套 / **114 测试，0 失败、0 跳过**。Mobile 实际存在的首页配置恢复、CustomWallView、主题回归 **7 测试，全部通过**；未运行全量 Mobile，因此不声明全量通过。早期一次 Java 静态类型错误已修复，一次新测试未配置 images 导致 NPE 已补齐测试夹具；误用 NativeHome Mobile 过滤器没有匹配到测试，已换成真实存在的测试。最终完整构建 SUCCESS。

构建：`:app:assembleLeanbackArm64_v8aDebug`；另处理 Release Manifest，未生成 Release APK。Debug 签名校验通过（证书 SHA-256 `97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`）。包名 `com.silent.android.webhtv`，debuggable=true；ABI 仅 arm64-v8a；既有 Current/History Launcher alias 均指向 HomeActivity，未改变默认入口。预览 Activity 仅 Debug Manifest，Release Manifest 无该 Activity；原有 Debug 模式门控未修改。

APK：`E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`

大小 **209259650 bytes**；SHA-256：`dcf573b93468c7270565a778707d9a5b5c6bc48201610b02c95c482e7b0a6a9e`。

保护核验：5237 个原有文件中，仅以上 5 个声明的原有 scoped 文件变化，**5232 个其余原文件逐内容一致，无丢失**；4 个新增 scoped 文件。dirty 287→291，HEAD 前后一致。WebHome 114 文件、WOGG 106 文件与原 HEAD/status 均一致；Suite gitlink 一致。未 commit、未 push。task guard 终检 **PASS**：branch/HEAD、原有 protected dirty、scope、staged paths 安全；限定已有 tracked 文件 `git diff --check` 通过。默认启用的 Launcher 为 Current alias，History alias 保持禁用。

## 真机复核与停止点

电视 NOT TESTED：安装后第12/2张→首卡→原退出；抽屉/筛选优先；1080P/4K 实际边界；长/极长标题静止焦点持续滚动的逐帧测量、快速 A→B→A、Logo 显示/失败及页面隐藏恢复；两部目标影片和正常对照的完整日志、站源原始字段、Glide 与最终绘制。需要电视 ADB 或现场日志才能闭合，不以在线手机替代电视验收。

本轮不启动 NATIVE-HOME-11，不恢复历史页任务，等待真机反馈。
