# heihe 0.1.2 交付记录

## 用户目标与结果

用户在暗色工具窗口点击刷新时，旧实现先显示原站浅色页面，再异步处理颜色。本版先切到深色加载卡，文档与已观察到的样式完成处理并强制计算当前颜色后，由固定 ready 信号显示网页。Java 在最终 EDT 显示点检查当前导航代次、本站地址和销毁状态；旧信号不会显示新导航。失败保持深色错误提示，原生刷新/首页/浏览器按钮仍可操作。

首版草案对所有历史导航预先隐藏，会在 SPA 同文档切换时等不到新文档事件。已按 CEF 契约修正：初次、刷新和实际主文档 onLoadStart 进入加载卡；前后退、首页和 popup 不对可能的同文档导航额外建立等待。当前有效方案与审查见 06_ready_design.md、08_ready_review.md。

插件展示名称为 `heihe`，版本 `0.1.2`，发布文件 `heihe.zip`，归档路径 `heihe/lib/heihe.jar`。稳定 ID 保留用于覆盖升级。当前声明兼容 IntelliJ IDEA / GoLand 2024.1.x（241），其他版本未扩大兼容声明。

## 构建范围与原始产物

沿用同一插件单 Java 包两个源文件的定向构建，SDK 参数仅在本机执行时传入；命令记录将机器目录替换为占位符：

```powershell
& .\tools\heihei-goland\build.ps1 -IdeHome '<IDE_HOME>'
```

第一次执行 Java 编译成功，但归档在写完条目后设置时间触发 `.NET Create mode` 限制；已改为 CreateEntry → 先设固定时间 → 写入内容。第二次构建退出码 0，无编译告警。只执行该单包构建，不运行测试或其它模块构建，未创建测试资产。

资源逐字核对和桌面副本原始输出：

```text
Archive identity/resources/source equality: OK
Desktop package: heihe.zip
Bytes: 16934
SHA256: 3753BBA9687BA76E5F34F36591C2113F148055758484834AA8C684FBBDBA0712
2026-10-28 19:07:40 +08:00
```

## 脱敏依据

独立审计报告 `07_privacy_review.md` 的 0.1.2 最终复核逐项读取当前 ZIP/JAR，并解析全部 class UTF-8 常量池。原始产物身份：

```text
ZIP SHA256=3753BBA9687BA76E5F34F36591C2113F148055758484834AA8C684FBBDBA0712 bytes=16934
PLUGIN id=cn.local.heihei name=heihe version=0.1.2 since=241 until=241.* dependency=com.intellij.modules.platform
CONTENT classes=5 resources=4
```

五份 class 的四类调试属性计数均为 0。全部 class 常量和四项资源未命中机器身份、真实绝对路径、仓库名、邮箱、凭据赋值或本地 URL；公开网站 URL 与插件内部资源路径不算隐私。

两层归档全部条目的原始元数据计数：

```text
relative=True fixed2000=True centralExtra=0 localExtra=0 comment=0
ZIP entries=1 archiveComment=0
JAR entries=9 archiveComment=0
```

源文件中的机器 SDK 默认目录及文档示例已改为显式参数和通用占位符。发布只允许类文件和四项资源，源码/文档/日志/配置/工作区不入包。主题回传仅接受固定 ready/error 加当前代次，不返回文件或项目数据。隐私结论限定为本次源码与发布包静态审计，不泛化为第三方网站的数据行为承诺。

## 交付边界与维护

桌面新包已复制并核对哈希，与独立审计对象相同。没有自动安装或重启 IDE；用户从磁盘导入后重启可创建新版浏览器窗口。脚本语法解析、独立静态审查、编译、归档和隐私扫描已完成；实际首帧视觉与 IDEA 实机运行尚未验证，不以静态检查替代。

维护说明已同步 README。已检查相关知识库，独立 IDE 工具无对应现有游戏模块索引需要更新。
