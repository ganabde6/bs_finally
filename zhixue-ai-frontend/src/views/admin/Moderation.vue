<template>
  <el-card>
    <template #header><span>智能内容风控</span></template>
    <el-alert title="管理员可在此预检内容是否违规,系统会自动过滤学生端 AI 违规提问、审核作业考试内容,保障平台纯教学属性与使用安全。" type="info" :closable="false" class="mb-20" />
    <el-form label-width="100px">
      <el-form-item label="待检内容">
        <el-input v-model="content" type="textarea" :rows="6" placeholder="请输入需要预检的文字内容" />
      </el-form-item>
      <el-form-item>
        <el-button type="primary" @click="check" :loading="loading">开始风控检测</el-button>
        <el-button @click="content = ''; result = null">清空</el-button>
      </el-form-item>
    </el-form>

    <el-card v-if="result" class="mt-20">
      <template #header><span>检测结果</span></template>
      <el-result
        :icon="result.passed ? 'success' : 'warning'"
        :title="result.passed ? '内容合规' : '内容存在风险'"
        :sub-title="result.reason || '未发现违规内容'"
      />
    </el-card>
  </el-card>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { moderationCheck } from '@/api'

const content = ref('')
const result = ref(null)
const loading = ref(false)

const check = async () => {
  if (!content.value.trim()) {
    ElMessage.warning('请输入待检内容')
    return
  }
  loading.value = true
  try {
    const res = await moderationCheck(content.value)
    result.value = res.data
  } finally {
    loading.value = false
  }
}
</script>
