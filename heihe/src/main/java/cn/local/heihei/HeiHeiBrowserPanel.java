package cn.local.heihei;

import com.intellij.ide.ui.LafManagerListener;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.util.Disposer;
import com.intellij.ui.jcef.JBCefApp;
import com.intellij.ui.jcef.JBCefBrowser;
import com.intellij.ui.jcef.JBCefBrowserBase;
import com.intellij.ui.jcef.JBCefJSQuery;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;
import org.cef.browser.CefBrowser;
import org.cef.browser.CefFrame;
import org.cef.handler.CefLifeSpanHandlerAdapter;
import org.cef.handler.CefLoadHandler;
import org.cef.handler.CefLoadHandlerAdapter;
import org.cef.handler.CefRequestHandlerAdapter;
import org.cef.network.CefRequest;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.AlphaComposite;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.Area;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/** 提供原站浏览、原生导航和限定于小黑盒页面的暗色外观。 */
final class HeiHeiBrowserPanel extends JPanel implements Disposable {
    private static final Logger LOG = Logger.getInstance(HeiHeiBrowserPanel.class);
    private static final String HOME_URL = "https://www.xiaoheihe.cn/app/bbs/home";
    /** 加载期间使用稳定暗色，避免尚未完成主题处理的网页先露出白底。 */
    private static final Color LOADING_BACKGROUND = new Color(0x2b2b2b);
    /** 卡片名仅用于当前面板的两种显示状态。 */
    private static final String LOADING_CARD = "loading";
    /** 网页仅在当前导航的主题就绪后展示。 */
    private static final String BROWSER_CARD = "browser";
    /** 遮罩按百分比调节，100 表示所有插件内容完全被面板色覆盖。 */
    private static final int MAX_SHADE = 100;

    /** 由面板生命周期管理的浏览器；运行时不支持 JCEF 时为空。 */
    private final JBCefBrowser browser;
    /** 网页只可报告本次主题就绪或失败，不提供本地数据读取接口。 */
    private final JBCefJSQuery themeReadyQuery;
    /** 隐藏加载中的原生浏览组件，保留独立的深色提示卡片。 */
    private final CardLayout pageLayout = new CardLayout();
    /** 浏览器与加载提示共用内容区域，工具栏始终可操作。 */
    private final JPanel pageArea = new JPanel(pageLayout);
    /** 网页、工具栏和状态共同参与遮罩，底部控制条位于此区域之外。 */
    private final JPanel contentArea = new DimPanel();
    /** 当前窗口的遮罩百分比，只在 EDT 调整，不持久化。 */
    private int shadeValue;
    /** 深色加载或错误提示，避免失败时放行未处理的浅色页面。 */
    private final JLabel loadingLabel = new JLabel("正在准备暗色页面…", SwingConstants.CENTER);
    /** 仅在 EDT 递增，旧导航的就绪信号不能展示新导航中的页面。 */
    private long navigationId;
    /** 只显示简洁加载状态，不回显可能携带登录信息的完整网址。 */
    private final JLabel status = new JLabel(" ");
    /** 导航按钮随浏览器历史状态更新。 */
    private final JButton backButton = new ToolbarButton("←");
    /** 导航按钮随浏览器历史状态更新。 */
    private final JButton forwardButton = new ToolbarButton("→");
    /** 原始主题资源在创建时读取，切主题时只替换颜色。 */
    private String themeCss = "";
    /** 注入脚本不提供网页到 Java 或本机数据的桥接。 */
    private String themeScript = "";
    /** 预编码的共用滚动条样式，登录跳转等站外页面也使用同一遮罩颜色。 */
    private String scrollbarCss = "";
    /** JCEF 回调可能晚于内容释放，跨线程只读取此销毁标记。 */
    private volatile boolean disposed;

    /** 首次显示时建立浏览器，并把遮罩控制放在始终可恢复的位置。 */
    HeiHeiBrowserPanel() {
        super(new BorderLayout());
        browser = JBCefApp.isSupported() ? new JBCefBrowser() : null;
        // query 自动归浏览器 client 管理，只接收显示状态信号。
        themeReadyQuery = browser == null ? null : JBCefJSQuery.create((JBCefBrowserBase) browser);
        contentArea.add(createToolbar(), BorderLayout.NORTH);
        contentArea.add(createStatusBar(), BorderLayout.SOUTH);
        add(contentArea, BorderLayout.CENTER);
        add(createShadeControl(), BorderLayout.SOUTH);
        loadingLabel.setOpaque(true);
        loadingLabel.setBackground(LOADING_BACKGROUND);
        loadingLabel.setForeground(Color.LIGHT_GRAY);
        loadingLabel.setBorder(JBUI.Borders.empty(16));
        pageArea.add(loadingLabel, LOADING_CARD);
        contentArea.add(pageArea, BorderLayout.CENTER);
        if (browser == null) {
            loadingLabel.setText("当前 IDE 运行时不支持内置浏览器。");
            status.setText("内置浏览器不可用");
            return;
        }

        Disposer.register(this, browser);
        browser.getComponent().setBackground(LOADING_BACKGROUND);
        pageArea.add(browser.getComponent(), BROWSER_CARD);
        themeReadyQuery.addHandler(signal -> {
            SwingUtilities.invokeLater(() -> onThemeSignal(signal));
            return null;
        });
        try {
            themeCss = readResource("/web/theme.css");
            themeScript = readResource("/web/theme.js");
            scrollbarCss = Base64.getEncoder().encodeToString(readResource("/web/scrollbars.css").getBytes(StandardCharsets.UTF_8));
        } catch (IOException exception) {
            LOG.warn("HeiHei 主题资源读取失败", exception);
            status.setText("主题资源读取失败，请重新安装插件");
        }
        registerHandlers();
        ApplicationManager.getApplication().getMessageBus().connect(this)
                .subscribe(LafManagerListener.TOPIC, source -> SwingUtilities.invokeLater(() -> {
                    if (disposed) return;
                    applyShade();
                    applyTheme();
                    contentArea.repaint();
                }));
        // 首次导航也必须经过回调和域名守卫，故在注册完成后加载首页。
        beginLoading();
        // 浏览器起初位于隐藏卡片中，显式创建以免等待首次显示才开始加载。
        browser.createImmediately();
        browser.loadURL(HOME_URL);
    }

    /** 使用 Swing 原生控件提供浏览操作，外观由 IDE 当前主题管理。 */
    private JToolBar createToolbar() {
        JToolBar toolbar = new JToolBar();
        toolbar.setFloatable(false);
        toolbar.setBorder(JBUI.Borders.empty(4));
        // 右侧窄窗口保留全部操作，文字说明放到提示与无障碍名称中。
        backButton.setToolTipText("后退");
        backButton.getAccessibleContext().setAccessibleName("后退");
        backButton.setEnabled(false);
        backButton.addActionListener(event -> {
            if (!disposed && browser != null) {
                // 原站同文档历史切换不会触发 loadEnd，实际换文档时由 onLoadStart 遮挡。
                browser.getCefBrowser().goBack();
            }
        });
        forwardButton.setToolTipText("前进");
        forwardButton.getAccessibleContext().setAccessibleName("前进");
        forwardButton.setEnabled(false);
        forwardButton.addActionListener(event -> {
            if (!disposed && browser != null) {
                browser.getCefBrowser().goForward();
            }
        });
        JButton reloadButton = new ToolbarButton("↻");
        reloadButton.setToolTipText("刷新");
        reloadButton.getAccessibleContext().setAccessibleName("刷新");
        reloadButton.setEnabled(browser != null);
        reloadButton.addActionListener(event -> {
            if (!disposed && browser != null) {
                beginLoading();
                browser.getCefBrowser().reload();
            }
        });
        JButton homeButton = new ToolbarButton("⌂");
        homeButton.setToolTipText("首页");
        homeButton.getAccessibleContext().setAccessibleName("首页");
        homeButton.setEnabled(browser != null);
        homeButton.addActionListener(event -> {
            if (!disposed && browser != null) {
                browser.loadURL(HOME_URL);
            }
        });
        toolbar.add(backButton);
        toolbar.add(forwardButton);
        toolbar.add(reloadButton);
        toolbar.add(homeButton);
        // 默认 Swing 按钮会画亮色立体边框，改用工具栏内的平面按钮外观。
        for (Component component : toolbar.getComponents()) {
            if (component instanceof JButton button) {
                button.setBorderPainted(false);
                button.setContentAreaFilled(false);
                button.setBorder(JBUI.Borders.empty(4, 8));
            }
        }
        return toolbar;
    }

    /** 显示加载与错误信息，状态区与网页一起参与遮罩。 */
    private JPanel createStatusBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBorder(JBUI.Borders.empty(3, 6));
        bar.add(status, BorderLayout.CENTER);
        return bar;
    }

    /** 控制条不参与遮罩，最右完全隐藏后仍可向左恢复。 */
    private JPanel createShadeControl() {
        JPanel control = new JPanel(new BorderLayout(6, 0));
        control.setBorder(JBUI.Borders.empty(4, 8));
        JSlider slider = new JSlider(0, MAX_SHADE, 0);
        slider.setToolTipText("向右加深遮罩，最右完全遮住内容");
        slider.getAccessibleContext().setAccessibleName("遮罩强度");
        JLabel valueLabel = new JLabel("0%");
        slider.addChangeListener(event -> {
            if (disposed || shadeValue == slider.getValue()) return;
            shadeValue = slider.getValue();
            valueLabel.setText(shadeValue + "%");
            // 原生浏览器画布不依赖 Swing 透明混合，100% 时直接隐藏页面区域。
            pageArea.setVisible(shadeValue < MAX_SHADE);
            contentArea.revalidate();
            contentArea.repaint();
            applyShade();
        });
        control.add(new JLabel("遮罩"), BorderLayout.WEST);
        control.add(slider, BorderLayout.CENTER);
        control.add(valueLabel, BorderLayout.EAST);
        return control;
    }

    /** 在默认 JCEF client 上注册浏览回调，资源随浏览器统一释放。 */
    private void registerHandlers() {
        browser.getJBCefClient().addLoadHandler(new CefLoadHandlerAdapter() {
            /** 覆盖网页内导航入口，并使前一页迟到的主题信号失效。 */
            @Override
            public void onLoadStart(CefBrowser cefBrowser, CefFrame frame, CefRequest.TransitionType transitionType) {
                if (!disposed && frame.isMain()) SwingUtilities.invokeLater(() -> {
                    if (!disposed) beginLoading();
                });
            }

            /** 将导航历史状态交回 EDT，避免 JCEF 线程直接改 Swing 控件。 */
            @Override
            public void onLoadingStateChange(CefBrowser cefBrowser, boolean loading, boolean canGoBack, boolean canGoForward) {
                SwingUtilities.invokeLater(() -> {
                    if (disposed) return;
                    backButton.setEnabled(canGoBack);
                    forwardButton.setEnabled(canGoForward);
                    if (loading) {
                        status.setText("正在加载…");
                    }
                });
            }

            /** 主文档完成后更新加载提示并应用本站主题，子框架不重复注入。 */
            @Override
            public void onLoadEnd(CefBrowser cefBrowser, CefFrame frame, int httpStatusCode) {
                if (disposed || !frame.isMain() || !isWebUrl(frame.getURL())) return;
                SwingUtilities.invokeLater(() -> {
                    if (disposed) return;
                    if (httpStatusCode > 0) {
                        status.setText(httpStatusCode >= 400 ? "页面返回 HTTP " + httpStatusCode + "，可尝试刷新" : " ");
                    }
                    applyShade();
                    if (isSiteUrl(cefBrowser.getURL())) applyTheme();
                    else pageLayout.show(pageArea, BROWSER_CARD);
                });
            }

            /** 仅报告主页面真实加载失败，正常取消导航不作为错误展示。 */
            @Override
            public void onLoadError(CefBrowser cefBrowser, CefFrame frame, CefLoadHandler.ErrorCode errorCode,
                                    String errorText, String failedUrl) {
                if (disposed || !frame.isMain() || errorCode == CefLoadHandler.ErrorCode.ERR_ABORTED) return;
                // 网址和错误描述可能携带登录参数，日志只记录可诊断的错误码。
                LOG.warn("HeiHei 主页面加载失败，错误码：" + errorCode);
                SwingUtilities.invokeLater(() -> {
                    if (!disposed) showLoadingError("页面加载失败（" + errorCode + "），请刷新或返回首页");
                });
            }
        }, browser.getCefBrowser());

        browser.getJBCefClient().addRequestHandler(new CefRequestHandlerAdapter() {
            /** 保留普通网页和登录跳转，禁止网页导航触发自定义系统协议。 */
            @Override
            public boolean onBeforeBrowse(CefBrowser cefBrowser, CefFrame frame, CefRequest request,
                                          boolean userGesture, boolean redirect) {
                if (disposed) return true;
                String url = request.getURL();
                if (isWebUrl(url) || "about:blank".equals(url)) return false;
                if (frame.isMain()) showStatus("已阻止非网页链接");
                return true;
            }
        }, browser.getCefBrowser());

        browser.getJBCefClient().addLifeSpanHandler(new CefLifeSpanHandlerAdapter() {
            /** 本站帖子弹窗复用当前窗口，站外弹窗不再提供外部浏览器入口。 */
            @Override
            public boolean onBeforePopup(CefBrowser cefBrowser, CefFrame frame, String targetUrl, String targetFrameName) {
                if (disposed) return true;
                if (isSiteUrl(targetUrl)) {
                    SwingUtilities.invokeLater(() -> {
                        if (!disposed) {
                            browser.loadURL(targetUrl);
                        }
                    });
                } else {
                    showStatus("暂不支持站外弹窗");
                }
                return true;
            }
        }, browser.getCefBrowser());
    }

    /** 将简洁诊断提示切回 EDT，并忽略已释放面板的迟到回调。 */
    private void showStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            if (!disposed) status.setText(message);
        });
    }

    /** 在触发原生导航前先遮住网页，刷新期间始终呈现暗色。 */
    private void beginLoading() {
        navigationId++;
        loadingLabel.setText("正在准备暗色页面…");
        pageLayout.show(pageArea, LOADING_CARD);
    }

    /** 保留可操作工具栏，主题或页面失败时不展示未完成的浅色页面。 */
    private void showLoadingError(String message) {
        loadingLabel.setText(message);
        status.setText(message);
        pageLayout.show(pageArea, LOADING_CARD);
    }

    /** 仅接收当前导航的固定信号，网页不能借此请求本地文件或项目数据。 */
    private void onThemeSignal(String signal) {
        if (disposed || browser == null || !isSiteUrl(browser.getCefBrowser().getURL())) return;
        if (("ready:" + navigationId).equals(signal)) pageLayout.show(pageArea, BROWSER_CARD);
        else if (("error:" + navigationId).equals(signal)) showLoadingError("暗色页面准备失败，请刷新或返回首页");
    }

    /** 网页和滚动条使用当前IDE面板色，与原生控件遮罩保持一致。 */
    private void applyShade() {
        if (disposed || browser == null || !isWebUrl(browser.getCefBrowser().getURL())) return;
        Color shadeColor = UIUtil.getPanelBackground();
        String shadeRgb = shadeColor.getRed() + "," + shadeColor.getGreen() + "," + shadeColor.getBlue();
        String opacity = Double.toString(shadeValue / (double) MAX_SHADE);
        String script = """
                (() => {
                  const root = document.documentElement;
                  if (!root) return;
                  root.style.setProperty('--hh-dim-color', 'rgba(%s,%s)');
                  if (!document.getElementById('heihe-scrollbars')) {
                    const style = document.createElement('style');
                    style.id = 'heihe-scrollbars';
                    style.textContent = new TextDecoder().decode(Uint8Array.from(atob('%s'), char => char.charCodeAt(0)));
                    (document.head || root).appendChild(style);
                  }
                  let mask = document.getElementById('heihe-panel-mask');
                  if (!mask) {
                    mask = document.createElement('div');
                    mask.id = 'heihe-panel-mask';
                    mask.setAttribute('aria-hidden', 'true');
                    mask.style.cssText = 'position:fixed!important;inset:0!important;pointer-events:none!important;z-index:2147483647!important';
                    root.appendChild(mask);
                  }
                  mask.style.setProperty('background-color', '%s', 'important');
                  mask.style.setProperty('opacity', '%s', 'important');
                })();
                """.formatted(shadeRgb, opacity, scrollbarCss, colorHex(shadeColor, "#2b2b2b"), opacity);
        browser.getCefBrowser().executeJavaScript(script, "heihe-mask", 0);
    }

    /** 从 IDE 取暗色调色板；浅色 IDE 仍使用 Darcula 网页配色。 */
    private void applyTheme() {
        if (disposed || browser == null) return;
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(this::applyTheme);
            return;
        }
        // 导航可能已经离开触发回调的页面，注入前再次核对当前域名。
        if (!isSiteUrl(browser.getCefBrowser().getURL())) return;
        if (themeCss.isEmpty() || themeScript.isEmpty() || scrollbarCss.isEmpty()) {
            showLoadingError("主题资源读取失败，请重新安装插件");
            return;
        }
        Color ideBackground = UIUtil.getPanelBackground();
        boolean dark = ideBackground.getRed() * 0.299 + ideBackground.getGreen() * 0.587
                + ideBackground.getBlue() * 0.114 < 150;
        String css = themeCss
                .replace("__BG__", colorHex(dark ? UIManager.getColor("EditorPane.background") : null, "#2b2b2b"))
                .replace("__PANEL__", colorHex(dark ? ideBackground : null, "#3c3f41"))
                .replace("__TEXT__", colorHex(dark ? UIUtil.getLabelForeground() : null, "#a9b7c6"))
                .replace("__MUTED__", colorHex(dark ? UIUtil.getContextHelpForeground() : null, "#8b9198"))
                .replace("__BORDER__", colorHex(dark ? UIManager.getColor("Component.borderColor") : null, "#55585c"))
                .replace("__LINK__", colorHex(dark ? UIManager.getColor("Link.activeForeground") : null, "#589df6"));
        String encodedCss = Base64.getEncoder().encodeToString(css.getBytes(StandardCharsets.UTF_8));
        String script = themeScript.replace("__HEIHEI_CSS_BASE64__", encodedCss)
                .replace("__HEIHEI_READY__", themeReadyQuery.inject("'ready:" + navigationId + "'"))
                .replace("__HEIHEI_ERROR__", themeReadyQuery.inject("'error:" + navigationId + "'"));
        browser.getCefBrowser().executeJavaScript(script, "heihei-theme", 0);
    }

    /** 读取随插件打包的 UTF-8 资源，缺失时通过异常交给创建入口提示。 */
    private String readResource(String path) throws IOException {
        try (InputStream stream = HeiHeiBrowserPanel.class.getResourceAsStream(path)) {
            if (stream == null) throw new IOException("Missing plugin resource: " + path);
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** 只处理具备真实主机名的网页地址，排除本地文件和自定义协议。 */
    private boolean isWebUrl(String url) {
        if (url == null) return false;
        try {
            URI uri = new URI(url);
            return ("https".equalsIgnoreCase(uri.getScheme()) || "http".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null;
        } catch (URISyntaxException exception) {
            return false;
        }
    }

    /** 以完整域名边界匹配本站，防止相似域名获得本站样式或弹窗待遇。 */
    private boolean isSiteUrl(String url) {
        if (!isWebUrl(url)) return false;
        String host = URI.create(url).getHost();
        return "xiaoheihe.cn".equalsIgnoreCase(host)
                || host.toLowerCase(java.util.Locale.ROOT).endsWith(".xiaoheihe.cn");
    }

    /** 将 IDE 颜色输出为 CSS 色值，缺失的主题键使用固定暗色后备值。 */
    private String colorHex(Color color, String fallback) {
        return color == null ? fallback : String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
    }

    /** 子浏览器由 Disposer 释放，此处只使迟到的回调和操作失效。 */
    @Override
    public void dispose() {
        disposed = true;
    }

    /** 导航按钮平时保持透明，悬停时显示与IDE操作按钮一致的圆角底色。 */
    private static final class ToolbarButton extends JButton {
        /** 圆角使用逻辑像素，随IDE缩放比例调整。 */
        private static final int CORNER_RADIUS = 6;

        /** 保留原生按钮的状态、提示和点击行为，仅启用悬停绘制。 */
        private ToolbarButton(String text) {
            super(text);
            setOpaque(false);
            setRolloverEnabled(true);
        }

        /** 在文字前绘制主题灰底，禁用按钮不显示悬停高亮。 */
        @Override
        protected void paintComponent(Graphics graphics) {
            if (isEnabled() && getModel().isRollover()) {
                Graphics2D hover = (Graphics2D) graphics.create();
                try {
                    Color color = UIManager.getColor("ActionButton.hoverBackground");
                    hover.setColor(color == null ? UIUtil.getPanelBackground().darker() : color);
                    hover.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int radius = JBUI.scale(CORNER_RADIUS);
                    hover.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
                } finally {
                    hover.dispose();
                }
            }
            super.paintComponent(graphics);
        }
    }

    /** 只绘制原生控件的遮罩，浏览器区域由网页遮罩负责，避免重复加深。 */
    private final class DimPanel extends JPanel {
        /** 内容保持原有上下工具栏及中间网页的布局。 */
        private DimPanel() {
            super(new BorderLayout());
        }

        /** 子控件悬停重绘时从此处起画，防止按钮盖到已经绘制的遮罩上。 */
        @Override
        protected boolean isPaintingOrigin() {
            return true;
        }

        /** 重绘控件后叠加IDE面板色，并排除仍在显示的浏览器区域。 */
        @Override
        protected void paintChildren(Graphics graphics) {
            super.paintChildren(graphics);
            if (shadeValue == 0) return;
            Graphics2D overlay = (Graphics2D) graphics.create();
            try {
                if (browser != null && browser.getComponent().isShowing()) {
                    Rectangle browserBounds = SwingUtilities.convertRectangle(pageArea, browser.getComponent().getBounds(), this);
                    Area area = new Area(new Rectangle(0, 0, getWidth(), getHeight()));
                    area.subtract(new Area(browserBounds));
                    overlay.clip(area);
                }
                overlay.setComposite(AlphaComposite.SrcOver.derive(shadeValue / (float) MAX_SHADE));
                overlay.setColor(UIUtil.getPanelBackground());
                overlay.fillRect(0, 0, getWidth(), getHeight());
            } finally {
                overlay.dispose();
            }
        }
    }
}
