<template>
  <div class="pk-container" v-loading="pageLoading">
    <!-- 顶部导航 -->
    <div class="pk-header">
      <el-button text @click="goBack">← 返回自主智练</el-button>
      <h2>🏆 同学PK竞技场</h2>
    </div>

    <!-- 阶段一：创建/加入房间 -->
    <div v-if="phase === 'lobby'" class="lobby-area">
      <el-row :gutter="30">
        <el-col :span="12">
          <el-card shadow="hover" class="pk-card">
            <template #header>
              <div class="card-title">🎮 创建PK房间</div>
            </template>
            <el-form :model="createForm" label-width="100px">
              <el-form-item label="选择学科" required>
                <el-select v-model="createForm.subjectId" placeholder="请选择学科" style="width:100%">
                  <el-option v-for="s in subjects" :key="s.id" :label="s.name" :value="s.id" />
                </el-select>
              </el-form-item>
              <el-form-item label="题目数量">
                <el-input-number v-model="createForm.questionCount" :min="5" :max="20" :step="5" style="width:100%" />
              </el-form-item>
              <el-form-item label="限时(分钟)">
                <el-input-number v-model="createTimeLimit" :min="5" :max="30" :step="5" style="width:100%" />
              </el-form-item>
              <el-button type="primary" style="width:100%" size="large" :loading="creating" @click="handleCreate">
                🚀 创建房间
              </el-button>
            </el-form>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover" class="pk-card">
            <template #header>
              <div class="card-title">🔗 加入PK房间</div>
            </template>
            <el-form label-width="100px">
              <el-form-item label="房间号">
                <el-input v-model="joinCode" placeholder="请输入6位房间号" maxlength="6" style="width:100%" @keyup.enter="handleJoin">
                  <template #append>
                    <el-button @click="handleJoin" :loading="joining">加入</el-button>
                  </template>
                </el-input>
              </el-form-item>
            </el-form>
            <el-divider />
            <div class="room-info" v-if="myRoomCode">
              <p>我的房间号：<el-tag type="success" size="large">{{ myRoomCode }}</el-tag></p>
              <p class="share-hint">将房间号分享给同学，邀请TA加入PK！</p>
              <el-button type="info" text size="small" @click="copyRoomCode"> 复制房间号</el-button>
            </div>
          </el-card>
        </el-col>
      </el-row>
    </div>

    <!-- 阶段二：等待其他玩家 -->
    <div v-if="phase === 'waiting'" class="waiting-area">
      <el-card shadow="hover" class="waiting-card">
        <div class="waiting-content">
          <div class="waiting-icon"></div>
          <h3>等待同学加入...</h3>
          <p class="room-code-display">房间号：<el-tag type="success" size="large">{{ myRoomCode }}</el-tag></p>
          <p class="share-hint">将房间号分享给同学，至少2人即可开始</p>

          <div class="member-list">
            <div v-for="(m, idx) in members" :key="idx" class="member-item">
              <el-avatar :size="40" class="member-avatar">{{ m.realName ? m.realName.charAt(0) : '?' }}</el-avatar>
              <span class="member-name">{{ m.realName || '未知' }}<el-tag v-if="m.isCreator" size="small" type="warning" style="margin-left:6px">房主</el-tag></span>
            </div>
            <div v-for="i in (4 - members.length)" :key="'empty'+i" class="member-item empty">
              <el-avatar :size="40" class="member-avatar empty-avatar">?</el-avatar>
              <span class="member-name">等待加入...</span>
            </div>
          </div>

          <el-button @click="checkRoomStatus" text size="small">🔄 刷新状态</el-button>
        </div>
      </el-card>
    </div>

    <!-- 阶段三：答题 -->
    <div v-if="phase === 'answering'" class="answering-area">
      <div class="answer-header">
        <el-tag type="info" size="large">房间：{{ myRoomCode }}</el-tag>
        <el-tag type="warning" size="large">⏱ 剩余：{{ formatTime(remainSeconds) }}</el-tag>
        <el-tag type="success" size="large">进度：{{ currentQuestionIndex + 1 }} / {{ pkQuestions.length }}</el-tag>
      </div>

      <el-card v-if="currentQuestion" class="question-card" shadow="hover">
        <template #header>
          <div class="q-header">
            <span>第 {{ currentQuestionIndex + 1 }} 题（{{ typeName(currentQuestion.type) }}）</span>
            <el-tag size="small" :type="difficultyTag(currentQuestion.difficulty)">{{ difficultyName(currentQuestion.difficulty) }}</el-tag>
          </div>
        </template>
        <p class="q-stem">{{ currentQuestion.stem }}</p>

        <!-- 单选题 -->
        <div v-if="currentQuestion.type === 1" class="options-group">
          <el-radio-group v-model="currentAnswer">
            <el-radio v-for="(opt, idx) in currentQuestion.options" :key="idx" :value="opt" class="option-item">
              {{ String.fromCharCode(65 + idx) }}. {{ opt }}
            </el-radio>
          </el-radio-group>
        </div>

        <!-- 判断题 -->
        <div v-else-if="currentQuestion.type === 3" class="judge-group">
          <el-radio-group v-model="currentAnswer">
            <el-radio value="正确" class="option-item">✅ 正确</el-radio>
            <el-radio value="错误" class="option-item">❌ 错误</el-radio>
          </el-radio-group>
        </div>

        <div class="q-actions">
          <el-button type="primary" size="large" :disabled="!currentAnswer" :loading="submitting" @click="submitCurrentAnswer">
            {{ currentQuestionIndex < pkQuestions.length - 1 ? '下一题 →' : '提交最后一题' }}
          </el-button>
        </div>
      </el-card>

      <!-- 实时排名 -->
      <el-card class="ranking-card" shadow="never">
        <template #header><span> 实时排名</span></template>
        <el-table :data="ranking" stripe size="small">
          <el-table-column label="排名" width="70">
            <template #default="{ row }">
              <span :class="['rank-badge', 'rank-' + row.rank]">{{ row.rank }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="answeredCount" label="已答" width="70" />
          <el-table-column label="正确率" width="100">
            <template #default="{ row }">
              <el-progress :percentage="Number(row.accuracy)" :stroke-width="12" style="width:80px" />
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <!-- 阶段四：结果展示 -->
    <div v-if="phase === 'result'" class="result-area">
      <el-card shadow="hover" class="result-card">
        <div class="result-header">
          <div class="result-icon">{{ myRank === 1 ? '' : myRank === 2 ? '' : myRank === 3 ? '' : '🎖️' }}</div>
          <h2>PK结束！你获得了第 {{ myRank }} 名</h2>
        </div>

        <el-table :data="ranking" stripe>
          <el-table-column label="排名" width="80">
            <template #default="{ row }">
              <span :class="['rank-badge', 'rank-' + row.rank]">
                {{ row.rank === 1 ? '🥇' : row.rank === 2 ? '🥈' : row.rank === 3 ? '🥉' : row.rank }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="realName" label="姓名" />
          <el-table-column prop="answeredCount" label="答题数" width="80" />
          <el-table-column prop="correctCount" label="正确数" width="80" />
          <el-table-column label="正确率" width="120">
            <template #default="{ row }">
              <el-tag :type="Number(row.accuracy) >= 80 ? 'success' : Number(row.accuracy) >= 60 ? 'warning' : 'danger'">
                {{ row.accuracy }}%
              </el-tag>
            </template>
          </el-table-column>
        </el-table>

        <div class="result-actions">
          <el-button type="primary" @click="handleRematch">🔄 再来一局</el-button>
          <el-button @click="goBack">返回自主智练</el-button>
        </div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getSubjects, pkCreateRoom, pkJoinRoom, pkGetQuestions, pkSubmitAnswer, pkGetRanking, pkGetRoomStatus } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const router = useRouter()

// 阶段: lobby | waiting | answering | result
const phase = ref('lobby')
const pageLoading = ref(false)

// 学科
const subjects = ref([])

// 创建表单
const createForm = ref({ subjectId: null, questionCount: 10 })
const createTimeLimit = ref(10)
const creating = ref(false)

// 加入
const joinCode = ref('')
const joining = ref(false)

// 房间信息
const myRoomCode = ref('')
const members = ref([])

// PK答题
const pkQuestions = ref([])
const currentQuestionIndex = ref(0)
const currentAnswer = ref(null)
const submitting = ref(false)
const remainSeconds = ref(600)
let countdownTimer = null
let rankingTimer = null

// 排名
const ranking = ref([])
const myRank = ref(0)

const currentQuestion = computed(() => pkQuestions.value[currentQuestionIndex.value] || null)

onMounted(async () => {
  pageLoading.value = true
  try {
    const gradeLevel = userStore.userInfo?.gradeLevel || 0
    const res = await getSubjects(gradeLevel)
    subjects.value = res.data || []
  } catch {}
  pageLoading.value = false

  // 检查是否有进行中的PK
  const savedCode = sessionStorage.getItem('pkRoomCode')
  if (savedCode) {
    myRoomCode.value = savedCode
    await checkRoomStatus()
  }
})

onUnmounted(() => {
  clearTimers()
})

function clearTimers() {
  if (countdownTimer) clearInterval(countdownTimer)
  if (rankingTimer) clearInterval(rankingTimer)
}

function typeName(type) {
  const map = { 1: '单选题', 2: '多选题', 3: '判断题', 4: '填空题' }
  return map[type] || '未知'
}

function difficultyName(d) {
  const map = { 1: '基础', 2: '中档', 3: '拔高' }
  return map[d] || '未知'
}

function difficultyTag(d) {
  const map = { 1: 'success', 2: 'warning', 3: 'danger' }
  return map[d] || 'info'
}

function formatTime(seconds) {
  const m = Math.floor(seconds / 60)
  const s = seconds % 60
  return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function goBack() {
  clearTimers()
  sessionStorage.removeItem('pkRoomCode')
  router.push('/student/selfPractice')
}

async function handleCreate() {
  if (!createForm.value.subjectId) {
    ElMessage.warning('请选择学科')
    return
  }
  creating.value = true
  try {
    const res = await pkCreateRoom({
      subjectId: createForm.value.subjectId,
      questionCount: createForm.value.questionCount,
      timeLimitSeconds: createTimeLimit.value * 60
    })
    if (res.code === 200) {
      myRoomCode.value = res.data.roomCode
      sessionStorage.setItem('pkRoomCode', res.data.roomCode)
      remainSeconds.value = res.data.timeLimitSeconds
      ElMessage.success(`房间创建成功！房间号：${res.data.roomCode}`)
      phase.value = 'waiting'
      await loadMembers()
    } else {
      ElMessage.error(res.message || '创建失败')
    }
  } catch {
    ElMessage.error('创建失败，请重试')
  } finally {
    creating.value = false
  }
}

async function handleJoin() {
  if (!joinCode.value || joinCode.value.length !== 6) {
    ElMessage.warning('请输入6位房间号')
    return
  }
  joining.value = true
  try {
    const res = await pkJoinRoom({ roomCode: joinCode.value })
    if (res.code === 200) {
      myRoomCode.value = joinCode.value
      sessionStorage.setItem('pkRoomCode', joinCode.value)
      remainSeconds.value = res.data.timeLimitSeconds
      ElMessage.success('加入成功！')
      if (res.data.status >= 1) {
        await startAnswering()
      } else {
        phase.value = 'waiting'
        await loadMembers()
      }
    } else {
      ElMessage.error(res.message || '加入失败')
    }
  } catch {
    ElMessage.error('加入失败，请检查房间号')
  } finally {
    joining.value = false
  }
}

async function loadMembers() {
  try {
    const res = await pkGetRoomStatus(myRoomCode.value)
    if (res.code === 200) {
      members.value = res.data.members || []
      if (res.data.status >= 1) {
        await startAnswering()
      }
    }
  } catch {}
}

async function checkRoomStatus() {
  if (!myRoomCode.value) return
  await loadMembers()
  // 同时刷新排名
  if (phase.value === 'answering') {
    await loadRanking()
  }
}

async function startAnswering() {
  phase.value = 'answering'
  currentQuestionIndex.value = 0
  currentAnswer.value = null

  try {
    const res = await pkGetQuestions(myRoomCode.value)
    if (res.code === 200) {
      pkQuestions.value = res.data
      // 启动倒计时
      countdownTimer = setInterval(() => {
        remainSeconds.value--
        if (remainSeconds.value <= 0) {
          clearTimers()
          ElMessage.warning('时间到！')
          showResult()
        }
      }, 1000)
      // 定时刷新排名
      rankingTimer = setInterval(() => loadRanking(), 3000)
      await loadRanking()
    } else {
      ElMessage.error(res.message || '获取题目失败')
      phase.value = 'lobby'
    }
  } catch {
    ElMessage.error('获取题目失败')
    phase.value = 'lobby'
  }
}

async function submitCurrentAnswer() {
  if (!currentAnswer.value) return
  submitting.value = true
  try {
    const res = await pkSubmitAnswer({
      roomCode: myRoomCode.value,
      questionId: currentQuestion.value.id,
      userAnswer: currentAnswer.value
    })
    if (res.code === 200) {
      if (res.data.correct) {
        ElMessage.success('✅ 回答正确！')
      } else {
        ElMessage.error(`❌ 回答错误！正确答案：${res.data.correctAnswer}`)
      }

      if (currentQuestionIndex.value < pkQuestions.value.length - 1) {
        currentQuestionIndex.value++
        currentAnswer.value = null
      } else {
        // 全部答完
        clearTimers()
        ElMessage.success('🎉 你已完成所有题目！')
        showResult()
      }
      await loadRanking()
    } else {
      ElMessage.error(res.message || '提交失败')
    }
  } catch {
    ElMessage.error('提交失败，请重试')
  } finally {
    submitting.value = false
  }
}

async function loadRanking() {
  try {
    const res = await pkGetRanking(myRoomCode.value)
    if (res.code === 200) {
      ranking.value = res.data || []
      // 找到自己的排名
      // 这里简化处理，假设当前用户是第一个匹配到的
      // 实际应该通过 userId 匹配
    }
  } catch {}
}

async function showResult() {
  phase.value = 'result'
  await loadRanking()
  // 简单计算自己的排名
  // 实际应该根据 userId 匹配
}

async function handleRematch() {
  sessionStorage.removeItem('pkRoomCode')
  myRoomCode.value = ''
  members.value = []
  ranking.value = []
  pkQuestions.value = []
  currentQuestionIndex.value = 0
  currentAnswer.value = null
  phase.value = 'lobby'
}

function copyRoomCode() {
  if (myRoomCode.value) {
    navigator.clipboard.writeText(myRoomCode.value).then(() => {
      ElMessage.success('房间号已复制到剪贴板')
    }).catch(() => {
      ElMessage.info(`房间号：${myRoomCode.value}`)
    })
  }
}
</script>

<style scoped>
.pk-container {
  padding: 20px;
  max-width: 1100px;
  margin: 0 auto;
}

.pk-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.pk-header h2 {
  margin: 0;
  font-size: 22px;
}

.pk-card {
  border-radius: 12px;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.room-info {
  text-align: center;
  padding: 10px 0;
}

.share-hint {
  color: #909399;
  font-size: 13px;
  margin: 8px 0;
}

/* 等待区域 */
.waiting-card {
  max-width: 500px;
  margin: 40px auto;
  border-radius: 12px;
}

.waiting-content {
  text-align: center;
  padding: 20px 0;
}

.waiting-icon {
  font-size: 60px;
  margin-bottom: 10px;
}

.room-code-display {
  margin: 16px 0;
}

.member-list {
  display: flex;
  justify-content: center;
  gap: 20px;
  margin: 24px 0;
  flex-wrap: wrap;
}

.member-item {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
}

.member-avatar {
  background: #409EFF;
  color: #fff;
  font-weight: 600;
}

.empty-avatar {
  background: #e4e7ed;
  color: #909399;
}

.member-name {
  font-size: 13px;
  color: #606266;
}

/* 答题区域 */
.answer-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding: 12px 20px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
}

.question-card {
  margin-bottom: 20px;
  border-radius: 12px;
}

.q-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.q-stem {
  font-size: 16px;
  line-height: 1.8;
  margin: 16px 0 24px;
  font-weight: 500;
}

.options-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.option-item {
  margin-bottom: 8px;
}

.judge-group {
  display: flex;
  gap: 30px;
}

.q-actions {
  text-align: center;
  margin-top: 24px;
}

.ranking-card {
  border-radius: 8px;
}

/* 排名徽章 */
.rank-badge {
  display: inline-block;
  width: 28px;
  height: 28px;
  line-height: 28px;
  text-align: center;
  border-radius: 50%;
  font-weight: 700;
  font-size: 13px;
}

.rank-1 { background: #FFD700; color: #fff; }
.rank-2 { background: #C0C0C0; color: #fff; }
.rank-3 { background: #CD7F32; color: #fff; }

/* 结果区域 */
.result-card {
  max-width: 700px;
  margin: 20px auto;
  border-radius: 12px;
}

.result-header {
  text-align: center;
  margin-bottom: 24px;
}

.result-icon {
  font-size: 60px;
}

.result-actions {
  text-align: center;
  margin-top: 24px;
  display: flex;
  justify-content: center;
  gap: 16px;
}
</style>
