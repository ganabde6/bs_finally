<template>
  <el-card>
    <template #header><span>作业/考试列表</span></template>
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
