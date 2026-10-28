# 0.1.1 暗色遗漏增量审查

结论：本轮增量静态审查通过，未发现需要追加修改的阻塞项。仅审查 `theme.css`、按钮外观、版本/说明变更及 root 提供的对应原站证据；未运行编译、测试、浏览器回归或新增测试资产。运行截图效果仍未验证。

## 业务场景与原始依据

root 转述用户实机反馈：社区正文已经变暗，但浏览列表和帖子时，顶部导航、横向分类边缘、帖子头部、游戏标签和上下白条仍与暗色 IDE 明显不协调。默认 Swing 按钮还有亮色边框。这里是若干组件的颜色来源漏处理，不能仅用“主题整体加载失败”解释。

root 已查日志原文：

```text
2026-10-28 18:23:22,715 [115319040]   INFO - #c.i.i.p.DynamicPlugins - Plugin cn.local.heihei loaded without restart in 78 ms
```

这条日志只证明插件已加载，不单独证明每一处主题注入成功。正文已变暗的用户现象与下列具体颜色来源共同支持本次局部修复。

### 游戏标签：已闭合为行内浅色值

本 reviewer 独立只读获取的 `safe-route-BVPxA3TQ.css:1` 原文：

```css
.hb-cpt__content-tag{display:inline-flex;align-items:center;height:24px;margin:0 10px 10px 0;border-radius:3px}
```

old/big/small 规则只补 padding、图标和文字尺寸，未声明白背景。因此“该样式文件有固定白色背景”的初始候选不成立。

root 就近定位到 `safe-route-CiT97g45.js:1` 的实际渲染（原文也已写入 `DARK_FIX.md` 游戏标签补充证据）：

```text
h=R(()=>m("background",e.value.bg_color)),B=R(()=>m("color",e.value.text_color))
return{theme:{light:{[r]:I},dark:{[r]:T}},style:{[r]:I}}
class:F(["hb-cpt__content-tag",["old","big","small"][C(e).style_type||0]]),style:H(h.value.style)
class:"content-tag-text",style:H(B.value.style)
```

具体场景：玩家在暗色帖子卡片看到“影之刃零”等标签；组件虽解析两套颜色，渲染仍将浅色值写进按钮及子文字的行内 style，原有 CSSOM 规则扫描不处理该行内路径，标签继续显示浅色块。`theme.css:97-101` 成对覆盖标签背景和子文字，使用 important 覆盖普通 inline style；不覆盖子图标与 `static-color-suffix-tag` 的品牌后缀。该修复与实际副作用位置对应。

### 帖子头部：必须覆盖两层

本 reviewer 独立抓取 `index-CEV-W59j.css:1`，确认：

```css
.hb-cpt-page-header{position:sticky;top:80px;left:0;z-index:501;background-color:#fff;border-radius:8px 8px 0 0;overflow:hidden}
.hb-cpt-page-header .page-header__container .page-header__container-bg{position:absolute;top:0;left:0;width:100%;height:100%;background-color:#fff;pointer-events:none}
```

用户进入帖子时，即使外层变暗，内部绝对定位背景仍可能盖出白条。`theme.css:88-90` 同时覆盖两层，而非只修最初点名的外层；保留定位、尺寸和交互。

### 导航、渐变与独立遮罩

- 导航：root 的原始规则为 `.nav[data-v-ab0ef0d9]{--nav-background:rgba(255,255,255,.92);...background:var(--nav-background)}`；原脚本跳过变量值，且半透明颜色不做通用改写。root 回传的 `theme.css:60-74` 在 `.nav` 本地覆盖 `--nav-background` 和相关文字/用户区域变量，作用点与局部变量声明相同。
- 分页：本 reviewer 独立确认 `index-7Ok5QYva.css:1` 两侧分别为 `linear-gradient(to right,#fff,#fff,rgba(255,255,255,0))` / `to left`。`theme.css:77-82` 保留左右方向及透明过渡，仅把实色端替换为 panel；未放开对全站 gradient 的通用改写。
- 遮罩：root 在 `index-DpCaELFV.js:1` 取得 `{zIndex:20,maskColor:"#f7f8f9",contentColor:"#ffffff",edgeHeight:8,topBorderRadius:"8px",bottomBorderRadius:"8px",teleport:".layout-normal"}`，创建 `data-mask-frame` 下的四种 data-mask 节点并设置行内 backgroundColor。`theme.css:105-111` 覆盖 top/bottom 的 bg/edge 四条路径；不是臆测的 `.hb-bbs-link` 伪元素，未改 DOM 或遮罩几何。
- emoji 淡出：`theme.css:83-84` 为具体 `.link-reply__emoji-box.blur::after` 使用 panel→transparent，仅修改外观；未扩展到图片、视频或验证码选择器。

## Java 与交付增量

用户点击导航按钮时，网页 CSS 无法改变 Swing 自己绘制的亮色边框。`HeiHeiBrowserPanel.java:155-161` 遍历已存在工具栏组件，仅对 JButton 关闭 border/content-area 绘制并设置空白内边距；原有点击回调、tooltip、无障碍名称和焦点能力不变。root 回传的 `:171-172` 对状态条 externalButton 同步关闭边框和填充，覆盖同类入口。没有新增线程、状态、重试或浏览机制。

root 已同步 plugin.xml 为 0.1.1、README 安装包名，并回传构建与交付证据：新 JAR 中 CSS 与源码相同；桌面 `C:/Users/honghenghui/Desktop/heihei-goland-0.1.1.zip`，`15761 bytes`，SHA256 `E593A5FB9A37BF2FFD2465B93B0AAF79C099752EF49A973E09BC7822E5691C1A`。这些是 root 执行证据，本 reviewer 未重复编译或校验包。

## [规则冲突检查]

1. 用户 P0 禁止本子任务运行测试/编译和新增测试资产：本阶段仅公开资源读取、源码阅读和本报告写入，符合（10/10）。
2. root 明确窄范围局部修复，不扩展架构：已见改动局限 CSS、已有 JButton 外观与版本说明；没有重新设计导航、账户、缓存或生命周期，符合（10/10）。
3. 用户要求结论有业务场景与原文：标签先排除 CSS 背景假设，再以真实行内消费路径确认；头部两层原文独立获取，符合（9/10）。
4. 原查询预算的最后一次读取后，root 明确额外授权一次纯报告写入；没有继续 CDN 探查或新建测试矩阵，符合（10/10）。

## [隐含假设审计]

- header 内层会遮住外层背景：独立原文证明其绝对定位、满宽高、白背景；修复同时覆盖，已验证静态路径（9/10）。
- 标签白底来自 CSS 固定值：已推翻；该 CSS 无背景声明。新的行内浅色来源由 root 取得的原始 JS 闭合（9/10）。
- 四种遮罩均需覆盖：root 原始 helper 与当前四个选择器对应，已核对（9/10）。
- 新版在用户实际 JCEF 中全部显示正确：未验证。静态来源对应和打包成功不能替代运行截图，不作通过声明（7/10，明确验证边界）。

## [副作用扫描]

- 页面：作用范围限定已有导航、分页、帖子标题、标签及布局遮罩；不改变位置/尺寸/请求或帖子 DOM。
- 品牌和媒体：标签背景及正文文字成对覆盖；子图标和品牌后缀保持原样。没有对全站渐变作普遍重写。
- Swing：六个工具栏按钮与状态条外链按钮使用同一外观规则；回调和系统浏览器确认流程不变。
- 游戏仓库：本 agent 未修改生产代码或其它模块，仅写指定 05 报告。

## [置信度校准]

局部修复与已确认颜色来源的静态对应置信度 9/10。没有需要新增业务决策的低置信度修复建议；缺少运行截图仅保留为交付边界，不据此扩大实现。

## 修订记录（第五步回看）

| 旧候选 | 新事实 | 修订与受影响范围 |
|---|---|---|
| 标签白底可能来自 `.hb-cpt__content-tag` 的 CSS | safe-route CSS 无背景；JS 实际写入 light inline style | 修复背景与子文字两处；不再用通用 CSSOM 颜色改写解释标签问题 |
| 底白条可能是 `.hb-bbs-link` 的伪元素 | helper 创建 portal 下 data-mask 节点并赋行内背景 | 改为四类节点的精确覆盖，避免错修外层卡片 |
| 初版静态检查未发现阻塞 | 实机反馈暴露变量/渐变/行内值的覆盖边界 | 不把初版静态通过当作视觉完成；本轮只处理已取得真实来源的组件 |

## 关联风险与路过发现

泛化模式为“颜色来源不是可被原脚本处理的普通规则值”。本轮就近核对同类作用点：头部外/内两层、分页左右、tag 背景/子文字、data-mask 上下 bg/edge、工具栏/状态条按钮，已纳入本次修复。未把这种模式扩展为全站扫色或全仓无关审计；没有发现必须另改的范围外缺陷。

## STATE_PROPOSAL

- 0.1.1 暗色遗漏增量静态审查完成，无新增阻塞。
- 标签根因记为行内 light 样式，遮罩记为独立 data-mask 节点；旧的 CSS 白底/伪元素候选已排除。
- 采用 root 已回传的 0.1.1 构建与桌面包证据；最终交付明确实际视觉和登录回归未运行，不生成测试资产。
