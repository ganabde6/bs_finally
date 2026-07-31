<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>作业/考试管理</span>
        <el-button type="primary" :icon="Plus" @click="$router.push('/teacher/paper/edit')">新建作业/考试</el-button>
      </div>
    </template>
    <el-form :inline="true" class="mb-20">
      <el-form-item label="类型">
        <el-select v-model="query.paperType" placeholder="全部" clearable style="width:120px">
          <el-option label="作业" :value="1" /><el-option label="考试" :value="2" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
    </el-form>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column prop="paperName" label="名称" />
      <el-table-column label="类型" width="80"><template #default="{row}">{{ row.paperType === 1 ? '作业' : '考试' }}</template></el-table-column>
      <el-table-column prop="totalScore" label="总分" width="80" />
      <el-table-column prop="duration" label="时长(分)" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{row}"><el-tag :type="['info','','success'][row.status]">{{ ['草稿','已发布','已结束'][row.status] }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="deadline" label="截止时间" width="180" />
      <el-table-column label="操作" width="280">
        <template #default="{row}">
          <el-button size="small" @click="$router.push(`/teacher/paper/edit/${row.id}`)">编辑</el-button>
          <el-button size="small" type="success" v-if="row.status === 0" @click="publish(row.id)">发布</el-button>
          <el-button size="small" type="warning" v-if="row.status === 1" @click="finish(row.id)">结束</el-button>
          <el-button size="small" type="primary" @click="$router.push(`/teacher/correct?paperId=${row.id}`)">批改</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="mt-20" v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadData" />
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { pagePapers, deletePaper, publishPaper, finishPaper } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const query = reactive({ current: 1, size: 10, paperType: null })

onMounted(loadData)
const loadData = async () => {
  loading.value = true
  try {
    const res = await pagePapers(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally { loading.value = false }
}

const publish = async (id) => {
  await ElMessageBox.confirm('发布后学生即可作答,确定?', '提示', { type: 'warning' })
  await publishPaper(id)
  ElMessage.success('已发布')
  loadData()
}

const finish = async (id) => {
  await finishPaper(id)
  ElMessage.success('已结束')
  loadData()
}

const del = async (id) => {
  await ElMessageBox.confirm('确定删除?', '提示', { type: 'warning' })
  await deletePaper(id)
  ElMessage.success('已删除')
  loadData()
}
</script>
