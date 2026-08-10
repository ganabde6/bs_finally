<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">AI 模型运维配置</h1>
        <p class="page-subtitle">管理批改严苛度、题型评分规则与 AI 功能开关</p>
      </div>
    </div>
    <el-card class="content-card">
    <el-alert title="管理员可在此自定义批改严苛度、题型评分规则,以及开关学生端 AI 答疑、润色、错题推送等功能,适配日常练习与正式考试不同场景。" type="info" :closable="false" class="mb-20" />
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="configName" label="配置项" width="200" />
      <el-table-column prop="configKey" label="配置键" width="220" />
      <el-table-column prop="configValue" label="配置值" width="160" />
      <el-table-column prop="description" label="说明" show-overflow-tooltip />
      <el-table-column prop="updateTime" label="更新时间" width="180" />
      <el-table-column label="操作" width="120" fixed="right">
        <template #default="{row}">
          <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="formVisible" title="编辑配置" width="480">
      <el-form :model="form" label-width="100px">
        <el-form-item label="配置项"><el-input v-model="form.configName" disabled /></el-form-item>
        <el-form-item label="配置值"><el-input v-model="form.configValue" /></el-form-item>
        <el-form-item label="说明"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
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
import { ElMessage } from 'element-plus'
import { aiConfigs, updateAiConfig } from '@/api'

const list = ref([])
const loading = ref(false)
const formVisible = ref(false)
const form = reactive({})

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    const res = await aiConfigs()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

const openEdit = (row) => {
  Object.assign(form, row)
  formVisible.value = true
}

const save = async () => {
  if (form.configValue === '' || form.configValue === null) {
    ElMessage.warning('请填写配置值')
    return
  }
  await updateAiConfig(form)
  ElMessage.success('配置已更新')
  formVisible.value = false
  loadData()
}
</script>
