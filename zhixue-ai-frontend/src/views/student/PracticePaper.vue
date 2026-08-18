<template>
  <div class="practice-paper-container" v-loading="pageLoading">
    <!-- 顶部信息栏 -->
    <div class="paper-header">
      <div class="header-left">
        <el-button text @click="goBack">
          ← 返回组卷配置
        </el-button>
        <el-tag type="info" size="small" style="margin-left: 10px">
          {{ configInfo }}
        </el-tag>
      </div>
      <div class="header-right">
        <el-tag type="warning" size="large">
          <el-icon style="vertical-align:-2px"><Timer /></el-icon> 用时：{{ formatDuration(elapsedSeconds) }}
        </el-tag>
      </div>
    </div>

    <!-- 题目列表 -->
    <div class="questions-list" v-if="katexReady">
      <el-card v-for="(item, index) in questions" :key="item.id" class="question-card" shadow="hover">
        <template #header>
          <div class="card-header">
            <span class="question-title">第 {{ index + 1 }} 题（{{ typeName(item.type) }}）</span>
            <el-tag size="small" :type="difficultyTag(item.difficulty)">
              {{ difficultyName(item.difficulty) }}
            </el-tag>
          </div>
        </template>

        <div class="question-content">
          <p class="stem" v-html="renderLatex(item.stem)"></p>

          <!-- 单选题 -->
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

          <!-- 多选题 -->
          <div v-else-if="item.type === 2" class="options-group">
            <el-checkbox-group v-model="answers[item.id]" :disabled="submitted">
              <el-checkbox
                v-for="(option, optIndex) in item.options"
                :key="optIndex"
                :value="option"
                class="option-item"
              >
                {{ String.fromCharCode(65 + optIndex) }}. <span v-html="renderLatex(option)"></span>
              </el-checkbox>
            </el-checkbox-group>
          </div>

          <!-- 判断题 -->
          <div v-else-if="item.type === 3" class="judge-group">
            <el-switch
              v-model="answers[item.id]"
              active-value="正确"
              inactive-value="错误"
              :disabled="submitted"
              active-text="正确"
              inactive-text="错误"
              inline-prompt
            />
            <span class="judge-hint">当前选择：{{ answers[item.id] || '未选择' }}</span>
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

          <!-- 简答题 / 计算题 -->
          <div v-else-if="item.type === 5 || item.type === 7" class="answer-group">
            <el-input
              v-model="answers[item.id]"
              type="textarea"
              :rows="4"
              :disabled="submitted"
              placeholder="请写出完整解答过程"
            />
          </div>

          <!-- 结果展示 -->
          <div v-if="submitted && results[item.id] !== undefined" class="result-row">
            <div v-if="results[item.id]" class="result-correct">
              <el-icon style="vertical-align:-2px"><CircleCheck /></el-icon> 正确！标准答案：<span v-html="renderLatex(item.correctAnswer)"></span>
            </div>
            <div v-else class="result-wrong">
              ❌ 错误！标准答案：<span v-html="renderLatex(item.correctAnswer)"></span>，你的答案：{{ formatAnswer(answers[item.id]) || '未作答' }}
            </div>
          </div>

          <!-- 题目解析 -->
          <div v-if="submitted && item.analysis" class="analysis-row">
            <strong>📖 解析：</strong><span v-html="renderLatex(item.analysis)"></span>
          </div>
        </div>
      </el-card>
    </div>

    <!-- 空状态 -->
    <el-empty v-if="questions.length === 0" description="暂无题目" />

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
      <span class="answered-hint">已答 {{ answeredCount }} / {{ questions.length }} 题</span>
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

      <div class="stats-row">
        <el-tag type="success">正确 {{ correctCount }} 题</el-tag>
        <el-tag type="danger">错误 {{ questions.length - correctCount }} 题</el-tag>
        <el-tag type="info">耗时 {{ formatDuration(elapsedSeconds) }}</el-tag>
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

      <!-- 继续练习 -->
      <div class="continue-area">
        <el-button @click="goBack">继续组卷练习</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { submitPractice, checkInStatus, doCheckIn } from '@/api'

const router = useRouter()

// 题目数据
const questions = ref([])
const answers = ref({})
const submitted = ref(false)
const results = ref({})
const accuracy = ref(0)
const correctCount = ref(0)
const isCheckedInToday = ref(false)
const continuousDays = ref(0)
const totalPoints = ref(0)
const badges = ref([])
const loading = ref(false)
const pageLoading = ref(false)
const katexReady = ref(typeof katex !== 'undefined')

// 计时器
const elapsedSeconds = ref(0)
let timer = null

// 配置信息
const configInfo = computed(() => {
  try {
    const config = JSON.parse(sessionStorage.getItem('practiceConfig') || '{}')
    if (config.mode === 1) {
      return `专项练习 · ${config.questionCount || questions.value.length}题`
    } else if (config.mode === 2) {
      const typeMap = { unit: '单元测验', midterm: '期中考试', final: '期末考试', entrance: '升学统考' }
      return `${typeMap[config.examType] || '智能套卷'} · ${config.questionCount || questions.value.length}题`
    }
  } catch {}
  return '自主智练'
})

const accuracyColor = computed(() => {
  if (accuracy.value >= 60) return '#67C23A'
  if (accuracy.value >= 30) return '#E6A23C'
  return '#F56C6C'
})

const answeredCount = computed(() => {
  return questions.value.filter(q => {
    const a = answers.value[q.id]
    if (q.type === 2) return Array.isArray(a) && a.length > 0
    return a !== null && a !== undefined && a !== ''
  }).length
})

const encouragementMessages = [
  '太棒了，积分 +10，继续加油！',
  '自律的你真帅，明天也要坚持哦！',
  '恭喜完成今日打卡，离学霸又近了一步！',
  '今日份努力已记录，继续保持！'
]

onMounted(async () => {
  pageLoading.value = true
  // 从 sessionStorage 读取题目
  try {
    const stored = sessionStorage.getItem('practiceQuestions')
    if (stored) {
      questions.value = JSON.parse(stored)
      // 解析选项 + LaTeX 渲染（题干不再整体包裹 $，交由 renderLatex 分段处理，避免中文进数学模式）
      questions.value.forEach(q => {
        // 选项
        if (q.options && Array.isArray(q.options)) {
          q.options = q.options.map(opt => {
            let text = opt
            if (typeof opt === 'string') {
              try {
                const parsed = JSON.parse(opt)
                text = parsed.value || opt
              } catch {}
            } else if (opt && typeof opt === 'object') {
              text = opt.value || opt
            }
            // 自动包裹 LaTeX
            if (typeof text === 'string' && /\\[a-zA-Z{(]/.test(text) && !/\$/.test(text)) {
              text = '$' + text + '$'
            }
            return text
          })
        }
      })
      // 初始化答案
      questions.value.forEach(q => {
        if (q.type === 2) {
          answers.value[q.id] = []
        } else if (q.type === 3) {
          answers.value[q.id] = '正确'
        } else {
          answers.value[q.id] = null
        }
      })
    }
  } catch {}

  // 加载打卡状态
  try {
    const res = await checkInStatus()
    if (res.code === 200) {
      isCheckedInToday.value = res.data.isCheckedInToday
      continuousDays.value = res.data.continuousDays
      totalPoints.value = res.data.totalPoints
      badges.value = res.data.badges || []
    }
  } catch {}

  pageLoading.value = false

  // 等待 KaTeX 加载完成后渲染
  const waitForKatex = () => {
    if (typeof katex !== 'undefined') {
      katexReady.value = true
    } else {
      setTimeout(waitForKatex, 200)
    }
  }
  waitForKatex()

  // 启动计时器
  timer = setInterval(() => {
    elapsedSeconds.value++
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

// 渲染 LaTeX 数学公式（用 v-html 方式）
function renderLatex(text) {
  if (!text) return ''
  // 如果文本中没有 LaTeX 命令，直接返回
  if (!/\\[a-zA-Z{(]/.test(text)) return text

  // 处理 $...$ / $$...$$ 包裹的数学公式：数学段用 KaTeX 渲染，文本段原样保留
  if (/\$/.test(text)) {
    const parts = text.split(/(\$\$[^$]*\$\$|\$[^$]*\$)/g)
    let result = ''
    for (const part of parts) {
      const dm = part.match(/^\$\$([\s\S]*)\$\$$/)
      const im = !dm && part.match(/^\$([\s\S]*)\$$/)
      if (dm || im) {
        const math = dm ? dm[1] : im[1]
        if (typeof katex !== 'undefined') {
          try {
            result += katex.renderToString(math, { throwOnError: false, displayMode: !!dm })
            continue
          } catch {}
        }
        result += part
        continue
      }
      // 文本段（如含 LaTeX 命令，继续走分段渲染，保证旧题兼容；若仍有不成对 $ 则原样保留防递归）
      result += part.includes('$') ? part : renderLatex(part)
    }
    return result
  }

  // 策略：只按中文字符和中文标点分割，保留英文空格在片段内
  const segments = []
  let current = ''
  for (let i = 0; i < text.length; i++) {
    const ch = text[i]
    const code = ch.charCodeAt(0)
    // 判断是否是中文或中文标点
    const isChinese = (code >= 0x4e00 && code <= 0x9fff) || 
                      ch === '，' || ch === '。' || ch === '；' || ch === '：' ||
                      ch === '！' || ch === '？' || ch === '、' || ch === '…' ||
                      ch === '（' || ch === '）' || ch === '【' || ch === '】' ||
                      ch === '「' || ch === '」' || ch === '『' || ch === '』'
    
    if (isChinese) {
      if (current) {
        segments.push(current)
        current = ''
      }
      segments.push(ch)
    } else {
      current += ch
    }
  }
  if (current) {
    segments.push(current)
  }

  // 对每个片段，如果包含 LaTeX 命令则用 KaTeX 渲染
  let result = ''
  for (const seg of segments) {
    if (/\\[a-zA-Z{(]/.test(seg)) {
      // 包含 LaTeX，尝试用 KaTeX 渲染
      if (typeof katex !== 'undefined') {
        try {
          result += katex.renderToString(seg, { throwOnError: false, displayMode: false })
        } catch {
          result += '$' + seg + '$'
        }
      } else {
        // KaTeX 未加载，用 $ 包裹显示
        result += '$' + seg + '$'
      }
    } else {
      result += seg
    }
  }

  return result
}

// 渲染 LaTeX 数学公式（auto-render 方式，用于提交后的解析区域）
function renderMath() {
  if (typeof renderMathInElement !== 'undefined') {
    renderMathInElement(document.querySelector('.questions-list'), {
      delimiters: [
        { left: '$$', right: '$$', display: true },
        { left: '$', right: '$', display: false },
        { left: '\\(', right: '\\)', display: false },
        { left: '\\[', right: '\\]', display: true }
      ],
      throwOnError: false
    })
  }
}

function typeName(type) {
  const map = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题', 5: '简答题', 6: '作文题', 7: '计算题' }
  return map[type] || '未知题型'
}

function difficultyName(d) {
  const map = { 1: '基础', 2: '中档', 3: '拔高', 4: '拔高', 5: '拔高' }
  return map[d] || '未知'
}

function difficultyTag(d) {
  const map = { 1: 'success', 2: 'warning', 3: 'danger', 4: 'danger', 5: 'danger' }
  return map[d] || 'info'
}

function formatAnswer(a) {
  if (Array.isArray(a)) return a.join(', ')
  return a
}

function formatDuration(seconds) {
  if (!seconds) return '0秒'
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  if (m > 0) return `${m}分${s}秒`
  return `${s}秒`
}

function goBack() {
  router.push('/student/selfPractice')
}

async function handleSubmit() {
  if (answeredCount.value < questions.value.length) {
    ElMessage.warning(`还有 ${questions.value.length - answeredCount.value} 道题未作答，请完成后再提交`)
    return
  }

  loading.value = true
  try {
    const questionAnswers = questions.value.map(q => {
      let userAnswer = answers.value[q.id]
      if (Array.isArray(userAnswer)) {
        userAnswer = userAnswer.join(',')
      }
      return {
        questionId: q.id,
        userAnswer: userAnswer || ''
      }
    })

    const res = await submitPractice({
      questionAnswers,
      durationSeconds: elapsedSeconds.value
    })

    if (res.code === 200) {
      let cc = 0
      res.data.forEach(item => {
        results.value[item.questionId] = item.correct
        if (item.correct) cc++
      })

      correctCount.value = cc
      accuracy.value = Math.round((cc / questions.value.length) * 100)
      submitted.value = true

      // 停止计时
      if (timer) clearInterval(timer)

      ElMessage.success('提交成功！')
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch (error) {
    ElMessage.error('提交失败，请重试')
  } finally {
    loading.value = false
  }
}

async function handleCheckIn() {
  if (loading.value) return

  loading.value = true
  try {
    const res = await doCheckIn()

    if (res.code === 200) {
      const randomMsg = encouragementMessages[Math.floor(Math.random() * encouragementMessages.length)]
      ElMessage.success(randomMsg)

      isCheckedInToday.value = true
      continuousDays.value = res.data.continuousDays
      totalPoints.value = res.data.totalPoints

      await loadCheckInStatus()
    } else {
      ElMessage.error(res.message || '打卡失败')
    }
  } catch (error) {
    ElMessage.error('打卡失败，请重试')
  } finally {
    loading.value = false
  }
}

async function loadCheckInStatus() {
  try {
    const res = await checkInStatus()
    if (res.code === 200) {
      badges.value = res.data.badges || []
    }
  } catch {}
}
</script>

<style scoped>
.practice-paper-container {
  padding: 20px;
  max-width: 1000px;
  margin: 0 auto;
}

/* 顶部信息栏 */
.paper-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.header-left {
  display: flex;
  align-items: center;
}

.header-right {
  display: flex;
  align-items: center;
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
  line-height: 1.8;
  margin-bottom: 20px;
  color: #303133;
  font-weight: 500;
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

.fill-group {
  margin-top: 10px;
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

.analysis-row {
  margin-top: 12px;
  padding: 10px;
  background: #f4f4f5;
  border-radius: 4px;
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

/* 操作区域 */
.action-area {
  margin-top: 30px;
  text-align: center;
}

.answered-hint {
  margin-left: 16px;
  color: #909399;
  font-size: 14px;
}

/* 结果汇总 */
.summary-area {
  margin-top: 30px;
  padding: 24px;
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
  margin-bottom: 16px;
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

.stats-row {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-bottom: 16px;
}

.checkin-area {
  margin-top: 20px;
}

.continue-area {
  margin-top: 16px;
}
</style>
