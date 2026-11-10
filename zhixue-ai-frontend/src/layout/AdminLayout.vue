<template>
  <BaseLayout :menus="visibleMenus" />
</template>
<script setup>
import { computed } from 'vue'
import BaseLayout from './BaseLayout.vue'
import { useUserStore } from '@/stores/user'

const userStore = useUserStore()

const menus = [
  { path: '/admin/dashboard', title: '数据大屏', icon: 'DataLine' },
  { path: '/admin/user', title: '用户管理', icon: 'User' },
  { path: '/admin/class', title: '班级管理', icon: 'OfficeBuilding' },
  { path: '/admin/course', title: '课程管理', icon: 'Reading' },
  { path: '/admin/homework', title: '作业监测', icon: 'DocumentChecked' },
  { path: '/admin/notice', title: '公告管理', icon: 'Bell' },
  { path: '/admin/moderation', title: '内容风控', icon: 'WarnTriangleFilled' },
  { path: '/admin/log', title: '系统日志', icon: 'List' },
  { path: '/admin/task', title: '任务派发', icon: 'Memo' }
]

// 按登录用户返回的权限(菜单类 permType=1,以 path 匹配)过滤侧边栏菜单
const visibleMenus = computed(() => {
  const perms = userStore.permissions || []
  const permPaths = new Set(perms.map(p => p.path))
  return menus.filter(m => permPaths.has(m.path))
})
</script>
