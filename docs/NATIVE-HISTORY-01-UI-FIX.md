# NATIVE-HISTORY-01 UI 定点修复

日期：2026-10-27。目标 Leanback；A 部分允许的历史 UI 修复与 B 部分只读悬浮审计分别交付。未实现任何新悬浮功能。

## 1. 原生删除 UI 复用

经典历史真实执行路径：`HistoryActivity.onDelete()`（`app/src/leanback/java/com/fongmi/android/tv/ui/activity/HistoryActivity.java:84`）使用 HistoryAdapter.isDelete/setDelete 控制模式，第二次删除按钮弹原确认框；`HistoryAdapter.java:59` 分派点击删除/正常打开，`:91` 绑定原 delete view；返回在 HistoryActivity`:237` 退出删除模式。主页最近观看的 HistoryPresenter 同样复用 AdapterVodBinding。

原视觉定义：`app/src/leanback/res/layout/adapter_vod.xml:20` 的 ShapeableImageView，使用 `@color/webhtv_color_player_scrim`、`@drawable/ic_vod_delete`（原垃圾桶图标）、Vod.Grid.Large 形状。它不是粉红色蒙层加大叉号。

沉浸适配器原先自行 new TextView("×")，填充 colorError 86% 透明度，造成用户反馈。现改为从 **同一个 AdapterVodBinding** 取得 delete 控件并放到原横向海报容器。唯一布局适配是 FrameLayout.LayoutParams(MATCH_PARENT,MATCH_PARENT)。没有复制一套删除资源/状态系统，没有修改经典 XML、图标、HistoryAdapter、HistoryPresenter 或 HistoryActivity。

仍由原 Activity 的删除状态和回调控制；局部 delete payload 只切可见性。单项删除、全部清除、确认、长按、Back、历史存储、播放入口和横向焦点缩放均保留。没有增加海报上的进度视觉。

## 2. 顶部按钮明暗背景可读性

实际共享入口：`app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewStyles.java` 的 function(TextView,ThemeTokens)。原普通态完全透明，聚焦态只有短下划线，文字使用 surface 语义色；明亮剧照可能降低对比度。NativeHistoryBrowseView.refreshTheme 和 NativeHomeBrowseView 的顶部功能均消费此共享样式，故统一修复。

- 每个文字按钮自身添加 6dp 圆角深色半透明背景；颜色来自全局 colorPlayerScrim，alpha 为 0.72。
- 普通/失焦文字使用 colorPlayerControlMuted，聚焦使用 colorPlayerControl；这些已有全局 Overlay Token 避免浅色主题的黑字叠深色按钮。
- 聚焦仍叠加现有 colorFocus 短下划线，保留 1.08 倍、120ms 动画。
- 无新主题设置、边框、全屏遮罩；不改变位置、LayoutParams、字号、padding、宽度、功能排序或焦点导航。房子图标样式不变。
- 主题返回时沿现有 refreshTheme 重建按钮状态 Drawable，普通/聚焦均消费最新 Token；不是只引用 XML 属性。全页及海报不因本补丁重建或重绑。

这是用户明确允许的共享小按钮样式修复，因此沉浸主页文字功能按钮同步受益；经典主页、Mobile 样式不变。

## 3. 实际变更文件

| 文件 | 本轮变化 |
| --- | --- |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/NativeHistoryPosterAdapter.java | 替换自制删除 TextView，复用原 AdapterVodBinding.delete。 |
| app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewStyles.java | 小按钮全局 Overlay Token 背景/前景，保留原下划线和缩放。 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHistoryBrowseTest.java | 更新共享按钮 Drawable 断言。 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHomeVisualRepairTest.java | 更新前景和背景断言，继续验证房子样式、缩放及尺寸。 |
| app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeHistoryUiFixTest.java | 新增：经典删除控件实际复用、浅深主题/自定义焦点色刷新、明暗底图文字对比度、几何不变。 |
| docs/NATIVE-HISTORY-01-UI-FIX.md | 本报告。 |
| docs/TMDB-DETAIL-02-FLOAT-AUDIT.md | 独立的只读审计结果，没有对应播放器代码修改。 |

前四项都基于已有未提交内容做最小增量；完整原件、逐路径 SHA-256 及原差异存于外部证据目录。

## 4. 自动化及构建

PASS：`testLeanbackArm64_v8aDebugUnitTest --tests '*NativeHistory*' --tests '*NativeHome*'`，**130 tests，0 failures，0 errors，0 skipped**。包括历史删除/长按回调、顶部与轨道导航、首卡进入、空/重排/删除、进度与详情入口、原主页相关配置/焦点/主题回归。

新增可读性测试：浅色、深色及自定义焦点色重新应用样式；白/黑底合成后普通和聚焦文字对比度均至少 4.5:1；未以此代替真机视觉验收。删除控件核对实际类、原资源 id、drawable 尺寸、背景类型、父容器、不可聚焦及初始隐藏状态。

PASS：完整 `:app:assembleLeanbackArm64_v8aDebug`。最终联合测试/构建 BUILD SUCCESSFUL，1m 2s。使用既有 JDK21、离线 Gradle9.5.1、Android SDK、Chaquopy Python3.10/wheels；未构建 Release。首次普通沙箱构建被外部 Gradle 缓存写权限阻止，随后使用受控工具权限执行。首轮新增测试编译遇到项目 android.graphics.Color 精简测试桩缺少常量，已只改新增测试为字面测试颜色并重跑通过；没有改公共桩或生产 Color 类。

```powershell
./gradlew.bat :app:testLeanbackArm64_v8aDebugUnitTest --tests '*NativeHistory*' --tests '*NativeHome*' :app:assembleLeanbackArm64_v8aDebug -PchaquopyBuildPython=E:/DevTools/python-3.10/python.exe --offline --no-daemon --console=plain
```

APK：`E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\debug\app-leanback-arm64_v8a-debug.apk`

- 文件大小：**209,260,154 bytes**。
- SHA-256：`4b1c7a3ee90d7b3be2decbed8044a42f5323050137d593cb10daaf1020707398`。
- 包名：`com.silent.android.webhtv`，android:debuggable=true。
- ABI：ZIP 中 lib 只有 `arm64-v8a`。
- apksigner verify：PASS，APK v2，Android Debug；证书 SHA-256 `97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`。
- 实际 Manifest：启用 HomeActivityCurrent alias 指向既有 HomeActivity，HomeActivityHistory alias 禁用。HistoryActivity 仍存在，无新增 Activity 或桌面图标；TmdbDetailActivity/VideoActivity 未开启系统 PiP，与本轮只读审计一致。

## 5. 工作区保护

施工前后 App HEAD 均为 `030485b70c263621fe4575abe5758b28115832c3`。初始 301 条 dirty 路径、5,251 个已有文件；结束为 304 条 dirty（新增测试和两份报告），只增量修改声明的四个原有文件，其余 5,247 个原有文件逐内容 SHA-256 不变，未缺失文件。

task guard start 已通过，保护 297 条范围外初始 dirty；最终 check 为 PASS（branch/HEAD、protected dirty、scope、staged paths），逐路径 hash 对照亦 PASS，证据记录在外部目录。WebHome 114 文件、WOGG 106 文件内容和 HEAD/status 均不变；Suite 原 gitlink 不变。播放器、TMDB 详情、Fusion、历史存储、Mobile 均未修改。不 commit、不 push、未 reset/clean/stash/checkout、未创建分支或 worktree。

外部证据：`C:\Users\ADMINI~1\AppData\Local\Temp\native-history-ui-float-audit-20261027-152306`，含 before.json、originals、build-test.log、test-summary.json、apk-signature.txt、apk-manifest.xml、protection-after.json 及 guard 日志。APK 和文档不意味着 App stable authority 更新。

## 6. 真机待验证

以下均 **NOT TESTED**：

1. 真实 TV 删除模式：原垃圾桶/暗遮罩效果、单项删除、全部清除确认、取消及 Back；经典历史对照。
2. 明亮/白色剧照下删除与观影报告的普通、聚焦及失焦可读性；浅深/自定义主题往返即时刷新。
3. 原位置/尺寸、遥控器上下左右、长按和历史首卡进入规则无回退。
4. 沉浸主页共享按钮背景是否合适，分类、站源抽屉、轨道和 WebHome 行为无回退。

本轮未安装 APK、未连接电视、未测设备帧率或播放 Surface 连续性。B 部分只给方案，不提前施工。
