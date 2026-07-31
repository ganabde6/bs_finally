<template>
  <el-card>
    <template #header><span>系统操作日志</span></template>
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
      <el-table-column prop="method" label="方法" show-overflow-tooltip />
      <el-table-column prop="ip" label="IP" width="140" />
      <el-table-column prop="costMs" label="耗时(ms)" width="100" />
      <el-table-column prop="createTime" label="操作时间" width="180" />
    </el-table>
    <el-pagination class="mt-20" v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadData" />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { pageLogs } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const modules = ['用户管理', '角色管理', '班级管理', '学科管理', '题库管理', '试卷管理', '批改管理', 'AI配置', '公告管理', '风控管理']
const query = reactive({ current: 1, size: 10, module: null })

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
