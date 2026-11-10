<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">学习首页</h1>
        <p class="page-subtitle">欢迎回来,今日也要加油哦</p>
      </div>
    </div>
    <el-row :gutter="20">
      <el-col :span="6" v-for="(c, i) in cards" :key="i">
        <el-card class="stat-card">
          <div class="num">{{ c.value }}</div>
          <div class="label">{{ c.label }}</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="mt-20">
      <template #header><span>最新公告</span></template>
      <el-empty v-if="!notices.length" description="暂无公告" />
      <el-timeline v-else>
        <el-timeline-item v-for="n in notices" :key="n.id" :timestamp="n.publishTime">
          <h4>{{ n.title }}</h4>
          <p style="color:#606266">{{ n.content }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>
    <el-card class="mt-20">
      <template #header><span>教师任务</span></template>
      <el-empty v-if="!tasks.length" description="暂无教师派发的任务" :image-size="60" />
      <el-table v-else :data="tasks" stripe>
        <el-table-column prop="title" label="任务" width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="要求" show-overflow-tooltip />
        <el-table-column label="截止时间" width="180">
          <template #default="{row}">{{ row.deadline || '不限时' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button type="primary" size="small" @click="completeTask(row.recordId)">完成</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-card class="mt-20">
      <template #header><span>我的待办</span></template>
      <el-table :data="pendingPapers" stripe>
        <el-table-column prop="paperName" label="名称" />
        <el-table-column prop="paperType" label="类型" width="100">
          <template #default="{row}">{{ row.paperType === 1 ? '作业' : '考试' }}</template>
        </el-table-column>
        <el-table-column prop="deadline" label="截止时间" width="180" />
        <el-table-column label="操作" width="120">
          <template #default="{row}">
            <el-button type="primary" size="small" @click="$router.push(`/student/paper/${row.id}`)">去作答</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { studentDashboard, studentPapers, studentNotices, studentTasks, completeStudentTask } from '@/api'

const cards = reactive([
  { label: '待完成作业/考试', value: 0 },
  { label: '已作答次数', value: 0 },
  { label: '错题总数', value: 0 },
  { label: '在学学科', value: 5 }
])
const notices = ref([])
const pendingPapers = ref([])
const tasks = ref([])

onMounted(async () => {
  try {
    const n = await studentNotices()
    notices.value = n.data || []
  } catch (e) {
    console.error('加载公告失败:', e)
  }
  try {
    const res = await studentDashboard()
    cards[1].value = res.data.answerCount
    cards[2].value = res.data.errorCount
  } catch {}
  try {
    const t = await studentTasks()
    tasks.value = t.data || []
  } catch {}
  const p = await studentPapers()
  pendingPapers.value = p.data || []
  cards[0].value = pendingPapers.value.length
})

const completeTask = async (recordId) => {
  try {
    await ElMessageBox.confirm('确定标记该任务为已完成?', '提示', { type: 'warning' })
  } catch { return }
  await completeStudentTask(recordId)
  ElMessage.success('任务已完成')
  const t = await studentTasks()
  tasks.value = t.data || []
}
</script>
