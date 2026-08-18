/**
 * LaTeX 数学公式渲染工具
 * <p>用于在 v-html 中渲染题目题干/选项/答案/解析里的数学公式。</p>
 * <p>依赖 index.html 中全局加载的 KaTeX（CDN），未加载时退化为 $...$ 包裹显示。</p>
 */

/**
 * 渲染 LaTeX 数学公式（用 v-html 方式）
 * @param {string} text 原始文本（可能包含 $...$ / $$...$$ 包裹的 LaTeX）
 * @returns {string} 渲染后的 HTML
 */
export function renderLatex(text) {
  if (!text) return ''

  // 优先处理 $...$ / $$...$$ 包裹的数学公式：数学段用 KaTeX 渲染，文本段原样保留
  // 必须先于反斜杠命令判断，避免 $ f(x) = x^2 $ 这类无命令的公式漏出 $ 符号
  if (/\$/.test(text)) {
    const parts = text.split(/(\$\$[^$]*\$\$|\$[^$]*\$)/g)
    let result = ''
    for (const part of parts) {
      const dm = part.match(/^\$\$([\s\S]*)\$\$$/)
      const im = !dm && part.match(/^\$([\s\S]*)\$$/)
      if (dm || im) {
        const math = dm ? dm[1] : im[1]
        if (typeof katex !== 'undefined') {
          try {
            result += katex.renderToString(math, { throwOnError: false, displayMode: !!dm })
            continue
          } catch {}
        }
        result += part
        continue
      }
      // 文本段（如含 LaTeX 命令，继续走分段渲染，保证旧题兼容；若仍有不成对 $ 则原样保留防递归）
      result += part.includes('$') ? part : renderLatex(part)
    }
    return result
  }

  // 没有 $ 包裹时：若文本中没有 LaTeX 命令，直接返回原文本
  if (!/\\[a-zA-Z{(]/.test(text)) return text

  // 策略：只按中文字符和中文标点分割，保留英文空格在片段内
  const segments = []
  let current = ''
  for (let i = 0; i < text.length; i++) {
    const ch = text[i]
    const code = ch.charCodeAt(0)
    // 判断是否是中文或中文标点
    const isChinese = (code >= 0x4e00 && code <= 0x9fff) ||
                      ch === '，' || ch === '。' || ch === '；' || ch === '：' ||
                      ch === '！' || ch === '？' || ch === '、' || ch === '…' ||
                      ch === '（' || ch === '）' || ch === '【' || ch === '】' ||
                      ch === '「' || ch === '」' || ch === '『' || ch === '』'

    if (isChinese) {
      if (current) {
        segments.push(current)
        current = ''
      }
      segments.push(ch)
    } else {
      current += ch
    }
  }
  if (current) {
    segments.push(current)
  }

  // 对每个片段，如果包含 LaTeX 命令则用 KaTeX 渲染
  let result = ''
  for (const seg of segments) {
    if (/\\[a-zA-Z{(]/.test(seg)) {
      // 包含 LaTeX，尝试用 KaTeX 渲染
      if (typeof katex !== 'undefined') {
        try {
          result += katex.renderToString(seg, { throwOnError: false, displayMode: false })
        } catch {
          result += '$' + seg + '$'
        }
      } else {
        // KaTeX 未加载，用 $ 包裹显示
        result += '$' + seg + '$'
      }
    } else {
      result += seg
    }
  }

  return result
}
