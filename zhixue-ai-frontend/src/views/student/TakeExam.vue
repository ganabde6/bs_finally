<template>
  <div v-loading="loading">
    <el-card v-if="paper">
      <template #header>
        <div class="card-header">
          <div>
            <span style="font-size:18px;font-weight:600">{{ paper.paper.paperName }}</span>
            <el-tag class="ml-10" :type="paper.paper.paperType === 1 ? 'success' : 'danger'">
              {{ paper.paper.paperType === 1 ? '作业' : '考试' }}
            </el-tag>
            <span class="ml-10" style="color:#909399">总分:{{ paper.paper.totalScore }} | 时长:{{ paper.paper.duration }}分钟</span>
          </div>
          <div>
            <span style="color:#F56C6C;font-weight:600">剩余时间: {{ formatTime(remainSec) }}</span>
          </div>
        </div>
      </template>
      <div v-for="(q, i) in paper.questions" :key="q.id" class="question-item">
        <div class="question-title">
          <el-tag size="small">{{ typeText(q.questionType) }}</el-tag>
          <span class="ml-10">{{ i + 1 }}. {{ q.content }}</span>
          <span style="color:#909399;margin-left:8px">({{ q.score }}分)</span>
        </div>
        <div class="mt-20">
          <!-- 单选/判断 -->
          <el-radio-group v-if="q.questionType === 1 || q.questionType === 3" v-model="answers[q.id]">
            <el-radio v-for="opt in parseOptions(q.options)" :key="opt.key" :value="opt.key" class="option-item">{{ opt.key }}. {{ opt.value }}</el-radio>
          </el-radio-group>
          <!-- 多选 -->
          <el-checkbox-group v-else-if="q.questionType === 2" v-model="answers[q.id]">
            <el-checkbox v-for="opt in parseOptions(q.options)" :key="opt.key" :value="opt.key" class="option-item">{{ opt.key }}. {{ opt.value }}</el-checkbox>
          </el-checkbox-group>
          <!-- 填空 -->
          <el-input v-else-if="q.questionType === 4" v-model="answers[q.id]" placeholder="请输入答案" style="max-width:400px" />
          <!-- 简答/计算 -->
          <el-input v-else-if="q.questionType === 5 || q.questionType === 7" v-model="answers[q.id]" type="textarea" :rows="4" placeholder="请输入答案" />
          <!-- 作文 -->
          <el-input v-else-if="q.questionType === 6" v-model="answers[q.id]" type="textarea" :rows="10" placeholder="请输入作文内容(不少于600字)" />
        </div>
      </div>
      <div class="mt-20" style="text-align:center">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="handleSubmit">提交作答</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { studentPaperDetail, startAnswer, submitAnswer } from '@/api'

const route = useRoute()
const router = useRouter()
const paper = ref(null)
const loading = ref(false)
const submitting = ref(false)
const answers = reactive({})
const answerId = ref(null)
const remainSec = ref(0)
const startTime = ref(0)
let timer = null

const typeText = (t) => ['','单选题','多选题','判断题','填空题','简答题','作文题','计算题'][t]
const parseOptions = (str) => str ? JSON.parse(str) : []
const formatTime = (s) => {
  const m = Math.floor(s / 60)
  const sec = s % 60
  return `${m}:${sec.toString().padStart(2,'0')}`
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await studentPaperDetail(route.params.id)
    paper.value = res.data
    remainSec.value = (paper.value.paper.duration || 60) * 60
    startTime.value = Date.now()
    // 初始化答案
    paper.value.questions.forEach(q => {
      answers[q.id] = q.questionType === 2 ? [] : ''
    })
    // 创建作答记录
    const ar = await startAnswer(route.params.id)
    answerId.value = ar.data.id
    // 启动倒计时
    timer = setInterval(() => {
      remainSec.value--
      if (remainSec.value <= 0) {
        clearInterval(timer)
        ElMessage.warning('考试时间到,自动提交')
        handleSubmit()
      }
    }, 1000)
    // 切屏风控监测
    window.addEventListener('blur', handleBlur)
  } catch (e) {
    // request.js 拦截器已弹出具体错误提示,这里兜底:加载失败返回列表页
    // (常见原因:试卷不存在/已过期/重复开考,startAnswer 被后端拒绝)
    setTimeout(() => router.replace('/student/paper'), 1200)
  } finally {
    loading.value = false
  }
})

let blurCount = 0
const handleBlur = () => {
  blurCount++
  import('@/api').then(({ reportRisk }) => {
    reportRisk({ answerId: answerId.value, riskType: 1, description: `第${blurCount}次切屏` })
        .catch(() => { /* 风控上报失败不影响作答,静默吞掉 */ })
  })
  ElMessage.warning(`检测到切屏,已记录第${blurCount}次`)
}

const handleSubmit = async () => {
  // ElMessageBox.confirm 在用户点"取消"时会 reject('cancel'),必须捕获,静默退出即可
  try {
    await ElMessageBox.confirm('确定提交作答吗?提交后将自动批改。', '提示', { type: 'warning' })
  } catch {
    return
  }
  submitting.value = true
  try {
    const duration = Math.floor((Date.now() - startTime.value) / 1000)
    const res = await submitAnswer(answerId.value, { answers, duration })
    ElMessage.success(`批改完成!总分:${res.data.totalScore},对${res.data.correctCount}题,错${res.data.wrongCount}题`)
    router.push('/student/paper')
  } catch (e) {
    // request.js 已弹出后端错误信息,这里恢复按钮状态即可(finally 中处理)
  } finally {
    submitting.value = false
  }
}

const goBack = () => router.push('/student/paper')

onUnmounted(() => {
  if (timer) clearInterval(timer)
  window.removeEventListener('blur', handleBlur)
})
</script>

<style scoped>
.question-item { padding: 16px 0; border-bottom: 1px dashed #ebeef5; }
.question-title { font-size: 15px; line-height: 1.8; }
.option-item { display: block; margin: 8px 0; }
.ml-10 { margin-left: 10px; }
</style>
