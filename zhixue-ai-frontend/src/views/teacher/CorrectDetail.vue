<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">批改详情</h1>
        <p class="page-subtitle">逐题查看 AI 评分与学生作答,可人工微调</p>
      </div>
      <div class="page-header-right">
        <el-button @click="$router.back()">返回</el-button>
      </div>
    </div>
    <el-card v-loading="loading" class="content-card">
    <div v-for="(d, i) in details" :key="d.record.id" class="correct-item">
      <div class="question-box">
        <el-tag size="small">{{ typeText(d.question.questionType) }}</el-tag>
        <span class="ml-10">{{ i + 1 }}. <span v-html="renderLatex(d.question.content)"></span></span>
        <span style="color:#909399;margin-left:8px">(满分 {{ d.record.fullScore }})</span>
      </div>
      <div class="mt-20"><strong>学生答案:</strong><div class="answer-box">{{ d.record.studentAnswer || '(未作答)' }}</div></div>
      <div class="mt-20"><strong>标准答案:</strong><span v-html="renderLatex(d.question.standardAnswer)"></span></div>
      <div class="mt-20"><strong>AI 评分:</strong>
        <el-input-number v-model="d.record.score" :precision="2" :min="0" :max="d.record.fullScore" size="small" style="width:120px;margin:0 10px" />
        <el-tag :type="['danger','success','warning'][d.record.isCorrect]">{{ ['错误','正确','部分对'][d.record.isCorrect] }}</el-tag>
      </div>
      <div class="mt-20"><strong>批改备注:</strong>
        <el-input v-model="d.record.correctRemark" type="textarea" :rows="2" style="margin-top:6px" />
      </div>
      <div class="mt-20"><strong>得分点详情:</strong><pre>{{ formatDetail(d.record.scoreDetail) }}</pre></div>
      <div class="mt-20"><el-button type="primary" size="small" @click="saveAdjust(d.record)">保存微调</el-button></div>
    </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import { teacherCorrectDetail, adjustCorrect } from '@/api'
import { renderLatex } from '@/utils/latex'

const route = useRoute()
const details = ref([])
const loading = ref(false)
const types = [
  { value: 1, label: '单选题' }, { value: 2, label: '多选题' },
  { value: 4, label: '填空题' }, { value: 5, label: '简答题' }, { value: 6, label: '作文题' }, { value: 7, label: '计算题' }
]
const typeText = (t) => types.find(x => x.value === t)?.label || ''
const formatDetail = (s) => s ? JSON.stringify(JSON.parse(s), null, 2) : ''

onMounted(async () => {
  loading.value = true
  try {
    const res = await teacherCorrectDetail(route.params.answerId)
    details.value = res.data
  } finally { loading.value = false }
})

const saveAdjust = async (r) => {
  await adjustCorrect(r.id, { score: r.score, remark: r.correctRemark })
  ElMessage.success('已微调')
}
</script>

<style scoped>
.correct-item { padding: 16px 0; border-bottom: 1px dashed #E8F1F4; }
.question-box { font-size: 15px; line-height: 1.8; }
.answer-box { background: #E8F1F4; padding: 10px; border-radius: 4px; margin-top: 6px; white-space: pre-wrap; }
pre { background: #E8F1F4; padding: 10px; border-radius: 4px; margin-top: 6px; font-size: 12px; }
.ml-10 { margin-left: 10px; }
</style>
