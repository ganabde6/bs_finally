<template>
  <div class="pk-ls-container">
    <el-card shadow="never">
      <div class="page-header">
        <div class="page-header-left">
          <h1 class="page-title">英语听说 PK</h1>
          <p class="page-subtitle">与同学发起听说挑战,比拼发音与表达</p>
        </div>
        <el-button type="primary" @click="showCreateDialog = true">发起挑战</el-button>
      </div>

      <!-- 我的挑战列表 -->
      <el-table :data="challengeList" stripe>
        <el-table-column prop="roomCode" label="房间号" width="120" />
        <el-table-column prop="questionTitle" label="题目标题" min-width="200" />
        <el-table-column prop="questionType" label="题型" width="100" />
        <el-table-column prop="status" label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="160" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" size="small" @click="viewDetail(row)">查看</el-button>
            <el-button v-if="row.status === 0 && !row.isCreator" link type="success" size="small" @click="acceptChallenge(row)">接受</el-button>
            <el-button v-if="row.status === 1" link type="warning" size="small" @click="showSubmitDialog(row)">作答</el-button>
            <el-button v-if="row.status === 2" link type="info" size="small" @click="viewResult(row)">结果</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 发起挑战弹窗 -->
    <el-dialog v-model="showCreateDialog" title="发起英语听说 PK 挑战" width="600px">
      <el-form :model="createForm" label-width="100px">
        <el-form-item label="题目标题">
          <el-input v-model="createForm.questionTitle" placeholder="请输入题目标题" />
        </el-form-item>
        <el-form-item label="题目内容">
          <el-input v-model="createForm.questionContent" type="textarea" :rows="6" placeholder="请输入题目内容（英文）" />
        </el-form-item>
        <el-form-item label="参考文本">
          <el-input v-model="createForm.referenceText" type="textarea" :rows="4" placeholder="请输入参考文本/标准答案（英文）" />
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="createForm.questionType" style="width: 100%">
            <el-option label="模仿朗读" value="模仿朗读" />
            <el-option label="故事复述" value="故事复述" />
            <el-option label="角色扮演" value="角色扮演" />
          </el-select>
        </el-form-item>
        <el-form-item label="评分要点">
          <el-input v-model="createForm.scorePoints" type="textarea" :rows="3" placeholder="请输入评分要点（中文）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCreateDialog = false">取消</el-button>
        <el-button type="primary" :loading="createLoading" @click="handleCreate">发起挑战</el-button>
      </template>
    </el-dialog>

    <!-- 接受挑战弹窗 -->
    <el-dialog v-model="showAcceptDialog" title="接受挑战" width="600px">
      <div v-if="currentChallenge">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="房间号">{{ currentChallenge.roomCode }}</el-descriptions-item>
          <el-descriptions-item label="题目标题">{{ currentChallenge.questionTitle }}</el-descriptions-item>
          <el-descriptions-item label="题目内容">
            <div style="white-space: pre-wrap">{{ currentChallenge.questionContent }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="参考文本">
            <div style="white-space: pre-wrap">{{ currentChallenge.referenceText }}</div>
          </el-descriptions-item>
          <el-descriptions-item label="题型">{{ currentChallenge.questionType }}</el-descriptions-item>
          <el-descriptions-item label="评分要点">
            <div style="white-space: pre-wrap">{{ currentChallenge.scorePoints }}</div>
          </el-descriptions-item>
        </el-descriptions>
      </div>
      <template #footer>
        <el-button @click="showAcceptDialog = false">取消</el-button>
        <el-button type="primary" @click="confirmAccept">接受挑战</el-button>
      </template>
    </el-dialog>

    <!-- 作答弹窗 -->
    <el-dialog v-model="showSubmitDialogFlag" title="提交 PK 作答" width="600px">
      <div v-if="currentChallenge">
        <div style="margin-bottom: 16px">
          <div style="font-weight: 600; margin-bottom: 8px">题目：{{ currentChallenge.questionTitle }}</div>
          <div style="background: #E8F1F4; padding: 12px; border-radius: 4px; white-space: pre-wrap">{{ currentChallenge.questionContent }}</div>
        </div>
        <el-upload
          :auto-upload="false"
          :limit="1"
          accept="audio/*"
          :on-change="onAudioChange"
          :on-remove="onAudioRemove"
          :file-list="audioFileList"
        >
          <el-button size="small">选择音频文件</el-button>
          <template #tip>
            <div class="el-upload__tip">支持 MP3/WAV/M4A 格式，最大 10MB</div>
          </template>
        </el-upload>
        <el-input v-model="supplementText" type="textarea" :rows="3" placeholder="补充文字说明（可选）" style="margin-top: 12px" />
      </div>
      <template #footer>
        <el-button @click="showSubmitDialogFlag = false">取消</el-button>
        <el-button type="primary" :loading="submitLoading" @click="handleSubmit">提交作答</el-button>
      </template>
    </el-dialog>

    <!-- 结果弹窗 -->
    <el-dialog v-model="showResultDialog" title="PK 结果" width="700px">
      <div v-if="pkResult">
        <div style="font-weight: 600; margin-bottom: 16px; text-align: center">
          题目：{{ pkResult.questionTitle }}
        </div>
        <el-table :data="pkResult.players" stripe>
          <el-table-column label="排名" width="80">
            <template #default="{ row }">
              <span :class="['rank-badge', 'rank-' + row.rank]">
                {{ row.rank === 1 ? '🥇' : row.rank === 2 ? '🥈' : row.rank }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="studentId" label="学生ID" width="100" />
          <el-table-column prop="totalScore" label="总分" width="80">
            <template #default="{ row }">
              <span :class="scoreClass(row.totalScore)">{{ row.totalScore }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="pronunciationScore" label="发音" width="80" />
          <el-table-column prop="fluencyScore" label="流利度" width="80" />
          <el-table-column prop="grammarScore" label="语法" width="80" />
          <el-table-column prop="contentScore" label="内容" width="80" />
          <el-table-column prop="aiFeedback" label="AI 评语" min-width="200" show-overflow-tooltip />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { pkLsList, pkLsCreate, pkLsAccept, pkLsDetail, pkLsSubmit, pkLsResult } from '@/api'

const challengeList = ref([])
const showCreateDialog = ref(false)
const showAcceptDialog = ref(false)
const showSubmitDialogFlag = ref(false)
const showResultDialog = ref(false)
const createLoading = ref(false)
const submitLoading = ref(false)
const currentChallenge = ref(null)
const pkResult = ref(null)
const audioFileList = ref([])
const audioFile = ref(null)
const supplementText = ref('')

const createForm = ref({
  questionTitle: '',
  questionContent: '',
  referenceText: '',
  questionType: '模仿朗读',
  scorePoints: ''
})

const statusLabel = (status) => ({ 0: '等待挑战', 1: '已接受', 2: '已完成' })[status] || '未知'
const statusType = (status) => ({ 0: 'info', 1: 'warning', 2: 'success' })[status] || 'info'
const scoreClass = (s) => {
  if (s >= 85) return 'score-good'
  if (s >= 60) return 'score-mid'
  return 'score-bad'
}

const loadChallenges = async () => {
  const res = await pkLsList()
  challengeList.value = res.data || []
}

const handleCreate = async () => {
  if (!createForm.value.questionContent) {
    ElMessage.warning('请输入题目内容')
    return
  }
  createLoading.value = true
  try {
    await pkLsCreate(createForm.value)
    ElMessage.success('挑战发起成功')
    showCreateDialog.value = false
    createForm.value = {
      questionTitle: '',
      questionContent: '',
      referenceText: '',
      questionType: '模仿朗读',
      scorePoints: ''
    }
    await loadChallenges()
  } catch (e) {
    // 拦截器已提示
  } finally {
    createLoading.value = false
  }
}

const viewDetail = async (row) => {
  const res = await pkLsDetail(row.roomCode)
  currentChallenge.value = res.data
  if (row.status === 0 && !row.isCreator) {
    showAcceptDialog.value = true
  } else if (row.status === 1) {
    showSubmitDialogFlag.value = true
  } else if (row.status === 2) {
    viewResult(row)
  }
}

const acceptChallenge = async (row) => {
  const res = await pkLsDetail(row.roomCode)
  currentChallenge.value = res.data
  showAcceptDialog.value = true
}

const confirmAccept = async () => {
  if (!currentChallenge.value) return
  try {
    await pkLsAccept(currentChallenge.value.roomCode)
    ElMessage.success('已接受挑战')
    showAcceptDialog.value = false
    await loadChallenges()
  } catch (e) {
    // 拦截器已提示
  }
}

const showSubmitDialog = async (row) => {
  const res = await pkLsDetail(row.roomCode)
  currentChallenge.value = res.data
  audioFileList.value = []
  audioFile.value = null
  supplementText.value = ''
  showSubmitDialogFlag.value = true
}

const onAudioChange = (file) => {
  audioFile.value = file.raw
  audioFileList.value = [file]
}

const onAudioRemove = () => {
  audioFileList.value = []
  audioFile.value = null
}

const handleSubmit = async () => {
  if (!audioFile.value) {
    ElMessage.warning('请选择音频文件')
    return
  }
  submitLoading.value = true
  try {
    await pkLsSubmit(currentChallenge.value.roomCode, audioFile.value, supplementText.value)
    ElMessage.success('作答提交成功')
    showSubmitDialogFlag.value = false
    await loadChallenges()
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitLoading.value = false
  }
}

const viewResult = async (row) => {
  const res = await pkLsResult(row.roomCode)
  pkResult.value = res.data
  showResultDialog.value = true
}

onMounted(() => {
  loadChallenges()
})
</script>

<style scoped>
.pk-ls-container {
  padding: 20px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.page-title {
  font-size: 18px;
  font-weight: 600;
}
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
.score-good { color: #67c23a; font-weight: 600; }
.score-mid { color: #e6a23c; font-weight: 600; }
.score-bad { color: #f56c6c; font-weight: 600; }
</style>
