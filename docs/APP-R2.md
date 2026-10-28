# APP-R2：Mihomo停止与TV历史首排聚焦

基线 main `030485b70c263621fe4575abe5758b28115832c3`。2026-10-25施工；不提交、不推送、不修改Suite索引gitlink。按用户最新指令仅构建Mobile/Leanback ARM64 Release，不生成Debug APK；DebugUnitTest变体任务仅用于自动化回归。

## A：真机崩溃证据与最小修复

用户附件 `.codex-attachments/13ac1947-3fd1-4acd-9bcb-e6ffaec9d6ff/1637b6a6-651d-4595-9157-6cbb6850322c/webhtv-debug-log.txt` SHA256=`BC94728E9D0C1AA7E6EB9B21CB4FEECDD2850CAEED498B43DBC099117724F72B`。日志确认TCL Smart TV Pro、Android14/sdk34，2026-10-25 19:31:43.079 FATAL main，NetworkOnMainThreadException；ConscryptEngineSocket.close向TLS输出流写关闭数据时被StrictMode拦截。

调用链：LabCommandSheet点击stop → LabRunner.stop（先同步撤销代理）→ LabMihomoProxy.stop → MihomoProxyMonitor.stop/transition → 同线程Binding.apply → LabMihomoProxy静态绑定回调 → OkProxySelector.setRuntimeProxy → evictRuntimeIdleConnections → ConnectionPool.evictAll → TLS socket close。monitor的synchronized不切换线程。源码实际清理点是OkProxySelector，不是OkHttp.closeIdleConnections；后者早已有独立后台线程。本路径用于启动清空旧绑定、ready状态切换、停止、进程退出、恢复探测与HTTP connectFailed撤销，因此统一修复此清理入口。

`OkProxySelector`继续同步发布volatile runtimeProxy和localHosts，故stop返回前新请求已恢复原持久策略。只把注册池的强引用快照交给进程内单线程执行器；执行器只调用evictAll，不写runtimeProxy，不再扫描稍后注册的新池，不持有selector生命周期锁。快速stop/start后旧任务不能覆盖新代理状态；同一池中新建的空闲连接可能被排队的清理驱逐，但活动exchange仍由OkHttp保留，不改变选路语义。每个池异常独立记日志，不会阻断状态恢复或其它池清理；未删除清理、未改StrictMode。

Mobile与Leanback共用这些Lab/selector类，均覆盖。未改规则匹配、优先级、回环直连、配置读写或进程停止语义。

收尾补充：异步清理存在新请求先于旧池清空取连接的窗口，因此在OkHttp.getBuilder既有请求入口追加runtimeCleanupInterceptor。仅网络请求执行线程等待pendingCleanup Future完成并在代际改变时重新等待；生命周期stop不等待、不持锁关闭socket。中断保留线程中断状态并返回InterruptedIOException，清理失败返回IOException，不允许请求绕过未完成的清理继续复用旧连接。这是同一修复的必要连接复用边界，不改变代理选择结果。

## B：历史首排

仅Leanback HistoryActivity使用HistoryAdapter的1.1倍、中心锚点聚焦缩放，SpaceItemDecoration首排top=0；RecyclerView已有clipToPadding=false和clipChildren=false，但其外层ProgressLayout仍按自身边界裁切。正常卡片首排恰好在列表顶部，聚焦后越界量为完整item测量高度的5%。

HistoryActivity布局完成后按首个实际可见卡片的测量高度设置列表顶部padding：ceil(height×0.05)+ceil(8dp)，留出缩放及高亮余量。保持item尺寸、1.1倍动画、中心锚点、方向键/FocusRestorer、标题评分来源及原行间距；不在focus事件中变更padding，避免聚焦/失焦跳动。Mobile独立HistoryAdapter未发现该1.1倍焦点动画，本轮不修改Mobile历史布局。

## 保护现场与差异边界

原33路径未暂存或提交；其中三条经用户明确授权作必要增量：`catvod/.../OkProxySelector.java`（后台清理快照及请求屏障）、`catvod/.../OkHttp.java`（既有builder增加一个共享应用拦截器），以及`app/src/test/.../MihomoRoutingIntegrationTest.java`（原连接数立即归零断言改为最多5秒等待后仍严格断言为0，相关客户端接入与生产相同的清理屏障）。既有断言内容未删除、三项Gson失败未绕过。其它30条由guard逐文件指纹保护。

施工前完整tracked patch、status以及三文件原样副本位于用户TEMP的`APP-R2-before`目录，并已保存各自`.incremental.diff`；本轮增量分别为selector +39/-1、OkHttp +1/-0、既有routing测试 +4/-2（含替换builder行与有界等待，零连接断言仍在）。新建`MihomoStopThreadTest`、`HistoryFocusInsetTest`及本报告；另修改Leanback HistoryActivity。未改其它播放器/WebHome/WOGG/构建配置/签名配置。guard保持active，供真机验收后的下一阶段恢复；不得finish提交。

## 验证

已有Mihomo配置10项、monitor4项；routing9项中保留三项Gson反射失败，其余6项通过。最终Leanback含历史inset与新HTTPS回归共25项：22通过、3项原有失败；Mobile含原HistoryAdapter3项共27项：24通过、3项原有失败。两端测试任务因这三项失败而整体BUILD FAILED，不能称全绿；本轮新增测试均通过。

最终HTTPS用例先在main runner校验Looper身份，建立本地真实TLS空闲连接；门闩阻塞后台清理worker，主线程stop立即返回且原策略可见，随后切换新端口；清理未完成时新HTTPS请求不能到达server，释放门闩后请求完成、idle pool清空且新会话状态不被旧任务覆盖。执行器屏障确认cleanup线程不同于main。

HTTPS夹具使用临时自签证书和真实本地TLS请求；JVM Robolectric/Conscrypt与SunJSSE冲突需夹具临时移除Conscrypt并在finally恢复，测试进程需`--add-opens=java.base/sun.security.ssl=ALL-UNNAMED`。未开放java.net包，因此旧Gson三项仍失败。初期夹具的socket门闩、TLS provider与IPv6监听不匹配导致失败，已修正测试环境，未放宽生产约束。历史测试覆盖多高度/密度下5%放大外扩与8dp余量及位置一致性，不代替实际TV焦点/滚动验收。

## 独立异常（仅记录）

同一日志19:31:43.112，CustomActivityOnCrash的uncaughtException中Class.forName(HomeActivityCurrent)失败。Leanback Manifest中HomeActivityCurrent是指向HomeActivity的activity-alias，并无对应Java类。建议另项为崩溃组件显式配置真实HomeActivity重启目标或解析alias targetActivity；本轮未修改，且未妨碍本次构建/测试。

## Recovery anchor

两项生产补丁、双端定向测试和最终Release产物核验完成，全部未提交。下一步仅等待用户Mobile/TV真机验收。

## 最终交付

最终双端ARM64 Release构建BUILD SUCCESSFUL，3分54秒。源码基线仍为`030485b70c263621fe4575abe5758b28115832c3`；APK内GIT_REVISION为该SHA，GIT_STATE=dirty，BUILD_TIME=202610252020（Asia/Shanghai）。版本5.6.0/code560，applicationId=com.silent.android.webhtv，minSdk24/targetSdk28。已从两包DEX直接找到该SHA、构建时间与新`mihomo-pool-cleaner`标记及清理屏障错误字符串，确认实际包含本轮共享补丁。未提交改动参与构建，SHA本身不能单独重建同一产物。

| 产物 | 完整路径 | 字节数 | SHA256 |
| --- | --- | --- | --- |
| Mobile | E:\GitHub\webhtv-suite\app\app\build\outputs\apk\mobileArm64_v8a\release\mobile-arm64_v8a.apk | 171556791 | DC775AF9E6DDCC2275203E3D8CF473F63568AA3DB86CCC7A49CCC78719CA31CA |
| Leanback | E:\GitHub\webhtv-suite\app\app\build\outputs\apk\leanbackArm64_v8a\release\leanback-arm64_v8a.apk | 171267700 | D1600989CCACEC02B280E4BF144702C935BD11F034BD25B34A54318C3BC50AFA |

apksigner两包均退出0、Verifies、v2=true，Android Debug证书；证书SHA256=`97598e0d573df77d8bc58f3946865f6c5d2a4119726d1bcd5276a494d7385dc5`。按用户最新授权，此证书不阻断自用测试；没有额外更改签名/版本。aapt2解析成功，常规lib实际仅arm64-v8a。本轮无原生依赖变更；C54已记录的32位assets/libmitv.so兼容性风险仍保留，未扩展处理。

最终定向结果：Leanback 25项/22通过/3项既有Gson失败；Mobile 27项/24通过/3项既有Gson失败，新增HTTPS清理与历史inset测试、既有Mobile HistoryAdapter3项均通过。没有生成Debug APK，没有运行全量测试，也没有将定向任务的三项失败伪报为成功。测试日志为TEMP/app-r2-tests.log、app-r2-mobile-tests.log；最终构建日志为TEMP/app-r2-release-final.log，产物核验为TEMP/app-r2-artifacts.json。

待真机：TCL Android14已有HTTPS连接时停止/重启Mihomo及连点启停，确认无主线程异常且原代理恢复；验证原显式分流/回环直连；历史页首排左中右焦点完整、上下与滚回首排、普通/聚焦切换无跳动及第二排/文字布局；Mobile启动停止与网络恢复。本轮自动化使用JVM TLS，不宣称已在Android Conscrypt真机复现并验证修复。HomeActivityCurrent独立恢复异常仍未修复。

最终不提交、不推送；Suite索引gitlink仍为74af2c5137a9380cd454c78098668806b1b27e6a。原33路径中三条必要增量已单列，其余30条保护指纹一致，既有内容保持；索引没有本轮暂存内容。停止施工，等待用户分别验收。
