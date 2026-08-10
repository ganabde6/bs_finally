<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">智能家校反馈</h1>
        <p class="page-subtitle">AI 生成学生学情反馈单,一键发送给家长</p>
      </div>
    </div>
    <el-card class="content-card">
    <el-form :inline="true" class="mb-20">
      <el-form-item label="选择学生">
        <el-select v-model="studentId" filterable placeholder="请选择学生" @change="loadFeedback">
          <el-option v-for="s in students" :key="s.id" :label="s.realName + '(' + s.username + ')'" :value="s.id" />
        </el-select>
      </el-form-item>
    </el-form>
    <div v-if="feedbackText" class="feedback-box">{{ feedbackText }}</div>
    <el-empty v-else description="请选择学生查看 AI 生成的学情反馈单" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getClasses, teacherClassStudents, feedback } from '@/api'

const classes = ref([])
const students = ref([])
const studentId = ref(null)
const feedbackText = ref('')

onMounted(async () => {
  const c = await getClasses()
  classes.value = c.data
  if (classes.value.length) {
    const s = await teacherClassStudents(classes.value[0].id)
    students.value = s.data
  }
})

const loadFeedback = async () => {
  if (!studentId.value) return
  const res = await feedback(studentId.value)
  feedbackText.value = res.data
}
</script>

<style scoped>
.feedback-box { background: #F0FDFA; padding: 24px; border-radius: 8px; line-height: 1.8; white-space: pre-wrap; border: 1px dashed #0D9488; }
</style>
