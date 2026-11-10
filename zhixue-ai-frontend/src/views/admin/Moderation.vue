<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">智能内容风控</h1>
        <p class="page-subtitle">教师/学生内容自动检测,违规自动拦截并记录</p>
      </div>
    </div>

    <el-alert
      title="系统会在教师/学生提交内容(新增题目、创建试卷、提交作答、推送家校反馈、发布公告、AI 助学问答等)时自动进行风控检测。违规内容会被自动拦截,并记录到下方列表中,无需人工复制检测。"
      type="info" :closable="false" class="mb-20" />

    <el-card class="content-card">
      <template #header><span>违规内容记录(系统自动记录)</span></template>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="username" label="用户" width="110" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">{{ roleLabel(row.roleCode) }}</template>
        </el-table-column>
        <el-table-column prop="source" label="内容入口" width="120" />
        <el-table-column prop="content" label="违规内容" show-overflow-tooltip />
        <el-table-column prop="riskReason" label="违规原因" show-overflow-tooltip />
        <el-table-column prop="createTime" label="时间" width="170" />
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无违规内容,触发风控拦截后会自动记录到这里" :image-size="80" />
      <el-pagination class="mt-20" v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadData" />
    </el-card>

  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { pageContentRisks } from '@/api'

const ROLE_LABELS = {
  SUPER_ADMIN: '超级管理员',
  SCHOOL_ADMIN: '校级管理员',
  TEACHER: '教师',
  STUDENT: '学生'
}
const roleLabel = (code) => ROLE_LABELS[code] || code || '-'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    const res = await pageContentRisks(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
</script>
