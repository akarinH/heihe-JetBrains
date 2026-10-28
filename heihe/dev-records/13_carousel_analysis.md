---
phase: 13
agent: plugin-developer-carousel-analysis
task: "heihei-goland"
depends_on: []
---

# 窄工具窗口中轮播箭头消失

## 结论与业务场景

**已确认根因是原站 `max-width:460px` 响应式规则直接隐藏多图帖的左右按钮。** 本期 root 添加的局部 CSS 覆盖与根因对应，静态复核通过；没有运行页面、编译或测试。

用户打开含三张图片的帖子时，在宽于 460 CSS px 的网页视口内，把鼠标移入图片区，原站显示左右箭头及当前张数，可以点击箭头翻图。缩窄 IDE 工具窗口后，网页视口跨入原站 460px 断点：原站把计数所在浮层设为可见，却单独将两个按钮设为 `display:none`。因此出现截图中“1/3 和分页小点仍在，箭头消失”的状态。用户失去明显的鼠标箭头入口，但分页小点和原站其它轮播能力并没有因此被删除。

断点针对网页 viewport 的 CSS 像素，不是帖子图片宽度或操作系统截图的物理像素。用户描述约 380px 与进入该分支相符，不把截图外框宽度当作精确断点。

## 原站证据

本 agent 从公开静态资源只读获取以下内容；资源均为压缩单行，准确位置为 `:1`。未访问账户或执行网页交互。

### 窄屏分支直接隐藏按钮

[index-ChemEL1Y.css](https://static.max-c.com/static/heybox/webapp/@heybox-spa/website/app/index-ChemEL1Y.css):1，关键原文：

```css
@media only screen and (max-width:460px){
  /* 同一原始 media 块的相关规则 */
  .image-text__header-image .header-image__container .header-image__hover-container{opacity:1}
  .image-text__header-image .header-image__container .header-image__hover-container .header-image__left-btn,
  .image-text__header-image .header-image__container .header-image__hover-container .header-image__right-btn{display:none}
}
```

上面仅重新排版并省略不相关的 border-radius 规则，没有改写关键声明。计数和按钮位于同一个 hover-container，但只有按钮被 display:none，直接解释两者在窄屏下表现不同。

### 按钮仍被渲染并绑定翻页

[index-BTtUlkqO.js](https://static.max-c.com/static/heybox/webapp/@heybox-spa/website/app/index-BTtUlkqO.js):1，`header-image` 组件只有图片数条件 `t(v).length>1`，随后在同一个分支创建 indicator 和两个 button；左按钮 `onClick:k`、右按钮 `onClick:L`。原始调用：

```javascript
function k(){S("left")}function L(){S("right")}
```

箭头的 `forbidden` 类分别由 `is_forbided_left` / `is_forbided_right` 控制。没有根据视口宽度销毁按钮的渲染分支。该组件还以 `ResizeObserver` 更新图片尺寸并重置 Swiper，但不会据此移除箭头 DOM。分页参数仍为 `pagination:{el:".header-image-pagination",clickable:!0}`，因此无需重建按钮或改 Swiper。

## 两个以上候选根因与排除

| 候选 | 支持证据 | 关键反证/排除方式 | 结论与置信度 |
|---|---|---|---|
| A：原站窄屏 media 隐藏箭头 | 原站在 <=460px 设两按钮 display:none，计数浮层 opacity:1；与用户看到的计数仍在、箭头不在完全对应 | JS 仍一起创建两按钮与计数，排除 DOM 被删除 | 已确认根因，10/10 |
| B：仅鼠标没有 hover 导致箭头暂时隐藏 | 宽屏默认 hover-container opacity:0，鼠标进入后才 opacity:1 | 窄屏分支强制 opacity:1；即使 hover，display:none 仍然生效。单纯 hover 不能解释同浮层内计数可见而按钮不可见 | 排除为本次主因，9/10 |
| C：插件暗色映射或窄布局误删/覆盖按钮 | 插件的确调整窄窗口布局和样式颜色 | 修复前 theme.css 无轮播按钮规则；theme.js:42 的变更属性只有颜色、背景、边框、阴影，:73-77 只对这些属性 setProperty，不改 display/opacity/transform。原站规则已能完整解释现象 | 排除为隐藏主因，9/10 |

不能把原站响应式设计本身泛化为网站缺陷：本问题是桌面 IDE 的窄视口继续使用鼠标，而原站窄屏交互选择隐藏箭头，二者不匹配。

## 按钮布局、hover 与最小修复

同一原站 CSS:1 的基础布局已核实：两按钮 `position:absolute`、`top:calc(50% - 12px)`、`padding:6px`、`pointer-events:all`；内部 `.hb-icon` 是 `display:block` 的 12×12px 图标。左按钮 `left:8px;transform:translate(-16px)`，右按钮 `right:8px;transform:translate(16px)`。容器 hover 时两者被改为 `transform:translate(0)`。

[index-BWnev_g1.css](https://static.max-c.com/static/heybox/webapp/@heybox-spa/website/app/index-BWnev_g1.css):1 的全局原文是 `button{border-radius:0;border:none;background:none;cursor:pointer}`。结合按钮局部 padding 和内部图标，视觉按钮为 24×24px；原站还用 `:after` 创建居中的 60×60px 点击扩展区域。本次都应保持。

恢复 `display:block` 足够：按钮已有绝对定位及 block 图标，不需要新增 flex 布局。仅恢复 display 还不完整，因为基础左右 transform 仍各把按钮向边缘外移动 16px，而外层 `overflow:hidden` 会裁掉部分按钮；窄屏同时设 `transform:none` 才可保持完整可见。

窄屏 hover-container 原本就有 `opacity:1`，无需另加透明度覆盖。父浮层 `pointer-events:none` 与按钮 `pointer-events:all` 是原站既有鼠标路径，无需改。不能顺便移除 `forbidden`、重绑点击或强制循环翻页，否则会改变原站首尾边界。

## root 实施后的定点复核

实际新增位置：`tools/heihei-goland/src/main/resources/web/theme.css:132-139`。

```css
@media (max-width: 460px) {
  .image-text__header-image .header-image__container .header-image__hover-container .header-image__left-btn,
  .image-text__header-image .header-image__container .header-image__hover-container .header-image__right-btn {
    display: block !important;
    transform: none !important;
  }
}
```

覆盖原站同一断点下的 display:none，并避免边缘裁切；不改变 >460px 的 hover 行为，不影响单图帖是否创建控件，也不修改原站点击处理、分页、图像尺寸、透明度和首尾禁用状态。`README.md:29` 已说明窄屏可用鼠标箭头翻图。

本 agent 没有写上述生产 CSS，只读复核 root 的修改。静态结论是选择器、断点和属性与已确认根因对应；没有把它表述成实机截图已验证。

## 修订记录、路过发现与关联风险

修订：初始将“hover 未触发”列为候选；取得同一 media 块同时 `opacity:1` 与箭头 `display:none` 的证据后，排除其为主因。该修订要求修复保持浮层透明度不动，只覆盖按钮属性。

路过发现：原站通用样式还有 Element Plus 的 `.el-carousel__arrow`，但本帖实际组件是 `.image-text__header-image` + Swiper，两者不是同一入口。没有对全部轮播或全部 button 施加通用覆盖。

关联风险扫描：确认模式是“原站窄视口规则隐藏现有鼠标控件”。在本组件中，左右两个 sibling 同属一条规则，均已覆盖；indicator 与 pagination 不被该 display:none 命中，保持原样。没有证据需要修改帖子外的轮播、导航或页面 JS。顶部入口由其他 agent 处理，本分析没有扩入该范围。

## [规则冲突检查]

1. P0 禁止编译、测试和测试资产：只读取插件文件和公开静态资源，未运行网页、构建或写测试文件，符合（10/10）。
2. 委派禁止生产写入：只写此分析报告；生产修复由 root 完成，本 agent 只读复核，符合（10/10）。
3. P1 要求至少两个候选和完整业务场景：列出三个候选、关键反证和置信度，确认结果由原站 CSS 与 JS 共同支撑，符合（10/10）。
4. 修改范围最小：仅建议现有两个按钮的窄屏显示与 transform，不改变业务流程、按钮事件或首尾状态，符合（10/10）。

## [隐含假设审计]

- 用户多图帖属于 header-image 组件：用户描述的“1/3、分页点”与该组件 DOM、计数模板和响应式规则一致；root 采信相同截图对应关系（9/10）。
- 当前线上资源规则仍存在：本轮直接 HTTP 获取对应公开资源，取得实际原文（10/10）。
- CSS 已加载时局部覆盖能恢复按钮：选择器与 !important 优先级成立；具体运行画面未执行确认（9/10）。
- 鼠标入口恢复不会重写 Swiper 行为：未改 JS，沿用现有 button handlers 与 forbidden 类（10/10）。

## [副作用扫描]

只读检索原站三个相关资源和本地 theme.css/theme.js；无账户操作、下载持久化、脚本执行或生产修改。所建议 CSS 限定一个组件和 <=460px，不影响宽屏入口、普通按钮或游戏仓库模块。

## [置信度校准]

根因为 10/10，组件与截图对应为 9/10；静态修复范围为 9/10。实机视觉和点击效果明确未验证。没有低于 7 或需要新增业务机制的决策。

## [STATE_PROPOSAL]

- 轮播缺箭头根因已确认：公开 CSS 的 <=460px 响应式 display:none，JS 保留 DOM 和处理器。
- root 的 theme.css:132-139 定向修复静态复核通过，README.md:29 已更新。
- 不运行编译/测试；最终单插件打包与安装后的界面确认由主线程按授权处理。
