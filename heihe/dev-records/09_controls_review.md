---
phase: 09
agent: qa-reviewer
task: "heihei-goland"
depends_on: ["08_ready_review.md"]
---

# 0.1.3 控件、滚动与遮罩审查

结论：最终增量静态审查通过，无剩余已确认阻塞。移除功能、滚轮保护、遮罩分工及两处滚动条遮罩遗漏已核对并收敛。未编译、测试、启动 IDE 或新增测试资产；原生控件实际显示、滚动手感和视觉效果未运行确认。

路径简称：Panel 为 `tools/heihei-goland/src/main/java/cn/local/heihei/HeiHeiBrowserPanel.java`；JS 为同插件 `src/main/resources/web/theme.js`；scrollbars 为 `src/main/resources/web/scrollbars.css`。

## 241 SDK 与最小方案

只读本机 `lib/app-client.jar` 的 class 描述符：`JBCefBrowserBase.Properties` 仅发现公开 `NO_DEFAULT_AUTH_CREDENTIALS`、`NO_CONTEXT_MENU`；未发现 `MOUSE_WHEEL_SCROLL_FACTOR` 或已确认公开倍率入口。Base 有通用 setProperty(String,Object)，但这不能证明任意自造 key 会生效。按约定停止穷举，没有改 IDE 全局设置或新增 OSR。

Java 遮罩只覆盖 Swing 内容，DOM 遮罩负责浏览器内容；可见浏览器矩形始终从 Java 黑色绘制区域扣除，不以 native/OSR 类型区分，避免在 OSR 下重复叠加。100 时整体隐藏 pageArea，slider 在内容区外，因此恢复不依赖网页回调。

## 关键链路核对

- **功能删除**：源码检索 `BrowserUtil|ToolWindowType|pendingExternal|externalButton|floatingButton|systemBrowserButton` 无旧功能残留；Factory `:15` 改为无参 Panel。Panel 的站外 popup 只提示不支持，正常主窗口 HTTP(S) 登录导航仍保留。插件元数据为 0.1.3，README 不再引导使用已删除入口。
- **滚轮**：`JS:114-140` 采用 1.25 倍，先排除已处理/不可取消事件、Ctrl/Meta/Shift、横向手势，沿 composedPath 找可滚动祖先，处理 line/page/pixel 单位。`:137-138` 在已到边界且禁止串动的内层区域交还原生。新监听仅随主题首次建立，重复主题分支不会重复添加。
- **输入控件遗漏已修**：最初 `JS:121-122` 仅排除 input/select/contenteditable，原站若使用 textarea 编辑长回复，滚轮会被 preventDefault 后手工接管，与保留输入控件原生行为的实现边界不符。最终 selector 已补 `textarea`，不新增其它输入机制。
- **100 与恢复**：`Panel:200-219` 将 slider 放在 root SOUTH、contentArea 之外。`:209-215` 更新同一 shadeValue，100 时 pageArea.setVisible(false)，向左恢复 true 并 revalidate/repaint；没有调用 beginLoading 或重置 CardLayout，因此恢复的是当前 loading/error/browser 状态。
- **绘制排除**：`Panel:442-455` 先画子控件，再按相同透明度画黑色；browser.isShowing 时将 browser bounds 从 pageArea 转到 DimPanel 坐标并扣除。loading 卡当前显示时 browser 不 showing，加载提示同样被遮暗。isPaintingOrigin 防止子按钮重绘绕过遮罩。
- **跨页面值一致**：主 frame HTTP(S) loadEnd 在 `Panel:256` 先 applyShade，再执行本站主题或外站放行。滑块变更也调用同一入口。脚本设置当前 root 透明度变量，复用一个 fixed 黑 div，pointer-events:none，只改外观，不读取网页或本机数据。
- **共享滚动条资源**：`Panel:120-122` 只在构造时读取并预编码 scrollbars；`:347-351` 按唯一 id heihe-scrollbars 注入，已有时不重复写入；`.formatted(opacity, scrollbarCss, opacity)` 的占位顺序一致。`JS:87` 跳过此插件自有样式，theme.css 已移除滚动条块，避免两份声明。
- **打包与资源失败**：build.ps1 `:18` 白名单包含新正式 CSS；Panel `:376-378` 将缺失滚动条资源纳入本站主题错误卡。此 CSS 是功能发布资源，不是测试资产。最终 JAR/ZIP 字节与隐私扫描由 root 执行，不以本报告替代。

## 遮罩同根因修正

### 站外 viewport 滚动条

场景：用户把遮罩调到中间值，再从小黑盒进入允许的第三方登录页面。初稿只在本站 theme.css 中定义滚动条的黑色渐变；DOM fixed mask 不覆盖浏览器 viewport 的滚动条，Java 又排除了整个 browser 矩形，所以站外 viewport 滚动条仍会发亮。

最终 scrollbars.css 在所有 HTTP(S) 主文档由 applyShade 独立注入，并使用暗色 fallback，故不依赖小黑盒主题变量或再次注入本站业务样式。100 的 pageArea 整体隐藏路径始终不受该问题影响。

### 页面内部滚动条重复变暗

场景：用户在帖子内的可滚动内容区阅读，将遮罩调到 50%。初稿全局 scrollbar 渐变先遮一次，而页面内部滚动条又属于 DOM fixed mask 覆盖的像素区域，结果会达到约 75% 的黑色叠加；只有 viewport 滚动条才需要单独渐变。

最终 scrollbars `:5,8,14` 的全局 track/corner、thumb、hover 仅提供平面底色，14px 槽宽和透明边框 5px→2px 保持可见滑块 4px→10px；两处 background-clip:padding-box!important 保留。viewport 单独规则从 `scrollbars.css:21` 起：

```css
:root::-webkit-scrollbar-track,
:root::-webkit-scrollbar-thumb,
:root::-webkit-scrollbar-corner {
  background-image: linear-gradient(var(--hh-dim-color), var(--hh-dim-color)) !important;
}
```

它只设置 image，不重置 hover 底色或 clip；内部滚动条仅受 DOM mask 一次。右下角 corner 有明确暗色背景，不依赖默认白色绘制。

## 关联风险与同类入口

本轮确认模式是“同一个视觉区域漏遮或被两层重复遮”。已检查：toolbar/status/loading（Java）、browser 内容（DOM）、viewport 滚动条（独立 gradient）、内部滚动条（DOM）、100 隐藏和恢复（Java 可见性），以及本站/站外 loadEnd。确认同款已在本轮修复；没有引入 OSR、截图或原生透明窗口。

输入控件 sibling 检查覆盖 input/select/textarea/contenteditable，缩放与横向手势保留原生路径。没有发现需要另改的范围外代码。

## [规则冲突检查]

1. P0 禁止本 agent 编译/测试/启动 IDE/测试资产：本轮仅只读 SDK、源码和指定报告写入，符合（10/10）。
2. 用户要求删浮动/浏览器功能：删除原生按钮、调用、字段与外站打开入口；不把 IDE 自有工具窗口能力当成本插件新增功能，符合（10/10）。
3. 用户要求全内容遮罩但可恢复：控制条在内容遮罩外，100 直接隐藏 browser 区域且不重建 ready 状态，符合（9/10）。
4. 最小设计与现有 heavyweight：使用原有 JCEF，不改 OSR 或窗口架构；滚动条仅一个正式资源，符合（9/10）。

## [隐含假设审计]

- 当前 241 有可用原生倍率属性：没有发现可证实入口，未用通用 setter 猜测；实际采用明确的局部 JS 路径（9/10）。
- browser bounds 的坐标父级是 pageArea：构造直接 pageArea.add(browser.getComponent())，与 convertRectangle 来源一致（10/10）。
- 100 时不改变当前卡：代码只改 pageArea 可见性；ready 回调仍维护原 CardLayout，已静态确认（9/10）。
- 实际平台绘制、手感、hover视觉满足用户体验：未运行，保留交付边界，不宣称实机通过（7/10）。

## [副作用扫描]

- 网页滚动：仅常规可取消竖向滚轮被接管；特定输入、缩放、横向与禁止串动边界有原生出口。
- 网页样式：本站主题仍限原域；遮罩和 viewport 滚动条作为用户要求的全内容外观应用于正常 HTTP(S) 页，不访问账户数据。
- Java UI：一个 slider 值与既有内容区绘制；无后台线程、轮询、重试或持久化。
- 代码/包：新增正式 scrollbars.css 进入白名单，其余产物私有路径检查由 root 交付环节负责。

## [置信度校准]

最终源码增量静态对应置信度 9/10。没有剩余已确认阻塞；不为未取得运行证据的边缘样式追加架构。实际渲染、滚轮手感和安装效果仍未运行复核。

## 修订记录（第五步回看）

| 初稿 | 修订 | 原因 |
|---|---|---|
| 输入控件排除缺少 textarea | 补入同一 selector | 多行编辑区不应被页面加速器接管 |
| 滚动条遮罩只在本站 theme.css | 单份 scrollbars.css 由每个 HTTP(S) 页 applyShade 注入 | 站外 viewport 也属于全内容遮罩 |
| 全局 scrollbar gradient | gradient 仅 viewport，内部使用平面底色 | 避免内部条被 CSS 与 DOM mask 遮两次 |

## 路过发现

范围为当前插件 SDK 查询和功能增量；未发现需要扩展到游戏仓库、其它插件或平台配置的缺陷。本 agent 没有修改代码或六个共享账本。

## STATE_PROPOSAL

- T14/T15/T16：最终源码增量完成，静态审查通过；已处理 textarea、站外滚动条与内层重复遮罩。
- T17：本报告对应代码静态子项完成，发布包隐私/哈希由 root 补充。
- T18：root 按既有单包授权重新打包 0.1.3 heihe.zip；本 agent 无编译或运行结果。
