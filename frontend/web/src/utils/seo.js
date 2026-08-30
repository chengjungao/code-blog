// SEO 辅助：动态设置页面 title / description / og 标签 / canonical / JSON-LD

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
 * @param {{title?: string, description?: string, url?: string, image?: string, type?: string}} opts
 */
export function setPageMeta({ title, description, url, image, type }) {
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
    // 同步设置 canonical 标签
    setCanonical(url)
  }
  if (image) {
    upsertMeta('property', 'og:image', image)
  }
  if (type) {
    upsertMeta('property', 'og:type', type)
  } else {
    // 默认 website 类型
    upsertMeta('property', 'og:type', 'website')
  }
}

/**
 * 设置 canonical 链接标签
 * @param {string} url 规范化的绝对 URL
 */
export function setCanonical(url) {
  if (!url) return
  let link = document.querySelector('link[rel="canonical"]')
  if (!link) {
    link = document.createElement('link')
    link.setAttribute('rel', 'canonical')
    document.head.appendChild(link)
  }
  link.setAttribute('href', url)
}

/**
 * 注入 JSON-LD 结构化数据（替换同类型旧节点）
 * @param {string} type JSON-LD 的 @type
 * @param {object} data 结构化数据对象
 */
export function setJsonLd(type, data) {
  if (!data) return
  const payload = { '@context': 'https://schema.org', '@type': type, ...data }
  const id = `jsonld-${type.toLowerCase()}`
  let script = document.getElementById(id)
  if (!script) {
    script = document.createElement('script')
    script.type = 'application/ld+json'
    script.id = id
    document.head.appendChild(script)
  }
  script.textContent = JSON.stringify(payload)
}

/**
 * 移除 JSON-LD 结构化数据
 * @param {string} type JSON-LD 的 @type
 */
export function removeJsonLd(type) {
  const id = `jsonld-${type.toLowerCase()}`
  const script = document.getElementById(id)
  if (script) script.remove()
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
