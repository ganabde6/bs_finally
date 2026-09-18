<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">智能出题</h1>
        <p class="page-subtitle">选择学科与知识点,AI 现场出题并组装试卷</p>
      </div>
    </div>
    <el-card class="content-card">
      <el-form label-width="100px" style="max-width:860px">
        <el-form-item label="学科">
          <el-select v-model="aiForm.subjectId" :disabled="aiGroupLoading" style="width:260px">
            <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识板块">
          <div class="kp-container">
            <div class="kp-custom">
              <el-input v-model="customKp" placeholder="输入自定义知识点，回车或点击添加" :disabled="aiGroupLoading" clearable @keyup.enter="addCustomKp" />
              <el-button type="primary" :disabled="aiGroupLoading || !customKp.trim()" @click="addCustomKp">添加</el-button>
            </div>
            <div v-if="customKpList.length" class="kp-custom-tags">
              <el-tag v-for="k in customKpList" :key="k" closable :disable-transitions="true" class="kp-custom-tag" @close="removeCustomKp(k)">{{ k }}</el-tag>
            </div>
            <div v-else class="kp-empty-tip">请添加至少一个知识点</div>
          </div>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="题目总数">
              <el-input-number v-model="aiForm.totalQuestions" :min="10" :max="50" :step="5" :disabled="aiGroupLoading" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="试卷总分">
              <el-input-number v-model="aiForm.totalScore" :min="50" :max="150" :step="10" :disabled="aiGroupLoading" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="难度占比">
          <el-row :gutter="10" style="width:100%">
            <el-col :span="8"><div class="type-config"><span>基础题</span><el-slider v-model="aiForm.easyRatio" :min="0" :max="100" :step="10" show-input :disabled="aiGroupLoading" /></div></el-col>
            <el-col :span="8"><div class="type-config"><span>中档题</span><el-slider v-model="aiForm.mediumRatio" :min="0" :max="100" :step="10" show-input :disabled="aiGroupLoading" /></div></el-col>
            <el-col :span="8"><div class="type-config"><span>拔高题</span><el-slider v-model="aiForm.hardRatio" :min="0" :max="100" :step="10" show-input :disabled="aiGroupLoading" /></div></el-col>
          </el-row>
          <el-alert v-if="aiRatioTotal !== 100" :title="`难度占比合计 ${aiRatioTotal}%，必须等于 100%（基础${aiForm.easyRatio}% + 中档${aiForm.mediumRatio}% + 拔高${aiForm.hardRatio}%）`" type="warning" show-icon :closable="false" style="margin-top:8px" />
        </el-form-item>
        <el-form-item label="题型分布">
          <el-row :gutter="10" style="width:100%">
            <el-col :span="5" v-for="tc in aiTypeConfigs" :key="tc.code">
              <div class="type-config">
                <span>{{ tc.label }}</span>
                <el-input-number v-model="aiForm[tc.key]" :min="0" :max="50" size="small" style="width:100%" :disabled="aiGroupLoading" />
              </div>
            </el-col>
          </el-row>
          <el-alert v-if="aiTypeCountTotal !== aiForm.totalQuestions" :title="`题型数量合计 ${aiTypeCountTotal} 道，必须等于题目总数 ${aiForm.totalQuestions} 道`" type="warning" show-icon :closable="false" style="margin-top:8px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" size="large" :loading="aiGroupLoading" @click="doAiGroup">{{ aiGroupLoading ? 'AI 出题中…' : '开始组卷' }}</el-button>
          <el-button size="large" :disabled="aiGroupLoading" @click="$router.push('/teacher/paper/edit')">先去组卷页面</el-button>
        </el-form-item>
      </el-form>
      <p v-if="aiGroupLoading" class="ai-group-tip">{{ aiGroupStatusText }}</p>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSubjects, aiGroup, aiGroupStatus, teacherMySubjects } from '@/api'

const router = useRouter()

const subjects = ref([])
const aiGroupLoading = ref(false)
const aiGroupStatusText = ref('')
// AI 组卷题型分布配置(单选/多选/填空/简答/计算)
const aiTypeConfigs = [
  { key: 'singleCount', code: 1, label: '单选' },
  { key: 'multiCount', code: 2, label: '多选' },
  { key: 'fillCount', code: 4, label: '填空' },
  { key: 'shortCount', code: 5, label: '简答' },
  { key: 'calcCount', code: 7, label: '计算' }
]
const aiForm = reactive({
  subjectId: null,
  knowledgePoints: [],
  totalQuestions: 10,
  totalScore: 100,
  easyRatio: 40,
  mediumRatio: 40,
  hardRatio: 20,
  singleCount: 2,
  multiCount: 2,
  fillCount: 2,
  shortCount: 2,
  calcCount: 2
})
// 难度占比合计
const aiRatioTotal = computed(() => (aiForm.easyRatio || 0) + (aiForm.mediumRatio || 0) + (aiForm.hardRatio || 0))
// 题型数量合计
const aiTypeCountTotal = computed(() => aiTypeConfigs.reduce((s, c) => s + (aiForm[c.key] || 0), 0))

// 自定义知识点
const customKp = ref('')
const customKpList = ref([])
const addCustomKp = () => {
  const kp = customKp.value.trim()
  if (!kp) return
  if (aiForm.knowledgePoints.includes(kp)) {
    ElMessage.warning('该知识点已存在，请勿重复添加')
    return
  }
  aiForm.knowledgePoints.push(kp)
  customKpList.value.push(kp)
  customKp.value = ''
}
const removeCustomKp = (kp) => {
  const i = aiForm.knowledgePoints.indexOf(kp)
  if (i > -1) aiForm.knowledgePoints.splice(i, 1)
  const j = customKpList.value.indexOf(kp)
  if (j > -1) customKpList.value.splice(j, 1)
}

onMounted(async () => {
  // 仅加载本人任教学科,默认锁定第一个
  const s = await teacherMySubjects()
  subjects.value = s.data || []
  if (!aiForm.subjectId && subjects.value.length) {
    aiForm.subjectId = subjects.value[0].id
  }
})


const doAiGroup = async () => {
  if (!aiForm.subjectId) {
    ElMessage.warning('请选择学科')
    return
  }
  if (!aiForm.knowledgePoints.length) {
    ElMessage.warning('请添加至少一个知识点')
    return
  }
  if (aiRatioTotal.value !== 100) {
    ElMessage.warning(`难度占比合计必须等于 100%（当前 ${aiRatioTotal.value}%）`)
    return
  }
  if (aiTypeCountTotal.value !== aiForm.totalQuestions) {
    ElMessage.warning(`题型数量合计必须等于题目总数（当前 ${aiTypeCountTotal.value} 道 / ${aiForm.totalQuestions} 道）`)
    return
  }
  // 组装组卷配置: 题型数量分布 + 难度占比 + 总分
  const typeCounts = {}
  const questionTypes = []
  aiTypeConfigs.forEach(c => {
    typeCounts[c.code] = aiForm[c.key]
    if (aiForm[c.key] > 0) questionTypes.push(c.code)
  })
  const payload = {
    subjectId: aiForm.subjectId,
    totalQuestions: aiForm.totalQuestions,
    totalScore: aiForm.totalScore,
    knowledgePoints: aiForm.knowledgePoints,
    easyRatio: aiForm.easyRatio,
    mediumRatio: aiForm.mediumRatio,
    hardRatio: aiForm.hardRatio,
    typeCounts,
    questionTypes
  }
  aiGroupLoading.value = true
  const startAt = Date.now()
  const timer = setInterval(() => {
    const s = Math.floor((Date.now() - startAt) / 1000)
    aiGroupStatusText.value = `AI 正在现场出题…已用时 ${Math.floor(s / 60)} 分 ${s % 60} 秒`
  }, 1000)
  try {
    ElMessage.info('已提交 AI 出题任务，完成后将自动提示，请勿离开页面')
    // 1. 提交异步任务,立即返回 taskId
    const res = await aiGroup(payload)
    const taskId = res.data && res.data.taskId
    if (!taskId) {
      ElMessage.warning('提交任务失败，请重试')
      return
    }
    // 2. 轮询任务状态(最长等待 30 分钟)
    const result = await pollAiGroup(taskId, 30 * 60 * 1000)
    const ids = result.questionIds || []
    if (ids.length === 0) {
      ElMessage.warning('AI 出题失败且题库无可用题目，请稍后重试或先到题库管理中添加题目')
      return
    }
    // 3. 保存抽取结果与学科,供组卷页面(PaperEdit)自动筛选载入
    localStorage.setItem('ai_group_question_ids', JSON.stringify(ids))
    localStorage.setItem('ai_group_subject_id', JSON.stringify(aiForm.subjectId))
    let tip
    if (result.source === 'ai') {
      tip = ids.length < aiForm.totalQuestions
        ? `AI 已为您现场生成 ${ids.length} 道新题(部分生成失败),正在前往组卷页面`
        : `AI 已为您现场生成 ${ids.length} 道新题,正在前往组卷页面`
    } else {
      tip = `AI 服务暂不可用,已从题库抽取 ${ids.length} 道题目,正在前往组卷页面`
    }
    ElMessage.success(tip)
    // 4. 直接跳转组卷页面载入题目
    setTimeout(() => router.push('/teacher/paper/edit'), 800)
  } catch (e) {
    ElMessage.error(e.message || 'AI 出题失败，请重试')
  } finally {
    clearInterval(timer)
    aiGroupLoading.value = false
    aiGroupStatusText.value = ''
  }
}

/** 轮询组卷任务状态,直至完成/失败/超时 */
const pollAiGroup = (taskId, timeoutMs) => {
  return new Promise((resolve, reject) => {
    const start = Date.now()
    const tick = async () => {
      try {
        const res = await aiGroupStatus(taskId)
        const st = res.data || {}
        if (st.status === 'DONE') return resolve(st)
        if (st.status === 'FAILED') return reject(new Error(st.message || 'AI 出题失败，请重试'))
        if (Date.now() - start > timeoutMs) return reject(new Error('出题超时，请稍后重试'))
        setTimeout(tick, 3000)
      } catch (e) {
        reject(new Error('查询任务状态失败，请重试'))
      }
    }
    tick()
  })
}
</script>

<style scoped>
.ai-group-tip {
  margin: 8px 0 0;
  font-size: 13px;
  color: #e6a23c;
  text-align: center;
}

.kp-container {
  width: 100%;
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #E8F1F4;
  border-radius: 4px;
  padding: 12px;
}

.kp-checkbox {
  margin-right: 20px;
  margin-bottom: 8px;
}

.kp-empty-tip {
  margin-top: 10px;
  font-size: 13px;
  color: #909399;
}

.kp-custom {
  display: flex;
  gap: 8px;
  margin-top: 10px;
}

.kp-custom-tags {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.kp-custom-tag {
  margin-right: 0;
}

.type-config {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 8px;
  background: #f7f9fa;
  border-radius: 4px;
}
</style>
