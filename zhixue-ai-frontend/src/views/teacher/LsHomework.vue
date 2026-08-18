<template>
  <div class="ls-homework">
    <el-card shadow="never">
      <div class="page-header">
        <div class="page-header-left">
          <h1 class="page-title">英语听说作业管理</h1>
          <p class="page-subtitle">创建并发布听说作业,查看完成报告</p>
        </div>
        <el-button type="primary" @click="showCreateDialog = true">创建听说作业</el-button>
      </div>

      <el-table :data="homeworkList" stripe>
        <el-table-column prop="title" label="作业名称" min-width="200" />
        <el-table-column prop="gradeLevel" label="学段" width="80">
          <template #default="{ row }">{{ gradeLevelLabel(row.gradeLevel) }}</template>
        </el-table-column>
        <el-table-column prop="groupMode" label="组题模式" width="120">
          <template #default="{ row }">{{ modeLabel(row.groupMode) }}</template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="160" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">查看</el-button>
            <el-button v-if="row.status === 0" link type="success" size="small" @click="publishHomework(row)">发布</el-button>
            <el-button link type="warning" size="small" @click="copyHomework(row)">复制</el-button>
            <el-button link type="info" size="small" @click="viewReport(row)">报告</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建作业弹窗 -->
    <el-dialog v-model="showCreateDialog" title="创建英语听说作业" width="700px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="作业名称">
          <el-input v-model="createForm.title" placeholder="请输入作业名称" />
        </el-form-item>
        <el-form-item label="班级">
          <el-select v-model="createClassId" placeholder="请选择班级" style="width: 100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学段">
          <el-select v-model="createForm.gradeLevel" style="width: 100%">
            <el-option label="初中" :value="2" />
            <el-option label="高中" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item label="截止时间">
          <el-date-picker v-model="createForm.deadline" type="datetime" placeholder="选择截止时间" style="width: 100%" />
        </el-form-item>
        <el-form-item label="组题模式">
          <el-radio-group v-model="createForm.groupMode">
            <el-radio label="STANDARD">按考试标准</el-radio>
            <el-radio label="TOPIC">按话题难度</el-radio>
            <el-radio label="CLASS_ANALYSIS">班级学情</el-radio>
            <el-radio label="CUSTOM">自定义素材</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="组题参数">
          <el-input v-model="createForm.groupParams" type="textarea" :rows="4" placeholder='JSON 格式，例如：{"topics":["旅行","动物"],"questionTypes":["模仿朗读","故事复述"],"count":3,"difficulty":2}' />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">创建作业</el-button>
      </template>
    </el-dialog>

    <!-- 作业详情弹窗 -->
    <el-dialog v-model="showDetailDialog" title="作业详情" width="800px">
      <div v-if="currentHomework">
        <el-descriptions :column="2" border>
          <el-descriptions-item label="作业名称">{{ currentHomework.title }}</el-descriptions-item>
          <el-descriptions-item label="学段">{{ gradeLevelLabel(currentHomework.gradeLevel) }}</el-descriptions-item>
          <el-descriptions-item label="组题模式">{{ modeLabel(currentHomework.groupMode) }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ currentHomework.deadline }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusType(currentHomework.status)">{{ statusLabel(currentHomework.status) }}</el-tag>
          </el-descriptions-item>
        </el-descriptions>

        <div style="margin-top: 20px">
          <div style="font-weight: 600; margin-bottom: 10px">题目列表</div>
          <el-table :data="currentHomework.questions || []" stripe size="small">
            <el-table-column prop="sortOrder" label="序号" width="60" />
            <el-table-column prop="title" label="题目标题" min-width="200" />
            <el-table-column prop="questionType" label="题型" width="100" />
            <el-table-column prop="difficulty" label="难度" width="80">
              <template #default="{ row }">{{ diffLabel(row.difficulty) }}</template>
            </el-table-column>
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="previewQuestion(row)">预览</el-button>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <div style="margin-top: 20px; text-align: right">
          <el-button v-if="currentHomework.status === 0" type="success" @click="publishCurrent">发布作业</el-button>
        </div>
      </div>
    </el-dialog>

    <!-- 题目预览弹窗 -->
    <el-dialog v-model="showQuestionDialog" title="题目预览" width="600px">
      <div v-if="currentQuestion">
        <div style="font-weight: 600; margin-bottom: 10px">{{ currentQuestion.title }}</div>
        <div style="margin-bottom: 10px">
          <el-tag size="small" type="info">{{ currentQuestion.questionType }}</el-tag>
          <el-tag size="small" :type="diffType(currentQuestion.difficulty)" style="margin-left: 8px">{{ diffLabel(currentQuestion.difficulty) }}</el-tag>
        </div>
        <div style="background: #E8F1F4; padding: 12px; border-radius: 4px; margin-bottom: 10px; white-space: pre-wrap" v-html="renderLatex(currentQuestion.content)"></div>
        <div v-if="currentQuestion.referenceText">
          <div style="font-weight: 600; margin-bottom: 6px">参考文本：</div>
          <div style="background: #ecf5ff; padding: 12px; border-radius: 4px; white-space: pre-wrap">{{ currentQuestion.referenceText }}</div>
        </div>
        <div v-if="currentQuestion.scorePoints">
          <div style="font-weight: 600; margin-top: 10px; margin-bottom: 6px">评分要点：</div>
          <div style="background: #f0f9eb; padding: 12px; border-radius: 4px; white-space: pre-wrap">{{ currentQuestion.scorePoints }}</div>
        </div>
      </div>
    </el-dialog>

    <!-- 班级报告弹窗 -->
    <el-dialog v-model="showReportDialog" title="班级作业报告" width="800px">
      <el-table :data="reportData" stripe>
        <el-table-column prop="studentId" label="学生ID" width="100" />
        <el-table-column prop="questionTitle" label="题目" min-width="200" />
        <el-table-column prop="totalScore" label="总分" width="80">
          <template #default="{ row }">
            <span :class="scoreClass(row.totalScore)">{{ row.totalScore }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="aiFeedback" label="AI 评语" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createTime" label="提交时间" width="160" />
      </el-table>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { lsHomeworkList, lsHomeworkCreate, lsHomeworkDetail, lsHomeworkPublish, lsHomeworkCopy, lsHomeworkReport, getClasses } from '@/api'
import { renderLatex } from '@/utils/latex'

const homeworkList = ref([])
const classes = ref([])
const createClassId = ref(null)
const showCreateDialog = ref(false)
const showDetailDialog = ref(false)
const showQuestionDialog = ref(false)
const showReportDialog = ref(false)
const createLoading = ref(false)
const currentHomework = ref(null)
const currentQuestion = ref(null)
const reportData = ref([])

const createForm = ref({
  title: '',
  gradeLevel: 2,
  groupMode: 'STANDARD',
  groupParams: '',
  deadline: null
})

const gradeLevelLabel = (level) => ({ 2: '初中', 3: '高中' })[level] || '未知'
const modeLabel = (mode) => ({ STANDARD: '考试标准', TOPIC: '话题难度', CLASS_ANALYSIS: '班级学情', CUSTOM: '自定义素材' })[mode] || mode
const statusLabel = (status) => ({ 0: '草稿', 1: '已发布', 2: '已结束' })[status] || '未知'
const statusType = (status) => ({ 0: 'info', 1: 'success', 2: 'warning' })[status] || 'info'
const diffLabel = (d) => ({ 1: '简单', 2: '中等', 3: '困难' })[d] || '中等'
const diffType = (d) => ({ 1: 'success', 2: 'warning', 3: 'danger' })[d] || 'warning'
const scoreClass = (s) => {
  if (s >= 85) return 'score-good'
  if (s >= 60) return 'score-mid'
  return 'score-bad'
}

const loadHomeworkList = async () => {
  const res = await lsHomeworkList()
  homeworkList.value = res.data || []
}

const loadClasses = async () => {
  const res = await getClasses()
  classes.value = res.data || []
}

const handleCreate = async () => {
  if (!createForm.value.title) {
    ElMessage.warning('请输入作业名称')
    return
  }
  if (!createClassId.value) {
    ElMessage.warning('请选择班级')
    return
  }
  createLoading.value = true
  try {
    const data = {
      ...createForm.value,
      classId: createClassId.value
    }
    const res = await lsHomeworkCreate(data)
    ElMessage.success('作业创建成功')
    showCreateDialog.value = false
    createForm.value = { title: '', gradeLevel: 2, groupMode: 'STANDARD', groupParams: '', deadline: null }
    createClassId.value = null
    await loadHomeworkList()
  } catch (e) {
    // 拦截器已提示
  } finally {
    createLoading.value = false
  }
}

const viewDetail = async (row) => {
  const res = await lsHomeworkDetail(row.id)
  currentHomework.value = res.data
  showDetailDialog.value = true
}

const publishHomework = async (row) => {
  try {
    await ElMessageBox.confirm('确定发布该作业吗？', '提示', { type: 'warning' })
    await lsHomeworkPublish(row.id)
    ElMessage.success('作业已发布')
    await loadHomeworkList()
  } catch (e) {
    // 用户取消或请求失败
  }
}

const publishCurrent = async () => {
  if (!currentHomework.value) return
  try {
    await ElMessageBox.confirm('确定发布该作业吗？', '提示', { type: 'warning' })
    await lsHomeworkPublish(currentHomework.value.id)
    ElMessage.success('作业已发布')
    showDetailDialog.value = false
    await loadHomeworkList()
  } catch (e) {
    // 用户取消或请求失败
  }
}

const copyHomework = async (row) => {
  try {
    const { value } = await ElMessageBox.prompt('复制作业时是否重新 AI 生成题目？', '复制作业', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /^(是|否)$/,
      inputErrorMessage: '请输入"是"或"否"',
      inputValue: '否'
    })
    const regenerate = value === '是'
    await lsHomeworkCopy(row.id, regenerate)
    ElMessage.success('作业已复制')
    await loadHomeworkList()
  } catch (e) {
    // 用户取消或请求失败
  }
}

const previewQuestion = (row) => {
  currentQuestion.value = row
  showQuestionDialog.value = true
}

const viewReport = async (row) => {
  const res = await lsHomeworkReport(row.id)
  reportData.value = res.data || []
  showReportDialog.value = true
}

onMounted(() => {
  loadHomeworkList()
  loadClasses()
})
</script>

<style scoped>
.ls-homework {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
}
.score-good { color: #67c23a; font-weight: 600; }
.score-mid { color: #e6a23c; font-weight: 600; }
.score-bad { color: #f56c6c; font-weight: 600; }
</style>
