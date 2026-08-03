<template>
  <el-card>
    <template #header><span>智能错题本</span></template>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="错题列表" name="errors">
        <el-table :data="errors" stripe>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="题目" min-width="300">
            <template #default="{row}">{{ row.question?.content }}</template>
          </el-table-column>
          <el-table-column label="错误类型" width="120">
            <template #default="{row}">
              <el-tag :type="errorTypeColor(row.errorBook.errorType)">{{ errorTypeText(row.errorBook.errorType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="知识点" width="140">
            <template #default="{row}">{{ row.question?.knowledgePoint }}</template>
          </el-table-column>
          <el-table-column label="复盘状态" width="100">
            <template #default="{row}">
              <el-tag :type="['info','','success'][row.errorBook.reviewStatus]">{{ ['未复盘','已复盘','已掌握'][row.errorBook.reviewStatus] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240">
            <template #default="{row}">
              <el-button size="small" @click="viewDetail(row)">查看详情</el-button>
              <el-button size="small" type="success" :loading="pushingId === row.errorBook.id" @click="pushV(row.errorBook.id)">
                {{ pushingId === row.errorBook.id ? 'AI 生成中...' : '推送变式题' }}
              </el-button>
              <el-button size="small" type="primary" @click="markReviewed(row.errorBook.id, 2)">已掌握</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="变式题" name="variants">
        <el-table :data="variants" stripe>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="变式题" min-width="300">
            <template #default="{row}">
              <span style="white-space:pre-wrap">{{ displayContent(row) }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="knowledgePoint" label="知识点" width="140" />
          <el-table-column label="状态" width="100">
            <template #default="{row}"><el-tag :type="row.isSolved ? 'success' : 'info'">{{ row.isSolved ? '已作答' : '未作答' }}</el-tag></template>
          </el-table-column>
          <el-table-column label="操作" width="110">
            <template #default="{row}">
              <el-button v-if="!row.isSolved" size="small" type="primary" @click="openAnswer(row)">作答</el-button>
              <el-tag v-else :type="row.isCorrect ? 'success' : 'danger'">{{ row.isCorrect ? '答对了' : '答错了' }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="answerVisible" title="变式题作答" width="700">
      <div v-if="answerTarget">
        <div class="variant-content">{{ displayContent(answerTarget) }}</div>
        <template v-if="!answerResult">
          <el-input v-model="myAnswer" type="textarea" :rows="4" placeholder="请输入你的答案" class="mt-20" />
        </template>
        <template v-else>
          <el-alert class="mt-20" :type="answerResult.correct ? 'success' : 'error'"
                    :title="answerResult.correct ? '回答正确' : '回答错误'"
                    :description="answerResult.feedback" :closable="false" />
          <div class="mt-20 variant-full">
            <strong>完整题目与解析:</strong>
            <div style="white-space:pre-wrap;margin-top:8px">{{ answerTarget.content }}</div>
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
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { errorBooks, myVariants, pushVariant, reviewError, answerVariant } from '@/api'

const activeTab = ref('errors')
const errors = ref([])
const variants = ref([])
const detailVisible = ref(false)
const current = ref(null)
const pushingId = ref(null)

// 变式题作答
const answerVisible = ref(false)
const answerTarget = ref(null)
const myAnswer = ref('')
const answering = ref(false)
const answerResult = ref(null)

// AI 生成的变式题内容内含【答案】【解析】段,未作答时不展示,防止剧透
const displayContent = (row) => {
  if (!row || !row.content) return ''
  if (row.isSolved) return row.content
  const idx = row.content.indexOf('【答案】')
  return idx > 0 ? row.content.slice(0, idx).trim() : row.content
}

const openAnswer = (row) => {
  answerTarget.value = row
  myAnswer.value = ''
  answerResult.value = null
  answerVisible.value = true
}

const submitAnswer = async () => {
  if (!myAnswer.value.trim()) {
    ElMessage.warning('请先填写答案')
    return
  }
  answering.value = true
  try {
    const res = await answerVariant(answerTarget.value.id, myAnswer.value.trim())
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
    const [e, v] = await Promise.all([errorBooks(), myVariants()])
    errors.value = e.data || []
    variants.value = v.data || []
  } catch (err) {
    // request.js 拦截器已弹出后端错误信息,这里接住 rejection 即可
  }
}

onMounted(load)

const viewDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

const pushV = async (id) => {
  pushingId.value = id
  try {
    await pushVariant(id)
    ElMessage.success('变式题已推送（AI 生成）')
    await load()
  } catch (err) {
    // 常见失败:该错题已推送过变式题(后端 BizException),拦截器已提示
  } finally {
    pushingId.value = null
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
</style>
