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
      <el-form-item label="选择班级">
        <el-select v-model="classId" placeholder="请选择班级" style="width:160px" @change="onClassChange">
          <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="选择学生">
        <el-select v-model="studentId" filterable :loading="studentsLoading" placeholder="请先选择班级" :disabled="!classId" style="width:220px" @change="loadFeedback">
          <el-option v-for="s in students" :key="s.id" :label="s.realName + '(' + s.username + ')'" :value="s.id" />
        </el-select>
        <span v-if="classId && !studentsLoading" class="student-count">共 {{ students.length }} 名学生</span>
      </el-form-item>
    </el-form>
    <div v-if="feedbackText" class="feedback-box">{{ feedbackText }}</div>
    <el-empty v-else-if="classId && !studentsLoading && !students.length" description="该班级暂无学生,请选择其他班级" />
    <el-empty v-else description="请选择学生查看 AI 生成的学情反馈单" />
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { getClasses, teacherClassStudents, feedback } from '@/api'

const classes = ref([])
const students = ref([])
const classId = ref(null)
const studentId = ref(null)
const feedbackText = ref('')
const studentsLoading = ref(false)

onMounted(async () => {
  try {
    const c = await getClasses()
    classes.value = c.data || []
    if (classes.value.length) {
      classId.value = classes.value[0].id
      await loadStudents(classId.value)
    }
  } catch (e) {
    console.error('加载班级列表失败:', e)
  }
})

const loadStudents = async (cid) => {
  studentId.value = null
  feedbackText.value = ''
  studentsLoading.value = true
  try {
    const s = await teacherClassStudents(cid)
    students.value = s.data || []
  } catch (e) {
    console.error('加载学生列表失败:', e)
    students.value = []
  } finally {
    studentsLoading.value = false
  }
}

const onClassChange = (cid) => {
  if (cid) loadStudents(cid)
}

const loadFeedback = async () => {
  if (!studentId.value) return
  try {
    const res = await feedback(studentId.value)
    feedbackText.value = res.data
  } catch (e) {
    console.error('生成反馈失败:', e)
    feedbackText.value = ''
  }
}
</script>

<style scoped>
.feedback-box { background: #F0FDFA; padding: 24px; border-radius: 8px; line-height: 1.8; white-space: pre-wrap; border: 1px dashed #0D9488; }
.student-count { margin-left: 10px; color: #909399; font-size: 13px; }
</style>
