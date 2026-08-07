<template>
  <div class="listening-speaking">
    <!-- 顶部:题目列表 + 学段过滤 -->
    <el-card shadow="never" class="q-card">
      <div class="q-header">
        <div class="q-title">高考英语听说练习</div>
        <div class="q-filter">
          <span class="filter-label">学段</span>
          <el-select v-model="gradeLevel" size="small" style="width: 110px" @change="loadQuestions">
            <el-option label="全部" :value="0" />
            <el-option label="小学" :value="1" />
            <el-option label="初中" :value="2" />
            <el-option label="高中" :value="3" />
          </el-select>
        </div>
      </div>
      <div class="q-list">
        <div
          v-for="q in questions"
          :key="q.id"
          class="q-item"
          :class="{ active: currentQuestion && currentQuestion.id === q.id }"
          @click="selectQuestion(q)"
        >
          <div class="q-item-title">{{ q.title }}</div>
          <div class="q-item-tags">
            <el-tag size="small" type="info" effect="plain">{{ typeLabel(q.questionType) }}</el-tag>
            <el-tag size="small" :type="diffType(q.difficulty)" effect="plain">{{ diffLabel(q.difficulty) }}</el-tag>
          </div>
        </div>
        <el-empty v-if="!questions.length" description="暂无题目" :image-size="60" />
      </div>
    </el-card>

    <!-- 中间:题目详情 + 作答区 -->
    <el-card shadow="never" class="main-card" v-loading="submitting">
      <template v-if="currentQuestion">
        <div class="section-title">
          <span>{{ currentQuestion.title }}</span>
          <div>
            <el-tag size="small" type="info" effect="plain">{{ typeLabel(currentQuestion.questionType) }}</el-tag>
            <el-tag size="small" :type="diffType(currentQuestion.difficulty)" effect="plain">{{ diffLabel(currentQuestion.difficulty) }}</el-tag>
          </div>
        </div>
        <pre class="question-content">{{ currentQuestion.content }}</pre>

        <!-- 参考音频 -->
        <div v-if="currentQuestion.referenceAudio" class="ref-audio">
          <el-button size="small" type="primary" plain :icon="refPlaying ? 'VideoPause' : 'VideoPlay'" @click="toggleRefAudio">
            {{ refPlaying ? '暂停参考音频' : '播放参考音频' }}
          </el-button>
        </div>
        <div v-else class="ref-audio">
          <el-button size="small" type="primary" plain icon="VideoPlay" disabled>暂无参考音频</el-button>
        </div>

        <!-- 参考文本(可展开) -->
        <div v-if="currentQuestion.referenceText" class="ref-text">
          <el-collapse>
            <el-collapse-item title="查看参考文本" name="ref">
              <pre>{{ currentQuestion.referenceText }}</pre>
            </el-collapse-item>
          </el-collapse>
        </div>

        <!-- 录音区 -->
        <div class="rec-card">
          <div class="rec-title">① 在线录音作答</div>
          <div class="rec-toolbar">
            <el-button v-if="!recording" type="danger" :icon="'Microphone'" @click="startRecord">开始录音</el-button>
            <el-button v-else type="danger" :icon="'VideoPause'" @click="stopRecord">停止录音</el-button>
            <el-button v-if="recordUrl" type="success" plain :icon="playState === 'playing' ? 'VideoPause' : 'VideoPlay'" @click="togglePlayback">
              {{ playState === 'playing' ? '暂停回放' : '播放回放' }}
            </el-button>
            <span class="rec-time">{{ formatDuration(recordSeconds) }}</span>
          </div>
          <canvas ref="waveCanvas" class="wave-canvas" :class="{ active: recording }"></canvas>
          <div class="rec-hint">{{ recording ? '正在录音,请朗读题目内容...' : (recordUrl ? '录音完成,可播放回放或直接提交' : '点击「开始录音」使用麦克风朗读') }}</div>
        </div>

        <!-- 上传区 -->
        <div class="upload-card">
          <div class="rec-title">② 上传音频文件(MP3/WAV/M4A,≤10MB)</div>
          <el-upload
            drag
            :auto-upload="false"
            accept=".mp3,.wav,.m4a"
            :limit="1"
            :on-change="onFileChange"
            :on-remove="onFileRemove"
            :on-exceed="onFileExceed"
            :file-list="fileList"
          >
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽音频文件到此处,或 <em>点击选择</em></div>
            <template #tip>
              <div class="el-upload__tip">仅支持 MP3 / WAV / M4A 格式,文件大小不超过 10MB;上传后自动覆盖录音</div>
            </template>
          </el-upload>
        </div>

        <!-- 文字补充 + 提交 -->
        <div class="submit-row">
          <el-input
            v-model="supplementText"
            placeholder="文字补充(可选):如朗读要点、回答内容等"
            clearable
            style="flex: 1"
          />
          <el-button type="primary" size="large" :loading="submitting" :disabled="!canSubmit" @click="handleSubmit">
            提交批改
          </el-button>
        </div>
      </template>
      <el-empty v-else description="请选择一道题目开始练习" />
    </el-card>

    <!-- 右侧:评分报告 + 历史 -->
    <el-card shadow="never" class="side-card">
      <template v-if="result">
        <div class="section-title"><span>AI 评分报告</span></div>
        <div class="score-total">
          <div class="score-num">{{ result.totalScore }}</div>
          <div class="score-label">总分 / 100</div>
        </div>
        <div ref="radarChart" class="radar-chart"></div>
        <div class="score-grid">
          <div class="score-item"><span class="s-label">发音</span><span class="s-val">{{ result.pronunciationScore }}</span></div>
          <div class="score-item"><span class="s-label">流利度</span><span class="s-val">{{ result.fluencyScore }}</span></div>
          <div class="score-item"><span class="s-label">语法</span><span class="s-val">{{ result.grammarScore }}</span></div>
          <div class="score-item"><span class="s-label">内容</span><span class="s-val">{{ result.contentScore }}</span></div>
        </div>
        <div class="ai-text-block">
          <div class="ai-text-title">AI 识别文本</div>
          <div class="ai-text-body">{{ result.recognizedText || '未识别到有效语音' }}</div>
        </div>
        <div class="ai-text-block">
          <div class="ai-text-title">改进建议</div>
          <div class="ai-text-body feedback">{{ result.aiFeedback }}</div>
        </div>
      </template>
      <el-empty v-else description="提交后展示评分报告" :image-size="60" />
    </el-card>

    <!-- 历史记录 -->
    <el-card shadow="never" class="history-card">
      <div class="section-title"><span>练习历史与评分趋势</span></div>
      <div v-if="records.length" ref="trendChart" class="trend-chart"></div>
      <el-table :data="records" size="small" stripe>
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
        <el-table-column label="操作" width="80" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.audioPath" link type="primary" size="small" @click="playRecordAudio(row.audioPath)">听录音</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import * as echarts from 'echarts'
import { listeningSpeakingList, listeningSpeakingDetail, submitListeningSpeaking, listeningSpeakingRecords } from '@/api'

// ============ 题目 ============
const questions = ref([])
const currentQuestion = ref(null)
const gradeLevel = ref(0)

const typeLabel = (t) => t || '模仿朗读'
const diffLabel = (d) => ({ 1: '简单', 2: '中等', 3: '困难' })[d] || '中等'
const diffType = (d) => ({ 1: 'success', 2: 'warning', 3: 'danger' })[d] || 'warning'

const loadQuestions = async () => {
  const res = await listeningSpeakingList(gradeLevel.value || undefined)
  questions.value = res.data || []
  if (questions.value.length) {
    selectQuestion(questions.value[0])
  } else {
    currentQuestion.value = null
  }
}

const selectQuestion = async (q) => {
  const res = await listeningSpeakingDetail(q.id)
  currentQuestion.value = res.data
  resetAnswer()
}

// ============ 录音(MediaRecorder + Web Audio) ============
const recording = ref(false)
const recordSeconds = ref(0)
const recordUrl = ref('')
const playState = ref('')
const waveCanvas = ref(null)
const submitFile = ref(null) // 待提交的音频文件(录音转 WAV 或上传的文件)
const fileList = ref([])

let mediaRecorder = null
let stream = null
let audioContext = null
let analyser = null
let animationId = null
let recordChunks = []
let recordTimer = null
let recordBlob = null
let recordStartTime = 0
let playbackAudio = null

// 录音期间用 AnalyserNode 画波形
const drawWave = () => {
  const canvas = waveCanvas.value
  if (!canvas || !analyser) return
  const ctx = canvas.getContext('2d')
  const w = canvas.width
  const h = canvas.height
  const data = new Uint8Array(analyser.fftSize)
  analyser.getByteTimeDomainData(data)
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

// 停止后把录音 blob 解码并转为 WAV(16bit PCM),兼容 DashScope 语音识别
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

  // 混音到单声道/双声道,16bit PCM
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

const startRecord = async () => {
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia || !window.MediaRecorder) {
    ElMessage.error('当前浏览器不支持录音,请使用 Chrome/Edge 或改用上传音频文件')
    return
  }
  try {
    stream = await navigator.mediaDevices.getUserMedia({ audio: true })
    // 优先 webm/opus,其次 mp4(Safari)
    const mimeTypes = ['audio/webm;codecs=opus', 'audio/webm', 'audio/mp4']
    const mimeType = mimeTypes.find(t => MediaRecorder.isTypeSupported(t)) || ''
    mediaRecorder = mimeType ? new MediaRecorder(stream, { mimeType }) : new MediaRecorder(stream)

    // 波形可视化
    audioContext = new (window.AudioContext || window.webkitAudioContext)()
    const source = audioContext.createMediaStreamSource(stream)
    analyser = audioContext.createAnalyser()
    analyser.fftSize = 256
    source.connect(analyser)

    recordChunks = []
    mediaRecorder.ondataavailable = (e) => {
      if (e.data && e.data.size > 0) recordChunks.push(e.data)
    }
    mediaRecorder.onstop = async () => {
      recordBlob = new Blob(recordChunks, { type: mimeType || 'audio/webm' })
      recordUrl.value = URL.createObjectURL(recordBlob)
      playState.value = ''
      stopWave()
      cleanupStream()
      // 转为 WAV 便于 AI 识别
      try {
        submitFile.value = await blobToWavFile(recordBlob)
        ElMessage.success('录音完成,已转换为 WAV 格式')
      } catch (e) {
        ElMessage.warning('录音转码失败,请改用手动上传音频文件')
        submitFile.value = null
      }
    }
    mediaRecorder.start()
    recording.value = true
    recordStartTime = Date.now()
    recordTimer = setInterval(() => {
      recordSeconds.value = Math.floor((Date.now() - recordStartTime) / 1000)
    }, 200)
    nextTick(drawWave)
  } catch (e) {
    ElMessage.error('无法访问麦克风:' + (e.message || e))
  }
}

const stopRecord = () => {
  if (mediaRecorder && mediaRecorder.state !== 'inactive') {
    mediaRecorder.stop()
  }
  recording.value = false
  if (recordTimer) {
    clearInterval(recordTimer)
    recordTimer = null
  }
}

const cleanupStream = () => {
  if (stream) {
    stream.getTracks().forEach(t => t.stop())
    stream = null
  }
  if (audioContext) {
    audioContext.close().catch(() => {})
    audioContext = null
  }
  analyser = null
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
  stopRecord()
  stopWave()
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
  if (!radarInstance) radarInstance = echarts.init(radarChart.value)
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
  window.addEventListener('resize', handleResize)
})
onBeforeUnmount(() => {
  stopRecord()
  stopWave()
  cleanupStream()
  if (playbackAudio) playbackAudio.pause()
  if (refAudio) refAudio.pause()
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
.listening-speaking {
  display: grid;
  grid-template-columns: 300px 1fr 330px;
  grid-template-rows: auto auto;
  gap: 16px;
  align-items: start;
}
.q-card { grid-column: 1; grid-row: 1 / span 2; }
.main-card { grid-column: 2; grid-row: 1 / span 2; }
.side-card { grid-column: 3; grid-row: 1; }
.history-card { grid-column: 3; grid-row: 2; }

.q-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; }
.q-title { font-size: 16px; font-weight: 600; }
.q-filter { display: flex; align-items: center; gap: 6px; }
.filter-label { font-size: 12px; color: #909399; }
.q-list { max-height: 620px; overflow-y: auto; }
.q-item { padding: 10px 12px; border: 1px solid #ebeef5; border-radius: 6px; margin-bottom: 8px; cursor: pointer; transition: all 0.2s; }
.q-item:hover { border-color: #409eff; }
.q-item.active { border-color: #409eff; background: #ecf5ff; }
.q-item-title { font-size: 14px; font-weight: 500; margin-bottom: 6px; }
.q-item-tags { display: flex; gap: 6px; }

.section-title { display: flex; justify-content: space-between; align-items: center; font-size: 15px; font-weight: 600; margin-bottom: 12px; }
.question-content { white-space: pre-wrap; font-family: inherit; font-size: 14px; line-height: 1.8; background: #f8f9fb; border: 1px solid #ebeef5; border-radius: 6px; padding: 14px; margin: 0 0 12px; }
.ref-audio { margin-bottom: 12px; }
.ref-text { margin-bottom: 16px; }

.rec-card, .upload-card { border: 1px solid #ebeef5; border-radius: 6px; padding: 14px; margin-bottom: 16px; }
.rec-title { font-size: 14px; font-weight: 600; margin-bottom: 10px; color: #303133; }
.rec-toolbar { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.rec-time { font-size: 18px; font-weight: 600; color: #409eff; font-variant-numeric: tabular-nums; }
.wave-canvas { width: 100%; height: 60px; background: #f5f7fa; border-radius: 4px; }
.rec-hint { font-size: 12px; color: #909399; margin-top: 6px; }

.submit-row { display: flex; gap: 12px; }

.score-total { text-align: center; padding: 10px 0 4px; }
.score-num { font-size: 42px; font-weight: 700; color: #409eff; line-height: 1.1; }
.score-label { font-size: 13px; color: #909399; }
.radar-chart { width: 100%; height: 220px; }
.score-grid { display: grid; grid-template-columns: repeat(4, 1fr); gap: 8px; margin: 10px 0 14px; }
.score-item { text-align: center; background: #f5f7fa; border-radius: 6px; padding: 8px 4px; }
.s-label { display: block; font-size: 12px; color: #909399; margin-bottom: 4px; }
.s-val { font-size: 18px; font-weight: 600; color: #303133; }
.ai-text-block { border: 1px solid #ebeef5; border-radius: 6px; padding: 10px 12px; margin-bottom: 10px; }
.ai-text-title { font-size: 13px; font-weight: 600; color: #409eff; margin-bottom: 6px; }
.ai-text-body { font-size: 13px; color: #606266; line-height: 1.7; white-space: pre-wrap; }
.ai-text-body.feedback { color: #303133; }

.trend-chart { width: 100%; height: 150px; margin-bottom: 10px; }
.score-good { color: #67c23a; font-weight: 600; }
.score-mid { color: #e6a23c; font-weight: 600; }
.score-bad { color: #f56c6c; font-weight: 600; }

@media (max-width: 1400px) {
  .listening-speaking { grid-template-columns: 1fr; }
  .q-card, .main-card, .side-card, .history-card { grid-column: 1; grid-row: auto; }
}
</style>
