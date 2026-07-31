<template>
  <el-card>
    <template #header><span>班级学情分析</span></template>
    <el-form :inline="true" class="mb-20">
      <el-form-item label="班级">
        <el-select v-model="classId" @change="loadAnalysis"><el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" /></el-select>
      </el-form-item>
      <el-form-item label="学科">
        <el-select v-model="subjectId" clearable @change="loadAnalysis"><el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" /></el-select>
      </el-form-item>
    </el-form>
    <div v-if="analysis">
      <el-row :gutter="20">
        <el-col :span="6"><el-card class="stat-card"><div class="num">{{ analysis.avgScore }}</div><div class="label">班级平均分</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#67C23A">{{ analysis.passRate }}%</div><div class="label">及格率</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#E6A23C">{{ analysis.excellentRate }}%</div><div class="label">优秀率</div></el-card></el-col>
        <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#F56C6C">{{ layering.length }}</div><div class="label">分层组数</div></el-card></el-col>
      </el-row>
      <el-row :gutter="20" class="mt-20">
        <el-col :span="12">
          <el-card><template #header><span>学生分层分布</span></template>
            <div ref="pieChart" style="height:300px"></div>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card><template #header><span>共性薄弱知识点</span></template>
            <div ref="barChart" style="height:300px"></div>
          </el-card>
        </el-col>
      </el-row>
      <el-card class="mt-20">
        <template #header><span>教学优化建议</span></template>
        <div style="white-space:pre-wrap;line-height:1.8">{{ analysis.teachingAdvice }}</div>
      </el-card>
    </div>
    <el-empty v-else description="请选择班级与学科" />
  </el-card>
</template>

<script setup>
import { ref, onMounted, nextTick, watch } from 'vue'
import * as echarts from 'echarts'
import { getClasses, getSubjects, classAnalysis } from '@/api'

const classes = ref([])
const subjects = ref([])
const classId = ref(null)
const subjectId = ref(null)
const analysis = ref(null)
const layering = ref([])
const pieChart = ref()
const barChart = ref()

onMounted(async () => {
  const [c, s] = await Promise.all([getClasses(), getSubjects()])
  classes.value = c.data
  subjects.value = s.data
  if (classes.value.length) {
    classId.value = classes.value[0].id
    loadAnalysis()
  }
})

const loadAnalysis = async () => {
  if (!classId.value) return
  try {
    const res = await classAnalysis(classId.value, subjectId.value)
    analysis.value = res.data
    if (analysis.value) {
      layering.value = JSON.parse(analysis.value.layering || '[]')
      await nextTick()
      drawPie()
      drawBar()
    }
  } catch {}
}

const drawPie = () => {
  const chart = echarts.init(pieChart.value)
  chart.setOption({
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%','70%'],
      data: layering.value.map(l => ({ name: l.layer, value: l.count }))
    }]
  })
}

const drawBar = () => {
  const errors = JSON.parse(analysis.value.commonErrors || '[]')
  const chart = echarts.init(barChart.value)
  chart.setOption({
    tooltip: {},
    xAxis: { type: 'category', data: errors, axisLabel: { rotate: 30 } },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', data: errors.map(() => Math.floor(Math.random() * 10) + 1) }]
  })
}
</script>
