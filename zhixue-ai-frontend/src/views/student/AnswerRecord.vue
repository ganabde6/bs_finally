<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">作答记录</h1>
        <p class="page-subtitle">查看已完成作业/考试的作答结果与 AI 批改详情</p>
      </div>
    </div>
    <el-card class="content-card">
      <div class="filter-bar">
        <span class="filter-label">科目筛选:</span>
        <el-select v-model="subjectFilter" placeholder="全部科目" clearable size="default" style="width: 160px" @change="load">
          <el-option v-for="s in subjects" :key="s.subjectId" :label="s.subjectName + (s.noRecord ? '(暂无记录)' : '')" :value="s.subjectId" />
        </el-select>
        <span class="filter-label" style="margin-left:16px">时间筛选:</span>
        <el-date-picker v-model="dateRange" type="daterange" value-format="YYYY-MM-DD"
                        range-separator="至" start-placeholder="开始日期" end-placeholder="结束日期"
                        size="default" style="width: 260px" @change="load" />
      </div>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column label="科目" width="100">
          <template #default="{row}">{{ row.subjectName || '-' }}</template>
        </el-table-column>
        <el-table-column label="试卷" min-width="160">
          <template #default="{row}">{{ paperMap[row.paperId]?.paperName || ('试卷#' + row.paperId) }}</template>
        </el-table-column>
        <el-table-column label="类型" width="90">
          <template #default="{row}">
            <el-tag :type="paperMap[row.paperId]?.paperType === 1 ? 'success' : 'danger'" size="small">
              {{ paperMap[row.paperId]?.paperType === 1 ? '作业' : '考试' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="得分" width="100">
          <template #default="{row}">{{ row.totalScore ?? '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="['warning','','success'][row.status] || 'info'" size="small">
              {{ ['未提交','已提交','已批改'][row.status] || '未知' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="作答时长" width="100">
          <template #default="{row}">{{ row.duration ? Math.floor(row.duration/60) + '分' : '-' }}</template>
        </el-table-column>
        <el-table-column label="提交时间" width="160">
          <template #default="{row}">{{ formatTime(row.submitTime || row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="primary" :disabled="row.status < 2" @click="openDetail(row)">
              {{ row.status >= 2 ? '查看批改' : '未批改' }}
            </el-button>
            <el-button size="small" type="danger" plain @click="removeRecord(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && !list.length" description="还没有作答记录,去「作业/考试」完成一份作业吧" :image-size="90" />
    </el-card>

    <!-- 批改详情弹窗 -->
    <el-dialog v-model="detailVisible" :title="`批改详情 · ${detailPaperName}`" width="720" top="5vh">
      <div v-loading="detailLoading">
        <div v-if="detailSummary" class="summary-bar">
          <el-tag type="success" size="large">总分 {{ detailSummary.totalScore }}</el-tag>
          <el-tag size="large" style="margin-left:12px">对 {{ detailSummary.correctCount }} 题</el-tag>
          <el-tag type="danger" size="large" style="margin-left:12px">错 {{ detailSummary.wrongCount }} 题</el-tag>
        </div>
        <div v-for="(item, i) in details" :key="i" class="question-item">
          <div class="q-head">
            <span class="q-index">{{ i + 1 }}.</span>
            <el-tag size="small" :type="item.record.isCorrect === 1 ? 'success' : 'danger'">
              {{ item.record.isCorrect === 1 ? '正确' : '错误' }} · {{ item.record.score }}/{{ item.record.fullScore }}分
            </el-tag>
          </div>
          <div class="q-content">{{ item.question.content }}</div>
          <div class="q-line"><span class="q-label">我的作答:</span><span :class="item.record.isCorrect === 1 ? 'ans-ok' : 'ans-bad'">{{ item.record.studentAnswer || '(未作答)' }}</span></div>
          <div class="q-line"><span class="q-label">参考答案:</span><span>{{ item.question.standardAnswer }}</span></div>
          <div class="q-line" v-if="item.record.correctRemark"><span class="q-label">AI 点评:</span><span>{{ item.record.correctRemark }}</span></div>
        </div>
        <el-empty v-if="!detailLoading && !details.length" description="暂无批改明细" :image-size="70" />
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { myAnswers, myAnswerSubjects, correctDetail, studentPapers, deleteAnswer } from '@/api'

const list = ref([])
const loading = ref(false)
const paperMap = ref({}) // paperId -> paper
const subjectFilter = ref(null) // null/空 = 全部科目
const dateRange = ref(null) // [startDate, endDate] 或 null
const subjects = ref([]) // 筛选下拉科目列表(动态)

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailPaperName = ref('')
const detailSummary = ref(null)
const details = ref([])

const load = async () => {
  loading.value = true
  try {
    const res = await myAnswers(
      subjectFilter.value || undefined,
      dateRange.value?.[0] || undefined,
      dateRange.value?.[1] || undefined
    )
    list.value = res.data || []
    // 拉取试卷信息用于显示名称/类型
    try {
      const pr = await studentPapers()
      const map = {}
      ;(pr.data || []).forEach(p => { map[p.id] = p })
      paperMap.value = map
    } catch {}
  } finally {
    loading.value = false
  }
}

// 时间格式化: 2026-09-19T21:54:29 -> 2026-09-19 21:54
const formatTime = (t) => {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

const removeRecord = async (row) => {
  try {
    await ElMessageBox.confirm('删除后该记录及批改详情将无法恢复,确定删除吗?', '删除作答记录', {
      type: 'warning', confirmButtonText: '删除', cancelButtonText: '取消'
    })
  } catch { return }
  await deleteAnswer(row.id)
  ElMessage.success('删除成功')
  load()
}

const openDetail = async (row) => {
  detailPaperName.value = paperMap.value[row.paperId]?.paperName || ('试卷#' + row.paperId)
  detailVisible.value = true
  detailLoading.value = true
  details.value = []
  detailSummary.value = null
  try {
    const res = await correctDetail(row.id)
    details.value = res.data || []
  } finally {
    detailLoading.value = false
  }
}

// 拉取科目筛选下拉(动态:先取有作答记录的科目,再按学段补齐)
const loadSubjects = async () => {
  try {
    const res = await myAnswerSubjects()
    subjects.value = res.data || []
  } catch {}
}

onMounted(() => { load(); loadSubjects() })
</script>

<style scoped>
.filter-bar { display: flex; align-items: center; gap: 8px; margin-bottom: 14px; }
.filter-label { font-size: 14px; color: #606266; }
.summary-bar { margin-bottom: 16px; }
.question-item { padding: 14px 0; border-bottom: 1px dashed #E8F1F4; }
.question-item:last-of-type { border-bottom: none; }
.q-head { display: flex; align-items: center; gap: 8px; margin-bottom: 8px; }
.q-index { font-weight: 600; }
.q-content { font-size: 14px; line-height: 1.7; margin-bottom: 10px; white-space: pre-wrap; }
.q-line { font-size: 13px; line-height: 1.8; }
.q-label { color: #909399; margin-right: 4px; }
.ans-ok { color: #67C23A; }
.ans-bad { color: #F56C6C; }
</style>
