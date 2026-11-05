<template>
  <BaseLayout :menus="visibleMenus" />
</template>
<script setup>
import { ref, onMounted } from 'vue'
import BaseLayout from './BaseLayout.vue'
import { teacherMySubjects } from '@/api'

const allMenus = [
  { path: '/teacher/dashboard', title: '教师首页', icon: 'HomeFilled' },
  { path: '/teacher/question', title: '题库管理', icon: 'Files' },
  { path: '/teacher/aiGroup', title: '智能出题', icon: 'MagicStick' },
  { path: '/teacher/paper', title: '作业考试管理', icon: 'Document' },
  { path: '/teacher/lsHomework', title: '听说作业', icon: 'Headset', englishOnly: true },
  { path: '/teacher/correct', title: '批改管理', icon: 'Edit' },
  { path: '/teacher/classAnalysis', title: '班级学情', icon: 'TrendCharts' },
  { path: '/teacher/feedback', title: '家校反馈', icon: 'Message' },
  { path: '/teacher/studentManage', title: '学员管理', icon: 'User' },
  { path: '/teacher/profile', title: '个人信息', icon: 'Postcard' }
]

// 听说作业仅对任教英语的教师可见;加载失败时保持默认可见,不影响其它菜单
const visibleMenus = ref(allMenus)

onMounted(async () => {
  try {
    const res = await teacherMySubjects()
    const subjects = res.data || []
    const teachesEnglish = subjects.some(s => (s.subjectName || '').includes('英语'))
    if (!teachesEnglish) {
      visibleMenus.value = allMenus.filter(m => !m.englishOnly)
    }
  } catch (e) {
    // 学科加载失败时不改变菜单
  }
})
</script>
