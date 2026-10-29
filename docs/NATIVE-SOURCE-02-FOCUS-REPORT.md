# NATIVE-SOURCE-02：沉浸换源焦点最短路径

本任务已并入 [NATIVE-SOURCE-02＋TMDB-INLINE-CRASH-01](NATIVE-SOURCE-02-TMDB-INLINE-CRASH-01.md)，联合交付结果及 APK 以该报告为准。

## 已实现

打开聚焦分组；初始下键直达当前实际源（不在分组则第一条）；列表返回直达分组，再次下键恢复刚浏览的有效站源 key；分组返回关闭。左右键在非排序状态一键关闭，清理下拉，并消费该手势重复/释放事件，恢复房子焦点。排序原有上下、确定保存、返回取消优先。焦点、浏览身份、实际选中和滚动位置独立，不产生自动切源。

分组确认后仍聚焦分组；健康排序、分组持久化、布局、宽度、无标题和主题视觉沿用 Source01。没有第二套滚动引擎或延迟焦点任务。

## 验证

NativeSourceDrawerTest 27项、NativeHomeDrawerTest 6项通过。联合影响回归包含主页/历史/分组/主题及 Fusion 浮窗等，详见联合报告。电视遥控器与长列表实际手感 NOT TESTED。

焦点任务初始 App HEAD d48e68823ae283fd9c7286ea221b0ef75a620eed，66条dirty、5286原文件。独立快照与 guard 在 TEMP/native-source-02-current.txt 指向目录；联合任务增加另一个保护会话，不覆盖 Source01/C56 保护记录。最初单独生成的 Source02 Debug 只是合并任务前的阶段产物，不作为最终联合交付。
