# 0.1.5 发布入口、悬停反馈与滚轮审查

结论：本轮定点源码与当前包轻量核对通过，无已确认阻塞。未运行编译、测试、IDE 或视觉回归，未新增测试资产。

## 源码依据

Panel 指 `tools/heihei-goland/src/main/java/cn/local/heihei/HeiHeiBrowserPanel.java`。

- **隐藏范围准确**：`web/theme.css:78` 仅 `.nav .publish-btn { display: none !important; }`。root 提供的原站原文为 class `publish-btn`、文本“发布内容”，登录入口使用另一个 `login-btn` class；没有扩大到导航所有按钮或登录区域。README 同步说明只隐藏发布入口。
- **四按钮统一**：`Panel:79,81,166,176` 均使用 ToolbarButton。`:451-454` 保持非 opaque 并启用 rollover；原工具栏 `:190-194` 继续关闭默认边框和 content-area 填充。
- **底色与 super 顺序**：`Panel:459-472` 先以独立 Graphics2D 画灰色圆角背景，再调用 super 绘制按钮内容。非 opaque 与 contentAreaFilled(false) 保留，避免标准按钮不透明底色清掉手绘背景；graphics 副本在 finally 释放。
- **禁用与主题**：`:460` 同时要求 enabled 与 rollover，禁用的历史按钮不产生自定义 hover。`:463-467` 优先 ActionButton.hoverBackground，缺失则当前 PanelBackground.darker，圆角按 JBUI.scale 缩放；每次绘制读取当前颜色，不缓存旧主题。
- **遮罩仍在最上层**：`Panel:485-486` 的 isPaintingOrigin 仍返回 true；既有 DimPanel 先画子控件再叠加遮罩的顺序未改，按钮悬停重绘不会作为新机制绕过当前灰色遮罩。
- **滚轮只改倍率**：`web/theme.js:114` 为 2，`:118-122` 仍排除已消费/不可取消事件、修饰键、横向、输入/select/textarea/contenteditable；`:128-138` 的单位转换、实际可滚动祖先与 overscroll 边界未改变。README 的“约 2 倍”与实现相符。

用户场景对应：阅读帖子时顶部发布入口消失，登录入口保留；悬停可用导航按钮出现主题灰色圆角底，禁用按钮不误导为可用；普通竖向滚轮加速，输入及其它受保护手势仍走原有出口。以上为静态路径结论，实际 hover 观感和滚动手感尚未实测。

## 发布包原始结果

只读当前 `tools/heihei-goland/dist/heihe.zip`：

```text
SHA256 775CB47E1FD82B93ADFFBB2CBA7E51693A5DE3E0B21EBB81F565566DE9FC0C18
BYTES 20776
OUTER heihe/lib/heihe.jar
CLASSES 7
RESOURCES 5 icons/heihei.svg,META-INF/plugin.xml,web/scrollbars.css,web/theme.css,web/theme.js
PLUGIN cn.local.heihei name=heihe version=0.1.5
```

与 root 回传的哈希和数量一致。完整隐私/ZIP 元数据扫描及桌面副本由 root 负责，不把本轻量检查扩大为这些项目已经独立验证。

## [规则冲突检查]

1. P0：仅两次定点只读与本报告写入，未运行测试、构建或创建测试资产（10/10）。
2. 用户范围：只处理发布入口、四按钮 hover 和追加滚轮倍率；没有重做导航或账户流程（10/10）。
3. 码风与资源：复用既有按钮状态/监听，只增加局部绘制类；未引入缓存、线程或持久化（9/10）。

## [隐含假设审计]

发布选择器与 root 已取得原站 class 对应；Java 标准非 opaque/contentAreaFilled 配置、enabled/rollover 守卫与绘制顺序已读（9/10）。当前 IDE 的实际 UI delegate 绘制和用户观感未运行验证，不声称截图验收通过（7/10，明确边界）。

## [副作用扫描]

四个按钮使用同一实现，禁用状态、tooltip、点击行为和遮罩父级不变；CSS 不匹配 login-btn；滚轮既有例外路径未改。无游戏模块或其它插件影响。

## [置信度校准]

本轮源码增量与包身份核对置信度 9/10，无剩余已确认问题；不为未验证视觉差异追加新机制。

## 修订、关联风险与路过发现

本轮没有需要推翻方案的结论。定点覆盖四个构造点、统一绘制类、默认填充禁用及遮罩父级，未发现同职责遗漏或范围外缺陷，无需扩大检索。

## STATE_PROPOSAL

T21/T22 对应源码及 0.1.5 静态审查完成。T23 可在 root 完整产物与桌面检查后交付；本报告保留未运行视觉验证边界。
