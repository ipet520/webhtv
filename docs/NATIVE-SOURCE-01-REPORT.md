# NATIVE-SOURCE-01：沉浸换源面板分组与健康状态

完成日期：2026-10-28T18:01:52.276876+08:00（Asia/Shanghai）。**本轮不添加任何面板标题**。全部PASS均表示源码/自动化/产物验证；真实电视/物理遥控器未执行，另列NOT TESTED。

## 原生功能审计与边界

- HomeActivity.immersiveSites本来就实例化原生SiteAdapter，读取与经典面板相同的可见站源和健康排序结果。本轮不改变该入口、不二次排序，不新建列表Repository。
- SiteAdapter.addAll：忽略isHide站源；Setting.isSiteHealthDialogSort开启时调用SiteHealthStore.sortSites，后者仍遵守全局isSiteHealthSort。关闭时保留原配置顺序。分组过滤只用Site.inGroup，不改顺序。
- Site.getGroups/inGroup经过SiteNameStore/GroupRuleConfig，分组身份继续使用原生规则；名称使用Site.getDisplayName。未修改C56的Site.trans能力标签保护、简繁身份或任何解析规则。
- 分组顺序唯一来源SiteGroupOrderStore.sort/move/save，仍按VodConfig.getCid持久化。save仍使用原全配置分组集合与当前可见顺序的合并规则，兼容消失分组/隐藏位置，不创建另一套存储。
- Leanback SiteDialog.selectedGroup是静态、仅进程内记忆。只增加rememberedGroup(available)/rememberGroup(group)访问接口；经典面板其余行为不改，两个面板共享原字段。失效组回到空身份“全部”，重开/切换配置时重新验证；不宣称跨进程恢复选择，也没有新增偏好数据库。
- 健康点直接SiteHealthStore.getColor；颜色语义不改，8dp点与8dp间距沿用adapter_site_switch。仅绑定、重新获焦、主题刷新或重开时读取已有数据，无健康探测/网络/轮询任务。健康排序不在聚焦时重新调度。

## 沉浸面板既有能力及本轮UI

原窗口宽度公式、END|TOP、MATCH_PARENT高度、全屏标志、主题背景和10%～28%轻遮罩不变。背景继续由原scroller绘制，新增字段区域绘制同一Token，避免半透明双层叠色。原20dp顶部留白、站源40dp行/9dp间距、15sp字号、24dp胶囊状态及文字内边距保留。

只在站源列表上方增加32dp紧凑分组字段及9dp间隔，列表获得剩余高度。**分组区域与站源有视觉区别**：13sp、左对齐、8dp圆角字段、固定末端三角箭头；分组下拉同样左对齐紧凑显示，不加健康点、不画成站源胶囊。不添加“切换视频源”等标题、搜索栏、工具栏、平铺标签或排序按钮。

分组文字单行END省略，复合Drawable为箭头保留空间；全部/空提示使用现有原生资源，新增三语言文案仅用于纵向排序无障碍说明。颜色全部来自现有ThemeController/ThemeTokens；健康颜色单独保留原生业务语义，不重定义。

采用同一Dialog的FrameLayout上层覆盖区：定位在字段下方，不参与body的测量；受抽屉实际可用高度和55%高度上限限制，长分组列表在自己的ScrollView内滚动。展开前后站源ScrollView尺寸/位置和已有站源按钮实例不变。不是全屏新弹窗，不用Spinner或第二窗口。

选择背景与聚焦描边独立；排序用↕轻量提示，其余项弱化且暂不可聚焦。弹出时body禁止后代获取焦点，方向边界留在下拉内；覆盖容器本身不可聚焦并关闭平台默认焦点高亮，避免重现整屏长框。主页仍通过原Dialog窗口隔离，未重建主页/海报/背景。

## 调用链、定位及交互

```text
HomeActivity.immersiveSites → SiteAdapter.getItems（原生可见性/健康顺序）
 → NativeHomeBrowseView.openSourceMenu（原当前站源索引计算）
 → HomePreviewMenuDialog(List<Site>, current, sourceApply, house)
 → Site.getGroups → SiteGroupOrderStore.sort → SiteDialog原进程内选择
 → 确认分组 → Site.inGroup过滤 → 只刷新抽屉站源行
 → 确认站源 → 原sourceApply(Site) → HomeActivity.setSite
```

分组行只有确认才写原selectedGroup/筛选，普通焦点移动不触发筛选。筛选不调用sourceApply、不发站源首页请求。空结果显示原生简洁空提示，字段仍可操作，不关闭抽屉。

长按确定进入排序，只调整临时展示顺序；上下调用SiteGroupOrderStore.move，确定调用原save；返回恢复原快照，不持久化、不筛选或换源。长按repeat与对应松键消费，下一次新的确定才保存；“全部”固定首项不参与排序，边界不越界。没有新增延迟调度器。

当前站源定位仍是原Button.requestFocus＋ScrollView原生可见性处理，未增加滚动算法。只将原索引映射为同一Site.key，以应对过滤和已存在的健康排序。当前源仍在组中则原生定位，组中没有则首可见，不标成当前、不改变真实站源；切回包含当前源的组再次按身份定位。

返回层级：排序→取消但留在下拉；下拉→关闭并恢复字段；抽屉→原关闭/房子焦点恢复。关闭/宿主销毁丢弃未保存排序，不请求焦点进入离开的主页。正常列表首项上键进入字段，初始打开仍定位当前站源，不抢到字段。

## 修改文件与C56隔离

本轮9路径：

- `app/src/leanback/java/com/fongmi/android/tv/ui/dialog/SiteDialog.java`
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewMenuDialog.java`
- `app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeBrowseView.java`
- `app/src/leanback/res/values-zh-rCN/native_source.xml`
- `app/src/leanback/res/values-zh-rTW/native_source.xml`
- `app/src/leanback/res/values/native_source.xml`
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/home/NativeHomeDrawerTest.java`
- `app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeSourceDrawerTest.java`
- `docs/NATIVE-SOURCE-01-REPORT.md`

前三个生产Java分别是最小记忆接口、抽屉展示适配、原有sourceApply接线；3个Leanback资源仅为排序无障碍文案；新NativeSourceDrawerTest为19个真实Dialog测试；既有NativeHomeDrawerTest仅将根ScrollView强转改为取得原scroller，原切换/焦点/请求数量断言全部保留；最后为本报告。

C56初始：41 tracked修改＋16新增＝57路径；完整清单见外部c56-paths.json和未修改的docs/C56-UPSTREAM-SELECTIVE-INTEGRATION.md。它们和本轮文件无交叉，**57条C56源码路径全部SHA不变**，不自动暂存/提交C56。C56交付目录Release/apk/C56-20261028下**2028文件逐SHA不变**。

原5281个App文件仅本轮4个原文件发生变化，其余5277完全不变，无缺失；新增5文件均在声明范围。WebHome/WOGG文件及HEAD/status不变，Suite gitlink不变。HEAD前后均d48e68823ae283fd9c7286ea221b0ef75a620eed，main；dirty57→66，无暂存、commit、push、tag、分支/worktree或破坏性Git操作。

施工前完整差异、57路径原件、4个本轮原件、原文件SHA及C56归档SHA存仓库外：C:\Users\ADMINI~1\AppData\Local\Temp\native-source-01-20261028-172829。独立guard保护57条C56原dirty，最终check PASS；未运行会提交/建tag的finish。中间范围文件CRLF导致误报，已仅修正本轮guard元数据行尾，保护名单和范围身份未改变。

## 测试结果与真实边界

最终影响范围：**Leanback 198项、Mobile 39项，失败/错误/跳过均0**。包含新抽屉19项、NativeHome/History、SiteGroup/Name/Adapter/Dialog、SiteTransBracket。Mobile未改UI，共享分组/命名和C56标签身份回归通过。

新增覆盖：全部/普通/多组/失效/空/无组，长名及大量分组末项滚动，简繁原始分组身份；长按松键、上下移动、首尾、保存/取消/关闭、重开/配置隔离/消失组；原生健康色未知与更新、排序开关及过滤保序；初始当前源/60项远端定位、源不在组内/切回、焦点不换源、确认callback身份、三级返回、overlay不重排/不穿透。

本地Native Graphics截图已查看：字段与下拉的独立紧凑区域、左对齐、原站源胶囊区分、无额外标题；不是电视验收。截图在visuals/native-source-dropdown.png。

失败归因与修复记录：初始测试编译因项目测试TextUtils stub未暴露嵌套枚举，改为检查同一END名称，不改生产；首轮16/17，消失分组的测试预期违背原Store保留已存顺序，修正测试；首次广回归两个原背景/根结构合同失败，生产代码保留旧预览ScrollView根和原scroller背景后通过，没有放宽它们。所有失败日志保留，不冒称首次全绿。

C56历史全量仍为Leanback4631/1失败/2跳过、Mobile5236/6失败/2跳过。本轮未重跑全部集合，不把全量标绿；未修改C56已归因的失败测试或断言。本轮仅影响范围结果为PASS。

真机NOT TESTED：1080P/4K/OEM显示区，物理遥控器长按/连按/触控返回，最大真实配置的下拉滚动与焦点，真实健康数据变化/设置往返/锁定源确认、配置切换、实际换源网络；浅/深/自定义主题最终观感和半透明叠层。未安装APK、未宣称真机通过。

## 构建与候选APK

完整执行assembleLeanbackArm64_v8aDebug与assembleLeanbackArm64_v8aRelease，-PfastRelease=false，BUILD SUCCESSFUL，2m43s。保留R8、资源压缩/优化、lintVital和现有签名，不改Gradle/依赖/播放器；Release文件名使用既有WEBHTV_APK_SUFFIX机制设置独立native-source后缀。

| 变体 | 独立交付路径 | bytes | SHA-256 |
|---|---|---:|---|
| debug | E:\GitHub\webhtv-suite\app\Release\apk\NATIVE-SOURCE-01-20261028\leanback-debug-arm64-native-source-01-20261028.apk | 209262254 | `c553fa37935cec2f14abe500808d9771bffd77f3de7ed85c5defca6e4456f1eb` |
| release | E:\GitHub\webhtv-suite\app\Release\apk\NATIVE-SOURCE-01-20261028\leanback-release-arm64-native-source-01-20261028.apk | 174126170 | `690ce659626e4a20cf46616743c2bd24d5231c0c2e8ae0595708ad980e65af05` |

两包均为com.silent.android.webhtv，versionCode560/versionName5.6.0，apksigner verify通过；沿用已有个人Android Debug证书97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5，不伪称新正式证书。Debug debuggable=true，Release=false。常规lib仅arm64-v8a，30个ELF均Class64/Machine183；未修改既有assets MITV ABI风险。

唯一启用Launcher仍HomeActivityCurrent→HomeActivity，未新增Activity，Release无NativeHomePreviewActivity。Release直接DEX验证openGroups、原生GroupOrderStore.move/save和SiteHealthStore.getColor实际存在于新产物；不是旧APK改名。C56原目录没有覆盖，候选均在NATIVE-SOURCE-01独立目录。

## 汇总结论（PASS限自动化/静态/构建）

```text
TASK = NATIVE-SOURCE-01
GROUP_DROPDOWN = PASS
GROUP_FILTER = PASS
GROUP_REORDER = PASS
GROUP_ORDER_PERSISTENCE = PASS
GROUP_MEMORY = PASS (同进程原selectedGroup；无新增跨进程选择存储)
HEALTH_STATUS_REUSED = PASS
HEALTH_SORT_REUSED = PASS
CURRENT_SITE_AUTO_LOCATE_PRESERVED = PASS
EXISTING_SITE_SWITCH_PRESERVED = PASS
IMMERSIVE_PANEL_WIDTH_UNCHANGED = PASS
IMMERSIVE_PANEL_STYLE_PRESERVED = PASS (仅新增独立紧凑分组区)
FOCUS_NAVIGATION = PASS
BACK_NAVIGATION = PASS
LEANBACK_TESTS = 198/0 failures/0 skipped
MOBILE_REGRESSION = 39/0 failures/0 skipped
LEANBACK_BUILD = PASS (完整Debug＋优化Release)
C56_CHANGES_PRESERVED = PASS
REAL_DEVICE = NOT TESTED
COMMIT = NO
PUSH = NO
SUITE_GITLINK_UPDATED = NO
```

## Recovery anchor / 停止点

源码、198/39回归、完整双候选与产物验证、C56/HTML/Suite保护及guard检查全部完成。唯一后续动作：等待用户真机验收；不自动提交、不继续扩大范围。
