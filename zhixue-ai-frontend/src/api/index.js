import request from './request'

// ============ 认证 ============
export const login = (data) => request.post('/api/auth/login', data)
export const register = (data) => request.post('/api/auth/register', data)
export const getUserInfo = () => request.get('/api/auth/info')
export const updateProfile = (data) => request.put('/api/auth/profile', data)
export const changePassword = (data) => request.put('/api/auth/password', data)
export const logout = () => request.post('/api/auth/logout')

// ============ 公共 ============
export const uploadFile = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/api/common/upload', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}
export const getSubjects = (gradeLevel) => request.get('/api/common/subjects', { params: { gradeLevel } })
export const getClasses = (gradeLevel) => request.get('/api/common/classes', { params: { gradeLevel } })
export const getRoles = () => request.get('/api/common/roles')

// ============ 学生端 ============
export const studentDashboard = () => request.get('/api/student/dashboard')
export const studentPapers = () => request.get('/api/student/papers')
export const studentPaperDetail = (id) => request.get(`/api/student/paper/${id}`)
export const startAnswer = (paperId) => request.post(`/api/student/answer/start/${paperId}`)
export const submitAnswer = (answerId, data) => request.post(`/api/student/answer/${answerId}/submit`, data)
export const myAnswers = () => request.get('/api/student/answers')
export const correctDetail = (answerId) => request.get(`/api/student/answer/${answerId}/correct`)
export const errorBooks = () => request.get('/api/student/errorbooks')
export const reviewError = (id, status) => request.put(`/api/student/errorbook/${id}/review?status=${status}`)
export const deleteErrorBook = (id) => request.delete(`/api/student/errorbook/${id}`)
export const pushVariant = (id, count = 1) => request.post(`/api/student/errorbook/${id}/variant`, null, { 
  params: { count },
  timeout: 120000 // AI 生成变式题需要更长时间，设置为 120 秒
})
export const myVariants = () => request.get('/api/student/variants')
export const deleteVariant = (id) => request.delete(`/api/student/variant/${id}`)
export const answerVariant = (id, answer, images = []) => request.post(`/api/student/variant/${id}/answer`, { answer, images }, { timeout: 120000 })
export const studyAnalysis = (subjectId) => request.get('/api/student/study/analysis', { params: { subjectId } })
export const tutorChat = (data) => request.post('/api/student/tutor/chat', data, { timeout: 120000 })
export const polishEssay = (content) => request.post('/api/student/tutor/polish', { content }, { timeout: 120000 })
export const polishEssayWithImage = (imageBase64, content) => request.post('/api/student/tutor/polish', { content, imageBase64 }, { timeout: 120000 })
export const chatHistory = () => request.get('/api/student/tutor/history')
export const reportRisk = (data) => request.post('/api/student/risk/report', data)

// ============ 自主智练与自律打卡 ============
export const generatePractice = () => request.post('/api/student/practice/generate')
export const generatePracticeConfig = (data) => request.post('/api/student/practice/generate-config', data)
export const getKnowledgePoints = (subjectId) => request.get('/api/student/practice/knowledge-points', { params: { subjectId } })
export const getRecentPracticeRecords = () => request.get('/api/student/practice/recent-records')
export const submitPractice = (data) => request.post('/api/student/practice/submit', data)
export const checkInStatus = () => request.get('/api/student/checkin/status')
export const doCheckIn = () => request.post('/api/student/checkin/do')

// ============ 同学PK ============
export const pkCreateRoom = (data) => request.post('/api/student/pk/create', data)
export const pkJoinRoom = (data) => request.post('/api/student/pk/join', data)
export const pkGetQuestions = (roomCode) => request.get('/api/student/pk/questions', { params: { roomCode } })
export const pkSubmitAnswer = (data) => request.post('/api/student/pk/answer', data)
export const pkGetRanking = (roomCode) => request.get('/api/student/pk/ranking', { params: { roomCode } })
export const pkGetRoomStatus = (roomCode) => request.get('/api/student/pk/status', { params: { roomCode } })

// ============ 英语听说练习 ============
export const listeningSpeakingList = (gradeLevel) => request.get('/api/student/listening-speaking/list', { params: { gradeLevel } })
export const listeningSpeakingDetail = (id) => request.get(`/api/student/listening-speaking/${id}`)
export const submitListeningSpeaking = (id, file, supplementText) => {
  const fd = new FormData()
  fd.append('file', file)
  if (supplementText) fd.append('supplementText', supplementText)
  return request.post(`/api/student/listening-speaking/${id}/submit`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000 // AI 语音识别 + 评分耗时较长
  })
}
export const listeningSpeakingRecords = () => request.get('/api/student/listening-speaking/records')
export const uploadAudioFile = (file) => {
  const fd = new FormData()
  fd.append('file', file)
  return request.post('/api/common/upload/audio', fd, { headers: { 'Content-Type': 'multipart/form-data' } })
}

// 学生自主出题
export const lsGenerateFromText = (data) => request.post('/api/student/listening-speaking/generate/text', data, { timeout: 120000 })
export const lsGenerateFromTopic = (data) => request.post('/api/student/listening-speaking/generate/topic', data, { timeout: 120000 })
export const lsGenerateFromImage = (data) => request.post('/api/student/listening-speaking/generate/image', data, { timeout: 120000 })
export const lsGenerateSimilar = (data) => request.post('/api/student/listening-speaking/generate/similar', data, { timeout: 120000 })
export const lsGetTopics = (gradeLevel) => request.get('/api/student/listening-speaking/topics', { params: { gradeLevel } })

// 教师听说作业
export const lsHomeworkCreate = (data) => request.post('/api/teacher/listening-speaking-homework/create', data)
export const lsHomeworkGenerate = (data) => request.post('/api/teacher/listening-speaking-homework/generate', data, { timeout: 120000 })
export const lsHomeworkSaveQuestions = (homeworkId, questions) => request.post(`/api/teacher/listening-speaking-homework/${homeworkId}/questions`, questions)
export const lsHomeworkDetail = (homeworkId) => request.get(`/api/teacher/listening-speaking-homework/${homeworkId}`)
export const lsHomeworkPublish = (homeworkId) => request.post(`/api/teacher/listening-speaking-homework/${homeworkId}/publish`)
export const lsHomeworkList = () => request.get('/api/teacher/listening-speaking-homework/list')
export const lsHomeworkCopy = (homeworkId, regenerate) => request.post(`/api/teacher/listening-speaking-homework/${homeworkId}/copy`, null, { params: { regenerate } })
export const lsHomeworkReport = (homeworkId) => request.get(`/api/teacher/listening-speaking-homework/${homeworkId}/report`)
export const lsHomeworkSubmit = (questionId, file, supplementText) => {
  const fd = new FormData()
  fd.append('file', file)
  if (supplementText) fd.append('supplementText', supplementText)
  return request.post(`/api/teacher/listening-speaking-homework/submit/${questionId}`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}
export const lsStudentHomeworkList = () => request.get('/api/teacher/listening-speaking-homework/student/list')
export const lsStudentHomeworkRecords = (homeworkId) => request.get(`/api/teacher/listening-speaking-homework/student/${homeworkId}/records`)

// 英语听说 PK
export const pkLsCreate = (data) => request.post('/api/student/pk-ls/create', data)
export const pkLsAccept = (roomCode) => request.post('/api/student/pk-ls/accept', { roomCode })
export const pkLsDetail = (roomCode) => request.get('/api/student/pk-ls/detail', { params: { roomCode } })
export const pkLsSubmit = (roomCode, file, supplementText) => {
  const fd = new FormData()
  fd.append('file', file)
  if (supplementText) fd.append('supplementText', supplementText)
  return request.post(`/api/student/pk-ls/submit?roomCode=${roomCode}`, fd, {
    headers: { 'Content-Type': 'multipart/form-data' },
    timeout: 120000
  })
}
export const pkLsResult = (roomCode) => request.get('/api/student/pk-ls/result', { params: { roomCode } })
export const pkLsList = () => request.get('/api/student/pk-ls/list')

// ============ 教师端 ============
export const teacherDashboard = () => request.get('/api/teacher/dashboard')
export const pageQuestions = (params) => request.get('/api/teacher/questions', { params })
export const getQuestion = (id) => request.get(`/api/teacher/question/${id}`)
export const addQuestion = (data) => request.post('/api/teacher/question', data)
export const updateQuestion = (data) => request.put('/api/teacher/question', data)
export const deleteQuestion = (id) => request.delete(`/api/teacher/question/${id}`)
export const aiGroup = (data) => request.post('/api/teacher/questions/ai-group', data)
export const pagePapers = (params) => request.get('/api/teacher/papers', { params })
export const teacherPaperDetail = (id) => request.get(`/api/teacher/paper/${id}`)
export const createPaper = (data) => request.post('/api/teacher/paper', data)
export const updatePaper = (data) => request.put('/api/teacher/paper', data)
export const deletePaper = (id) => request.delete(`/api/teacher/paper/${id}`)
export const publishPaper = (id) => request.put(`/api/teacher/paper/${id}/publish`)
export const finishPaper = (id) => request.put(`/api/teacher/paper/${id}/finish`)
export const paperAnswers = (paperId) => request.get(`/api/teacher/correct/${paperId}/answers`)
export const teacherCorrectDetail = (answerId) => request.get(`/api/teacher/correct/${answerId}`)
export const batchCorrect = (paperId) => request.post(`/api/teacher/correct/${paperId}/batch`)
export const adjustCorrect = (correctId, data) => request.put(`/api/teacher/correct/${correctId}/adjust`, data)
export const similarityCheck = (paperId) => request.post(`/api/teacher/correct/${paperId}/similarity`)
export const classAnalysis = (classId, subjectId) => request.get(`/api/teacher/class/${classId}/analysis`, { params: { subjectId } })
export const feedback = (studentId) => request.get(`/api/teacher/feedback/${studentId}`)
export const teacherClassStudents = (classId) => request.get(`/api/teacher/class/${classId}/students`)

// ============ 教师学员管理 ============
export const teacherMyClasses = () => request.get('/api/teacher/my-classes')
export const teacherPageStudents = (params) => request.get('/api/teacher/students', { params })
export const teacherAddStudent = (data) => request.post('/api/teacher/student', data)
export const teacherUpdateStudent = (data) => request.put('/api/teacher/student', data)
export const teacherDeleteStudent = (id) => request.delete(`/api/teacher/student/${id}`)
export const teacherResetStudentPassword = (id, password) => request.put(`/api/teacher/student/${id}/reset-password`, { password })

// ============ 管理端 ============
export const adminDashboard = () => request.get('/api/admin/dashboard')
export const paperDistribution = () => request.get('/api/admin/dashboard/paper-distribution')
export const classRanking = () => request.get('/api/admin/dashboard/class-ranking')
export const riskDistribution = () => request.get('/api/admin/dashboard/risk-distribution')
export const recentLogs = () => request.get('/api/admin/dashboard/recent-logs')

export const pageUsers = (params) => request.get('/api/admin/users', { params })
export const getUser = (id) => request.get(`/api/admin/user/${id}`)
export const addUser = (data) => request.post('/api/admin/user', data)
export const updateUser = (data) => request.put('/api/admin/user', data)
export const deleteUser = (id) => request.delete(`/api/admin/user/${id}`)
export const resetPassword = (id, password) => request.put(`/api/admin/user/${id}/reset-password`, { password })
export const classStudents = (classId) => request.get(`/api/admin/class/${classId}/students`)

export const roles = () => request.get('/api/admin/roles')
export const addRole = (data) => request.post('/api/admin/role', data)
export const updateRole = (data) => request.put('/api/admin/role', data)
export const deleteRole = (id) => request.delete(`/api/admin/role/${id}`)
export const permissions = () => request.get('/api/admin/permissions')
export const rolePermissions = (roleId) => request.get(`/api/admin/role/${roleId}/permissions`)
export const assignPermissions = (roleId, permissionIds) => request.put(`/api/admin/role/${roleId}/permissions`, { permissionIds })

export const adminClasses = () => request.get('/api/admin/classes')
export const addClass = (data) => request.post('/api/admin/class', data)
export const updateClass = (data) => request.put('/api/admin/class', data)
export const deleteClass = (id) => request.delete(`/api/admin/class/${id}`)

export const adminSubjects = () => request.get('/api/admin/subjects')
export const addSubject = (data) => request.post('/api/admin/subject', data)
export const updateSubject = (data) => request.put('/api/admin/subject', data)
export const deleteSubject = (id) => request.delete(`/api/admin/subject/${id}`)
export const teacherCourses = (teacherId) => request.get(`/api/admin/teacher/${teacherId}/courses`)
export const assignCourse = (data) => request.post('/api/admin/teacher-course', data)
export const removeCourse = (id) => request.delete(`/api/admin/teacher-course/${id}`)

export const aiConfigs = () => request.get('/api/admin/ai-configs')
export const updateAiConfig = (data) => request.put('/api/admin/ai-config', data)

export const pageNotices = (params) => request.get('/api/admin/notices', { params })
export const addNotice = (data) => request.post('/api/admin/notice', data)
export const updateNotice = (data) => request.put('/api/admin/notice', data)
export const deleteNotice = (id) => request.delete(`/api/admin/notice/${id}`)

export const moderationCheck = (content) => request.post('/api/admin/moderation/check', { content })

export const pageLogs = (params) => request.get('/api/admin/logs', { params })
