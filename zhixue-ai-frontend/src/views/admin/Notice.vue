<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">公告管理</h1>
        <p class="page-subtitle">向学生与教师发布平台公告</p>
      </div>
      <div class="page-header-right">
        <el-button type="primary" :icon="Plus" @click="openAdd">发布公告</el-button>
      </div>
    </div>
    <el-card class="content-card">
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="title" label="标题" show-overflow-tooltip />
      <el-table-column label="目标角色" width="120"><template #default="{row}">{{ roleText(row.targetRole) }}</template></el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '已发布' : '草稿' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="publishTime" label="发布时间" width="180" />
      <el-table-column label="操作" width="180" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="mt-20" v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadData" />

    <el-dialog v-model="formVisible" :title="form.id ? '编辑公告' : '发布公告'" width="600">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题"><el-input v-model="form.title" /></el-form-item>
        <el-form-item label="目标角色">
          <el-select v-model="form.targetRole" placeholder="请选择">
            <el-option label="全体" value="ALL" />
            <el-option label="学生" value="STUDENT" />
            <el-option label="教师" value="TEACHER" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容"><el-input v-model="form.content" type="textarea" :rows="5" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="发布" inactive-text="草稿" /></el-form-item>
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
import { pageNotices, addNotice, updateNotice, deleteNotice } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10 })
const formVisible = ref(false)
const form = reactive({})

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    const res = await pageNotices(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const roleText = (code) => ({ ALL: '全体', STUDENT: '学生', TEACHER: '教师' }[code] || code || '-')

const openAdd = () => {
  Object.keys(form).forEach(k => delete form[k])
  form.targetRole = 'ALL'
  form.status = 1
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
  if (form.id) await updateNotice(form)
  else await addNotice(form)
  ElMessage.success('保存成功')
  formVisible.value = false
  loadData()
}

const del = async (id) => {
  await ElMessageBox.confirm('确定删除该公告?', '提示', { type: 'warning' })
  await deleteNotice(id)
  ElMessage.success('已删除')
  loadData()
}
</script>
