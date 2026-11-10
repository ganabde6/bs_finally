<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">任务派发</h1>
        <p class="page-subtitle">向教师派发工作任务,在教师端首页待办事项中可见</p>
      </div>
      <div class="page-header-right">
        <el-button type="primary" :icon="Plus" @click="openAdd">派发任务</el-button>
      </div>
    </div>
    <el-card class="content-card">
      <el-table :data="list" stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="title" label="任务标题" width="180" show-overflow-tooltip />
        <el-table-column prop="content" label="任务内容" show-overflow-tooltip />
        <el-table-column prop="targetTeacherName" label="目标教师" width="110" />
        <el-table-column prop="deadline" label="截止时间" width="170" />
        <el-table-column label="状态" width="100">
          <template #default="{row}">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.status === 1 ? '已完成' : '待完成' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="completeTime" label="完成时间" width="170">
          <template #default="{row}">{{ row.completeTime || '—' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="派发时间" width="170" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{row}">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-dialog v-model="formVisible" :title="form.id ? '编辑任务' : '派发任务'" width="600">
        <el-form :model="form" label-width="100px">
          <el-form-item label="任务标题"><el-input v-model="form.title" /></el-form-item>
          <el-form-item label="目标教师">
            <el-select v-model="form.targetTeacherId" placeholder="请选择" style="width:100%">
              <el-option label="全体教师" :value="null" />
              <el-option v-for="t in teachers" :key="t.id" :label="t.realName + '(' + t.username + ')'" :value="t.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker v-model="form.deadline" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选填,不填则不限时" style="width:100%" />
          </el-form-item>
          <el-form-item label="任务内容"><el-input v-model="form.content" type="textarea" :rows="5" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" @click="save">保存</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { listTasks, addTask, updateTask, deleteTask, pageUsers, getRoles } from '@/api'

const list = ref([])
const teachers = ref([])
const loading = ref(false)
const formVisible = ref(false)
const form = reactive({})

onMounted(async () => {
  loadData()
  try {
    const r = await getRoles()
    const teacherRole = r.data.find(x => x.roleCode === 'TEACHER')
    if (teacherRole) {
      const tRes = await pageUsers({ current: 1, size: 200, roleId: teacherRole.id })
      teachers.value = tRes.data.records
    }
  } catch {}
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await listTasks()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  Object.keys(form).forEach(k => delete form[k])
  form.targetTeacherId = null
  formVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  formVisible.value = true
}

const save = async () => {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  if (form.id) await updateTask(form)
  else await addTask(form)
  ElMessage.success('保存成功')
  formVisible.value = false
  loadData()
}

const del = async (id) => {
  try {
    await ElMessageBox.confirm('确定删除该任务?', '提示', { type: 'warning' })
  } catch { return }
  await deleteTask(id)
  ElMessage.success('已删除')
  loadData()
}
</script>
