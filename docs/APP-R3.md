# APP-R3 — 遗留测试与崩溃恢复修复

## 边界与基线

仓库 E:\GitHub\webhtv-suite\app，main，HEAD 030485b70c263621fe4575abe5758b28115832c3。未提交、未推送、不修改 Suite 索引 gitlink，不修改 WebHome/WOGG。APP-R2 真机验收仍 PENDING。

APP-R2 guard 原样保留于 `.codex/task-state/deferred/APP-R2-before-APP-R3-20261025`，本轮 guard 保护36条既有路径；另行核对采用路径 MihomoRoutingIntegrationTest 与施工前副本，要求完全一致。原33路径及R2后台连接池清理、请求屏障、历史焦点padding不改。

## A：Gson 真正触发点

独立复测 Mobile 原三方法，3/3失败：loopbackControllerAndLocalInterfaceNeverLoopThroughProxyOrWildcard、explicitWildcardStillWinsOverAutomaticDefault、appAndSiteExplicitRulesRetainPriorityAndPreferencesRemainUnchanged。日志 TEMP/app-r3-before.log；完整异常XML保留 TEMP/APP-R3-before/three-failures.xml。

`MihomoRoutingIntegrationTest.java:85` 暴露生产 `Proxy.arrayFrom()` 吞掉的解析异常：Gson JsonIOException / InaccessibleObjectException，无法反射 java.net.Proxy#type，java.base 未开放 java.net。不是单纯测试快照：生产模型将 proxies、uris、wildcard 派生缓存纳入默认反射，生产解析异常被 catch 后返回空规则列表。ProxySetting、VodConfig、LiveConfig 均消费该解析器。已确认JDK21复现；未据此宣称Android设备已有同样反射错误。

最小修复：`catvod/src/main/java/com/github/catvod/bean/Proxy.java` 三个派生字段标记 transient。name/hosts/urls持久化格式、init计算、代理匹配优先级不变。新增 ProxyConfigSerializationTest 2项：实际生产解析与init前后序列化一致、键集合严格一致、往返路由一致；伪造缓存输入不能注入运行时代理。原routing测试完全不改，不放宽断言。

## B：Alias 被当作 Java 类

日志19:31:43.112中的 HomeActivityCurrent 是activity-alias，不是真实Java类。CAOC 2.4.0未显式指定重启类时从Launcher推测目标并Class.forName(alias)，触发ClassNotFoundException。本地依赖Builder已核实支持restartActivity(Class)。

`app/src/main/java/com/fongmi/android/tv/Startup.java` 添加 `.restartActivity(HomeActivity.class)`，共享源集在两端均解析真实HomeActivity。保留trackActivities、后台模式、CrashActivity、CrashReport安装顺序、恢复进程守卫；未修改CAOC崩溃循环限制、静默重启或日志导出。两端Current/History Alias和AppBranding图标切换保持原样，:error_activity独立进程不变。

新增 CrashRestartTargetTest 3项：配置真实可加载类、两端Alias target和独立错误进程、两种图标模式Launcher仍选相应Alias。配置/源码与JVM检查不等同于真机异常进程重启验收。

## 自动化

无任何 add-opens 的 R3 定向任务：Mobile 45/45、Leanback 45/45通过。含原routing9项（原三失败均通过）、序列化2项、重启3项、CrashLogWiring13项、CrashLogText10项、AppBrandingContract8项。日志 TEMP/app-r3-mobile.log、app-r3-targeted.log；XML已保存 TEMP/APP-R3-before/{mobile,leanback}-r3-results。

R2回归与Release结果将在构建完成后追加。测试任务使用DebugUnitTest变体不等于构建Debug APK。未运行全量测试，不宣称全量通过。

## APP-R2 APK备份（先于新构建）

保存在 `Release/apk/APP-R2-before-APP-R3-20261025`，逐个复制并校验源/备份SHA256一致，未删除原成果：

| 文件 | 字节 | SHA256 |
| --- | ---: | --- |
| mobile-arm64_v8a.apk | 171556791 | DC775AF9E6DDCC2275203E3D8CF473F63568AA3DB86CCC7A49CCC78719CA31CA |
| leanback-arm64_v8a.apk | 171267700 | D1600989CCACEC02B280E4BF144702C935BD11F034BD25B34A54318C3BC50AFA |

## 待真机验收

R2：TCL Android14已有HTTPS连接时停止、快速启停、代理恢复和首排历史卡片聚焦完整性；Mobile启停及网络恢复。R3：切换Current/History图标后的正常桌面启动、受控崩溃后的错误页面/日志导出/重启与静默恢复、连续崩溃防循环。自动化或新APK构建不代替上述验收。
## R2保护与回归结果

R2定向回归 Leanback16/16（配置10、monitor4、HTTPS stop/restart1、历史inset1）、Mobile18/18（配置10、monitor4、HTTPS1、HistoryAdapter3）通过。与R3定向合计 Leanback61/61、Mobile63/63；这是指定集合，不是全量测试。R2真实TLS测试夹具沿用测试进程 sun.security.ssl opens，仅供JVM TLS provider兼容；R3全部45项/端单独运行时没有add-opens，绝未开放java.net，也没有把JVM参数作为Gson修复。

保护检查通过：guard验证36条既有路径指纹；独立对比已采用的routing测试SHA256均为31BDF5D180F38145250D8C550E12C750F12F7F63B4DFAA76B9E8FF646A5252DE。故原33条及R2增量全部保留，本轮仅修改原干净的Startup.java、Proxy.java，新增两个测试及此报告。索引无暂存，HEAD不变，Suite索引app gitlink仍为74af2c5137a9380cd454c78098668806b1b27e6a。
## 双端Release交付（2026-10-25）

现有标准ARM64 Release任务构建成功，3分55秒，未构建Debug APK，未改签名、版本、ABI、R8或检查配置。包名com.silent.android.webhtv，5.6.0/code560，minSdk24/targetSdk28。两包DEX均检出SHA 030485b70c263621fe4575abe5758b28115832c3、BUILD_TIME=202610252058（Asia/Shanghai）、GIT_STATE=dirty及R2 mihomo-pool-cleaner标记；SHA表示提交基线，不能独立重建未提交候选。

| 产物 | 完整路径 | 字节 | SHA256 |
| --- | --- | ---: | --- |
| Mobile | E:\GitHub\webhtv-suite\app\app\build\outputs\apk\mobileArm64_v8a\release\mobile-arm64_v8a.apk | 171556791 | 502DB189849D7A74A1B0CE8C03DCC1D79EFE57F403F7F5565ABEC827A5BE7698 |
| Leanback | E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\release\leanback-arm64_v8a.apk | 171267700 | E46AE59B719D199AF1E69794E15954367BC13B48770D4C96120F3EB68E98BA5D |

两包aapt2解析通过；apksigner verify退出0，v2签名有效。沿用Android Debug证书（本次用户明确接受自用测试），证书SHA256：97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5。未改签名。不作为正式发布资格证明。

实际lib目录均仅arm64-v8a，包含30个.so，关键库包括libijkplayer、libijkffmpeg、libijksdl、libexo_ass、libmedia3ext、libcnode、libquickjs-android-wrapper、libpython3.10。完整清单见证据artifacts.json。构建仍有既有CXX5202/armeabi-v7a告警；常规lib的实际ARM64清单已核实，历史assets/libmitv.so的32位兼容风险保留，不擅自重建原生依赖。Gradle弃用提示仍在。

不仅检查生成源：从两包实际DEX反汇编确认Startup将真实HomeActivity常量写入CAOC restartActivityClass（Mobile映射kw.i，Leanback hv.i）；两包Proxy派生字段m/n/o均保留TRANSIENT位。最终APK Manifest两种Alias均指向真实HomeActivity，:error_activity仍在。源集回归测试、R8映射和实际APK字节证据相互对应。

证据存于 `Release/apk/APP-R3-evidence-20261025`：原失败XML、双端R3与R2回归XML/日志、构建日志、APK信息JSON、DEX关键类与最终Manifest文本。新增差异单列为该目录内APP-R3-incremental.patch，只有两个生产文件、两个新增测试和本报告，不混入原33路径或R2差异。

最终状态：main HEAD未变；未暂存、未提交、未推送；原33保护路径及R2全部保留，Suite gitlink不改。仅交付自用测试候选，APP-R2与R3真机验收仍待用户回报。自动化没有发现本轮新增回归，不声称全量测试或设备验收通过。完成后停止施工。