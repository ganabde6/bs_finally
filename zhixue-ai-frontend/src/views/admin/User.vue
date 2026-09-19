<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">用户管理</h1>
        <p class="page-subtitle">管理系统账号、角色分配与启用状态</p>
      </div>
      <div class="page-header-right">
        <el-button type="primary" :icon="Plus" @click="openAdd">新增用户</el-button>
      </div>
    </div>
    <el-card class="content-card">
    <el-form :inline="true" class="mb-20">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="账号/姓名" clearable style="width:180px" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.roleId" placeholder="全部" clearable style="width:140px">
          <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="班级">
        <el-select v-model="query.classId" placeholder="全部" clearable style="width:140px">
          <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
    </el-form>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="username" label="账号" width="120" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column label="角色" width="100"><template #default="{row}">{{ roleText(row.roleId) }}</template></el-table-column>
      <el-table-column label="班级" width="120"><template #default="{row}">{{ row.roleId === 3 ? (row.className || '无任课') : classText(row.classId) }}</template></el-table-column>
      <el-table-column prop="phone" label="手机" width="130" />
      <el-table-column label="状态" width="80">
        <template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLogin" label="最近登录" width="160" />
      <el-table-column label="操作" width="280" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="primary" plain v-if="row.roleId === 4" @click="openTransfer(row)">调班</el-button>
          <el-button size="small" type="primary" plain v-if="row.roleId === 3" @click="openTeacherTransfer(row)">调班</el-button>
          <el-button size="small" type="warning" @click="openReset(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="mt-20" v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadData" />

    <!-- 新增/编辑 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑用户' : '新增用户'" width="560">
      <el-form :model="form" label-width="100px">
        <el-form-item label="账号" v-if="!form.id"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="form.roleId"><el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" /></el-select></el-form-item>
        <el-form-item label="班级" v-if="form.roleId === 4"><el-select v-model="form.classId" clearable><el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="400">
      <el-form label-width="100px">
        <el-form-item label="账号"><span>{{ resetForm.username }}</span></el-form-item>
        <el-form-item label="新密码"><el-input v-model="resetForm.password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="doReset">确认重置</el-button>
      </template>
    </el-dialog>

    <!-- 调班 -->
    <el-dialog v-model="transferVisible" title="学生调班" width="420">
      <el-form label-width="100px">
        <el-form-item label="学生"><span>{{ transferForm.realName }}（{{ transferForm.username }}）</span></el-form-item>
        <el-form-item label="当前班级"><span>{{ classText(transferForm.fromClassId) || '无' }}</span></el-form-item>
        <el-form-item label="新班级">
          <el-select v-model="transferForm.classId" placeholder="请选择新班级" style="width:100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="transferVisible = false">取消</el-button>
        <el-button type="primary" :loading="transferring" @click="doTransfer">确认调班</el-button>
      </template>
    </el-dialog>

    <!-- 教师调班(调整任课班级) -->
    <el-dialog v-model="teacherTransferVisible" title="教师调班" width="640">
      <el-form label-width="100px">
        <el-form-item label="教师"><span>{{ teacherTransferForm.realName }}（{{ teacherTransferForm.username }}）</span></el-form-item>
      </el-form>
      <el-table :data="teacherCoursesList" size="small" v-loading="teacherCoursesLoading">
        <el-table-column label="当前班级"><template #default="{row}">{{ classText(row.classId) }}</template></el-table-column>
        <el-table-column label="学科"><template #default="{row}">{{ subjectText(row.subjectId) }}</template></el-table-column>
        <el-table-column label="操作" width="100">
          <template #default="{row}">
            <el-button size="small" type="danger" @click="removeTeacherCourseRow(row)">移除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-divider />
      <el-form label-width="100px">
        <el-form-item label="调至班级">
          <el-select v-model="newCourse.classId" placeholder="请选择新班级" style="width:100%">
            <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="任教学科">
          <el-select v-model="newCourse.subjectId" placeholder="请选择学科" style="width:100%">
            <el-option v-for="s in subjects" :key="s.id" :label="s.subjectName" :value="s.id" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" plain @click="addTeacherCourse">新增任课</el-button>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="teacherTransferVisible = false">关闭</el-button>
      </template>
    </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { pageUsers, addUser, updateUser, deleteUser, resetPassword, getRoles, getClasses, getSubjects, teacherCourses, assignCourse, removeCourse } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const roles = ref([])
const classes = ref([])
const subjects = ref([])
const query = reactive({ current: 1, size: 10, keyword: '', roleId: null, classId: null })
const formVisible = ref(false)
const form = reactive({})
const resetVisible = ref(false)
const resetForm = reactive({ id: null, username: '', password: '' })

onMounted(async () => {
  const [r, c, s] = await Promise.all([getRoles(), getClasses(), getSubjects()])
  roles.value = r.data
  classes.value = c.data
  subjects.value = s.data
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await pageUsers(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const roleText = (id) => roles.value.find(r => r.id === id)?.roleName || '-'
const classText = (id) => classes.value.find(c => c.id === id)?.className || '-'

const openAdd = () => {
  Object.keys(form).forEach(k => delete form[k])
  form.roleId = roles.value[0]?.id
  form.status = 1
  formVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  formVisible.value = true
}

const save = async () => {
  if (!form.username || !form.realName || !form.roleId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.id) await updateUser(form)
  else await addUser(form)
  ElMessage.success('保存成功')
  formVisible.value = false
  loadData()
}

const del = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该用户?', '提示', { type: 'warning' })
  } catch { return }
  await deleteUser(id)
  ElMessage.success('已删除')
  loadData()
}

const openReset = (row) => {
  resetForm.id = row.id
  resetForm.username = row.username
  resetForm.password = ''
  resetVisible.value = true
}

// ============ 调班 ============
const transferVisible = ref(false)
const transferring = ref(false)
const transferForm = reactive({ id: null, username: '', realName: '', fromClassId: null, classId: null })

const openTransfer = (row) => {
  transferForm.id = row.id
  transferForm.username = row.username
  transferForm.realName = row.realName
  transferForm.fromClassId = row.classId
  transferForm.classId = null
  transferVisible.value = true
}

const doTransfer = async () => {
  if (!transferForm.classId) {
    ElMessage.warning('请选择新班级')
    return
  }
  if (transferForm.classId === transferForm.fromClassId) {
    ElMessage.warning('新班级与当前班级相同')
    return
  }
  transferring.value = true
  try {
    await updateUser({ id: transferForm.id, classId: transferForm.classId })
    ElMessage.success(`已将 ${transferForm.realName} 调至 ${classText(transferForm.classId)}`)
    transferVisible.value = false
    loadData()
  } finally {
    transferring.value = false
  }
}

// ============ 教师调班(调整任课班级) ============
const teacherTransferVisible = ref(false)
const teacherCoursesLoading = ref(false)
const teacherTransferForm = reactive({ id: null, username: '', realName: '' })
const teacherCoursesList = ref([])
const newCourse = reactive({ classId: null, subjectId: null })

const subjectText = (id) => subjects.value.find(s => s.id === id)?.subjectName || '-'

const openTeacherTransfer = async (row) => {
  teacherTransferForm.id = row.id
  teacherTransferForm.username = row.username
  teacherTransferForm.realName = row.realName
  newCourse.classId = null
  newCourse.subjectId = null
  teacherTransferVisible.value = true
  loadTeacherCourses()
}

const loadTeacherCourses = async () => {
  teacherCoursesLoading.value = true
  try {
    const res = await teacherCourses(teacherTransferForm.id)
    teacherCoursesList.value = res.data || []
  } finally {
    teacherCoursesLoading.value = false
  }
}

const addTeacherCourse = async () => {
  if (!newCourse.classId || !newCourse.subjectId) {
    ElMessage.warning('请选择新班级和学科')
    return
  }
  await assignCourse({ teacherId: teacherTransferForm.id, classId: newCourse.classId, subjectId: newCourse.subjectId })
  ElMessage.success(`已将 ${teacherTransferForm.realName} 调至 ${classText(newCourse.classId)}`)
  newCourse.classId = null
  newCourse.subjectId = null
  loadTeacherCourses()
}

const removeTeacherCourseRow = async (row) => {
  try {
    await ElMessageBox.confirm(`确定移除 ${classText(row.classId)} 的任课?`, '提示', { type: 'warning' })
  } catch { return }
  await removeCourse(row.id)
  ElMessage.success('已移除')
  loadTeacherCourses()
}

const doReset = async () => {
  if (!resetForm.password) {
    ElMessage.warning('请输入新密码')
    return
  }
  await resetPassword(resetForm.id, resetForm.password)
  ElMessage.success('密码已重置')
  resetVisible.value = false
}
</script>
