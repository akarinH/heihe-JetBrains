# Handoff

- 已确认当前进程是 D:/goland/GoLand 2024.1.6/bin/goland64.exe。
- 源码目标 tools/heihei-goland，任务账本在当前目录。
- 普通 exec、node_repl、cua 初始化失败，require_escalated 的只读 PowerShell 成功。
- 已完成架构/API 核对、主题 CSS/JS 和 build.ps1、README。Java 由 plugin_developer 实现。
- 最新用户明确要求可从磁盘导入的插件 ZIP 放桌面；执行范围为单 Java package 两源文件定向编译打包，严禁游戏构建/依赖编译/测试/新增测试资产。
- 下一步：完成实现 → 续跑 plugin_architect 静态审查 → 单插件编译 → 检查 ZIP/JAR 结构 → 复制桌面。
- 不重读完整 product-info.json 或全仓 git status，输出太大。

## Handoff - 构建与桌面交付

- Java 与紧凑工具栏已完成；plugin id cn.local.heihei、version0.1.0、toolWindow id“小黑盒”。
- 首次单插件编译成功，ZIP/JAR 含完整资源与 Java17 class。
- 桌面已交付 C:/Users/honghenghui/Desktop/heihei-goland-0.1.0.zip，14898 bytes；SHA256 与 dist 一致，原始依据 BUILD.md。
- 独立审查已通过，toolbar 定点复核已合并，04_review_report.md:8；无修复请求、路过发现或待裁决项。
- 不再编译或测试；用户从磁盘安装后确认网页实际显示/登录。未自动安装或重启 GoLand。
- 索引已查无对应游戏模块，独立工具维护入口 README；无需改既有游戏索引。
- 时间账见 TIMELINE.md：部分早期时间未精确采样，未伪造总墙钟或主动工作耗时；构建工具报告1.43s。

全部交付任务已完成。最终回复提供桌面 ZIP 与安装入口；不声称页面/登录运行验收通过。

## Handoff - 0.1.1 白块与渐变修复

- 用户实际截图显示导航、分类渐变、游戏标签、详情上下遮罩和 Swing 按钮白框。
- 根因与原文已写 DARK_FIX.md：导航组件私有变量；分页白色渐变被旧通用逻辑跳过；tag 与4种滚动遮罩直接写行内浅色值；原生 JButton 边框独立于网页 CSS。
- 新增定向 CSS 覆盖，不修改通用 theme.js，不改变图片、导航、登录和浏览器生命周期；Java 仅平面按钮外观。
- 版本已升0.1.1，README更新。05_dark_review.md:3 独立增量静态审查通过，无阻塞/待裁决。
- 构建首次通过，包内CSS与源码相等。桌面 C:/Users/honghenghui/Desktop/heihei-goland-0.1.1.zip，15761bytes，SHA256 E593A5FB9A37BF2FFD2465B93B0AAF79C099752EF49A973E09BC7822E5691C1A。
- 原版ZIP保留；未自动安装/重启。用户从磁盘导入0.1.1后需重启，让旧浏览窗口重新加载资源。
- 浏览器工具仍 sandbox helper 初始化失败，没有进行新版本实机视觉复核；不得把静态审查或编译成功说成截图已验证。
- 依旧无测试或测试资产，无游戏模块构建。知识库：独立IDE工具无现有游戏模块索引需改，README已同步。时间明细见 TIMELINE.md。

## Handoff - 0.1.2 heihe 发布

- 当前版本0.1.2，插件展示名heihe，桌面固定文件名heihe.zip；稳定id不变，IDEA/GoLand兼容范围仍241。
- 刷新/初次/实际新文档用暗色CardLayout卡片遮挡；当前主题处理和样式计算完成通过ready:代次信号展示。错误保留dark错误卡。query归默认client生命周期，无本地数据API。
- 同文档SPA历史不会触发loadEnd：仅刷新预遮挡，其余真实导航靠onLoadStart，06方案已修正，08独立静态审查通过。
- 源码SDK机器目录已清理；构建-g:none、资源白名单4项、ZIP/JAR条目固定时间。07最终复核读取当前0.1.2：5class+4资源、所有debug属性0、敏感模式无命中、归档extra/comment0。
- 桌面heihe.zip 16934bytes，SHA256 3753BBA9687BA76E5F34F36591C2113F148055758484834AA8C684FBBDBA0712，与审计包一致；全部资源与源码逐字一致。交付依据RELEASE_0.1.2.md。
- 首次构建编译成功但.NET归档设时间顺序失败；改CreateEntry先设时间再写内容后第二次构建成功。无测试或新增测试资产，无其它模块构建。
- 未自动安装/重启；用户从磁盘导入新包后重启。实际首帧视觉/IDEA运行未验证，不能把静态审计说成运行验收。
- README维护入口已同步，独立工具无既有游戏模块索引需改；无待裁决事项。时间明细见TIMELINE。

## Handoff - 0.1.3 控件与遮罩

- 最新0.1.3，桌面heihe.zip已替换；移除浮动、BrowserUtil及外站打开入口，工厂同步无参Panel。
- JS普通竖向wheel1.25倍，保护input/select/textarea/contenteditable、ctrl/meta/shift/deltaX与已preventDefault事件，按嵌套区域及overscroll边界处理。
- 新正式资源web/scrollbars.css，固定14px槽、thumb可见4→10px、corner暗色。只根视口画同值遮罩gradient，内部bar由DOMmask遮一次。
- native contentArea DimPanel遮toolbar/status/loading，始终排除正在显示browser矩形，browser用固定DOM黑mask；slider在外侧，0正常、100隐藏pageArea并整块原生遮黑，CardLayout状态不变可恢复。
- 每HTTP(S) loadEnd先applyShade，通用scrollbar资源注入一次，站外登录页也保持当前强度；既有就绪门禁和隐私边界保留。
- 独立09静态审查与10最终包脱敏通过。6class5资源，debug属性0/元数据固定/敏感模式无命中。
- 最终heihe.zip 19542bytes，SHA256 965F9E29BD6C47FE469CEDC712B8AF16F86DBC85CFCE62F52432E2B507D57ABA。原始交付输出RELEASE_0.1.3.md；包资源与源码、桌面哈希均一致。
- 单包两个源文件构建，静态语法解析通过；无测试/新增测试资产。未自动安装/重启，实际滚动和视觉未运行验证。
- README已同步，独立工具无游戏模块索引需改。无待裁决项。

## Handoff - 0.1.4 面板灰色遮罩

- 遮罩改为UIUtil.getPanelBackground取当前IDE面板色，Web实色/RGBA和Java绘制同源；Laf变化在EDT同步更新。
- 0透明、100隐藏/覆盖和恢复逻辑未变，其它0.1.3功能保留。11_gray_review.md定点审查通过。
- 单包两个Java文件构建成功，内嵌JS静态解析通过，无测试或测试资产。最终包6class5资源、隐私模式/debug0、资源相等、元数据固定。
- 桌面heihe.zip已更新0.1.4，19651bytes，SHA256 A2E9B84849434D1DA4387D079C50226357EAEEB43B85EA03BB9CEE7C3FCB6E89。原始输出RELEASE_0.1.4.md。
- 未自动安装/重启或视觉复核。README已同步，独立工具无现有游戏模块索引需改，无待裁决项。

## Handoff - 0.1.5 导航与滚动体验

- 最新桌面heihe.zip已更新0.1.5：隐藏.nav .publish-btn；四导航按钮启用主题色圆角悬停，禁用按钮不响应；wheelSpeed由1.25改2，既有输入、横向、缩放及嵌套区域保护保留。
- 源码依据和独立复核见12_hover_review.md；原有面板灰遮罩及加载门禁保留。
- Node静态语法检查、单包两个Java源文件构建通过；无测试或测试资产。
- 最终包7class5资源，敏感模式/debug属性0，资源与源码相同、归档元数据固定；桌面副本SHA256一致。
- 20776bytes，SHA256 775CB47E1FD82B93ADFFBB2CBA7E51693A5DE3E0B21EBB81F565566DE9FC0C18，交付时间2026-10-28 20:06:00 +0800。原始输出RELEASE_0.1.5.md。
- 未自动安装/重启或实机视觉验证。README已同步，独立插件无相关游戏模块索引需改，无待裁决项。

## Handoff - 0.1.6 顶栏与窄屏轮播

- 最新桌面文件为heihe-0.1.6.zip；旧heihe.zip被其他程序占用，无法覆盖，未干预进程。最终副本哈希一致。
- theme.css:81只隐藏nav-links的第3到7个nav-link，原站中文数组前两项首页/社区保留，nav-actions登录独立；14_nav_review.md定点复核通过。
- theme.css:132-139在<=460px恢复原有轮播两箭头display:block与transform:none。根因原站media隐藏，JS仍创建/绑定两按钮；13_carousel_analysis.md给出候选、原文、反证与独立静态通过。
- 此轮打包资源变化仅theme.css与plugin.xml版本号，Java/theme.js/scrollbars.css未改；README已同步。原有加载门禁、灰色遮罩与2倍滚动保留。
- 同包两个Java源定向编译一次成功；未运行测试/新增测试资产。最终7class5资源、敏感模式/debug属性0、资源源码相等、元数据固定。
- 21026bytes，SHA256 5FFFE40D60F5A1020D7DC0B256E307E4820F2F7CD65157645C8BBDB53D2358D9；2026-10-28 20:38:48 +0800。详细交付与原始输出RELEASE_0.1.6.md。
- 未自动安装/重启或实机验证；独立工具无游戏模块索引需改，无待裁决项。

## Handoff - 桌面独立项目迁移

- 后续唯一开发根：桌面heihe。源码src、脚本build.ps1、说明README.md、安装包dist在项目根；全部历史开发记录移至dev-records。
- 历史两代构建目录归新项目.tmp/build/{heihe,heihei-goland}，149文件迁移前后大小及SHA256逐项相同；原游戏仓库四目录已移除，未动其它文件或Go缓存。
- build.ps1不再推导repoRoot，未来临时产物写项目自身.tmp/build/heihe；独立操作说明和.tmp忽略规则已同步。
- 仅静态解析、存在性与哈希核查，未构建或测试；dist/heihe.zip保持0.1.6，SHA256 5FFFE40D60F5A1020D7DC0B256E307E4820F2F7CD65157645C8BBDB53D2358D9。
- 原路径引用仅属历史记录；当前布局、命令与迁移原始输出见MIGRATION.md、E030/E031。无需修改游戏模块索引；无待裁决项。
