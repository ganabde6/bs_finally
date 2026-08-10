<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">{{ isEdit ? '编辑试卷' : '新建试卷/作业' }}</h1>
        <p class="page-subtitle">配置试卷信息并从题库选题组卷</p>
      </div>
      <div class="page-header-right">
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>
    <el-card v-loading="loading" class="content-card">
    <el-form :model="form" label-width="100px">
      <el-row :gutter="20">
        <el-col :span="12"><el-form-item label="名称"><el-input v-model="form.paperName" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="类型"><el-select v-model="form.paperType"><el-option label="作业" :value="1" /><el-option label="考试" :value="2" /></el-select></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="总分"><el-input-number v-model="form.totalScore" :precision="2" disabled /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="学科"><el-select v-model="form.subjectId"><el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" /></el-select></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="班级"><el-select v-model="form.classId"><el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" /></el-select></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="时长(分)"><el-input-number v-model="form.duration" :min="1" /></el-form-item></el-col>
        <el-col :span="6"><el-form-item label="截止时间"><el-date-picker v-model="form.deadline" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" /></el-form-item></el-col>
        <el-col :span="24"><el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item></el-col>
      </el-row>
    </el-form>

    <el-divider>题目选择(从题库添加)</el-divider>
    <el-form :inline="true" class="mb-20">
      <el-form-item label="学科">
        <el-select v-model="qQuery.subjectId" placeholder="按学科筛选" clearable @change="loadQuestions">
          <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadQuestions">刷新题库</el-button>
    </el-form>
    <el-table :data="questions" stripe max-height="400" @selection-change="onSelect">
      <el-table-column type="selection" width="50" />
      <el-table-column label="题型" width="80"><template #default="{row}">{{ typeText(row.questionType) }}</template></el-table-column>
      <el-table-column prop="content" label="题干" show-overflow-tooltip />
      <el-table-column prop="knowledgePoint" label="知识点" width="120" />
      <el-table-column label="难度" width="100"><template #default="{row}">{{ '★'.repeat(row.difficulty) }}</template></el-table-column>
      <el-table-column label="本题分值" width="120">
        <template #default="{row}">
          <el-input-number v-model="scoreMap[row.id]" :min="0.5" :precision="2" size="small" style="width:100px" />
        </template>
      </el-table-column>
    </el-table>
    <div class="mt-20" style="text-align:center">
      <el-button @click="$router.back()">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="save">保存试卷</el-button>
    </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSubjects, getClasses, pageQuestions, createPaper, updatePaper, teacherPaperDetail } from '@/api'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const isEdit = computed(() => !!route.params.id)
const subjects = ref([])
const classes = ref([])
const questions = ref([])
const selected = ref([])
const scoreMap = reactive({})
const qQuery = reactive({ current: 1, size: 100, subjectId: null })
const form = reactive({
  paperName: '', paperType: 1, subjectId: null, classId: null,
  totalScore: 0, duration: 60, deadline: null, description: '', status: 0
})
const types = [
  { value: 1, label: '单选题' }, { value: 2, label: '多选题' }, { value: 3, label: '判断题' },
  { value: 4, label: '填空题' }, { value: 5, label: '简答题' }, { value: 6, label: '作文题' }, { value: 7, label: '计算题' }
]
const typeText = (t) => types.find(x => x.value === t)?.label || ''

onMounted(async () => {
  const [s, c] = await Promise.all([getSubjects(), getClasses()])
  subjects.value = s.data
  classes.value = c.data
  await loadQuestions()
  if (isEdit.value) {
    loading.value = true
    try {
      const res = await teacherPaperDetail(route.params.id)
      Object.assign(form, res.data.paper)
      res.data.questions.forEach(q => {
        selected.value.push(q.question)
        scoreMap[q.question.id] = q.score
      })
    } finally { loading.value = false }
  }
})

const loadQuestions = async () => {
  const res = await pageQuestions(qQuery)
  questions.value = res.data.records
  // 默认分值
  questions.value.forEach(q => {
    if (!scoreMap[q.id]) scoreMap[q.id] = q.fullScore || 5
  })
}

const onSelect = (rows) => { selected.value = rows }

const save = async () => {
  if (!form.paperName || !form.subjectId || !form.classId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (selected.value.length === 0) {
    ElMessage.warning('请至少选择一道题目')
    return
  }
  submitting.value = true
  try {
    const questionsPayload = selected.value.map(q => ({ questionId: q.id, score: scoreMap[q.id] }))
    const payload = { paper: form, questions: questionsPayload }
    if (isEdit.value) {
      form.id = Number(route.params.id)
      await updatePaper(payload)
    } else {
      await createPaper(payload)
    }
    ElMessage.success('保存成功')
    router.push('/teacher/paper')
  } finally {
    submitting.value = false
  }
}
</script>
