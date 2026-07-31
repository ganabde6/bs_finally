<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>题库管理</span>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增题目</el-button>
      </div>
    </template>
    <el-form :inline="true" class="mb-20">
      <el-form-item label="学科">
        <el-select v-model="query.subjectId" placeholder="全部" clearable style="width:120px">
          <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="题型">
        <el-select v-model="query.questionType" placeholder="全部" clearable style="width:120px">
          <el-option v-for="t in types" :key="v" :label="t.label" :value="t.value" />
        </el-select>
      </el-form-item>
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="题干/知识点" clearable style="width:200px" />
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
      <el-button type="success" @click="aiGroupVisible = true">AI智能组卷</el-button>
    </el-form>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column label="题型" width="80"><template #default="{row}">{{ typeText(row.questionType) }}</template></el-table-column>
      <el-table-column prop="knowledgePoint" label="知识点" width="120" />
      <el-table-column prop="content" label="题干" show-overflow-tooltip />
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

    <el-dialog v-model="aiGroupVisible" title="AI 智能组卷" width="500">
      <el-form label-width="100px">
        <el-form-item label="学科"><el-select v-model="aiForm.subjectId"><el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="题目数量"><el-input-number v-model="aiForm.totalQuestions" :min="1" :max="50" /></el-form-item>
        <el-form-item label="目标难度"><el-rate v-model="aiForm.difficulty" :max="5" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="aiGroupVisible = false">取消</el-button>
        <el-button type="primary" @click="doAiGroup">生成</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { pageQuestions, addQuestion, updateQuestion, deleteQuestion, getSubjects, aiGroup } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const subjects = ref([])
const types = [
  { value: 1, label: '单选题' }, { value: 2, label: '多选题' }, { value: 3, label: '判断题' },
  { value: 4, label: '填空题' }, { value: 5, label: '简答题' }, { value: 6, label: '作文题' }, { value: 7, label: '计算题' }
]
const typeText = (t) => types.find(x => x.value === t)?.label || ''
const query = reactive({ current: 1, size: 10, subjectId: null, questionType: null, keyword: '' })
const formVisible = ref(false)
const form = reactive({})
const aiGroupVisible = ref(false)
const aiForm = reactive({ subjectId: null, totalQuestions: 10, difficulty: 3 })

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
  const res = await aiGroup(aiForm)
  ElMessage.success(`AI 已为您抽取 ${res.data.length} 道题目,可前往组卷页面使用`)
  aiGroupVisible.value = false
}
</script>
