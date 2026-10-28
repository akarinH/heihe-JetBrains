---
phase: 10
agent: plugin-developer-controls-privacy
task: "heihei-goland"
depends_on: ["07_privacy_review.md"]
---

# 0.1.3 滚动条方案与最终发布隐私复核

## 结论与审计边界

最终 `tools/heihei-goland/dist/heihe.zip` 的静态隐私审计通过；本报告结论绑定以下实际读取的包：

```text
bytes=19542
SHA256=965F9E29BD6C47FE469CEDC712B8AF16F86DBC85CFCE62F52432E2B507D57ABA
outer=heihe/lib/heihe.jar
PLUGIN cn.local.heihei heihe 0.1.3 241-241.* com.intellij.modules.platform
```

未发现待修复的隐私问题。滚动条保持固定 14px 槽宽、滑块通过透明边框切换可见宽度，交角明确使用暗色；只有根视口滚动条额外应用遮罩渐变，内部滚动条由现有网页遮罩覆盖一次。

本 agent 只读源码、官方文档和归档，并写入本报告；未编译、测试、运行浏览器脚本或新增测试资产，未修改生产文件或六个共享状态文件。实际拖动、滚轮手感与所有页面的视觉效果不属于本报告的已运行结论。

## CSS 方案与实际落点

用户在右侧窄窗口看帖子时，希望默认滚动条较细，鼠标移入后容易拖动，而且页面宽度不随悬停变化。固定槽宽 14px，滑块普通状态使用两侧 5px 透明边框，得到 4px 可见宽度；悬停边框改为 2px，得到 10px。这里只改变槽内可见滑块，不动态改滚动条宽高。最终发布资源 `web/scrollbars.css:2`、`:7-16` 与该方案一致。

关键是 `background-clip: padding-box` 必须保留在 background 简写之后，否则背景会绘到透明边框下，无法获得预期的细条。最终普通与悬停规则均显式声明该属性，分别位于 `scrollbars.css:9`、`:15`。MDN 明确 padding-box 不在边框下绘制背景，支持这一实现判断。[MDN background-clip](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/Properties/background-clip)

现有基线 `theme.css:130-133` 只覆盖 track/thumb，没有交角规则；这与交角沿用平台外观的候选原因一致，但“默认一定是白色”不是 CSS 规范保证，不能只凭缺规则就断言所有环境都会出现同一种白块。最终 `scrollbars.css:3-5` 显式覆盖 track/corner，处理范围明确。

Chrome 文档说明，设置 `::-webkit-scrollbar` 宽高会使 overlay 滚动条常驻，效果接近使用独立槽位的 classic 滚动条。本期本来就希望固定可拖动区域，因此采用现有 Chromium 专用样式即可；没有为此新增通用布局系统、JS 改宽或全站 `scrollbar-gutter` 规则。[Chrome Scrollbar styling](https://developer.chrome.com/docs/css-ui/scrollbar-styling)

## 修订记录：内部滚动条避免重复压暗

| 修订 | 原方案 | 最终方案 | 原因与受影响范围 |
|---|---|---|---|
| R1 | 所有滚动条的背景都叠加当前遮罩渐变 | 所有滚动条使用平面暗色底；只对 `:root` 的 track/thumb/corner 加渐变 | 内部滚动条已经位于网页 DOM 遮罩下；额外渐变会重复压暗。只改 `web/scrollbars.css`，其余发布条目字节不变。 |

具体场景：用户在带内部滚动区域的帖子页面，把遮罩设为 50%。预期内容和滚动条都按同一强度变暗；若内部滚动条先被 CSS 渐变压暗一次、再被整页 DOM 黑层压暗一次，其等效黑色覆盖会成为 75%，比附近内容更暗。最终 `scrollbars.css:3-16` 为平面底色，`:20-24` 仅给根视口补渐变，避免该重复作用。网页遮罩落点为 `HeiHeiBrowserPanel.java:339` 的 `applyShade`：固定全屏、忽略指针事件的黑层。

本 agent 初始建议没有区分内部滚动条与视口滚动条，root 的独立复核指出该问题后已修正。没有把初始全局渐变建议当作最终正确方案。

## 新控制的数据边界

`HeiHeiBrowserPanel.java:339` 的 `applyShade` 本轮完整读取：只把 UI 当前遮罩数值转换成 alpha，与静态滚动条 CSS 的 Base64 文本一起注入页面。脚本设置 CSS 变量、添加 style/黑层并调整 opacity；没有查询本机文件、项目、账户或环境，也没有发送网络请求。该方法允许正常 HTTP(S) 页面使用同一视觉遮罩，未通过回传接口暴露新的本机能力。

`theme.js:117` 的滚轮处理只读取事件修饰键、滚动方向、祖先元素的滚动范围和样式，再在能滚动的目标上调用 `scrollBy`。读取依据包括：

```text
114: const wheelSpeed = 1.25;
118: if (event.defaultPrevented || !event.cancelable || event.ctrlKey || event.metaKey || event.shiftKey ||
119:     event.deltaX !== 0 || event.deltaY === 0) return;
125: if (!(node instanceof HTMLElement) || node.scrollHeight <= node.clientHeight) continue;
127: if (node !== rootScroller && !/auto|scroll|overlay/.test(style.overflowY)) continue;
134: node.scrollBy({ top: event.deltaY * unit * wheelSpeed, behavior: 'auto' });
```

没有新增网络数据发送。滚轮行为的完整功能评审由该子任务另行负责，本报告不以这些静态片段替代嵌套编辑框和触控板的运行验证。

共扫描 9 个可分享文件（两个 Java、五项资源、build.ps1、README）。真实绝对路径、本地地址、邮箱、当前机器用户名、仓库名和显式凭据赋值均无命中；源码内未命中新增网络发送/存储 API。`BrowserUtil` 与 `ToolWindowType` 也无命中，与删除系统浏览器及浮动入口的范围一致。公开小黑盒地址、通用类包名和插件资源路径不计为个人信息。

## 发布包完整内容与常量池

外层只有 `heihe/lib/heihe.jar`。JAR 有六个 class：

```text
cn/local/heihei/HeiHeiBrowserPanel.class
cn/local/heihei/HeiHeiBrowserPanel$1.class
cn/local/heihei/HeiHeiBrowserPanel$2.class
cn/local/heihei/HeiHeiBrowserPanel$3.class
cn/local/heihei/HeiHeiBrowserPanel$DimPanel.class
cn/local/heihei/HeiHeiToolWindowFactory.class
```

五项资源为 `icons/heihei.svg`、`META-INF/plugin.xml`、`web/scrollbars.css`、`web/theme.css`、`web/theme.js`。没有源码、构建脚本、README、工作区记录、日志、个人配置或 SDK 文件。

本轮第一次 0.1.3 包审计逐个解析 class 常量池，六类的 UTF-8 常量计数分别为 354、101、33、40、64、27。六类的 `SourceFile`、`LineNumberTable`、`LocalVariableTable`、`SourceDebugExtension` 四项调试属性均不存在，敏感模式均无命中。

之后 root 只修正滚动条重复遮罩并重建。最终包对已完整审计包逐条内容哈希比较，原始结果：

```text
CHANGED web/scrollbars.css
COMPARE identical=10 changed=1 total=11
```

因此六个 class 和其余四项资源与已完整扫描内容字节相同，调试属性/常量池结论仍成立；唯一变化的完整 CSS 已重新读取，所有最终条目又做了敏感模式扫描，未发现命中。最终结论没有沿用上一包的整体哈希。

## 最终归档元数据

最终包逐条核对中央目录和本地文件头，原始汇总：

```text
ZIP entries=1 badEntryMeta=0 archiveComment=0
JAR entries=11 badEntryMeta=0 archiveComment=0
```

`badEntryMeta` 同时检查：条目无根路径/盘符/`..`，中央目录及本地头时间均为固定 2000-01-01 00:00:00，两处 extra 长度与条目 comment 长度均为零。两层 archive comment 也为零。因此未发现通过本地路径、构建修改时间、注释或额外字段携带机器信息。

## [规则冲突检查]

1. P0 与委派禁止本 agent 编译、测试、生产写入和测试资产：只有只读核对与本报告，符合（10/10）。
2. 隐私要求不得复述个人值：扫描通过当前环境生成匹配模式，只输出类型和条目；报告不复制个人标识或机器绝对路径，符合（10/10）。
3. P1 要求问题落到业务场景：重复压暗以 50% 滑块和内部滚动区域说明可观察结果；平台默认白色保留为候选而非规范事实，符合（10/10）。
4. 五次只读预算之外的最终包差异核对由 root 明确追加授权一次；该核对只读既有两个包，没有扩大生产写入范围，符合（10/10）。

## [隐含假设审计]

- 最终对象为 0.1.3：实际元数据、大小、SHA256 已核实（10/10）。
- 新包仅 CSS 改动：11 个条目逐一内容哈希比较，确认只有 scrollbars.css 不同（10/10）。
- UI 注入不包含本机数据：完整读取 applyShade，全部可分享文件和包常量/资源模式扫描无命中（9/10）。
- 滚动条视觉与滚轮手感：未运行，不作为已通过事实；静态方案与官方 CSS 语义一致（8/10）。

## [副作用扫描]

只读当前插件目录、官方文档与 root 指定的旧包；归档全部在内存中打开，没有解压落盘、加载 class、执行 JS 或修改文件元数据。未进入游戏模块，报告不在发布白名单内。

## [置信度校准]

隐私和归档结论为 9–10/10，CSS 静态方案为 8–9/10。没有发现需要改变用户已确认方案的低置信度决策；实际界面验证仍由 root 与用户按交付边界确认。

## 路过发现与关联风险

探查范围内未见其他隐私问题。重复遮罩模式已按视口/内部两类检查并收敛到 root 选择器；同类 track、thumb、corner 均纳入最终规则。不存在需要修改其他工具或游戏模块的同款入口，不增加全仓审计或永久规则。

## [STATE_PROPOSAL]

- 0.1.3 最终隐私审计完成，结论绑定 `965F9E29BD6C47FE469CEDC712B8AF16F86DBC85CFCE62F52432E2B507D57ABA`。
- 当前包为六个 class、五项白名单资源，稳定 ID 和平台兼容范围保持；无调试属性、敏感模式、真实时间或归档额外字段。
- 初始全局滚动条渐变方案已被根视口限定方案取代，最终包仅该 CSS 与先审包不同；无需保留旧包结论为最终结果。
