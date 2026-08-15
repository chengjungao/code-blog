// SEO 辅助：动态设置页面 title / description / og 标签（对 Google 等渲染 JS 的爬虫有效）

function upsertMeta(attr, key, content) {
  if (!content) return
  let el = document.querySelector(`meta[${attr}="${key}"]`)
  if (!el) {
    el = document.createElement('meta')
    el.setAttribute(attr, key)
    document.head.appendChild(el)
  }
  el.setAttribute('content', content)
}

/**
 * 设置页面 SEO 元信息
 * @param {{title?: string, description?: string, url?: string}} opts
 */
export function setPageMeta({ title, description, url }) {
  const suffix = '拾光集 — 程军高'
  const fullTitle = title ? `${title} · ${suffix}` : `${suffix} | 搜索架构与 AI 工程`

  document.title = fullTitle
  upsertMeta('name', 'description', description)
  upsertMeta('property', 'og:title', fullTitle)
  upsertMeta('property', 'og:description', description)
  upsertMeta('name', 'twitter:title', fullTitle)
  upsertMeta('name', 'twitter:description', description)
  if (url) {
    upsertMeta('property', 'og:url', url)
  }
}

/**
 * 从 Markdown 提取纯文本摘要
 */
export function excerpt(md, len = 150) {
  if (!md) return ''
  const text = md
    .replace(/```[\s\S]*?```/g, ' ')
    .replace(/`[^`]*`/g, ' ')
    .replace(/!\[[^\]]*\]\([^)]*\)/g, ' ')
    .replace(/\[([^\]]*)\]\([^)]*\)/g, '$1')
    .replace(/[#>*_\-~|]/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
  return text.slice(0, len)
}
