<template>
  <div>
    <el-card>
      <template #header><span>AI 专属助学老师</span></template>
      <div class="chat-box" ref="chatBox">
        <div v-for="msg in messages" :key="msg.id" :class="['msg', msg.role]">
          <div class="bubble">{{ msg.content }}</div>
          <div class="time">{{ msg.createTime }}</div>
        </div>
        <el-empty v-if="!messages.length" description="开始与AI助学老师对话吧" />
      </div>
      <div class="mt-20">
        <el-input v-model="question" type="textarea" :rows="3" placeholder="请输入您的问题(支持文字答疑)" />
        <div class="mt-20" style="display:flex;justify-content:space-between;align-items:center">
          <el-upload :show-file-list="false" :before-upload="handleUpload" action="#">
            <el-button :icon="Camera">拍照提问</el-button>
          </el-upload>
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
import { ref, onMounted, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { Camera, Promotion } from '@element-plus/icons-vue'
import { tutorChat, chatHistory, polishEssay } from '@/api'

const messages = ref([])
const question = ref('')
const loading = ref(false)
const chatBox = ref()
const polishContent = ref('')
const polishResult = ref('')
const polishing = ref(false)

onMounted(async () => {
  try {
    const res = await chatHistory()
    messages.value = res.data || []
    scrollBottom()
  } catch {}
})

const send = async () => {
  if (!question.value.trim()) {
    ElMessage.warning('请输入问题')
    return
  }
  loading.value = true
  // 先显示用户消息
  messages.value.push({ role: 'user', content: question.value, createTime: new Date().toLocaleString() })
  const q = question.value
  question.value = ''
  try {
    const res = await tutorChat({ question: q, chatType: 1 })
    messages.value.push({ role: 'assistant', content: res.data.answer, createTime: new Date().toLocaleString() })
    scrollBottom()
  } finally {
    loading.value = false
  }
}

const handleUpload = (file) => {
  question.value = `[已上传图片 ${file.name}] 请老师帮我讲解相关题目`
  return false
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
</style>
