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

    <el-card class="content-card" style="margin-top:16px">
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>出题记录</span>
          <el-button text :icon="Refresh" @click="loadRecords">刷新</el-button>
        </div>
      </template>
      <el-table v-if="records.length" :data="records" size="default">
        <el-table-column prop="id" label="批次" width="70" />
        <el-table-column prop="subjectName" label="学科" width="110" />
        <el-table-column label="来源" width="110">
          <template #default="{ row }">
            <el-tag :type="row.source === 'ai' ? 'success' : 'info'" size="small">
              {{ row.source === 'ai' ? 'AI 现场出题' : '题库抽取' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="count" label="题目数" width="90" />
        <el-table-column prop="createTime" label="出题时间" min-width="160" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <el-button type="primary" text @click="viewRecord(row)">查看题目</el-button>
            <el-button type="danger" text @click="delRecord(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="暂无出题记录,完成一次智能出题后这里会显示历史记录" :image-size="80" />
    </el-card>

    <el-dialog v-model="recordDialogVisible" :title="`出题记录 #${currentRecord?.id || ''} 的题目(${recordQuestions.length} 道)`" width="760px" top="6vh">
      <div v-if="recordQuestions.length" class="record-questions">
        <div v-for="(q, i) in recordQuestions" :key="q.id" class="record-question-item">
          <div class="rq-title">
            <span class="rq-index">{{ i + 1 }}.</span>
            <el-tag size="small" style="margin-right:6px">{{ typeLabel(q.questionType) }}</el-tag>
            <span>{{ cleanText(q.content) }}</span>
          </div>
          <div v-if="optionList(q).length" class="rq-options">
            <div v-for="opt in optionList(q)" :key="opt" class="rq-option-line">{{ opt }}</div>
          </div>
          <div class="rq-answer">答案：{{ cleanText(q.standardAnswer) }}</div>
        </div>
      </div>
      <el-empty v-else description="该记录的题目已被删除" :image-size="80" />
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import { getSubjects, aiGroup, aiGroupStatus, teacherMySubjects, aiGroupRecords, aiGroupRecordQuestions, deleteAiGroupRecord } from '@/api'

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

// ===================== 出题记录 =====================
const records = ref([])
const recordDialogVisible = ref(false)
const currentRecord = ref(null)
const recordQuestions = ref([])
// 题型编码 -> 名称(与后端 SystemConstants 保持一致)
const typeLabelMap = { 1: '单选', 2: '多选', 4: '填空', 5: '简答', 7: '计算' }
const typeLabel = (t) => typeLabelMap[t] || `题型${t}`

/** 清理文本中 AI 可能夹带的 markdown 代码围栏等标记 */
const cleanText = (text) => {
  if (!text) return ''
  return String(text)
    .replace(/```[a-zA-Z]*\n?/g, '')
    .replace(/\\n/g, '\n')
    .trim()
}

/** 解析选项:options 存的是 JSON 数组字符串([{key,value}] 或字符串数组),解析为逐行选项;解析失败则按原文处理 */
const optionList = (q) => {
  const raw = q && q.options
  if (!raw) return []
  const cleaned = cleanText(raw)
  try {
    const arr = JSON.parse(cleaned)
    if (Array.isArray(arr)) {
      return arr.map(o => {
        if (o && typeof o === 'object') {
          const value = o.value != null ? String(o.value) : (o.content != null ? String(o.content) : '')
          return o.key != null && value ? `${o.key}. ${value}` : value
        }
        return String(o)
      }).filter(Boolean)
    }
  } catch { /* 非合法 JSON,按原文展示 */ }
  return cleaned.split('\n').map(s => s.trim()).filter(Boolean)
}

const loadRecords = async () => {
  try {
    const res = await aiGroupRecords()
    records.value = res.data || []
  } catch (e) {
    // 静默失败:记录面板不影响出题主流程
  }
}

const viewRecord = async (row) => {
  currentRecord.value = row
  recordQuestions.value = []
  recordDialogVisible.value = true
  try {
    const res = await aiGroupRecordQuestions(row.id)
    recordQuestions.value = res.data || []
  } catch (e) {
    ElMessage.error(e.message || '加载题目失败')
  }
}

/** 删除出题记录(仅删记录条目,不影响已生成的题目) */
const delRecord = async (row) => {
  try {
    await ElMessageBox.confirm(
      '确定删除该条出题记录吗?已生成的题目仍保留在题库中。',
      '删除出题记录',
      { type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消' }
    )
  } catch { return }
  await deleteAiGroupRecord(row.id)
  ElMessage.success('记录已删除')
  loadRecords()
}

onMounted(async () => {
  // 仅加载本人任教学科,默认锁定第一个
  const s = await teacherMySubjects()
  subjects.value = s.data || []
  if (!aiForm.subjectId && subjects.value.length) {
    aiForm.subjectId = subjects.value[0].id
  }
  loadRecords()
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

.record-questions {
  max-height: 60vh;
  overflow-y: auto;
}

.record-question-item {
  padding: 10px 12px;
  border: 1px solid #E8F1F4;
  border-radius: 4px;
  margin-bottom: 10px;
}

.rq-title {
  font-size: 14px;
  line-height: 1.6;
  word-break: break-all;
}

.rq-index {
  font-weight: 600;
  margin-right: 4px;
}

.rq-options {
  margin-top: 6px;
  font-size: 13px;
  color: #606266;
  white-space: pre-wrap;
  word-break: break-all;
}

.rq-option-line {
  line-height: 1.8;
}

.rq-answer {
  margin-top: 6px;
  font-size: 13px;
  color: #67c23a;
}
</style>
