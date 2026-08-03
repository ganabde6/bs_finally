<template>
  <BaseLayout :menus="menus" />
</template>
<script setup>
import BaseLayout from './BaseLayout.vue'
const menus = [
  { path: '/student/dashboard', title: '学习首页', icon: 'HomeFilled' },
  { path: '/student/paper', title: '作业/考试', icon: 'Document' },
  { path: '/student/tutor', title: 'AI助学', icon: 'ChatDotRound' },
  { path: '/student/errorbook', title: '错题本', icon: 'EditPen' },
  { path: '/student/study', title: '学情中心', icon: 'TrendCharts' },
  { path: '/student/selfPractice', title: '自主智练', icon: 'Notebook' },
  { path: '/student/pkArena', title: '同学PK', icon: 'Trophy' }
]
</script>
