<template>
  <div class="page-container">
    <div class="page-header">
      <div class="page-header-left">
        <h1 class="page-title">任务派发</h1>
        <p class="page-subtitle">向所教班级派发任务待办,学生端学习首页可见</p>
      </div>
      <div class="page-header-right">
        <el-button type="primary" :icon="Plus" @click="openAdd" :disabled="!classes.length">派发任务</el-button>
      </div>
    </div>
    <el-card class="content-card">
      <el-table v-if="list.length" :data="list" stripe v-loading="loading">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="title" label="任务标题" width="200" show-overflow-tooltip />
        <el-table-column prop="content" label="任务内容" show-overflow-tooltip />
        <el-table-column prop="targetClassName" label="目标班级" width="120" />
        <el-table-column prop="deadline" label="截止时间" width="170">
          <template #default="{row}">{{ row.deadline || '不限时' }}</template>
        </el-table-column>
        <el-table-column label="完成进度" width="110">
          <template #default="{row}">
            <el-tag :type="row.completed === row.total ? 'success' : 'info'" size="small">{{ row.completed }}/{{ row.total }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="派发时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{row}">
            <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-empty v-else description="还没有派发过任务,点击右上角「派发任务」开始" />

      <el-dialog v-model="formVisible" title="派发任务" width="600">
        <el-form :model="form" label-width="100px">
          <el-form-item label="目标班级">
            <el-select v-model="form.targetClassId" placeholder="请选择所教班级" style="width:100%">
              <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
            </el-select>
          </el-form-item>
          <el-form-item label="任务标题"><el-input v-model="form.title" placeholder="如:完成本月错题整理" /></el-form-item>
          <el-form-item label="截止时间">
            <el-date-picker v-model="form.deadline" type="datetime" value-format="yyyy-MM-dd HH:mm:ss" placeholder="选填,不填则不限时" style="width:100%" />
          </el-form-item>
          <el-form-item label="任务内容"><el-input v-model="form.content" type="textarea" :rows="5" placeholder="请描述任务要求" /></el-form-item>
        </el-form>
        <template #footer>
          <el-button @click="formVisible = false">取消</el-button>
          <el-button type="primary" @click="save">派发</el-button>
        </template>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { teacherMyClasses, dispatchStudentTask, myStudentTasks, deleteStudentTask } from '@/api'

const list = ref([])
const classes = ref([])
const loading = ref(false)
const formVisible = ref(false)
const form = reactive({})

onMounted(async () => {
  try {
    const c = await teacherMyClasses()
    classes.value = c.data || []
  } catch {}
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await myStudentTasks()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  Object.keys(form).forEach(k => delete form[k])
  formVisible.value = true
}

const save = async () => {
  if (!form.targetClassId) {
    ElMessage.warning('请选择目标班级')
    return
  }
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和内容')
    return
  }
  await dispatchStudentTask(form)
  ElMessage.success('任务已派发')
  formVisible.value = false
  loadData()
}

const del = async (id) => {
  try {
    await ElMessageBox.confirm('删除后学生端将不再显示该待办,确定删除该任务?', '提示', { type: 'warning' })
  } catch { return }
  await deleteStudentTask(id)
  ElMessage.success('已删除')
  loadData()
}
</script>
