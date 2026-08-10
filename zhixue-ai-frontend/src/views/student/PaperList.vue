<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">作业/考试</h1>
        <p class="page-subtitle">查看老师发布的作业与考试,按时完成作答</p>
      </div>
    </div>
    <el-card class="content-card">
    <el-table :data="papers" stripe>
      <el-table-column prop="paperName" label="名称" />
      <el-table-column label="类型" width="100">
        <template #default="{row}">
          <el-tag :type="row.paperType === 1 ? 'success' : 'danger'">{{ row.paperType === 1 ? '作业' : '考试' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column prop="duration" label="时长(分钟)" width="100" />
      <el-table-column prop="publishTime" label="发布时间" width="180" />
      <el-table-column prop="deadline" label="截止时间" width="180" />
      <el-table-column label="操作" width="120">
        <template #default="{row}">
          <el-button type="primary" size="small" @click="$router.push(`/student/paper/${row.id}`)">进入作答</el-button>
        </template>
      </el-table-column>
    </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { studentPapers } from '@/api'

const papers = ref([])
onMounted(async () => {
  const res = await studentPapers()
  papers.value = res.data || []
})
</script>
