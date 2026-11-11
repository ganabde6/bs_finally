<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">教师首页</h1>
        <p class="page-subtitle">{{ greeting }},{{ userStore.userInfo.realName || '老师' }}!{{ pendingCorrect > 0 ? `今天有 ${pendingCorrect} 项待批改任务` : '所有作业均已批改完毕' }}</p>
      </div>
    </div>

    <!-- 核心数据卡片 -->
    <el-row :gutter="20">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover" @click="$router.push('/teacher/studentManage')">
          <div class="num" style="color:#E6A23C">{{ stats.studentCount || 0 }}</div>
          <div class="label">所教学生</div>
          <div class="hint">{{ stats.studentCount ? `${classCount} 个班级` : '点击去添加学生' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover" @click="$router.push('/teacher/paper')">
          <div class="num">{{ stats.paperCount || 0 }}</div>
          <div class="label">我的试卷/作业</div>
          <div class="hint">已发布 {{ stats.publishedCount || 0 }} 份</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover" @click="$router.push('/teacher/lsHomework')">
          <div class="num" style="color:#409EFF">{{ lsStats.homeworkCount || 0 }}</div>
          <div class="label">听说作业</div>
          <div class="hint">{{ lsStats.recordCount ? `平均得分 ${lsStats.avgScore}` : '暂无练习记录' }}</div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover" @click="$router.push('/teacher/correct')">
          <div class="num" style="color:#F56C6C">{{ pendingCorrect }}</div>
          <div class="label">待批改数</div>
          <div class="hint">{{ pendingCorrect > 0 ? '点击立即批改' : '太棒了,全部批改完毕!' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 最新公告 -->
    <el-card class="content-card mt-20" v-if="notices.length">
      <template #header><span>最新公告</span></template>
      <el-timeline>
        <el-timeline-item v-for="n in notices.slice(0, 5)" :key="n.id" :timestamp="n.publishTime">
          <h4>{{ n.title }}</h4>
          <p style="color:#606266">{{ n.content }}</p>
        </el-timeline-item>
      </el-timeline>
    </el-card>

    <!-- 中部 2:1: 提交趋势 + 待办事项 -->
    <el-row :gutter="20" class="mt-20">
      <el-col :span="16">
        <el-card class="content-card">
          <template #header><span>作业提交趋势(近7天)</span></template>
          <div ref="trendChart" style="height:300px"></div>
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="content-card todo-card">
          <template #header><span>待办事项</span></template>
          <div v-if="tasks.length" class="todo-group">
            <div class="todo-group-title">管理员任务</div>
            <div v-for="t in tasks" :key="t.id" class="todo-item">
              <el-icon class="todo-task-icon"><Bell /></el-icon>
              <div class="todo-info">
                <div class="todo-name">{{ t.title }}</div>
                <div class="todo-meta">{{ t.content ? (t.content.length > 24 ? t.content.slice(0, 24) + '…' : t.content) : '—' }}{{ t.deadline ? ' · 截止 ' + t.deadline : '' }}</div>
              </div>
              <el-button size="small" @click="completeTask(t.id)">完成</el-button>
            </div>
          </div>
          <div v-if="pendingList.length" class="todo-group">
            <div class="todo-group-title">待批改作业</div>
            <div v-for="item in pendingList" :key="item.paperId" class="todo-item" @click="$router.push(`/teacher/correct?paperId=${item.paperId}`)">
              <el-badge :value="item.pendingCount" type="warning" class="todo-badge" />
              <div class="todo-info">
                <div class="todo-name">{{ item.paperName }}</div>
                <div class="todo-meta">{{ item.className }} · 待批改 {{ item.pendingCount }} 份</div>
              </div>
            </div>
          </div>
          <el-empty v-if="!tasks.length && !pendingList.length" description="太棒了,所有事项均已完成!" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 下部 2:1: 最近作业列表 + 成绩分布 -->
    <el-row :gutter="20" class="mt-20">
      <el-col :span="16">
        <el-card class="content-card">
          <template #header>
            <div class="card-header"><span>最近发布/进行中的作业</span>
              <el-button link type="primary" @click="$router.push('/teacher/paper')">查看全部</el-button>
            </div>
          </template>
          <el-table v-if="recentPapers.length" :data="recentPapers" size="small">
            <el-table-column prop="paperName" label="作业名称" show-overflow-tooltip />
            <el-table-column prop="className" label="班级" width="110" show-overflow-tooltip />
            <el-table-column label="提交人数" width="110">
              <template #default="{row}">
                <span>{{ row.submittedCount }}/{{ row.classStudentCount }}</span>
              </template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{row}">
                <el-tag size="small" :type="['info','primary','success'][row.status]">{{ ['草稿','进行中','已结束'][row.status] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="deadline" label="截止时间" width="160">
              <template #default="{row}">{{ row.deadline || '—' }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-else description="还没有发布过作业,点击右上角「发布作业」开始" :image-size="80" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <el-card class="content-card">
          <template #header><span>成绩分布</span></template>
          <div ref="scoreChart" style="height:240px"></div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 快捷入口宫格 -->
    <el-card class="content-card mt-20">
      <template #header><span>快捷入口</span></template>
      <div class="quick-grid">
        <div v-for="q in quickEntries" :key="q.path" class="quick-item" @click="$router.push(q.path)">
          <el-icon :size="26" :color="q.color"><component :is="q.icon" /></el-icon>
          <div class="quick-title">{{ q.title }}</div>
          <div class="quick-desc">{{ q.desc }}</div>
        </div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, markRaw } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import * as echarts from 'echarts'
import { Edit, Document, EditPen, TrendCharts, Files, User, Headset, Bell } from '@element-plus/icons-vue'
import { teacherDashboard, teacherNotices, teacherTasks, completeTeacherTask } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()
const stats = ref({})
const pendingCorrect = computed(() => stats.value.pendingCorrect || 0)
const pendingList = computed(() => stats.value.pendingList || [])
const recentPapers = computed(() => stats.value.recentPapers || [])
const lsStats = computed(() => stats.value.lsStats || {})
const classCount = computed(() => new Set((stats.value.recentPapers || []).map(p => p.className).filter(c => c && c !== '未分配班级')).size)

const notices = ref([])
const tasks = ref([])

const greeting = computed(() => {
  const h = new Date().getHours()
  if (h < 6) return '夜深了'
  if (h < 12) return '上午好'
  if (h < 14) return '中午好'
  if (h < 18) return '下午好'
  return '晚上好'
})

const quickEntries = [
  { title: '题库管理', desc: '维护题目与知识点', path: '/teacher/question', icon: markRaw(Files), color: '#FF6900' },
  { title: '发布作业/考试', desc: '新建并发布作业', path: '/teacher/paper', icon: markRaw(Document), color: '#409EFF' },
  { title: '批改管理', desc: 'AI 批改与人工复核', path: '/teacher/correct', icon: markRaw(EditPen), color: '#E6A23C' },
  { title: '班级学情', desc: '成绩与知识点分析', path: '/teacher/classAnalysis', icon: markRaw(TrendCharts), color: '#409EFF' },
  { title: '听说作业', desc: '英语听说训练布置', path: '/teacher/lsHomework', icon: markRaw(Headset), color: '#F56C6C' },
  { title: '学员管理', desc: '班级与学生维护', path: '/teacher/studentManage', icon: markRaw(User), color: '#909399' }
]

const trendChart = ref()
const scoreChart = ref()
let trendInstance = null
let scoreInstance = null

const drawTrend = (data) => {
  if (!trendChart.value) return
  trendInstance = echarts.init(trendChart.value)
  trendInstance.setOption({
    tooltip: { trigger: 'axis' },
    legend: { data: ['提交数', '按时提交'], top: 0 },
    grid: { left: 40, right: 20, top: 36, bottom: 28 },
    xAxis: { type: 'category', data: data.map(d => d.date), boundaryGap: false },
    yAxis: { type: 'value', minInterval: 1 },
    series: [
      { name: '提交数', type: 'line', smooth: true, data: data.map(d => d.submitted), itemStyle: { color: '#FF6900' }, areaStyle: { color: 'rgba(255,105,0,0.12)' } },
      { name: '按时提交', type: 'line', smooth: true, data: data.map(d => d.onTime), itemStyle: { color: '#409EFF' } }
    ]
  })
}

const drawScore = (data) => {
  if (!scoreChart.value) return
  const hasData = data.some(d => d.value > 0)
  scoreInstance = echarts.init(scoreChart.value)
  scoreInstance.setOption({
    color: ['#FF6900', '#F7BA1E', '#409EFF', '#9B59F5', '#F56C6C'],
    tooltip: { trigger: 'item' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '68%'],
      data: hasData ? data : [{ name: '暂无成绩', value: 1, itemStyle: { color: '#E4E7ED' }, tooltip: { show: false } }],
      label: { formatter: '{b}: {c}' }
    }]
  })
}

const handleResize = () => {
  trendInstance?.resize()
  scoreInstance?.resize()
}

onMounted(async () => {
  try {
    const res = await teacherDashboard()
    stats.value = res.data || {}
  } catch {}
  try {
    const n = await teacherNotices()
    notices.value = n.data || []
  } catch {}
  try {
    const t = await teacherTasks()
    tasks.value = t.data || []
  } catch {}
  await nextTick()
  drawTrend(stats.value.submitTrend || [])
  drawScore(stats.value.scoreDistribution || [])
  window.addEventListener('resize', handleResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize)
  trendInstance?.dispose()
  scoreInstance?.dispose()
})

const completeTask = async (id) => {
  try {
    await ElMessageBox.confirm('确定标记该任务为已完成?', '提示', { type: 'warning' })
  } catch { return }
  await completeTeacherTask(id)
  ElMessage.success('任务已完成')
  const t = await teacherTasks()
  tasks.value = t.data || []
}
</script>

<style scoped>
.stat-card { cursor: pointer; }
.stat-card .hint {
  margin-top: 8px;
  font-size: 12px;
  color: var(--color-text-secondary, #909399);
}

.todo-card :deep(.el-card__body) { padding: 10px 16px; }
.todo-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 4px;
  border-bottom: 1px solid #f0f2f5;
  cursor: pointer;
}
.todo-item:last-child { border-bottom: none; }
.todo-item:hover .todo-name { color: var(--el-color-primary); }
.todo-badge :deep(.el-badge__content) { position: static; transform: none; }
.todo-info { flex: 1; min-width: 0; }
.todo-name { font-size: 14px; font-weight: 500; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.todo-meta { font-size: 12px; color: #909399; margin-top: 2px; }

.quick-grid {
  display: grid;
  grid-template-columns: repeat(6, 1fr);
  gap: 16px;
}
.quick-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  padding: 18px 8px;
  border-radius: 12px;
  background: linear-gradient(180deg, #FAFBFC 0%, #FAFBFC 100%);
  border: 1px solid #E8ECF1;
  cursor: pointer;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}
.quick-item:hover {
  transform: translateY(-3px);
  box-shadow: 0 8px 18px rgba(255, 105, 0, 0.12);
}
.quick-title { font-size: 14px; font-weight: 600; color: var(--color-foreground, #303133); }
.quick-desc { font-size: 12px; color: #909399; text-align: center; }

.todo-group { margin-bottom: 4px; }
.todo-group-title {
  font-size: 12px;
  color: #909399;
  padding: 0 4px 6px;
}
.todo-task-icon { color: #FF6900; font-size: 16px; flex-shrink: 0; }

@media (max-width: 1200px) {
  .quick-grid { grid-template-columns: repeat(3, 1fr); }
}
</style>
