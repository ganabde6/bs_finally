/**
 * AI 回复富文本渲染:将 AI 返回的 markdown + LaTeX 数学公式转换为可显示的 HTML
 * - 数学公式语法:\(行内\) \[独立公式\] $$独立公式$$ $行内$
 * - 文本语法:markdown(标题/加粗/列表/代码块等)
 *
 * XSS 防护:先用 markdown-it 的 html:false 过滤原始 HTML,
 * 再把数学段提取为占位符,由 KaTeX 渲染为安全的 HTML。
 */
import MarkdownIt from 'markdown-it'
import katex from 'katex'
import 'katex/dist/katex.min.css'

const md = new MarkdownIt({ html: false, linkify: true, breaks: true })

// 数学公式占位符
const MATH_STORE = []
const PLACEHOLDER = '\uE000MATH\uE001' // 私有区字符,不会与正文冲突

/** 提取 LaTeX 数学段并替换为占位符 */
function extractMath(text) {
  MATH_STORE.length = 0
  let result = text
  // 依次匹配 $$...$$、\[...\]、$...$、\(...\)
  const rules = [
    { re: /\$\$([\s\S]+?)\$\$/g, display: true },
    { re: /\\\[([\s\S]+?)\\\]/g, display: true },
    { re: /\$([^\$\n]+?)\$/g, display: false },
    { re: /\\\(([\s\S]+?)\\\)/g, display: false }
  ]
  for (const { re, display } of rules) {
    result = result.replace(re, (_, expr) => {
      const idx = MATH_STORE.length
      MATH_STORE.push({ expr: expr.trim(), display })
      return `${PLACEHOLDER}${idx}${PLACEHOLDER}`
    })
  }
  return result
}

/** 用 KaTeX 渲染占位符(失败时降级显示原始式子) */
function restoreMath(html) {
  return html.replace(new RegExp(`${PLACEHOLDER}(\\d+)${PLACEHOLDER}`, 'g'), (_, i) => {
    const item = MATH_STORE[Number(i)]
    if (!item) return ''
    try {
      return katex.renderToString(item.expr, {
        displayMode: item.display,
        throwOnError: false,
        output: 'html'
      })
    } catch (e) {
      return `<code>${escapeHtml(item.expr)}</code>`
    }
  })
}

function escapeHtml(s) {
  return s.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
}

/**
 * 渲染 AI 回复为 HTML 字符串
 * @param {string} text AI 原始回复(markdown + LaTeX)
 * @returns {string} 可用 v-html 显示的 HTML
 */
export function renderRich(text) {
  if (!text) return ''
  const withPlaceholders = extractMath(text)
  const html = md.render(withPlaceholders)
  return restoreMath(html)
}
