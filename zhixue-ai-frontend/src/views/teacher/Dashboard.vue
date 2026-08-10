<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">教师首页</h1>
        <p class="page-subtitle">作业发布、批改进度与班级概况一览</p>
      </div>
    </div>
    <el-row :gutter="20">
      <el-col :span="6"><el-card class="stat-card"><div class="num">{{ stats.paperCount || 0 }}</div><div class="label">我的试卷/作业</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#67C23A">{{ stats.publishedCount || 0 }}</div><div class="label">已发布</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#E6A23C">{{ stats.studentCount || 0 }}</div><div class="label">所教学生</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#F56C6C">{{ stats.pendingCorrect || 0 }}</div><div class="label">待批改数</div></el-card></el-col>
    </el-row>
    <el-card class="mt-20">
      <template #header><span>快捷入口</span></template>
      <el-space wrap>
        <el-button type="primary" :icon="Edit" @click="$router.push('/teacher/question')">题库管理</el-button>
        <el-button type="success" :icon="Document" @click="$router.push('/teacher/paper')">发布作业/考试</el-button>
        <el-button type="warning" :icon="EditPen" @click="$router.push('/teacher/correct')">批改管理</el-button>
        <el-button :icon="TrendCharts" @click="$router.push('/teacher/classAnalysis')">班级学情</el-button>
      </el-space>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { Edit, Document, EditPen, TrendCharts } from '@element-plus/icons-vue'
import { teacherDashboard } from '@/api'

const stats = ref({})
onMounted(async () => {
  try {
    const res = await teacherDashboard()
    stats.value = res.data
  } catch {}
})
</script>
