<template>
  <div class="self-practice-container" v-loading="pageLoading">
    <!-- 顶部状态区 -->
    <div class="status-bar">
      <div class="status-left">
        <el-tag v-if="isCheckedInToday" type="success" size="large">
          今日已自律打卡（连续 {{ continuousDays }} 天）
        </el-tag>
        <el-tag v-else type="info" size="large">
          今日尚未打卡
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

    <!-- 题目列表 -->
    <div v-if="questions.length > 0" class="questions-list">
      <el-card v-for="(item, index) in questions" :key="item.id" class="question-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="question-title">第 {{ index + 1 }} 题（{{ item.type === 1 ? '单选题' : '填空题' }}）</span>
          </div>
        </template>

        <div class="question-content">
          <p class="stem" v-html="renderLatex(item.stem)"></p>

          <!-- 选择题 -->
          <div v-if="item.type === 1" class="options-group">
            <el-radio-group v-model="answers[item.id]" :disabled="submitted">
              <el-radio
                v-for="(option, optIndex) in item.options"
                :key="optIndex"
                :value="option"
                class="option-item"
              >
                {{ String.fromCharCode(65 + optIndex) }}. <span v-html="renderLatex(option)"></span>
              </el-radio>
            </el-radio-group>
          </div>

          <!-- 填空题 -->
          <div v-else-if="item.type === 4" class="fill-group">
            <el-input
              v-model="answers[item.id]"
              :disabled="submitted"
              placeholder="请输入答案"
              style="max-width: 400px"
            />
          </div>

          <!-- 结果展示 -->
          <div v-if="submitted && results[item.id] !== undefined" class="result-row">
            <div v-if="results[item.id]" class="result-correct">
              ✅ 正确！标准答案：<span v-html="renderLatex(item.correctAnswer)"></span>
            </div>
            <div v-else class="result-wrong">
              ❌ 错误！标准答案：<span v-html="renderLatex(item.correctAnswer)"></span>，你的答案：{{ answers[item.id] || '未作答' }}
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-empty v-else description="暂无推荐题目，去错题本生成吧" />

    <!-- 提交按钮 -->
    <div v-if="!submitted && questions.length > 0" class="action-area">
      <el-button
        type="primary"
        size="large"
        :loading="loading"
        @click="handleSubmit"
      >
         提交批改
      </el-button>
    </div>

    <!-- 结果汇总 -->
    <div v-if="submitted" class="summary-area">
      <div class="accuracy-display">
        <span class="accuracy-label">本次正确率：</span>
        <el-progress
          :percentage="accuracy"
          :color="accuracyColor"
          :stroke-width="20"
          style="width: 300px; display: inline-block; vertical-align: middle;"
        />
        <span class="accuracy-text">{{ accuracy }}%</span>
      </div>

      <!-- 正确率不足提示 -->
      <el-tag v-if="accuracy < 30" type="warning" style="margin-top: 10px;">
        再练练吧，正确率达30%即可打卡哦～
      </el-tag>

      <!-- 自律打卡按钮 -->
      <div v-if="submitted && accuracy >= 30 && !isCheckedInToday" class="checkin-area">
        <el-button
          type="success"
          size="large"
          :loading="loading"
          @click="handleCheckIn"
        >
          自律打卡
        </el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { generatePractice, submitPractice, checkInStatus, doCheckIn } from '@/api'
import { renderLatex } from '@/utils/latex'

// 响应式数据
const questions = ref([])
const answers = ref({})
const submitted = ref(false)
const results = ref({})
const accuracy = ref(0)
const isCheckedInToday = ref(false)
const continuousDays = ref(0)
const totalPoints = ref(0)
const badges = ref([])
const loading = ref(false)
const pageLoading = ref(false)

// 进度条颜色
const accuracyColor = computed(() => {
  if (accuracy.value >= 60) return '#67C23A'
  if (accuracy.value >= 30) return '#E6A23C'
  return '#F56C6C'
})

// 鼓励语
const encouragementMessages = [
  '太棒了，积分 +10，继续加油！',
  '自律的你真帅，明天也要坚持哦！',
  '恭喜完成今日打卡，离学霸又近了一步！',
  '今日份努力已记录，继续保持！'
]

// 页面加载
onMounted(async () => {
  pageLoading.value = true
  try {
    await Promise.all([
      loadQuestions(),
      loadCheckInStatus()
    ])
  } catch (error) {
    ElMessage.error('页面加载失败')
    console.error(error)
  } finally {
    pageLoading.value = false
  }
})

// 加载题目
async function loadQuestions() {
  try {
    const res = await generatePractice()
    if (res.code === 200) {
      questions.value = res.data
      // 初始化答案对象
      res.data.forEach(q => {
        answers.value[q.id] = null
      })
    }
  } catch (error) {
    ElMessage.error('获取题目失败')
    console.error(error)
  }
}

// 加载打卡状态
async function loadCheckInStatus() {
  try {
    const res = await checkInStatus()
    if (res.code === 200) {
      isCheckedInToday.value = res.data.isCheckedInToday
      continuousDays.value = res.data.continuousDays
      totalPoints.value = res.data.totalPoints
      badges.value = res.data.badges || []
    }
  } catch (error) {
    console.error('获取打卡状态失败', error)
  }
}

// 提交批改
async function handleSubmit() {
  // 检查是否所有题目都已作答
  const unanswered = questions.value.filter(q => !answers.value[q.id])
  if (unanswered.length > 0) {
    ElMessage.warning('请完成所有题目后再提交')
    return
  }

  loading.value = true
  try {
    const questionAnswers = questions.value.map(q => ({
      questionId: q.id,
      userAnswer: answers.value[q.id]
    }))

    const res = await submitPractice({
      questionAnswers,
      durationSeconds: 0
    })

    if (res.code === 200) {
      // 处理批改结果
      let correctCount = 0
      res.data.forEach(item => {
        results.value[item.questionId] = item.correct
        if (item.correct) correctCount++
      })

      accuracy.value = Math.round((correctCount / questions.value.length) * 100)
      submitted.value = true

      ElMessage.success('提交成功！')
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (error) {
    ElMessage.error('提交失败，请重试')
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 自律打卡
async function handleCheckIn() {
  if (loading.value) return

  loading.value = true
  try {
    const res = await doCheckIn()

    if (res.code === 200) {
      // 随机鼓励语
      const randomMsg = encouragementMessages[Math.floor(Math.random() * encouragementMessages.length)]
      ElMessage.success(randomMsg)

      // 刷新打卡状态
      isCheckedInToday.value = true
      continuousDays.value = res.data.continuousDays
      totalPoints.value = res.data.totalPoints

      // 重新请求勋章列表
      await loadCheckInStatus()
    } else {
      ElMessage.error(res.message || '打卡失败')
    }
  } catch (error) {
    ElMessage.error('打卡失败，请重试')
    console.error(error)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.self-practice-container {
  padding: 20px;
  max-width: 1200px;
  margin: 0 auto;
}

/* 顶部状态栏 */
.status-bar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 30px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
}

.status-left {
  flex: 1;
}

.status-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

.points-text {
  font-size: 16px;
  font-weight: 600;
  color: #0D9488;
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

/* 题目列表 */
.questions-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.question-card {
  border-radius: 8px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.question-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.question-content {
  padding: 10px 0;
}

.stem {
  font-size: 15px;
  line-height: 1.6;
  margin-bottom: 20px;
  color: #303133;
}

.options-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  margin-right: 0;
  margin-bottom: 8px;
}

.judge-group {
  display: flex;
  align-items: center;
  gap: 15px;
}

.judge-hint {
  font-size: 14px;
  color: #606266;
}

/* 结果展示 */
.result-row {
  margin-top: 20px;
  padding: 12px;
  border-radius: 4px;
  font-size: 14px;
}

.result-correct {
  color: #67C23A;
  background: #f0f9eb;
  padding: 10px;
  border-radius: 4px;
}

.result-wrong {
  color: #F56C6C;
  background: #fef0f0;
  padding: 10px;
  border-radius: 4px;
}

/* 操作区域 */
.action-area {
  margin-top: 30px;
  text-align: center;
}

/* 结果汇总 */
.summary-area {
  margin-top: 30px;
  padding: 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.1);
  text-align: center;
}

.accuracy-display {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 15px;
  margin-bottom: 20px;
}

.accuracy-label {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.accuracy-text {
  font-size: 24px;
  font-weight: 700;
  color: #0D9488;
}

.checkin-area {
  margin-top: 20px;
}
</style>
