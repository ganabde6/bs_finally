import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', name: 'Login', component: () => import('@/views/Login.vue') },

  // 学生端
  {
    path: '/student',
    component: () => import('@/layout/StudentLayout.vue'),
    redirect: '/student/dashboard',
    children: [
      { path: 'dashboard', name: 'StudentDashboard', component: () => import('@/views/student/Dashboard.vue'), meta: { title: '学习首页' } },
      { path: 'paper', name: 'StudentPaper', component: () => import('@/views/student/PaperList.vue'), meta: { title: '作业/考试' } },
      { path: 'paper/:id', name: 'StudentTakeExam', component: () => import('@/views/student/TakeExam.vue'), meta: { title: '作答' } },
      { path: 'tutor', name: 'StudentTutor', component: () => import('@/views/student/Tutor.vue'), meta: { title: 'AI助学' } },
      { path: 'listeningSpeaking', name: 'StudentListeningSpeaking', component: () => import('@/views/student/ListeningSpeaking.vue'), meta: { title: '英语听说' } },
      { path: 'errorbook', name: 'StudentErrorBook', component: () => import('@/views/student/ErrorBook.vue'), meta: { title: '错题本' } },
      { path: 'study', name: 'StudentStudy', component: () => import('@/views/student/StudyCenter.vue'), meta: { title: '学情中心' } },
      { path: 'selfPractice', name: 'StudentSelfPractice', component: () => import('@/views/student/PracticeConfig.vue'), meta: { title: '自主智练' } },
      { path: 'practicePaper', name: 'StudentPracticePaper', component: () => import('@/views/student/PracticePaper.vue'), meta: { title: '练习作答' } },
      { path: 'pk', name: 'StudentPk', component: () => import('@/views/student/PkArena.vue'), meta: { title: '同学PK' } },
      { path: 'pkLs', name: 'StudentPkLs', component: () => import('@/views/student/PkLs.vue'), meta: { title: '听说PK' } }
    ]
  },

  // 教师端
  {
    path: '/teacher',
    component: () => import('@/layout/TeacherLayout.vue'),
    redirect: '/teacher/dashboard',
    children: [
      { path: 'dashboard', name: 'TeacherDashboard', component: () => import('@/views/teacher/Dashboard.vue'), meta: { title: '教师首页' } },
      { path: 'question', name: 'TeacherQuestion', component: () => import('@/views/teacher/Question.vue'), meta: { title: '题库管理' } },
      { path: 'paper', name: 'TeacherPaper', component: () => import('@/views/teacher/Paper.vue'), meta: { title: '作业考试管理' } },
      { path: 'paper/edit/:id?', name: 'TeacherPaperEdit', component: () => import('@/views/teacher/PaperEdit.vue'), meta: { title: '组卷' } },
      { path: 'correct', name: 'TeacherCorrect', component: () => import('@/views/teacher/Correct.vue'), meta: { title: '批改管理' } },
      { path: 'correct/:answerId', name: 'TeacherCorrectDetail', component: () => import('@/views/teacher/CorrectDetail.vue'), meta: { title: '批改详情' } },
      { path: 'classAnalysis', name: 'TeacherClassAnalysis', component: () => import('@/views/teacher/ClassAnalysis.vue'), meta: { title: '班级学情' } },
      { path: 'feedback', name: 'TeacherFeedback', component: () => import('@/views/teacher/Feedback.vue'), meta: { title: '家校反馈' } },
      { path: 'lsHomework', name: 'TeacherLsHomework', component: () => import('@/views/teacher/LsHomework.vue'), meta: { title: '听说作业' } },
      { path: 'studentManage', name: 'TeacherStudentManage', component: () => import('@/views/teacher/StudentManage.vue'), meta: { title: '学员管理' } }
    ]
  },

  // 管理端
  {
    path: '/admin',
    component: () => import('@/layout/AdminLayout.vue'),
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', name: 'AdminDashboard', component: () => import('@/views/admin/Dashboard.vue'), meta: { title: '数据大屏' } },
      { path: 'user', name: 'AdminUser', component: () => import('@/views/admin/User.vue'), meta: { title: '用户管理' } },
      { path: 'role', name: 'AdminRole', component: () => import('@/views/admin/Role.vue'), meta: { title: '角色权限' } },
      { path: 'class', name: 'AdminClass', component: () => import('@/views/admin/Class.vue'), meta: { title: '班级管理' } },
      { path: 'course', name: 'AdminCourse', component: () => import('@/views/admin/Course.vue'), meta: { title: '课程管理' } },
      { path: 'aiConfig', name: 'AdminAiConfig', component: () => import('@/views/admin/AiConfig.vue'), meta: { title: 'AI配置' } },
      { path: 'notice', name: 'AdminNotice', component: () => import('@/views/admin/Notice.vue'), meta: { title: '公告管理' } },
      { path: 'moderation', name: 'AdminModeration', component: () => import('@/views/admin/Moderation.vue'), meta: { title: '内容风控' } },
      { path: 'log', name: 'AdminLog', component: () => import('@/views/admin/Log.vue'), meta: { title: '系统日志' } }
    ]
  },

  { path: '/:pathMatch(.*)*', redirect: '/login' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.path === '/login') {
    next()
    return
  }
  if (!userStore.token) {
    next('/login')
    return
  }
  // 角色路由校验
  const roleCode = userStore.roleCode
  if (to.path.startsWith('/student') && roleCode !== 'STUDENT') {
    next(userStore.homePath)
    return
  }
  if (to.path.startsWith('/teacher') && roleCode !== 'TEACHER') {
    next(userStore.homePath)
    return
  }
  if (to.path.startsWith('/admin') && roleCode !== 'SUPER_ADMIN' && roleCode !== 'SCHOOL_ADMIN') {
    next(userStore.homePath)
    return
  }
  next()
})

export default router
