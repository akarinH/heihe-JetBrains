---
phase: 03
agent: ecs-developer
task: "heihei-goland"
depends_on: ["01_architect_analysis.md"]
---

# 小黑盒插件实现

## 业务路径与实现结果

用户在 GoLand 的「视图 → 工具窗口 → 小黑盒」首次打开窗口，工厂才创建浏览面板和浏览器。页面从官方社区首页开始；用户可通过原生按钮后退、前进、刷新、回首页、切换浮动/停靠、转到系统浏览器。具体创建点为 `tools/heihei-goland/src/main/java/cn/local/heihei/HeiHeiToolWindowFactory.java:14-19`，初始导航在 `HeiHeiBrowserPanel.java:93`，按钮在 `HeiHeiBrowserPanel.java:97-154`。

正常 HTTP(S) 页面跳转保留原站流程；本站帖子弹窗在当前工具窗口加载。JCEF 241 的四参数 popup 回调没有用户手势信息，因此外站弹窗只在原生状态条展示目标域名，等用户点「打开外部链接」才调用系统浏览器；非网页协议被阻止。不会因网页脚本或重定向自动唤起系统浏览器。证据：`HeiHeiBrowserPanel.java:158-172`、`:218-247`、`:251-258`。

主文档加载完成和 IDE 切换主题都会调用 `applyTheme`。方法在 EDT 上重新检查当前地址，只有小黑盒域名才读取 IDE 当前暗色调色板并替换本地 CSS 占位符；浅色 IDE 使用 Darcula 后备值。脚本资源由 root 维护，注入文本以 UTF-8 CSS 的 Base64 传入，无 Java bridge。证据：`HeiHeiBrowserPanel.java:90-91`、`:196-204`、`:269-289`。资源接口与 root 的 `/web/theme.css`、`/web/theme.js` 保持一致。

当 IDE 不支持 JCEF 时，原生页面解释原因，系统浏览器入口仍可用（`HeiHeiBrowserPanel.java:69-77`）。断网等主页面加载失败时，页面状态和 `idea.log` 记录错误码；不记录可能携带账户参数的 URL 或原始错误描述，正常取消导航不报错（`:209-215`）。隐藏工具窗口保留页面；内容释放时由 Content → panel → browser 释放资源，迟到回调检查销毁标记（工厂 `:17`，面板 `:80`、`:327-329`）。

## 本阶段修改文件

1. `tools/heihei-goland/src/main/java/cn/local/heihei/HeiHeiToolWindowFactory.java`：懒创建工具窗口内容，接入 Content 生命周期。
2. `tools/heihei-goland/src/main/java/cn/local/heihei/HeiHeiBrowserPanel.java`：浏览器、原生工具栏、加载反馈、主题、弹窗和协议边界。
3. `tools/heihei-goland/src/main/resources/META-INF/plugin.xml`：插件名和工具窗口 ID 均为“小黑盒”，默认右侧，目标 241.*，仅依赖 platform。
4. `tools/heihei-goland/src/main/resources/icons/heihei.svg`：16px 工具窗口图标。
5. `_workspace/heihei-goland/03_implementation.md`：本阶段记录。

未编辑 root 负责的脚本、README、CSS、JS，也未编辑六个共享状态文件。

## Round 2 修改文件

- `tools/heihei-goland/src/main/java/cn/local/heihei/HeiHeiBrowserPanel.java`：root 指出右侧窄窗可能裁掉工具栏右端操作；前四个导航改为短符号，保留中文 tooltip 和无障碍名，后两个按钮缩为“浮动”“浏览器”。现有 JToolBar 布局不变，未新增布局系统或配置。
- `_workspace/heihei-goland/03_implementation.md`：同步本轮清单和最终源码行号。

本轮静态核对六个按钮都有中文 tooltip 和无障碍名（面板 `:102-140`）。仅缩短按钮标签，不改变导航和工具窗口状态流；JCEF 相关 sibling 搜索范围内仅有本工具栏，不存在应同步修改的第二实现。

## 新增方法

- 工厂：`createToolWindowContent`。
- 面板：构造方法、`createToolbar`、`createStatusBar`、`registerHandlers`、`showExternalLink`、`showStatus`、`applyTheme`、`readResource`、`isWebUrl`、`isSiteUrl`、`colorHex`、`dispose`。
- JCEF 回调：`onLoadingStateChange`、`onLoadEnd`、`onLoadError`、`onBeforeBrowse`、`onBeforePopup`。

上述手写方法和回调均有中文职责注释。工具栏事件使用标准 Swing 回调；没有新增线程、锁、重试、配置存储、地址栏、收藏或账户数据能力。

## 静态检查与未验证边界

- PowerShell XML 解析成功，原始输出注册值：`id=小黑盒`、`anchor=right`、`factoryClass=cn.local.heihei.HeiHeiToolWindowFactory`。
- 静态核对的资源替换项为 `__BG__`、`__PANEL__`、`__TEXT__`、`__MUTED__`、`__BORDER__`、`__LINK__`、`__HEIHEI_CSS_BASE64__`，与 root 资源约定一致。
- architect 提供本机 SDK 241 的 request/load/error/popup 描述符核实结果，当前参数一致；尤其 `onBeforePopup` 为四参数，`ERR_ABORTED` 枚举存在。
- `git status --short -- tools/heihei-goland` 输出 `?? tools/heihei-goland/`；该目录是新增资产。`git diff --check` 返回 0，但因未跟踪文件不在普通 diff 中，不把它当作新增源文件语法验证。
- 本 agent 未执行 javac、Gradle、Go 编译或任何测试，未新增测试文件、夹具、测试矩阵、测试脚本或缓存。用户后续明确要求桌面 ZIP，root 按新增授权负责本插件单包定向编译与打包。
- 尚未安装运行。实际页面、扫码登录、第三方 popup 登录回传和视频兼容性不能由静态检查确认；不宣称这些路径已通过。

## 修订事件记录

| 修订 | 原结论 | 新结论 | 新事实/依据 | 受影响范围 |
|---|---|---|---|---|
| R1 | 初始任务描述要求仅本站导航内嵌 | 普通 HTTP(S) 导航保留原站登录跳转；本站才注入样式 | root 明确收敛导航策略，避免擅自阻断第三方登录跳转 | `onBeforeBrowse`、主题域名守卫；无自动外部浏览器跳转 |
| R2 | 外站 popup 可直接交系统浏览器 | 只显示域名和原生确认按钮 | 四参数回调没有 userGesture，页面脚本也能发起 popup；root 明确认可临时 pending link 方案 | `onBeforePopup`、原生状态条；URL 不持久化 |
| R3 | ToolWindow id 暂定 HeiHei | 使用中文“小黑盒” | root 要求和 README 菜单路径一致 | plugin.xml；不增加 bundle 或额外窗口标题同步 |

## 路过发现

探查范围：本机 API 报告、当前新插件源码、root 的主题脚本和 README、`tools` 中 Java/JCEF 注册文本。未发现范围外可疑代码；没有探查或修改游戏模块。JBR 不带 javap 已由 architect 记录，本 agent 没有用编译替代只读查询。

## 关联风险

本阶段是新增独立插件，没有确认既有代码缺陷模式，不触发针对旧代码的修复扩张。为确认边界已执行 `rg -n 'JBCefBrowser|onBeforePopup|cn\.local\.heihei' tools --glob '*.java' --glob 'plugin.xml'`，命中仅当前新增两个 Java 文件和当前 plugin.xml；不存在本任务应同步修改的 sibling 插件入口。

网页兼容性是运行验收边界：用户在帖子页发起第三方弹窗登录后，登录页面可能需要向原窗口回传，而系统浏览器与嵌入页面不是同一窗口；本期没有承诺该原站流程一定可用。现有原生系统浏览器入口允许用户继续原站操作，没有自行实现账户流程或兼容桥接。

## [规则冲突检查]

1. P0 禁止未经授权测试、编译和测试资产：本 agent 仅新增生产源码与报告，工具调用为文本/XML/SDK 签名核对，没有执行构建。后续准确构建由 root 承接新增用户授权。符合（10/10）。
2. 用户 P2 要求只实现本期并保留现有 Diff：只写指定四个插件文件与本报告，不动 root 文件、Go 源码、现有用户修改或共享状态文件。符合（10/10）。
3. P1 禁止空壳公开包装私有方法：没有为默认参数或兼容旧调用引入公开包装层；Java 的平台回调公开，局部组装及 URI/颜色转换保留 private。符合（9/10）。
4. 架构要求 Content 管理 JCEF 生命周期与 EDT 更新：浏览器只注册一个 owner，UI 回调均切 EDT，销毁标记为 volatile；`setType` 由原生按钮 EDT 事件直接调用。符合（9/10）。
5. 游戏 ECS/Proto/Robot 和服务日志规则与本插件无对应对象；没有为形式满足而加服务端日志、线程或代码生成。符合（10/10）。

## [隐含假设审计]

- 本机 API 与 241 兼容：architect 的类描述符已验证（10/10）。其他 Swing/JDK 标准用法尚等待 root 定向编译，不宣称编译通过。
- 根目录资源会随打包脚本进入 JAR：按 root 已完成构建脚本的既定接口实施；Java 缺失资源有可诊断分支（8/10）。最终 ZIP 内容由 root 核对。
- JCEF 当前环境支持：未运行核实，`isSupported` 原生降级路径明确（7/10）。
- 原站页面与完整登录可用：未验证，只交付明确边界，不扩展账户实现（7/10）。
- Java 域名守卫与 root 脚本守卫一致：均要求 HTTP(S) 且 host 为 `xiaoheihe.cn` 或以 `.xiaoheihe.cn` 结尾；静态读取核对（10/10）。

## [副作用扫描]

- IDE 资源：每个实际打开工具窗口的项目创建一个浏览器，隐藏时保留；释放由原生内容树管理，没有全局服务和线程。
- 网站页面：对本站注入本地外观脚本，不读取账户信息；普通 HTTP(S) 导航保持原站，弹窗方式存在上述明确兼容边界。
- 系统浏览器：只在两个原生按钮事件中调用 `BrowserUtil.browse`，不会从 JCEF 页面回调直接调用（源码 `:144`、`:168`）。
- 仓库模块：静态搜索 `tools` 中 JCEF 和插件入口，仅命中本次新目录；没有游戏源、持久化、缓存或模块索引副作用。独立插件维护入口由 root README 承担。

## [置信度校准]

已实现边界的静态依据均不低于 7/10。运行效果未作已验证结论，不据此新增机制或资产。未发现需要改变已确认业务方案的低置信度决策；具体安装/登录效果交 root 实际构建交付及用户安装使用确认。

## [STATE_PROPOSAL]

- T02 源码子项完成：上述两个 Java 文件、元数据与图标；主题资源由 root 另行交付。
- 新 Evidence：XML 解析成功；241 request/load/error 签名与实现一致；BrowserUtil 两处调用均来自原生按钮事件。
- T03 等独立静态审查，不以本报告代替审查结论。
- T04 更新为用户已要求可安装 ZIP 放桌面，root 执行本插件单包定向构建；本 agent 无编译或运行结论。
