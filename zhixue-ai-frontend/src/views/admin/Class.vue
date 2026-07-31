<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>班级管理</span>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增班级</el-button>
      </div>
    </template>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="className" label="班级名称" width="160" />
      <el-table-column prop="grade" label="年级" width="120" />
      <el-table-column label="班主任" width="140"><template #default="{row}">{{ teacherText(row.headTeacherId) }}</template></el-table-column>
      <el-table-column label="状态" width="80">
        <template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="formVisible" :title="form.id ? '编辑班级' : '新增班级'" width="480">
      <el-form :model="form" label-width="100px">
        <el-form-item label="班级名称"><el-input v-model="form.className" /></el-form-item>
        <el-form-item label="年级"><el-input v-model="form.grade" placeholder="如:高一" /></el-form-item>
        <el-form-item label="班主任">
          <el-select v-model="form.headTeacherId" clearable filterable>
            <el-option v-for="t in teachers" :key="t.id" :label="t.realName + '(' + t.username + ')'" :value="t.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="停用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { adminClasses, addClass, updateClass, deleteClass, pageUsers, getRoles } from '@/api'

const list = ref([])
const loading = ref(false)
const teachers = ref([])
const formVisible = ref(false)
const form = reactive({})

onMounted(async () => {
  // 拉取教师列表用于班主任选择
  try {
    const r = await getRoles()
    const teacherRole = r.data.find(x => x.roleCode === 'TEACHER')
    if (teacherRole) {
      const tRes = await pageUsers({ current: 1, size: 200, roleId: teacherRole.id })
      teachers.value = tRes.data.records
    }
  } catch {}
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await adminClasses()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

const teacherText = (id) => teachers.value.find(t => t.id === id)?.realName || '-'

const openAdd = () => {
  Object.keys(form).forEach(k => delete form[k])
  form.status = 1
  formVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  formVisible.value = true
}

const save = async () => {
  if (!form.className || !form.grade) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.id) await updateClass(form)
  else await addClass(form)
  ElMessage.success('保存成功')
  formVisible.value = false
  loadData()
}

const del = async (id) => {
  await ElMessageBox.confirm('确定删除该班级?', '提示', { type: 'warning' })
  await deleteClass(id)
  ElMessage.success('已删除')
  loadData()
}
</script>
