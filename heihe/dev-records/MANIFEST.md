# 小黑盒 GoLand 插件

状态：completed，全部149个插件开发文件已从游戏仓库迁至桌面heihe；源码位于项目根、记录位于dev-records、历史产物位于.tmp/build。构建路径已独立，原四目录已移除。0.1.6发布包不变。
编排：plugin_architect 定规格并续跑复核；plugin_developer 写 Java；主对话负责网页主题、打包和交付。
P0：禁止大范围测试/构建和任何测试资产。最新用户明确要求完成安装包并放桌面，执行范围仅本插件一个 Java 包的两个源文件定向编译，读取已编译 SDK，不构建任何依赖或游戏模块。

## Gate

定位已确定；单独新增工具模块；浏览效果需运行确认。独立审查覆盖生命周期、241 API、导航和样式；实际安装由用户操作。

## Instances

- A：/root/plugin_architect
- B：/root/plugin_developer

基线：0a6cf6ace535454ecd80721cbdca1b6919616ad1。仅审查新增目录，不包含既有用户 Diff。
