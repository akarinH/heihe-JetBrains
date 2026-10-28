# 桌面 heihe 独立项目

全部插件开发资产已从游戏仓库迁入此项目。以后以桌面 heihe 为开发根，无需进入游戏仓库。

| 原位置 | 新位置 |
|---|---|
| tools/heihei-goland/ | 项目根（src、build.ps1、README、dist） |
| _workspace/heihei-goland/ | dev-records/ |
| .tmp/build/heihe/ | .tmp/build/heihe/ |
| .tmp/build/heihei-goland/ | .tmp/build/heihei-goland/ |

迁移共149文件，全部复制后逐文件检查大小和SHA256相等，再移除原四目录；未操作其它仓库文件、Git引用或Go缓存。dev-records内早期路径保留为历史事实，最新位置以本文为准。

构建脚本已移除游戏仓库根推导，构建产物仅写本项目.tmp/build/heihe，安装包写dist/heihe.zip。在项目根使用：

```powershell
& .\build.ps1 -IdeHome '<IDE_HOME>'
```

IDE_HOME由使用者明确传入。脚本保持只编译本插件单包两个Java源文件、只读现成SDK，不运行测试。

本次仅做目录迁移和必要路径调整，未执行编译或测试。PowerShell静态解析通过，必需源码和资源存在，旧四目录为0；详细原始输出见EVIDENCE.md的E030/E031。原0.1.6安装包SHA256保持5FFFE40D60F5A1020D7DC0B256E307E4820F2F7CD65157645C8BBDB53D2358D9。

README和忽略规则已同步。此插件已独立于游戏仓库，无游戏模块索引需修改。
