# heihe 0.1.5 交付记录

用户刷帖时，顶部发布入口占空间且无需使用，导航按钮缺少鼠标反馈，普通竖向滚动仍偏慢。本版隐藏该发布入口、增加导航悬停反馈，并把普通竖向滚轮从1.25倍调至2倍。

- `tools/heihei-goland/src/main/resources/web/theme.css:78`：仅隐藏 `.nav .publish-btn`，保留登录与社区导航。
- `tools/heihei-goland/src/main/java/cn/local/heihei/HeiHeiBrowserPanel.java:451`：四按钮采用同一ToolbarButton，先画主题圆角灰底再画按钮内容；禁用时不响应悬停，沿用原有遮罩绘制顺序。
- `tools/heihei-goland/src/main/resources/web/theme.js:114`：`wheelSpeed = 2`；输入、横向、缩放、嵌套滚动与已消费事件保护未改。

## 验证与交付

Node语法检查通过；仅对本插件同一Java包两个源文件进行定向编译，读取已编译IDE SDK，构建成功。独立定点审查见 [12_hover_review.md](12_hover_review.md)，无已确认阻塞。未运行测试、未新增测试资产。

最终安装包归档、资源、脱敏与桌面副本核对输出：

```text
Payload: heihe 0.1.5 / 7 classes / 5 resources
Privacy patterns/debug attributes: 0; source equality/fixed metadata: OK
Desktop package: heihe.zip
Bytes: 20776
SHA256: 775CB47E1FD82B93ADFFBB2CBA7E51693A5DE3E0B21EBB81F565566DE9FC0C18
2026-10-28 20:06:00 +08:00
```

扫描覆盖嵌套JAR、用户名、工作目录、绝对本地路径、凭证模式及调试属性；归档无机器路径、工作区或日志，时间戳固定、额外字段与归档注释为空。资源字节与最终源码一致，桌面副本哈希一致。包名heihe.zip，兼容范围保持IntelliJ Platform 241（GoLand/IDEA 2024.1.x）。

未自动安装、重启IDE或实机复核视觉/滚动手感。README已同步；独立IDE插件未涉及现有游戏模块索引。实际安装后可通过设置→插件→齿轮→从磁盘安装插件选择桌面heihe.zip。
