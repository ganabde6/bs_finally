<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">批改管理</h1>
        <p class="page-subtitle">AI 自动初批 + 教师复核,答案雷同查重与人工微调</p>
      </div>
    </div>
    <el-card class="content-card">
    <el-form :inline="true" class="mb-20">
      <el-form-item label="试卷">
        <el-select v-model="paperId" placeholder="请选择试卷" filterable style="width:220px" @change="loadAnswers">
          <el-option v-for="p in papers" :key="p.id" :label="p.paperName" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" :disabled="!paperId" @click="batchC">批量复核</el-button>
      <el-button type="warning" :disabled="!paperId" @click="similarity">答案雷同查重</el-button>
      <el-button type="success" :disabled="!paperId" @click="openClassAnswers">整卷作答总览</el-button>
    </el-form>
    <el-empty v-if="!paperId" description="请先在上方选择一份试卷查看作答情况" :image-size="90" />
    <el-table v-else :data="answers" stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column label="学生" width="140"><template #default="{row}">{{ studentMap[row.studentId] || ('学号' + row.studentId) }}</template></el-table-column>
      <el-table-column label="提交方式" width="100"><template #default="{row}">{{ ['','在线','拍照','语音'][row.submitType] }}</template></el-table-column>
      <el-table-column label="作答时长" width="100"><template #default="{row}">{{ Math.floor(row.duration/60) }}分</template></el-table-column>
      <el-table-column label="总分" width="80"><template #default="{row}">{{ row.totalScore ?? '-' }}</template></el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{row}"><el-tag :type="['info','','success','primary'][row.status]">{{ ['未提交','已提交','已批改','已复核'][row.status] }}</el-tag></template>
      </el-table-column>
      <el-table-column label="提交时间" width="180"><template #default="{row}">{{ formatTime(row.submitTime || row.createTime) }}</template></el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button size="small" type="primary" @click="$router.push({ path: `/teacher/correct/${row.id}`, query: { paperId } })">查看作答情况</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-empty v-if="paperId && !loading && !answers.length" description="该试卷还没有学生提交作答" :image-size="90" />
    </el-card>

    <!-- 整卷作答总览弹窗 -->
    <el-dialog v-model="caVisible" :title="`整卷作答总览 · ${caData.paperName || ''}`" width="900" top="4vh">
      <div v-loading="caLoading">
        <div class="ca-summary">
          <el-tag size="large">班级人数 {{ caData.studentCount || 0 }}</el-tag>
          <el-tag type="success" size="large" style="margin-left:12px">已提交 {{ caData.submittedCount || 0 }}</el-tag>
          <span class="ca-hint">客观题在选项处标注全班作答分布,点击题目标题可展开逐人明细</span>
        </div>
        <el-collapse v-if="caData.questions && caData.questions.length" v-model="caOpen">
          <el-collapse-item v-for="(q, qi) in caData.questions" :key="q.questionId" :name="qi">
            <template #title>
              <div class="ca-q-title">
                <span class="ca-q-index">{{ qi + 1 }}.</span>
                <span class="ca-q-content">{{ q.content }}</span>
                <el-tag size="small" :type="q.objective ? 'success' : 'warning'">
                  {{ q.objective ? '客观题' : '主观题' }}
                </el-tag>
                <el-tag size="small" style="margin-left:8px">
                  答对 {{ q.stat.correct }}/{{ q.stat.answered }} · 均分 {{ q.stat.avgScore }}
                </el-tag>
              </div>
            </template>
            <!-- 客观题:选项分布标注 -->
            <div v-if="q.objective" class="ca-options">
              <div v-for="opt in parseOptions(q.options)" :key="opt.key" class="ca-option-row">
                <span class="ca-opt-key" :class="{ 'ca-opt-right': isRightOption(q, opt.key) }">{{ opt.key }}. {{ opt.value }}</span>
                <el-progress :percentage="optPct(q, opt.key)" :stroke-width="14"
                             :color="isRightOption(q, opt.key) ? '#67C23A' : '#E6A23C'" style="flex:1" />
                <span class="ca-opt-count">{{ optCount(q, opt.key) }} 人{{ isRightOption(q, opt.key) ? '(正确答案)' : '' }}</span>
              </div>
              <div class="ca-std-answer">正确答案:{{ q.standardAnswer }}</div>
            </div>
            <!-- 逐人明细 -->
            <el-table :data="q.rows" size="small" stripe max-height="300">
              <el-table-column label="学生" width="130">
                <template #default="{row}">{{ row.studentName }}（{{ row.username }}）</template>
              </el-table-column>
              <el-table-column label="作答" min-width="200">
                <template #default="{row}">{{ row.studentAnswer || '—' }}</template>
              </el-table-column>
              <el-table-column label="得分" width="100">
                <template #default="{row}">{{ row.score ?? '—' }}<span v-if="row.score != null"> / {{ q.fullScore }}</span></template>
              </el-table-column>
              <el-table-column label="状态" width="110">
                <template #default="{row}">
                  <el-tag size="small" :type="row.status === '已批改' ? (row.isCorrect === 1 ? 'success' : 'danger') : 'info'">
                    {{ row.status === '已批改' ? (row.isCorrect === 1 ? '正确' : '错误') : row.status }}
                  </el-tag>
                </template>
              </el-table-column>
            </el-table>
          </el-collapse-item>
        </el-collapse>
        <el-empty v-else-if="!caLoading" description="该试卷暂无题目" :image-size="70" />
      </div>
      <template #footer>
        <el-button @click="caVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pagePapers, paperAnswers, batchCorrect, similarityCheck, teacherPageStudents, paperClassAnswers } from '@/api'

const route = useRoute()
const papers = ref([])
const paperId = ref(null)
const answers = ref([])
const loading = ref(false)
const studentMap = ref({})

onMounted(async () => {
  const res = await pagePapers({ current: 1, size: 100 })
  papers.value = res.data.records
  // 加载学生姓名映射(静默失败不阻塞)
  teacherPageStudents({ current: 1, size: 200 }).then(r => {
    const map = {}
    ;((r.data && r.data.records) || []).forEach(s => { map[s.id] = `${s.realName}（${s.username}）` })
    studentMap.value = map
  }).catch(() => {})
  if (route.query.paperId) {
    paperId.value = Number(route.query.paperId)
  } else if (papers.value.length) {
    paperId.value = papers.value[0].id
  }
  if (paperId.value) loadAnswers()
})

const loadAnswers = async () => {
  if (!paperId.value) return
  loading.value = true
  try {
    const res = await paperAnswers(paperId.value)
    answers.value = res.data
  } finally { loading.value = false }
}

// 时间格式化: 2026-09-19T21:54:29 -> 2026-09-19 21:54
const formatTime = (t) => {
  if (!t) return '-'
  return String(t).replace('T', ' ').slice(0, 16)
}

const batchC = async () => {
  const res = await batchCorrect(paperId.value)
  const { reviewed = 0, skip = 0, diff = 0 } = res.data || {}
  if (reviewed === 0) {
    ElMessage.info(`没有可复核的作答(跳过 ${skip} 份)`)
  } else if (diff === 0) {
    ElMessage.success(`AI 自检完成:复核 ${reviewed} 份作答,复核结论与初批一致,无存疑题`)
  } else {
    ElMessage.warning(`AI 自检完成:复核 ${reviewed} 份作答,发现 ${diff} 道复核结论与初批不一致的存疑题,请在批改详情中人工确认`)
  }
  loadAnswers()
}

const similarity = async () => {
  const res = await similarityCheck(paperId.value)
  ElMessage.success(`查重完成,命中雷同 ${res.data.similarCount} 处`)
}

// ============ 整卷作答总览 ============
const caVisible = ref(false)
const caLoading = ref(false)
const caData = ref({})
const caOpen = ref([0]) // 默认展开第一题

const openClassAnswers = async () => {
  caVisible.value = true
  caLoading.value = true
  caData.value = {}
  caOpen.value = [0]
  try {
    const res = await paperClassAnswers(paperId.value)
    caData.value = res.data || {}
  } finally { caLoading.value = false }
}

// 解析题目 options JSON -> [{key,value}]
const parseOptions = (options) => {
  if (!options) return []
  try {
    const arr = typeof options === 'string' ? JSON.parse(options) : options
    return (arr || []).map(o => ({ key: o.key, value: o.value }))
  } catch { return [] }
}

// 判断某选项是否为该题正确答案(支持多选/判断)
const isRightOption = (q, key) => {
  const ans = (q.standardAnswer || '').toUpperCase()
  if (q.questionType === 2) return ans.includes(key) // 多选
  return ans === key
}
const optCount = (q, key) => (q.optionDist && q.optionDist[key]) || 0
const optPct = (q, key) => {
  const answered = q.stat ? q.stat.answered : 0
  if (!answered) return 0
  return Math.round(optCount(q, key) * 100 / answered)
}
</script>

<style scoped>
.ca-summary { margin-bottom: 14px; display: flex; align-items: center; }
.ca-hint { margin-left: 14px; font-size: 12px; color: #909399; }
.ca-q-title { display: flex; align-items: center; gap: 8px; overflow: hidden; }
.ca-q-index { font-weight: 600; flex-shrink: 0; }
.ca-q-content { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 480px; }
.ca-options { padding: 4px 0 12px; }
.ca-option-row { display: flex; align-items: center; gap: 10px; margin-bottom: 8px; }
.ca-opt-key { width: 220px; font-size: 13px; flex-shrink: 0; }
.ca-opt-key.ca-opt-right { color: #67C23A; font-weight: 600; }
.ca-opt-count { font-size: 13px; color: #606266; width: 110px; text-align: right; flex-shrink: 0; }
.ca-std-answer { font-size: 13px; color: #67C23A; margin-top: 6px; }
</style>
