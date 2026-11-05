<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">{{ isEdit ? '编辑试卷' : '新建试卷/作业' }}</h1>
        <p class="page-subtitle">左侧题库选题,右侧已选清单,总分自动计算</p>
      </div>
      <div class="page-header-right">
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>

    <!-- 试卷信息 -->
    <el-card v-loading="loading" class="content-card mb-20">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="20">
          <el-col :span="12"><el-form-item label="名称"><el-input v-model="form.paperName" placeholder="如: 高三数学周测(一)" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="类型"><el-select v-model="form.paperType" style="width:100%"><el-option label="作业" :value="1" /><el-option label="考试" :value="2" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="总分"><el-input-number v-model="totalScore" :precision="2" disabled style="width:100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="学科"><el-select v-model="form.subjectId" style="width:100%"><el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="班级"><el-select v-model="form.classId" style="width:100%"><el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" /></el-select></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="时长(分)"><el-input-number v-model="form.duration" :min="1" style="width:100%" /></el-form-item></el-col>
          <el-col :span="6"><el-form-item label="截止时间"><el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width:100%" /></el-form-item></el-col>
          <el-col :span="24"><el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item></el-col>
        </el-row>
      </el-form>
    </el-card>

    <!-- 双栏: 左题库 / 右已选 -->
    <el-row :gutter="20">
      <el-col :span="16">
        <el-card class="content-card">
          <template #header><span>题库选题</span></template>
          <el-form :inline="true" class="mb-10">
            <el-form-item label="学科">
              <el-select v-model="qQuery.subjectId" placeholder="全部" clearable style="width:110px" @change="onSubjectFilterChange">
                <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="题型">
              <el-select v-model="qQuery.questionType" placeholder="全部" clearable style="width:100px">
                <el-option v-for="t in types" :key="t.value" :label="t.label" :value="t.value" />
              </el-select>
            </el-form-item>
            <el-form-item label="难度">
              <el-select v-model="qQuery.difficulty" placeholder="全部" clearable style="width:90px">
                <el-option v-for="d in [1,2,3,4,5]" :key="d" :label="'★'.repeat(d)" :value="d" />
              </el-select>
            </el-form-item>
            <el-form-item>
              <el-input v-model="qQuery.keyword" placeholder="题干/知识点" clearable style="width:150px" @keyup.enter="search" />
            </el-form-item>
            <el-button type="primary" @click="search">查询</el-button>
          </el-form>
          <el-table ref="bankTable" :data="questions" stripe max-height="420" row-key="id"
                    @selection-change="onSelect">
            <el-table-column type="selection" width="46" reserve-selection />
            <el-table-column label="题型" width="80">
              <template #default="{row}"><el-tag size="small" :type="typeTag(row.questionType)" effect="light">{{ typeText(row.questionType) }}</el-tag></template>
            </el-table-column>
            <el-table-column label="题干" show-overflow-tooltip>
              <template #default="{row}"><span v-html="renderLatex(row.content)"></span></template>
            </el-table-column>
            <el-table-column prop="knowledgePoint" label="知识点" width="110" show-overflow-tooltip />
            <el-table-column label="难度" width="100"><template #default="{row}">{{ '★'.repeat(row.difficulty) }}</template></el-table-column>
          </el-table>
          <el-pagination class="mt-10" v-model:current-page="qQuery.current" v-model:page-size="qQuery.size"
                         :total="qTotal" layout="total, prev, pager, next" @current-change="loadQuestions" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="content-card selected-card">
          <template #header>
            <div class="card-header"><span>已选题目</span>
              <el-button v-if="selected.length" link type="danger" @click="clearSelected">清空</el-button>
            </div>
          </template>
          <div v-if="selected.length" class="selected-list">
            <div v-for="(q, i) in selected" :key="q.id" class="selected-item">
              <span class="sel-idx">{{ i + 1 }}</span>
              <el-tag size="small" :type="typeTag(q.questionType)" effect="light" class="sel-type">{{ typeText(q.questionType) }}</el-tag>
              <span class="sel-content" :title="stripHtml(q.content)" v-html="renderLatex(q.content)"></span>
              <el-input-number v-model="scoreMap[q.id]" :min="0.5" :precision="2" size="small" style="width:92px" />
              <el-button type="danger" link :icon="Delete" @click="removeSelected(q)" />
            </div>
          </div>
          <el-empty v-else description="从左侧题库勾选题目" :image-size="70" />
          <div class="selected-footer">
            共 <strong>{{ selected.length }}</strong> 题 · 合计 <strong>{{ totalScore }}</strong> 分
          </div>
        </el-card>
      </el-col>
    </el-row>

    <div class="mt-20" style="text-align:center">
      <el-button @click="$router.back()">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="save(false)">保存试卷</el-button>
      <el-button v-if="form.status === 0" type="success" :loading="submitting" @click="save(true)">保存并发布</el-button>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Delete } from '@element-plus/icons-vue'
import { getClasses, pageQuestions, createPaper, updatePaper, teacherPaperDetail, teacherMySubjects, publishPaper } from '@/api'
import { renderLatex } from '@/utils/latex'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => !!route.params.id)
const subjects = ref([])
const classes = ref([])
const questions = ref([])
const qTotal = ref(0)
const selected = ref([])
const scoreMap = reactive({})
const bankTable = ref()
const qQuery = reactive({ current: 1, size: 10, subjectId: null, questionType: null, difficulty: null, keyword: '' })
const form = reactive({
  paperName: '', paperType: 1, subjectId: null, classId: null,
  totalScore: 0, duration: 60, deadline: null, description: '', status: 0
})
const types = [
  { value: 1, label: '单选题' }, { value: 2, label: '多选题' },
  { value: 4, label: '填空题' }, { value: 5, label: '简答题' }, { value: 6, label: '作文题' }, { value: 7, label: '计算题' }
]
const typeText = (t) => types.find(x => x.value === t)?.label || ''
const typeTag = (t) => ({ 1: 'primary', 2: 'success', 4: 'warning', 5: 'danger', 6: 'danger', 7: 'info' }[t] || 'info')
const stripHtml = (s) => (s || '').replace(/<[^>]+>/g, '')

// 总分 = 已选题目分值之和(自动计算,不可手填)
const totalScore = computed(() => {
  const sum = selected.value.reduce((s, q) => s + (scoreMap[q.id] || 0), 0)
  form.totalScore = sum
  return sum
})

onMounted(async () => {
  const [s, c] = await Promise.all([teacherMySubjects(), getClasses()])
  subjects.value = s.data || []
  classes.value = c.data || []
  // 新建试卷默认选中第一个任教学科
  if (!isEdit.value && !form.subjectId && subjects.value.length) {
    form.subjectId = subjects.value[0].id
    qQuery.subjectId = form.subjectId
  }
  // 载入 AI 智能组卷结果(仅新建试卷时): 自动按 AI 学科筛选并匹配全部题目
  if (!isEdit.value) {
    const aiIds = JSON.parse(localStorage.getItem('ai_group_question_ids') || '[]')
    const aiSubjectId = JSON.parse(localStorage.getItem('ai_group_subject_id') || 'null')
    localStorage.removeItem('ai_group_question_ids')
    localStorage.removeItem('ai_group_subject_id')
    if (aiIds.length) {
      await loadAiQuestions(aiIds, aiSubjectId)
    } else {
      await loadQuestions()
    }
  }
  // 编辑模式: 载入试卷信息并回显已选题目
  if (isEdit.value) {
    loading.value = true
    try {
      const res = await teacherPaperDetail(route.params.id)
      Object.assign(form, res.data.paper)
      qQuery.subjectId = form.subjectId
      await loadQuestions()
      const bankMap = new Map(questions.value.map(q => [q.id, q]))
      for (const q of res.data.questions) {
        scoreMap[q.question.id] = q.score
        const bankRow = bankMap.get(q.question.id)
        if (bankRow) {
          bankTable.value?.toggleRowSelection(bankRow, true)
        } else {
          // 题目不在当前页时也加入已选清单,避免编辑丢失
          selected.value.push(q.question)
        }
      }
    } finally { loading.value = false }
  }
})

const loadQuestions = async () => {
  const res = await pageQuestions(qQuery)
  questions.value = res.data.records
  qTotal.value = res.data.total
  // 默认分值
  questions.value.forEach(q => {
    if (!scoreMap[q.id]) scoreMap[q.id] = q.fullScore || 5
  })
  await nextTick()
  // 翻页/刷新后回显已选中(带 reserve-selection 仍需同步分值默认)
}

const search = () => { qQuery.current = 1; loadQuestions() }

const onSubjectFilterChange = () => { qQuery.current = 1; loadQuestions() }

/** AI 组卷结果载入: 按 AI 学科拉取全量题目进行匹配,不局限于前 10 条 */
const loadAiQuestions = async (aiIds, aiSubjectId) => {
  if (aiSubjectId) qQuery.subjectId = aiSubjectId
  const res = await pageQuestions({ ...qQuery, current: 1, size: 500 })
  questions.value = res.data.records
  qTotal.value = res.data.total
  questions.value.forEach(q => {
    if (!scoreMap[q.id]) scoreMap[q.id] = q.fullScore || 5
  })
  const matched = questions.value.filter(q => aiIds.includes(q.id))
  await nextTick()
  matched.forEach(q => bankTable.value?.toggleRowSelection(q, true))
  if (matched.length) {
    ElMessage.success(`已载入智能出题生成的 ${matched.length} 道题目`)
  }
}

const onSelect = (rows) => { selected.value = rows }

const removeSelected = (q) => {
  bankTable.value?.toggleRowSelection(q, false)
  // 若该题不在当前表格页(编辑模式跨页题目),直接从清单移除
  const idx = selected.value.indexOf(q)
  if (idx > -1) selected.value.splice(idx, 1)
}

const clearSelected = () => {
  selected.value.forEach(q => bankTable.value?.toggleRowSelection(q, false))
  selected.value = []
}

const save = async (publish = false) => {
  if (!form.paperName || !form.subjectId || !form.classId) {
    ElMessage.warning('请填写完整信息(名称/学科/班级)')
    return
  }
  if (selected.value.length === 0) {
    ElMessage.warning('请至少选择一道题目')
    return
  }
  if (form.totalScore <= 0) {
    ElMessage.warning('试卷总分必须大于 0,请为题目设置分值')
    return
  }
  submitting.value = true
  try {
    const questionsPayload = selected.value.map(q => ({ questionId: q.id, score: scoreMap[q.id] }))
    const payload = { paper: { ...form, totalScore: totalScore.value }, questions: questionsPayload }
    let paperId
    if (isEdit.value) {
      payload.paper.id = Number(route.params.id)
      paperId = payload.paper.id
      await updatePaper(payload)
    } else {
      const res = await createPaper(payload)
      paperId = res.data
    }
    if (publish) {
      await publishPaper(paperId)
      ElMessage.success('保存并发布成功,学生端已可见')
    } else {
      ElMessage.success('保存成功')
    }
    router.push('/teacher/paper')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.selected-card :deep(.el-card__body) { display: flex; flex-direction: column; }
.selected-list { flex: 1; overflow-y: auto; max-height: 420px; }
.selected-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px dashed #f0f2f5;
}
.sel-idx {
  width: 20px; height: 20px;
  border-radius: 50%;
  background: #0D9488;
  color: #fff;
  font-size: 12px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.sel-type { flex-shrink: 0; }
.sel-content {
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}
.selected-footer {
  margin-top: 12px;
  padding: 10px 12px;
  background: #F3FBF8;
  border-radius: 8px;
  text-align: center;
  font-size: 14px;
}
.selected-footer strong { color: #0D9488; font-size: 16px; }
.mb-10 { margin-bottom: 10px; }
.mt-10 { margin-top: 10px; }
</style>
