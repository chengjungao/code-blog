<template>
  <div class="message-page">
    <header class="page-header">
      <span class="page-kicker">Guestbook</span>
      <h1 class="page-title">留言板</h1>
      <p class="page-lead">
        欢迎留下你的想法、建议或问题。留言提交后需要审核通过才会展示，敏感内容会自动过滤。
      </p>
    </header>

    <section class="message-layout">
      <!-- 留言表单 -->
      <div class="message-form-wrap">
        <h2 class="form-title">提交留言</h2>
        <form class="message-form" @submit.prevent="handleSubmit">
          <div class="form-row">
            <label>
              <span>昵称 <em>*</em></span>
              <input v-model="form.nickname" type="text" maxlength="30" placeholder="你的称呼" />
            </label>
          </div>
          <div class="form-row">
            <label>
              <span>邮箱 <em>*</em></span>
              <input v-model="form.email" type="email" placeholder="不会公开展示" />
            </label>
          </div>
          <div class="form-row">
            <label>
              <span>留言内容 <em>*</em></span>
              <textarea
                v-model="form.messageBody"
                rows="4"
                maxlength="500"
                placeholder="说点什么..."
              ></textarea>
              <small class="char-count">{{ form.messageBody.length }}/500</small>
            </label>
          </div>
          <button class="btn primary" type="submit" :disabled="submitting">
            {{ submitting ? '提交中...' : '提交留言' }}
          </button>
        </form>
        <p v-if="submitMsg" :class="['form-msg', submitOk ? 'ok' : 'err']">{{ submitMsg }}</p>
      </div>

      <!-- 留言列表 -->
      <div class="message-list-wrap">
        <div class="list-head">
          <h2 class="form-title">留言列表</h2>
          <span v-if="totalMessages" class="total-count">共 {{ totalMessages }} 条留言</span>
        </div>

        <div v-if="messages.length" class="message-list">
          <article class="message-card" v-for="msg in messages" :key="msg.messageId">
            <div class="msg-avatar" :style="{ background: avatarColor(msg.nickname) }">
              {{ msg.nickname.charAt(0).toUpperCase() }}
            </div>
            <div class="msg-body">
              <div class="msg-meta">
                <strong>{{ msg.nickname }}</strong>
                <span class="msg-time">{{ formatTime(msg.messageCreateTime) }}</span>
              </div>
              <p class="msg-text">{{ msg.messageBody }}</p>
              <div v-if="msg.replyBody" class="msg-reply">
                <span class="reply-label">站长回复：</span>
                <p>{{ msg.replyBody }}</p>
              </div>
            </div>
          </article>
        </div>

        <div v-else-if="!loading" class="empty-state">
          还没有留言，快来抢沙发吧。
        </div>

        <div v-if="totalPage > 1" class="pagination">
          <button class="btn" :disabled="page <= 1" @click="changePage(page - 1)">上一页</button>
          <span class="page-info">{{ page }} / {{ totalPage }}</span>
          <button class="btn" :disabled="page >= totalPage" @click="changePage(page + 1)">下一页</button>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { fetchMessages, submitMessage } from '../api/blog'

const form = ref({
  nickname: '',
  email: '',
  messageBody: ''
})
const submitting = ref(false)
const submitMsg = ref('')
const submitOk = ref(false)
const messages = ref([])
const loading = ref(false)
const page = ref(1)
const totalPage = ref(0)
const totalMessages = ref(0)

const fetchData = async () => {
  loading.value = true
  try {
    const res = await fetchMessages(page.value)
    const d = res.data || {}
    const msgPage = d.messagePage || {}
    messages.value = msgPage.list || []
    totalPage.value = msgPage.totalPage || 0
    totalMessages.value = d.totalMessages || 0
  } catch (e) {
    console.error('加载留言失败', e)
  } finally {
    loading.value = false
  }
}

const handleSubmit = async () => {
  if (!form.value.nickname.trim()) {
    showMsg('请输入昵称', false)
    return
  }
  if (!form.value.email.trim()) {
    showMsg('请输入邮箱', false)
    return
  }
  if (!form.value.messageBody.trim()) {
    showMsg('请输入留言内容', false)
    return
  }
  submitting.value = true
  try {
    const res = await submitMessage(form.value)
    if (res.resultCode === 200) {
      showMsg('留言提交成功，等待审核后展示', true)
      form.value.messageBody = ''
    } else {
      showMsg(res.message || '提交失败', false)
    }
  } catch (e) {
    showMsg('网络错误，请稍后重试', false)
  } finally {
    submitting.value = false
  }
}

const showMsg = (msg, ok) => {
  submitMsg.value = msg
  submitOk.value = ok
  setTimeout(() => { submitMsg.value = '' }, 4000)
}

const changePage = (p) => {
  if (p < 1 || p > totalPage.value) return
  page.value = p
  fetchData()
}

const formatTime = (t) => {
  if (!t) return ''
  return t.replace('T', ' ').substring(0, 16)
}

const avatarColor = (name) => {
  const colors = ['#2e7d32', '#1565c0', '#6a1b9a', '#c62828', '#ef6c00', '#00838f']
  let hash = 0
  for (let i = 0; i < name.length; i++) hash = name.charCodeAt(i) + ((hash << 5) - hash)
  return colors[Math.abs(hash) % colors.length]
}

onMounted(() => fetchData())
</script>

<style scoped>
.message-page {
  max-width: 100%;
  margin: 0 auto;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-lead {
  margin-top: 12px;
  color: var(--color-subtle);
  font-size: 15px;
}

.message-layout {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 32px;
  align-items: start;
}

.message-form-wrap {
  position: sticky;
  top: 20px;
  padding: 24px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: var(--color-surface);
}

.form-title {
  font-size: 18px;
  font-weight: 800;
  margin-bottom: 18px;
}

.message-form {
  display: grid;
  gap: 16px;
}

.form-row label {
  display: grid;
  gap: 6px;
}

.form-row span {
  font-size: 13px;
  font-weight: 600;
  color: var(--color-text);
}

.form-row em {
  color: #c62828;
  font-style: normal;
}

.form-row input,
.form-row textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid var(--color-border);
  border-radius: 6px;
  font-size: 14px;
  font-family: inherit;
  resize: vertical;
}

.form-row input:focus,
.form-row textarea:focus {
  outline: none;
  border-color: var(--color-accent);
  box-shadow: 0 0 0 2px rgba(46, 125, 50, 0.1);
}

.char-count {
  text-align: right;
  font-size: 12px !important;
  font-weight: 400 !important;
  color: var(--color-muted);
}

.form-msg {
  margin-top: 12px;
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 14px;
}

.form-msg.ok {
  background: rgba(46, 125, 50, 0.1);
  color: #2e7d32;
}

.form-msg.err {
  background: rgba(198, 40, 40, 0.1);
  color: #c62828;
}

.list-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}

.total-count {
  font-size: 13px;
  color: var(--color-muted);
}

.message-list {
  display: grid;
  gap: 16px;
}

.message-card {
  display: flex;
  gap: 14px;
  padding: 20px;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  background: #fff;
}

.msg-avatar {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  font-size: 18px;
  font-weight: 800;
}

.msg-body {
  flex: 1;
  min-width: 0;
}

.msg-meta {
  display: flex;
  align-items: baseline;
  gap: 10px;
}

.msg-meta strong {
  font-size: 15px;
  color: var(--color-text);
}

.msg-time {
  font-size: 12px;
  color: var(--color-muted);
}

.msg-text {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.6;
  color: var(--color-text);
  word-wrap: break-word;
}

.msg-reply {
  margin-top: 12px;
  padding: 10px 14px;
  background: var(--color-surface);
  border-radius: 6px;
  border-left: 3px solid var(--color-accent);
}

.reply-label {
  font-size: 12px;
  font-weight: 700;
  color: var(--color-accent);
}

.msg-reply p {
  margin-top: 4px;
  font-size: 14px;
  color: var(--color-text);
}

.empty-state {
  padding: 40px;
  text-align: center;
  border: 1px dashed var(--color-border);
  border-radius: 8px;
  color: var(--color-muted);
  background: var(--color-surface);
}

.pagination {
  margin-top: 24px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.page-info {
  font-size: 14px;
  color: var(--color-subtle);
}

.btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

@media (max-width: 760px) {
  .message-layout {
    grid-template-columns: 1fr;
  }

  .message-form-wrap {
    position: static;
  }
}
</style>
