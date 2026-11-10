<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">智能家校反馈</h1>
        <p class="page-subtitle">AI 生成学生学情反馈单,一键复制或推送到企业微信家长群</p>
      </div>
    </div>
    <el-card class="content-card">
    <el-form :inline="true" class="mb-20">
      <el-form-item label="选择班级">
        <el-select v-model="classId" placeholder="请选择班级" style="width:160px" @change="onClassChange">
          <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="选择学生">
        <el-select v-model="studentId" filterable :loading="studentsLoading" placeholder="请先选择班级" :disabled="!classId" style="width:220px" @change="loadFeedback">
          <el-option v-for="s in students" :key="s.id" :label="s.realName + '(' + s.username + ')'" :value="s.id" />
        </el-select>
        <span v-if="classId && !studentsLoading" class="student-count">共 {{ students.length }} 名学生</span>
      </el-form-item>
      <el-form-item v-if="feedbackText">
        <el-button type="primary" plain @click="copyFeedback">一键复制</el-button>
        <el-button type="success" @click="openPushDialog">推送到企业微信群</el-button>
      </el-form-item>
    </el-form>
    <div v-if="feedbackText" class="feedback-box">{{ feedbackText }}</div>
    <el-empty v-else-if="classId && !studentsLoading && !students.length" description="该班级暂无学生,请选择其他班级" />
    <el-empty v-else description="请选择学生查看 AI 生成的学情反馈单" />

    <el-dialog v-model="pushVisible" title="推送到企业微信群" width="520px">
      <el-alert type="info" :closable="false" class="mb-20"
        title="获取方式:在企业微信群右键 → 添加群机器人 → 创建后复制 Webhook 地址" />
      <el-input v-model="webhookUrl" placeholder="https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=..." clearable />
      <template #footer>
        <el-button @click="pushVisible = false">取消</el-button>
        <el-button type="success" :loading="pushing" @click="doPush">确认推送</el-button>
      </template>
    </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getClasses, teacherClassStudents, feedback, pushFeedback } from '@/api'

const WEBHOOK_KEY = 'zhixue_wecom_webhook'

const classes = ref([])
const students = ref([])
const classId = ref(null)
const studentId = ref(null)
const feedbackText = ref('')
const studentsLoading = ref(false)
const pushVisible = ref(false)
const pushing = ref(false)
const webhookUrl = ref(localStorage.getItem(WEBHOOK_KEY) || '')

onMounted(async () => {
  try {
    const c = await getClasses()
    classes.value = c.data || []
    if (classes.value.length) {
      classId.value = classes.value[0].id
      await loadStudents(classId.value)
    }
  } catch (e) {
    console.error('加载班级列表失败:', e)
  }
})

const loadStudents = async (cid) => {
  studentId.value = null
  feedbackText.value = ''
  studentsLoading.value = true
  try {
    const s = await teacherClassStudents(cid)
    students.value = s.data || []
  } catch (e) {
    console.error('加载学生列表失败:', e)
    students.value = []
  } finally {
    studentsLoading.value = false
  }
}

const onClassChange = (cid) => {
  if (cid) loadStudents(cid)
}

const loadFeedback = async () => {
  if (!studentId.value) return
  try {
    const res = await feedback(studentId.value)
    feedbackText.value = res.data
  } catch (e) {
    console.error('生成反馈失败:', e)
    feedbackText.value = ''
  }
}

const currentStudentName = () => {
  const s = students.value.find(x => x.id === studentId.value)
  return s ? s.realName : ''
}

const copyFeedback = async () => {
  const text = `【智汇云课堂·学情反馈】\n学生:${currentStudentName()}\n\n${feedbackText.value}`
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板,可直接粘贴到微信群发送给家长')
  } catch (e) {
    // 兼容非 HTTPS/旧浏览器环境
    const ta = document.createElement('textarea')
    ta.value = text
    document.body.appendChild(ta)
    ta.select()
    document.execCommand('copy')
    document.body.removeChild(ta)
    ElMessage.success('已复制到剪贴板,可直接粘贴到微信群发送给家长')
  }
}

const openPushDialog = () => {
  if (!webhookUrl.value) {
    ElMessage.info('请先填写企业微信群机器人的 Webhook 地址')
  }
  pushVisible.value = true
}

const doPush = async () => {
  if (!webhookUrl.value.trim()) {
    ElMessage.warning('请填写 Webhook 地址')
    return
  }
  pushing.value = true
  try {
    const res = await pushFeedback(studentId.value, webhookUrl.value.trim())
    localStorage.setItem(WEBHOOK_KEY, webhookUrl.value.trim())
    ElMessage.success(res.data || '推送成功,请到企业微信群查看')
    pushVisible.value = false
  } catch (e) {
    ElMessage.error(e?.response?.data?.message || '推送失败,请检查 Webhook 地址')
  } finally {
    pushing.value = false
  }
}
</script>

<style scoped>
.feedback-box { background: #F0FDFA; padding: 24px; border-radius: 8px; line-height: 1.8; white-space: pre-wrap; border: 1px dashed #0D9488; }
.student-count { margin-left: 10px; color: #909399; font-size: 13px; }
</style>
