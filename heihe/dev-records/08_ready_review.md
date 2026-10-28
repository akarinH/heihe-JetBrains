---
phase: 08
agent: qa-reviewer
task: "heihei-goland"
depends_on: ["06_ready_design.md"]
---

# 0.1.2 就绪显示增量审查

结论：最终增量静态审查通过。初稿存在的同文档导航永久等待问题已修正；未发现其它已确认阻塞。没有运行编译、测试、浏览器回归或新增测试资产。本结论不等同于实际 JCEF 首帧视觉验收。

审查范围为当前两个状态卡片、Java 加载/导航/query 回调、theme.js 就绪协议、plugin.xml 和 README。路径缩写：`Panel` 为 `tools/heihei-goland/src/main/java/cn/local/heihei/HeiHeiBrowserPanel.java`，`JS` 为 `tools/heihei-goland/src/main/resources/web/theme.js`。

## 已修正问题：同文档历史导航不能等待新文档完成

具体场景：用户在已变暗的小黑盒 SPA 中从列表进入帖子，然后点击插件后退返回列表。初稿先把 browser 隐藏并递增代次，但浏览器可能只做 history state 切换；不会产生新的主文档 loadEnd，主题也不会重新注入，因此没有本轮 ready，用户会一直停留在深色“正在准备”卡片。

依据：初稿后退/前进入口在 goBack/goForward 前调用 beginLoading。CEF 官方 `cef_load_handler.h:76-79,91-94` 明确 fragments/history state 这类同页导航不会触发 OnLoadStart/OnLoadEnd：
https://github.com/chromiumembedded/cef/blob/master/include/cef_load_handler.h

最终 `Panel:139-150` 直接执行历史操作；`:167-169` 首页和 `:300-305` 本站 popup 也不再提前进入等待，覆盖仅移除/改变当前 fragment 的同类路径。真正新文档由 `:235-238` 的主 frame onLoadStart 隐藏；只有确定重载的刷新按钮在 `:157-160` 先隐藏。初次创建仍在 `:124-127` 预隐藏并显式创建浏览器。

这是 **06 规格缺陷** 的修正：原设计要求多个本地导航入口统一提前遮挡，未区分同文档导航。不能要求实现回到该错误规格。修订后的约束是“首次创建和刷新预遮挡，实际新文档统一由 onLoadStart 进入等待”。没有增加 popstate bridge、计时器或重试。

## 已核对关键链路

- **创建与资源释放**：`Panel:91` 使用本机 241 的 `JBCefJSQuery.create((JBCefBrowserBase) browser)`；query 构造器自动绑定默认 client 的证据已在 06，当前没有再次注册 query。`:105-108` 仍由 panel 管理 browser，`:124-127` 在隐藏卡片中调用 createImmediately 后才加载首页。
- **刷新显示**：`:157-160` 在 EDT 先切深色卡再 reload；`:336-339` 递增 navigationId。新文档主 frame start 再更新当前代次，旧页面脚本携带的旧信号不能直接显示新页面。
- **固定信号与最终守卫**：`:109-110` query 回调仅转交 EDT；`:350-353` 核对 disposed、当前站点和完全相等的 ready/error:navId。`:380-383` 只注入这两种固定状态，不提供 URL、文件、项目数据或任意命令入口。
- **主页面与外站**：`:258-267` 排除非主 frame 与非 HTTP(S) 的初始空页面；本站进入主题处理，外站主页面完成后直接显示，避免第三方登录页等待永远不会注入的本站 ready。第三方页面不接收本站样式。
- **失败出口**：`:272-280` 主页面真实加载错误进入深色错误卡，忽略正常 ERR_ABORTED；`:365-369` 主题资源缺失进入可诊断错误提示；JS 的异常进入 error 信号。错误卡保留工具栏，未通过失败直接显示未处理网页。
- **新建和重复主题**：`JS:19-25` 已有 style 分支既更新 CSS，也能立即通知或订阅本轮 ready；不会再无条件 update+return。`:98-105` 完成规则改写后设置 ready、通知既有订阅者和当前注入调用。
- **真正的就绪条件**：`JS:8-10` 强制计算根元素样式后发信号；`:39-40,98-99` 等 document complete 与已观察的 pending 样式结算。没有 RAF、字体等待、setTimeout、setInterval 或持续轮询。
- **样式失败/移除结算**：`JS:114-126` 的 load 与 error 都删除 pending；`:133` 移除节点也结算；`:136-138` 仅跟踪新观察的 stylesheet 请求，不将注入前可能已经失败的 null-sheet link 加入等待。后续 CSS 继续原有事件适配，未遍历帖子 DOM。
- **元数据和说明**：plugin.xml 保持 platform/241.*、稳定 id，展示名和 vendor 为 heihe，版本 0.1.2；README 标明 IDEA/GoLand 2024.1.x、heihe.zip、需要 JCEF 以及实际运行边界。发布物脱敏和归档字节检查由另行工作负责，不以本报告替代。

## 多视角对抗校验

| 视角 | 候选 | 结论 |
|---|---|---|
| 规格冲突 | 四个本地导航全部提前进入 loading | 确认规格遗漏同文档情况，已按上文修订与复核 |
| 隐含假设 | 隐藏 browser 不 create，或 RAF 不执行而一直等 | 明确 createImmediately；JS 不依赖 RAF，候选排除 |
| 隐含假设 | 已有 style 分支不发新 ready | 已改为已完成立即报告/未完成订阅事件，候选排除 |
| 隐含假设 | 旧失败 stylesheet 永远没有新 error | 仅跟踪观察到的新请求，且 load/error/remove 均结算；没有等待全部旧 null-sheet |
| 副作用 | 新 query 泄露数据或重复绑定释放父级 | 信号形状固定，只有显示动作，query 沿原 client 生命周期释放 |
| 副作用 | 旧 loadEnd/error 的迟到 Runnable 一定破坏新导航 | 未取得绕过 CEF 回调序列与 EDT 排队的确定触发证据；不把理论时序当 confirmed finding，不追加并发机制 |

## 关联风险与 sibling 核对

确认根因泛化为“没有保证生成新文档事件的操作，却先进入依赖 loadEnd 的等待”。对应检查 back、forward、home、本站 popup、refresh、首次创建及主 frame start：前三类可能同文档的入口已修；refresh、首次主页、实际 start 保留等待。

执行 `rg -n --glob '*.java' 'themeReadyQuery|onThemeSignal|beginLoading\(' tools`，命中仅当前面板。最终 beginLoading 调用仅 `Panel:124`（初次）、`:159`（刷新）、`:237`（真正主 frame start），没有遗漏同职责入口或另一插件实现需要同步。

## [规则冲突检查]

1. P0：本 reviewer 只读源码和官方回调契约，只写本报告；未编译、测试或创建测试矩阵，符合（10/10）。
2. 最小实现：同文档问题通过删错误的提前遮挡解决，没有加 popstate 状态、重试、锁、后台线程或持久化，符合（10/10）。
3. 用户需“颜色准备后显示”：新文档成功路径使用脚本实际完成通知，错误保留深色卡；没有用固定延时假冒完成，符合（9/10）。
4. 原业务边界：普通 HTTP(S) 登录导航、原生系统浏览器确认与原 Content/browser 生命周期不变，符合（9/10）。

## [隐含假设审计]

- 同文档历史不会调用 frame start/end：官方 CEF 契约已证实，已修对应前提（10/10）。
- query 生命周期与 241 方法：06 的本机原始描述符和构造器证据，当前使用一致（10/10）。
- CSS/JS 无语法或二进制引用问题：本 reviewer 只做静态源码检查，root 的后续定向编译/脚本解析结果另记，不宣称运行通过（8/10）。
- 首次 native 组件显示完全没有闪帧、全部原站异步页面均匹配：未运行，本报告只证明已读链路和明确出口（7/10，验证边界）。

## [副作用扫描]

- Java UI：CardLayout 只控制中间网页区域，工具栏和系统浏览器入口始终可用。
- 网站：只新增显示状态信号；样式范围、二维码/媒体保护规则、账户流程不扩展。
- 内存/CPU：一个 query、一个代次和局部 pending Set；无常驻轮询、全 DOM 观察或额外线程。pending 样式按事件/移除释放。
- 仓库：改动仍在当前插件，未触及游戏服务或用户已有 Diff；本 agent 只写指定报告。

## [置信度校准]

最终静态审查置信度 9/10。没有剩余已确认阻塞；late-load 的推测不升级成必须实现的新机制。实际首帧、登录和全部 SPA 页面依然需要运行证据，不能由静态通过替代。

## 修订记录（第五步回看）

| 原结论 | 新结论 | 影响 |
|---|---|---|
| 06 要求全部导航按钮先遮挡 | 仅刷新必定 reload 可预遮挡，其他按实际新文档事件 | 已修 back/forward，并扩查 home/popup 的 fragment 同类情况 |
| 初稿 query 使用 deprecated browser 重载 | 使用已验证的 JBCefBrowserBase 重载 | 不改变创建或生命周期语义 |

## 路过发现

探查范围为当前 Java/JS/元数据/README 和 CEF 回调文档；没有确认范围外缺陷。打包脚本固定时间改法与发布包脱敏由 root/对应审查负责，本轮未扩大检查。

## STATE_PROPOSAL

- T10：源码实现及本轮修正可标完成；同文档导航等待问题已修复并定点复审。
- 静态审查：通过，无待修阻塞；保留实际视觉/登录未运行验证边界。
- T12：由 root 继续授权范围内定向编译与 heihe.zip 桌面交付，不以本报告作为编译或归档结果。
