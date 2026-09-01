<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">题库管理</h1>
        <p class="page-subtitle">维护学科题库,支持 AI 智能组卷</p>
      </div>
      <div class="page-header-right">
        <el-button type="success" @click="aiGroupVisible = true">AI智能组卷</el-button>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增题目</el-button>
      </div>
    </div>
    <el-card class="content-card">
    <el-form :inline="true" class="mb-20">
      <el-form-item label="学科">
        <el-select v-model="query.subjectId" placeholder="全部" clearable style="width:120px">
          <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="题型">
        <el-select v-model="query.questionType" placeholder="全部" clearable style="width:120px">
          <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="题干/知识点" clearable style="width:200px" />
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
    </el-form>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column label="题型" width="80"><template #default="{row}">{{ typeText(row.questionType) }}</template></el-table-column>
      <el-table-column prop="knowledgePoint" label="知识点" width="120" />
      <el-table-column label="题干" show-overflow-tooltip>
        <template #default="{row}"><span v-html="renderLatex(row.content)"></span></template>
      </el-table-column>
      <el-table-column label="难度" width="80"><template #default="{row}">{{ '★'.repeat(row.difficulty) }}</template></el-table-column>
      <el-table-column prop="fullScore" label="满分" width="80" />
      <el-table-column label="操作" width="160">
        <template #default="{row}">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="mt-20" v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadData" />

    <el-dialog v-model="formVisible" :title="form.id ? '编辑题目' : '新增题目'" width="700">
      <el-form :model="form" label-width="100px">
        <el-form-item label="学科"><el-select v-model="form.subjectId"><el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="题型"><el-select v-model="form.questionType"><el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" /></el-select></el-form-item>
        <el-form-item label="难度"><el-rate v-model="form.difficulty" :max="5" /></el-form-item>
        <el-form-item label="知识点"><el-input v-model="form.knowledgePoint" /></el-form-item>
        <el-form-item label="题干"><el-input v-model="form.content" type="textarea" :rows="3" /></el-form-item>
        <el-form-item label="选项(JSON)" v-if="[1,2,3].includes(form.questionType)">
          <el-input v-model="form.options" type="textarea" :rows="4" placeholder='[{"key":"A","value":"选项A"}]' />
        </el-form-item>
        <el-form-item label="标准答案"><el-input v-model="form.standardAnswer" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="得分点(JSON)" v-if="[5,6,7].includes(form.questionType)">
          <el-input v-model="form.scorePoint" type="textarea" :rows="4" placeholder='[{"point":"关键词","score":2}]' />
        </el-form-item>
        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="满分"><el-input-number v-model="form.fullScore" :precision="2" :min="0" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="aiGroupVisible" title="AI 智能组卷" width="620" :close-on-click-modal="!aiGroupLoading">
      <el-form label-width="100px">
        <el-form-item label="学科">
          <el-select v-model="aiForm.subjectId" :disabled="aiGroupLoading" style="width:100%" @change="loadAiKnowledgePoints">
            <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识板块">
          <div class="kp-container">
            <el-checkbox-group v-model="aiForm.knowledgePoints" :disabled="aiGroupLoading">
              <el-checkbox v-for="kp in aiKnowledgePoints" :key="kp" :value="kp" class="kp-checkbox">{{ kp }}</el-checkbox>
            </el-checkbox-group>
            <el-empty v-if="aiKnowledgePoints.length === 0" description="请先选择学科" :image-size="40" />
            <div class="kp-custom">
              <el-input v-model="customKp" placeholder="输入自定义知识点，回车或点击添加" :disabled="aiGroupLoading" clearable @keyup.enter="addCustomKp" />
              <el-button type="primary" :disabled="aiGroupLoading || !customKp.trim()" @click="addCustomKp">添加</el-button>
            </div>
            <div v-if="customKpList.length" class="kp-custom-tags">
              <el-tag v-for="k in customKpList" :key="k" closable :disable-transitions="true" class="kp-custom-tag" @close="removeCustomKp(k)">{{ k }}</el-tag>
            </div>
          </div>
        </el-form-item>
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="题目总数">
              <el-input-number v-model="aiForm.totalQuestions" :min="10" :max="50" :step="5" :disabled="aiGroupLoading" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="试卷总分">
              <el-input-number v-model="aiForm.totalScore" :min="50" :max="150" :step="10" :disabled="aiGroupLoading" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-form-item label="难度占比">
          <el-row :gutter="10">
            <el-col :span="8"><div class="type-config"><span>基础题</span><el-slider v-model="aiForm.easyRatio" :min="0" :max="100" :step="10" show-input :disabled="aiGroupLoading" /></div></el-col>
            <el-col :span="8"><div class="type-config"><span>中档题</span><el-slider v-model="aiForm.mediumRatio" :min="0" :max="100" :step="10" show-input :disabled="aiGroupLoading" /></div></el-col>
            <el-col :span="8"><div class="type-config"><span>拔高题</span><el-slider v-model="aiForm.hardRatio" :min="0" :max="100" :step="10" show-input :disabled="aiGroupLoading" /></div></el-col>
          </el-row>
          <el-alert v-if="aiRatioTotal !== 100" :title="`难度占比合计 ${aiRatioTotal}%，必须等于 100%（基础${aiForm.easyRatio}% + 中档${aiForm.mediumRatio}% + 拔高${aiForm.hardRatio}%）`" type="warning" show-icon :closable="false" style="margin-top:8px" />
        </el-form-item>
        <el-form-item label="题型分布">
          <el-row :gutter="10">
            <el-col :span="5" v-for="tc in aiTypeConfigs" :key="tc.code">
              <div class="type-config">
                <span>{{ tc.label }}</span>
                <el-input-number v-model="aiForm[tc.key]" :min="0" :max="50" size="small" style="width:100%" :disabled="aiGroupLoading" />
              </div>
            </el-col>
          </el-row>
          <el-alert v-if="aiTypeCountTotal !== aiForm.totalQuestions" :title="`题型数量合计 ${aiTypeCountTotal} 道，必须等于题目总数 ${aiForm.totalQuestions} 道`" type="warning" show-icon :closable="false" style="margin-top:8px" />
        </el-form-item>
      </el-form>
      <p v-if="aiGroupLoading" class="ai-group-tip">{{ aiGroupStatusText }}</p>
      <template #footer>
        <el-button :disabled="aiGroupLoading" @click="aiGroupVisible = false">取消</el-button>
        <el-button type="primary" :loading="aiGroupLoading" @click="doAiGroup">生成</el-button>
      </template>
    </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { pageQuestions, addQuestion, updateQuestion, deleteQuestion, getSubjects, aiGroup, aiGroupStatus, teacherKnowledgePoints } from '@/api'
import { renderLatex } from '@/utils/latex'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const subjects = ref([])
const types = [
  { value: 1, label: '单选题' }, { value: 2, label: '多选题' },
  { value: 4, label: '填空题' }, { value: 5, label: '简答题' }, { value: 6, label: '作文题' }, { value: 7, label: '计算题' }
]
const typeText = (t) => types.find(x => x.value === t)?.label || ''
// AI 组卷题型分布配置(单选/多选/填空/简答/计算)
const aiTypeConfigs = [
  { key: 'singleCount', code: 1, label: '单选' },
  { key: 'multiCount', code: 2, label: '多选' },
  { key: 'fillCount', code: 4, label: '填空' },
  { key: 'shortCount', code: 5, label: '简答' },
  { key: 'calcCount', code: 7, label: '计算' }
]
const query = reactive({ current: 1, size: 10, subjectId: null, questionType: null, keyword: '' })
const formVisible = ref(false)
const form = reactive({})
const aiGroupVisible = ref(false)
const aiGroupLoading = ref(false)
const aiGroupStatusText = ref('')
const aiKnowledgePoints = ref([])
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

/** 学科切换时加载知识点列表(与学科联动) */
const loadAiKnowledgePoints = async () => {
  aiForm.knowledgePoints = []
  customKpList.value = []
  customKp.value = ''
  if (!aiForm.subjectId) {
    aiKnowledgePoints.value = []
    return
  }
  const res = await teacherKnowledgePoints(aiForm.subjectId)
  aiKnowledgePoints.value = res.data || []
}

onMounted(async () => {
  const s = await getSubjects()
  subjects.value = s.data
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await pageQuestions(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  Object.keys(form).forEach(k => delete form[k])
  form.questionType = 1
  form.difficulty = 3
  form.fullScore = 5
  form.subjectId = subjects.value[0]?.id
  formVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  formVisible.value = true
}

const save = async () => {
  if (form.id) await updateQuestion(form)
  else await addQuestion(form)
  ElMessage.success('保存成功')
  formVisible.value = false
  loadData()
}

const del = async (id) => {
  await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' })
  await deleteQuestion(id)
  ElMessage.success('已删除')
  loadData()
}

const doAiGroup = async () => {
  if (!aiForm.subjectId) {
    ElMessage.warning('请选择学科')
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
    ElMessage.info('已提交 AI 出题任务，完成后将自动提示，请勿关闭弹窗')
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
      return // 保持弹窗打开,便于调整学科/难度后重试
    }
    // 3. 保存抽取结果,供组卷页面(PaperEdit)载入
    localStorage.setItem('ai_group_question_ids', JSON.stringify(ids))
    let tip
    if (result.source === 'ai') {
      tip = ids.length < aiForm.totalQuestions
        ? `AI 已为您现场生成 ${ids.length} 道新题(部分生成失败),可前往组卷页面使用`
        : `AI 已为您现场生成 ${ids.length} 道新题,可前往组卷页面使用`
    } else {
      tip = `AI 服务暂不可用,已从题库抽取 ${ids.length} 道题目,可前往组卷页面使用`
    }
    ElMessage.success(tip)
    aiGroupVisible.value = false
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
