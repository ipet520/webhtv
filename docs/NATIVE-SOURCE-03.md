# NATIVE-SOURCE-03：换源面板真机问题小修

## 范围与基线

2026-10-28，Leanback 沉浸主页换源面板。App main / d48e68823ae283fd9c7286ea221b0ef75a620eed，初始71条dirty、5290个已有非忽略文件。仅修改面板类、对应测试和本报告；不修改 Mobile、播放器、主页/历史/TMDB其他模块、站源数据或 Suite gitlink。未提交、未推送。

## 四项问题与修复

### 1. 分组文字裁切

原按钮仍为32dp、13sp，箭头10dp、间距8dp，单行 END 省略。没有明确关闭系统字体留白，背景切换后的 padding 又沿用 Button 现状，文本度量与实际电视字体/主题相关。本地 xhdpi 修复前实测：height=64px、paddingTop/Bottom=0/0、lineHeight=35px、baseline=43px。**本地没有复现电视的实际上下截断，不能把零padding说成非零，也不能将裁切根因夸大为已完成电视测量。** 已确认的问题是字体留白和基线没有显式收敛，未对不同字体做边界保证。

修复：统一 compact Button 的 includeFontPadding=false、明确垂直 CENTER、上下padding=0、左右12dp、minHeight/minimumHeight=0；仍使用原32dp高度，不增高。保留原单行END省略、箭头10dp和间距8dp。分组选择器保持START对齐，下拉/站源条目保持居中。没有改变列表位置或宽度。

### 2. 返回键列表/下拉直接关闭

原 dispatchKeyEvent 有层级处理，但没有覆盖 Dialog.onBackPressed；Android14 Dialog.onStart 注册默认系统返回回调，直接调用 onBackPressed，其默认行为为取消对话框，绕过自定义 KeyEvent 逻辑。[AOSP Android14 Dialog.java，onStart / onBackPressed](https://raw.githubusercontent.com/aosp-mirror/platform_frameworks_base/android14-release/core/java/android/app/Dialog.java)。本地修复前测试直接调用系统入口，确认列表中返回会关闭面板。这条代码路径解释用户现象；没有电视按键链路录制，因此不宣称已经捕获电视实际回调时序。

修复：原生面板覆盖 onBackPressed 作为唯一返回决策；KeyEvent BACK 在有效UP时委托同一入口，DOWN/repeat/取消UP只消费。优先级：

1. 排序：仅取消排序；
2. 分组下拉：仅收起并恢复分组真实焦点；
3. 列表或暂时无焦点：聚焦分组，不关闭；
4. 只有分组已获真实焦点：关闭整个面板。

保留 Source02 的浏览key、滚动和实际选中身份；回到分组后下键恢复有效浏览位置。左右快捷关闭及其事件尾部防穿透没有改动。没有额外延迟任务、Activity返回拦截或第二套返回管理器。

### 3. 胶囊上下视觉留白

与第1项共因：Button保留系统font padding/baseline，且不同主题可能带不同默认文本度量；固定尺寸背景本身没有inset，stroke是原1dp（已选聚焦2dp），无StateListAnimator。修复统一分组及站源条目的文本垂直规则，聚焦/失焦不改变padding、baseline、尺寸或scale；保留原40dp站源高度。通过字形上下边界及中心误差测试检查，不以扩大高度掩盖。最终电视字体的光学中心仍需真机复核。

### 4. 材质厚重

原面板颜色来自 ThemeTokens.colorSurfaceContainer，alpha直接使用 dialogOpacity，接近实色，且没有玻璃高光过渡。

本地WOGG对照分开记录：HEAD=be71724d3a168ba69dbc2c925d1ac71ac720d355 中，旧drawer alpha=.97、scrim=.54；当前未提交 tv-browse.css:167–170 为drawer rgba(18,22,30,.42)、scrim=.28、backdrop blur14px/saturate1.15、伪元素blur24px/alpha.42、高光.08→.02、底部暗色.06。本轮采用用户指定的**当前工作区材质参数**作参照，不将其冒称为HEAD稳定版本。

原生实现保留全局主题：底色仍为colorSurfaceContainer，填充alpha=clamp(dialogOpacity×.47,.30,.50)，顶部叠加主题colorOverlayLight×.08、底部叠加colorShadow×.06，中部保持主题底色。用同一个背景GradientDrawable覆盖原body，header和scroller背景透明，避免重复堆叠不透明层。Window dim仍按原主题scrim限制在.10–.28。分组悬浮下拉的可读背景、列表胶囊、文字、健康点、焦点和选中颜色保持。

**没有真实背景模糊**：采用用户允许的轻透渐变近似，不新增RenderEffect、截图纹理、图片解码或逐帧模糊；不能宣称与浏览器backdrop blur像素等效。布局结构、面板宽高和顶部留白完全不变。旧String[]测试/预览构造路径保留原材质；真实站源构造路径应用新材质。

## 修改文件

- app/src/leanback/java/com/fongmi/android/tv/ui/homepreview/HomePreviewMenuDialog.java：统一系统/KeyEvent返回、显式文字度量、主题半透明材质。
- app/src/testLeanback/java/com/fongmi/android/tv/ui/homepreview/NativeSourceDrawerTest.java：系统返回各层级、字形边界/基线/长文本、材质透明度/主题动态刷新回归。
- docs/NATIVE-SOURCE-03.md：本报告。

## 验证与APK

### 测试结果

- PASS：209项 / 35 suites，0 failures、0 errors、0 skips。包含最终32项 NativeSourceDrawerTest、原有NativeHome、分组/站源/健康排序、PlaybackControllerStateTest和FusionFloating回归。先执行208项，再新增中文/4K测试后定向重跑32项抽屉测试；合并最后有效结果为209项。
- 修复前系统返回测试：FAIL（预期的缺陷复现），列表被取消关闭；修复后同测试PASS，覆盖列表、分组下拉、排序、选择器四层。排序/下拉的原KeyEvent Back及系统Back均覆盖。
- 中文“全部”在1920×1080与3840×2160密度下：字形上下边界完整、视觉中心误差≤3dp；长文本正常单行ellipsisCount>0；聚焦前后baseline/尺寸不变；箭头仍10dp。原32dp槽未增高，1080P本地lineHeight由35px降为30px、baseline由43px变为41px。
- 原胶囊聚焦/选中配色和健康点逻辑不变；浅/深/自定义主题刷新测试PASS，材质中部RGB取当前主题surface，焦点描边取当前colorFocus，且刷新不改实际源或浏览焦点。真实影视背景下的可读性最终需电视确认。
- 本地Native Graphics渲染已检查中文与下拉布局，截图位于交付evidence/native-source-dropdown.png；截图不是电视验收。
- Mobile测试NOT RUN：生产修改仅Leanback。共享PlaybackActivity、TMDB详情与其他已有崩溃修复通过施工前后SHA保护验证未改。
- 新增测试曾遇现有轻量TextUtils/Color测试桩缺少API，以及普通Activity不匹配AppCompat参数的编译问题，已只修正本轮测试写法，未更改共享测试桩或生产主题接口。最终定向编译/运行通过，失败尝试日志保留。

### 工作区保护

- 前后HEAD=d48e68823ae283fd9c7286ea221b0ef75a620eed，main不变；无commit/push/tag。
- 全部5290个既有文件逐路径SHA对照，只有本轮授权的面板类和对应测试改变；新增本报告。初始71条dirty，最终72条。没有清理、reset或覆盖旧源码。
- 独立guard保护69个既有dirty路径，check PASS；仅接纳面板类与其既有测试。12个原有数据、站源定位、分组/健康/排序与窗口几何核心方法逐方法一致。
- 17个既有归档APK SHA全部不变，包括C56、NATIVE-SOURCE-01/02及TMDB-INLINE-CRASH-01交付包。新包使用独立目录。
- WebHome/WOGG HEAD与完整status不变，Suite gitlink不变。
- 原始快照与证据：`%TEMP%/native-source03-20261028`；交付evidence保存定向测试XML、构建日志、APK校验、保护结果、差异补丁及Release mapping。

### 构建与APK

PASS：单次最终联合构建 `assembleLeanbackArm64_v8aDebug assembleLeanbackArm64_v8aRelease -PfastRelease=false`，2m28s、346 tasks。实际Release执行R8、资源收缩/优化、Release lint，debuggable=false；Debug为true。

两包applicationId=com.silent.android.webhtv，versionCode=560 / 5.6.0；仅arm64-v8a、30个AArch64 ELF。Manifest已启用Launcher仍为HomeActivityCurrent → HomeActivity，未新增图标。签名使用已授权Debug keystore，证书SHA-256：97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5，与上轮一致。

**DEBUG**

路径：`E:\GitHub\webhtv-suite\app\Release\apk\NATIVE-SOURCE-03-20261028\leanback-debug-arm64-native-source-03-20261028.apk`

大小：209,262,254 bytes（199.57 MiB）

SHA-256：`2144974b7e803daa17b5fbd915047b34ae52cfc72707856d10e6891b1140d96c`

**RELEASE**

路径：`E:\GitHub\webhtv-suite\app\Release\apk\NATIVE-SOURCE-03-20261028\leanback-release-arm64-native-source-03-20261028.apk`

大小：174,126,170 bytes（166.06 MiB）

SHA-256：`e45aeaa40b91ec78c07e9500c16688ed587362ccb84bf4d36a1ac3d142087212`



## 真机待确认

NOT TESTED：TCL实际系统返回路径、真实电视字体的上下截断/光学中心、浅色影视大背景下的材质与文字/健康点对比度、长按左右及快速返回手感。自动测试和本地渲染不能替代电视验收。

完成本轮后停止，等待真机反馈。
