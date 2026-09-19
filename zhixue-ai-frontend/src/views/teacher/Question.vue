<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">题库管理</h1>
        <p class="page-subtitle">维护学科题库,支持批量管理与快速预览</p>
      </div>
      <div class="page-header-right">
        <el-button type="primary" :icon="Plus" @click="$router.push('/teacher/aiGroup')">智能出题</el-button>
        <el-button type="success" :icon="Plus" @click="openAdd">新增题目</el-button>
        <el-button type="danger" plain :disabled="!selection.length" @click="batchDel">批量删除({{ selection.length }})</el-button>
      </div>
    </div>
    <el-card class="content-card">
    <el-form :inline="true" class="mb-20">
      <el-form-item label="题型">
        <el-select v-model="query.questionType" placeholder="全部" clearable style="width:110px">
          <el-option v-for="t in filteredTypes" :key="t.value" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="难度">
        <el-select v-model="query.difficulty" placeholder="全部" clearable style="width:100px">
          <el-option v-for="d in [1,2,3,4,5]" :key="d" :label="`${'★'.repeat(d)} (${d})`" :value="d" />
        </el-select>
      </el-form-item>
      <el-form-item label="知识点">
        <el-select v-model="query.knowledgePoint" placeholder="全部" clearable filterable style="width:150px">
          <el-option v-for="kp in knowledgePoints" :key="kp" :label="kp" :value="kp" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="题干关键词" clearable style="width:180px" @keyup.enter="search" />
      </el-form-item>
      <el-button type="primary" @click="search">查询</el-button>
    </el-form>
    <el-table :data="list" stripe v-loading="loading" @selection-change="onSelectionChange">
      <el-table-column type="selection" width="42" />
      <el-table-column type="index" label="#" width="50" />
      <el-table-column label="题型" width="86">
        <template #default="{row}"><el-tag size="small" :type="typeTag(row.questionType)" effect="light">{{ typeText(row.questionType) }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="knowledgePoint" label="知识点" width="130" show-overflow-tooltip />
      <el-table-column label="题干" show-overflow-tooltip>
        <template #default="{row}"><span v-html="renderLatex(row.content)"></span></template>
      </el-table-column>
      <el-table-column label="难度" width="110">
        <template #default="{row}">{{ '★'.repeat(row.difficulty) }}</template>
      </el-table-column>
      <el-table-column prop="fullScore" label="满分" width="70" />
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{row}">
          <el-button size="small" type="primary" link @click="openPreview(row)">预览</el-button>
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" link @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="mt-20" v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadData" />
    </el-card>

    <!-- 新增/编辑弹窗: 动态表单,免 JSON -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑题目' : '新增题目'" width="720" top="6vh">
      <el-form :model="form" label-width="100px">
        <el-row :gutter="12">
          <el-col :span="8"><el-form-item label="题型"><el-select v-model="form.questionType" style="width:100%" @change="onTypeChange"><el-option v-for="t in filteredTypes" :key="t.value" :label="t.label" :value="t.value" /></el-select></el-form-item></el-col>
          <el-col :span="8"><el-form-item label="难度"><el-rate v-model="form.difficulty" :max="5" style="margin-top:6px" /></el-form-item></el-col>
        </el-row>
        <el-row :gutter="12">
          <el-col :span="16"><el-form-item label="知识点">
            <el-select v-model="form.knowledgePoint" filterable allow-create default-first-option placeholder="选择或输入新知识点" style="width:100%">
              <el-option v-for="kp in formKnowledgePoints" :key="kp" :label="kp" :value="kp" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="8"><el-form-item label="满分"><el-input-number v-model="form.fullScore" :precision="2" :min="0" style="width:100%" /></el-form-item></el-col>
        </el-row>
        <el-form-item label="题干"><el-input v-model="form.content" type="textarea" :rows="3" placeholder="支持 LaTeX 公式" /></el-form-item>

        <!-- 选择题: 动态选项 -->
        <el-form-item v-if="isChoice" label="选项">
          <div class="opt-list">
            <div v-for="(opt, i) in optionRows" :key="i" class="opt-row">
              <el-tag class="opt-key">{{ opt.key }}</el-tag>
              <el-input v-model="opt.value" placeholder="选项内容,支持 LaTeX" />
              <el-button v-if="optionRows.length > 2" type="danger" link :icon="Delete" @click="optionRows.splice(i, 1)" />
            </div>
            <el-button v-if="optionRows.length < 8" size="small" :icon="Plus" @click="addOption">添加选项</el-button>
          </div>
        </el-form-item>

        <!-- 单选题: 标准答案为选项 key -->
        <el-form-item v-if="form.questionType === 1" label="标准答案">
          <el-radio-group v-model="form.standardAnswer">
            <el-radio v-for="opt in optionRows" :key="opt.key" :value="opt.key">{{ opt.key }}</el-radio>
          </el-radio-group>
        </el-form-item>
        <!-- 多选题: 标准答案为多个 key -->
        <el-form-item v-else-if="form.questionType === 2" label="标准答案">
          <el-checkbox-group v-model="correctKeys">
            <el-checkbox v-for="opt in optionRows" :key="opt.key" :value="opt.key">{{ opt.key }}</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <!-- 填空/简答/作文/计算: 文本答案 -->
        <el-form-item v-else label="标准答案"><el-input v-model="form.standardAnswer" type="textarea" :rows="2" placeholder="支持 LaTeX 公式" /></el-form-item>

        <!-- 主观题: 动态得分点 -->
        <el-form-item v-if="isSubjective" label="得分点">
          <div class="opt-list">
            <div v-for="(sp, i) in scorePointRows" :key="i" class="opt-row">
              <el-input v-model="sp.point" placeholder="得分点描述" style="flex:1" />
              <el-input-number v-model="sp.score" :precision="1" :min="0" :max="form.fullScore || 100" placeholder="分值" style="width:110px" />
              <el-button type="danger" link :icon="Delete" @click="scorePointRows.splice(i, 1)" />
            </div>
            <el-button size="small" :icon="Plus" @click="scorePointRows.push({ point: '', score: 1 })">添加得分点</el-button>
          </div>
        </el-form-item>

        <el-form-item label="解析"><el-input v-model="form.analysis" type="textarea" :rows="2" placeholder="选填" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 题目预览抽屉 -->
    <el-drawer v-model="previewVisible" title="题目预览" size="480">
      <template v-if="preview">
        <div class="preview-tags">
          <el-tag size="small" :type="typeTag(preview.questionType)">{{ typeText(preview.questionType) }}</el-tag>
          <el-tag size="small" type="info">{{ preview.knowledgePoint || '未分类' }}</el-tag>
          <el-tag size="small" type="warning">{{ '★'.repeat(preview.difficulty) }}</el-tag>
          <el-tag size="small">满分 {{ preview.fullScore }}</el-tag>
        </div>
        <div class="preview-block"><div class="preview-label">题干</div><div class="preview-content" v-html="renderLatex(preview.content)"></div></div>
        <div v-if="parsedOptions(preview).length" class="preview-block">
          <div class="preview-label">选项</div>
          <div v-for="opt in parsedOptions(preview)" :key="opt.key" class="preview-option" :class="{ correct: isCorrectOption(preview, opt.key) }">
            <strong>{{ opt.key }}.</strong> <span v-html="renderLatex(opt.value)"></span>
          </div>
        </div>
        <div class="preview-block"><div class="preview-label">标准答案</div><div class="preview-content" v-html="renderLatex(preview.standardAnswer)"></div></div>
        <div v-if="parsedScorePoints(preview).length" class="preview-block">
          <div class="preview-label">得分点</div>
          <el-tag v-for="(sp, i) in parsedScorePoints(preview)" :key="i" size="small" style="margin:0 6px 6px 0">{{ sp.point }}({{ sp.score }}分)</el-tag>
        </div>
        <div v-if="preview.analysis" class="preview-block"><div class="preview-label">解析</div><div class="preview-content" v-html="renderLatex(preview.analysis)"></div></div>
      </template>
    </el-drawer>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Delete } from '@element-plus/icons-vue'
import { pageQuestions, addQuestion, updateQuestion, deleteQuestion, teacherMySubjects, teacherKnowledgePoints, teacherQuestionTypes } from '@/api'
import { renderLatex } from '@/utils/latex'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const subjects = ref([])
const knowledgePoints = ref([])
const selection = ref([])
// 题型列表由后端按学科动态返回(学科-题型映射), 表格标签仍用全量映射兜底
const allTypes = [
  { value: 1, label: '单选题' }, { value: 2, label: '多选题' },
  { value: 4, label: '填空题' }, { value: 5, label: '简答题' }, { value: 6, label: '作文题' }, { value: 7, label: '计算题' }
]
const types = ref(allTypes)
const filteredTypes = types
const typeText = (t) => allTypes.find(x => x.value === t)?.label || ''
// 题型对应的彩色标签类型
const typeTag = (t) => ({ 1: 'primary', 2: 'success', 4: 'warning', 5: 'danger', 6: 'danger', 7: 'info' }[t] || 'info')
const isChoice = computed(() => [1, 2, 3].includes(form.questionType))
const isSubjective = computed(() => [5, 6, 7].includes(form.questionType))

const query = reactive({ current: 1, size: 10, subjectId: null, questionType: null, difficulty: null, knowledgePoint: null, keyword: '' })
const formVisible = ref(false)
const form = reactive({})
// 动态选项行 / 得分点行
const optionRows = ref([{ key: 'A', value: '' }, { key: 'B', value: '' }])
const scorePointRows = ref([])
const correctKeys = ref([])
const formKnowledgePoints = ref([])

// 预览
const previewVisible = ref(false)
const preview = ref(null)
const parseList = (s) => { try { return JSON.parse(s) || [] } catch { return [] } }
const parsedOptions = (row) => (row ? parseList(row.options) : [])
const parsedScorePoints = (row) => (row && [5, 6, 7].includes(row.questionType) ? parseList(row.scorePoint) : [])
const isCorrectOption = (row, key) => row.questionType === 1
  ? row.standardAnswer === key
  : row.questionType === 2 && parseList(row.options).length && (row.standardAnswer || '').split(',').map(s => s.trim()).includes(key)

const onSelectionChange = (rows) => { selection.value = rows }

onMounted(async () => {
  // 仅加载本人任教学科;单科教师无需选择学科,知识点筛选直接按任教学科加载
  const s = await teacherMySubjects()
  subjects.value = s.data || []
  if (subjects.value.length) {
    const res = await teacherKnowledgePoints(subjects.value[0].id)
    knowledgePoints.value = res.data || []
    // 按学科动态拉取专属题型(后端学科-题型映射)
    const tr = await teacherQuestionTypes(subjects.value[0].id)
    types.value = tr.data || allTypes
    // 学科加载后: 若已选题型不属于当前学科, 清空该筛选条件
    const allowed = types.value.map(t => t.value)
    if (query.questionType && !allowed.includes(query.questionType)) query.questionType = null
  }
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const params = { ...query }
    // 知识点筛选走关键词字段(后端按知识点模糊匹配)
    if (params.knowledgePoint) params.keyword = params.knowledgePoint
    const res = await pageQuestions(params)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const search = () => { query.current = 1; loadData() }

// 学科自动带出: 单科教师无需选择,直接使用唯一任教学科
const mySubjectId = () => subjects.value[0]?.id
const initKnowledgePoints = async () => {
  const sid = mySubjectId()
  if (!sid) { formKnowledgePoints.value = []; return }
  const res = await teacherKnowledgePoints(sid)
  formKnowledgePoints.value = res.data || []
}

const addOption = () => {
  const keys = 'ABCDEFGH'
  optionRows.value.push({ key: keys[optionRows.value.length], value: '' })
}

/** 题型切换: 初始化对应结构 */
const onTypeChange = (t) => {
  if ([1, 2, 3].includes(t) && optionRows.value.length < 3) {
    while (optionRows.value.length < 4) addOption()
  }
  if ([5, 6, 7].includes(t) && scorePointRows.value.length === 0) {
    scorePointRows.value = [{ point: '', score: 1 }]
  }
}

/** 行数据 → 表单结构(JSON 字符串转动态行) */
const rowToForm = (row) => {
  Object.keys(form).forEach(k => delete form[k])
  Object.assign(form, row)
  optionRows.value = parseList(row.options)
  if (isChoice.value && optionRows.value.length === 0) optionRows.value = [{ key: 'A', value: '' }, { key: 'B', value: '' }]
  scorePointRows.value = parseList(row.scorePoint)
  correctKeys.value = row.questionType === 2 ? (row.standardAnswer || '').split(',').map(s => s.trim()).filter(Boolean) : []
}

const openAdd = () => {
  Object.keys(form).forEach(k => delete form[k])
  form.questionType = 1
  form.difficulty = 3
  form.fullScore = 5
  // 学科自动带出任教学科,无需选择
  form.subjectId = mySubjectId()
  optionRows.value = [{ key: 'A', value: '' }, { key: 'B', value: '' }, { key: 'C', value: '' }, { key: 'D', value: '' }]
  scorePointRows.value = []
  correctKeys.value = []
  initKnowledgePoints()
  formVisible.value = true
}

const openEdit = (row) => {
  rowToForm(row)
  initKnowledgePoints()
  formVisible.value = true
}

const openPreview = (row) => {
  preview.value = row
  previewVisible.value = true
}

/** 表单结构 → 保存 payload(动态行转回 JSON 字符串) */
const buildPayload = () => {
  const data = { ...form }
  if (isChoice.value) {
    data.options = JSON.stringify(optionRows.value.map(o => ({ key: o.key, value: o.value })))
    if (form.questionType === 2) data.standardAnswer = correctKeys.value.slice().sort().join(',')
  } else {
    data.options = null
  }
  if (isSubjective.value) {
    data.scorePoint = JSON.stringify(scorePointRows.value.filter(sp => sp.point && sp.point.trim()))
  } else {
    data.scorePoint = null
  }
  return data
}

const validate = () => {
  if (!form.subjectId) return '请选择学科'
  if (!form.content || !form.content.trim()) return '请填写题干'
  if (!form.knowledgePoint || !form.knowledgePoint.trim()) return '请选择或填写知识点'
  if (isChoice.value && optionRows.value.some(o => !o.value.trim())) return '请填写完整的选项内容'
  if (isChoice.value && !form.standardAnswer) return '请选择标准答案'
  if (isSubjective.value && !form.standardAnswer?.trim()) return '请填写标准答案'
  return null
}

const save = async () => {
  const err = validate()
  if (err) { ElMessage.warning(err); return }
  const payload = buildPayload()
  if (form.id) await updateQuestion(payload)
  else await addQuestion(payload)
  ElMessage.success('保存成功')
  formVisible.value = false
  loadData()
}

const del = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该题目?', '提示', { type: 'warning' })
  } catch { return }
  await deleteQuestion(id)
  ElMessage.success('已删除')
  loadData()
}

const batchDel = async () => {
  try {
    await ElMessageBox.confirm(`确定删除选中的 ${selection.value.length} 道题目?`, '提示', { type: 'warning' })
  } catch { return }
  for (const row of selection.value) {
    await deleteQuestion(row.id)
  }
  ElMessage.success(`已删除 ${selection.value.length} 道题目`)
  selection.value = []
  loadData()
}
</script>

<style scoped>
.opt-list { width: 100%; display: flex; flex-direction: column; gap: 8px; }
.opt-row { display: flex; align-items: center; gap: 8px; }
.opt-key { width: 32px; justify-content: center; }

.preview-tags { display: flex; gap: 8px; margin-bottom: 16px; }
.preview-block { margin-bottom: 18px; }
.preview-label { font-size: 13px; font-weight: 600; color: #909399; margin-bottom: 6px; }
.preview-content { font-size: 14px; line-height: 1.8; color: #303133; }
.preview-option { padding: 6px 10px; border-radius: 6px; font-size: 14px; line-height: 1.6; }
.preview-option.correct { background: #F0F9EB; color: #67C23A; font-weight: 500; }
</style>
