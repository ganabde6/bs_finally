<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">系统操作日志</h1>
        <p class="page-subtitle">查看系统操作与接口调用记录</p>
      </div>
    </div>
    <el-card class="content-card">
    <el-form :inline="true" class="mb-20">
      <el-form-item label="模块">
        <el-select v-model="query.module" placeholder="全部" clearable style="width:160px" @change="loadData">
          <el-option v-for="m in modules" :key="m" :label="m" :value="m" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
    </el-form>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="username" label="用户" width="120" />
      <el-table-column prop="module" label="模块" width="120" />
      <el-table-column prop="operation" label="操作" show-overflow-tooltip />
      <el-table-column label="方法" width="90">
        <template #default="{ row }">
          <el-tag v-if="httpMethod(row.method)" :type="httpMethod(row.method).type" size="small">{{ httpMethod(row.method).label }}</el-tag>
          <span v-else-if="row.method" class="legacy-method">{{ legacyMethod(row.method) }}</span>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="costMs" label="耗时(ms)" width="100" />
      <el-table-column prop="createTime" label="操作时间" width="180" />
    </el-table>
    <el-pagination class="mt-20" v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadData" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { pageLogs } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const modules = ['用户管理', '角色管理', '班级管理', '学科管理', '任课管理', '题库管理', '试卷管理', '批改管理', 'AI组卷', 'AI配置', '公告管理', '风控管理', '学员管理', '家校反馈', '数据大屏', '系统日志']
const query = reactive({ current: 1, size: 10, module: null })

// "方法"栏:把 HTTP 请求方法翻译成管理员能看懂的中文
const HTTP_METHODS = {
  GET: { type: 'info', label: '查询' },
  POST: { type: 'success', label: '新增' },
  PUT: { type: 'warning', label: '修改' },
  PATCH: { type: 'warning', label: '修改' },
  DELETE: { type: 'danger', label: '删除' }
}
const httpMethod = (m) => (m && HTTP_METHODS[String(m).toUpperCase()]) || null
// 兼容旧数据:早期 method 存的是 Java 方法签名,只取末尾方法名展示
const legacyMethod = (m) => String(m).split(/[#.]/).pop()

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    const res = await pageLogs(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.legacy-method { color: #909399; font-size: 12px; }
.text-muted { color: #c0c4cc; }
</style>
