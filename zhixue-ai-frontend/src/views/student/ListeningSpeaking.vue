<template>
  <div class="listening-speaking">
    <!-- 页面头部 -->
    <div class="page-header">
      <div class="header-left">
        <h1 class="page-title">英语听说练习</h1>
        <p class="page-subtitle">AI 智能评分 · 实时反馈 · 随题设计</p>
      </div>
      <div class="header-actions">
        <el-button type="primary" @click="showTextDialog = true">
          <el-icon><Edit /></el-icon>自定义文本出题
        </el-button>
        <el-button @click="showImageDialog = true">
          <el-icon><PictureFilled /></el-icon>图片出题
        </el-button>
      </div>
    </div>

    <!-- 主体：两栏布局 -->
    <div class="main-content">
      <!-- 左侧：题目列表 -->
      <div class="left-panel">
        <div class="panel-card">
          <div class="panel-header">
            <h2>题目列表</h2>
            <span class="question-count">{{ filteredQuestions.length }} 题</span>
          </div>

          <!-- 筛选区 -->
          <div class="filter-section">
            <div class="filter-row">
              <span class="filter-label">学段</span>
              <el-select v-model="gradeLevel" size="small" style="width:100%" @change="loadQuestions">
                <el-option label="全部" :value="0" />
                <el-option label="初中" :value="2" />
                <el-option label="高中" :value="3" />
              </el-select>
            </div>
            <div class="filter-row">
              <span class="filter-label">题型</span>
              <el-select v-model="filterType" size="small" style="width:100%" clearable placeholder="全部题型" @change="loadQuestions">
                <el-option label="模仿朗读" value="模仿朗读" />
                <el-option label="故事复述" value="故事复述" />
                <el-option label="角色扮演" value="角色扮演" />
              </el-select>
            </div>
            <div class="filter-row">
              <span class="filter-label">难度</span>
              <el-select v-model="filterDiff" size="small" style="width:100%" clearable placeholder="全部" @change="loadQuestions">
                <el-option label="简单" :value="1" />
                <el-option label="中等" :value="2" />
                <el-option label="困难" :value="3" />
              </el-select>
            </div>
          </div>

          <!-- 题目列表 -->
          <div class="question-list">
            <div
              v-for="q in filteredQuestions"
              :key="q.id"
              class="question-item"
              :class="{ active: currentQuestion && currentQuestion.id === q.id }"
              @click="selectQuestion(q)"
            >
              <div class="question-title">{{ q.title }}</div>
              <div class="meta-tags">
                <span class="tag tag-type">{{ typeLabel(q.questionType) }}</span>
                <span class="tag" :class="'tag-diff-' + q.difficulty">{{ diffLabel(q.difficulty) }}</span>
              </div>
            </div>
            <el-empty v-if="!filteredQuestions.length" description="暂无题目" :image-size="60" />
          </div>
        </div>
      </div>

      <!-- 右侧：作答区 -->
      <div class="right-area" v-loading="submitting">
        <template v-if="currentQuestion">
          <!-- 题目信息 -->
          <div class="question-header">
            <h2>{{ currentQuestion.title }}</h2>
            <div class="question-tags">
              <span class="tag tag-type">{{ typeLabel(currentQuestion.questionType) }}</span>
              <span class="tag" :class="'tag-diff-' + currentQuestion.difficulty">{{ diffLabel(currentQuestion.difficulty) }}</span>
            </div>
          </div>

          <!-- 题目内容卡片(故事复述:提交前只显示指令,故事正文听录音) -->
          <div class="question-content-card">
            <div class="content-label">题目内容</div>
            <div class="content-text" v-if="isListeningType">{{ listeningInstruction }}</div>
            <div class="content-text" v-else v-html="renderLatex(currentQuestion.content)"></div>
          </div>

          <!-- 参考音频(故事复述:无音频文件时用 TTS 朗读故事,原文提交后才展示) -->
          <div class="reference-row">
            <div class="ref-audio">
              <el-button v-if="currentQuestion.referenceAudio" type="primary" plain @click="toggleRefAudio">
                <el-icon v-if="refPlaying"><VideoPause /></el-icon>
                <el-icon v-else><VideoPlay /></el-icon>
                {{ refPlaying ? '暂停' : '播放参考音频' }}
              </el-button>
              <el-button v-else-if="isListeningType" type="primary" @click="speakStory">
                <el-icon v-if="ttsSpeaking"><VideoPause /></el-icon>
                <el-icon v-else><VideoPlay /></el-icon>
                {{ ttsSpeaking ? '暂停播放' : '播放录音' }}
              </el-button>
              <el-button v-else type="info" plain disabled>暂无参考音频</el-button>
              <span v-if="isListeningType && !currentQuestion.referenceAudio" class="tts-hint">
                请认真听录音,跟读/复述时不要看原文;原文将在提交批改后展示
              </span>
            </div>
          </div>

          <!-- 查看参考文本 -->
          <div class="ref-text-collapse" v-if="currentQuestion.referenceText && !isListeningType">
            <el-collapse>
              <el-collapse-item title="查看参考文本" name="ref">
                <pre class="ref-text-content">{{ currentQuestion.referenceText }}</pre>
              </el-collapse-item>
            </el-collapse>
          </div>

          <!-- 录音区和提交区 -->
          <div class="answer-area">
            <!-- 录音区 -->
            <div class="record-section">
              <div class="section-header">
                <span class="section-icon"></span>
                <h3>在线录音作答</h3>
              </div>
              <div class="mic-device-row">
                <span class="mic-device-label">录音设备</span>
                <el-select v-model="selectedMic" size="small" class="mic-select" :disabled="recording" placeholder="系统默认麦克风">
                  <el-option v-for="d in micDevices" :key="d.deviceId" :label="d.label" :value="d.deviceId" />
                </el-select>
                <span v-if="recording" class="mic-level" :class="{ silent: micSilentNow }">
                  输入电平 {{ micLevelDisplay }}
                </span>
                <span v-if="recording" class="mic-active-device" :title="activeMicLabel">
                  实际录音:{{ activeMicLabel || '检测中…' }}
                </span>
              </div>
              <div class="record-controls">
                <el-button
                  v-if="!recording"
                  type="danger"
                  @click="startRecord"
                  class="record-btn"
                >
                  <el-icon><Microphone /></el-icon>{{ recordUrl ? '重新录制' : '开始录音' }}
                </el-button>
                <el-button
                  v-else
                  type="danger"
                  @click="stopRecord"
                  class="record-btn recording"
                >
                  <el-icon><VideoPause /></el-icon>停止录音
                </el-button>
                <el-button
                  v-if="recordUrl"
                  type="success"
                  plain
                  @click="togglePlayback"
                >
                  <el-icon v-if="playState === 'playing'"><VideoPause /></el-icon>
                  <el-icon v-else><VideoPlay /></el-icon>
                  {{ playState === 'playing' ? '暂停回放' : '播放回放' }}
                </el-button>
                <div class="record-timer">{{ formatDuration(recordSeconds) }}</div>
              </div>
              <div class="waveform-container">
                <canvas ref="waveCanvas" class="wave-canvas" :class="{ active: recording }"></canvas>
              </div>
              <div class="record-hint">
                {{ recording ? '正在录音，请朗读题目内容...' : (recordUrl ? '可播放回放、提交批改，或点「重新录制」再录一遍(将替换上一段录音)' : '点击「开始录音」使用麦克风朗读') }}
              </div>
            </div>

            <!-- 上传区 -->
            <div class="upload-section">
              <div class="section-header">
                <span class="section-icon">📁</span>
                <h3>上传音频文件</h3>
              </div>
              <el-upload
                drag
                :auto-upload="false"
                accept=".mp3,.wav,.m4a"
                :limit="1"
                :on-change="onFileChange"
                :on-remove="onFileRemove"
                :on-exceed="onFileExceed"
                :file-list="fileList"
                class="audio-upload"
              >
                <el-icon class="upload-icon"><UploadFilled /></el-icon>
                <div class="upload-text">
                  <p class="upload-main">拖拽音频文件到此处，或 <em>点击选择</em></p>
                  <p class="upload-tip">支持 MP3 / WAV / M4A 格式，文件大小不超过 10MB</p>
                </div>
              </el-upload>
            </div>

            <!-- 提交区 -->
            <div class="submit-section">
              <el-input
                v-model="supplementText"
                placeholder="文字补充（可选）：如朗读要点、回答内容等"
                clearable
                class="supplement-input"
              />
              <el-button
                type="primary"
                :loading="submitting"
                :disabled="!canSubmit"
                @click="handleSubmit"
                class="submit-btn"
              >
                提交批改
              </el-button>
            </div>
          </div>

          <!-- 空状态占位 -->
          <div class="answer-placeholder" v-if="!result">
            <el-empty description="提交后展示评分报告" :image-size="80" />
          </div>

          <!-- 评分报告 -->
          <div class="panel-card report-card" v-if="result">
            <div class="panel-header">
              <h2>AI 评分报告</h2>
            </div>
            <div class="report-body">
              <div class="report-top">
                <div class="score-circle">
                  <div class="score-value">{{ result.totalScore }}</div>
                  <div class="score-label">总分 / 100</div>
                </div>
                <div ref="radarChart" class="radar-chart"></div>
              </div>
              <div class="score-details">
                <div class="score-item">
                  <span class="score-icon"><el-icon :size="18"><Aim /></el-icon></span>
                  <div class="score-info">
                    <div class="score-name">发音</div>
                    <div class="score-val">{{ result.pronunciationScore }}</div>
                  </div>
                </div>
                <div class="score-item">
                  <span class="score-icon">💫</span>
                  <div class="score-info">
                    <div class="score-name">流利度</div>
                    <div class="score-val">{{ result.fluencyScore }}</div>
                  </div>
                </div>
                <div class="score-item">
                  <span class="score-icon"><el-icon :size="18"><EditPen /></el-icon></span>
                  <div class="score-info">
                    <div class="score-name">语法</div>
                    <div class="score-val">{{ result.grammarScore }}</div>
                  </div>
                </div>
                <div class="score-item">
                  <span class="score-icon"><el-icon :size="18"><LightBulb /></el-icon></span>
                  <div class="score-info">
                    <div class="score-name">内容</div>
                    <div class="score-val">{{ result.contentScore }}</div>
                  </div>
                </div>
              </div>
              <div class="ai-feedback-card">
                <div class="feedback-title">
                  <el-icon><Document /></el-icon>AI 识别文本
                </div>
                <div class="feedback-content">{{ result.recognizedText || '未识别到有效语音' }}</div>
              </div>
              <div class="ai-feedback-card" v-if="isListeningType && currentQuestion.referenceText">
                <div class="feedback-title">
                  <el-icon><Document /></el-icon>原文(参考)
                </div>
                <div class="feedback-content">{{ currentQuestion.referenceText }}</div>
              </div>
              <div class="ai-feedback-card">
                <div class="feedback-title">
                  <el-icon><ChatLineSquare /></el-icon>改进建议
                </div>
                <div class="feedback-content suggestion">{{ result.aiFeedback }}</div>
              </div>
            </div>
          </div>

          <!-- 历史记录 -->
          <div class="panel-card history-card">
            <div class="panel-header">
              <h2>练习历史与评分趋势</h2>
            </div>
            <div v-if="records.length" ref="trendChart" class="trend-chart"></div>
            <el-table :data="records" size="small" stripe class="history-table">
              <el-table-column prop="questionTitle" label="题目" min-width="180" show-overflow-tooltip />
              <el-table-column prop="questionType" label="题型" width="90" />
              <el-table-column prop="totalScore" label="总分" width="80" sortable>
                <template #default="{ row }">
                  <span :class="scoreClass(row.totalScore)">{{ row.totalScore }}</span>
                </template>
              </el-table-column>
              <el-table-column label="发音" width="70">
                <template #default="{ row }">{{ row.pronunciationScore }}</template>
              </el-table-column>
              <el-table-column label="流利度" width="70">
                <template #default="{ row }">{{ row.fluencyScore }}</template>
              </el-table-column>
              <el-table-column label="语法" width="70">
                <template #default="{ row }">{{ row.grammarScore }}</template>
              </el-table-column>
              <el-table-column label="内容" width="70">
                <template #default="{ row }">{{ row.contentScore }}</template>
              </el-table-column>
              <el-table-column prop="createTime" label="时间" width="160" />
              <el-table-column label="操作" width="160" fixed="right">
                <template #default="{ row }">
                  <el-button v-if="row.audioPath" link type="primary" size="small" @click="playRecordAudio(row.audioPath)">听录音</el-button>
                  <el-button link type="success" size="small" @click="handleGenerateSimilar(row)">生成同类</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </template>
        <el-empty v-else description="请选择一道题目开始练习" />
      </div>
    </div>

    <!-- 自定义文本出题弹窗 -->
    <el-dialog v-model="showTextDialog" title="自定义文本出题" width="500px">
      <el-form label-width="80px">
        <el-form-item label="英文文本">
          <el-input v-model="textGenInput" type="textarea" :rows="6" placeholder="粘贴或输入英文文本..." />
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="textGenType" style="width: 100%">
            <el-option label="模仿朗读" value="模仿朗读" />
            <el-option label="故事复述" value="故事复述" />
            <el-option label="角色扮演" value="角色扮演" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showTextDialog = false">取消</el-button>
        <el-button type="primary" :loading="textGenLoading" @click="handleTextGenerate">AI 生成题目</el-button>
      </template>
    </el-dialog>

    <!-- 图片出题弹窗 -->
    <el-dialog v-model="showImageDialog" title="图片出题" width="500px">
      <el-form label-width="80px">
        <el-form-item label="上传图片">
          <el-upload
            :auto-upload="false"
            :limit="1"
            accept="image/*"
            :on-change="onImageChange"
            :on-remove="onImageRemove"
            :file-list="imageFileList"
          >
            <el-button size="small">选择图片</el-button>
            <template #tip>
              <div class="el-upload__tip">支持 JPG/PNG 格式</div>
            </template>
          </el-upload>
        </el-form-item>
        <el-form-item label="题型">
          <el-select v-model="imageGenType" style="width: 100%">
            <el-option label="故事复述" value="故事复述" />
            <el-option label="角色扮演" value="角色扮演" />
            <el-option label="模仿朗读" value="模仿朗读" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showImageDialog = false">取消</el-button>
        <el-button type="primary" :loading="imageGenLoading" @click="handleImageGenerate">AI 生成题目</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { Edit, PictureFilled, Microphone, Document, ChatLineSquare, UploadFilled, VideoPlay, VideoPause } from '@element-plus/icons-vue'
import { listeningSpeakingList, listeningSpeakingDetail, submitListeningSpeaking, listeningSpeakingRecords, lsTtsAudio, lsGenerateFromText, lsGenerateFromImage, lsGenerateSimilar, lsGetTopics } from '@/api'
import { renderLatex } from '@/utils/latex'

// ============ 题目 ============
const questions = ref([])
const currentQuestion = ref(null)
const gradeLevel = ref(0)
const topics = ref([])
const filterTopic = ref('')
const filterType = ref('')
const filterDiff = ref(null)

const typeLabel = (t) => t || '模仿朗读'
const diffLabel = (d) => ({ 1: '简单', 2: '中等', 3: '困难' })[d] || '中等'
const diffType = (d) => ({ 1: 'success', 2: 'warning', 3: 'danger' })[d] || 'warning'

const filteredQuestions = computed(() => {
  return questions.value.filter(q => {
    if (filterTopic.value && q.topic !== filterTopic.value) return false
    if (filterType.value && q.questionType !== filterType.value) return false
    if (filterDiff.value && q.difficulty !== filterDiff.value) return false
    return true
  })
})

const loadQuestions = async () => {
  const res = await listeningSpeakingList(gradeLevel.value || undefined)
  questions.value = res.data || []
  const topicRes = await lsGetTopics(gradeLevel.value || undefined)
  topics.value = topicRes.data || []
  if (filteredQuestions.value.length) {
    selectQuestion(filteredQuestions.value[0])
  } else {
    currentQuestion.value = null
  }
}

const selectQuestion = async (q) => {
  const res = await listeningSpeakingDetail(q.id)
  currentQuestion.value = res.data
  resetAnswer()
}

// ============ 自定义文本出题 ============
const showTextDialog = ref(false)
const textGenInput = ref('')
const textGenType = ref('模仿朗读')
const textGenLoading = ref(false)

const handleTextGenerate = async () => {
  if (!textGenInput.value.trim()) {
    ElMessage.warning('请输入英文文本')
    return
  }
  textGenLoading.value = true
  try {
    const res = await lsGenerateFromText({
      text: textGenInput.value,
      questionType: textGenType.value,
      gradeLevel: gradeLevel.value || undefined
    })
    ElMessage.success('题目生成成功')
    showTextDialog.value = false
    textGenInput.value = ''
    await loadQuestions()
    const newQ = questions.value.find(q => q.id === res.data.id)
    if (newQ) selectQuestion(newQ)
  } catch (e) {
    // 拦截器已提示
  } finally {
    textGenLoading.value = false
  }
}

// ============ 图片出题 ============
const showImageDialog = ref(false)
const imageGenType = ref('故事复述')
const imageGenLoading = ref(false)
const imageFileList = ref([])
const imageBase64 = ref('')

const onImageChange = (file) => {
  const f = file.raw
  if (!f) return
  const reader = new FileReader()
  reader.onload = (e) => {
    imageBase64.value = e.target.result.split(',')[1]
  }
  reader.readAsDataURL(f)
  imageFileList.value = [file]
}

const onImageRemove = () => {
  imageFileList.value = []
  imageBase64.value = ''
}

const handleImageGenerate = async () => {
  if (!imageBase64.value) {
    ElMessage.warning('请上传图片')
    return
  }
  imageGenLoading.value = true
  try {
    const res = await lsGenerateFromImage({
      imageBase64: imageBase64.value,
      questionType: imageGenType.value,
      gradeLevel: gradeLevel.value || undefined
    })
    ElMessage.success('题目生成成功')
    showImageDialog.value = false
    imageFileList.value = []
    imageBase64.value = ''
    await loadQuestions()
    const newQ = questions.value.find(q => q.id === res.data.id)
    if (newQ) selectQuestion(newQ)
  } catch (e) {
    // 拦截器已提示
  } finally {
    imageGenLoading.value = false
  }
}

// ============ 生成同类练习 ============
const handleGenerateSimilar = async (row) => {
  try {
    const res = await lsGenerateSimilar({ previousQuestionId: row.questionId })
    ElMessage.success('同类题目生成成功')
    await loadQuestions()
    const newQ = questions.value.find(q => q.id === res.data.id)
    if (newQ) selectQuestion(newQ)
  } catch (e) {
    // 拦截器已提示
  }
}

// ============ 录音 ============
const recording = ref(false)
const recordSeconds = ref(0)
const recordUrl = ref('')
const playState = ref('')
const waveCanvas = ref(null)
const submitFile = ref(null)
const fileList = ref([])

let mediaRecorder = null
let stream = null
let recordingSeq = 0 // 录音轮次号:防止上一轮迟到的转码回调用旧音频覆盖新录音
let audioContext = null
let analyser = null
let animationId = null
let recordChunks = []
let recordTimer = null
let recordBlob = null
let recordStartTime = 0
let playbackAudio = null
let lastMime = 'audio/webm'
let stopRequested = false // 已请求 stop,等待 onstop 收尾,防止二次 stop / 提前释放
const starting = ref(false) // startRecord 并发锁:申请麦克风期间防连点,连点会泄漏旧流并占死麦克风

// 录音设备选择:浏览器默认走系统默认录音设备,可能选到静音的虚拟设备,允许显式指定
const micDevices = ref([{ deviceId: '', label: '系统默认麦克风' }])
const selectedMic = ref(localStorage.getItem('ls-mic-device') || '')
const persistSelectedMic = (val) => {
  if (val) localStorage.setItem('ls-mic-device', val)
  else localStorage.removeItem('ls-mic-device')
}
const micLiveLevel = ref(0)
const activeMicLabel = ref('') // 浏览器实际抓取的录音设备名(用于排查"录不到声音"问题)
const micLevelDisplay = computed(() => Math.min(100, Math.round(micLiveLevel.value * 100 / 127)))
const micSilentNow = computed(() => recording.value && micMaxLevel < 3)

const loadMicDevices = async () => {
  if (!navigator.mediaDevices || !navigator.mediaDevices.enumerateDevices) return
  try {
    const devices = await navigator.mediaDevices.enumerateDevices()
    const inputs = devices.filter(d => d.kind === 'audioinput')
    if (inputs.length) {
      micDevices.value = inputs.map((d, i) => ({ deviceId: d.deviceId, label: d.label || ('麦克风 ' + (i + 1)) }))
      if (!micDevices.value.some(d => d.deviceId === selectedMic.value)) {
        selectedMic.value = ''
        localStorage.removeItem('ls-mic-device')
      }
      // 拿到设备名后:未手动选择且系统默认是虚拟麦克风时,自动切到真实麦克风
      const hasRealLabels = micDevices.value.some(d => d.label && !/^麦克风 \d+$/.test(d.label))
      if (hasRealLabels && !selectedMic.value) {
        const real = pickRealMic()
        if (real && micDevices.value.some(d => isVirtualMic(d))) {
          selectedMic.value = real.deviceId
          persistSelectedMic(real.deviceId)
        }
      }
    }
  } catch (e) {
    // 枚举失败时保持默认设备
  }
}

let micMaxLevel = 0
let micSilentWarned = false

// 虚拟麦克风识别:本机系统默认录音设备是网易虚拟麦克风(宿主应用未运行时输出纯静音),
// 浏览器走系统默认时录出来是无声的,需要自动避开
const VIRTUAL_MIC_RE = /网易|虚拟|virtual/i
const isVirtualMic = (d) => VIRTUAL_MIC_RE.test(d.label || '')
const pickRealMic = () => {
  const real = micDevices.value.filter(d => d.deviceId && !isVirtualMic(d))
  if (!real.length) return null
  return real.find(d => /英特尔|intel/i.test(d.label || '')) || real[0]
}

const drawWave = () => {
  const canvas = waveCanvas.value
  if (!canvas || !analyser) return
  const ctx = canvas.getContext('2d')
  const w = canvas.width
  const h = canvas.height
  const data = new Uint8Array(analyser.fftSize)
  analyser.getByteTimeDomainData(data)
  // 麦克风输入电平检测:长时间检测不到输入,说明录的是静音
  let level = 0
  for (let i = 0; i < data.length; i++) {
    const v = Math.abs(data[i] - 128)
    if (v > level) level = v
  }
  if (level > micMaxLevel) micMaxLevel = level
  micLiveLevel.value = level
  if (!micSilentWarned && Date.now() - recordStartTime > 2500 && micMaxLevel < 3) {
    micSilentWarned = true
    ElMessage.warning('未检测到麦克风输入,本次录音将是静音。请在上方「录音设备」中切换其他设备,或到 Windows 设置→系统→声音→输入 检查默认录音设备是否静音')
  }
  ctx.clearRect(0, 0, w, h)
  ctx.lineWidth = 2
  ctx.strokeStyle = '#409eff'
  ctx.beginPath()
  const slice = w / data.length
  for (let i = 0; i < data.length; i++) {
    const y = (data[i] / 128) * (h / 2)
    if (i === 0) ctx.moveTo(i * slice, y)
    else ctx.lineTo(i * slice, y)
  }
  ctx.stroke()
  animationId = requestAnimationFrame(drawWave)
}

const blobToWavFile = (blob) => {
  return new Promise((resolve, reject) => {
    const ctx = new (window.AudioContext || window.webkitAudioContext)()
    const reader = new FileReader()
    reader.onload = async () => {
      try {
        const audioBuffer = await ctx.decodeAudioData(reader.result)
        const wavBlob = audioBufferToWav(audioBuffer)
        const file = new File([wavBlob], 'recording-' + Date.now() + '.wav', { type: 'audio/wav' })
        resolve(file)
      } catch (e) {
        reject(e)
      } finally {
        ctx.close()
      }
    }
    reader.onerror = reject
    reader.readAsArrayBuffer(blob)
  })
}

const audioBufferToWav = (buffer) => {
  const numChannels = Math.min(2, buffer.numberOfChannels)
  const sampleRate = buffer.sampleRate
  const numFrames = buffer.length
  const bytesPerSample = 2
  const blockAlign = numChannels * bytesPerSample
  const dataSize = numFrames * blockAlign
  const bufferSize = 44 + dataSize
  const arrayBuffer = new ArrayBuffer(bufferSize)
  const view = new DataView(arrayBuffer)
  const writeString = (offset, str) => {
    for (let i = 0; i < str.length; i++) view.setUint8(offset + i, str.charCodeAt(i))
  }
  writeString(0, 'RIFF')
  view.setUint32(4, 36 + dataSize, true)
  writeString(8, 'WAVE')
  writeString(12, 'fmt ')
  view.setUint32(16, 16, true)
  view.setUint16(20, 1, true)
  view.setUint16(22, numChannels, true)
  view.setUint32(24, sampleRate, true)
  view.setUint32(28, sampleRate * blockAlign, true)
  view.setUint16(32, blockAlign, true)
  view.setUint16(34, 16, true)
  writeString(36, 'data')
  view.setUint32(40, dataSize, true)
  const channels = []
  for (let c = 0; c < numChannels; c++) channels.push(buffer.getChannelData(c))
  let offset = 44
  for (let i = 0; i < numFrames; i++) {
    for (let c = 0; c < numChannels; c++) {
      const sample = Math.max(-1, Math.min(1, channels[c][i]))
      view.setInt16(offset, sample < 0 ? sample * 0x8000 : sample * 0x7fff, true)
      offset += 2
    }
  }
  return new Blob([arrayBuffer], { type: 'audio/wav' })
}

// 本机音频栈被改过,二次申请麦克风可能永久挂起:包一层超时,挂死时给出明确提示而不是无响应
const getUserMediaWithTimeout = (constraint, ms = 10000) => {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => {
      reject(new Error('申请麦克风超时,请关闭其他占用麦克风的程序后重试'))
    }, ms)
    navigator.mediaDevices.getUserMedia(constraint).then(s => {
      clearTimeout(timer)
      resolve(s)
    }, e => {
      clearTimeout(timer)
      reject(e)
    })
  })
}

const startRecord = async () => {
  if (recording.value || starting.value) return
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia || !window.MediaRecorder) {
    ElMessage.error('当前浏览器不支持录音,请使用 Chrome/Edge 或改用上传音频文件')
    return
  }
  starting.value = true
  recordingSeq++
  const seq = recordingSeq
  // 重新录制时清掉旧批改报告:否则「AI 识别文本」仍显示上一段录音的识别结果
  if (recordUrl.value) result.value = null
  // 上一段回放还在播时先停掉,避免重录时新旧声音叠在一起
  if (playbackAudio) {
    playbackAudio.pause()
    playState.value = ''
  }
  try {
    // 强制释放上一轮占用的麦克风资源:旧流不释放,二次 getUserMedia 会打不开设备或静默回退到静音默认麦
    releaseMic()
    stopRequested = false
    const audioConstraint = { audio: true }
    if (selectedMic.value) {
      audioConstraint.audio = { deviceId: { ideal: selectedMic.value } }
    }
    stream = await getUserMediaWithTimeout(audioConstraint)
    stream = await ensureUsableMic(stream)
    // 记录浏览器实际抓取的录音设备,便于核对是否录到了正确的麦克风
    const track0 = stream.getAudioTracks()[0]
    const trackSettings = track0 && track0.getSettings ? track0.getSettings() : {}
    const devMatch = micDevices.value.find(d => d.deviceId === trackSettings.deviceId)
    activeMicLabel.value = (trackSettings && trackSettings.label) || (devMatch && devMatch.label) || '未知设备'
    if (devMatch && devMatch.label && !trackSettings.label) {
      selectedMic.value = devMatch.deviceId
    }
    const mimeTypes = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']
    const mimeType = mimeTypes.find(t => MediaRecorder.isTypeSupported(t)) || ''
    lastMime = mimeType || 'audio/webm'
    mediaRecorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream)
    audioContext = new (window.AudioContext || window.webkitAudioContext)()
    audioContext.resume().catch(() => {})
    const source = audioContext.createMediaStreamSource(stream)
    analyser = audioContext.createAnalyser()
    analyser.fftSize = 256
    source.connect(analyser)
    recordChunks = []
    micMaxLevel = 0
    micSilentWarned = false
    mediaRecorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) recordChunks.push(e.data)
    }
    mediaRecorder.onstop = async () => {
      stopRequested = false
      if (seq !== recordingSeq) return // 本轮已被更新的录音取代,丢弃旧结果,避免覆盖新录音
      recordBlob = new Blob(recordChunks, { type: lastMime })
      recordChunks = []
      recordUrl.value = URL.createObjectURL(recordBlob)
      playState.value = ''
      stopWave()
      releaseMic()
      try {
        submitFile.value = await blobToWavFile(recordBlob)
        ElMessage.success('录音完成,已转换为 WAV 格式')
        if (micMaxLevel < 3) {
          ElMessage.error('本次录音几乎检测不到声音,提交后可能识别失败,请检查麦克风后重新录制')
        }
      } catch (e) {
        ElMessage.warning('录音转码失败,请改用手动上传音频文件')
        submitFile.value = null
      }
    }
    // 设备被系统中途断开(本机音频栈不稳定):自动收尾,避免 UI 卡在"停止录音"且流泄漏
    track0.onended = () => {
      if (mediaRecorder && mediaRecorder.state !== 'inactive') {
        stopRequested = true
        mediaRecorder.stop()
      }
      recording.value = false
      micLiveLevel.value = 0
    }
    mediaRecorder.start()
    recording.value = true
    recordStartTime = Date.now()
    recordTimer = setInterval(() => {
      recordSeconds.value = Math.floor((Date.now() - recordStartTime) / 1000)
    }, 200)
    nextTick(drawWave)
  } catch (e) {
    recording.value = false
    releaseMic()
    ElMessage.error('无法访问麦克风:' + (e.message || e))
  } finally {
    starting.value = false
  }
}

// 拿到权限后才能读到设备名:若实际抓到的设备是虚拟麦克风且存在真实麦克风,自动换到真实麦克风重开流
// 注意:ideal 约束在设备打不开时会静默回退到系统默认(本机默认是静音虚拟麦),
// 所以不管用户是否手选,只要抓到虚拟麦就必须换
const ensureUsableMic = async (s) => {
  try {
    await loadMicDevices()
    const track = s.getAudioTracks()[0]
    const settings = track && track.getSettings ? track.getSettings() : {}
    if (settings.deviceId && micDevices.value.some(d => d.deviceId === settings.deviceId)) {
      selectedMic.value = settings.deviceId
    }
    const cur = micDevices.value.find(d => d.deviceId === settings.deviceId)
    if (cur && isVirtualMic(cur)) {
      const real = pickRealMic()
      if (real) {
        s.getTracks().forEach(t => t.stop())
        try {
          s = await navigator.mediaDevices.getUserMedia({ audio: { deviceId: { exact: real.deviceId } } })
          selectedMic.value = real.deviceId
          persistSelectedMic(real.deviceId)
          ElMessage.warning('系统默认麦克风是虚拟设备,录不到声音;已自动切换到真实麦克风:' + real.label)
        } catch (e2) {
          // 切换失败时回退到默认设备,保证录音能开始
          s = await navigator.mediaDevices.getUserMedia({ audio: true })
        }
      }
    }
  } catch (e) {
    // 检查失败不影响正常录音
  }
  return s
}

const stopRecord = () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    if (!stopRequested) {
      stopRequested = true
      try {
        mediaRecorder.stop() // onstop 里完成 blob/回放/资源释放
      } catch (e) {
        stopRequested = false
        releaseMic() // stop 失败(设备已断开等),直接释放
      }
    }
  } else if (recordChunks.length) {
    // 录音器已自动失效但 onstop 未走完的边界情况,手动收尾已录到的内容
    recordBlob = new Blob(recordChunks, { type: lastMime })
    recordChunks = []
    recordUrl.value = URL.createObjectURL(recordBlob)
    playState.value = ''
    stopWave()
    releaseMic()
    blobToWavFile(recordBlob).then(f => {
      submitFile.value = f
      ElMessage.success('录音完成,已转换为 WAV 格式')
    }).catch(() => {
      ElMessage.warning('录音转码失败,请改用手动上传音频文件')
    })
  }
  recording.value = false
  micLiveLevel.value = 0
  activeMicLabel.value = ''
  if (recordTimer) {
    clearInterval(recordTimer)
    recordTimer = null
  }
}

const stopWave = () => {
  if (animationId) {
    cancelAnimationFrame(animationId)
    animationId = null
  }
  const canvas = waveCanvas.value
  if (canvas) {
    const ctx = canvas.getContext('2d')
    ctx.clearRect(0, 0, canvas.width, canvas.height)
  }
}

// 强制释放录音占用的全部资源(录音器/媒体流/AudioContext/定时器/波形)。
// 重录前必调:旧流不释放,二次 getUserMedia 会打不开设备或静默回退到静音的默认虚拟麦
const releaseMic = () => {
  if (recordTimer) {
    clearInterval(recordTimer)
    recordTimer = null
  }
  if (mediaRecorder) {
    mediaRecorder.onstop = null
    mediaRecorder.ondataavailable = null
    try {
      if (mediaRecorder.state !== 'inactive') mediaRecorder.stop()
    } catch (e) { /* 录音器已失效 */ }
    mediaRecorder = null
  }
  if (stream) {
    stream.getTracks().forEach(t => { try { t.stop() } catch (e) { /* 已断开 */ } })
    stream = null
  }
  if (audioContext) {
    audioContext.close().catch(() => {})
    audioContext = null
  }
  analyser = null
  stopWave()
}

const togglePlayback = () => {
  if (!recordUrl.value) return
  if (playState.value === 'playing') {
    if (playbackAudio) playbackAudio.pause()
    playState.value = ''
    return
  }
  if (!playbackAudio) {
    playbackAudio = new Audio(recordUrl.value)
    playbackAudio.onended = () => { playState.value = '' }
  }
  playbackAudio.currentTime = 0
  playbackAudio.play()
  playState.value = 'playing'
}

// ============ 听力型题目:先听录音再作答(提交前不展示原文) ============
// 模仿朗读/故事复述均为听力型:英文正文以录音播放,原文仅在提交后展示(与高考听说考试形式一致)
// 角色扮演需要学生看情景文本,保持直接展示
const isListeningType = computed(() =>
  !!currentQuestion.value && ['模仿朗读', '故事复述'].includes(currentQuestion.value.questionType)
)

// 提交前只展示指令部分,英文正文一律不展示
const listeningInstruction = computed(() => {
  const q = currentQuestion.value
  if (!q) return ''
  const refText = (q.referenceText || '').trim()
  if (refText && q.content && q.content.includes(refText)) {
    const head = q.content.split(refText)[0].trim()
    if (head) return head
  }
  const firstPara = (q.content || '').split(/\n\s*\n/)[0].trim()
  return firstPara || '请听故事录音,然后用自己的话复述故事内容。'
})

// TTS 朗读:服务端 SAPI 生成英语音频(不依赖浏览器语音包,任何浏览器都能播)
const ttsSpeaking = ref(false)
let ttsAudio = null
let ttsAudioUrl = ''
const speakStory = () => {
  const q = currentQuestion.value
  if (!q || !q.id) return
  // 正在播放时点击 = 暂停
  if (ttsSpeaking.value) {
    if (ttsAudio) { ttsAudio.pause(); ttsAudio.currentTime = 0 }
    ttsSpeaking.value = false
    return
  }
  ElMessage.info('正在生成朗读音频,请稍候...')
  lsTtsAudio(q.id).then(res => {
    const url = res && res.data
    if (!url) return
    if (!ttsAudio || ttsAudioUrl !== url) {
      ttsAudio = new Audio(url)
      ttsAudioUrl = url
      ttsAudio.onended = () => { ttsSpeaking.value = false }
      ttsAudio.onerror = () => {
        ttsSpeaking.value = false
        ElMessage.error('音频播放失败')
      }
    }
    ttsSpeaking.value = true
    ttsAudio.play().catch(() => {
      ttsSpeaking.value = false
      ElMessage.error('音频播放失败,请检查系统播放设备(右下角喇叭图标里设置默认设备)')
    })
  }).catch(() => { /* 请求拦截器已提示错误 */ })
}

// ============ 参考音频 ============
const refPlaying = ref(false)
let refAudio = null
const toggleRefAudio = () => {
  if (!currentQuestion.value || !currentQuestion.value.referenceAudio) return
  if (refPlaying.value) {
    if (refAudio) refAudio.pause()
    refPlaying.value = false
    return
  }
  if (!refAudio) {
    refAudio = new Audio(currentQuestion.value.referenceAudio)
    refAudio.onended = () => { refPlaying.value = false }
  }
  refAudio.play()
  refPlaying.value = true
}

// ============ 文件上传 ============
const onFileChange = (file, files) => {
  const f = file.raw
  if (!f) return
  const sizeMB = f.size / 1024 / 1024
  if (sizeMB > 10) {
    ElMessage.error('音频文件不能超过 10MB')
    fileList.value = []
    submitFile.value = null
    return
  }
  const name = (f.name || '').toLowerCase()
  if (!/\.(mp3|wav|m4a)$/.test(name)) {
    ElMessage.error('仅支持 MP3/WAV/M4A 格式')
    fileList.value = []
    submitFile.value = null
    return
  }
  submitFile.value = f
  ElMessage.success('已选择音频文件:' + f.name)
}
const onFileRemove = () => {
  fileList.value = []
  submitFile.value = null
}
const onFileExceed = () => {
  ElMessage.warning('仅支持上传一个音频文件')
}

// ============ 提交 ============
const supplementText = ref('')
const submitting = ref(false)
const result = ref(null)
const canSubmit = computed(() => !!submitFile.value && !!currentQuestion.value && !submitting.value)

const resetAnswer = () => {
  // 切换题目:放弃进行中的录音(直接释放,不收尾成回放,避免 onstop 异步覆盖下面的重置)
  releaseMic()
  recordingSeq++
  recording.value = false
  activeMicLabel.value = ''
  if (playbackAudio) {
    playbackAudio.pause()
    playbackAudio = null
  }
  recordUrl.value = ''
  recordSeconds.value = 0
  playState.value = ''
  submitFile.value = null
  fileList.value = []
  supplementText.value = ''
  result.value = null
}

const handleSubmit = async () => {
  if (!submitFile.value) {
    ElMessage.warning('请先录音或上传音频文件')
    return
  }
  submitting.value = true
  try {
    const res = await submitListeningSpeaking(currentQuestion.value.id, submitFile.value, supplementText.value)
    result.value = res.data
    ElMessage.success('批改完成')
    await loadRecords()
    await nextTick()
    drawRadar()
  } catch (e) {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

// ============ 评分报告图表 ============
const radarChart = ref(null)
let radarInstance = null
const drawRadar = () => {
  if (!radarChart.value || !result.value) return
  // 报告卡片经 v-if 移除重建后,旧实例仍挂在已移除的 DOM 节点上,需重新初始化
  if (!radarInstance || radarInstance.getDom() !== radarChart.value) {
    if (radarInstance) { try { radarInstance.dispose() } catch (e) { /* 已销毁 */ } }
    radarInstance = echarts.init(radarChart.value)
  }
  radarInstance.setOption({
    tooltip: {},
    radar: {
      indicator: [
        { name: '发音', max: 25 },
        { name: '流利度', max: 25 },
        { name: '语法', max: 25 },
        { name: '内容', max: 25 }
      ],
      radius: '65%'
    },
    series: [{
      type: 'radar',
      itemStyle: { color: '#FF6900' },
      data: [{
        value: [result.value.pronunciationScore, result.value.fluencyScore, result.value.grammarScore, result.value.contentScore],
        areaStyle: { opacity: 0.35 },
        lineStyle: { width: 2 }
      }]
    }]
  })
}

// ============ 历史记录 ============
const records = ref([])
const trendChart = ref(null)
let trendInstance = null

const loadRecords = async () => {
  const res = await listeningSpeakingRecords()
  records.value = res.data || []
  await nextTick()
  drawTrend()
}

const drawTrend = () => {
  if (!trendChart.value || records.value.length < 2) return
  if (!trendInstance) trendInstance = echarts.init(trendChart.value)
  const items = [...records.value].reverse()
  trendInstance.setOption({
    tooltip: { trigger: 'axis' },
    grid: { left: 40, right: 20, top: 20, bottom: 30 },
    xAxis: {
      type: 'category',
      data: items.map(r => (r.createTime || '').slice(5, 16))
    },
    yAxis: { type: 'value', min: 0, max: 100 },
    series: [{
      name: '总分',
      type: 'line',
      smooth: true,
      itemStyle: { color: '#FF6900' },
      data: items.map(r => r.totalScore),
      areaStyle: { opacity: 0.15 }
    }]
  })
}

const scoreClass = (s) => {
  if (s >= 85) return 'score-good'
  if (s >= 60) return 'score-mid'
  return 'score-bad'
}

const playRecordAudio = (path) => {
  const audio = new Audio(path)
  audio.play()
}

const formatDuration = (sec) => {
  const m = Math.floor(sec / 60).toString().padStart(2, '0')
  const s = (sec % 60).toString().padStart(2, '0')
  return `${m}:${s}`
}

// ============ 生命周期 ============
onMounted(() => {
  loadQuestions()
  loadRecords()
  loadMicDevices()
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  releaseMic()
  if (playbackAudio) playbackAudio.pause()
  if (refAudio) refAudio.pause()
  if ('speechSynthesis' in window) window.speechSynthesis.cancel()
  if (radarInstance) radarInstance.dispose()
  if (trendInstance) trendInstance.dispose()
  window.removeEventListener('resize', handleResize)
})
const handleResize = () => {
  if (radarInstance) radarInstance.resize()
  if (trendInstance) trendInstance.resize()
}
</script>

<style scoped>
/* 页面整体 */
.listening-speaking {
  height: 100vh;
  padding: 12px 16px;
  background: #f5f7fb;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  box-sizing: border-box;
}

/* 页面头部 */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  background: #fff;
  padding: 12px 18px;
  border-radius: 10px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  flex-shrink: 0;
}

.header-left h1 {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0 0 2px 0;
}

.header-left p {
  font-size: 11px;
  color: #8b8fa3;
  margin: 0;
  letter-spacing: 1px;
}

.header-actions {
  display: flex;
  gap: 10px;
}

/* 主体：两栏布局 */
.main-content {
  display: grid;
  grid-template-columns: 250px 1fr;
  gap: 12px;
  flex: 1;
  min-height: 0;
  overflow: hidden;
}

/* 左侧面板 */
.left-panel {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  min-height: 0;
}

.left-panel .panel-card {
  display: flex;
  flex-direction: column;
  overflow: hidden;
  flex: 1;
}

/* 面板卡片 */
.panel-card {
  background: #fff;
  border-radius: 10px;
  padding: 14px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  border: 1px solid #E8ECF1;
}

.panel-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 10px;
  border-bottom: 1px solid #f0f2f5;
}

.panel-header h2 {
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0;
}

.question-count {
  font-size: 12px;
  color: #409eff;
  font-weight: 600;
  background: #ecf5ff;
  padding: 2px 10px;
  border-radius: 10px;
}

/* 筛选区 */
.filter-section {
  margin-bottom: 10px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.filter-label {
  font-size: 12px;
  color: #8b8fa3;
  font-weight: 500;
  min-width: 32px;
  flex-shrink: 0;
}

/* 题目列表 */
.question-list {
  flex: 1;
  overflow-y: auto;
  padding-right: 4px;
  min-height: 0;
}

.question-list::-webkit-scrollbar {
  width: 4px;
}

.question-list::-webkit-scrollbar-thumb {
  background: #E8ECF1;
  border-radius: 2px;
}

.question-item {
  padding: 10px 12px;
  border: 1px solid #E8ECF1;
  border-radius: 8px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: #fff;
}

.question-item:hover {
  border-color: #409eff;
  background: #E8ECF1;
}

.question-item.active {
  background: #409eff;
  border-color: #409eff;
  color: #fff;
}

.question-item.active .question-title {
  color: #fff;
}

.question-item.active .tag-type {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.question-item.active .tag-diff-1,
.question-item.active .tag-diff-2,
.question-item.active .tag-diff-3 {
  background: rgba(255, 255, 255, 0.25);
  color: #fff;
}

.question-title {
  font-size: 13px;
  font-weight: 500;
  color: #303133;
  margin-bottom: 6px;
  line-height: 1.4;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.meta-tags {
  display: flex;
  gap: 4px;
  flex-wrap: wrap;
}

.tag {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 4px;
  font-weight: 500;
}

.tag-type {
  background: #ecf5ff;
  color: #409eff;
}

.tag-topic {
  background: #fdf6ec;
  color: #e6a23c;
}

.tag-diff-1 {
  background: #ECF5FF;
  color: #409EFF;
}

.tag-diff-2 {
  background: #fdf6ec;
  color: #e6a23c;
}

.tag-diff-3 {
  background: #fef0f0;
  color: #f56c6c;
}

/* 右侧区域 */
.right-area {
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  min-height: 0;
  padding-right: 4px;
}

.right-area::-webkit-scrollbar {
  width: 5px;
}

.right-area::-webkit-scrollbar-thumb {
  background: #E8ECF1;
  border-radius: 3px;
}

/* 作答区 */
.answer-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.question-header {
  margin-bottom: 14px;
}

.question-header h2 {
  font-size: 18px;
  font-weight: 600;
  color: #1a1a2e;
  margin: 0 0 8px 0;
}

.question-tags {
  display: flex;
  gap: 8px;
}

.question-content-card {
  background: #f8f9fb;
  border: 1px solid #E8ECF1;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
}

.content-label {
  font-size: 11px;
  color: #8b8fa3;
  font-weight: 600;
  margin-bottom: 6px;
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.content-text {
  font-size: 14px;
  line-height: 1.7;
  color: #303133;
  white-space: pre-wrap;
}

/* 参考区 */
.reference-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.ref-audio {
  flex: 1;
}

.ref-text-collapse {
  margin-bottom: 14px;
}

.tts-hint {
  font-size: 12px;
  color: #e6a23c;
  margin-left: 10px;
}

.ref-text-content {
  font-size: 13px;
  line-height: 1.7;
  color: #606266;
  white-space: pre-wrap;
  margin: 0;
}

/* 录音区 */
.record-section {
  border: 1px solid #E8ECF1;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
  background: #fafbfc;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 10px;
}

.section-icon {
  font-size: 18px;
}

.section-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
  margin: 0;
}

.mic-device-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.mic-device-label {
  font-size: 12px;
  color: #8b8fa3;
  flex-shrink: 0;
}

.mic-select {
  width: 320px;
  max-width: 100%;
}

.mic-level {
  font-size: 12px;
  color: #409eff;
  font-variant-numeric: tabular-nums;
  margin-left: auto;
}

.mic-level.silent {
  color: #f56c6c;
}

.mic-active-device {
  font-size: 12px;
  color: #8b8fa3;
  max-width: 360px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.record-controls {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 10px;
}

.record-btn {
  font-weight: 600;
}

.record-btn.recording {
  animation: pulse 1.5s infinite;
}

@keyframes pulse {
  0%, 100% { box-shadow: 0 0 0 0 rgba(245, 108, 108, 0.4); }
  50% { box-shadow: 0 0 0 8px rgba(245, 108, 108, 0); }
}

.record-timer {
  font-size: 22px;
  font-weight: 700;
  color: #f56c6c;
  font-variant-numeric: tabular-nums;
  margin-left: auto;
}

.waveform-container {
  margin-bottom: 8px;
}

.wave-canvas {
  width: 100%;
  height: 50px;
  background: #E8ECF1;
  border-radius: 6px;
  border: 1px solid #E8ECF1;
}

.wave-canvas.active {
  border-color: #f56c6c;
}

.record-hint {
  font-size: 12px;
  color: #8b8fa3;
  text-align: center;
}

/* 上传区 */
.upload-section {
  border: 1px solid #E8ECF1;
  border-radius: 8px;
  padding: 14px;
  margin-bottom: 14px;
  background: #fafbfc;
}

.audio-upload {
  margin-top: 8px;
}

.upload-icon {
  font-size: 32px;
  color: #8b8fa3;
  margin-bottom: 6px;
}

.upload-text {
  text-align: center;
}

.upload-main {
  font-size: 13px;
  color: #606266;
  margin: 0 0 4px 0;
}

.upload-main em {
  color: #409eff;
  font-style: normal;
  font-weight: 600;
}

.upload-tip {
  font-size: 12px;
  color: #8b8fa3;
  margin: 0;
}

/* 提交区 */
.submit-section {
  display: flex;
  gap: 12px;
  align-items: center;
}

.supplement-input {
  flex: 1;
}

.submit-btn {
  min-width: 110px;
  font-weight: 600;
}

/* 空状态占位 */
.answer-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 200px;
}

.score-good { color: #409EFF; font-weight: 600; }
.score-mid { color: #e6a23c; font-weight: 600; }
.score-bad { color: #f56c6c; font-weight: 600; }

/* 响应式 */
@media (max-width: 1200px) {
  .main-content {
    grid-template-columns: 240px 1fr;
    gap: 14px;
  }
}

@media (max-width: 900px) {
  .main-content {
    grid-template-columns: 1fr;
  }
}
</style>
