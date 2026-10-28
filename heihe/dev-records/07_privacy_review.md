---
phase: 07
agent: plugin-developer-privacy-review
task: "heihei-goland"
depends_on: ["TASKS.md", "EVIDENCE.md", "DECISIONS.md"]
---

# 发布隐私只读审计

## 审计范围与结论

本报告检查的是 0.1.1 发布基线：当时 `src/main/resources/META-INF/plugin.xml:4` 为 `0.1.1`。范围包括两个 Java 源文件、四项资源、`build.ps1`、`README.md` 和 `dist/heihei-goland-0.1.1.zip` 内存解包后的 JAR。root 并行实现的 0.1.2 加载流程不自动继承本报告结论，最终 `heihe.zip` 仍需核对自身内容与元数据。

源码分享时存在一处本机 SDK 路径及其文档示例；现有 ZIP/JAR 保留构建和资源修改时间。0.1.1 发布包本身未发现本机用户名、仓库名、邮箱、凭据字面量、本地网络地址或绝对机器路径。上述结论来自限定模式扫描和数据流阅读，不是对第三方网站行为的运行审计，也不宣称未知内容绝对不存在。

本 agent 只执行六次只读命令和一次报告写入。未修改生产代码或构建脚本，未运行编译、测试、插件或页面脚本，未新增测试资产，未在磁盘解包归档。

## 已确认处理项

### P1：分享插件源码会带出本机构建目录

业务场景：用户将插件源码目录交给另一位开发者。接收者原本只需要自己的 IDE SDK 路径，但脚本默认参数和 README 示例保留了制作机器的绝对 SDK 安装位置；分享后接收者可以看到制作机器的目录布局，脚本还会默认尝试该机器特有位置。

证据：审计快照 `tools/heihei-goland/build.ps1:3` 的 `$IdeHome` 默认值为绝对 Windows SDK 安装路径；`README.md:34` 的命令示例重复该路径。为遵守脱敏要求，本报告不复述完整值。发布包条目清单不包含这两个文件，因此这是源码分享问题，不是已经从 0.1.1 安装 ZIP 暴露的路径。

修复方向：`IdeHome` 改为必填参数或中性环境配置；README 使用 `<IDE 安装目录>` 一类占位说明。不要将本机路径改存到仍会分享的默认配置文件中。置信度 10/10。

### P2：分享发布包会带出制作时间信息

业务场景：用户把现有 ZIP 转发给其他人。正常发布只需包含插件运行内容，但归档函数直接继承源文件时间；接收者查看 JAR/ZIP 条目属性，可区分构建时刻和资源修改时刻。它不会直接暴露用户名，但违背本期发布元数据最小化目标。

代码证据：审计快照 `build.ps1:61-64` 使用 `CreateEntryFromFile` 并未覆盖 `LastWriteTime`。现有归档读取原始依据包括：

```text
ZIP entry=heihei-goland/lib/heihei-goland.jar bytes=16320 LastWriteTime=2026-10-28T18:38:24.0000000+08:00 attrs=0
JAR entry=web/theme.js bytes=4709 LastWriteTime=2026-10-28T18:03:44.0000000+08:00 attrs=0
```

这里 `+08:00` 是当前读取环境对 DOS ZIP 时间的呈现，不据此声称归档另存了机器时区。修复方向是对 JAR 和外层 ZIP 的每个条目设置同一固定、合法的 ZIP 时间。置信度 10/10。

## 发布最小化与候选风险

### 调试信息确实存在，但不是个人信息命中

0.1.1 五个 class 都含 `SourceFile` 与 `LineNumberTable`；未检出 `LocalVariableTable` 或 `SourceDebugExtension`。原始提取片段：

```text
cn/local/heihei/HeiHeiBrowserPanel.class
attributes=SourceFile,LineNumberTable
sourceNames=HeiHeiBrowserPanel.java

cn/local/heihei/HeiHeiToolWindowFactory.class
attributes=SourceFile,LineNumberTable
sourceNames=HeiHeiToolWindowFactory.java
```

这些是通用源文件名和行号，没有包含绝对源码路径。现有 `build.ps1:47-49` 没有 `-g:none`；按 root 的发布方案增加该参数可以移除不必要的源文件/行号元数据，但不能将当前通用文件名包装成已泄露个人信息。置信度 10/10。

### 当前包干净，资源复制入口仍过宽

审计快照 `build.ps1:54` 递归复制 `src/main/resources/*`，`:61` 又递归打包整个 classes 目录。当前资源只有 plugin.xml、CSS、JS、SVG，实际归档也只包含预期内容；未发生多带敏感文件的问题。

候选场景：后续开发者临时把日志或配置放进 resources，再打包时会被通配复制包含。该场景尚未发生，属于未来误装包风险。root 已计划明确四项资源白名单，并只保留本插件 class；这一小改与本期分享边界直接相关。无需引入额外扫描框架或测试资产。置信度 9/10。

## 现有发布包的原始内容依据

外层 ZIP 仅有：

```text
heihei-goland/lib/heihei-goland.jar
```

JAR 完整条目清单：

```text
cn/local/heihei/HeiHeiBrowserPanel.class
cn/local/heihei/HeiHeiBrowserPanel$1.class
cn/local/heihei/HeiHeiBrowserPanel$2.class
cn/local/heihei/HeiHeiBrowserPanel$3.class
cn/local/heihei/HeiHeiToolWindowFactory.class
icons/heihei.svg
META-INF/plugin.xml
web/theme.css
web/theme.js
```

没有 Java 源码、README、build.ps1、日志、工作区文档、SDK JAR、账户文件或编译参数文件；条目都是相对路径，没有 `..` 路径段或盘符。读取中央目录原始输出中，外层 1 项、内层 9 项的 `extraBytes=0 commentBytes=0`，两层 `archiveCommentBytes=0`，所有 `ExternalAttributes=0`。因此未发现通过归档注释或中央目录额外字段带出文件所有者等机器信息。

字节内容按 UTF-8 读取做敏感模式检查，范围覆盖全部条目；源码覆盖 6 个文件。类别包含真实 Windows/Unix 绝对路径、本地地址、邮箱、当前机器用户名、当前仓库名以及显式凭据赋值。没有精确模式命中。类常量字符串和资源路径本身不是个人信息；公开网站 URL、`cn.local.heihei`、`/web/theme.css` 明确作为合理内容保留。

## 本机数据到网页的路径审查

1. 工厂只接收平台 Project 参数并创建面板，没有读取项目名称、路径、文件或配置：`HeiHeiToolWindowFactory.java:14-19`。
2. 面板从自身 JAR 读取 `/web/theme.css` 和 `/web/theme.js`：审计快照 `HeiHeiBrowserPanel.java:84-85`；读取实现 `:304-307` 仅为 `getResourceAsStream(path)`。不是读取任意本地路径。
3. 导航从公开常量首页开始（`:43`、`:94`）；正常页面地址来自浏览器请求和用户导航，未附加机器名或本地项目参数。系统浏览器入口来自原生按钮（`:145`、`:179`）。
4. 注入页面的字符串由 CSS 资源与六项 IDE 颜色组成（`:288-300`），没有环境变量、系统属性、文件内容、账户密钥或项目数据。网页能够看到注入后的颜色值，这是外观功能本身，不等同于读取个人信息。
5. `theme.js:4-5` 校验站点；`:7-17` 解码 CSS 并写入 style；`:51-93` 只调整 CSSOM 和观察新增样式节点。静态检索未见 `fetch`、XHR、WebSocket、sendBeacon、cookie 读取或 Java bridge。CSS 没有额外 `url(...)`/`@import` 数据发送入口。
6. 页面加载错误日志只记录错误码（`:220-224`），没有完整失败 URL。公开原站正常加载、登录和网络连接仍遵循原站行为，本报告不把这些用户主动使用网站的正常流量误判为插件外传本地数据。

0.1.2 若新增主题就绪回传，后续审查应核对它仅传就绪标记并只驱动显示状态，不能由本报告推定新增回传实现已安全。

## 修订记录

| 修订 | 原检查结果 | 修订结果 | 原因与重评 |
|---|---|---|---|
| R1 | 初筛将一个 class 和 SVG 标记为 absolutePath | 两项均为路径规则误报，精确检查无命中 | 过宽的 `[a-z]:/` 会命中公共 HTTP URL 的尾部；增加字母边界后重扫所有归档条目，并保持公开 URL/命名空间不算隐私。所有路径结论以第二轮精确扫描为准。 |

## 路过发现与关联风险

没有发现范围外业务缺陷。审计未进入游戏模块；本期只检查当前独立插件与既有发布包，不修改历史审计文档里的真实证据。

两项确认模式已在本插件范围泛化：机器路径扫描覆盖源码资源、build.ps1、README 与全部包条目，确认只在构建默认值/说明中；时间继承覆盖内外两层归档，均需固定。公共 URL 误报已排除。没有证据要求修改其他工具构建链；只对本插件落地，不扩展为全仓发布改造。

## [规则冲突检查]

1. 用户 P0 和本轮委派禁止本 agent 编译、测试、生产写入与测试资产：只读源码、内存读取 ZIP、静态文本/XML信息及此报告写入，符合（10/10）。
2. 用户要求不泄露本机路径/个人信息：报告指明敏感值类型和具体行号，不复制完整机器路径或用户名；历史原始证据不改写、不打包，符合（10/10）。
3. P1 要求确认问题先说明业务场景：两个处理项分别从源码分享和发布包转发说明可观察影响；未来误装资源仅列候选风险，符合（10/10）。
4. root 限制最多六次只读调用后一次报告：已按预算收敛，没有额外文件搜索或持久化辅助脚本，符合（10/10）。

## [隐含假设审计]

- 被检查的发布对象是 0.1.1：ZIP 文件名、包内/源码 plugin.xml 版本和完整条目已验证（10/10）。
- current machine/repo 的模式扫描不输出其完整值：脚本使用环境和当前目录取得模式，只报告类别与条目；源码扫描无命中（10/10）。
- `-g:none`、固定时间和资源白名单会进入 0.1.2：root 的计划，尚未作为实际产物核实；本报告只建议，不宣称完成（7/10）。
- 网络数据流未读任意本机文件：当前 Java 与脚本实际调用点已读，未发现该路径（9/10）；不涵盖未来代码或第三方网页自身行为。

## [副作用扫描]

审计只读两份源码、资源、构建说明和既有 15KB ZIP，通过内存读取 16KB JAR；没有修改文件时间、解压落盘、加载插件类或执行脚本。没有 IDE、账户、网络或游戏服务副作用。报告不进入插件打包目录。

## [置信度校准]

所有确认项有文件行号或归档原始读数，置信度 9–10。候选未来误装包风险与尚未完成的 0.1.2 改造分别标明，不把它们作为现有泄露或已完成事实。不存在低于 7 且需要更改用户业务方案的决策。

## [STATE_PROPOSAL]

- T11：0.1.1 基线隐私审计完成；源码路径和真实时间元数据交 root 在 0.1.2 修正。
- Evidence：既有 ZIP/JAR 完整条目、中央目录 extra/comment 为零；未发现本机个人标识；class 只有通用 SourceFile/LineNumberTable。
- T12：最终 heihe.zip 应核对自身白名单内容、固定时间、无调试属性和无机器信息；本报告不冒充最终 0.1.2 产物审计。

---

## 0.1.2 最终复核：源码静态审计与发布包内存审计

本节是独立于前述 0.1.1 基线的最终复核。收到 root 的新包成功通知后，实际读取 `tools/heihei-goland/dist/heihe.zip`，没有沿用旧版归档结论。只读检查三次，并仅追加本报告；未编译、测试、运行插件或新增测试资产。

**结论：本次发布包在所列隐私审计范围内通过。** 前述源码机器路径和真实文件时间两项已处理；源码资源白名单与调试信息最小化也已落实。没有发现尚需修改的隐私项。网页实际加载和主题显示效果由其他阶段核对，本报告不作运行效果承诺。

### 源码分享边界复核

- `build.ps1:3-4`：SDK 目录由必填 `IdeHome` 参数传入，已无本机默认值。
- `README.md:38`：构建示例使用中性 `<IDE_HOME>` 占位符；`:41` 解释由使用者替换为自己的 IDE 目录。
- `build.ps1:18`、`:56-64`：逐项复制 `META-INF/plugin.xml`、`web/theme.css`、`web/theme.js`、`icons/heihei.svg`，不再通配复制资源目录。
- `build.ps1:49`：编译参数明确 `-g:none`；`:74-80` 在写条目内容之前设定固定时间；`:91-96` 发布为 `heihe/lib/heihe.jar` 和 `heihe.zip`。
- 重新扫描两个 Java、四项资源、脚本、README 共 8 文件，真实绝对路径、本地地址、邮箱、当前机器用户名、仓库名和显式凭据赋值均无命中。公开 URL、通用类包名、资源路径不是个人信息。

这些检查针对可分享插件目录，未改写历史工作区证据，也未将其放入发布包。

### 新增主题回传的数据边界

用户刷新后，页面准备完成只需告诉原生面板切换显示。当前实现中，Java 在 `HeiHeiBrowserPanel.java:379-383` 注入 CSS 和固定格式 `ready:<navigationId>` / `error:<navigationId>`；数字是窗口内当前导航序号，不是本机或用户身份。

回传处理器在 `:109-111` 把信号切回 EDT 后 `return null`，不向网页返回本机数据；`:350-353` 校验当前本站页面，并仅按当前导航序号切换显示或提示准备失败。没有以收到的字符串作为文件路径、命令、URL、项目查询或其他本机操作参数。

脚本 `theme.js:8-15` 仅发送上述固定信号，不附带异常文本、页面地址或账户内容；`:19-31` 写入本地主题，`:68-144` 读取已加载的 CSSOM、更新颜色并观察样式节点。完整静态读取 148 行，未见额外网络发送或本机数据读取。Java 的本地读取仍局限于自身 JAR 的两项主题资源（`:114-115`、`:387-390`）。该回传是新增的显示状态能力，不能继续沿用旧版“没有 Java bridge”的表述；其实际能力已按上述入口独立审查。

### 最终归档身份与完整内容

原始读取结果：

```text
ZIP SHA256=3753BBA9687BA76E5F34F36591C2113F148055758484834AA8C684FBBDBA0712 bytes=16934
PLUGIN id=cn.local.heihei name=heihe version=0.1.2 since=241 until=241.* dependency=com.intellij.modules.platform
CONTENT classes=5 resources=4
```

外层 ZIP 的唯一条目为 `heihe/lib/heihe.jar`。内层完整条目为：

```text
cn/local/heihei/HeiHeiBrowserPanel.class
cn/local/heihei/HeiHeiBrowserPanel$1.class
cn/local/heihei/HeiHeiBrowserPanel$2.class
cn/local/heihei/HeiHeiBrowserPanel$3.class
cn/local/heihei/HeiHeiToolWindowFactory.class
icons/heihei.svg
META-INF/plugin.xml
web/theme.css
web/theme.js
```

没有源码、README、构建脚本、SDK、日志、配置、工作区审计记录或临时构建路径。

### 常量池与资源扫描依据

内存中逐个解析 class 常量池条目，不执行或加载 class；对全部 UTF-8 常量及全部资源做敏感模式检查。原始计数：

```text
HeiHeiBrowserPanel.class   utf8Constants=347 debugAttributeCount=0
HeiHeiBrowserPanel$1.class utf8Constants=104 debugAttributeCount=0
HeiHeiBrowserPanel$2.class utf8Constants=33  debugAttributeCount=0
HeiHeiBrowserPanel$3.class utf8Constants=42  debugAttributeCount=0
HeiHeiToolWindowFactory.class utf8Constants=28 debugAttributeCount=0
```

`debugAttributeCount` 检查 `SourceFile`、`LineNumberTable`、`LocalVariableTable`、`SourceDebugExtension` 四个属性名，均未存在。所有常量池/资源未发现本机身份、仓库名、真实绝对路径、邮箱、凭据赋值或本地 URL 模式命中。对通用包名、公开网站地址及插件资源相对路径未作隐私误报。

### ZIP/JAR 元数据依据

逐条检查中央目录和相应本地文件头，两层全部 10 个条目均满足：

```text
relative=True fixed2000=True centralExtra=0 localExtra=0 comment=0
ZIP entries=1 archiveComment=0
JAR entries=9 archiveComment=0
```

`relative=True` 表示没有根路径、盘符或 `..` 路径段；`fixed2000=True` 同时核对中央目录和本地文件头的 DOS 日期为 2000-01-01、时间为 00:00:00。两层均无额外字段或注释可携带机器/文件信息；本次没有把读取环境显示时区误认为归档字段。

### 本轮反思、修订与关联风险

**[规则冲突检查]**

1. 委派只允许只读和报告追加：三个只读命令及一次报告更新，没有生产文件写入、构建、测试或磁盘解包，符合（10/10）。
2. 用户要求分享时去掉本地信息：源码与新包均独立检查；输出不复述个人标识或绝对机器路径，符合（10/10）。
3. 兼容性保持 241、不擅自扩大：最终包实际 XML 为 `since=241 until=241.*`，符合（10/10）。

**[隐含假设审计]**

- 当前对象确为新 0.1.2：实际 XML、ZIP SHA256 和完整条目已核对（10/10）。
- 新回传不会给网页本机读取能力：实际 handler 只匹配信号并切换 UI，响应为 null；没有转交输入字符串到其他能力（9/10）。
- 源码计划确实进入产物：固定时间和无调试属性都直接从新包验证，不只依赖脚本声明（10/10）。

**[副作用扫描]**

仅在内存读取约 17KB 新 ZIP 及其 JAR，未加载 class、执行 JS、触发网络或修改归档。报告追加在工作区，发布白名单不包含该文件。无源码/账户/服务副作用。

**[置信度校准]**

源码数据边界与归档结构结论为 9–10/10；范围限定为静态隐私审计，未将其扩大为第三方网页或运行环境的全面保证。

**修订记录：** 前述 0.1.1 的机器路径与真实时间处理项，在本节 0.1.2 源码/产物证据下关闭；旧版“无 bridge”事实不适用于新版，已按固定主题就绪桥单独审查。没有推翻其他隐私结论。

**路过发现：** 源码分享文件、主题桥、内外归档和所有 class/资源范围内未见新增范围外可疑点。

**关联风险：** 路径扫描覆盖所有 8 个可分享文件与 9 个 JAR 条目；时间、注释和 extra 字段覆盖所有归档条目。未留下同款未处理入口，无需扩大到其他工具或游戏模块。

### [STATE_PROPOSAL] 0.1.2 更新

- T11 完成：源码及新包隐私复核通过，0.1.1 两项处理完成；发布无机器信息模式命中，bridge 不提供本机数据能力。
- T12 的发布包审计完成：`heihe.zip` 为 0.1.2、平台 241，白名单内容/固定时间/无调试属性已验证。桌面复制与运行验证仍由 root 按各自证据交付。
