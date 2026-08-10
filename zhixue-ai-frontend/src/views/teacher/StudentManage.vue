<template>
  <div class="student-manage page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">学员管理</h1>
        <p class="page-subtitle">管理所教班级的学员账号与状态</p>
      </div>
      <div class="page-header-right">
        <el-button type="primary" @click="handleAdd">
          <el-icon><Plus /></el-icon>新增学员
        </el-button>
      </div>
    </div>
    <el-card class="content-card">

      <!-- 筛选区 -->
      <el-form :inline="true" :model="queryForm" class="filter-form">
        <el-form-item label="班级">
          <el-select v-model="queryForm.classId" placeholder="全部班级" clearable>
            <el-option
              v-for="cls in myClasses"
              :key="cls.id"
              :label="cls.className"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input
            v-model="queryForm.keyword"
            placeholder="姓名/学号"
            clearable
            @keyup.enter="loadData"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="loadData">
            <el-icon><Search /></el-icon>查询
          </el-button>
          <el-button @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>

      <!-- 表格 -->
      <el-table :data="tableData" v-loading="loading" border stripe>
        <el-table-column prop="username" label="学号" width="120" />
        <el-table-column prop="realName" label="姓名" width="120" />
        <el-table-column label="班级" width="150">
          <template #default="{ row }">
            {{ getClassLabel(row.classId) }}
          </template>
        </el-table-column>
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '启用' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="handleEdit(row)">编辑</el-button>
            <el-button size="small" type="warning" @click="handleResetPwd(row)">重置密码</el-button>
            <el-popconfirm
              title="确定删除该学员吗？"
              @confirm="handleDelete(row.id)"
            >
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页 -->
      <el-pagination
        v-model:current-page="queryForm.current"
        v-model:page-size="queryForm.size"
        :total="total"
        :page-sizes="[10, 20, 50, 100]"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="loadData"
        @current-change="loadData"
        class="pagination"
      />
    </el-card>

    <!-- 新增/编辑弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="isEdit ? '编辑学员' : '新增学员'"
      width="500px"
    >
      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="100px"
      >
        <el-form-item label="学号" prop="username">
          <el-input v-model="form.username" :disabled="isEdit" placeholder="请输入学号" />
        </el-form-item>
        <el-form-item label="姓名" prop="realName">
          <el-input v-model="form.realName" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item label="班级" prop="classId">
          <el-select v-model="form.classId" placeholder="请选择班级" style="width: 100%">
            <el-option
              v-for="cls in myClasses"
              :key="cls.id"
              :label="cls.className"
              :value="cls.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" placeholder="请输入手机号" />
        </el-form-item>
        <el-form-item label="状态" prop="status">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="密码" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="默认密码：123456"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleSubmit" :loading="submitLoading">
          确定
        </el-button>
      </template>
    </el-dialog>

    <!-- 重置密码弹窗 -->
    <el-dialog
      v-model="resetPwdDialogVisible"
      title="重置密码"
      width="400px"
    >
      <el-form
        ref="resetPwdFormRef"
        :model="resetPwdForm"
        :rules="resetPwdRules"
        label-width="100px"
      >
        <el-form-item label="新密码" prop="password">
          <el-input
            v-model="resetPwdForm.password"
            type="password"
            show-password
            placeholder="请输入新密码"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetPwdDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="handleResetPwdSubmit" :loading="resetPwdLoading">
          确定
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  teacherMyClasses,
  teacherPageStudents,
  teacherAddStudent,
  teacherUpdateStudent,
  teacherDeleteStudent,
  teacherResetStudentPassword
} from '@/api'

// 班级列表
const myClasses = ref([])

// 查询表单
const queryForm = reactive({
  current: 1,
  size: 10,
  classId: null,
  keyword: ''
})

// 表格数据
const tableData = ref([])
const total = ref(0)
const loading = ref(false)

// 弹窗
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref(null)
const submitLoading = ref(false)
const form = reactive({
  id: null,
  username: '',
  realName: '',
  classId: null,
  phone: '',
  status: 1,
  password: ''
})

// 表单校验
const rules = {
  username: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在 3 到 20 个字符', trigger: 'blur' }
  ],
  realName: [
    { required: true, message: '请输入姓名', trigger: 'blur' }
  ],
  classId: [
    { required: true, message: '请选择班级', trigger: 'change' }
  ]
}

// 重置密码弹窗
const resetPwdDialogVisible = ref(false)
const resetPwdFormRef = ref(null)
const resetPwdLoading = ref(false)
const resetPwdForm = reactive({
  id: null,
  password: ''
})

const resetPwdRules = {
  password: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码长度至少 6 位', trigger: 'blur' }
  ]
}

// 加载班级列表
const loadClasses = async () => {
  try {
    const res = await teacherMyClasses()
    myClasses.value = res.data || []
  } catch (error) {
    console.error('加载班级列表失败', error)
  }
}

// 加载数据
const loadData = async () => {
  loading.value = true
  try {
    const res = await teacherPageStudents(queryForm)
    tableData.value = res.data?.records || []
    total.value = res.data?.total || 0
  } catch (error) {
    console.error('加载学员列表失败', error)
    ElMessage.error('加载学员列表失败')
  } finally {
    loading.value = false
  }
}

// 重置查询
const resetQuery = () => {
  queryForm.classId = null
  queryForm.keyword = ''
  queryForm.current = 1
  loadData()
}

// 获取班级名称
const getClassLabel = (classId) => {
  const cls = myClasses.value.find(c => c.id === classId)
  return cls?.className || '-'
}

// 新增
const handleAdd = () => {
  isEdit.value = false
  Object.assign(form, {
    id: null,
    username: '',
    realName: '',
    classId: null,
    phone: '',
    status: 1,
    password: ''
  })
  dialogVisible.value = true
}

// 编辑
const handleEdit = (row) => {
  isEdit.value = true
  Object.assign(form, {
    id: row.id,
    username: row.username,
    realName: row.realName,
    classId: row.classId,
    phone: row.phone,
    status: row.status,
    password: ''
  })
  dialogVisible.value = true
}

// 提交表单
const handleSubmit = async () => {
  if (!formRef.value) return
  await formRef.value.validate()

  submitLoading.value = true
  try {
    if (isEdit.value) {
      await teacherUpdateStudent(form)
      ElMessage.success('编辑成功')
    } else {
      await teacherAddStudent(form)
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch (error) {
    console.error('提交失败', error)
    ElMessage.error(error.message || '操作失败')
  } finally {
    submitLoading.value = false
  }
}

// 删除
const handleDelete = async (id) => {
  try {
    await teacherDeleteStudent(id)
    ElMessage.success('删除成功')
    loadData()
  } catch (error) {
    console.error('删除失败', error)
    ElMessage.error(error.message || '删除失败')
  }
}

// 重置密码
const handleResetPwd = (row) => {
  resetPwdForm.id = row.id
  resetPwdForm.password = ''
  resetPwdDialogVisible.value = true
}

// 提交重置密码
const handleResetPwdSubmit = async () => {
  if (!resetPwdFormRef.value) return
  await resetPwdFormRef.value.validate()

  resetPwdLoading.value = true
  try {
    await teacherResetStudentPassword(resetPwdForm.id, resetPwdForm.password)
    ElMessage.success('密码重置成功')
    resetPwdDialogVisible.value = false
  } catch (error) {
    console.error('重置密码失败', error)
    ElMessage.error(error.message || '重置密码失败')
  } finally {
    resetPwdLoading.value = false
  }
}

onMounted(() => {
  loadClasses()
  loadData()
})
</script>

<style scoped>
.student-manage {
  padding: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  font-size: 18px;
  font-weight: bold;
}

.filter-form {
  margin-bottom: 20px;
}

.pagination {
  margin-top: 20px;
  display: flex;
  justify-content: flex-end;
}
</style>
