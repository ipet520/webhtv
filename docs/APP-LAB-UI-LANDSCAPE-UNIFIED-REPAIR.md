# 默影视 App 实验室横屏与焦点视觉统一修复

## 交付状态

- App 子仓库：`main`，基线 HEAD `ad46fe5da81eacbfbd0465de6b0b85450cf3780a`
- 本轮没有提交或推送。
- Mihomo 自动代理修复及其未提交文件保留；本报告所列 UI 改动没有改动 Mihomo 代理实现、WOGG、WebHome 或播放器业务逻辑。
- 源码级回归测试与 Leanback ARM64 Debug 构建完成；电视真机焦点视觉与遥控验收仍待执行。

## A. 实验室 UI

### 横屏裁切、二级弹层和滚动

命令详情弹层原布局把 `android:maxHeight="640dp"` 放在 `LinearLayout` 上，该属性不能限制这个容器的测量高度；`BottomSheetDialog` 也没有显式切换到展开状态。长命令表单在横屏/较短视口下因而没有可靠的可视高度和展开约束。依据见 `app/src/main/res/layout/dialog_lab_command_sheet.xml:1-48` 和 `app/src/main/java/com/fongmi/android/tv/lab/LabCommandSheet.java:149-168`。

现在在弹层显示后，从 `BottomSheetBehavior` 设置真实像素高度上限：`min(640dp, 当前 display height)`，并切换到 `STATE_EXPANDED`。移除了放错容器的 `android:maxHeight`；内部 `NestedScrollView` 使用 `fillViewport=true` 和权重高度承载较长内容。命令编辑、运行、停止、输出及动态点击参数按钮也显式设为可聚焦。来源：`LabCommandSheet.java:118-168,315-319`、`dialog_lab_command_sheet.xml:42-48`。

`NestedScrollView` 保留 Android 对聚焦子项的常规可见性滚动行为；本轮没有新增自定义焦点滚动算法。源码测试验证了有界视口、展开状态和滚动容器结构，但弹层动画完成时序、不同电视安全区下的实际裁切、焦点滚动和关闭后的焦点恢复仍需真机确认。

### 主体行可达和焦点背景

- 命令详情页的两秒运行状态轮询沿用 `LabCommandAdapter.refreshStates()` 的逐行差异刷新，只通知发生变化的行，避免整表重建导致焦点丢失。代码在 `LabDetailActivity.java:137-157`、`LabCommandAdapter.java:63-76`；`LabTvFocusSourceTest` 对此已有回归约束（测试文件 `app/src/test/java/com/fongmi/android/tv/lab/LabTvFocusSourceTest.java:48-78`）。
- 命令整行现在可聚焦并使用局部 `selector_lab_command_row`；移除同一行上额外的 selectable foreground。行内“运行”按钮保留自己的 focused drawable，并明确不继承父行 drawable state。行点击打开详情、按钮点击运行的处理器仍分开。来源：`adapter_lab_command_compact.xml:1-53`、`selector_lab_command_row.xml:1-22`、`LabCommandAdapter.java`。
- 包列表行已有单一可点击/可聚焦边界；之前 card 背景与 selectable foreground 会在同一行叠绘。现在由 `selector_lab_package_row` 管理 pressed/focused/normal 状态，移除了第二层 foreground。来源：`adapter_lab_package.xml:1-12`、`selector_lab_package_row.xml:1-22`。
- 动态命令参数按钮继续使用独立的 `shape_lab_click_btn`，现在显式可聚焦。终端快捷项和历史 chip 以前没有适合焦点的背景/状态；现在分别使用紧凑的 `selector_lab_terminal_shortcut` 和 `selector_lab_terminal_history`，保留各自视觉语义。来源：`LabCommandSheet.java:315-319`、`LabTerminalActivity.java:126-136,247-260` 及对应 `selector_lab_terminal_*.xml`。
- 这些行/按钮没有新增 scale 动画。没有修改全局焦点 Drawable、ThemeBinder 或全局缩放；其他工具、开关、标签和图标继续使用各自现有样式。整行与行内按钮分别绘制自身状态，测试检查了父行不再覆盖第二个 foreground，及子按钮不复制父状态。

### 实验室根因与边界

源码确认的裁切根因是 `maxHeight` 属性施加在不支持它的 `LinearLayout` 上，且 BottomSheet 未显式展开。焦点可达性方面，既有整表刷新会丢失焦点的问题已由逐行差异刷新规避；本轮补齐的行/动态控件则分别存在背景双层叠加或缺少可见焦点态的问题。修复按控件类型使用局部 selector，没有把不同控件统一成同一种焦点设计。

源代码测试不能证明真实遥控移动、背景像素大小、动画与安全区表现；这些项目保持 `PENDING`，不以静态测试结果替代电视验收。

## B. 原生播放器字幕 UI

### “第二字幕”异常焦点背景

Leanback 字幕面板中的 `secondary` 是文本控件，宽度为 `wrap_content`、高度 `36dp`，另有水平 `12dp` padding；之前使用 `?attr/selectableItemBackgroundBorderless`。相邻 `search` 是 wrap-content 图标，也使用 borderless ripple，但其内容区域只由搜索图标决定。来源：`app/src/leanback/res/layout/dialog_track.xml:29-60`。

因此两者虽然同处工具栏，RippleDrawable 的边界语义并不相同：第二字幕的宽文本区域使用无界 ripple，焦点涟漪可超出控件边界，造成相对文字显得过大的深色圆形；搜索图标区域较小，表现正常。静态代码未发现这两个控件设置了额外 `minimumWidth/Height`、selected/activated 状态或 scale/stateListAnimator。运行时实际测量尺寸和主题最终生成的 ripple 像素仍须真机截图确认。

最小修改仅把 `secondary` 背景切换为主题属性 `?attr/selectableItemBackground`，让 ripple 限定在文本控件自身边界内。其 wrap-content 宽度、36dp 高度、padding 和焦点能力均保留；搜索按钮仍使用原本的 borderless 背景，没有强制二者同宽，也没有固定日/夜颜色。Java 操作未改：两个控件仍分别进入 `onSecondary` 和 `onSearch`；第二字幕仍切换角色并刷新同一列表，轨道选择仍调用原有 `setSecondarySubtitleTrack`。来源：`dialog_track.xml:29-60`、`app/src/main/java/com/fongmi/android/tv/ui/dialog/TrackDialog.java:154-197,810`。

播放器异常与实验室的行背景问题没有共享 Drawable、布局或状态管理根因；没有修改播放器全局主题、ThemeBinder、列表状态或字幕业务行为。

## 自动化验证

| 检查 | 结果 | 证据边界 |
|---|---|---|
| `:app:testLeanbackArm64_v8aDebugUnitTest --tests 'com.fongmi.android.tv.lab.*' --offline --no-daemon --console=plain` | 59 tests，0 failures，0 errors，0 skipped | 包含实验室焦点源码回归及同包已有测试；不是电视像素/遥控实测 |
| `:app:testMobileArm64_v8aDebugUnitTest --tests com.fongmi.android.tv.ui.dialog.TrackDialogTest --offline --no-daemon --console=plain` | 10 tests，0 failures，0 errors，0 skipped | 新增测试检查有界主题 ripple、按钮独立尺寸/处理器及原字幕状态逻辑；不是实际焦点截图 |
| `:app:assembleLeanbackArm64_v8aDebug --offline --no-daemon --console=plain` | BUILD SUCCESSFUL | Leanback ARM64 Debug 构建 |
| `git diff --check` / `task_guard.sh check` | PASS | 无空白错误；当前任务范围、受保护未提交路径检查通过 |

新增/扩展的源码测试覆盖：实验室命令整行与子按钮焦点背景独立、包行不叠第二层 foreground、二级弹层的展开/高度上限/滚动结构、动态参数与终端控件的焦点态；播放器第二字幕 bounded ripple、搜索按钮保持独立 borderless ripple、两个按钮操作处理器分离、切换后不引入 sticky selected/activated 状态、无新增 scale 动画。

自动化结构测试不渲染控件，也不验证用户要求的日/夜主题实际颜色、连续切换时的焦点像素、字幕菜单的遥控操作和返回行为；这些没有标为通过。

## Debug APK

- 路径：`app/build/outputs/apk/leanbackArm64_v8a/debug/app-leanback-arm64_v8a-debug.apk`
- 大小：209,213,019 bytes
- SHA-256：`2D7F2791EE4EAC0CA65F04247D21DACED4931FF94925BE5B7678D0A7C7012AB3`

本次没有安装到电视。当前可用设备清单没有确认到 Android TV/Leanback 电视目标，因此本报告不将移动设备或 APK 成功构建当作电视验收。

## 待电视真机验收

`PENDING_DEVICE_VERIFICATION`：

1. 实验室横屏详情/命令弹层无裁切，弹层打开后自动展开；长内容能滚动到焦点项，关闭后焦点回到合理的原位置。
2. 命令主体整行可达，行背景与行内运行按钮连续聚焦时互不覆盖、无异常放大/残影；动态参数、终端快捷项和历史 chip 显示各自焦点态。
3. 原生播放器第二字幕与搜索按钮连续切换焦点时，背景尺寸协调且移走后不残留；深色/浅色主题均检查。
4. 字幕列表选择、第二字幕切换、搜索和返回操作与修复前保持一致。

## 最终状态

- `LAB_UI_SOURCE_FIX`：PASS（源码修改及自动化结构回归完成；设备视觉待验）
- `PLAYER_SUBTITLE_FOCUS_SOURCE_FIX`：PASS（局部布局修改及单元回归完成；设备视觉待验）
- `REGRESSION_TESTS`：PASS（上述测试命令及计数）
- `LEANBACK_ARM64_DEBUG_APK`：PASS（路径和校验值见上）
- `TV_REMOTE_AND_THEME_ACCEPTANCE`：PENDING_DEVICE_VERIFICATION
- `COMMIT_PUSH`：未执行
