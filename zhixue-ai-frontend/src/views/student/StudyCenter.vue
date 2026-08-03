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
      <template #header>
        <div style="display:flex;justify-content:space-between;align-items:center">
          <span>AI 个性化提升建议</span>
          <el-button 
            v-if="weakPoints.length > 0" 
            type="primary" 
            size="small"
            @click="goToPractice"
          >
            🎯 针对薄弱点智能出题
          </el-button>
        </div>
      </template>
      <div style="white-space:pre-wrap;line-height:1.8">{{ analysis.suggestion }}</div>
      
      <el-divider v-if="weakPoints.length > 0" />
      
      <div v-if="weakPoints.length > 0" style="margin-top:16px">
        <div style="margin-bottom:12px;color:#606266;font-size:14px">
          <strong> 你的薄弱知识点：</strong>
        </div>
        <el-tag 
          v-for="wp in weakPoints" 
          :key="wp" 
          type="danger" 
          style="margin:4px"
          closable
          @close="removeWeakPoint(wp)"
        >
          {{ wp }}
        </el-tag>
        <div style="margin-top:16px;color:#909399;font-size:13px">
          💡 点击「针对薄弱点智能出题」，AI 将根据你的薄弱知识点生成专项练习题，底层知识不变但题型和逻辑会变化，帮助你真正掌握这些知识点。
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { studyAnalysis } from '@/api'

const router = useRouter()
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
  if (!trendChart.value) return
  const chart = echarts.init(trendChart.value)
  chart.setOption({
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: trend.value.map(t => t.paper) },
    yAxis: { type: 'value' },
    series: [{ name: '成绩', type: 'line', data: trend.value.map(t => t.score), smooth: true, areaStyle: {} }]
  })
}

const drawRadar = () => {
  if (!radarChart.value) return
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

function removeWeakPoint(wp) {
  weakPoints.value = weakPoints.value.filter(p => p !== wp)
}

function goToPractice() {
  if (weakPoints.value.length === 0) {
    ElMessage.warning('没有可训练的薄弱知识点')
    return
  }
  // 将薄弱知识点和学科ID存入 sessionStorage，供 PracticeConfig 读取
  sessionStorage.setItem('weakPointsForPractice', JSON.stringify({
    knowledgePoints: weakPoints.value,
    subjectId: analysis.value.subjectId
  }))
  router.push('/student/selfPractice')
}
</script>

<style scoped>
.mt-20 { margin-top: 20px; }
.stat-card { text-align: center; }
.stat-card .num { font-size: 28px; font-weight: 700; color: #409EFF; }
.stat-card .label { font-size: 13px; color: #909399; margin-top: 4px; }
</style>
