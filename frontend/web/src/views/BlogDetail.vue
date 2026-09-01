<template>
  <div class="blog-detail" v-if="blog">
    <article class="detail-card">
      <h1 class="detail-title">{{ blog.blogTitle }}</h1>
      <div class="detail-meta">
        <span>{{ formatDate(blog.createTime) }}</span>
        <span>{{ blog.commentCount || 0 }} 条评论</span>
        <span>{{ blog.blogViews }} 次阅读</span>
      </div>
      <div class="detail-tags" v-if="blog.blogTags?.length">
        <router-link v-for="tag in blog.blogTags" :key="tag" :to="'/tag/' + tag + '/1'" class="detail-tag">
          #{{ tag }}
        </router-link>
      </div>
      <div ref="contentRef" class="markdown-body" v-html="renderedContent"></div>

      <div class="copyright-notice">
        本站笔记除注明转载/出处外，皆为作者原创；转载时请保留来源说明。
      </div>

      <!-- 评论列表 -->
      <section class="comments-section" id="comments" v-if="comments?.list?.length">
        <h3>评论 ({{ comments.totalCount }})</h3>
        <div class="comment-item" v-for="c in comments.list" :key="c.commentId">
          <img class="comment-avatar" :src="AVATAR" alt="avatar" />
          <div class="comment-body">
            <div class="comment-header">
              <strong>{{ c.commentator }}</strong>
              <span class="comment-time">{{ formatDate(c.commentCreateTime) }}</span>
            </div>
            <p>{{ c.commentBody }}</p>
            <!-- 博主回复 -->
            <div class="reply-box" v-if="c.replyBody">
              <div class="reply-header">
                <strong>{{ config.yourName || '博主' }}</strong>
                <span>{{ formatDate(c.replyCreateTime) }}</span>
              </div>
              <p>{{ c.replyBody }}</p>
            </div>
          </div>
        </div>

        <!-- 评论分页 -->
        <ul class="pagination" v-if="comments.totalPage > 1" style="margin-top: 24px;">
          <li :class="{ disabled: commentPage <= 1 }">
            <a v-if="commentPage > 1" href="#comments" @click.prevent="loadComments(commentPage - 1)">«</a>
            <span v-else>«</span>
          </li>
          <li v-for="p in commentPages" :key="p" :class="{ active: p === commentPage }">
            <a v-if="p !== commentPage" href="#comments" @click.prevent="loadComments(p)">{{ p }}</a>
            <span v-else>{{ p }}</span>
          </li>
          <li :class="{ disabled: commentPage >= comments.totalPage }">
            <a v-if="commentPage < comments.totalPage" href="#comments" @click.prevent="loadComments(commentPage + 1)">»</a>
            <span v-else>»</span>
          </li>
        </ul>
      </section>

      <!-- 评论表单 -->
      <section class="comment-form" v-if="blog.enableComment === 0">
        <h3>添加评论</h3>
        <form @submit.prevent="submitComment">
          <div class="form-row">
            <input v-model="form.commentator" placeholder="* 怎么称呼你" required />
            <input v-model="form.email" type="email" placeholder="* 你的邮箱" required />
          </div>
          <div class="form-row">
            <input v-model="form.websiteUrl" placeholder="你的网站（可不填）" />
            <div class="captcha-row">
              <input v-model="form.verifyCode" placeholder="* 验证码" required />
              <img :src="captchaUrl" class="captcha-img" @click="refreshCaptcha" title="点击刷新" />
            </div>
          </div>
          <textarea v-model="form.commentBody" placeholder="* 请输入评论内容" rows="6" required></textarea>
          <button type="submit" class="submit-btn" :disabled="submitting">{{ submitting ? '提交中...' : '提交评论' }}</button>
        </form>
      </section>
    </article>
  </div>
  <div v-else-if="loading" class="loading-state">笔记加载中...</div>
  <div v-else class="empty-state">笔记不存在或已删除</div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchBlogDetail, fetchPageBySubUrl, submitComment as apiSubmitComment } from '../api/blog'
import { setPageMeta, excerpt, setJsonLd, removeJsonLd } from '../utils/seo'
import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js/lib/core'
import java from 'highlight.js/lib/languages/java'
import python from 'highlight.js/lib/languages/python'
import javascript from 'highlight.js/lib/languages/javascript'
import typescript from 'highlight.js/lib/languages/typescript'
import bash from 'highlight.js/lib/languages/bash'
import sql from 'highlight.js/lib/languages/sql'
import yaml from 'highlight.js/lib/languages/yaml'
import json from 'highlight.js/lib/languages/json'
import xml from 'highlight.js/lib/languages/xml'
import markdown from 'highlight.js/lib/languages/markdown'
import css from 'highlight.js/lib/languages/css'
import scala from 'highlight.js/lib/languages/scala'
import dockerfile from 'highlight.js/lib/languages/dockerfile'
import properties from 'highlight.js/lib/languages/properties'
import 'highlight.js/styles/github-dark.css'
import mermaid from 'mermaid'

// 按需注册常用语言，避免全量打包 190+ 语言导致体积膨胀
hljs.registerLanguage('java', java)
hljs.registerLanguage('python', python)
hljs.registerLanguage('javascript', javascript)
hljs.registerLanguage('js', javascript)
hljs.registerLanguage('typescript', typescript)
hljs.registerLanguage('ts', typescript)
hljs.registerLanguage('bash', bash)
hljs.registerLanguage('shell', bash)
hljs.registerLanguage('sh', bash)
hljs.registerLanguage('sql', sql)
hljs.registerLanguage('yaml', yaml)
hljs.registerLanguage('yml', yaml)
hljs.registerLanguage('json', json)
hljs.registerLanguage('xml', xml)
hljs.registerLanguage('html', xml)
hljs.registerLanguage('markdown', markdown)
hljs.registerLanguage('md', markdown)
hljs.registerLanguage('css', css)
hljs.registerLanguage('scala', scala)
hljs.registerLanguage('dockerfile', dockerfile)
hljs.registerLanguage('properties', properties)
hljs.registerLanguage('ini', properties)

const props = defineProps({ config: { type: Object, default: () => ({}) } })
const route = useRoute()
const router = useRouter()

const md = new MarkdownIt({
  html: true,
  linkify: true,
  breaks: true,
  highlight: function (str, lang) {
    // mermaid 代码块保留 language-mermaid 类名，交给 renderMermaid 渲染成图表
    if (lang === 'mermaid') {
      return '<pre class="mermaid-pre"><code class="language-mermaid">' +
        md.utils.escapeHtml(str) +
        '</code></pre>'
    }
    // 有语言标识且 highlight.js 支持时，做语法高亮
    if (lang && hljs.getLanguage(lang)) {
      try {
        return '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
      } catch (e) { /* 高亮失败则回退到转义纯文本 */ }
    }
    // 无语言或未知语言：转义 HTML 特殊字符，防止注释/标签被误解析
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})
mermaid.initialize({ startOnLoad: false, theme: 'default' })

// 首屏直出：同步读取预渲染嵌入的数据（必须有 blogId 才可信，防止旧文件中的空数据污染）
const preloaded = getPreloadedData()
const blog = ref(preloaded?.blog?.blogId ? preloaded.blog : null)
const comments = ref(preloaded?.blog?.blogId ? preloaded.comments || null : null)
// 无预渲染注入数据时进入加载态（SPA 内导航场景），避免误显示"笔记不存在"
const loading = ref(!preloaded?.blog?.blogId)
const commentPage = ref(1)
const captchaUrl = ref('')
const submitting = ref(false)
const contentRef = ref(null)

const form = ref({
  commentator: '', email: '', websiteUrl: '', verifyCode: '', commentBody: ''
})

const renderedContent = computed(() => {
  if (!blog.value?.blogContent) return ''
  return md.render(blog.value.blogContent)
})

// 深拷贝为普通对象：Vue reactive proxy 经 Puppeteer/CDP 序列化会变成空壳 {}，
// 导致 render.js 提取注入数据时读到无效结构。写入 window.__PRELOADED_DATA__ 前必须去掉 proxy。
const toPlain = (obj) => {
  if (obj === null || obj === undefined) return null
  try { return JSON.parse(JSON.stringify(obj)) } catch (e) { return null }
}

// 渲染 Mermaid 语法图（流程图/时序图等），将 language-mermaid 代码块替换为 SVG
const renderMermaid = async () => {
  await nextTick()
  const el = contentRef.value
  if (!el) return
  const blocks = el.querySelectorAll('pre code.language-mermaid')
  for (let i = 0; i < blocks.length; i++) {
    const code = blocks[i].textContent || ''
    const pre = blocks[i].parentElement
    if (!pre || !code.trim()) continue
    try {
      const { svg } = await mermaid.render('mmd-' + Date.now() + '-' + i, code)
      const wrapper = document.createElement('div')
      wrapper.className = 'mermaid-wrapper'
      wrapper.innerHTML = svg
      pre.replaceWith(wrapper)
    } catch (e) {
      console.error('mermaid 渲染失败', e)
    }
  }
}

watch(renderedContent, () => {
  renderMermaid()
})

const commentPages = computed(() => {
  if (!comments.value) return []
  const pages = []
  const start = Math.max(1, commentPage.value - 2)
  const end = Math.min(comments.value.totalPage, commentPage.value + 2)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

const formatDate = (d) => {
  if (!d) return ''
  const date = new Date(d)
  const pad = n => String(n).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

const AVATAR = 'data:image/svg+xml,' + encodeURIComponent('<svg xmlns="http://www.w3.org/2000/svg" width="40" height="40" viewBox="0 0 40 40"><rect fill="#e8f0ee" width="40" height="40" rx="4"/><circle cx="20" cy="15" r="6" fill="#0f766e"/><path d="M8 36c0-6.6 5.4-12 12-12s12 5.4 12 12" fill="#0f766e"/></svg>')
const refreshCaptcha = () => {
  captchaUrl.value = '/common/kaptcha?d=' + Date.now()
}

const loadComments = async (page) => {
  commentPage.value = page
  try {
    const blogId = route.params.blogId
    const res = await fetchBlogDetail(blogId, page)
    comments.value = res.data?.comments || null
  } catch (e) { console.error(e) }
}

const submitComment = async () => {
  if (submitting.value) return
  submitting.value = true
  try {
    await apiSubmitComment({
      blogId: blog.value.blogId,
      ...form.value
    })
    alert('评论提交成功！')
    form.value = { commentator: '', email: '', websiteUrl: '', verifyCode: '', commentBody: '' }
    refreshCaptcha()
    loadComments(1)
  } catch (e) {
    alert('评论失败: ' + (e.response?.data?.message || '请重试'))
    refreshCaptcha()
  } finally {
    submitting.value = false
  }
}

const loadBlog = async () => {
  loading.value = true
  try {
    // 优先使用预渲染嵌入的数据（避免 API 请求导致的页面闪烁）
    const preloaded = getPreloadedData()
    if (preloaded?.blog?.blogId) {
      blog.value = preloaded.blog
      comments.value = preloaded.comments || null
      // 同步回写，供 render.js 重新预渲染时提取（避免嵌入数据在迭代渲染中丢失）
      window.__PRELOADED_DATA__ = preloaded
      setupSeo()
      return
    }

    const param = route.params.blogId || route.params.subUrl
    let res
    if (route.name === 'Page') {
      res = await fetchPageBySubUrl(param)
    } else {
      res = await fetchBlogDetail(param, 1)
    }
    const data = res.data || {}
    // 兼容两种返回结构：{ blog, comments } 嵌套式 与 博客字段直接平铺式；
    // 错误响应（无 blogId）统一归一为 null，避免空对象被当成有效数据渲染出空卡片
    const blogData = data.blog || data
    blog.value = blogData?.blogId ? blogData : null
    comments.value = data.comments || null
    
    // 仅在数据有效时写回，供 render.js 提取注入；无效时清除，
    // 防止 {blog:null} 假数据污染注入判断（render.js 的 waitForFunction 会因此一直等不到有效数据）
    if (blog.value?.blogId) {
      window.__PRELOADED_DATA__ = { blog: toPlain(blog.value), comments: toPlain(comments.value) }
    } else {
      delete window.__PRELOADED_DATA__
    }
    
    // 博客不存在时跳转到 404 页面
    if (!blog.value || !blog.value.blogId) {
      router.replace({ name: 'NotFound' })
      return
    }
    
    // 如果通过 blogId 访问且文章有自定义路径，重定向到自定义路径
    if (route.name !== 'Page' && blog.value?.blogSubUrl && blog.value.blogSubUrl.trim()) {
      const customPath = '/' + blog.value.blogSubUrl.trim()
      if (customPath !== route.path) {
        router.replace(customPath)
        return
      }
    }
    
    setupSeo()
  } catch (e) { 
    console.error(e)
    // 数据加载失败时清除预渲染数据，防止残留旧数据污染后续渲染
    delete window.__PRELOADED_DATA__
    router.replace({ name: 'NotFound' })
  } finally {
    loading.value = false
  }
}

/**
 * 从预渲染 HTML 中提取嵌入的博客数据（避免闪烁）
 */
function getPreloadedData() {
  const el = document.getElementById('__PRELOADED_DATA__')
  if (!el) return null
  try {
    const data = JSON.parse(el.textContent)
    el.remove() // 读取后移除，防止客户端导航时重复使用
    return data
  } catch (e) {
    return null
  }
}

/**
 * 设置 SEO 元信息和结构化数据
 */
const setupSeo = () => {
  if (!blog.value?.blogTitle) return
  
  const pageUrl = window.location.origin + window.location.pathname
  const description = excerpt(blog.value.blogContent, 150)
  
  setPageMeta({
    title: blog.value.blogTitle,
    description,
    url: pageUrl,
    image: blog.value.blogCoverImage || undefined,
    type: 'article'
  })
  
  const jsonLdData = {
    headline: blog.value.blogTitle,
    description,
    datePublished: blog.value.createTime ? new Date(blog.value.createTime).toISOString() : undefined,
    dateModified: blog.value.updateTime ? new Date(blog.value.updateTime).toISOString() : undefined,
    author: {
      '@type': 'Person',
      name: '程军高',
      url: 'https://www.chengjungao.cn/'
    },
    publisher: {
      '@type': 'Person',
      name: '程军高'
    },
    mainEntityOfPage: {
      '@type': 'WebPage',
      '@id': pageUrl
    },
    inLanguage: 'zh-CN'
  }
  if (blog.value.blogCoverImage) {
    jsonLdData.image = blog.value.blogCoverImage
  }
  if (blog.value.blogCategoryName) {
    jsonLdData.articleSection = blog.value.blogCategoryName
  }
  if (blog.value.blogTags?.length) {
    jsonLdData.keywords = blog.value.blogTags.join(', ')
  }
  setJsonLd('BlogPosting', jsonLdData)
}

watch(() => route.params, () => loadBlog(), { deep: true })
onMounted(() => {
  if (blog.value) {
    // setup 阶段已从嵌入数据同步初始化：只需补 SEO 元信息，无需再请求 API，避免二次渲染闪烁；
    // 同时回写数据供 render.js 提取（嵌入数据在 setup 时已被移除；toPlain 去除 reactive proxy）
    window.__PRELOADED_DATA__ = { blog: toPlain(blog.value), comments: toPlain(comments.value) }
    setupSeo()
  } else {
    loadBlog()
  }
  refreshCaptcha()
})

onUnmounted(() => {
  removeJsonLd('BlogPosting')
})
</script>

<style scoped>
.blog-detail {
  max-width: var(--content-width);
  margin: 0 auto;
}

.detail-card {
  background: var(--color-card);
  border-radius: var(--radius);
  border: 1px solid var(--color-border);
  padding: 48px 44px;
}
.detail-title {
  font-size: 26px;
  font-weight: 700;
  color: #1b2e1b;
  margin-bottom: 16px;
  line-height: 1.4;
}
.detail-meta {
  display: flex;
  gap: 16px;
  color: var(--color-text-secondary);
  font-size: 13px;
  margin-bottom: 12px;
  flex-wrap: wrap;
}
.detail-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
  margin-bottom: 24px;
}
.detail-tag {
  font-size: 13px;
  padding: 2px 10px;
  background: #f1f8e9;
  border-radius: 20px;
  color: var(--color-primary);
}
.detail-tag:hover { background: #c8e6c9; }

.copyright-notice {
  margin-top: 32px;
  padding: 16px;
  background: #f1f8e9;
  border-radius: 8px;
  font-size: 13px;
  color: var(--color-text-secondary);
  line-height: 1.6;
}

/* 评论 */
.comments-section {
  margin-top: 40px;
}
.comments-section h3, .comment-form h3 {
  font-size: 18px;
  margin-bottom: 20px;
  color: var(--color-text);
}
.comment-item {
  display: flex;
  gap: 14px;
  padding: 16px 0;
  border-bottom: 1px solid var(--color-border);
}
.comment-item:last-child { border-bottom: none; }
.comment-avatar {
  width: 44px;
  height: 44px;
  border-radius: 50%;
  flex-shrink: 0;
}
.comment-body { flex: 1; }
.comment-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 6px;
}
.comment-header strong { font-size: 14px; color: var(--color-text); }
.comment-time { font-size: 12px; color: var(--color-text-secondary); }
.comment-body p { font-size: 14px; color: var(--color-text); margin: 0; line-height: 1.6; }

.reply-box {
  margin-top: 10px;
  padding: 12px;
  background: #f1f8e9;
  border-radius: 8px;
  border-left: 3px solid var(--color-primary-light);
}
.reply-header { display: flex; gap: 12px; margin-bottom: 4px; }
.reply-header strong { font-size: 13px; color: var(--color-primary); }
.reply-header span { font-size: 12px; color: var(--color-text-secondary); }
.reply-box p { font-size: 13px; color: var(--color-text-secondary); }

/* 评论表单 */
.comment-form { margin-top: 40px; }
.form-row {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  margin-bottom: 12px;
}
.comment-form input,
.comment-form textarea {
  width: 100%;
  padding: 10px 14px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  font-size: 14px;
  background: #fafffe;
  outline: none;
  transition: border-color 0.2s;
  font-family: inherit;
}
.comment-form input:focus,
.comment-form textarea:focus { border-color: var(--color-primary-light); }
.comment-form textarea { resize: vertical; min-height: 120px; }
.captcha-row { display: flex; gap: 8px; }
.captcha-row input { flex: 1; }
.captcha-img {
  height: 40px;
  border-radius: 8px;
  cursor: pointer;
  border: 1px solid var(--color-border);
}
.submit-btn {
  margin-top: 12px;
  padding: 10px 32px;
  background: var(--color-primary);
  color: #fff;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}
.submit-btn:hover { background: var(--color-accent); }
.submit-btn:disabled { opacity: 0.6; cursor: not-allowed; }

.empty-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--color-text-secondary);
}

.loading-state {
  text-align: center;
  padding: 80px 20px;
  color: var(--color-text-secondary);
  font-size: 14px;
}

@media (max-width: 768px) {
  .detail-card { padding: 24px 16px; }
  .form-row { grid-template-columns: 1fr; }
}

@media (max-width: 1200px) {
  .blog-detail {
    max-width: 100%;
  }
}
</style>
