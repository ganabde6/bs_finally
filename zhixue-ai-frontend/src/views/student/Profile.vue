<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">个人信息</h1>
        <p class="page-subtitle">查看本人信息、所在班级及各科任课老师联系方式</p>
      </div>
    </div>

    <el-row :gutter="20">
      <el-col :span="10">
        <el-card class="content-card">
          <template #header>
            <div class="card-header"><span>基本信息</span>
              <el-button type="primary" link @click="openEdit">编辑</el-button>
            </div>
          </template>
          <el-descriptions :column="1" border v-loading="loading">
            <el-descriptions-item label="学号">{{ info.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ info.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="所在班级">
              <el-tag type="primary">{{ info.className || '未分配班级' }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="学段">{{ gradeText }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ info.phone || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ info.email || '未填写' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card class="content-card">
          <template #header><span>任课老师</span></template>
          <el-table :data="info.teachers || []" stripe v-loading="loading">
            <el-table-column prop="subjectName" label="科目" width="110">
              <template #default="{row}"><el-tag size="small" effect="light">{{ row.subjectName || '-' }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="teacherName" label="老师" width="120" />
            <el-table-column prop="phone" label="电话">
              <template #default="{row}">{{ row.phone || '未填写' }}</template>
            </el-table-column>
            <el-table-column prop="email" label="邮箱">
              <template #default="{row}">{{ row.email || '未填写' }}</template>
            </el-table-column>
          </el-table>
          <el-empty v-if="!loading && !(info.teachers && info.teachers.length)" description="暂无任课老师信息" :image-size="70" />
        </el-card>
      </el-col>
    </el-row>
  </div>

  <!-- 编辑基本信息弹窗 -->
  <el-dialog v-model="editVisible" title="编辑基本信息" width="420px">
    <el-form :model="editForm" label-width="80px">
      <el-form-item label="学号"><el-input :model-value="info.username" disabled /></el-form-item>
      <el-form-item label="姓名"><el-input v-model="editForm.realName" /></el-form-item>
      <el-form-item label="手机号"><el-input v-model="editForm.phone" maxlength="20" /></el-form-item>
      <el-form-item label="邮箱"><el-input v-model="editForm.email" /></el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="editVisible = false">取消</el-button>
      <el-button type="primary" :loading="saving" @click="saveEdit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { studentProfile, updateProfile } from '@/api'

const loading = ref(false)
const saving = ref(false)
const info = ref({})
const editVisible = ref(false)
const editForm = reactive({ realName: '', phone: '', email: '' })

const gradeText = computed(() => ({ 0: '通用', 1: '小学', 2: '初中', 3: '高中' }[info.value.gradeLevel] || '-'))

const openEdit = () => {
  editForm.realName = info.value.realName || ''
  editForm.phone = info.value.phone || ''
  editForm.email = info.value.email || ''
  editVisible.value = true
}

const saveEdit = async () => {
  if (!editForm.realName.trim()) {
    ElMessage.warning('姓名不能为空')
    return
  }
  saving.value = true
  try {
    await updateProfile({
      realName: editForm.realName.trim(),
      phone: editForm.phone,
      email: editForm.email,
      avatar: info.value.avatar
    })
    ElMessage.success('保存成功')
    editVisible.value = false
    await load()
  } finally {
    saving.value = false
  }
}

const load = async () => {
  loading.value = true
  try {
    const res = await studentProfile()
    info.value = res.data || {}
  } finally {
    loading.value = false
  }
}
onMounted(load)
</script>
