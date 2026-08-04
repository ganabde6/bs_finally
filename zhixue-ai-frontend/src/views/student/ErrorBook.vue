<template>
  <el-card>
    <template #header><span>智能错题本</span></template>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="错题列表" name="errors">
        <!-- 筛选栏 -->
        <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap;align-items:center">
          <el-input v-model="filterKeyword" placeholder="搜索题目/知识点..." clearable style="width:220px" @input="applyFilters">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="filterSubject" placeholder="按科目筛选" clearable style="width:160px" @change="applyFilters">
            <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
          </el-select>
          <el-date-picker v-model="filterDateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
            style="width:260px" @change="applyFilters" />
          <el-button v-if="filterKeyword || filterSubject || filterDateRange?.length" @click="clearFilters" text type="primary" size="small">清除筛选</el-button>
        </div>
        <el-table :data="filteredErrors" stripe>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="题目" min-width="300">
            <template #default="{row}">{{ row.question?.content }}</template>
          </el-table-column>
          <el-table-column label="科目" width="100">
            <template #default="{row}">{{ getSubjectName(row.question?.subjectId) }}</template>
          </el-table-column>
          <el-table-column label="错误类型" width="120">
            <template #default="{row}">
              <el-tag :type="errorTypeColor(row.errorBook.errorType)">{{ errorTypeText(row.errorBook.errorType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="知识点" width="140">
            <template #default="{row}">{{ row.question?.knowledgePoint }}</template>
          </el-table-column>
          <el-table-column label="日期" width="120">
            <template #default="{row}">{{ formatDate(row.errorBook.createTime) }}</template>
          </el-table-column>
          <el-table-column label="复盘状态" width="100">
            <template #default="{row}">
              <el-tag :type="['info','warning','success'][row.errorBook.reviewStatus]">{{ ['未复盘','已复盘','已掌握'][row.errorBook.reviewStatus] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="320">
            <template #default="{row}">
              <el-button size="small" @click="viewDetail(row)">查看详情</el-button>
              <el-button size="small" type="success" :loading="pushingId === row.errorBook.id" @click="openVariantDialog(row.errorBook.id)">
                {{ pushingId === row.errorBook.id ? 'AI 生成中...' : '推送变式题' }}
              </el-button>
              <el-button size="small" type="primary" @click="markReviewed(row.errorBook.id, 2)">已掌握</el-button>
              <el-popconfirm title="确定删除这道错题吗？" @confirm="deleteError(row.errorBook.id)">
                <template #reference>
                  <el-button size="small" type="danger" plain>删除</el-button>
                </template>
              </el-popconfirm>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="变式题" name="variants">
        <!-- 筛选栏 -->
        <div style="display:flex;gap:12px;margin-bottom:16px;flex-wrap:wrap;align-items:center">
          <el-input v-model="variantFilterKeyword" placeholder="搜索题目/知识点..." clearable style="width:220px" @input="applyVariantFilters">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="variantFilterSubject" placeholder="按科目筛选" clearable style="width:160px" @change="applyVariantFilters">
            <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
          </el-select>
          <el-date-picker v-model="variantFilterDateRange" type="daterange" range-separator="至"
            start-placeholder="开始日期" end-placeholder="结束日期" value-format="YYYY-MM-DD"
            style="width:260px" @change="applyVariantFilters" />
          <el-button v-if="variantFilterKeyword || variantFilterSubject || variantFilterDateRange?.length" @click="clearVariantFilters" text type="primary" size="small">清除筛选</el-button>
        </div>
        <el-empty v-if="filteredVariantGroups.length === 0" description="还没有变式题，请在错题列表中点击「推送变式题」生成" />
        <el-collapse v-else v-model="expandedGroups" accordion style="margin-top:10px">
          <el-collapse-item v-for="group in filteredVariantGroups" :key="group.sourceQuestionId" :name="group.sourceQuestionId">
            <template #title>
              <div style="display:flex;align-items:center;gap:12px;width:100%">
                <el-tag type="warning" size="small">{{ group.variants.length }} 道</el-tag>
                <el-tag type="info" size="small">{{ getSubjectName(group.subjectId) }}</el-tag>
                <span style="flex:1;overflow:hidden;text-overflow:ellipsis;white-space:nowrap">
                  原题：{{ group.sourceContent }}
                </span>
                <el-popconfirm title="确定删除该批次全部变式题吗？" @confirm="deleteAllV(group.variants.map(v => v.id))">
                  <template #reference>
                    <el-button size="small" type="danger" plain @click.stop>全部删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </template>
            <div v-for="(v, idx) in group.variants" :key="v.id" class="variant-item">
              <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px">
                <el-tag type="primary" size="small">变式题 {{ idx + 1 }}</el-tag>
                <el-tag v-if="v.isSolved" :type="v.isCorrect ? 'success' : 'danger'" size="small">
                  {{ v.isCorrect ? '答对了' : '答错了' }}
                </el-tag>
                <el-tag v-else type="info" size="small">未作答</el-tag>
              </div>
              <div class="variant-content" style="white-space:pre-wrap">{{ displayContent(v) }}</div>
              <div style="margin-top:8px;text-align:right">
                <el-button v-if="!v.isSolved" size="small" type="primary" @click="openAnswer(v)">作答</el-button>
                <el-button v-else size="small" @click="openAnswer(v)">查看解析</el-button>
                <el-popconfirm title="确定删除这道变式题吗？" @confirm="deleteV(v.id)">
                  <template #reference>
                    <el-button size="small" type="danger" plain style="margin-left:8px">删除</el-button>
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </el-collapse-item>
        </el-collapse>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="answerVisible" title="变式题作答" width="700">
      <div v-if="answerTarget">
        <div class="variant-content">{{ displayContent(answerTarget) }}</div>
        <template v-if="!answerResult">
          <el-input v-model="myAnswer" type="textarea" :rows="4" placeholder="请输入你的答案（数学题可上传草稿纸照片）" class="mt-20" />
          
          <!-- 图片上传区域 -->
          <div class="mt-20">
            <div style="margin-bottom:8px;color:#606266;font-size:14px">
              <el-icon><Picture /></el-icon> 上传图片答案（可选）
            </div>
            <el-upload
              v-model:file-list="uploadFileList"
              :auto-upload="false"
              :on-change="handleImageChange"
              :on-remove="handleImageRemove"
              list-type="picture-card"
              accept="image/*"
              :limit="3"
            >
              <el-icon><Plus /></el-icon>
            </el-upload>
            <div style="color:#909399;font-size:12px;margin-top:4px">
              支持 JPG/PNG 格式，最多 3 张，每张不超过 5MB
            </div>
          </div>
        </template>
        <template v-else>
          <!-- 学生作答内容 -->
          <div v-if="myAnswer || uploadedImages.length > 0" class="mt-20" style="background:#f5f7fa;padding:12px 16px;border-radius:8px;border-left:4px solid #409eff">
            <strong style="color:#409eff">我的作答:</strong>
            <div v-if="myAnswer" style="white-space:pre-wrap;margin-top:6px;color:#303133">{{ myAnswer }}</div>
            <div v-if="uploadedImages.length > 0" style="margin-top:8px">
              <div style="color:#909399;font-size:13px;margin-bottom:6px"> 上传图片答案 ({{ uploadedImages.length }} 张):</div>
              <div style="display:flex;gap:8px;flex-wrap:wrap">
                <el-image v-for="(img, idx) in uploadedImages" :key="idx" :src="img"
                          style="width:100px;height:100px;border-radius:6px" fit="cover" :preview-src-list="uploadedImages" />
              </div>
            </div>
          </div>
          <!-- AI 批改结果 -->
          <el-alert class="mt-20" :type="answerResult.correct ? 'success' : 'error'"
                    :title="answerResult.correct ? '回答正确' : '回答错误'"
                    :description="answerResult.feedback" :closable="false" />
          <!-- 只显示答案和解析，不重复显示题目 -->
          <div v-if="answerAndAnalysis" class="mt-20 variant-full">
            <strong>答案与解析:</strong>
            <div style="white-space:pre-wrap;margin-top:8px">{{ answerAndAnalysis }}</div>
          </div>
        </template>
      </div>
      <template #footer>
        <el-button @click="answerVisible = false">关闭</el-button>
        <el-button v-if="!answerResult" type="primary" :loading="answering" @click="submitAnswer">提交答案</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="detailVisible" title="错题详情" width="700">
      <div v-if="current">
        <p><strong>题目:</strong>{{ current.question?.content }}</p>
        <p class="mt-20"><strong>标准答案:</strong>{{ current.question?.standardAnswer }}</p>
        <p class="mt-20"><strong>解析:</strong>{{ current.question?.analysis }}</p>
      </div>
    </el-dialog>

    <!-- 变式题数量自定义弹窗 -->
    <el-dialog v-model="variantCountVisible" title="生成变式题" width="400">
      <div style="text-align:center">
        <p style="margin-bottom:20px">请输入要生成的变式题数量：</p>
        <el-input-number 
          v-model="variantCount" 
          :min="1" 
          :max="10" 
          size="large" 
          style="width:200px"
        />
        <div style="margin-top:16px;color:#909399;font-size:13px">
          💡 支持 1-10 道，AI 将根据错题知识点生成变式题，底层知识不变但题型和逻辑会变化
        </div>
      </div>
      <template #footer>
        <el-button @click="variantCountVisible = false">取消</el-button>
        <el-button type="primary" :loading="pushingId !== null" @click="confirmPushVariant">
          {{ pushingId !== null ? 'AI 生成中...' : '开始生成' }}
        </el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Picture, Plus, Search } from '@element-plus/icons-vue'
import { errorBooks, myVariants, pushVariant, reviewError, answerVariant, deleteVariant, deleteErrorBook, getSubjects } from '@/api'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

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

// 筛选条件
const filterKeyword = ref('')
const filterSubject = ref(null)
const filterDateRange = ref(null)
const variantFilterKeyword = ref('')
const variantFilterSubject = ref(null)
const variantFilterDateRange = ref(null)

// 变式题作答
const answerVisible = ref(false)
const answerTarget = ref(null)
const myAnswer = ref('')
const answering = ref(false)
const answerResult = ref(null)

// 图片上传
const uploadFileList = ref([])
const uploadedImages = ref([]) // base64 图片数据

const handleImageChange = (file) => {
  // 限制文件大小 5MB
  if (file.size > 5 * 1024 * 1024) {
    ElMessage.error('图片大小不能超过 5MB')
    uploadFileList.value = uploadFileList.value.filter(f => f.uid !== file.uid)
    return
  }
  // 读取图片为 base64
  const reader = new FileReader()
  reader.onload = (e) => {
    uploadedImages.value.push(e.target.result)
  }
  reader.readAsDataURL(file.raw)
}

const handleImageRemove = (file) => {
  const index = uploadFileList.value.findIndex(f => f.uid === file.uid)
  if (index > -1) {
    uploadedImages.value.splice(index, 1)
  }
}

// 学科名称映射
const subjectMap = computed(() => {
  const map = {}
  subjects.value.forEach(s => { map[s.id] = s.subjectName })
  return map
})

const getSubjectName = (subjectId) => subjectMap.value[subjectId] || '-'

// 日期格式化
const formatDate = (dateStr) => {
  if (!dateStr) return '-'
  return dateStr.slice(0, 10)
}

// 错题列表筛选
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
  if (filterSubject.value) {
    list = list.filter(e => e.question?.subjectId === filterSubject.value)
  }
  if (filterDateRange.value && filterDateRange.value.length === 2) {
    const [start, end] = filterDateRange.value
    list = list.filter(e => {
      const d = formatDate(e.errorBook.createTime)
      return d >= start && d <= end
    })
  }
  return list
})

const applyFilters = () => {} // computed 自动响应
const clearFilters = () => { filterKeyword.value = ''; filterSubject.value = null; filterDateRange.value = null }

// 变式题分组（带科目信息）
const variantGroups = computed(() => {
  const map = new Map()
  for (const v of variants.value) {
    const key = v.sourceQuestionId || 0
    if (!map.has(key)) {
      const error = errors.value.find(e => e.question?.id === key)
      map.set(key, {
        sourceQuestionId: key,
        sourceContent: error?.question?.content || '未知原题',
        subjectId: error?.question?.subjectId || null,
        variants: []
      })
    }
    map.get(key).variants.push(v)
  }
  return Array.from(map.values())
})

// 变式题筛选
const filteredVariantGroups = computed(() => {
  let groups = variantGroups.value
  if (variantFilterKeyword.value) {
    const kw = variantFilterKeyword.value.toLowerCase()
    groups = groups.filter(g => {
      const content = (g.sourceContent || '').toLowerCase()
      return content.includes(kw)
    })
  }
  if (variantFilterSubject.value) {
    groups = groups.filter(g => g.subjectId === variantFilterSubject.value)
  }
  if (variantFilterDateRange.value && variantFilterDateRange.value.length === 2) {
    const [start, end] = variantFilterDateRange.value
    groups = groups.filter(g => {
      // 使用该组第一道变式题的创建时间作为分组日期
      const first = g.variants[0]
      const d = formatDate(first.createTime)
      return d >= start && d <= end
    })
  }
  return groups
})

const applyVariantFilters = () => {}
const clearVariantFilters = () => { variantFilterKeyword.value = ''; variantFilterSubject.value = null; variantFilterDateRange.value = null }

// AI 生成的变式题内容内含【答案】【解析】段,未作答时不展示,防止剧透
// 统一格式化为：题目 + 选项(每行一个) 的标准格式
const displayContent = (row) => {
  if (!row || !row.content) return ''
  let content = row.content
  if (row.isSolved) return content

  // 截断【答案】及之后的内容
  const answerIdx = content.indexOf('【答案】')
  if (answerIdx > 0) {
    content = content.slice(0, answerIdx).trim()
  }

  // 提取【题目】部分（到【选项】为止）
  const questionMatch = content.match(/【题目】([\s\S]*?)(?=【选项】)/)
  // 提取【选项】部分
  const optionsMatch = content.match(/【选项】([\s\S]*?)(?=【答案】|$)/)

  if (!questionMatch) return content

  let questionText = questionMatch[1].trim()
  let optionsText = optionsMatch ? optionsMatch[1].trim() : ''

  // 从题目文本中提取选项行（以 A/B/C/D. 开头且内容长度>5的行）
  const lines = questionText.split('\n')
  const questionLines = []
  const optionLines = []

  for (const line of lines) {
    const trimmed = line.trim()
    // 检测是否是真实选项行（以 A-D 开头，后面有实质内容，长度>5）
    if (/^[A-Da-d][.、]\s+\S/.test(trimmed) && trimmed.length > 5) {
      optionLines.push(trimmed)
    } else {
      questionLines.push(line)
    }
  }

  // 判断【选项】段落是否有实质内容（不是只有 "A. A" 这种无意义内容）
  const hasRealOptionsInSection = optionsText && 
    optionsText.split('\n').some(l => l.trim().length > 5 && /^[A-Da-d][.、]\s+\S/.test(l.trim()))

  // 优先使用从题目中提取的真实选项
  let finalOptions = ''
  if (optionLines.length >= 2) {
    finalOptions = optionLines.join('\n')
  } else if (hasRealOptionsInSection) {
    // 格式化【选项】段落：确保每行一个
    finalOptions = optionsText.replace(/\s*([A-Da-d][.、])\s*/g, '\n$1 ').trim()
    if (finalOptions.startsWith('\n')) finalOptions = finalOptions.slice(1)
  }

  // 组装最终内容
  let result = `【题目】${questionLines.join('\n').trim()}`
  if (finalOptions) {
    result += `\n\n【选项】\n${finalOptions}`
  }

  return result
}

// 提取答案和解析部分（不含题目）
const answerAndAnalysis = computed(() => {
  if (!answerTarget.value?.content) return ''
  const content = answerTarget.value.content
  const answerIdx = content.indexOf('【答案】')
  if (answerIdx < 0) return ''
  return content.slice(answerIdx).trim()
})

const openAnswer = (row) => {
  answerTarget.value = row
  uploadFileList.value = []
  uploadedImages.value = []
  
  // 已作答的题目直接显示结果,未作答的显示作答表单
  if (row.isSolved) {
    // 解析学生答案
    myAnswer.value = row.studentAnswer || ''
    
    // 解析学生上传的图片
    if (row.studentImages) {
      try {
        const imgs = JSON.parse(row.studentImages)
        if (Array.isArray(imgs)) {
          uploadedImages.value = imgs
        }
      } catch (e) {
        console.error('解析图片数据失败:', e)
      }
    }
    
    const hasImages = uploadedImages.value.length > 0
    
    answerResult.value = { 
      correct: row.isCorrect === 1, 
      feedback: hasImages ? `已作答（含 ${uploadedImages.value.length} 张图片答案）` : '已作答' 
    }
  } else {
    myAnswer.value = ''
    answerResult.value = null
  }
  answerVisible.value = true
}

const submitAnswer = async () => {
  if (!myAnswer.value.trim() && uploadedImages.value.length === 0) {
    ElMessage.warning('请先填写答案或上传草稿纸照片')
    return
  }
  answering.value = true
  try {
    const res = await answerVariant(answerTarget.value.id, myAnswer.value.trim(), uploadedImages.value)
    answerResult.value = res.data
    await load()
  } catch (err) {
    // 常见失败:已作答过/答案为空,拦截器已提示
  } finally {
    answering.value = false
  }
}

const errorTypeText = (t) => ['','知识点缺失','计算失误','审题错误','思路错误','表达不清'][t]
const errorTypeColor = (t) => ['','danger','warning','info','warning','info'][t]

const load = async () => {
  try {
    const gradeLevel = userStore.userInfo?.gradeLevel || 0
    const [e, v, s] = await Promise.all([errorBooks(), myVariants(), getSubjects(gradeLevel)])
    errors.value = e.data || []
    variants.value = v.data || []
    subjects.value = s.data || []
  } catch (err) {
    // request.js 拦截器已弹出后端错误信息,这里接住 rejection 即可
  }
}

onMounted(load)

const viewDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

const pushV = async (id, count) => {
  pushingId.value = id
  try {
    await pushVariant(id, count)
    ElMessage.success(`已生成 ${count} 道变式题（AI 生成）`)
    await load()
  } catch (err) {
    // 常见失败:该错题已推送过变式题(后端 BizException),拦截器已提示
  } finally {
    pushingId.value = null
  }
}

const openVariantDialog = (id) => {
  currentErrorBookId.value = id
  variantCount.value = 3
  variantCountVisible.value = true
}

const confirmPushVariant = async () => {
  variantCountVisible.value = false
  await pushV(currentErrorBookId.value, variantCount.value)
}

const deleteV = async (id) => {
  try {
    await deleteVariant(id)
    ElMessage.success('已删除')
    await load()
  } catch (err) {
    // request.js 拦截器已弹出后端错误信息
  }
}

const deleteAllV = async (ids) => {
  try {
    for (const id of ids) {
      await deleteVariant(id)
    }
    ElMessage.success(`已删除 ${ids.length} 道变式题`)
    await load()
  } catch (err) {
    // request.js 拦截器已弹出后端错误信息
  }
}

const deleteError = async (id) => {
  try {
    await deleteErrorBook(id)
    ElMessage.success('已删除错题')
    await load()
  } catch (err) {
    // request.js 拦截器已弹出后端错误信息
  }
}

const markReviewed = async (id, status) => {
  try {
    await reviewError(id, status)
    ElMessage.success('已标记')
    await load()
  } catch (err) {
    // request.js 拦截器已弹出后端错误信息
  }
}
</script>

<style scoped>
.variant-content { background: #f4f9ff; padding: 16px; border-radius: 8px; white-space: pre-wrap; line-height: 1.8; }
.variant-full { background: #fafafa; padding: 16px; border-radius: 8px; border: 1px dashed #dcdfe6; line-height: 1.8; }
.variant-item { padding: 12px 0; border-bottom: 1px solid #f0f0f0; }
.variant-item:last-child { border-bottom: none; }
</style>
