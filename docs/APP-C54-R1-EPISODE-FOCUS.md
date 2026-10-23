# C54-R1 TV 选集焦点可视性

阶段 A：补丁未提交，等待用户 TV Debug 验收。基线 main 38f89f3dc2d16508cc45c3450ffa91509b0045a3。用户已反馈 C54 初步真机验收未见其他问题。

## 代码证据与修改

EpisodeListDialog 将 VerticalGridView 放入 NestedScrollView，关闭 nested scrolling 并设置整段估算高度。关闭 nested scrolling 不会关闭 Leanback 自身的焦点定位滚动。旧 alignEpisodeScroll 在 selected 回调后按行号乘固定高度调用外层 scrollTo；该目标没有读取内层实际滚动偏移、控件测量矩形，也没有等待 Leanback positioned 回调。两套定位可能在不同布局时刻覆盖彼此。代码能够证明这个坐标/时序缺陷，但没有连接用户 TV，不能确认其恰好导致照片中120—123集的全部现象。

新 EpisodeScrollVisibility 使用 item.getDrawingRect 与 offsetDescendantRectToMyCoords 得到真实内容坐标，和外层 scrollY/height/padding 定义的视口比较，给焦点描边保留8dp余量，只滚动必要差值。选择回调及 selected-and-positioned 回调在下一帧读取当前选中且持有焦点的 holder，避免排队的旧行/分段位置用于新焦点。移除了 alignEpisodeScroll 的行号计算和首行强制归零；整段高度计算仍保留用于现有布局，不再作为焦点滚动目标。

布局 XML、EpisodeAdapter、线路/季/分段切换和方向键处理未修改。生产改动为 EpisodeListDialog.java 与新 EpisodeScrollVisibility.java。

## 自动化证据

Robolectric SDK28 真实 NestedScrollView/FrameLayout/View 测量布局测试读取 descendant Rect 与 scrollY，覆盖20/40/100/160条、40/57/190/206像素项高、119/122索引附近及正反跳转；每次断言包含8像素余量的上下边界均处于视口，重复 reveal 不跳动。另测试完全可见时零滚动、上下边缘最小差值。此为坐标几何测试，不是完整 Leanback 遥控器事件或 TV 仪器测试。

首两次测试失败于测试夹具：NestedScrollView 无界测量子内容，实际内容只有190px，scrollY被范围约束为0；显式设置夹具最小内容高度后最终通过，没有放宽可视性断言。

最终 :app:testLeanbackArm64_v8aDebugUnitTest 过滤 EpisodeScrollVisibilityTest 和原有 EpisodeListConcurrencyContractTest，4 tests，0 failures/errors；同次 :app:assembleLeanbackArm64_v8aDebug BUILD SUCCESSFUL。日志位于用户 TEMP 的 c54-r1-validation.log。

APK：app/build/outputs/apk/leanbackArm64_v8a/debug/app-leanback-arm64_v8a-debug.apk。

## 待设备验收

文本/TMDB卡片各检查20、40、100+集；连续上下、快速反向、顶部/中间/底部、120—123集；切季/分段/线路；当前播放集初始定位；末行不满时方向键行为及完整描边。没有 adb 可执行入口，未执行设备坐标/滚动事件采样；不能声称真机根因闭环或全部交互已通过。

原33条用户dirty路径由task guard保护。阶段A禁止提交/推送/Release；guard保留active，待用户确认Debug后进入阶段B。

## 阶段 B 用户验收

2026-10-24 用户明确确认 TV 真机验证通过，授权独立提交 R1 和双端 ARM64 Release 构建。沿用已通过的4项定向测试及 Debug 构建证据；不重复修改业务逻辑。旧阶段A记录保留为历史证据。

Recovery anchor：用户TV验收通过；收口R1提交后进行双端Release构建与产物核验。
