# 0.1.1 暗色遗漏修复

## 场景与候选原因

用户在 GoLand 正常打开社区，列表背景已经变暗，但顶部网站导航仍发白，分类横向列表右侧出现白色遮罩；打开帖子后还能看到浅色标题背景、底部白条，游戏标签有白底。插件工具栏按钮也带亮色矩形边框。这些会在 IDE 暗色背景上形成刺眼色块。

1. 已确认：原站组件私有颜色变量和渐变被现有主题处理跳过。导航变量不在 root 的 --hb-general-* 主题映射里，且 rewriteRules 跳过包含 var/gradient 的值；分页两侧原本就是白到透明的渐变。
2. 已排除作为整体根因：整个主题或插件加载失败。截图中大多数正文已经变暗；日志也记录插件已加载。部分组件漏处理不能据此推断整个注入失败。动态样式具体运行时序尚未证实，不据此引入轮询或全 DOM 观察。
3. 独立已确认：顶部 Swing JButton 默认边框不受网页 CSS 控制。createToolbar 使用默认 JButton，没有调整 bevel 边框；截图中的六个亮框对应六个按钮。

## 原始依据

日志：`C:/Users/honghenghui/AppData/Local/JetBrains/GoLand2024.1/log/idea.log`。

```text
2026-10-28 18:23:22,715 [115319040]   INFO - #c.i.i.p.DynamicPlugins - Plugin cn.local.heihei loaded without restart in 78 ms
```

原站资源基址：`https://static.max-c.com/static/heybox/webapp/@heybox-spa/website/app/`。

`index-BWnev_g1.css`：

```css
.nav[data-v-ab0ef0d9]{--nav-background: rgba(255, 255, 255, .92); /* 同条规则末尾 background:var(--nav-background) */}
```

`index-7Ok5QYva.css`：

```css
.hb-cpt__pagination .hb-cpt__pagination--left{ /* 省略尺寸 */ background:linear-gradient(to right,#fff,#fff,rgba(255,255,255,0)); /* 省略其余 */ }
.hb-cpt__pagination .hb-cpt__pagination--right{ /* 省略尺寸 */ background:linear-gradient(to left,#fff,#fff,rgba(255,255,255,0)); /* 省略其余 */ }
```

`index-CEV-W59j.css`：

```css
.hb-cpt-page-header{position:sticky;top:80px;left:0;z-index:501;background-color:#fff;border-radius:8px 8px 0 0;overflow:hidden}
.hb-cpt-page-header .page-header__container .page-header__container-bg{position:absolute;top:0;left:0;width:100%;height:100%;background-color:#fff;pointer-events:none}
```

## 修改契约

- `web/theme.css`：只对已确认界面组件补局部变量、渐变与背景；同时覆盖详情页标题外层及内层 container-bg。
- `HeiHeiBrowserPanel.createToolbar/createStatusBar`：消除普通 Swing 按钮亮色 bevel 边框，沿用现有按钮与回调。
- `META-INF/plugin.xml` 版本升为 0.1.1，README 更新安装包名。
- 不改变浏览器导航、登录、资源生命周期或帖子图片。保留原有渐变扫描的媒体保护边界，不泛化替换所有渐变。
- 验证：源码与公开 CSS 对照、脚本静态语法、原范围单插件编译、ZIP/JAR 结构与桌面副本一致性。浏览器工具仍因 sandbox helper 初始化失败不可用，不声称截图级视觉复核通过。

## 白条补充证据

帖子详情组件 `index-BTtUlkqO.js` 导入 `index-DpCaELFV.js` 的布局组件。后者的遮罩 helper 默认 `maskColor/contentColor` 为 `#fff`，并调用：

```js
{zIndex:20,maskColor:"#f7f8f9",contentColor:"#ffffff",edgeHeight:8,topBorderRadius:"8px",bottomBorderRadius:"8px",teleport:".layout-normal"}
```

节点原文使用 `data-mask-frame` 容器和 `data-mask="top-bg"/"top-edge"/"bottom-edge"/"bottom-bg"`，`backgroundColor` 由 Vue 的 `style` 直接写入。这说明帖子上下白条的副作用在行内遮罩节点，不在外层卡片的 CSS 声明。补充定向重要样式覆盖其背景，不改定位、尺寸或 DOM。

修订：从“部分样式漏处理”进一步定位到局部变量、渐变和行内遮罩三类具体来源；未把动态加载时序升级成根因。

## 游戏标签补充证据

社区正文 `index.vue_vue_type_style_index_0_lang-SDgJxRwZ.js` 从 `safe-route-CiT97g45.js` 导入标签组件，并通过 `tag:t` 渲染游戏标签。后者原始片段：

```text
h=R(()=>m("background",e.value.bg_color)),B=R(()=>m("color",e.value.text_color))
return{theme:{light:{[r]:I},dark:{[r]:T}},style:{[r]:I}}
class:F(["hb-cpt__content-tag",["old","big","small"][C(e).style_type||0]]),style:H(h.value.style)
class:"content-tag-text",style:H(B.value.style)
```

组件虽解析了 light/dark 两份颜色，实际渲染仍消费 `style` 中浅色值；它们直接出现在按钮和子文字的行内样式里。`safe-route-BVPxA3TQ.css` 仅定义标签布局尺寸，没有白背景声明。因此本次针对 `.hb-cpt__content-tag` 的背景与 `.content-tag-text` 的文字同时覆盖；不改变标签图标与 `static-color-suffix-tag` 品牌后缀。

## 0.1.1 构建与桌面交付

沿用已授权的准确命令：

```powershell
& '.\tools\heihei-goland\build.ps1' -IdeHome 'D:\goland\GoLand 2024.1.6'
```

单插件、同一个 Java 包的两个源文件编译首次成功，退出码 0。原始输出：

```text
Plugin package: D:\Z-game\git_servers\tools\heihei-goland\dist\heihei-goland-0.1.1.zip
Build output: D:\Z-game\git_servers\.tmp\build\heihei-goland\f5a494b177eb4183a3b50abc98704ed5
```

只读归档检查确认入口类、描述文件、图标和主题资源齐全，版本为 0.1.1，包内 theme.css 与修复源码逐字一致。复制桌面后 SHA256 相同，原始输出：

```text
Archive identity/resources/theme: OK (cn.local.heihei 0.1.1)
Desktop: C:\Users\honghenghui\Desktop\heihei-goland-0.1.1.zip
Bytes: 15761
SHA256: E593A5FB9A37BF2FFD2465B93B0AAF79C099752EF49A973E09BC7822E5691C1A
```

没有自动安装或重启 IDE。浏览器工具仍因 Windows sandbox helper 初始化失败不可用，因此不宣称已完成新版本实机视觉复核。未运行任何测试，未新增测试资产。维护说明同步在 tools/heihei-goland/README.md，独立插件无对应现有游戏模块索引需要更新。
