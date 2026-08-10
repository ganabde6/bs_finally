<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">AI 答疑</h1>
        <p class="page-subtitle">专属助学老师,支持文字、语音、图片答疑与作文润色</p>
      </div>
    </div>
    <el-card class="content-card">
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
            <el-tag v-if="uploadedFile" type="info" closable @close="uploadedFile = ''"><el-icon style="margin-right:4px"><Paperclip /></el-icon>{{ uploadedFile }}</el-tag>
          </div>
          <el-button type="primary" :loading="loading" :icon="Promotion" @click="send">发送</el-button>
        </div>
      </div>
    </el-card>
    <el-card class="mt-20">
      <template #header><span>作文/简答题智能润色</span></template>
      <el-input v-model="polishContent" type="textarea" :rows="6" placeholder="请输入需要润色的作文或简答题内容（也可拍照上传手写作文）" />
      <!-- 润色图片上传区 -->
      <div class="mt-20" style="display:flex;gap:10px;align-items:center;flex-wrap:wrap">
        <el-upload :show-file-list="false" :before-upload="handlePolishUpload" accept="image/*" action="#">
          <el-button :icon="Camera">拍照/上传手写作文</el-button>
        </el-upload>
        <el-tag v-if="polishImageName" type="info" closable @close="clearPolishImage"><el-icon style="margin-right:4px"><Paperclip /></el-icon>{{ polishImageName }}</el-tag>
        <img v-if="polishImagePreview" :src="polishImagePreview" style="max-height:120px;max-width:200px;border-radius:4px;border:1px solid #E8F1F4" />
      </div>
      <div class="mt-20" style="text-align:right">
        <el-button type="primary" :loading="polishing" @click="doPolish">AI 润色</el-button>
      </div>
      <el-input v-if="polishResult" v-model="polishResult" type="textarea" :rows="10" readonly class="mt-20" />
      
      <!-- 润色后对话区 -->
      <div v-if="polishResult" class="mt-20" style="border-top:1px solid #E8F1F4;padding-top:16px">
        <div style="font-weight:600;margin-bottom:12px;color:#134E4A"><el-icon style="vertical-align:-2px;margin-right:6px"><ChatDotRound /></el-icon>与 AI 继续讨论作文</div>
        <div class="polish-chat-box" ref="polishChatBox">
          <div v-for="msg in polishChatMessages" :key="msg.id" :class="['msg', msg.role]">
            <div class="bubble">
              {{ msg.content }}
            </div>
          </div>
          <div v-if="!polishChatMessages.length" style="color:#909399;font-size:13px;text-align:center;padding:20px">
            针对润色结果向 AI 提问，例如：「帮我改一下结尾段」「这段描写可以更生动吗」
          </div>
        </div>
        <div style="display:flex;gap:10px;margin-top:12px">
          <el-input v-model="polishChatInput" type="textarea" :rows="2" placeholder="输入你想修改或讨论的内容... Ctrl+Enter 发送" @keyup.ctrl.enter="sendPolishChat" />
          <el-button type="primary" :loading="polishChatLoading" @click="sendPolishChat" style="align-self:flex-end;height:60px">发送</el-button>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Paperclip, Promotion, Microphone, VideoPause, Camera, ChatDotRound } from '@element-plus/icons-vue'
import { tutorChat, chatHistory, polishEssay, polishEssayWithImage } from '@/api'

const messages = ref([])
const question = ref('')
const loading = ref(false)
const chatBox = ref()
const polishContent = ref('')
const polishResult = ref('')
const polishing = ref(false)
const polishImageName = ref('')
const polishImageBase64 = ref('')
const polishImagePreview = ref('')
const uploadedFile = ref('')
const uploadedImageBase64 = ref('') // 图片 base64 数据
const isRecording = ref(false)

// 润色后对话
const polishChatMessages = ref([])
const polishChatInput = ref('')
const polishChatLoading = ref(false)
const polishChatBox = ref()
let msgId = 0
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
  const imgBase64 = uploadedImageBase64.value || null
  question.value = ''
  uploadedFile.value = ''
  uploadedImageBase64.value = ''
  try {
    const res = await tutorChat({ question: q, chatType: 1, imageBase64: imgBase64 })
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
  // 如果是图片，转换为 base64
  if (file.type.startsWith('image/')) {
    const reader = new FileReader()
    reader.onload = (e) => {
      uploadedImageBase64.value = e.target.result // data:image/xxx;base64,xxx
    }
    reader.readAsDataURL(file)
  }
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
        // 转换为 base64
        const reader = new FileReader()
        reader.onload = (e) => {
          uploadedImageBase64.value = e.target.result
        }
        reader.readAsDataURL(file)
        ElMessage.success('📎 已粘贴图片，可直接发送')
      }
      break
    }
  }
}

const doPolish = async () => {
  if (!polishContent.value.trim() && !polishImageBase64.value) {
    ElMessage.warning('请输入内容或上传手写作文图片')
    return
  }
  polishing.value = true
  try {
    let res
    if (polishImageBase64.value) {
      // 有图片时使用多模态接口
      res = await polishEssayWithImage(polishImageBase64.value, polishContent.value)
    } else {
      res = await polishEssay(polishContent.value)
    }
    polishResult.value = res.data
    // 重置对话区
    polishChatMessages.value = []
    msgId = 0
  } finally {
    polishing.value = false
  }
}

const handlePolishUpload = (file) => {
  const maxSize = 10 * 1024 * 1024 // 10MB
  if (file.size > maxSize) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  if (!file.type.startsWith('image/')) {
    ElMessage.error('仅支持图片格式')
    return false
  }
  polishImageName.value = file.name
  const reader = new FileReader()
  reader.onload = (e) => {
    polishImageBase64.value = e.target.result
    polishImagePreview.value = e.target.result
  }
  reader.readAsDataURL(file)
  return false
}

const clearPolishImage = () => {
  polishImageName.value = ''
  polishImageBase64.value = ''
  polishImagePreview.value = ''
}

const sendPolishChat = async () => {
  if (!polishChatInput.value.trim()) {
    ElMessage.warning('请输入内容')
    return
  }
  polishChatLoading.value = true
  const userMsg = polishChatInput.value.trim()
  polishChatInput.value = ''
  
  // 添加用户消息
  polishChatMessages.value.push({
    id: ++msgId,
    role: 'user',
    content: userMsg
  })
  scrollPolishChatBottom()
  
  try {
    // 将原文 + 润色结果 + 用户问题一起发给 AI
    const context = `【原文】\n${polishContent.value}\n\n【AI润色结果】\n${polishResult.value}\n\n【学生问题】\n${userMsg}`
    const res = await tutorChat({ question: context, chatType: 1 })
    polishChatMessages.value.push({
      id: ++msgId,
      role: 'assistant',
      content: res.data.answer
    })
    scrollPolishChatBottom()
  } catch (err) {
    ElMessage.error('对话失败，请重试')
  } finally {
    polishChatLoading.value = false
  }
}

const scrollPolishChatBottom = async () => {
  await nextTick()
  if (polishChatBox.value) polishChatBox.value.scrollTop = polishChatBox.value.scrollHeight
}

const scrollBottom = async () => {
  await nextTick()
  if (chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight
}
</script>

<style scoped>
.chat-box { max-height: 400px; overflow-y: auto; padding: 10px; background: #E8F1F4; border-radius: 8px; min-height: 200px; }
.polish-chat-box { max-height: 300px; overflow-y: auto; padding: 10px; background: #E8F1F4; border-radius: 8px; min-height: 80px; }
.msg { margin: 12px 0; }
.msg.user { text-align: right; }
.msg.assistant .bubble { background: #fff; border: 1px solid #E8F1F4; }
.msg.user .bubble { background: #0D9488; color: #fff; }
.bubble { display: inline-block; padding: 10px 16px; border-radius: 8px; max-width: 70%; text-align: left; white-space: pre-wrap; }
.time { color: #c0c4cc; font-size: 12px; margin-top: 4px; }
.attachment-tag { display: block; font-size: 12px; margin-bottom: 4px; opacity: 0.8; }
</style>
