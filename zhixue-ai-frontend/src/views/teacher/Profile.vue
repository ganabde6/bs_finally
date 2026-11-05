<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">个人信息</h1>
        <p class="page-subtitle">查看本人信息、执教班级科目及所带学生联系方式</p>
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
            <el-descriptions-item label="工号">{{ info.username || '-' }}</el-descriptions-item>
            <el-descriptions-item label="姓名">{{ info.realName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="手机号">{{ info.phone || '未填写' }}</el-descriptions-item>
            <el-descriptions-item label="邮箱">{{ info.email || '未填写' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>
        <el-card class="content-card mt-20">
          <template #header><span>执教班级/科目</span></template>
          <div v-loading="loading">
            <el-tag v-for="(c, i) in info.courses || []" :key="i" class="course-tag" effect="light">
              {{ c.className }} · {{ c.subjectName }}
            </el-tag>
            <el-empty v-if="!loading && !(info.courses && info.courses.length)" description="暂无执教班级" :image-size="60" />
          </div>
        </el-card>
      </el-col>
      <el-col :span="14">
        <el-card class="content-card">
          <template #header>
            <div class="card-header"><span>所带学生</span>
              <el-select v-model="query.classId" placeholder="全部班级" clearable size="small" style="width:140px" @change="onClassChange">
                <el-option v-for="(name, cid) in classMap" :key="cid" :label="name" :value="Number(cid)" />
              </el-select>
            </div>
          </template>
          <el-table :data="students" stripe v-loading="stuLoading" max-height="520">
            <el-table-column prop="realName" label="姓名" width="110" />
            <el-table-column prop="username" label="学号" width="120" />
            <el-table-column label="班级" width="140">
              <template #default="{row}">{{ classMap[row.classId] || '-' }}</template>
            </el-table-column>
            <el-table-column prop="phone" label="电话">
              <template #default="{row}">{{ row.phone || '未填写' }}</template>
            </el-table-column>
          </el-table>
          <el-pagination class="mt-10" v-model:current-page="query.current" v-model:page-size="query.size"
                         :total="total" layout="total, prev, pager, next" @current-change="loadStudents" />
        </el-card>
      </el-col>
    </el-row>
  </div>

  <!-- 编辑基本信息弹窗 -->
  <el-dialog v-model="editVisible" title="编辑基本信息" width="420px">
    <el-form :model="editForm" label-width="80px">
      <el-form-item label="工号"><el-input :model-value="info.username" disabled /></el-form-item>
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { teacherProfile, teacherPageStudents, teacherMyClasses, updateProfile } from '@/api'

const loading = ref(false)
const stuLoading = ref(false)
const saving = ref(false)
const info = ref({})
const students = ref([])
const total = ref(0)
const query = reactive({ current: 1, size: 20, classId: null })
const classMap = ref({}) // classId -> className
const editVisible = ref(false)
const editForm = reactive({ realName: '', phone: '', email: '' })

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
    const res = await teacherProfile()
    info.value = res.data || {}
  } finally {
    loading.value = false
  }
}

const loadStudents = async () => {
  stuLoading.value = true
  try {
    const res = await teacherPageStudents({ current: query.current, size: query.size, classId: query.classId })
    students.value = (res.data && res.data.records) || []
    total.value = (res.data && res.data.total) || 0
  } finally {
    stuLoading.value = false
  }
}

const onClassChange = () => {
  query.current = 1
  loadStudents()
}
onMounted(async () => {
  load()
  try {
    const res = await teacherMyClasses()
    const map = {}
    ;(res.data || []).forEach(c => { map[c.id] = c.className })
    classMap.value = map
  } catch {}
  loadStudents()
})
</script>

<style scoped>
.course-tag { margin: 0 8px 8px 0; }
</style>
