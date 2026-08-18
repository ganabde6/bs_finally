<template>
  <div class="errorbook-page">
    <!-- ========== 页面标题 ========== -->
    <div class="page-header">
      <div class="header-left">
        <div class="header-badge">
          <el-icon :size="22"><EditPen /></el-icon>
        </div>
        <div>
          <h1 class="page-title">智能错题本</h1>
          <p class="page-subtitle">查漏补缺 · 精准提升 · 高效复盘</p>
        </div>
      </div>
      <div class="header-stats">
        <span class="header-stat-item">错题总量 <strong>{{ errors.length }}</strong></span>
        <span class="header-stat-item">待复盘 <strong>{{ unreviewedCount }}</strong></span>
        <span class="header-stat-item">已复盘 <strong>{{ reviewedCount }}</strong></span>
        <span class="header-stat-item">已掌握 <strong>{{ masteredCount }}</strong></span>
      </div>
      <div class="header-right">
        <el-button v-if="viewMode === 'list'" class="btn-back" @click="viewMode = 'cards'">
          <el-icon><ArrowLeft /></el-icon> 返回概览
        </el-button>
      </div>
    </div>

    <!-- ========== 科目卡片首页 ========== -->
    <div v-if="viewMode === 'cards'" class="cards-view">
      <!-- 全部错题 + 科目卡片统一网格 -->
      <div class="subject-grid">
        <!-- 全部错题卡片 -->
        <div class="subject-card card-all" @click="enterList(null)">
          <div class="sc-top">
            <div class="sc-icon-wrap sc-icon-all">
              <span class="sc-icon"><el-icon :size="22"><Notebook /></el-icon></span>
            </div>
            <div v-if="unreviewedCount > 0" class="sc-badge">{{ unreviewedCount }}</div>
          </div>
          <h3 class="sc-name">全部错题</h3>
          <div class="sc-stats-row sc-stats-all">
            <div class="sc-stat">
              <span class="sc-stat-val">{{ errors.length }}</span>
              <span class="sc-stat-lbl">总错题</span>
            </div>
            <div class="sc-stat sc-stat-warn">
              <span class="sc-stat-val">{{ unreviewedCount }}</span>
              <span class="sc-stat-lbl">待复盘</span>
            </div>
          </div>
          <div class="sc-progress-wrap">
            <div class="sc-progress"><div class="sc-progress-bar" :style="{ width: allMasteryRate + '%' }"></div></div>
          </div>
          <div class="sc-summary">总错题 {{ masteredCount }}/{{ errors.length }}</div>
        </div>

        <!-- 各科目卡片 -->
        <div
          v-for="item in subjectCards"
          :key="item.subjectId"
          class="subject-card"
          @click="enterList(item.subjectId)"
          :style="{ '--theme-color': item.themeColor, '--theme-bg': item.themeBg }"
        >
          <div class="sc-top">
            <div class="sc-icon-wrap" :style="{ background: item.themeBg, color: item.themeColor }">
              <span class="sc-icon"><el-icon :size="22"><component :is="item.icon" /></el-icon></span>
            </div>
            <div v-if="item.unreviewed > 0" class="sc-badge">{{ item.unreviewed }}</div>
          </div>
          <h3 class="sc-name" :style="{ color: item.themeColor }">{{ item.subjectName }}</h3>
          <div class="sc-stats-row">
            <div class="sc-stat">
              <span class="sc-stat-val">{{ item.total }}</span>
              <span class="sc-stat-lbl">总错题</span>
            </div>
            <div class="sc-stat sc-stat-warn">
              <span class="sc-stat-val">{{ item.unreviewed }}</span>
              <span class="sc-stat-lbl">待复盘</span>
            </div>
          </div>
          <div class="sc-progress-wrap">
            <div class="sc-progress">
              <div class="sc-progress-bar" :style="{ width: item.masteryRate + '%', background: item.themeColor }"></div>
            </div>
          </div>
          <div class="sc-summary" v-if="item.topType">
            高频错误：{{ errorTypeText(item.topType.type) }}（{{ item.topType.count }}次）
          </div>
          <div class="sc-summary" v-else>暂无错题记录</div>
        </div>
      </div>

      <!-- 空状态 -->
      <div v-if="errors.length === 0" class="empty-state">
        <div class="empty-illustration">
          <div class="empty-circle">
            <span class="empty-icon"></span>
          </div>
        </div>
        <h3>暂无错题记录</h3>
        <p>继续保持，你很棒！</p>
      </div>
    </div>

    <!-- ========== 错题列表 ========== -->
    <div v-else class="list-view">
      <!-- 筛选栏 -->
      <div class="filter-bar">
        <div class="filter-search">
          <el-input v-model="filterKeyword" placeholder="搜索题目或知识点..." clearable class="filter-input">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
        </div>
        <div class="filter-right">
          <el-select v-model="filterSubject" placeholder="全部科目" clearable class="filter-select" @change="applyFilters">
            <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
          </el-select>
          <el-date-picker v-model="filterDateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
            class="filter-date" @change="applyFilters" />
          <el-button v-if="filterKeyword || filterSubject || filterDateRange?.length" @click="clearFilters" class="filter-clear">
            <el-icon><Close /></el-icon> 清除
          </el-button>
        </div>
      </div>

      <!-- Tab 切换 -->
      <div class="tab-bar">
        <button :class="['tab-btn', { active: activeTab === 'errors' }]" @click="activeTab = 'errors'">
          <span class="tab-icon"><el-icon><Tickets /></el-icon></span>
          错题列表
          <span class="tab-count">{{ filteredErrors.length }}</span>
        </button>
        <button :class="['tab-btn', { active: activeTab === 'variants' }]" @click="activeTab = 'variants'">
          <span class="tab-icon">🧩</span>
          变式题
          <span class="tab-count">{{ filteredVariantGroups.length }}</span>
        </button>
      </div>

      <!-- 错题列表内容 -->
      <div v-if="activeTab === 'errors'">
        <div v-if="filteredErrors.length === 0" class="empty-state">
          <div class="empty-illustration">
            <div class="empty-circle">
              <span class="empty-icon">📭</span>
            </div>
          </div>
          <h3>没有找到错题</h3>
          <p>试试调整筛选条件</p>
        </div>
        <div v-else class="error-list">
          <div v-for="(row, idx) in filteredErrors" :key="row.errorBook.id" class="error-card" :class="'status-' + row.errorBook.reviewStatus">
            <div class="ec-status-bar"></div>
            <div class="ec-main">
              <div class="ec-top">
                <div class="ec-index-wrap">
                  <span class="ec-index">{{ idx + 1 }}</span>
                </div>
                <div class="ec-badges">
                  <el-tag :type="['info','warning','success'][row.errorBook.reviewStatus]" size="small" effect="dark" round>
                    {{ ['未复盘','已复盘','已掌握'][row.errorBook.reviewStatus] }}
                  </el-tag>
                  <el-tag :type="errorTypeColor(row.errorBook.errorType)" size="small" effect="plain" round>
                    {{ errorTypeText(row.errorBook.errorType) }}
                  </el-tag>
                </div>
              </div>
              <div class="ec-question" v-html="renderLatex(row.question?.content)"></div>
              <div class="ec-meta">
                <span class="ec-subject">
                  <span class="ec-subject-dot"></span>
                  {{ getSubjectName(row.question?.subjectId) }}
                </span>
                <span v-if="row.question?.knowledgePoint" class="ec-kp">
                  <el-icon><Collection /></el-icon>
                  {{ row.question?.knowledgePoint }}
                </span>
                <span class="ec-date">
                  <el-icon><Calendar /></el-icon>
                  {{ formatDate(row.errorBook.createTime) }}
                </span>
              </div>
              <div class="ec-actions">
                <el-button size="small" round @click="viewDetail(row)">
                  <el-icon><View /></el-icon> 详情
                </el-button>
                <el-button size="small" type="success" round :loading="pushingId === row.errorBook.id" @click="openVariantDialog(row.errorBook.id)">
                  {{ pushingId === row.errorBook.id ? '生成中...' : '✨ 变式题' }}
                </el-button>
                <el-button size="small" :type="row.errorBook.reviewStatus === 2 ? 'warning' : 'primary'" round
                  @click="markReviewed(row.errorBook.id, row.errorBook.reviewStatus === 2 ? 0 : 2)">
                  {{ row.errorBook.reviewStatus === 2 ? '取消掌握' : '掌握' }}
                </el-button>
                <el-popconfirm title="确定删除这道错题吗？" @confirm="deleteError(row.errorBook.id)">
                  <template #reference>
                    <el-button size="small" type="danger" round plain>
                      <el-icon><Delete /></el-icon> 删除
                    </el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 变式题内容 -->
      <div v-if="activeTab === 'variants'">
        <div v-if="filteredVariantGroups.length === 0" class="empty-state">
          <div class="empty-illustration">
            <div class="empty-circle">
              <span class="empty-icon">🧩</span>
            </div>
          </div>
          <h3>还没有变式题</h3>
          <p>在错题列表中点击「变式题」按钮生成</p>
        </div>
        <div v-else class="variant-list">
          <div v-for="group in filteredVariantGroups" :key="group.sourceQuestionId" class="variant-group">
            <div class="vg-header" @click="toggleGroup(group.sourceQuestionId)">
              <div class="vg-expand-icon" :class="{ 'vg-rotate': expandedGroups === group.sourceQuestionId }">
                <el-icon><ArrowDown /></el-icon>
              </div>
              <div class="vg-info">
                <span class="vg-source">原题：<span v-html="renderLatex(group.sourceContent)"></span></span>
                <div class="vg-tags">
                  <el-tag type="warning" size="small" effect="plain" round>{{ group.variants.length }} 道变式题</el-tag>
                  <el-tag type="info" size="small" effect="plain" round>{{ getSubjectName(group.subjectId) }}</el-tag>
                </div>
              </div>
              <el-popconfirm title="确定删除该批次全部变式题吗？" @confirm="deleteAllV(group.variants.map(v => v.id))">
                <template #reference>
                  <el-button size="small" type="danger" text round @click.stop>全部删除</el-button>
                </template>
              </el-popconfirm>
            </div>
            <div v-show="expandedGroups === group.sourceQuestionId" class="vg-body">
              <div v-for="(v, idx) in group.variants" :key="v.id" class="variant-card">
                <div class="vc-header">
                  <span class="vc-tag">变式题 {{ idx + 1 }}</span>
                  <el-tag v-if="v.isSolved" :type="v.isCorrect ? 'success' : 'danger'" size="small" effect="dark" round>
                    {{ v.isCorrect ? '答对了' : '答错了' }}
                  </el-tag>
                  <el-tag v-else type="info" size="small" effect="plain" round>未作答</el-tag>
                </div>
                <div class="vc-content" v-html="renderLatex(displayContent(v))"></div>
                <div class="vc-actions">
                  <el-button v-if="!v.isSolved" size="small" type="primary" round @click="openAnswer(v)">
                    ✏️ 作答
                  </el-button>
                  <el-button v-else size="small" round @click="openAnswer(v)">
                    📖 查看解析
                  </el-button>
                  <el-popconfirm title="确定删除这道变式题吗？" @confirm="deleteV(v.id)">
                    <template #reference>
                      <el-button size="small" type="danger" text round>删除</el-button>
                    </template>
                  </el-popconfirm>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 变式题作答弹窗 -->
    <el-dialog v-model="answerVisible" title="变式题作答" width="700" class="answer-dialog" destroy-on-close>
      <div v-if="answerTarget">
        <div class="variant-content" v-html="renderLatex(displayContent(answerTarget))"></div>
        <template v-if="!answerResult">
          <el-input v-model="myAnswer" type="textarea" :rows="4" placeholder="请输入你的答案（可上传草稿纸照片）" class="mt-20" />
          <div class="mt-20">
            <div class="upload-label">
              <el-icon><Picture /></el-icon> 上传图片答案（可选）
            </div>
            <el-upload v-model:file-list="uploadFileList" :auto-upload="false" :on-change="handleImageChange"
              :on-remove="handleImageRemove" list-type="picture-card" accept="image/*" :limit="3">
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div class="upload-tip">支持 JPG/PNG 格式，最多 3 张，每张不超过 5MB</div>
          </div>
        </template>
        <template v-else>
          <div v-if="myAnswer || uploadedImages.length > 0" class="mt-20 answer-box">
            <strong class="answer-box-title">我的作答</strong>
            <div v-if="myAnswer" class="answer-text">{{ myAnswer }}</div>
            <div v-if="uploadedImages.length > 0" class="answer-images">
              <div class="answer-images-label">上传图片答案 ({{ uploadedImages.length }} 张)</div>
              <div class="answer-images-grid">
                <el-image v-for="(img, idx) in uploadedImages" :key="idx" :src="img"
                  style="width:100px;height:100px;border-radius:8px" fit="cover"
                  :preview-src-list="uploadedImages" />
              </div>
            </div>
          </div>
          <el-alert class="mt-20" :type="answerResult.correct ? 'success' : 'error'"
            :title="answerResult.correct ? '回答正确' : '回答错误'"
            :description="answerResult.feedback" :closable="false" show-icon />
          <div v-if="answerAndAnalysis" class="mt-20 variant-full">
            <strong>答案与解析</strong>
            <div class="variant-full-content">{{ answerAndAnalysis }}</div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button round @click="answerVisible = false">关闭</el-button>
        <el-button v-if="!answerResult" type="primary" round :loading="answering" @click="submitAnswer">
          提交答案
        </el-button>
      </template>
    </el-dialog>

    <!-- 错题详情弹窗 -->
    <el-dialog v-model="detailVisible" title="错题详情" width="700" class="detail-dialog" destroy-on-close>
      <div v-if="current">
        <div class="detail-section">
          <div class="detail-label">
            <span class="detail-label-icon">📄</span> 题目
          </div>
          <div class="detail-value" v-html="renderLatex(current.question?.content)"></div>
        </div>
        <div class="detail-section">
          <div class="detail-label">
            <span class="detail-label-icon"><el-icon><CircleCheck /></el-icon></span> 标准答案
          </div>
          <div class="detail-value detail-answer" v-html="renderLatex(current.question?.standardAnswer)"></div>
        </div>
        <div class="detail-section">
          <div class="detail-label">
            <span class="detail-label-icon"><el-icon><LightBulb /></el-icon></span> 解析
          </div>
          <div class="detail-value detail-analysis" v-html="renderLatex(current.question?.analysis)"></div>
        </div>
      </div>
    </el-dialog>

    <!-- 变式题数量弹窗 -->
    <el-dialog v-model="variantCountVisible" title="生成变式题" width="420" class="variant-count-dialog" destroy-on-close>
      <div class="vc-dialog-content">
        <div class="vc-dialog-icon">🧪</div>
        <p class="vc-dialog-desc">AI 将根据错题知识点生成变式题</p>
        <el-input-number v-model="variantCount" :min="1" :max="10" size="large" style="width:200px" />
        <div class="vc-dialog-tip">支持 1-10 道变式题</div>
      </div>
      <template #footer>
        <el-button round @click="variantCountVisible = false">取消</el-button>
        <el-button type="primary" round :loading="pushingId !== null" @click="confirmPushVariant">
          {{ pushingId !== null ? 'AI 生成中...' : '开始生成' }}
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, Plus, Search, ArrowLeft, ArrowRight, ArrowDown, Close, WarningFilled, View, Delete, Calendar, Collection } from '@element-plus/icons-vue'
import { errorBooks, myVariants, pushVariant, reviewError, answerVariant, deleteVariant, deleteErrorBook, getSubjects } from '@/api'
import { renderLatex } from '@/utils/latex'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const viewMode = ref('cards')
const activeTab = ref('errors')
const errors = ref([])
const variants = ref([])
const subjects = ref([])
const detailVisible = ref(false)
const current = ref(null)
const pushingId = ref(null)
const variantCountVisible = ref(false)
const variantCount = ref(3)
const currentErrorBookId = ref(null)
const expandedGroups = ref(null)

const filterKeyword = ref('')
const filterSubject = ref(null)
const filterDateRange = ref(null)
const variantFilterKeyword = ref('')
const variantFilterSubject = ref(null)
const variantFilterDateRange = ref(null)

const answerVisible = ref(false)
const answerTarget = ref(null)
const myAnswer = ref('')
const answering = ref(false)
const answerResult = ref(null)
const uploadFileList = ref([])
const uploadedImages = ref([])

const handleImageChange = (file) => {
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    uploadFileList.value = uploadFileList.value.filter(f => f.uid !== file.uid)
    return
  }
  const reader = new FileReader()
  reader.onload = (e) => { uploadedImages.value.push(e.target.result) }
  reader.readAsDataURL(file.raw)
}

const handleImageRemove = (file) => {
  const index = uploadFileList.value.findIndex(f => f.uid === file.uid)
  if (index > -1) uploadedImages.value.splice(index, 1)
}

const subjectMap = computed(() => {
  const map = {}
  subjects.value.forEach(s => { map[s.id] = s.subjectName })
  return map
})
const getSubjectName = (subjectId) => subjectMap.value[subjectId] || '-'
const formatDate = (dateStr) => { if (!dateStr) return '-'; return dateStr.slice(0, 10) }

// 统计
const unreviewedCount = computed(() => errors.value.filter(e => e.errorBook.reviewStatus === 0).length)
const reviewedCount = computed(() => errors.value.filter(e => e.errorBook.reviewStatus === 1).length)
const masteredCount = computed(() => errors.value.filter(e => e.errorBook.reviewStatus === 2).length)
const allMasteryRate = computed(() => errors.value.length > 0 ? Math.round((masteredCount.value / errors.value.length) * 100) : 0)

const topErrorType = computed(() => {
  const map = {}
  errors.value.forEach(e => { const t = e.errorBook.errorType; if (t) map[t] = (map[t] || 0) + 1 })
  let top = null, max = 0
  for (const [type, count] of Object.entries(map)) { if (count > max) { max = count; top = { type: Number(type), count } } }
  return top
})

const subjectIcons = {
  '语文': '📖', '数学': '🔢', '英语': '🔤', '物理': '⚡', '化学': '🧪',
  '生物': '🧬', '历史': '📜', '地理': '🌍', '政治': '⚖️',
  '道德与法治': '🏛️', '科学': '🔬', '音乐': '🎵', '美术': '🎨', '体育': '⚽', '信息技术': '💻'
}
const artsSubjects = new Set(['语文', '英语', '历史', '地理', '政治', '道德与法治'])

// 预设学科固定列表（按顺序展示，每个学科独立主题色）
const presetSubjects = [
  { name: '数学', icon: 'Odometer', themeColor: '#0d9488', themeBg: '#e7f4f3' },
  { name: '语文', icon: 'Reading', themeColor: '#f56c6c', themeBg: '#fef0f0' },
  { name: '英语', icon: 'ChatLineRound', themeColor: '#67c23a', themeBg: '#f0f9eb' },
  { name: '物理', icon: 'Lightning', themeColor: '#e6a23c', themeBg: '#fdf6ec' },
  { name: '化学', icon: 'MagicStick', themeColor: '#909399', themeBg: '#f4f4f5' },
  { name: '生物', icon: 'Cherry', themeColor: '#67c23a', themeBg: '#f0f9eb' },
  { name: '历史', icon: 'Memo', themeColor: '#e6a23c', themeBg: '#fdf6ec' },
  { name: '地理', icon: 'Location', themeColor: '#0d9488', themeBg: '#e7f4f3' },
  { name: '政治', icon: 'ScaleToOriginal', themeColor: '#f56c6c', themeBg: '#fef0f0' }
]

const subjectCards = computed(() => {
  // 统计各学科错题数据
  const errorMap = {}
  errors.value.forEach(e => {
    const sid = e.question?.subjectId
    if (!sid) return
    if (!errorMap[sid]) errorMap[sid] = { total: 0, unreviewed: 0, mastered: 0, types: {} }
    errorMap[sid].total++
    if (e.errorBook.reviewStatus === 0) errorMap[sid].unreviewed++
    if (e.errorBook.reviewStatus === 2) errorMap[sid].mastered++
    const t = e.errorBook.errorType
    if (t) errorMap[sid].types[t] = (errorMap[sid].types[t] || 0) + 1
  })

  // 固定渲染所有预设学科（无论是否有错题）
  const rendered = new Set()
  const cards = []

  for (const preset of presetSubjects) {
    const subj = subjects.value.find(s => s.subjectName === preset.name)
    if (!subj) continue
    const data = errorMap[subj.id] || { total: 0, unreviewed: 0, mastered: 0, types: {} }
    let topType = null, max = 0
    for (const [type, count] of Object.entries(data.types)) {
      if (count > max) { max = count; topType = { type: Number(type), count } }
    }
    const masteryRate = data.total > 0 ? Math.round((data.mastered / data.total) * 100) : 0
    cards.push({
      subjectId: subj.id,
      subjectName: preset.name,
      total: data.total,
      unreviewed: data.unreviewed,
      mastered: data.mastered,
      masteryRate,
      topType,
      icon: preset.icon,
      themeColor: preset.themeColor,
      themeBg: preset.themeBg
    })
    rendered.add(subj.id)
  }

  // 补充不在预设列表但有错题的学科
  for (const [sid, data] of Object.entries(errorMap)) {
    const id = Number(sid)
    if (rendered.has(id)) continue
    const name = getSubjectName(id)
    let topType = null, max = 0
    for (const [type, count] of Object.entries(data.types)) {
      if (count > max) { max = count; topType = { type: Number(type), count } }
    }
    const masteryRate = data.total > 0 ? Math.round((data.mastered / data.total) * 100) : 0
    cards.push({
      subjectId: id,
      subjectName: name,
      total: data.total,
      unreviewed: data.unreviewed,
      mastered: data.mastered,
      masteryRate,
      topType,
      icon: 'Memo',
      themeColor: '#909399',
      themeBg: '#f4f4f5'
    })
  }

  return cards
})

const enterList = (subjectId) => { filterSubject.value = subjectId; activeTab.value = 'errors'; viewMode.value = 'list' }
const toggleGroup = (id) => { expandedGroups.value = expandedGroups.value === id ? null : id }

const filteredErrors = computed(() => {
  let list = errors.value
  if (filterKeyword.value) {
    const kw = filterKeyword.value.toLowerCase()
    list = list.filter(e => {
      const content = (e.question?.content || '').toLowerCase()
      const kp = (e.question?.knowledgePoint || '').toLowerCase()
      return content.includes(kw) || kp.includes(kw)
    })
  }
  if (filterSubject.value) list = list.filter(e => e.question?.subjectId === filterSubject.value)
  if (filterDateRange.value && filterDateRange.value.length === 2) {
    const [start, end] = filterDateRange.value
    list = list.filter(e => { const d = formatDate(e.errorBook.createTime); return d >= start && d <= end })
  }
  return list
})
const applyFilters = () => {}
const clearFilters = () => { filterKeyword.value = ''; filterSubject.value = null; filterDateRange.value = null }

const variantGroups = computed(() => {
  const map = new Map()
  for (const v of variants.value) {
    const key = v.sourceQuestionId || 0
    if (!map.has(key)) {
      const error = errors.value.find(e => e.question?.id === key)
      map.set(key, { sourceQuestionId: key, sourceContent: error?.question?.content || '未知原题', subjectId: error?.question?.subjectId || null, variants: [] })
    }
    map.get(key).variants.push(v)
  }
  return Array.from(map.values())
})

const filteredVariantGroups = computed(() => {
  let groups = variantGroups.value
  if (variantFilterKeyword.value) {
    const kw = variantFilterKeyword.value.toLowerCase()
    groups = groups.filter(g => (g.sourceContent || '').toLowerCase().includes(kw))
  }
  if (variantFilterSubject.value) groups = groups.filter(g => g.subjectId === variantFilterSubject.value)
  if (variantFilterDateRange.value && variantFilterDateRange.value.length === 2) {
    const [start, end] = variantFilterDateRange.value
    groups = groups.filter(g => { const d = formatDate(g.variants[0].createTime); return d >= start && d <= end })
  }
  return groups
})
const applyVariantFilters = () => {}
const clearVariantFilters = () => { variantFilterKeyword.value = ''; variantFilterSubject.value = null; variantFilterDateRange.value = null }

const displayContent = (row) => {
  if (!row || !row.content) return ''
  let content = row.content
  if (row.isSolved) return content
  const answerIdx = content.indexOf('【答案】')
  if (answerIdx > 0) content = content.slice(0, answerIdx).trim()
  const questionMatch = content.match(/【题目】([\s\S]*?)(?=【选项】)/)
  const optionsMatch = content.match(/【选项】([\s\S]*?)(?=【答案】|$)/)
  if (!questionMatch) return content
  let questionText = questionMatch[1].trim()
  let optionsText = optionsMatch ? optionsMatch[1].trim() : ''
  const lines = questionText.split('\n')
  const questionLines = [], optionLines = []
  for (const line of lines) {
    const trimmed = line.trim()
    if (/^[A-Da-d][.、]\s+\S/.test(trimmed) && trimmed.length > 5) optionLines.push(trimmed)
    else questionLines.push(line)
  }
  const hasRealOptionsInSection = optionsText && optionsText.split('\n').some(l => l.trim().length > 5 && /^[A-Da-d][.、]\s+\S/.test(l.trim()))
  let finalOptions = ''
  if (optionLines.length >= 2) finalOptions = optionLines.join('\n')
  else if (hasRealOptionsInSection) { finalOptions = optionsText.replace(/\s*([A-Da-d][.、])\s*/g, '\n$1 ').trim(); if (finalOptions.startsWith('\n')) finalOptions = finalOptions.slice(1) }
  let result = `【题目】${questionLines.join('\n').trim()}`
  if (finalOptions) result += `\n\n【选项】\n${finalOptions}`
  return result
}

const answerAndAnalysis = computed(() => {
  if (!answerTarget.value?.content) return ''
  const content = answerTarget.value.content
  const answerIdx = content.indexOf('【答案】')
  if (answerIdx < 0) return ''
  return content.slice(answerIdx).trim()
})

const openAnswer = (row) => {
  answerTarget.value = row; uploadFileList.value = []; uploadedImages.value = []
  if (row.isSolved) {
    myAnswer.value = row.studentAnswer || ''
    if (row.studentImages) { try { const imgs = JSON.parse(row.studentImages); if (Array.isArray(imgs)) uploadedImages.value = imgs } catch (e) { console.error('解析图片数据失败:', e) } }
    const hasImages = uploadedImages.value.length > 0
    answerResult.value = { correct: row.isCorrect === 1, feedback: hasImages ? `已作答（含 ${uploadedImages.value.length} 张图片答案）` : '已作答' }
  } else { myAnswer.value = ''; answerResult.value = null }
  answerVisible.value = true
}

const submitAnswer = async () => {
  if (!myAnswer.value.trim() && uploadedImages.value.length === 0) { ElMessage.warning('请先填写答案或上传草稿纸照片'); return }
  answering.value = true
  try { const res = await answerVariant(answerTarget.value.id, myAnswer.value.trim(), uploadedImages.value); answerResult.value = res.data; await load() } catch (err) {} finally { answering.value = false }
}

const errorTypeText = (t) => ['','知识点缺失','计算失误','审题错误','思路错误','表达不清'][t]
const errorTypeColor = (t) => ['','danger','warning','info','warning','info'][t]

const load = async () => {
  try {
    const gradeLevel = userStore.userInfo?.gradeLevel || 0
    const [e, v, s] = await Promise.all([errorBooks(), myVariants(), getSubjects(gradeLevel)])
    errors.value = e.data || []; variants.value = v.data || []; subjects.value = s.data || []
  } catch (err) {}
}
onMounted(load)

const viewDetail = (row) => { current.value = row; detailVisible.value = true }
const pushV = async (id, count) => { pushingId.value = id; try { await pushVariant(id, count); ElMessage.success(`已生成 ${count} 道变式题`); await load() } catch (err) {} finally { pushingId.value = null } }
const openVariantDialog = (id) => { currentErrorBookId.value = id; variantCount.value = 3; variantCountVisible.value = true }
const confirmPushVariant = async () => { variantCountVisible.value = false; await pushV(currentErrorBookId.value, variantCount.value) }
const deleteV = async (id) => { try { await deleteVariant(id); ElMessage.success('已删除'); await load() } catch (err) {} }
const deleteAllV = async (ids) => { try { for (const id of ids) await deleteVariant(id); ElMessage.success(`已删除 ${ids.length} 道变式题`); await load() } catch (err) {} }
const deleteError = async (id) => { try { await deleteErrorBook(id); ElMessage.success('已删除错题'); await load() } catch (err) {} }
const markReviewed = async (id, status) => { try { await reviewError(id, status); ElMessage.success('已标记'); await load() } catch (err) {} }
</script>

<style scoped>
/* ========== 页面整体 ========== */
.errorbook-page {
  padding: 24px 32px;
  max-width: 1400px;
  margin: 0 auto;
  min-height: 100vh;
  background: #f0f2f5;
}

/* ========== 页面标题 ========== */
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}
.header-left {
  display: flex;
  align-items: center;
  gap: 14px;
}
.header-badge {
  width: 48px;
  height: 48px;
  background: #409eff;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
}
.page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1a1a2e;
  margin: 0;
}
.page-subtitle {
  font-size: 13px;
  color: #909399;
  margin: 2px 0 0;
}
.header-stats {
  display: flex;
  gap: 28px;
  align-items: center;
}
.header-stat-item {
  font-size: 14px;
  color: #606266;
}
.header-stat-item strong {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  margin-left: 4px;
}
.btn-back {
  border-radius: 6px;
  padding: 8px 16px;
  font-size: 13px;
}

/* ========== 科目卡片网格 ========== */
.subject-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 16px;
}
@media (max-width: 1200px) {
  .subject-grid { grid-template-columns: repeat(3, 1fr); }
}
@media (max-width: 900px) {
  .subject-grid { grid-template-columns: repeat(2, 1fr); }
}
@media (max-width: 599px) {
  .subject-grid { grid-template-columns: 1fr; }
}

.subject-card {
  position: relative;
  background: #fff;
  border-radius: 12px;
  cursor: pointer;
  transition: box-shadow 0.2s, transform 0.2s;
  border: 1px solid #E8F1F4;
  overflow: hidden;
  padding: 20px;
}
.subject-card:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,0.08);
  transform: translateY(-2px);
}

.sc-top {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 12px;
}
.sc-icon-wrap {
  width: 44px;
  height: 44px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
}
.sc-icon-all { background: #f0f0f5; }

.sc-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f56c6c;
  color: #fff;
  font-size: 11px;
  font-weight: 600;
  min-width: 22px;
  height: 22px;
  padding: 0 6px;
  border-radius: 11px;
}

.sc-name {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  margin: 0 0 14px;
}

.sc-stats-row {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 14px;
  padding: 10px 14px;
  background: #E8F1F4;
  border-radius: 8px;
}
.sc-stats-all { background: #f0f0f5; }
.sc-stat { display: flex; flex-direction: column; }
.sc-stat-val {
  font-size: 22px;
  font-weight: 700;
  color: #303133;
  line-height: 1.2;
}
.sc-stat-warn .sc-stat-val { color: #f56c6c; }
.sc-stat-lbl {
  font-size: 11px;
  color: #909399;
  margin-top: 2px;
}

.sc-progress-wrap { margin-bottom: 10px; }
.sc-progress {
  height: 6px;
  background: #E8F1F4;
  border-radius: 3px;
  overflow: hidden;
}
.sc-progress-bar {
  height: 100%;
  background: #67c23a;
  border-radius: 3px;
  transition: width 0.4s;
}

.sc-summary {
  font-size: 12px;
  color: #909399;
  margin-top: 8px;
}

/* ========== 筛选栏 ========== */
.filter-bar {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 20px;
  flex-wrap: wrap;
  align-items: center;
  padding: 14px 18px;
  background: #fff;
  border-radius: 8px;
  border: 1px solid #E8F1F4;
}
.filter-search { flex: 0 0 auto; }
.filter-input { width: 260px; }
.filter-right {
  display: flex;
  gap: 12px;
  align-items: center;
  flex-wrap: wrap;
}
.filter-select { width: 140px; }
.filter-date { width: 240px; }
.filter-clear { border-radius: 6px; font-size: 13px; }

/* ========== Tab 栏 ========== */
.tab-bar {
  display: flex;
  gap: 4px;
  margin-bottom: 20px;
  background: #E8F1F4;
  border-radius: 6px;
  padding: 4px;
  width: fit-content;
}
.tab-btn {
  padding: 8px 20px;
  border: none;
  background: transparent;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  color: #606266;
  cursor: pointer;
  transition: all 0.2s;
  display: flex;
  align-items: center;
  gap: 6px;
}
.tab-btn:hover { color: #409eff; }
.tab-btn.active {
  background: #fff;
  color: #409eff;
  box-shadow: 0 1px 4px rgba(0,0,0,0.08);
}
.tab-icon { font-size: 15px; }
.tab-count {
  background: #E8F1F4;
  padding: 1px 8px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 600;
}
.tab-btn.active .tab-count { background: #ecf5ff; color: #409eff; }

/* ========== 错题卡片列表 ========== */
.error-list { display: flex; flex-direction: column; gap: 12px; }
.error-card {
  position: relative;
  display: flex;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  transition: box-shadow 0.2s;
  border: 1px solid #E8F1F4;
}
.error-card:hover {
  box-shadow: 0 2px 10px rgba(0,0,0,0.06);
}
.ec-status-bar {
  width: 4px;
  flex-shrink: 0;
}
.status-0 .ec-status-bar { background: #f56c6c; }
.status-1 .ec-status-bar { background: #e6a23c; }
.status-2 .ec-status-bar { background: #67c23a; }

.ec-main {
  flex: 1;
  padding: 16px 20px;
  min-width: 0;
}
.ec-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 10px;
}
.ec-index-wrap {
  width: 30px;
  height: 30px;
  background: #E8F1F4;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.ec-index {
  font-size: 13px;
  font-weight: 600;
  color: #909399;
}
.ec-badges {
  display: flex;
  gap: 6px;
}
.ec-question {
  font-size: 14px;
  color: #303133;
  line-height: 1.6;
  margin-bottom: 10px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.ec-meta {
  display: flex;
  align-items: center;
  gap: 14px;
  flex-wrap: wrap;
  margin-bottom: 12px;
}
.ec-subject {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  color: #409eff;
  font-weight: 500;
}
.ec-subject-dot {
  width: 5px;
  height: 5px;
  background: #409eff;
  border-radius: 50%;
}
.ec-kp {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #909399;
}
.ec-date {
  display: flex;
  align-items: center;
  gap: 3px;
  font-size: 12px;
  color: #c0c4cc;
}
.ec-actions {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

/* ========== 变式题 ========== */
.variant-list { display: flex; flex-direction: column; gap: 12px; }
.variant-group {
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  border: 1px solid #E8F1F4;
}
.vg-header {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 14px 18px;
  cursor: pointer;
  transition: background 0.15s;
}
.vg-header:hover { background: #fafafa; }
.vg-expand-icon {
  width: 26px;
  height: 26px;
  background: #E8F1F4;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.2s;
  flex-shrink: 0;
  color: #909399;
  font-size: 13px;
}
.vg-rotate { transform: rotate(180deg); }
.vg-info { flex: 1; min-width: 0; }
.vg-source {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
  color: #606266;
  margin-bottom: 4px;
}
.vg-tags {
  display: flex;
  gap: 6px;
}
.vg-body { padding: 0 18px 18px; }

.variant-card {
  padding: 16px;
  background: #fafafa;
  border-radius: 6px;
  margin-top: 10px;
  border: 1px solid #E8F1F4;
}
.vc-header { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.vc-tag {
  font-size: 13px;
  font-weight: 600;
  color: #409eff;
}
.vc-content {
  background: #fff;
  padding: 14px 16px;
  border-radius: 6px;
  white-space: pre-wrap;
  line-height: 1.7;
  font-size: 14px;
  color: #303133;
  border: 1px solid #E8F1F4;
}
.vc-actions { display: flex; justify-content: flex-end; gap: 8px; margin-top: 12px; }

/* ========== 空状态 ========== */
.empty-state {
  text-align: center;
  padding: 60px 20px;
}
.empty-illustration {
  position: relative;
  display: inline-block;
  margin-bottom: 16px;
}
.empty-circle {
  width: 80px;
  height: 80px;
  background: #E8F1F4;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.empty-icon { font-size: 36px; }
.empty-state h3 {
  font-size: 16px;
  color: #303133;
  margin: 0 0 6px;
  font-weight: 600;
}
.empty-state p {
  font-size: 13px;
  color: #909399;
  margin: 0;
}

/* ========== 弹窗样式 ========== */
.upload-label {
  display: flex;
  align-items: center;
  gap: 5px;
  margin-bottom: 8px;
  color: #606266;
  font-size: 13px;
}
.upload-tip {
  color: #909399;
  font-size: 12px;
  margin-top: 4px;
}
.answer-box {
  background: #E8F1F4;
  padding: 14px 16px;
  border-radius: 6px;
  border-left: 3px solid #409eff;
}
.answer-box-title {
  color: #409eff;
  font-size: 13px;
  font-weight: 600;
  display: block;
  margin-bottom: 6px;
}
.answer-text {
  white-space: pre-wrap;
  color: #303133;
  line-height: 1.6;
}
.answer-images-label {
  color: #909399;
  font-size: 12px;
  margin-bottom: 6px;
}
.answer-images-grid {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.variant-content {
  background: #E8F1F4;
  padding: 16px;
  border-radius: 6px;
  white-space: pre-wrap;
  line-height: 1.7;
  border: 1px solid #E8F1F4;
}
.variant-full {
  background: #fafafa;
  padding: 16px;
  border-radius: 6px;
  border: 1px dashed #E8F1F4;
  line-height: 1.7;
}
.variant-full strong {
  display: block;
  margin-bottom: 6px;
  color: #303133;
}
.variant-full-content {
  white-space: pre-wrap;
  color: #606266;
}

.detail-section { margin-bottom: 20px; }
.detail-section:last-child { margin-bottom: 0; }
.detail-label {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 13px;
  font-weight: 600;
  color: #909399;
  margin-bottom: 6px;
}
.detail-label-icon { font-size: 15px; }
.detail-value {
  font-size: 14px;
  color: #303133;
  line-height: 1.7;
  background: #E8F1F4;
  padding: 12px 14px;
  border-radius: 6px;
  border: 1px solid #E8F1F4;
}
.detail-answer {
  border-left: 3px solid #67c23a;
}
.detail-analysis {
  border-left: 3px solid #409eff;
}

/* 变式题数量弹窗 */
.vc-dialog-content {
  text-align: center;
  padding: 10px 0;
}
.vc-dialog-icon {
  font-size: 40px;
  margin-bottom: 10px;
}
.vc-dialog-desc {
  font-size: 14px;
  color: #606266;
  margin-bottom: 18px;
}
.vc-dialog-tip {
  margin-top: 12px;
  color: #909399;
  font-size: 12px;
}

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .header-stats { display: none; }
  .filter-bar {
    flex-direction: column;
    align-items: stretch;
  }
  .filter-input,
  .filter-select,
  .filter-date {
    width: 100%;
  }
  .filter-right {
    flex-direction: column;
  }
}
</style>
