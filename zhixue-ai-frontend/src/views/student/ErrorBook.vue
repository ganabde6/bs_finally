<template>
  <el-card>
    <template #header><span>智能错题本</span></template>
    <el-tabs v-model="activeTab">
      <el-tab-pane label="错题列表" name="errors">
        <el-table :data="errors" stripe>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column label="题目" min-width="300">
            <template #default="{row}">{{ row.question?.content }}</template>
          </el-table-column>
          <el-table-column label="错误类型" width="120">
            <template #default="{row}">
              <el-tag :type="errorTypeColor(row.errorBook.errorType)">{{ errorTypeText(row.errorBook.errorType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="知识点" width="140">
            <template #default="{row}">{{ row.question?.knowledgePoint }}</template>
          </el-table-column>
          <el-table-column label="复盘状态" width="100">
            <template #default="{row}">
              <el-tag :type="['info','','success'][row.errorBook.reviewStatus]">{{ ['未复盘','已复盘','已掌握'][row.errorBook.reviewStatus] }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240">
            <template #default="{row}">
              <el-button size="small" @click="viewDetail(row)">查看详情</el-button>
              <el-button size="small" type="success" @click="pushV(row.errorBook.id)">推送变式题</el-button>
              <el-button size="small" type="primary" @click="markReviewed(row.errorBook.id, 2)">已掌握</el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
      <el-tab-pane label="变式题" name="variants">
        <el-table :data="variants" stripe>
          <el-table-column type="index" label="#" width="50" />
          <el-table-column prop="content" label="变式题" min-width="300" />
          <el-table-column prop="knowledgePoint" label="知识点" width="140" />
          <el-table-column label="状态" width="100">
            <template #default="{row}"><el-tag :type="row.isSolved ? 'success' : 'info'">{{ row.isSolved ? '已作答' : '未作答' }}</el-tag></template>
          </el-table-column>
        </el-table>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="detailVisible" title="错题详情" width="700">
      <div v-if="current">
        <p><strong>题目:</strong>{{ current.question?.content }}</p>
        <p class="mt-20"><strong>标准答案:</strong>{{ current.question?.standardAnswer }}</p>
        <p class="mt-20"><strong>解析:</strong>{{ current.question?.analysis }}</p>
      </div>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { errorBooks, myVariants, pushVariant, reviewError } from '@/api'

const activeTab = ref('errors')
const errors = ref([])
const variants = ref([])
const detailVisible = ref(false)
const current = ref(null)

const errorTypeText = (t) => ['','知识点缺失','计算失误','审题错误','思路错误','表达不清'][t]
const errorTypeColor = (t) => ['','danger','warning','info','warning','info'][t]

onMounted(load)
const load = async () => {
  const [e, v] = await Promise.all([errorBooks(), myVariants()])
  errors.value = e.data || []
  variants.value = v.data || []
}

const viewDetail = (row) => {
  current.value = row
  detailVisible.value = true
}

const pushV = async (id) => {
  await pushVariant(id)
  ElMessage.success('变式题已推送')
  load()
}

const markReviewed = async (id, status) => {
  await reviewError(id, status)
  ElMessage.success('已标记')
  load()
}
</script>
