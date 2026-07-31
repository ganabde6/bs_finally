<template>
  <BaseLayout :menus="menus" />
</template>
<script setup>
import BaseLayout from './BaseLayout.vue'
const menus = [
  { path: '/teacher/dashboard', title: '教师首页', icon: 'HomeFilled' },
  { path: '/teacher/question', title: '题库管理', icon: 'Files' },
  { path: '/teacher/paper', title: '作业考试管理', icon: 'Document' },
  { path: '/teacher/correct', title: '批改管理', icon: 'Edit' },
  { path: '/teacher/classAnalysis', title: '班级学情', icon: 'TrendCharts' },
  { path: '/teacher/feedback', title: '家校反馈', icon: 'Message' }
]
</script>
