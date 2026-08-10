<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">批改管理</h1>
        <p class="page-subtitle">AI 批量批改、答案雷同查重与人工复核</p>
      </div>
    </div>
    <el-card class="content-card">
    <el-form :inline="true" class="mb-20">
      <el-form-item label="试卷">
        <el-select v-model="paperId" placeholder="请选择试卷" filterable @change="loadAnswers">
          <el-option v-for="p in papers" :key="p.id" :label="p.paperName" :value="p.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" :disabled="!paperId" @click="batchC">批量 AI 批改</el-button>
      <el-button type="warning" :disabled="!paperId" @click="similarity">答案雷同查重</el-button>
    </el-form>
    <el-table :data="answers" stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column label="学生" prop="studentId" width="100" />
      <el-table-column label="提交方式" width="100"><template #default="{row}">{{ ['','在线','拍照','语音'][row.submitType] }}</template></el-table-column>
      <el-table-column label="作答时长" width="100"><template #default="{row}">{{ Math.floor(row.duration/60) }}分</template></el-table-column>
      <el-table-column label="总分" width="80"><template #default="{row}">{{ row.totalScore ?? '-' }}</template></el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{row}"><el-tag :type="['info','','success','primary'][row.status]">{{ ['未提交','已提交','已批改','已复核'][row.status] }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="submitTime" label="提交时间" width="180" />
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button size="small" type="primary" @click="$router.push(`/teacher/correct/${row.id}`)">查看批改</el-button>
        </template>
      </el-table-column>
    </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pagePapers, paperAnswers, batchCorrect, similarityCheck } from '@/api'

const route = useRoute()
const papers = ref([])
const paperId = ref(null)
const answers = ref([])
const loading = ref(false)

onMounted(async () => {
  const res = await pagePapers({ current: 1, size: 100 })
  papers.value = res.data.records
  if (route.query.paperId) {
    paperId.value = Number(route.query.paperId)
    loadAnswers()
  }
})

const loadAnswers = async () => {
  if (!paperId.value) return
  loading.value = true
  try {
    const res = await paperAnswers(paperId.value)
    answers.value = res.data
  } finally { loading.value = false }
}

const batchC = async () => {
  await batchCorrect(paperId.value)
  ElMessage.success('批量批改完成')
  loadAnswers()
}

const similarity = async () => {
  const res = await similarityCheck(paperId.value)
  ElMessage.success(`查重完成,命中雷同 ${res.data.similarCount} 处`)
}
</script>
