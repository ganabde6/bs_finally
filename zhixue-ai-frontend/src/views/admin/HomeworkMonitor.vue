<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">作业监测中心</h1>
        <p class="page-subtitle">全校作业布置与完成情况总览,支撑家校一体化作业管理</p>
      </div>
    </div>

    <el-alert
      title="仅统计已发布的作业。提交完成率 = 已提交学生数 / 布置班级学生总数;批改完成率 = 已批改数 / 已提交数。可结合班级排行与作业明细,掌握各班作业进度与教师批改进度。"
      type="info" :closable="false" class="mb-20" />

    <!-- 核心指标卡 -->
    <el-row :gutter="20">
      <el-col :span="6"><el-card class="stat-card"><div class="num">{{ summary.homeworkCount || 0 }}</div><div class="label">作业总数(已发布)</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#67C23A">{{ summary.completionRate || 0 }}%</div><div class="label">提交完成率({{ summary.submittedCount || 0 }}/{{ summary.assignedCount || 0 }})</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#E6A23C">{{ summary.pendingCorrectCount || 0 }}</div><div class="label">待批改(批改完成率 {{ summary.correctRate || 0 }}%)</div></el-card></el-col>
      <el-col :span="6"><el-card class="stat-card"><div class="num" style="color:#F56C6C">{{ summary.pendingCount || 0 }}</div><div class="label">未提交人次(平均 {{ summary.avgScore || 0 }} 分)</div></el-card></el-col>
    </el-row>

    <!-- 班级完成率排行 -->
    <el-row :gutter="20" class="mt-20">
      <el-col :span="12">
        <el-card>
          <template #header><span>班级作业提交完成率排行</span></template>
          <div v-show="ranking.length" ref="rankChart" style="height:300px"></div>
          <el-empty v-if="!ranking.length" description="暂无已发布作业" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header><span>班级明细</span></template>
          <el-table :data="ranking" size="small" height="300">
            <el-table-column type="index" label="#" width="50" />
            <el-table-column prop="className" label="班级" width="120" />
            <el-table-column prop="homeworkCount" label="作业数" width="70" />
            <el-table-column label="提交进度" width="130">
              <template #default="{row}">{{ row.submittedCount }}/{{ row.assignedCount }}</template>
            </el-table-column>
            <el-table-column label="完成率">
              <template #default="{row}">
                <el-progress :percentage="row.completionRate" :stroke-width="8" />
              </template>
            </el-table-column>
            <el-table-column prop="avgScore" label="平均分" width="80" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>

    <!-- 作业任务明细 -->
    <el-card class="content-card mt-20">
      <template #header><span>作业任务明细</span></template>
      <el-form :inline="true" class="mb-20">
        <el-form-item label="班级">
          <el-select v-model="query.classId" placeholder="全部" clearable style="width:140px">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="学科">
          <el-select v-model="query.subjectId" placeholder="全部" clearable style="width:140px">
            <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="名称">
          <el-input v-model="query.keyword" placeholder="作业名称" clearable style="width:180px" @keyup.enter="search" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="search">查询</el-button>
        </el-form-item>
      </el-form>
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="paperName" label="作业名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="subjectName" label="学科" width="90" />
        <el-table-column prop="className" label="班级" width="110" />
        <el-table-column prop="teacherName" label="教师" width="90" />
        <el-table-column prop="publishTime" label="发布时间" width="170" />
        <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column label="提交进度" width="160">
          <template #default="{row}">
            <div>{{ row.submittedCount }}/{{ row.studentCount }}</div>
            <el-progress :percentage="row.completionRate" :stroke-width="6" class="mt-8" />
          </template>
        </el-table-column>
        <el-table-column label="批改率" width="80">
          <template #default="{row}">{{ row.correctRate }}%</template>
        </el-table-column>
        <el-table-column prop="avgScore" label="平均分" width="80" />
        <el-table-column label="状态" width="80">
          <template #default="{row}">
            <el-tag :type="row.status === 2 ? 'info' : 'success'" size="small">{{ row.status === 2 ? '已结束' : '进行中' }}</el-tag>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-if="!loading && list.length === 0" description="暂无已发布作业" :image-size="80" />
      <el-pagination class="mt-20" v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadTasks" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { homeworkSummary, homeworkTasks, homeworkClassRanking, getClasses, getSubjects } from '@/api'

const summary = ref({})
const ranking = ref([])
const list = ref([])
const total = ref(0)
const loading = ref(false)
const classes = ref([])
const subjects = ref([])
const query = reactive({ current: 1, size: 10, classId: null, subjectId: null, keyword: '' })
const rankChart = ref(null)
let chart = null

onMounted(async () => {
  const [c, s] = await Promise.all([getClasses(), getSubjects()])
  classes.value = c.data
  subjects.value = s.data
  await Promise.all([loadSummary(), loadRanking(), loadTasks()])
})

const loadSummary = async () => {
  const res = await homeworkSummary()
  summary.value = res.data
}

const loadTasks = async () => {
  loading.value = true
  try {
    const res = await homeworkTasks(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const loadRanking = async () => {
  const res = await homeworkClassRanking()
  ranking.value = res.data
  await nextTick()
  drawRanking()
}

const drawRanking = () => {
  if (!ranking.value.length) return
  if (!rankChart.value) return
  chart = chart || echarts.init(rankChart.value)
  const data = [...ranking.value].reverse()
  chart.setOption({
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 100, right: 50, top: 20, bottom: 30 },
    xAxis: { type: 'value', max: 100, axisLabel: { formatter: '{value}%' } },
    yAxis: { type: 'category', data: data.map(d => d.className) },
    series: [{
      type: 'bar',
      data: data.map(d => d.completionRate),
      barWidth: 14,
      itemStyle: { color: '#409EFF', borderRadius: [0, 7, 7, 0] },
      label: { show: true, position: 'right', formatter: '{c}%' }
    }]
  })
}

const search = () => {
  query.current = 1
  loadTasks()
}
</script>
