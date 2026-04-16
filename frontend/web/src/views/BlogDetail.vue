<template>
  <div class="blog-detail" v-if="blog">
    <article class="detail-card">
      <h1 class="detail-title">{{ blog.blogTitle }}</h1>
      <div class="detail-meta">
        <span>📅 {{ formatDate(blog.createTime) }}</span>
        <span>💬 {{ blog.commentCount || 0 }} 条评论</span>
        <span>👁️ {{ blog.blogViews }} 浏览</span>
      </div>
      <div class="detail-tags" v-if="blog.blogTags?.length">
        <router-link v-for="tag in blog.blogTags" :key="tag" :to="'/tag/' + tag + '/1'" class="detail-tag">
          #{{ tag }}
        </router-link>
      </div>
      <div class="markdown-body" v-html="renderedContent"></div>

      <div class="copyright-notice">
        本站文章除注明转载/出处外，皆为作者原创，欢迎转载，但未经作者同意必须保留此段声明。
      </div>

      <!-- 评论列表 -->
      <section class="comments-section" id="comments" v-if="comments?.list?.length">
        <h3>💬 评论 ({{ comments.totalCount }})</h3>
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
        <h3>✏️ 添加评论</h3>
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
  <div v-else class="empty-state">文章不存在或已删除</div>
</template>

<script setup>
import { ref, computed, watch, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { fetchBlogDetail, fetchPageBySubUrl, submitComment as apiSubmitComment } from '../api/blog'
import MarkdownIt from 'markdown-it'

const props = defineProps({ config: { type: Object, default: () => ({}) } })
const route = useRoute()

const md = new MarkdownIt({ html: true, linkify: true, breaks: true })
const blog = ref(null)
const comments = ref(null)
const commentPage = ref(1)
const captchaUrl = ref('')
const submitting = ref(false)

const form = ref({
  commentator: '', email: '', websiteUrl: '', verifyCode: '', commentBody: ''
})

const renderedContent = computed(() => {
  if (!blog.value?.blogContent) return ''
  return md.render(blog.value.blogContent)
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

const AVATAR = '/blog/default/img/avatar.png'
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
  try {
    const param = route.params.blogId || route.params.subUrl
    let res
    if (route.name === 'Page') {
      res = await fetchPageBySubUrl(param)
    } else {
      res = await fetchBlogDetail(param, 1)
    }
    const data = res.data || {}
    blog.value = data.blog || data
    comments.value = data.comments || null
  } catch (e) { console.error(e) }
}

watch(() => route.params, () => loadBlog(), { deep: true })
onMounted(() => {
  loadBlog()
  refreshCaptcha()
})
</script>

<style scoped>
.detail-card {
  background: var(--color-card);
  border-radius: var(--radius);
  border: 1px solid var(--color-border);
  padding: 40px 36px;
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

@media (max-width: 768px) {
  .detail-card { padding: 24px 16px; }
  .form-row { grid-template-columns: 1fr; }
}
</style>
