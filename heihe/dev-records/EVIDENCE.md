# Evidence

| ID | 类型 | 来源 | 定位 | 结论 | 置信度 | 下游使用 | 失效条件 |
|---|---|---|---|---|---|---|---|
| E001 | doc | D:/goland/GoLand 2024.1.6 | 进程路径与 jbr/bin | 当前 GoLand 2024.1.6 自带 javac | high | 实现 | IDE 变更 |
| E002 | doc | https://plugins.jetbrains.com/docs/intellij/embedded-browser-jcef.html | JCEF lifecycle | JCEF 可嵌入工具窗口，支持 DOM 注入与原生生命周期 | high | 方案 | 需核对 241 本地 API |
| E003 | git | 当前工作区 git status | 初始状态 | 存在大量用户改动，严格限制新插件目录 | high | 实现 | - |
| E004 | doc | 01_architect_analysis.md | 本地 241 API 证据 | JCEF 在 lib/lib-client.jar，平台 API 在 app-client.jar；popup 为 4 参数 | high | 实现/编译 | SDK 版本变更 |
| E005 | code | 小黑盒 index-BWnev_g1.css、index-DBcvV3KQ.css | HTTP 200、CORS *、--hb-general-*、#app min-width:800px、#page-bbs-community .list/.right | 页面公开样式支持 CSSOM，主题和首页窄屏有直接依据 | high | 主题 | 原站资源更新 |
| E006 | doc | 最新用户消息 | “做完打个包放在桌面上…从这里导入插件” | 用户明确要求安装 ZIP；仅单插件包的定向编译与打包交付 | high | 交付 | 用户改变要求 |
| E007 | doc | 分类索引定向关键词检索 | goland/jetbrains/intellij/IDE插件 无命中 | 独立 IDE 工具无对应游戏业务/框架索引；README 作为维护入口 | high | 文档 | 分类新增插件模块 |
| E008 | code | theme.js / build.ps1 | node --check 与 PowerShell Parser::ParseFile | 两脚本静态语法解析成功；没有执行浏览脚本或构建 | high | 审查 | 文件变更 |
| E009 | generated | BUILD.md | build exit 0、归档原始输出、桌面路径与 SHA256 | 0.1.0 历史：编译与归档/桌面副本一致性通过，不代表 0.1.1 当前源码 | high | 历史交付 | 0.1.0 包变更 |
| E010 | doc | 04_review_report.md:8 | 独立审查结论与完整链路 | 0.1.0 历史静态审查，实际视觉遗漏由 E011 补充，不代表 0.1.1 | high | 历史交付 | 源码或范围变更 |
| E011 | code/log | 用户 3 张实际截图、原站 CSS、GoLand idea.log | DARK_FIX.md | 导航组件局部半透明变量未适配，分页渐变被跳过，Swing 按钮边框独立于网页主题 | high | 修复 | 原站或主题代码变化 |
| E012 | code | safe-route-CiT97g45.js / index-DpCaELFV.js | DARK_FIX.md 原始片段 | 标签浅色值和4种边缘遮罩使用行内背景，不能只靠 CSSOM 扫描 | high | 修复 | 原站组件变化 |
| E013 | generated | DARK_FIX.md | 0.1.1 构建与桌面交付原始输出 | 定向编译通过，包内主题与源码一致，桌面副本哈希一致 | high | 交付 | 源码或0.1.1包变化 |
| E014 | doc | 05_dark_review.md:3 | 独立增量静态审查 | 0.1.1 修复对应原始颜色来源，无新增阻塞，运行视觉未验证 | high | 交付 | 增量代码变更 |
| E015 | code/doc | 06_ready_design.md / 当前Panel与theme.js | 本地241API及CEF同文档导航契约 | createImmediately/固定就绪信号可用，刷新前隐藏；不对SPA历史等待loadEnd | high | 0.1.2 | SDK或实现变更 |
| E016 | doc | 08_ready_review.md:10 | 独立增量静态审查 | 同文档死等已修正，最终静态通过，首帧视觉未验证 | high | 0.1.2交付 | 代码变化 |
| E017 | generated/doc | 07_privacy_review.md:144 | 当前ZIP/JAR、常量池与元数据原始计数 | 当前发布包静态脱敏审计通过；5class4资源、debug属性0、固定时间/无extra/comment | high | 0.1.2交付 | 包变化 |
| E018 | generated | RELEASE_0.1.2.md | 版本、资源相等、桌面副本原始输出 | 桌面heihe.zip为0.1.2，与已审包SHA256相同 | high | 最终交付 | 包变化 |
| E019 | code | Panel:203/259/339/451、theme.js:117、scrollbars.css:23 | RELEASE_0.1.3.md | 移除两功能；新增1.25倍滚动、角区/悬停样式及0–100同值遮罩 | high | 0.1.3交付 | 源码变化 |
| E020 | doc | 09_controls_review.md:10 | 独立静态审查及修订记录 | textarea、外站滚动条与重复压暗修正后通过，实际视觉未验证 | high | 0.1.3交付 | 源码变化 |
| E021 | generated/doc | 10_controls_privacy.md:12 | 最终0.1.3包6class5资源、常量池及元数据 | 最终包脱敏通过，结论绑定965F9E29开头SHA256 | high | 0.1.3交付 | 包变化 |
| E022 | generated | RELEASE_0.1.3.md | 最终版本、源码相等与桌面原始输出 | heihe.zip为0.1.3，19542bytes，与最终审计包哈希一致 | high | 最终交付 | 包变化 |
| E023 | code/doc | Panel:129/346/368/371/471、11_gray_review.md:3 | 灰色遮罩三处消费者与LAF同步 | 当前IDE面板色统一用于网页、原生和根滚动条，静态通过 | high | 0.1.4 | 代码变化 |
| E024 | generated | RELEASE_0.1.4.md | 最终包检查及桌面原始输出 | 0.1.4/19651bytes，隐私模式与debug0，资源/元数据/副本一致 | high | 0.1.4交付 | 包变化 |

## E005 公开页面原文

2026-10-28 对入口 HTTP GET 返回 200，页面版本 `1.2.57`。首页 HTML 含：

```html
<link rel="stylesheet" crossorigin href="https://static.max-c.com/static/heybox/webapp/@heybox-spa/website/app/index-BWnev_g1.css">
```

该 CSS 响应头 `Access-Control-Allow-Origin: *`。原始样式关键片段：

```css
#app{width:100%;min-width:800px;min-height:100vh}
#page-bbs-community .list[data-v-a4016135]{flex:1;max-width:660px;width:0;padding:0}
#page-bbs-community .right[data-v-a4016135]{position:sticky;top:138px;flex-shrink:0;z-index:10;width:356px}
```

`index-ChemEL1Y.css` 的帖子详情已具备 `max-width:460px` 响应式规则；本次只解除全局根容器最小宽度并调整首页信息流，不重写详情布局。

## E025 — 0.1.5发布隐藏、导航悬停与2倍滚动

刷帖时不需要发布入口、按钮悬停无反馈且滚动偏慢。原站脚本 `index-C2SOP9CS.js` 的发布按钮使用 `class:"publish-btn"` 与文字“发布内容”，登录按钮使用 `class:"login-btn"`。theme.css:78仅隐藏 `.nav .publish-btn`；Panel:451-472统一四导航按钮主题圆角悬停绘制，禁用保护与原遮罩顺序保留；theme.js:114把wheelSpeed改为2，后续输入/横向/缩放/嵌套滚动保护不变。独立源码与当前包轻量审查见12_hover_review.md，无已确认阻塞；未实机视觉验证。

## E026 — 0.1.5最终包与桌面副本

同Java包两个源文件定向编译成功，Node静态语法检查通过；未运行测试或新增测试资产。最终包7class5资源，敏感模式及debug属性0命中，资源与源码一致、归档时间固定且无额外字段/注释。桌面heihe.zip已覆盖并核对SHA256一致：20776bytes，775CB47E1FD82B93ADFFBB2CBA7E51693A5DE3E0B21EBB81F565566DE9FC0C18；时间2026-10-28 20:06:00 +0800。原始输出见RELEASE_0.1.5.md。未自动安装/重启，实际滚动手感待用户体验。

## E027 — 0.1.6顶部五入口隐藏

用户只需要刷帖，要求移除截图五个推广入口。当前公开index-C2SOP9CS.js:1定义MMe为nav-links、DMe为nav-actions，前者逐项渲染nav-link button，中文数组顺序为首页、社区、小黑盒加速器、黑盒语音、黑盒工坊、开放平台、加入我们；后五项采用onClick打开站外页面，无href/title可用于属性匹配。theme.css:81仅隐藏nav-links的第3到7个nav-link，保留前两项与独立nav-actions。原文定位和范围复核见14_nav_review.md。

## E028 — 0.1.6窄窗口轮播按钮

正常宽屏多图帖在鼠标移入图片后显示两箭头，缩窄进入460px断点后原站将两箭头display:none而计数浮层opacity:1，用户看到1/3却无法用箭头翻图。原站index-ChemEL1Y.css:1与index-BTtUlkqO.js:1共同证实：按钮仍创建并绑定现有翻页事件，消失由media样式决定。theme.css:132-139仅在同断点恢复两按钮display:block并取消边缘transform，保留首尾禁用状态。候选、原文、反证与独立静态复核见13_carousel_analysis.md。未实机验证。

## E029 — 0.1.6定向构建、脱敏与桌面文件

同一脚本仅编译本插件单包两个Java源文件，构建通过；未运行测试或新增测试资产。对0.1.5包的静态资源比较只列出plugin.xml和theme.css变化。最终包7class5资源，敏感模式/debug属性0命中，资源与源码一致、归档元数据固定。首次覆盖旧桌面heihe.zip失败，系统原因为“being used by another process”；未干预占用进程，改存桌面heihe-0.1.6.zip并核对哈希通过。21026bytes，SHA256 5FFFE40D60F5A1020D7DC0B256E307E4820F2F7CD65157645C8BBDB53D2358D9；2026-10-28 20:38:48 +0800。原始输出见RELEASE_0.1.6.md。

## E030 — 桌面独立项目迁移

用户要求全部插件代码和资源移出git_servers，放桌面heihe。迁移前目标不存在；来源四目录均为仓库内已确认绝对路径且无reparse point：tools/heihei-goland 13文件、_workspace/heihei-goland 26文件、.tmp/build/heihe 88文件、.tmp/build/heihei-goland 22文件。先复制全部149文件，逐一核对SHA256和大小相同，再复查来源/目标边界并移除原件。原始结果：

```text
Migrated files: 149
Content verification: SHA256 and size matched for all 149 files
Original removed: tools\heihei-goland = True
Original removed: _workspace\heihei-goland = True
Original removed: .tmp\build\heihe = True
Original removed: .tmp\build\heihei-goland = True
Destination: Desktop/heihe
2026-10-28 21:09:50 +08:00
```

## E031 — 迁移后的静态核查

build.ps1移除原repoRoot推导，将buildRoot直接基于pluginRoot；README改为在项目根调用build.ps1，.gitignore加入/.tmp/。必需两Java源码、五资源和dev-records存在，旧四目录均不存在；未执行构建或测试。核查输出：

```text
Build script static parse: OK
Required source/resources/development records: present
Old repository plugin directories: 0
Release package SHA256: 5FFFE40D60F5A1020D7DC0B256E307E4820F2F7CD65157645C8BBDB53D2358D9
Build/test execution: none
2026-10-28 21:11:45 +08:00
```
