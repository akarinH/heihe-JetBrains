# heihe 0.1.4 灰色遮罩

遮罩颜色统一来自当前 IDE 的 `UIUtil.getPanelBackground()`。网页 RGBA、网页实色和原生绘制共用此来源（Panel:346-347、:368-371、:471），根视口滚动条沿用同一变量。主题改变时在 EDT 更新遮罩、主题和原生重绘（Panel:129）。0 保持透明，100 仍隐藏网页区域并完全覆盖其余内容，外侧滑块可恢复；不修改其它交互。

独立定点审查通过，见 `11_gray_review.md:3`。内嵌 JS 静态语法与五个参数顺序检查通过；同一插件单包两个源文件编译成功，无测试或新增测试资产。

最终包逐项检查资源与源码相同，敏感模式与调试属性未发现命中；两层归档固定时间、无额外字段和注释，条目为相对路径。桌面副本与已审包相同。原始输出：

```text
Payload: heihe 0.1.4 / 6 classes / 5 resources
Privacy patterns/debug attributes: 0; source equality/fixed metadata: OK
Desktop package: heihe.zip
Bytes: 19651
SHA256: A2E9B84849434D1DA4387D079C50226357EAEEB43B85EA03BB9CEE7C3FCB6E89
2026-10-28 19:54:49 +08:00
```

未自动安装/重启，也未进行实际视觉复核。README 已同步；独立插件无对应游戏模块索引需要更新。
