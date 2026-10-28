# 0.1.2 主题就绪后显示：最小方案

## 结论与业务场景

用户在已正常浏览的暗色小黑盒窗口点击刷新。当前 `HeiHeiBrowserPanel.java:82` 始终把 browser 组件放在可见中心；`:207-214` 等主文档 loadEnd 才调用 applyTheme；`:300` 的 executeJavaScript 又是异步提交。因而新文档可以先用原站浅色绘制，再由主题改暗，形成已确认白闪时窗。

采用两张原生 CardLayout 卡片：深色加载/错误卡与现有 browser 卡。初次创建和刷新先隐藏 browser；其它导航仅在实际主 frame 开始加载新文档时隐藏，主题处理和样式计算完成后，只接收固定 ready 信号并切回 browser。使用已有 JCEF，不引入 OSR、遮挡层、任意 sleep、持续轮询、重试或缓存。

## 本机 241 API 原始依据

对本机 `lib/app-client.jar` / `lib/lib-client.jar` 的公开方法描述符只读核对：

```text
JBCefBrowserBase.createImmediately ()V
CefBrowser.createImmediately ()V
JBCefJSQuery.create (Lcom/intellij/ui/jcef/JBCefBrowserBase;)Lcom/intellij/ui/jcef/JBCefJSQuery;
JBCefJSQuery.create (Lcom/intellij/ui/jcef/JBCefBrowser;)Lcom/intellij/ui/jcef/JBCefJSQuery;
JBCefJSQuery.addHandler (Ljava/util/function/Function;)V
JBCefJSQuery.inject (Ljava/lang/String;)Ljava/lang/String;
JBCefJSQuery.inject (Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
JBCefJSQuery.removeHandler (Ljava/util/function/Function;)V
JBCefJSQuery.dispose ()V
```

Query 生命周期并非需要插件另行注册：构造器 `(JBCefBrowserBase, JSQueryFunc)` 的 bytecode@58 为 `aload_1`，@59 调用常量池 #33，@62 为 `aload_0`，@63 调用 `Disposer.register(Disposable,Disposable)`。#33 实际解析为 `com/intellij/ui/jcef/JBCefBrowserBase.getJBCefClient ()Lcom/intellij/ui/jcef/JBCefClient;`。因此 factory 已建立 query 的释放父级；**不要再次 Disposer.register(panel, query) 或重复手动 dispose**。本插件使用默认 client，它随 browser 释放，仍由现有 Content → panel → browser 所有权收敛。

## 隐藏创建与显示条件

优先调用公开包装 `browser.createImmediately()`，在注册好 query/handler 后、首次导航前调用；不能依赖隐藏的 Swing 卡片首次 addNotify 才创建浏览器。

官方 JetBrains/jcef 源 `CefBrowserWr.java:319-326`：createImmediately 将创建投递到 EDT，并调用 `createBrowserIfRequired(false)`；注释明确浏览器先创建，Java 父窗口可用后再建立父子关系。地址：
https://github.com/JetBrains/jcef/blob/master/java/org/cef/browser/CefBrowserWr.java

本地 241 已验证入口存在；官方 master 的源码说明用于解释入口用途，不伪称为已取得 241 native 全部实现或运行验证。尝试获取 IntelliJ 241 Java 源的指定 tag URL 返回 404，未用其他版本签名冒充本机 API。

**不等待 requestAnimationFrame**。MDN 明确后台或隐藏上下文中的回调可能暂停：https://developer.mozilla.org/en-US/docs/Web/API/Window/requestAnimationFrame 。如果先隐藏 browser，又要求其 RAF 后才发送 ready，会形成“显示等待 ready、ready 等待可见”的相互等待。普通 Swing 轻量 JPanel 覆盖 native browser 也不能当作可靠遮挡层；本方案不走该路径。

## 可判定实现约束

1. 创建 browser 卡时先显示深色加载卡，query 仅注册一个 handler；回调只接受当前代次的固定 ready/error 信号，不接收 URL、文件路径或任意 Java 操作请求。
2. 仅初次创建与确定重载的刷新入口在 EDT 预先显示加载卡。前后退、首页及本站 popup 不预先进入等待，因为它们可能只切换同文档历史或 fragment；实际新文档统一由主 frame onLoadStart 显示加载卡并使旧 ready 失效。不得为同文档导航等待不存在的 loadEnd。
3. 用一个最小导航代次关联当前加载与 ready。query 回调返回 EDT 后再次比较代次与 disposed；旧文档的迟到信号不能显示正在加载的新页面。
4. 本站主 frame loadEnd 后应用主题。现有 style id 分支不得继续只 update+return；它也必须完成本轮 ready，否则重复应用或 LAF 重注入会留下遮挡。
5. JS 先更新主题、改写已存在的可访问 CSS 规则，再通过 getComputedStyle(document.documentElement).backgroundColor 触发样式计算，随后发送 ready。隐藏阶段不等待 RAF，也不把 document.fonts.ready 作为颜色就绪的门槛。
6. 如等待 stylesheet，只等待能确认仍在途的请求，并在监听后复查状态；load 和 error 都要收敛。不能将所有 `link.sheet === null` 解释为“仍在加载”：已失败的 link 可能早已触发 error，再等未来事件会永久停留加载卡。onLoadEnd 后已完成文档中的失败 link 应作为已结算处理；后续新样式继续使用原事件适配，无需扫描帖子 DOM。
7. 非本站 HTTP(S) 主页面不执行本站主题，应在 loadEnd 明确显示，以保留登录跳转；不能等一个永远不会注入的本站 query。不是把本站 CSS 扩展到第三方域名。
8. 资源缺失、JS 异常/error 信号、实际主页面加载失败，都进入深色错误卡并保留原生刷新/首页/系统浏览器按钮。不能通过失败直接展示未主题化页面，也不能只留下“正在加载”。ERR_ABORTED 的旧导航取消不能覆盖新导航状态。
9. LAF 切换仍只更新配色；所有 Swing 卡片更新在 EDT，销毁标记沿用既有 volatile 字段，不增设线程、锁或持久化状态。
10. 打包通用名 heihe、241 GoLand/IDEA 范围及发布物脱敏由 root 的独立工作负责。本方案不扩大 IDE 版本范围，稳定 plugin id 保持覆盖升级。

## 失败场景审视

- 用户刷新后立刻再点首页：旧页主题脚本稍后返回 ready；若不核对代次，会过早显示尚未变暗的新首页。守卫必须在最终 EDT 显示点执行。
- 用户进入帖子时某 CSS 请求已失败：若插件发现 sheet=null 后只订阅未来 load，会无后续事件而一直加载。当前文档完成状态和 load/error 双出口需要共同成立。
- 用户跳去第三方登录页：不注入本站主题是正确边界，但必须有独立显示出口；否则原站正常跳转变成不可见的登录页面。
- 用户在加载时关闭项目：query 由已建立的 Disposer 父级释放；已投递的 UI 回调仍检查 disposed，避免操作销毁卡片。

## [规则冲突检查]

1. P0：本 agent 未执行编译/测试、未创建测试资产；全部 SDK 核对为只读 class 解析，符合（10/10）。
2. 用户已授权最小“准备好再显示”流程：只加入可见状态和固定就绪信号，不提供数据 API、账户机制或重试，符合（9/10）。
3. 当前插件导航与生命周期已有明确边界：保持外站登录出口、Content 所有权和原生按钮；query 自动注册不重复接管，符合（9/10）。
4. 原角色预算最多七次工具：最后一次合并常量池单点确认与本报告写入；无进一步外部探查，符合（10/10）。

## [隐含假设审计]

- 241 API 存在：本机签名已验证（10/10）。
- query 是否已自动注册：本机构造器调用和实参字节码已验证（10/10）。
- createImmediately 用于先建后挂父窗口：本机入口+官方 JCEF 源码说明，未运行隐藏窗口创建（8/10）。
- 隐藏页 RAF 一定会暂停：不作此绝对声明；官方允许暂停已足以排除它作为可靠显示门槛（9/10）。
- 首次 native 展示是否完全无可见闪帧：未运行，不以静态方案保证实际截图结果（7/10，交付边界）。

## [副作用扫描]

- 浏览器：页面加载/登录本身保留，仅显示受准备信号控制；当前代次避免跨导航信号混用。
- UI：loading/error 两种文本复用一张深色卡，原有工具栏始终可操作。
- JS：fixed ready/error bridge 不暴露本机数据；原有样式事件处理保持无轮询。
- 其它仓库模块：无代码改动，仅本报告，不涉及游戏服务端。

## [置信度校准]

上述规格静态可行性不低于 8/10，实际首帧体验为明确未运行边界；不再用 RAF 或固定延时掩盖尚未验证的显示时序。不存在需要扩大业务方案的低置信度决策。

## 修订记录与关联风险

初稿“隐藏 browser 后等两次 RAF 再 ready”已撤回：隐藏上下文可能不执行 RAF；新规格使用实际样式处理结束与强制计算，不依赖重绘回调。初稿“query 注册到 panel/browser”收敛为“不重复注册”，因为本机 factory 已建立 disposer 父级。

后续 08 独立审查确认原规格“多个本地导航入口统一预遮挡”有缺陷：CEF 同文档 fragments/history state 不调用 frame start/end，会永久等待。当前条款 2 已修订为仅初次、刷新及实际主 frame start 进入等待，并同步检查首页和 popup；详见 `08_ready_review.md`。此处保留修订依据，不再要求实现回到旧行为。

本轮泛化检查仅覆盖相关执行入口：本地工具栏导航、原站 onLoadStart、本站/外站 loadEnd、主题新建/已有分支、失败和销毁出口。没有确认范围外既有缺陷；无需扩大到全站或游戏模块。

## STATE_PROPOSAL

- T09：最小方案与 241 API 核对完成；采用 CardLayout、createImmediately、固定 ready/error 与代次守卫，不采用 RAF/任意延时。
- T10：按以上可判定约束实施后续增量，独立复核聚焦生命周期、旧信号及失败出口。
- Evidence：补本机 query 构造器自动 Disposer 注册、createImmediately 与 JSQuery API；首次运行视觉仍未验证。
