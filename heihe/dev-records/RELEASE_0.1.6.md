# heihe 0.1.6 交付记录

本次隐藏截图中的五个顶部推广入口，并修复窄工具窗口下多图帖有计数、没有左右箭头的问题。

## 改动与依据

用户刷帖时无需打开顶部推广页面。原站公开脚本[index-C2SOP9CS.js](https://static.max-c.com/static/heybox/webapp/@heybox-spa/website/app/index-C2SOP9CS.js):1中，中文nav-links的七个nav-link依次为首页、社区、小黑盒加速器、黑盒语音、黑盒工坊、开放平台、加入我们。登录区域使用独立nav-actions。`tools/heihei-goland/src/main/resources/web/theme.css:81`只隐藏该容器第3到7项，保留前两项与登录区域。独立范围复核见14_nav_review.md。

正常宽屏多图帖在鼠标移入图片时显示箭头；视口缩至460 CSS px及以下后，原站[index-ChemEL1Y.css](https://static.max-c.com/static/heybox/webapp/@heybox-spa/website/app/index-ChemEL1Y.css):1同时设置计数浮层`opacity:1`、两箭头`display:none`，导致用户仍看见1/3却失去鼠标箭头入口。原站JS保留两个按钮及翻页事件。`theme.css:132-139`仅在同断点恢复按钮display并取消边缘位移，沿用原站点击处理与首尾状态；宽屏hover行为保持原样。三个候选、反证、原文与独立静态复核见[13_carousel_analysis.md](13_carousel_analysis.md)。

README已同步。Java、网页JS、滚动条资源未改；与0.1.5包的静态资源比较输出只有：

```text
Changed resource: META-INF/plugin.xml
Changed resource: web/theme.css
```

## 打包与脱敏

沿用已授权的定向构建脚本，只编译本插件同一Java包两个源文件，读取现成IDE SDK，不编译依赖、不运行测试或新增测试资产。构建退出码0。最终包保持稳定ID及Platform 241（GoLand/IDEA 2024.1.x）兼容范围。

首次覆盖桌面heihe.zip时失败，系统原因原文：

```text
The process cannot access the file ... because it is being used by another process.
```

未终止或操作占用进程，改将同一构建包复制为桌面**heihe-0.1.6.zip**。最终扫描和副本核对输出：

```text
Payload: heihe 0.1.6 / 7 classes / 5 resources
Privacy patterns/debug attributes: 0; source equality/fixed metadata: OK
Desktop package: heihe-0.1.6.zip
Bytes: 21026
SHA256: 5FFFE40D60F5A1020D7DC0B256E307E4820F2F7CD65157645C8BBDB53D2358D9
2026-10-28 20:38:48 +08:00
```

扫描覆盖嵌套JAR、用户名、工作目录、绝对本地路径、凭证模式与调试属性；无命中。归档元数据固定，无额外字段/注释；五项资源与最终源码字节一致，桌面副本SHA256一致。发布包不含源码、工作区、日志或审计记录。

实际安装、窄窗口画面与点击翻图未实机验证。请在设置→插件→齿轮→从磁盘安装插件中选择新版文件，应用后重启IDE。未自动安装或重启；独立插件不涉及游戏模块，无相关游戏模块索引需更新。
