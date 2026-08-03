<template>
  <div class="practice-config-container" v-loading="pageLoading">
    <!-- 顶部打卡状态区 -->
    <div class="status-bar">
      <div class="status-left">
        <el-tag v-if="isCheckedInToday" type="success" size="large">
          ✅ 今日已自律打卡（连续 {{ continuousDays }} 天）
        </el-tag>
        <el-tag v-else type="info" size="large">
          📅 今日尚未打卡
        </el-tag>
      </div>
      <div class="status-right">
        <span class="points-text">积分：{{ totalPoints }}</span>
        <div class="badges">
          <span v-if="badges.length === 0" class="no-badge">暂无勋章，坚持打卡获取</span>
          <span v-for="(badge, idx) in badges" :key="idx" class="badge-icon">{{ badge }}</span>
        </div>
      </div>
    </div>

    <!-- 模式选择卡片 -->
    <div class="mode-selector">
      <el-card
        :class="['mode-card', { active: mode === 1 }]"
        shadow="hover"
        @click="mode = 1"
      >
        <div class="mode-icon">📝</div>
        <div class="mode-title">专项板块定向练习</div>
        <div class="mode-desc">选择学科和知识点，AI 针对性出题</div>
      </el-card>
      <el-card
        :class="['mode-card', { active: mode === 2 }]"
        shadow="hover"
        @click="mode = 2"
      >
        <div class="mode-icon">📋</div>
        <div class="mode-title">考纲大数据智能套卷</div>
        <div class="mode-desc">AI 根据考纲智能组卷，模拟真实考试</div>
      </el-card>
    </div>

    <!-- 模式一：专项板块定向练习 -->
    <el-card v-if="mode === 1" class="config-card" shadow="never">
      <template #header>
        <div class="card-header-title">
          <el-icon><EditPen /></el-icon>
          <span>专项板块定向练习 - 参数配置</span>
        </div>
      </template>
      <el-form :model="form1" label-width="100px" label-position="left">
        <el-form-item label="选择学科" required>
          <el-select v-model="form1.subjectId" placeholder="请选择学科" style="width:100%" @change="loadKnowledgePoints">
            <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
          </el-select>
        </el-form-item>

        <el-form-item label="知识板块">
          <div class="kp-container">
            <el-checkbox-group v-model="form1.knowledgePoints">
              <el-checkbox v-for="kp in knowledgePoints" :key="kp" :label="kp" class="kp-checkbox">
                {{ kp }}
              </el-checkbox>
            </el-checkbox-group>
            <el-empty v-if="knowledgePoints.length === 0" description="请先选择学科" :image-size="60" />
          </div>
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="题目数量">
              <el-input-number v-model="form1.questionCount" :min="5" :max="30" :step="5" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="难度等级">
              <el-select v-model="form1.difficulty" style="width:100%">
                <el-option label="基础" :value="1" />
                <el-option label="中档" :value="2" />
                <el-option label="拔高" :value="3" />
                <el-option label="混合难度" :value="0" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="题型">
              <el-select v-model="form1.questionTypes" multiple placeholder="选择题型" style="width:100%">
                <el-option label="单选题" :value="1" />
                <el-option label="多选题" :value="2" />
                <el-option label="判断题" :value="3" />
              </el-select>
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="错题优先推送">
          <el-switch v-model="form1.priorityErrors" active-text="开启" inactive-text="关闭" />
          <span class="form-tip">开启后，AI 将优先从你的错题本中匹配同知识点题目，针对性更强</span>
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" size="large" :loading="generating" :disabled="!canGenerate1" @click="handleGenerate">
            🚀 AI 智能组卷
          </el-button>
        </div>
      </el-form>
    </el-card>

    <!-- 模式二：考纲大数据智能套卷 -->
    <el-card v-if="mode === 2" class="config-card" shadow="never">
      <template #header>
        <div class="card-header-title">
          <el-icon><Document /></el-icon>
          <span>考纲大数据智能套卷 - 参数配置</span>
        </div>
      </template>
      <el-form :model="form2" label-width="120px" label-position="left">
        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="选择学科" required>
              <el-select v-model="form2.subjectId" placeholder="请选择学科" style="width:100%">
                <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="考试类型" required>
              <el-select v-model="form2.examType" placeholder="请选择考试类型" style="width:100%">
                <el-option label="单元测验" value="unit" />
                <el-option label="期中考试" value="midterm" />
                <el-option label="期末考试" value="final" />
                <el-option label="升学统考" value="entrance" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="试卷总分">
              <el-input-number v-model="form2.totalScore" :min="50" :max="150" :step="10" style="width:100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item label="题目总数">
              <el-input-number v-model="form2.questionCount" :min="10" :max="50" :step="5" style="width:100%" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="基础题占比">
              <el-slider v-model="form2.easyRatio" :min="0" :max="100" :step="10" show-input />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="中档题占比">
              <el-slider v-model="form2.mediumRatio" :min="0" :max="100" :step="10" show-input />
            </el-form-item>
          </el-col>
        </el-row>

        <el-alert
          v-if="form2.easyRatio + form2.mediumRatio > 100"
          title="基础题和中档题占比之和不能超过 100%，剩余为拔高题"
          type="warning"
          show-icon
          :closable="false"
          style="margin-bottom: 20px"
        />

        <el-form-item label="题型分布（可选微调）">
          <el-row :gutter="10">
            <el-col :span="6">
              <div class="type-config">
                <span>单选题</span>
                <el-input-number v-model="form2.singleCount" :min="0" :max="30" size="small" style="width:100%" />
              </div>
            </el-col>
            <el-col :span="6">
              <div class="type-config">
                <span>多选题</span>
                <el-input-number v-model="form2.multiCount" :min="0" :max="20" size="small" style="width:100%" />
              </div>
            </el-col>
            <el-col :span="6">
              <div class="type-config">
                <span>判断题</span>
                <el-input-number v-model="form2.judgeCount" :min="0" :max="20" size="small" style="width:100%" />
              </div>
            </el-col>
            <el-col :span="6">
              <div class="type-config">
                <span>填空题</span>
                <el-input-number v-model="form2.fillCount" :min="0" :max="20" size="small" style="width:100%" />
              </div>
            </el-col>
          </el-row>
        </el-form-item>

        <div class="form-actions">
          <el-button type="primary" size="large" :loading="generating" :disabled="!canGenerate2" @click="handleGenerate">
            🚀 AI 智能组卷
          </el-button>
        </div>
      </el-form>
    </el-card>

    <!-- 最近练习记录 -->
    <el-card class="mt-20" shadow="never">
      <template #header><span>最近练习记录</span></template>
      <el-table :data="recentRecords" stripe style="width:100%">
        <el-table-column prop="generateSource" label="来源" width="120">
          <template #default="{ row }">
            <el-tag :type="row.generateSource === '错题生成' ? 'warning' : 'success'" size="small">
              {{ row.generateSource }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalCount" label="题量" width="80" />
        <el-table-column prop="correctCount" label="正确数" width="80" />
        <el-table-column label="正确率" width="120">
          <template #default="{ row }">
            <el-progress :percentage="Number(row.accuracy)" :stroke-width="14" style="width:100px" />
          </template>
        </el-table-column>
        <el-table-column prop="durationSeconds" label="耗时" width="100">
          <template #default="{ row }">
            {{ formatDuration(row.durationSeconds) }}
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="练习时间" />
      </el-table>
      <el-empty v-if="recentRecords.length === 0" description="暂无练习记录" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { EditPen, Document } from '@element-plus/icons-vue'
import { getSubjects, checkInStatus, generatePracticeConfig, getRecentPracticeRecords, getKnowledgePoints } from '@/api'

const router = useRouter()

// 打卡状态
const isCheckedInToday = ref(false)
const continuousDays = ref(0)
const totalPoints = ref(0)
const badges = ref([])
const pageLoading = ref(false)

// 模式选择
const mode = ref(1)

// 学科列表
const subjects = ref([])

// 知识点列表（从题库中提取）
const knowledgePoints = ref([])

// 模式一表单
const form1 = ref({
  subjectId: null,
  knowledgePoints: [],
  questionCount: 10,
  difficulty: 0,
  questionTypes: [1, 3],
  priorityErrors: false
})

// 模式二表单
const form2 = ref({
  subjectId: null,
  examType: 'unit',
  totalScore: 100,
  questionCount: 20,
  easyRatio: 60,
  mediumRatio: 30,
  singleCount: 10,
  multiCount: 3,
  judgeCount: 5,
  fillCount: 2
})

// 生成状态
const generating = ref(false)

// 最近练习记录
const recentRecords = ref([])

// 校验
const canGenerate1 = computed(() => {
  return form1.value.subjectId && form1.value.questionTypes.length > 0
})

const canGenerate2 = computed(() => {
  return form2.value.subjectId && form2.value.examType && form2.value.easyRatio + form2.value.mediumRatio <= 100
})

onMounted(async () => {
  pageLoading.value = true
  try {
    await Promise.all([
      loadCheckInStatus(),
      loadSubjects(),
      loadRecentRecords()
    ])
    // 检查是否从学情中心跳转过来（携带薄弱知识点）
    const weakPointsData = sessionStorage.getItem('weakPointsForPractice')
    if (weakPointsData) {
      try {
        const data = JSON.parse(weakPointsData)
        if (data.subjectId) {
          form1.value.subjectId = data.subjectId
          mode.value = 1 // 切换到专项练习模式
          await loadKnowledgePoints()
          // 自动勾选薄弱知识点
          if (data.knowledgePoints && Array.isArray(data.knowledgePoints)) {
            form1.value.knowledgePoints = data.knowledgePoints.filter(kp =>
              knowledgePoints.value.includes(kp)
            )
          }
          ElMessage.success(`已自动填入 ${form1.value.knowledgePoints.length} 个薄弱知识点，可直接生成练习`)
        }
      } catch (e) {
        console.warn('解析薄弱知识点数据失败', e)
      } finally {
        sessionStorage.removeItem('weakPointsForPractice')
      }
    }
  } catch (error) {
    ElMessage.error('页面加载失败')
  } finally {
    pageLoading.value = false
  }
})

async function loadCheckInStatus() {
  try {
    const res = await checkInStatus()
    if (res.code === 200) {
      isCheckedInToday.value = res.data.isCheckedInToday
      continuousDays.value = res.data.continuousDays
      totalPoints.value = res.data.totalPoints
      badges.value = res.data.badges || []
    }
  } catch {}
}

async function loadSubjects() {
  try {
    const res = await getSubjects()
    subjects.value = res.data || []
  } catch {}
}

async function loadKnowledgePoints() {
  if (!form1.value.subjectId) {
    knowledgePoints.value = []
    return
  }
  try {
    const res = await getKnowledgePoints(form1.value.subjectId)
    if (res.code === 200 && res.data) {
      knowledgePoints.value = res.data
    } else {
      knowledgePoints.value = []
    }
  } catch {
    knowledgePoints.value = []
  }
}

async function loadRecentRecords() {
  try {
    const res = await getRecentPracticeRecords()
    if (res.code === 200) {
      recentRecords.value = res.data || []
    }
  } catch {}
}

async function handleGenerate() {
  generating.value = true
  try {
    const payload = mode.value === 1
      ? { mode: 1, ...form1.value }
      : { mode: 2, ...form2.value }

    const res = await generatePracticeConfig(payload)
    if (res.code === 200 && res.data && res.data.questions) {
      // 将题目数据存入 sessionStorage 传递给答题页
      sessionStorage.setItem('practiceQuestions', JSON.stringify(res.data.questions))
      sessionStorage.setItem('practiceConfig', JSON.stringify(payload))
      ElMessage.success(`AI 组卷完成，共 ${res.data.questions.length} 道题`)
      router.push('/student/practicePaper')
    } else {
      ElMessage.error(res.message || '组卷失败，请重试')
    }
  } catch (error) {
    ElMessage.error('组卷失败，请重试')
  } finally {
    generating.value = false
  }
}

function formatDuration(seconds) {
  if (!seconds) return '0秒'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}
</script>

<style scoped>
.practice-config-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 顶部状态栏 */
.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 16px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.status-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.points-text {
  font-size: 16px;
  font-weight: 600;
  color: #409EFF;
}

.badges {
  display: flex;
  align-items: center;
  gap: 8px;
}

.badge-icon {
  font-size: 24px;
}

.no-badge {
  font-size: 14px;
  color: #909399;
}

/* 模式选择 */
.mode-selector {
  display: flex;
  gap: 20px;
  margin-bottom: 24px;
}

.mode-card {
  flex: 1;
  cursor: pointer;
  border: 2px solid transparent;
  transition: all 0.3s;
  text-align: center;
  padding: 10px 0;
}

.mode-card:hover {
  border-color: #409EFF;
  transform: translateY(-2px);
}

.mode-card.active {
  border-color: #409EFF;
  background: #ecf5ff;
}

.mode-icon {
  font-size: 40px;
  margin-bottom: 8px;
}

.mode-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 6px;
}

.mode-desc {
  font-size: 13px;
  color: #909399;
}

/* 配置卡片 */
.config-card {
  margin-bottom: 20px;
}

.card-header-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
}

.kp-container {
  max-height: 200px;
  overflow-y: auto;
  border: 1px solid #ebeef5;
  border-radius: 4px;
  padding: 12px;
}

.kp-checkbox {
  margin-right: 20px;
  margin-bottom: 8px;
}

.form-actions {
  text-align: center;
  margin-top: 20px;
  padding-top: 20px;
  border-top: 1px solid #ebeef5;
}

.type-config {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 10px;
}

.mt-20 {
  margin-top: 20px;
}
</style>
