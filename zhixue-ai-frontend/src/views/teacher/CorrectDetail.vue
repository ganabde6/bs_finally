<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">批改详情</h1>
        <p class="page-subtitle">逐题查看 AI 评分与学生作答,可人工微调</p>
      </div>
      <div class="page-header-right">
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>
    <el-card v-loading="loading" class="content-card">
    <div v-for="(d, i) in details" :key="d.record.id" class="correct-item">
      <div class="question-box">
        <el-tag v-if="d.question" size="small">{{ typeText(d.question.questionType) }}</el-tag>
        <el-tag v-if="d.record.reviewStatus === 1" size="small" type="danger" effect="dark" style="margin-left:8px">
          AI 复核存疑,请人工确认
        </el-tag>
        <span class="ml-10">{{ i + 1 }}. <span v-html="d.question ? renderLatex(d.question.content) : '(题目已删除)'"></span></span>
        <span style="color:#909399;margin-left:8px">(满分 {{ d.record.fullScore }})</span>
      </div>
      <div class="mt-20"><strong>学生答案:</strong><div class="answer-box">{{ d.record.studentAnswer || '(未作答)' }}</div></div>
      <div class="mt-20"><strong>标准答案:</strong><span v-if="d.question" v-html="renderLatex(d.question.standardAnswer)"></span><span v-else>—</span></div>
      <div class="mt-20"><strong>AI 评分:</strong>
        <el-input-number v-model="d.record.score" :precision="2" :min="0" :max="d.record.fullScore" size="small" style="width:120px;margin:0 10px" />
        <el-tag :type="['danger','success','warning'][d.record.isCorrect]">{{ ['错误','正确','部分对'][d.record.isCorrect] }}</el-tag>
      </div>
      <div class="mt-20"><strong>批改备注:</strong>
        <el-input v-model="d.record.correctRemark" type="textarea" :rows="2" style="margin-top:6px" />
      </div>
      <div class="mt-20"><strong>得分点详情:</strong>
        <div v-if="detailRows(d.record.scoreDetail).length" class="score-detail">
          <div v-for="(row, ri) in detailRows(d.record.scoreDetail)" :key="ri" class="score-row">
            <span class="score-label">{{ row.label }}</span>
            <span class="score-desc">{{ row.desc || '—' }}</span>
            <el-tag v-if="row.hit != null" size="small" :type="row.hit ? 'success' : 'danger'">{{ row.hit ? '命中' : '未命中' }}</el-tag>
          </div>
        </div>
        <span v-else style="color:#909399">—</span>
      </div>
      <div class="mt-20">
        <el-button type="primary" size="small" @click="saveAdjust(d.record)">保存微调</el-button>
        <el-button size="small" @click="toggleClassView(d)">{{ classView[d.question?.id]?.visible ? '收起全班作答' : '全班作答情况' }}</el-button>
      </div>
      <!-- 全班作答情况 -->
      <div v-if="classView[d.question?.id]?.visible" class="class-view" v-loading="classView[d.question.id].loading">
        <div class="cv-stat" v-if="classView[d.question.id].stat">
          <el-tag>班级人数 {{ classView[d.question.id].stat.total }}</el-tag>
          <el-tag type="success" style="margin-left:8px">答对 {{ classView[d.question.id].stat.correct }}</el-tag>
          <el-tag type="danger" style="margin-left:8px">答错 {{ classView[d.question.id].stat.wrong }}</el-tag>
          <el-tag type="info" style="margin-left:8px">未作答 {{ classView[d.question.id].stat.unanswered }}</el-tag>
          <el-tag type="warning" style="margin-left:8px">正确率 {{ classView[d.question.id].stat.correctRate }}</el-tag>
        </div>
        <el-table :data="classView[d.question.id].rows || []" size="small" stripe class="mt-10" max-height="360">
          <el-table-column prop="studentName" label="姓名" width="90" />
          <el-table-column prop="username" label="学号" width="100" />
          <el-table-column label="状态" width="110">
            <template #default="{row}">
              <el-tag size="small" :type="row.status === '已批改' ? (row.isCorrect === 1 ? 'success' : 'danger') : 'info'">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="作答内容" min-width="200" show-overflow-tooltip>
            <template #default="{row}">{{ row.studentAnswer || '-' }}</template>
          </el-table-column>
          <el-table-column label="得分" width="80">
            <template #default="{row}">{{ row.score ?? '-' }}</template>
          </el-table-column>
          <el-table-column label="AI 点评" min-width="180" show-overflow-tooltip>
            <template #default="{row}">{{ row.remark || '-' }}</template>
          </el-table-column>
        </el-table>
        <el-empty v-if="!classView[d.question.id].loading && !(classView[d.question.id].rows || []).length" description="该班级暂无学生数据" :image-size="60" />
      </div>
    </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { teacherCorrectDetail, adjustCorrect, questionClassAnswers, teacherPaperDetail } from '@/api'
import { renderLatex } from '@/utils/latex'

const route = useRoute()
const details = ref([])
const loading = ref(false)
const classId = ref(null) // 试卷所属班级,用于查询全班作答
const classView = reactive({}) // questionId -> {visible, loading, stat, rows}
const types = [
  { value: 1, label: '单选题' }, { value: 2, label: '多选题' },
  { value: 4, label: '填空题' }, { value: 5, label: '简答题' }, { value: 6, label: '作文题' }, { value: 7, label: '计算题' }
]
const typeText = (t) => types.find(x => x.value === t)?.label || ''
// 把后端存的得分点 JSON 解析成可读行:兼容得分点 {point/step,maxScore,gotScore,hit}、
// 客观题 {step,score} 和作文三维度 {dim,value,comment} 三种结构
const detailRows = (s) => {
  if (!s) return []
  let arr
  try { arr = JSON.parse(s) } catch { return [] }
  if (!Array.isArray(arr)) return []
  return arr.map(it => {
    const label = it.point || it.step || it.dim || '评分项'
    let desc = ''
    if (it.maxScore != null) desc = `得分 ${it.gotScore ?? 0} / ${it.maxScore}`
    else if (it.score != null) desc = `得分 ${it.score}`
    else if (it.comment) desc = `${it.value != null ? it.value + ' · ' : ''}${it.comment}`
    return { label, desc, hit: it.hit }
  })
}

onMounted(async () => {
  loading.value = true
  try {
    const res = await teacherCorrectDetail(route.params.answerId)
    details.value = res.data
  } finally { loading.value = false }
})

const toggleClassView = async (d) => {
  const qid = d.question?.id
  if (!qid) return
  let view = classView[qid]
  if (!view) {
    view = classView[qid] = reactive({ visible: false, loading: false, stat: null, rows: [] })
  }
  view.visible = !view.visible
  if (!view.visible || view.rows.length) return // 已有数据不重复加载
  // 懒加载班级ID:优先从路由 query 取 paperId,再查试卷详情
  if (!classId.value && route.query.paperId) {
    try {
      const pr = await teacherPaperDetail(route.query.paperId)
      classId.value = pr.data?.paper?.classId
    } catch {}
  }
  if (!classId.value) {
    ElMessage.warning('未能获取试卷所属班级')
    return
  }
  view.loading = true
  try {
    const res = await questionClassAnswers(qid, classId.value)
    view.stat = res.data?.stat || null
    view.rows = res.data?.rows || []
  } finally {
    view.loading = false
  }
}

const saveAdjust = async (r) => {
  await adjustCorrect(r.id, { score: r.score, remark: r.correctRemark })
  ElMessage.success('已微调')
}
</script>

<style scoped>
.correct-item { padding: 16px 0; border-bottom: 1px dashed #E8ECF1; }
.question-box { font-size: 15px; line-height: 1.8; }
.answer-box { background: #E8ECF1; padding: 10px; border-radius: 4px; margin-top: 6px; white-space: pre-wrap; }
.score-detail { background: #FAFBFC; border: 1px solid #E8ECF1; border-radius: 8px; padding: 10px 14px; margin-top: 6px; }
.score-row { display: flex; align-items: center; gap: 10px; padding: 5px 0; border-bottom: 1px dashed #E8ECF1; }
.score-row:last-child { border-bottom: none; }
.score-label { font-weight: 600; min-width: 120px; }
.score-desc { color: #606266; flex: 1; }
pre { background: #E8ECF1; padding: 10px; border-radius: 4px; margin-top: 6px; font-size: 12px; }
.ml-10 { margin-left: 10px; }
.class-view { margin-top: 12px; padding: 12px; background: #FAFBFC; border: 1px solid #E8ECF1; border-radius: 8px; }
.cv-stat { display: flex; flex-wrap: wrap; }
.mt-10 { margin-top: 10px; }
</style>
