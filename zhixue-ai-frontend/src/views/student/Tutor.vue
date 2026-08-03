<template>
  <div>
    <el-card>
      <template #header><span>AI 专属助学老师</span></template>
      <div class="chat-box" ref="chatBox">
        <div v-for="msg in messages" :key="msg.id" :class="['msg', msg.role]">
          <div class="bubble">
            <span v-if="msg.attachment" class="attachment-tag"> {{ msg.attachment }}</span>
            {{ msg.content }}
          </div>
          <div class="time">{{ msg.createTime }}</div>
        </div>
        <el-empty v-if="!messages.length" description="开始与AI助学老师对话吧" />
      </div>
      <div class="mt-20">
        <el-input v-model="question" type="textarea" :rows="3" placeholder="请输入您的问题(支持文字、语音、图片粘贴答疑)" @paste="handlePaste" />
        <div class="mt-20" style="display:flex;justify-content:space-between;align-items:center;flex-wrap:wrap;gap:10px">
          <div style="display:flex;gap:10px;align-items:center">
            <!-- 上传附件/照片 -->
            <el-upload :show-file-list="false" :before-upload="handleUpload" accept="image/*,.pdf,.doc,.docx,.jpg,.jpeg,.png,.gif" action="#">
              <el-button :icon="Paperclip">上传附件</el-button>
            </el-upload>
            <!-- 语音识别 -->
            <el-button :type="isRecording ? 'danger' : 'default'" :icon="isRecording ? VideoPause : Microphone" @click="toggleVoice">
              {{ isRecording ? '停止录音' : '语音输入' }}
            </el-button>
            <el-tag v-if="uploadedFile" type="info" closable @close="uploadedFile = ''">📎 {{ uploadedFile }}</el-tag>
          </div>
          <el-button type="primary" :loading="loading" :icon="Promotion" @click="send">发送</el-button>
        </div>
      </div>
    </el-card>
    <el-card class="mt-20">
      <template #header><span>作文/简答题智能润色</span></template>
      <el-input v-model="polishContent" type="textarea" :rows="6" placeholder="请输入需要润色的作文或简答题内容" />
      <div class="mt-20" style="text-align:right">
        <el-button type="primary" :loading="polishing" @click="doPolish">AI 润色</el-button>
      </div>
      <el-input v-if="polishResult" v-model="polishResult" type="textarea" :rows="10" readonly class="mt-20" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Paperclip, Promotion, Microphone, VideoPause } from '@element-plus/icons-vue'
import { tutorChat, chatHistory, polishEssay } from '@/api'

const messages = ref([])
const question = ref('')
const loading = ref(false)
const chatBox = ref()
const polishContent = ref('')
const polishResult = ref('')
const polishing = ref(false)
const uploadedFile = ref('')
const isRecording = ref(false)
let recognition = null

onMounted(async () => {
  try {
    const res = await chatHistory()
    messages.value = res.data || []
    scrollBottom()
  } catch {}
  // 初始化语音识别
  initSpeechRecognition()
})

onUnmounted(() => {
  if (recognition) {
    recognition.stop()
    recognition = null
  }
})

const initSpeechRecognition = () => {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition
  if (!SpeechRecognition) {
    // 浏览器不支持语音识别，静默处理
    return
  }
  recognition = new SpeechRecognition()
  recognition.lang = 'zh-CN'
  recognition.continuous = true
  recognition.interimResults = true

  recognition.onresult = (event) => {
    let transcript = ''
    for (let i = 0; i < event.results.length; i++) {
      transcript += event.results[i][0].transcript
    }
    question.value = transcript
  }

  recognition.onerror = (event) => {
    ElMessage.error('语音识别失败：' + (event.error === 'no-speech' ? '未检测到语音' : event.error))
    isRecording.value = false
  }

  recognition.onend = () => {
    isRecording.value = false
  }
}

const toggleVoice = () => {
  if (!recognition) {
    ElMessage.warning('当前浏览器不支持语音识别，请使用 Chrome 或 Edge 浏览器')
    return
  }
  if (isRecording.value) {
    recognition.stop()
    isRecording.value = false
  } else {
    try {
      recognition.start()
      isRecording.value = true
      ElMessage.info('🎤 正在录音，请开始说话...')
    } catch (e) {
      ElMessage.error('语音识别启动失败')
      isRecording.value = false
    }
  }
}

const send = async () => {
  if (!question.value.trim() && !uploadedFile.value) {
    ElMessage.warning('请输入问题或上传附件')
    return
  }
  loading.value = true
  const content = uploadedFile.value
    ? `[已上传附件: ${uploadedFile.value}] ${question.value}`
    : question.value
  // 先显示用户消息
  messages.value.push({
    role: 'user',
    content: question.value,
    attachment: uploadedFile.value || null,
    createTime: new Date().toLocaleString()
  })
  const q = content
  question.value = ''
  uploadedFile.value = ''
  try {
    const res = await tutorChat({ question: q, chatType: 1 })
    messages.value.push({ role: 'assistant', content: res.data.answer, createTime: new Date().toLocaleString() })
    scrollBottom()
  } finally {
    loading.value = false
  }
}

const handleUpload = (file) => {
  const maxSize = 10 * 1024 * 1024 // 10MB
  if (file.size > maxSize) {
    ElMessage.error('文件大小不能超过 10MB')
    return false
  }
  const allowedTypes = ['image/jpeg', 'image/png', 'image/gif', 'image/jpg', 'application/pdf', 'application/msword', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document']
  if (!allowedTypes.includes(file.type)) {
    ElMessage.error('仅支持图片、PDF、Word 文档格式')
    return false
  }
  uploadedFile.value = file.name
  return false
}

const handlePaste = (event) => {
  const items = event.clipboardData?.items
  if (!items) return
  for (let i = 0; i < items.length; i++) {
    const item = items[i]
    if (item.type.startsWith('image/')) {
      event.preventDefault()
      const file = item.getAsFile()
      if (file) {
        const maxSize = 10 * 1024 * 1024
        if (file.size > maxSize) {
          ElMessage.error('粘贴的图片大小不能超过 10MB')
          return
        }
        const fileName = `粘贴图片_${new Date().getTime()}.png`
        uploadedFile.value = fileName
        ElMessage.success('📎 已粘贴图片，可直接发送')
      }
      break
    }
  }
}

const doPolish = async () => {
  if (!polishContent.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  polishing.value = true
  try {
    const res = await polishEssay(polishContent.value)
    polishResult.value = res.data
  } finally {
    polishing.value = false
  }
}

const scrollBottom = async () => {
  await nextTick()
  if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
}
</script>

<style scoped>
.chat-box { max-height: 400px; overflow-y: auto; padding: 10px; background: #f5f7fa; border-radius: 4px; min-height: 200px; }
.msg { margin: 12px 0; }
.msg.user { text-align: right; }
.msg.assistant .bubble { background: #fff; border: 1px solid #dcdfe6; }
.msg.user .bubble { background: #409EFF; color: #fff; }
.bubble { display: inline-block; padding: 10px 16px; border-radius: 8px; max-width: 70%; text-align: left; white-space: pre-wrap; }
.time { color: #c0c4cc; font-size: 12px; margin-top: 4px; }
.attachment-tag { display: block; font-size: 12px; margin-bottom: 4px; opacity: 0.8; }
</style>
