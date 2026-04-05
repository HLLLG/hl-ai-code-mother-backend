import { ref, onBeforeUnmount } from 'vue'

export interface SelectedElement {
  tagName: string
  id: string
  className: string
  textContent: string
  xpath: string
}

export const VISUAL_EDITOR_MESSAGE_TYPE = {
  INJECT: 'visual-editor-inject',
  ELEMENT_SELECTED: 'visual-editor-element-selected',
  EXIT_EDIT_MODE: 'visual-editor-exit',
} as const

/**
 * 生成注入到 iframe 的可视化编辑脚本
 * 在 iframe 内部运行，实现 hover 高亮 + 点击选中 + 通信
 */
const buildInjectionScript = (): string => {
  return `
(function() {
  if (window.__visualEditorInjected) return;
  window.__visualEditorInjected = true;

  var HOVER_OUTLINE = '2px dashed #1890ff';
  var SELECTED_OUTLINE = '2px solid #f5222d';
  var hoveredEl = null;
  var selectedEl = null;

  function getXPath(el) {
    if (!el || el.nodeType !== 1) return '';
    var parts = [];
    while (el && el.nodeType === 1) {
      var idx = 1;
      var sib = el.previousSibling;
      while (sib) {
        if (sib.nodeType === 1 && sib.tagName === el.tagName) idx++;
        sib = sib.previousSibling;
      }
      parts.unshift(el.tagName.toLowerCase() + '[' + idx + ']');
      el = el.parentNode;
    }
    return '/' + parts.join('/');
  }

  function truncate(str, max) {
    if (!str) return '';
    str = str.replace(/\\s+/g, ' ').trim();
    return str.length > max ? str.slice(0, max) + '...' : str;
  }

  function buildElementInfo(el) {
    return {
      tagName: el.tagName.toLowerCase(),
      id: el.id || '',
      className: (typeof el.className === 'string' ? el.className : '').trim(),
      textContent: truncate(el.textContent, 120),
      xpath: getXPath(el)
    };
  }

  document.addEventListener('mouseover', function(e) {
    var target = e.target;
    if (target === document.body || target === document.documentElement) return;
    if (target === selectedEl) return;
    if (hoveredEl && hoveredEl !== selectedEl) {
      hoveredEl.style.outline = '';
    }
    hoveredEl = target;
    target.style.outline = HOVER_OUTLINE;
  }, true);

  document.addEventListener('mouseout', function(e) {
    var target = e.target;
    if (target === selectedEl) return;
    if (target === hoveredEl) {
      target.style.outline = '';
      hoveredEl = null;
    }
  }, true);

  document.addEventListener('click', function(e) {
    e.preventDefault();
    e.stopPropagation();
    var target = e.target;
    if (target === document.body || target === document.documentElement) return;

    if (selectedEl) {
      selectedEl.style.outline = '';
    }
    selectedEl = target;
    target.style.outline = SELECTED_OUTLINE;

    window.parent.postMessage({
      type: '${VISUAL_EDITOR_MESSAGE_TYPE.ELEMENT_SELECTED}',
      payload: buildElementInfo(target)
    }, '*');
  }, true);

  window.addEventListener('message', function(e) {
    if (e.data && e.data.type === '${VISUAL_EDITOR_MESSAGE_TYPE.EXIT_EDIT_MODE}') {
      if (hoveredEl) { hoveredEl.style.outline = ''; hoveredEl = null; }
      if (selectedEl) { selectedEl.style.outline = ''; selectedEl = null; }
      window.__visualEditorInjected = false;
    }
  });
})();
`
}

/**
 * 向 iframe 注入可视化编辑脚本
 */
const injectEditorScript = (iframe: HTMLIFrameElement) => {
  try {
    const iframeDoc = iframe.contentDocument || iframe.contentWindow?.document
    if (!iframeDoc) return false

    const script = iframeDoc.createElement('script')
    script.textContent = buildInjectionScript()
    iframeDoc.head.appendChild(script)
    return true
  } catch (e) {
    console.error('注入可视化编辑脚本失败（可能跨域）:', e)
    return false
  }
}

/**
 * 通知 iframe 退出编辑模式
 */
const notifyIframeExitEditMode = (iframe: HTMLIFrameElement) => {
  try {
    iframe.contentWindow?.postMessage(
      { type: VISUAL_EDITOR_MESSAGE_TYPE.EXIT_EDIT_MODE },
      '*',
    )
  } catch {
    // 跨域时 postMessage 不会抛异常，此处仅作兜底
  }
}

/**
 * 将选中的元素信息格式化为可添加到提示词的文本
 */
export const formatSelectedElementsForPrompt = (elements: SelectedElement[]): string => {
  if (elements.length === 0) return ''

  const lines = elements.map((el, i) => {
    const parts: string[] = [`<${el.tagName}>`]
    if (el.id) parts.push(`id="${el.id}"`)
    if (el.className) parts.push(`class="${el.className}"`)
    if (el.textContent) parts.push(`文本内容: "${el.textContent}"`)
    parts.push(`路径: ${el.xpath}`)
    return `  ${i + 1}. ${parts.join(', ')}`
  })

  return `\n\n[用户选中的页面元素]:\n${lines.join('\n')}\n请针对以上选中的元素进行修改。`
}

/**
 * 格式化单个元素的简要描述（用于 alert 展示）
 */
export const formatElementLabel = (el: SelectedElement): string => {
  const parts: string[] = [`<${el.tagName}>`]
  if (el.id) parts.push(`#${el.id}`)
  if (el.className) {
    const cls = el.className.split(/\s+/).slice(0, 2).join('.')
    parts.push(`.${cls}`)
  }
  if (el.textContent) {
    const text = el.textContent.length > 30 ? el.textContent.slice(0, 30) + '...' : el.textContent
    parts.push(`"${text}"`)
  }
  return parts.join(' ')
}

/**
 * 可视化编辑 composable
 * 管理编辑模式状态、iframe 注入/通信、选中元素列表
 */
export function useVisualEditor() {
  const isEditMode = ref(false)
  const selectedElements = ref<SelectedElement[]>([])

  let currentIframe: HTMLIFrameElement | null = null
  let messageHandler: ((e: MessageEvent) => void) | null = null

  const handleMessage = (e: MessageEvent) => {
    if (e.data?.type !== VISUAL_EDITOR_MESSAGE_TYPE.ELEMENT_SELECTED) return
    const payload = e.data.payload as SelectedElement
    if (!payload?.tagName) return

    const exists = selectedElements.value.some((el) => el.xpath === payload.xpath)
    if (!exists) {
      selectedElements.value = [...selectedElements.value, payload]
    }
  }

  const enterEditMode = (iframe: HTMLIFrameElement) => {
    if (isEditMode.value) return

    currentIframe = iframe
    isEditMode.value = true

    injectEditorScript(iframe)

    messageHandler = handleMessage
    window.addEventListener('message', messageHandler)
  }

  const exitEditMode = () => {
    if (!isEditMode.value) return

    if (currentIframe) {
      notifyIframeExitEditMode(currentIframe)
      currentIframe = null
    }

    if (messageHandler) {
      window.removeEventListener('message', messageHandler)
      messageHandler = null
    }

    isEditMode.value = false
    selectedElements.value = []
  }

  const removeSelectedElement = (index: number) => {
    selectedElements.value = selectedElements.value.filter((_, i) => i !== index)
  }

  const clearSelectedElements = () => {
    selectedElements.value = []
  }

  onBeforeUnmount(() => {
    exitEditMode()
  })

  return {
    isEditMode,
    selectedElements,
    enterEditMode,
    exitEditMode,
    removeSelectedElement,
    clearSelectedElements,
    formatSelectedElementsForPrompt,
    formatElementLabel,
  }
}
