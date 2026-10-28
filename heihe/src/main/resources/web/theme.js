/* 将原站颜色变量和固定中性色接到 IDE 主题，保留图片、品牌色、登录二维码。 */
(() => {
  'use strict';
  if (!['https:', 'http:'].includes(location.protocol) ||
      !(location.hostname === 'xiaoheihe.cn' || location.hostname.endsWith('.xiaoheihe.cn'))) return;

  /** 隐藏浏览器不会持续绘制，只强制完成样式计算后通知原生界面。 */
  function notifyReady() {
    getComputedStyle(document.documentElement).backgroundColor;
    __HEIHEI_READY__;
  }

  /** 失败通知不带异常文本、页面地址或任何本机数据。 */
  function notifyError() {
    __HEIHEI_ERROR__;
  }

  try {
  const css = new TextDecoder().decode(Uint8Array.from(atob('__HEIHEI_CSS_BASE64__'), c => c.charCodeAt(0)));
  const existing = document.getElementById('heihei-ide-theme');
  if (existing) {
    existing.textContent = css;
    if (existing.dataset.ready === 'true') notifyReady();
    else existing.addEventListener('heihei-ready', notifyReady, { once: true });
    return;
  }

  const theme = document.createElement('style');
  theme.id = 'heihei-ide-theme';
  theme.textContent = css;
  (document.head || document.documentElement).appendChild(theme);

  // 网站有固定十六进制颜色的旧组件；只映射已确认的界面中性色，不遍历帖子 DOM。
  const light = new Set(['#fff', '#ffffff', '#fafbfc', '#fafafa', '#f7f8f9', '#f5f7fa', '#f3f4f5', '#f2f3f5', '#f1f2f3']);
  const dark = new Set(['#000', '#000000', '#14191e', '#303133', '#393d40', '#464b50']);
  const muted = new Set(['#64696e', '#606266', '#8c9196', '#909399', '#c8cdd2', '#a8abb2', '#c0c4cc']);
  const lines = new Set(['#dadde0', '#dcdfe6', '#e4e7ed', '#ebeef5', '#d4d7de', '#cdd0d6']);
  const processed = new WeakSet();
  const pendingStyles = new Set();
  let documentReady = document.readyState === 'complete';
  let notified = false;
  const properties = ['color', 'background', 'background-color', 'border', 'border-color',
    'border-top', 'border-right', 'border-bottom', 'border-left', 'box-shadow'];

  /** 按 CSS 属性语义转换固定中性色，避免把浅色文字当作浅色背景处理。 */
  function mapColor(color, property) {
    let key = color.toLowerCase();
    // CSSOM 会把十六进制颜色规范化为 rgb；半透明遮罩保持原值，避免改变透明度。
    const rgb = /^rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)(?:\s*,\s*([\d.]+))?\s*\)$/i.exec(key);
    if (rgb) {
      if (rgb[4] !== undefined && Number(rgb[4]) !== 1) return color;
      key = '#' + rgb.slice(1, 4).map(channel => Number(channel).toString(16).padStart(2, '0')).join('');
    }
    if (property.startsWith('border') || property === 'box-shadow') {
      return light.has(key) || muted.has(key) || lines.has(key) ? 'var(--hh-border)' : color;
    }
    if (property.startsWith('background')) {
      if (key === '#f7f8f9' || key === '#f2f3f5') return 'var(--hh-bg)';
      return light.has(key) || dark.has(key) ? 'var(--hh-panel)' : color;
    }
    if (dark.has(key)) return 'var(--hh-text)';
    if (muted.has(key)) return 'var(--hh-muted)';
    if (key === '#004b96') return 'var(--hh-link)';
    return color;
  }

  /** 扫描样式规则一次；懒加载帖子详情沿用相同变量，切主题不重新扫描。 */
  function rewriteRules(rules) {
    for (const rule of rules) {
      if (rule.style && rule.selectorText) {
        // 二维码必须保留白底黑码，品牌静态色与视频遮罩也保留原始颜色。
        if (/qrcode|qr-code|captcha|static-color|video|image-cnt/i.test(rule.selectorText)) continue;
        for (const property of properties) {
          const value = rule.style.getPropertyValue(property);
          if (!value || /url\(|gradient\(|var\(/i.test(value)) continue;
          const mapped = value.replace(/#[0-9a-f]{3,8}\b|rgba?\([^)]*\)/gi, color => mapColor(color, property));
          if (mapped !== value) rule.style.setProperty(property, mapped, rule.style.getPropertyPriority(property));
        }
      }
      // 不改 keyframes，避免干扰原站加载和点赞动画。
      if (rule.cssRules && !(rule instanceof CSSKeyframesRule)) rewriteRules(rule.cssRules);
    }
  }

  /** 原站公开 CSS 当前支持 CORS；受保护的第三方样式保持原样。 */
  function rewriteSheet(sheet) {
    if (!sheet || sheet.ownerNode === theme || sheet.ownerNode?.id === 'heihe-scrollbars' || processed.has(sheet)) return;
    try {
      rewriteRules(sheet.cssRules);
      processed.add(sheet);
    } catch (error) {
      processed.add(sheet);
      if (error.name !== 'SecurityError') console.warn('[HeiHei] theme stylesheet:', error.name);
    }
  }

  /** 等文档及已观察到的新样式结束；不等待可能早已失败的旧 link。 */
  function finishTheme() {
    if (notified || !documentReady || pendingStyles.size > 0) return;
    try {
      for (const sheet of document.styleSheets) rewriteSheet(sheet);
      theme.dataset.ready = 'true';
      theme.dispatchEvent(new Event('heihei-ready'));
      notifyReady();
      notified = true;
    } catch (error) {
      notifyError();
    }
  }

  for (const sheet of document.styleSheets) rewriteSheet(sheet);

  // 241 没有公开的 JCEF 滚轮倍率选项，仅适度加快常规竖向滚动。
  const wheelSpeed = 2;
  const defaultLineHeightPx = 20;
  /** 沿事件祖先找到实际可滚动区域，保留输入控件、缩放和横向手势。 */
  document.addEventListener('wheel', function onWheel(event) {
    if (event.defaultPrevented || !event.cancelable || event.ctrlKey || event.metaKey || event.shiftKey ||
        event.deltaX !== 0 || event.deltaY === 0) return;
    const path = event.composedPath();
    if (path.some(node => node instanceof HTMLElement &&
        (node.matches('input, select, textarea') || node.isContentEditable))) return;
    const rootScroller = document.scrollingElement;
    for (const node of path) {
      if (!(node instanceof HTMLElement) || node.scrollHeight <= node.clientHeight) continue;
      const style = getComputedStyle(node);
      if (node !== rootScroller && !/auto|scroll|overlay/.test(style.overflowY)) continue;
      const canScroll = event.deltaY > 0 ? node.scrollTop + node.clientHeight < node.scrollHeight : node.scrollTop > 0;
      if (canScroll) {
        const unit = event.deltaMode === WheelEvent.DOM_DELTA_LINE ?
          parseFloat(style.lineHeight) || defaultLineHeightPx :
          event.deltaMode === WheelEvent.DOM_DELTA_PAGE ? node.clientHeight : 1;
        event.preventDefault();
        node.scrollBy({ top: event.deltaY * unit * wheelSpeed, behavior: 'auto' });
        return;
      }
      // 内层滚动区明确禁止越界串动时，交回原生处理，不滚动背后的帖子。
      if (node !== rootScroller && style.overscrollBehaviorY !== 'auto') return;
    }
  }, { passive: false });

  // SPA 页面切换只处理新加载样式，不轮询页面、不修改帖子内容。
  document.addEventListener('load', function onStylesheetLoad(event) {
    if (event.target instanceof HTMLLinkElement) {
      rewriteSheet(event.target.sheet);
      pendingStyles.delete(event.target);
      finishTheme();
    }
  }, true);
  document.addEventListener('error', function onStylesheetError(event) {
    if (event.target instanceof HTMLLinkElement) {
      pendingStyles.delete(event.target);
      finishTheme();
    }
  }, true);
  window.addEventListener('load', function onDocumentLoaded() {
    documentReady = true;
    finishTheme();
  }, { once: true });
  const observer = new MutationObserver(function onStyleAdded(records) {
    for (const record of records) {
      for (const node of record.removedNodes) pendingStyles.delete(node);
      for (const node of record.addedNodes) {
        if (node instanceof HTMLStyleElement || node instanceof HTMLLinkElement) rewriteSheet(node.sheet);
        // 只跟踪本轮看到的新请求；旧失败 link 不会再次触发 error，不能等它。
        if (!notified && node instanceof HTMLLinkElement && node.relList.contains('stylesheet') &&
            !node.sheet && !node.disabled && (!node.media || matchMedia(node.media).matches)) pendingStyles.add(node);
      }
    }
    finishTheme();
  });
  if (document.head) observer.observe(document.head, { childList: true });
  queueMicrotask(finishTheme);
  } catch (error) {
    notifyError();
  }
})();
