# 0.1.4 灰色遮罩定点审查

结论：源码增量与当前发布包轻量核对通过，无已确认阻塞。未执行编译、测试、IDE 启动或视觉回归，未新增测试资产。

## 源码依据

以下 Panel 指 `tools/heihei-goland/src/main/java/cn/local/heihei/HeiHeiBrowserPanel.java`。

- **五参数顺序正确**：`Panel:353,357,368-371` 的五个占位符依次接收 `shadeRgb`、`opacity`、`scrollbarCss`、同一 shadeColor 的 hex、`opacity`；分别服务于 RGBA 滚动条变量、样式资源、DOM mask 底色与透明度，没有参数错位。
- **三处同色**：`Panel:346-347` 从 `UIUtil.getPanelBackground()` 生成网页 RGB，`:368-371` 用相同 Color 生成 mask 背景；`:470-472` 的 Java 绘制同样取当前面板色。`scrollbars.css:24` 消费该 `--hh-dim-color`，没有独立黑色遮罩常量。`theme.css:10` 默认 transparent。
- **主题变化**：`Panel:129-134` 在 EDT 中检查 disposed，再依次更新网页遮罩、主题和原生内容重绘，避免面板换色后仅一侧保留旧颜色。
- **0/100 边界**：比例仍由 `shadeValue / MAX_SHADE` 计算；0 对应透明度 0，100 在 `Panel:220-223` 隐藏 pageArea，并由原生 alpha=1 的面板色覆盖内容。控制条在内容外的既有结构未改，向左恢复可见性和重绘，不新增 ready 状态。

具体效果对应用户场景：把滑块拉到最右时，应显示当前 GoLand/IDEA 面板灰而非纯黑；切换 IDE 主题后，网页、viewport 滚动条和原生控件遮罩一起使用新面板色。以上为代码路径结论，实际显示未运行确认。

## 当前包原始结果

只读核对 `tools/heihei-goland/dist/heihe.zip`：

```text
SHA256 A2E9B84849434D1DA4387D079C50226357EAEEB43B85EA03BB9CEE7C3FCB6E89
OUTER heihe/lib/heihe.jar
CLASSES 6
RESOURCES 5 icons/heihei.svg,META-INF/plugin.xml,web/scrollbars.css,web/theme.css,web/theme.js
PLUGIN cn.local.heihei name=heihe version=0.1.4
```

哈希与 root 回传一致。完整资源相等、敏感模式、ZIP 元数据和桌面副本检查由 root 负责，本报告不冒充这些结果。

## [规则冲突检查]

1. P0：只读源码/归档并写指定报告，无编译、测试或测试资产（10/10）。
2. 仅改变遮罩颜色：没有扩展导航、滚轮、窗口或 ready 流程（10/10）。
3. 预算与范围：两次定点只读、一次报告写入；没有重做设计或全仓扫描（10/10）。

## [隐含假设审计]

面板颜色由当前 UIUtil 提供、五参数与三处消费者一致，均有上述源码依据（10/10）。平台实际绘制与主观灰度效果未运行，不当作视觉验收通过（7/10，明确边界）。

## [副作用扫描]

同色转换覆盖网页、viewport 滚动条及 Java 绘制；0/100 可见性、浏览区域排除和控制条位置未改变。没有其它业务模块或持久化影响。

## [置信度校准]

本次极小源码改动与包身份核对置信度 9/10，无剩余已确认问题。没有新增需要用户裁决的机制。

## 修订、关联风险与路过发现

无推翻前轮流程的结论；同职责三处颜色消费者已经定点核对。探查范围仅灰色增量与当前 ZIP 身份，未发现范围外缺陷，无需扩大 sibling 扫描。

## STATE_PROPOSAL

0.1.4 灰色遮罩静态审查与轻量包核对完成。root 可在其完整产物/桌面检查后交付；实际视觉仍未运行验证。
