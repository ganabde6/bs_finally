<template>
  <div class="ai-guide">
    <!-- 对话面板 -->
    <div v-show="open" class="guide-panel">
      <!-- 头部 -->
      <div class="panel-header">
        <svg class="header-avatar" width="28" height="28" viewBox="0 0 72 72">
          <line x1="36" y1="11" x2="36" y2="19" stroke="#9AA0B0" stroke-width="3"/>
          <circle cx="36" cy="9" r="3.5" fill="var(--color-primary)"/>
          <rect x="18" y="19" width="36" height="26" rx="8" fill="#4A4E5A"/>
          <rect x="24" y="25" width="24" height="14" rx="4" fill="#23252E"/>
          <circle cx="31" cy="32" r="3" fill="#7FE3F0"/>
          <circle cx="41" cy="32" r="3" fill="#7FE3F0"/>
          <rect x="13" y="48" width="9" height="15" rx="4.5" fill="#5A5F6E"/>
          <rect x="50" y="48" width="9" height="15" rx="4.5" fill="#5A5F6E"/>
          <rect x="24" y="47" width="24" height="17" rx="6" fill="#5A5F6E"/>
          <rect x="30" y="52" width="12" height="6" rx="2" fill="#8A8FA0"/>
          <rect x="26" y="64" width="9" height="6" rx="2" fill="#4A4E5A"/>
          <rect x="37" y="64" width="9" height="6" rx="2" fill="#4A4E5A"/>
        </svg>
        <div class="header-text">
          <div class="header-title">AI 向导</div>
          <div class="header-sub">网站使用引导助手</div>
        </div>
        <button class="header-close" title="收起" @click="open = false">✕</button>
      </div>

      <!-- 消息区 -->
      <div ref="msgListRef" class="msg-list">
        <div v-for="(msg, i) in messages" :key="i" :class="['msg', 'msg-' + msg.role]">
          <div class="msg-bubble">{{ msg.content }}</div>
        </div>
        <!-- AI 思考中 -->
        <div v-if="loading" class="msg msg-assistant">
          <div class="msg-bubble thinking">
            <span></span><span></span><span></span>
          </div>
        </div>
      </div>

      <!-- 快捷问题(初始状态显示) -->
      <div v-if="messages.length <= 1" class="quick-row">
        <button v-for="q in quickQuestions" :key="q" class="quick-chip" @click="askQuick(q)">{{ q }}</button>
      </div>

      <!-- 输入区 -->
      <div class="input-row">
        <input
          v-model="input"
          class="guide-input"
          type="text"
          placeholder="输入你想问的操作问题…"
          maxlength="500"
          :disabled="loading"
          @keyup.enter="send"
        />
        <button class="send-btn" :disabled="loading || !input.trim()" @click="send">发送</button>
      </div>
    </div>

    <!-- 机器人入口按钮 -->
    <div class="robot-btn-wrap">
      <button class="robot-btn" :title="open ? '收起 AI 向导' : '打开 AI 向导'" @click="open = !open">
        <svg width="40" height="40" viewBox="0 0 72 72">
          <line x1="36" y1="11" x2="36" y2="19" stroke="#9AA0B0" stroke-width="3"/>
          <circle cx="36" cy="9" r="3.5" fill="var(--color-primary)"/>
          <rect x="18" y="19" width="36" height="26" rx="8" fill="#4A4E5A"/>
          <rect x="24" y="25" width="24" height="14" rx="4" fill="#23252E"/>
          <circle cx="31" cy="32" r="3" fill="#7FE3F0"/>
          <circle cx="41" cy="32" r="3" fill="#7FE3F0"/>
          <rect x="13" y="48" width="9" height="15" rx="4.5" fill="#5A5F6E"/>
          <rect x="50" y="48" width="9" height="15" rx="4.5" fill="#5A5F6E"/>
          <rect x="24" y="47" width="24" height="17" rx="6" fill="#5A5F6E"/>
          <rect x="30" y="52" width="12" height="6" rx="2" fill="#8A8FA0"/>
          <rect x="26" y="64" width="9" height="6" rx="2" fill="#4A4E5A"/>
          <rect x="37" y="64" width="9" height="6" rx="2" fill="#4A4E5A"/>
        </svg>
      </button>
      <!-- 未打开时的吸引注意气泡 -->
      <div v-if="!open" class="robot-bubble">
        <span></span><span></span><span></span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, nextTick, watch } from 'vue'
import { guideChat } from '@/api'

const open = ref(false)
const loading = ref(false)
const input = ref('')
const msgListRef = ref(null)

const messages = ref([
  { role: 'assistant', content: '你好！我是智汇云课堂 AI 向导，关于网站的任何功能、操作问题都可以直接问我。' }
])

const quickQuestions = ['网站有什么功能？', '怎么注册账号？', '学生端怎么用？', '教师端怎么用？']

const scrollToBottom = () => {
  nextTick(() => {
    if (msgListRef.value) {
      msgListRef.value.scrollTop = msgListRef.value.scrollHeight
    }
  })
}

watch([() => messages.value.length, loading], scrollToBottom)
watch(open, (v) => { if (v) scrollToBottom() })

const doAsk = async (question) => {
  const q = question.trim()
  if (!q || loading.value) return
  messages.value.push({ role: 'user', content: q })
  input.value = ''
  loading.value = true
  try {
    const res = await guideChat({ question: q })
    const answer = (typeof res.data === 'string' ? res.data : '') || '抱歉，我暂时没有想好怎么回答，换个问法试试？'
    messages.value.push({ role: 'assistant', content: answer })
  } catch (e) {
    messages.value.push({ role: 'assistant', content: '回答暂时失败，请稍后再试。' })
  } finally {
    loading.value = false
  }
}

const send = () => doAsk(input.value)
const askQuick = (q) => doAsk(q)
</script>

<style scoped>
.ai-guide {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 3000;
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 12px;
}

/* ============ 对话面板 ============ */
.guide-panel {
  width: 340px;
  height: 470px;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 14px;
  box-shadow: var(--shadow-lg);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 12px 14px;
  background: linear-gradient(135deg, #2A2D36, #1F2329);
  color: #fff;
  flex-shrink: 0;
}

.header-avatar { flex-shrink: 0; }

.header-text { flex: 1; min-width: 0; }
.header-title { font-size: 14px; font-weight: 600; line-height: 1.3; }
.header-sub { font-size: 11px; color: rgba(255, 255, 255, 0.6); }

.header-close {
  border: none;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
  font-size: 14px;
  cursor: pointer;
  padding: 4px 6px;
  border-radius: 6px;
  line-height: 1;
}
.header-close:hover { background: rgba(255, 255, 255, 0.12); color: #fff; }

/* 消息区 */
.msg-list {
  flex: 1;
  overflow-y: auto;
  padding: 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  background: var(--color-background);
}

.msg { display: flex; }
.msg-user { justify-content: flex-end; }
.msg-assistant { justify-content: flex-start; }

.msg-bubble {
  max-width: 82%;
  padding: 9px 12px;
  border-radius: 12px;
  font-size: 13px;
  line-height: 1.7;
  white-space: pre-wrap;
  word-break: break-word;
}

.msg-user .msg-bubble {
  background: var(--color-primary);
  color: var(--color-on-primary);
  border-bottom-right-radius: 4px;
}

.msg-assistant .msg-bubble {
  background: #fff;
  border: 1px solid var(--color-border);
  color: var(--color-foreground);
  border-bottom-left-radius: 4px;
}

/* AI 思考中气泡 */
.msg-bubble.thinking {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 12px 14px;
}
.msg-bubble.thinking span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: guideBubbleDot 1.4s ease-in-out infinite;
}
.msg-bubble.thinking span:nth-child(2) { animation-delay: 0.2s; }
.msg-bubble.thinking span:nth-child(3) { animation-delay: 0.4s; }

@keyframes guideBubbleDot {
  0%, 100% { opacity: 0.3; transform: translateY(0); }
  50% { opacity: 1; transform: translateY(-2px); }
}

/* 快捷问题 */
.quick-row {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 8px 14px;
  background: var(--color-background);
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
}

.quick-chip {
  border: 1px solid var(--color-primary);
  background: var(--el-color-primary-light-9);
  color: var(--color-primary-dark);
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 14px;
  cursor: pointer;
  transition: all 0.15s;
}
.quick-chip:hover { background: var(--color-primary); color: #fff; }

/* 输入区 */
.input-row {
  display: flex;
  gap: 8px;
  padding: 10px 14px;
  background: #fff;
  border-top: 1px solid var(--color-border);
  flex-shrink: 0;
}

.guide-input {
  flex: 1;
  min-width: 0;
  border: 1px solid var(--color-border);
  border-radius: 8px;
  padding: 8px 12px;
  font-size: 13px;
  color: var(--color-foreground);
  outline: none;
  background: var(--color-background);
  transition: border-color 0.15s;
}
.guide-input:focus { border-color: var(--color-primary); }
.guide-input:disabled { opacity: 0.6; }

.send-btn {
  border: none;
  background: var(--color-primary);
  color: #fff;
  font-size: 13px;
  padding: 0 16px;
  border-radius: 8px;
  cursor: pointer;
  transition: background 0.15s;
  flex-shrink: 0;
}
.send-btn:hover:not(:disabled) { background: var(--color-primary-dark); }
.send-btn:disabled { opacity: 0.5; cursor: not-allowed; }

/* ============ 机器人入口 ============ */
.robot-btn-wrap {
  position: relative;
}

.robot-btn {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  border: 1px solid var(--color-border);
  background: #fff;
  box-shadow: var(--shadow-lg);
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  animation: guideRobotFloat 4.5s ease-in-out infinite;
  transition: transform 0.15s, box-shadow 0.15s;
}
.robot-btn:hover {
  transform: scale(1.06);
  box-shadow: 0 12px 24px rgba(31, 35, 41, 0.18);
}

@keyframes guideRobotFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

/* 吸引注意的思考气泡 */
.robot-bubble {
  position: absolute;
  top: -14px;
  right: 0;
  background: #fff;
  border: 1px solid var(--color-border);
  border-radius: 12px;
  padding: 6px 9px;
  display: flex;
  align-items: center;
  gap: 4px;
  box-shadow: var(--shadow-sm);
  pointer-events: none;
}
.robot-bubble::after {
  content: '';
  position: absolute;
  bottom: -4px;
  right: 12px;
  width: 7px;
  height: 7px;
  background: #fff;
  border-right: 1px solid var(--color-border);
  border-bottom: 1px solid var(--color-border);
  transform: rotate(45deg);
}
.robot-bubble span {
  width: 5px;
  height: 5px;
  border-radius: 50%;
  background: var(--color-primary);
  animation: guideBubbleDot 1.4s ease-in-out infinite;
}
.robot-bubble span:nth-child(2) { animation-delay: 0.2s; }
.robot-bubble span:nth-child(3) { animation-delay: 0.4s; }

/* 小屏适配 */
@media (max-width: 480px) {
  .ai-guide { right: 12px; left: 12px; bottom: 12px; align-items: stretch; }
  .guide-panel { width: 100%; height: min(470px, 60vh); }
  .robot-btn-wrap { align-self: flex-end; }
}
</style>
