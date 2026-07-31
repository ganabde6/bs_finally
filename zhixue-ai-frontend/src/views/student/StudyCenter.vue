<template>
  <div v-loading="loading">
    <el-card v-if="analysis">
      <template #header><span>个人学情中心</span></template>
      <el-row :gutter="20">
        <el-col :span="6">
          <el-card class="stat-card"><div class="num">{{ analysis.avgScore }}</div><div class="label">平均分</div></el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card"><div class="num" style="color:#F56C6C">{{ weakPoints.length }}</div><div class="label">薄弱知识点</div></el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card"><div class="num" style="color:#67C23A">{{ strongPoints.length }}</div><div class="label">优势模块</div></el-card>
        </el-col>
        <el-col :span="6">
          <el-card class="stat-card"><div class="num" style="color:#E6A23C">{{ trend.length }}</div><div class="label">已完成测评</div></el-card>
        </el-col>
      </el-row>
    </el-card>
    <el-row :gutter="20" class="mt-20" v-if="analysis">
      <el-col :span="12">
        <el-card>
          <template #header><span>成绩趋势</span></template>
          <div ref="trendChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>知识掌握雷达图</span></template>
          <div ref="radarChart" style="height:300px"></div>
        </el-card>
      </el-col>
    </el-row>
    <el-card class="mt-20" v-if="analysis">
      <template #header><span>AI 个性化提升建议</span></template>
      <div style="white-space:pre-wrap;line-height:1.8">{{ analysis.suggestion }}</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { studyAnalysis } from '@/api'

const analysis = ref(null)
const loading = ref(false)
const trendChart = ref()
const radarChart = ref()
const trend = ref([])
const weakPoints = ref([])
const strongPoints = ref([])

onMounted(async () => {
  loading.value = true
  try {
    const res = await studyAnalysis()
    analysis.value = res.data
    if (analysis.value) {
      trend.value = JSON.parse(analysis.value.trend || '[]')
      weakPoints.value = JSON.parse(analysis.value.weakPoints || '[]')
      strongPoints.value = JSON.parse(analysis.value.strongPoints || '[]')
      await nextTick()
      drawTrend()
      drawRadar()
    }
  } finally {
    loading.value = false
  }
})

const drawTrend = () => {
  const chart = echarts.init(trendChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.value.map(t => t.paper) },
    yAxis: { type: 'value' },
    series: [{ name: '成绩', type: 'line', data: trend.value.map(t => t.score), smooth: true, areaStyle: {} }]
  })
}

const drawRadar = () => {
  const allPoints = [...weakPoints.value, ...strongPoints.value]
  const chart = echarts.init(radarChart.value)
  chart.setOption({
    tooltip: {},
    radar: {
      indicator: allPoints.map(p => ({ name: p, max: 100 }))
    },
    series: [{
      type: 'radar',
      data: [{
        value: allPoints.map(p => strongPoints.value.includes(p) ? 90 : 40),
        name: '掌握度'
      }]
    }]
  })
}
</script>
