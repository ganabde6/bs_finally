<template>
  <BaseLayout :menus="menus" />
</template>
<script setup>
import BaseLayout from './BaseLayout.vue'
const menus = [
  { path: '/student/dashboard', title: '学习首页', icon: 'HomeFilled' },
  { path: '/student/paper', title: '作业/考试', icon: 'Document' },
  { path: '/student/tutor', title: 'AI答疑', icon: 'ChatDotRound' },
  { path: '/student/listeningSpeaking', title: '英语听说', icon: 'Microphone' },
  { path: '/student/errorbook', title: '错题本', icon: 'EditPen' },
  { path: '/student/study', title: '学情中心', icon: 'TrendCharts' },
  { path: '/student/selfPractice', title: '自主智练', icon: 'Notebook' },
  { path: '/student/pk', title: '同学PK', icon: 'Trophy' },
  { path: '/student/pkLs', title: '听说PK', icon: 'Microphone' }
]
</script>
