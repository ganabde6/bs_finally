<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">数据大屏</h1>
        <p class="page-subtitle">平台核心数据总览与风控预警</p>
      </div>
    </div>
    <!-- 核心指标卡 -->
    <el-row :gutter="20">
      <el-col :span="6"><el-card class="stat-card"><div class="num">{{ stats.studentCount || 0 }}</div><div class="label">学生总数</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#67C23A">{{ stats.teacherCount || 0 }}</div><div class="label">教师总数</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#E6A23C">{{ stats.classCount || 0 }}</div><div class="label">班级总数</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#909399">{{ stats.subjectCount || 0 }}</div><div class="label">学科总数</div></el-card></el-col>
    </el-row>
    <el-row :gutter="20" class="mt-20">
      <el-col :span="6"><el-card class="stat-card"><div class="num">{{ stats.paperCount || 0 }}</div><div class="label">试卷/作业总数</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#67C23A">{{ stats.correctRate || 0 }}%</div><div class="label">批改完成率</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#E6A23C">{{ stats.errorCount || 0 }}</div><div class="label">错题总数</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#F56C6C">{{ stats.highRiskCount || 0 }}</div><div class="label">高危风控预警</div></el-card></el-col>
    </el-row>

    <!-- 图表区 -->
    <el-row :gutter="20" class="mt-20">
      <el-col :span="12">
        <el-card>
          <template #header><span>学科试卷分布</span></template>
          <div ref="paperChart" style="height:320px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>班级平均分排名</span></template>
          <div ref="rankChart" style="height:320px"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-row :gutter="20" class="mt-20">
      <el-col :span="12">
        <el-card>
          <template #header><span>风控类型分布</span></template>
          <div ref="riskChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>最近操作日志</span></template>
          <el-table :data="logs" size="small" height="300">
            <el-table-column prop="username" label="用户" width="100" />
            <el-table-column prop="module" label="模块" width="100" />
            <el-table-column prop="operation" label="操作" show-overflow-tooltip />
            <el-table-column prop="createTime" label="时间" width="160" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { adminDashboard, paperDistribution, classRanking, riskDistribution, recentLogs } from '@/api'

const stats = ref({})
const logs = ref([])
const paperChart = ref()
const rankChart = ref()
const riskChart = ref()

onMounted(async () => {
  try {
    const [s, p, r, rk, lg] = await Promise.all([
      adminDashboard(), paperDistribution(), classRanking(), riskDistribution(), recentLogs()
    ])
    stats.value = s.data
    logs.value = lg.data
    await nextTick()
    drawPaper(p.data)
    drawRank(r.data)
    drawRisk(rk.data)
  } catch {}
})

const drawPaper = (data) => {
  const chart = echarts.init(paperChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: data.map(d => d.subject) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: data.map(d => d.count), itemStyle: { color: '#0D9488' } }]
  })
}

const drawRank = (data) => {
  const chart = echarts.init(rankChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'value' },
    yAxis: { type: 'category', data: data.map(d => d.className).reverse() },
    series: [{ type: 'bar', data: data.map(d => d.avgScore).reverse(), itemStyle: { color: '#67C23A' } }]
  })
}

const drawRisk = (data) => {
  const chart = echarts.init(riskChart.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: data.map(d => ({ name: d.type, value: d.count }))
    }]
  })
}
</script>
