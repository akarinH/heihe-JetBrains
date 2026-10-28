---
phase: 01
agent: architect
task: "heihei-goland"
depends_on: []
---

# GoLand 小黑盒浏览插件最小设计

## 结论与业务流程

用户在 GoLand 打开“小黑盒”工具窗口，浏览官方社区列表并进入帖子，使用原站登录流程；IDE 自带工具窗口管理提供停靠和浮动。插件只负责嵌入网页、少量导航和本站暗色 CSS。首次打开才创建一个 JCEF 浏览器；关闭项目或释放内容时销毁浏览器。无游戏 Actor、帧循环、Proto、服务端或 codegen 影响。

已确认本机是 GoLand 2024.1.6 / 241.19072.18，依据 `D:/goland/GoLand 2024.1.6/product-info.json:3-4`。核心 API 已从本机 SDK class 文件的公开方法描述符核对。没有执行 javac、Gradle、Go、测试或安装，也没有创建编译缓存和测试资产；运行效果仍未验证。

## 可判定实施规格

1. 新代码仅落在 `tools/heihei-goland/`；不修改现有 Go 源码与用户 Diff。
2. `plugin.xml` 声明一个 `com.intellij.toolWindow`，使用工厂懒加载，默认右侧停靠。浮动/停靠通过 IDE 原生工具窗口模式；如添加快捷按钮，使用 `ToolWindow.setType(ToolWindowType.FLOATING/DOCKED, null)`，不另造 JFrame。
3. 工厂实现 `ToolWindowFactory`，浏览不依赖索引，宜同时实现 `DumbAware`。检查 `JBCefApp.isSupported()` 后再构造浏览器；不支持时显示简洁原因与官方页面入口，不尝试下载浏览内核或改 IDE 参数。
4. 一个 `BrowserPanel` 拥有一个默认 `JBCefBrowser`。先注册 handler，再 `loadURL("https://www.xiaoheihe.cn/app/bbs/home")`，避免首屏加载先于样式 handler。
5. `Content.setDisposer(panel)`，`Disposer.register(panel, browser)`；使用默认隐式 client，不创建自有 client、项目服务或持久化层。工厂扩展本身并非自动释放的 Disposable，不把它当生命周期 owner。
6. `CefLoadHandlerAdapter.onLoadEnd(CefBrowser,CefFrame,int)` 内仅当 `frame.isMain()` 且 URL 的协议和 host 精确匹配小黑盒原站时注入本地 CSS；可再加 JS 的 `location.origin` 守卫抵御异步导航后的误注入。以固定 style id 更新，重复加载不得重复累积 style。不接入 `JBCefJSQuery`，网页无需访问 Java、本地文件或项目数据。
7. 样式由 root 按已抓取的原站变量与布局实现。仅修改展示，不改登录表单、验证码、请求、cookie 和账户状态。不保证未经运行确认的登录或重启后会话保留。
8. 工具栏限首页、前进、后退、刷新和外部浏览器入口等直接浏览操作。`CefBrowser` 已确认具备 goBack/goForward/reload；不增加自动刷新、轮询、重试、抓取、缓存或离线内容。
9. 若需要接管新窗口链接，241 的 `onBeforePopup` 是 **4 参数**（见 API 证据），不能照抄新 JCEF 的扩展签名。只处理用户点击产生的网页导航；不擅自启动网站自定义协议。登录弹窗的行为必须在实际登录前确认，不把其正确性假定为已验证。
10. 元数据面向当前 IDE，兼容范围 `since-build="241" until-build="241.*"`，使用 `com.intellij.modules.platform`。**不得添加 `com.intellij.modules.jcef` 依赖**：官方文档明确此 alias 从 2025.3.1 才提供。
11. Swing 状态变更从 JCEF 回调转到 EDT；涉及 ToolWindow 的延迟操作使用 `ToolWindowManager.invokeLater`。页面加载和脚本 API 不阻塞 EDT 等待结果；无自行创建线程、锁或后台 worker。

## 调用链

`plugin.xml toolWindow` → 用户点击侧栏 → `ToolWindowFactory.createToolWindowContent(project, toolWindow)` → 检查 JCEF 支持 → 创建 panel/browser 并绑定 disposer → 注册 load handler → `browser.loadURL(HOME)` → 主文档完成且原站 origin 命中 → 执行幂等 CSS 注入 → 用户按原站操作浏览/登录。

项目/内容释放 → Content 的 disposer → panel 子资源 browser → 默认隐式 client；隐藏工具窗口不是销毁事件，重新显示继续原页面。

## 本地 241 API 证据

证据位置是二进制 JAR 中的 class，不存在可引用的 Java 源行号；以下为只读解析常量池及 methods 表所得原始公开方法描述符（`L...;` 是 JVM 引用类型，`Z` 为 boolean，`V` 为 void）。未加载或执行这些类。

SDK 根目录：`D:/goland/GoLand 2024.1.6/`。

```text
lib/app-client.jar!/com/intellij/ui/jcef/JBCefApp
  isSupported ()Z
lib/app-client.jar!/com/intellij/ui/jcef/JBCefBrowser
  <init> ()V
  <init> (Ljava/lang/String;)V
  getComponent ()Ljavax/swing/JComponent;
  dispose ()V
lib/app-client.jar!/com/intellij/ui/jcef/JBCefBrowserBase
  loadURL (Ljava/lang/String;)V
  getCefBrowser ()Lorg/cef/browser/CefBrowser;
  getJBCefClient ()Lcom/intellij/ui/jcef/JBCefClient;
lib/app-client.jar!/com/intellij/ui/jcef/JBCefClient
  addLoadHandler (Lorg/cef/handler/CefLoadHandler;Lorg/cef/browser/CefBrowser;)Lcom/intellij/ui/jcef/JBCefClient;
  removeLoadHandler (Lorg/cef/handler/CefLoadHandler;Lorg/cef/browser/CefBrowser;)V
  addLifeSpanHandler (Lorg/cef/handler/CefLifeSpanHandler;Lorg/cef/browser/CefBrowser;)Lcom/intellij/ui/jcef/JBCefClient;
  removeLifeSpanHandler (Lorg/cef/handler/CefLifeSpanHandler;Lorg/cef/browser/CefBrowser;)V
lib/app-client.jar!/com/intellij/openapi/wm/ToolWindowFactory
  createToolWindowContent (Lcom/intellij/openapi/project/Project;Lcom/intellij/openapi/wm/ToolWindow;)V
lib/app-client.jar!/com/intellij/openapi/wm/ToolWindow
  getType ()Lcom/intellij/openapi/wm/ToolWindowType;
  setType (Lcom/intellij/openapi/wm/ToolWindowType;Ljava/lang/Runnable;)V
  getContentManager ()Lcom/intellij/ui/content/ContentManager;
lib/app-client.jar!/com/intellij/ui/content/Content
  setPreferredFocusableComponent (Ljavax/swing/JComponent;)V
  setDisposer (Lcom/intellij/openapi/Disposable;)V
lib/lib-client.jar!/org/cef/browser/CefBrowser
  goBack ()V
  goForward ()V
  reload ()V
  loadURL (Ljava/lang/String;)V
  executeJavaScript (Ljava/lang/String;Ljava/lang/String;I)V
  getURL ()Ljava/lang/String;
lib/lib-client.jar!/org/cef/browser/CefFrame
  getURL ()Ljava/lang/String;
  isMain ()Z
  executeJavaScript (Ljava/lang/String;Ljava/lang/String;I)V
lib/lib-client.jar!/org/cef/handler/CefLoadHandlerAdapter
  onLoadEnd (Lorg/cef/browser/CefBrowser;Lorg/cef/browser/CefFrame;I)V
lib/lib-client.jar!/org/cef/handler/CefLifeSpanHandlerAdapter
  onBeforePopup (Lorg/cef/browser/CefBrowser;Lorg/cef/browser/CefFrame;Ljava/lang/String;Ljava/lang/String;)Z
```

方法访问标记过滤为 public。浏览器 handler 的完整业务语义仍以官方文档及后续运行验证为准，二进制签名只证明当前版本存在这些入口。

## 官方依据

- [JCEF 文档](https://plugins.jetbrains.com/docs/intellij/embedded-browser-jcef.html)：Using JCEF、Browser Client、Executing JavaScript、Disposing Resources。支持检查、默认 client 随 browser 释放、加载后注入、2025.3.1 新依赖边界。文档是当前版本，不以其新 API 替代上面的 241 二进制证据。
- [Tool Windows](https://plugins.jetbrains.com/docs/intellij/tool-windows.html)：Declarative Setup / Contents / Threading。声明式工厂按需创建、Content 持有 disposer、ToolWindow 延迟 UI 操作调度。
- [Disposer and Disposable](https://plugins.jetbrains.com/docs/intellij/disposers.html)：Automatically Disposed Objects / Choosing a Disposable Parent。扩展自身不自动释放；工具窗口内容资源绑定 Content；不要直接以 Project 为 parent。

## 涉及文件清单

| 文件:起止行 | 当前行为 | 需要的改动 | 结论来源 |
|---|---|---|---|
| `D:/goland/GoLand 2024.1.6/product-info.json:3-4` | 版本 2024.1.6，构建 241.19072.18 | 只读，无修改 | 本机安装元数据 |
| `tools/heihei-goland/src/main/resources/META-INF/plugin.xml`（新增） | 无现有插件注册 | 平台依赖、241 范围、ToolWindow 注册 | 官方 Tool Windows + 本地 API |
| `tools/heihei-goland/src/main/java/.../HeiHeiToolWindowFactory.java`（新增，命名由实现统一） | 无 | 懒创建工厂、JCEF 支持判断、Content 生命周期 | 官方 JCEF/Disposer + 本地 API |
| `tools/heihei-goland/src/main/java/.../HeiHeiBrowserPanel.java`（新增，允许收敛到一个文件） | 无 | 浏览器、最小导航、样式 handler | 本地 241 签名 |
| `tools/heihei-goland/src/main/resources/...css`（新增，root 负责） | 原站有主题变量和宽度约束 | 定向暗色与窄窗适配 | root 已取原站 HTML/CSS 的前序证据 |
| `tools/heihei-goland/README.md` / 构建脚本（新增，root 负责） | 无 | 使用方式、当前支持版本、准确授权命令和真实验证状态 | 用户 P0 + 当前版本 |

Proto 变更：无。索引：本任务是独立 IDE 工具，不对应现有游戏业务/框架模块；插件 README 承担维护入口，是否还需工具目录索引由 root 按实际目录检查决定。

## 具体风险与边界

- **页面兼容，待运行确认**：用户已打开帖子列表，再点击需要验证或扫码的登录按钮，原站可能依赖弹窗/第三方跳转；若插件把全部 popup 强行复用当前页，登录后回传可能与原站预期不同，表现为登录界面停留或回不到原帖。API 只能证明 popup 入口存在，尚未验证网站登录实现；不能据此断言登录已故障。保留正常网页链路，在授权运行后人工验证。
- **释放遗漏，设计已约束**：用户浏览后关闭项目，如果 browser 没有绑定 Content 生命周期，Chromium 资源可继续存在，用户看到 IDE 内存/进程不释放。官方 Disposer/JCEF 文档明确释放契约；规格 5 使用唯一 owner 和默认 client 避免该候选实现风险。
- **版本依赖，已确认边界**：当前用户安装 241 插件，如果元数据照抄新版 `com.intellij.modules.jcef`，该依赖别名在 241 不存在，插件无法按目标版本启用。官网 Plugin Dependency 明确最低 2025.3.1；规格 10 排除此路径。
- **页面导航后误注入，设计已约束**：用户从帖子跳至第三方登录页，前一页的异步回调稍后到达，若仅据旧 frame URL 注入，可能将小黑盒 CSS 应用到当前登录页。注入前 Java URL 守卫与脚本内 location.origin 双重核对均为局部样式安全边界，不读取账户内容。

## [规则冲突检查]

1. 用户 P0 禁止未经明确授权测试/编译/测试资产。本阶段只读解析 SDK 与写架构报告；原计划 javap 工具不存在，未调用 javac 代替。符合（10/10）。
2. 用户要求可浮动/停靠并融入 IDE；采用原生 ToolWindow 生命周期和模式，未另造窗口管理、缓存或账户机制。符合（9/10）。
3. 用户限定新工具目录、保留既有 Diff；本阶段仅写指定任务报告，实施规格仅新增工具目录。符合（10/10）。
4. 游戏专属 Actor/ECS/codegen/起服规则与当前 Java 工具无对应对象，未引入 Go 构建或 Proto。符合（10/10）。

## [隐含假设审计]

- 目标是本机 241.19072.18：已验证，product-info.json:3-4（10/10）。
- 所列 JCEF/ToolWindow 方法在该 SDK 存在：已验证，二进制原始描述符（10/10）。
- IDE 当前运行时支持 JCEF：未执行运行态验证；规格强制 isSupported 分支，属于部署验证边界，不能当成功事实（7/10，条件方案）。
- 小黑盒完整浏览和登录在当前 JCEF 内可用：未验证，不作为已完成结论；用户后续已授权单插件两源文件定向编译与打包，安装后的网页运行效果由实际使用确认（7/10，条件方案）。
- 原站 CSS 变量与布局：由 root 提供已抓取证据，本 agent 未重复读取；Java 方案不依赖具体选择器（8/10）。

## [副作用扫描]

- IDE：每个实际打开工具窗口的项目持有一个 Chromium browser；懒加载避免未使用时开销，隐藏窗口保留浏览状态，无额外轮询。
- 网站：样式限原站主文档；无内容发布、自动登录、账户采集或请求改写。
- 游戏仓库：范围仅新插件目录和指定阶段报告；SDK 探查仅 app-client.jar、lib-client.jar 及产品元数据，没有修改现有模块、缓存、持久化或索引。

## [置信度校准]

上述具体规格均 >= 7；页面/登录效果为明确未验证的交付边界，不能以静态 API 核对替代运行验收。无需要改变用户已确认业务方案的低置信度决策。阶段后续状态：E006 已记录用户授权单插件两源文件定向编译及安装 ZIP 放桌面；不包括大范围构建、依赖编译或测试。

## 修订记录（第五步回看）

| 修订 | 原结论 | 新结论 | 原因 / 新事实 | 受影响范围 |
|---|---|---|---|---|
| R1 | 计划调用本机 javap 核对 API | JBR 无 javap 且无 jdk.jdeps，改为只读解析 class methods | 目录清单与 java --list-modules 输出 | 仅证据获取方式；API 方案未变，未运行编译 |

## 路过发现

探查范围为本机 SDK 目标类、产品元数据及三份官方插件文档。未探查游戏业务源码，未发现范围外可疑点。新版文档依赖 alias 的版本差异已作为本任务实施约束，不扩展成仓库规则。

## 关联风险

本阶段未确认已有插件缺陷模式，无需全仓 sibling 扫描。涉及释放和导航的内容是待实现设计约束，不能当作现有缺陷；不得因此修改其他工具或游戏模块。

## STATE_PROPOSAL

- T01：Java SDK/API 方案子项完成；页面 CSS 部分由 root 的调查补齐。
- 新 Evidence：本机 app-client.jar/lib-client.jar 公开方法描述符，证明 241 API 可用，尤其 onBeforePopup 为 4 参数。
- 新 Evidence：官方 JCEF 文档的 com.intellij.modules.jcef alias 下限是 2025.3.1，当前 241 不使用。
- T04：后续用户要求安装 ZIP 放桌面，E006 / DECISIONS 已解除单插件两源文件定向打包的授权阻塞；用户自行从磁盘导入，不自动安装 IDE。本架构阶段历史事实仍为未执行编译/测试。
- Decisions：默认一个 ToolWindow 和一个浏览器；Content 管理生命周期；无自建 client、JS bridge、持久化或后台任务。
