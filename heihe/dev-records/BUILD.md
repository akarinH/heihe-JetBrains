# 安装包交付记录

交付日期：2026-10-28，Asia/Singapore。

## 实际命令与输出

```powershell
& '.\tools\heihei-goland\build.ps1' -IdeHome 'D:\goland\GoLand 2024.1.6'
```

退出码 `0`，执行耗时约 `1.43s`。仅一个 Java 包的两个源文件；未编译 SDK 或游戏模块，未运行测试，未新增测试资产。

原始输出：

```text
Plugin package: D:\Z-game\git_servers\tools\heihei-goland\dist\heihei-goland-0.1.0.zip
Build output: D:\Z-game\git_servers\.tmp\build\heihei-goland\4a256d232f1c45eba1cff0d4e5100ee0
```

## 归档结构与桌面副本

通过 .NET ZipArchive 只读检查 ZIP 和其中的 JAR；必需描述文件、主题资源、图标、两个入口类齐全，5 个 class 的 magic 与 major version `61`（Java 17）均正确。该检查不加载或运行插件类。

原始检查输出：

```text
ZIP entry: heihei-goland/lib/heihei-goland.jar
JAR entry: cn/local/heihei/HeiHeiBrowserPanel.class
JAR entry: cn/local/heihei/HeiHeiBrowserPanel$1.class
JAR entry: cn/local/heihei/HeiHeiBrowserPanel$2.class
JAR entry: cn/local/heihei/HeiHeiBrowserPanel$3.class
JAR entry: cn/local/heihei/HeiHeiToolWindowFactory.class
JAR entry: icons/heihei.svg
JAR entry: META-INF/plugin.xml
JAR entry: web/theme.css
JAR entry: web/theme.js
Plugin identity: cn.local.heihei / 0.1.0 / GoLand 241.*
Desktop package: C:\Users\honghenghui\Desktop\heihei-goland-0.1.0.zip
Bytes: 14898
SHA256: CEC971EABE21C50077B6CBA1D51FCEB89F1641B1AC62070F9A6A5A35178D9AA6
```

桌面目标原先不存在，使用 File.Copy 的禁止覆盖模式复制；桌面副本与 dist 文件 SHA256 一致。

## 尚未执行

没有自动安装、重启或实际运行 GoLand 插件。原站登录、全部页面效果和浮动窗口实际显示需安装后确认；构建成功和归档结构正确不能代替运行效果验证。

## 维护入口

源码与使用说明：`tools/heihei-goland/README.md`。已检索现有模块索引，无对应 GoLand/JetBrains 插件模块；本任务为独立工具，维护入口保存在 README，无需修改游戏业务或框架索引。
