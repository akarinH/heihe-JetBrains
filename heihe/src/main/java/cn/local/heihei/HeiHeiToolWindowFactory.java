package cn.local.heihei;

import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;

/** 在用户首次打开工具窗口时创建浏览器，并交给内容生命周期管理。 */
public final class HeiHeiToolWindowFactory implements ToolWindowFactory, DumbAware {
    /** 把浏览面板挂到原生工具窗口，关闭项目时一并释放浏览器。 */
    @Override
    public void createToolWindowContent(Project project, ToolWindow toolWindow) {
        HeiHeiBrowserPanel panel = new HeiHeiBrowserPanel();
        Content content = ContentFactory.getInstance().createContent(panel, "", false);
        content.setDisposer(panel);
        content.setPreferredFocusableComponent(panel);
        toolWindow.getContentManager().addContent(content);
    }
}
