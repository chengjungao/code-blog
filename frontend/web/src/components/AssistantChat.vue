<template>
  <div class="assistant-widget">
    <!-- 浮动按钮 -->
    <button
      v-show="!open"
      class="chat-fab"
      aria-label="打开智能分身"
      @click="open = true"
    >
      <img src="/robot.png" class="chat-fab-icon" alt="智能分身" />
      <span class="fab-pulse"></span>
    </button>

    <!-- 聊天面板 -->
    <transition name="chat-slide">
      <div v-show="open" class="chat-panel">
        <!-- 头部 -->
        <header class="chat-header">
          <div class="chat-header-info">
            <div class="chat-avatar">
              <span>CG</span>
            </div>
            <div>
              <strong>智能分身</strong>
              <small>{{ online ? '在线 · 随时聊' : '连接中…' }}</small>
            </div>
          </div>
          <button class="chat-close" aria-label="关闭" @click="open = false">×</button>
        </header>

        <!-- 消息列表 -->
        <div ref="msgList" class="chat-body">
          <!-- 欢迎消息 -->
          <div class="msg msg-assistant">
            <div class="msg-bubble">
              你好！我是军高的智能分身。关于搜索架构、AI 搜索、Agent 或者这个网站，有什么想聊的？
            </div>
          </div>

          <!-- 快捷提问 -->
          <div v-if="messages.length === 0" class="quick-chips">
            <button v-for="chip in quickChips" :key="chip" @click="send(chip)">{{ chip }}</button>
          </div>

          <!-- 对话消息 -->
          <div
            v-for="(msg, i) in messages"
            :key="i"
            class="msg"
            :class="msg.role === 'user' ? 'msg-user' : 'msg-assistant'"
          >
            <div class="msg-bubble">{{ msg.content }}</div>
          </div>

          <!-- 加载指示器 -->
          <div v-if="loading" class="msg msg-assistant">
            <div class="msg-bubble typing">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>

        <!-- 输入区 -->
        <footer class="chat-footer">
          <textarea
            ref="input"
            v-model="text"
            rows="1"
            placeholder="输入消息…"
            maxlength="500"
            @keydown.enter.exact.prevent="send()"
            @input="autoResize"
          ></textarea>
          <button class="send-btn" :disabled="!text.trim() || loading" @click="send()">
            <svg viewBox="0 0 24 24" width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
              <line x1="22" y1="2" x2="11" y2="13" />
              <polygon points="22 2 15 22 11 13 2 9 22 2" />
            </svg>
          </button>
        </footer>
      </div>
    </transition>
  </div>
</template>

<script setup>
import { ref, nextTick, onMounted } from 'vue'
import { assistantChat } from '../api/blog'

const open = ref(false)
const text = ref('')
const loading = ref(false)
const messages = ref([])
const msgList = ref(null)
const input = ref(null)
const online = ref(true)

const quickChips = [
  '你主要做什么技术方向？',
  '你主导的搜索平台有多大量级？',
  '双塔模型是什么？',
  '怎么联系你？'
]

const scrollToBottom = () => {
  nextTick(() => {
    if (msgList.value) {
      msgList.value.scrollTop = msgList.value.scrollHeight
    }
  })
}

const autoResize = () => {
  if (input.value) {
    input.value.style.height = 'auto'
    input.value.style.height = Math.min(input.value.scrollHeight, 120) + 'px'
  }
}

const send = async (preset) => {
  const content = (preset || text.value).trim()
  if (!content || loading.value) return

  // 先取历史（不包含当前消息），再 push 当前消息
  const history = messages.value
    .slice(-10)
    .map(m => ({ role: m.role, content: m.content }))

  messages.value.push({ role: 'user', content })
  text.value = ''
  autoResize()
  scrollToBottom()

  loading.value = true
  try {

    const res = await assistantChat(content, JSON.stringify(history))
    if (res && res.resultCode === 200 && res.data) {
      messages.value.push({ role: 'assistant', content: res.data })
    } else if (res && res.message) {
      // 后端返回的业务错误提示（如限流、参数异常）
      messages.value.push({ role: 'assistant', content: res.message })
    } else {
      messages.value.push({ role: 'assistant', content: '分身暂时开小差了，稍后再试一下吧～' })
    }
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '网络好像有点问题，稍后再试一下吧～' })
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

onMounted(() => {
  online.value = navigator.onLine
})
</script>

<style scoped>
.assistant-widget {
  position: fixed;
  bottom: 24px;
  right: 24px;
  z-index: 9999;
}

/* 浮动按钮 */
.chat-fab {
  position: relative;
  width: 56px;
  height: 56px;
  border: none;
  border-radius: 50%;
  background: var(--color-text);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.chat-fab-icon {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.chat-fab:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 28px rgba(0, 0, 0, 0.22);
}

.fab-pulse {
  position: absolute;
  inset: 0;
  border-radius: 50%;
  border: 2px solid var(--color-text);
  opacity: 0;
  animation: pulse 2.5s ease-out infinite;
}

@keyframes pulse {
  0% { transform: scale(1); opacity: 0.4; }
  100% { transform: scale(1.6); opacity: 0; }
}

/* 聊天面板 */
.chat-panel {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 380px;
  height: 560px;
  max-height: calc(100vh - 48px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 16px;
  box-shadow: 0 12px 48px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}

/* 头部 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 16px;
  background: var(--color-text);
  color: #fff;
  flex-shrink: 0;
}

.chat-header-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.chat-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 1px;
}

.chat-header-info strong {
  font-size: 15px;
  display: block;
  line-height: 1.2;
}

.chat-header-info small {
  font-size: 11px;
  opacity: 0.7;
}

.chat-close {
  background: none;
  border: none;
  color: #fff;
  font-size: 22px;
  cursor: pointer;
  line-height: 1;
  padding: 0;
  opacity: 0.7;
  transition: opacity 0.2s;
}

.chat-close:hover {
  opacity: 1;
}

/* 消息列表 */
.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: var(--color-surface);
}

.chat-body::-webkit-scrollbar {
  width: 4px;
}

.chat-body::-webkit-scrollbar-thumb {
  background: var(--color-border);
  border-radius: 2px;
}

/* 消息气泡 */
.msg {
  display: flex;
  max-width: 85%;
}

.msg-user {
  align-self: flex-end;
}

.msg-assistant {
  align-self: flex-start;
}

.msg-bubble {
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.msg-user .msg-bubble {
  background: var(--color-text);
  color: #fff;
  border-bottom-right-radius: 4px;
}

.msg-assistant .msg-bubble {
  background: #fff;
  color: var(--color-text);
  border: 1px solid var(--color-border);
  border-bottom-left-radius: 4px;
}

/* 快捷提问 */
.quick-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-top: 4px;
}

.quick-chips button {
  padding: 7px 14px;
  border: 1px solid var(--color-border);
  border-radius: 20px;
  background: #fff;
  color: var(--color-subtle);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.quick-chips button:hover {
  border-color: var(--color-text);
  color: var(--color-text);
  background: var(--color-surface);
}

/* 打字动画 */
.typing {
  display: flex;
  gap: 4px;
  align-items: center;
  padding: 14px 18px;
}

.typing span {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--color-muted);
  animation: bounce 1.2s ease-in-out infinite;
}

.typing span:nth-child(2) { animation-delay: 0.2s; }
.typing span:nth-child(3) { animation-delay: 0.4s; }

@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: 0.4; }
  30% { transform: translateY(-6px); opacity: 1; }
}

/* 输入区 */
.chat-footer {
  display: flex;
  align-items: flex-end;
  gap: 8px;
  padding: 12px 14px;
  border-top: 1px solid var(--color-border);
  background: #fff;
  flex-shrink: 0;
}

.chat-footer textarea {
  flex: 1;
  border: 1px solid var(--color-border);
  border-radius: 10px;
  padding: 9px 12px;
  font-size: 14px;
  font-family: inherit;
  resize: none;
  outline: none;
  color: var(--color-text);
  background: var(--color-surface);
  transition: border-color 0.2s;
  max-height: 120px;
  line-height: 1.5;
}

.chat-footer textarea:focus {
  border-color: var(--color-text);
}

.chat-footer textarea::placeholder {
  color: var(--color-muted);
}

.send-btn {
  width: 38px;
  height: 38px;
  border: none;
  border-radius: 10px;
  background: var(--color-text);
  color: #fff;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: opacity 0.2s;
}

.send-btn:disabled {
  opacity: 0.35;
  cursor: not-allowed;
}

.send-btn:not(:disabled):hover {
  opacity: 0.85;
}

/* 面板动画 */
.chat-slide-enter-active,
.chat-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.chat-slide-enter-from,
.chat-slide-leave-to {
  opacity: 0;
  transform: translateY(20px) scale(0.96);
}

/* 移动端 */
@media (max-width: 640px) {
  .assistant-widget {
    bottom: 16px;
    right: 16px;
  }

  .chat-panel {
    width: calc(100vw - 32px);
    height: calc(100vh - 120px);
    border-radius: 14px;
  }
}
</style>
