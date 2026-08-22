import DOMPurify from 'dompurify'
import { marked } from 'marked'

marked.setOptions({
  gfm: true,
  // 旧商品描述是纯文本：单个换行也渲染为换行，保证自动兼容
  breaks: true,
})

/**
 * 把商品描述渲染为安全的 HTML。
 * 输入经过 DOMPurify 消毒，只保留文本、图片、链接等常规标签。
 */
export function renderMarkdown(source?: string | null): string {
  if (!source || !source.trim()) {
    return ''
  }
  const html = marked.parse(source, { async: false })
  return DOMPurify.sanitize(html)
}
