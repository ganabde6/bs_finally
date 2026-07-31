<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>用户管理</span>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增用户</el-button>
      </div>
    </template>
    <el-form :inline="true" class="mb-20">
      <el-form-item label="关键词">
        <el-input v-model="query.keyword" placeholder="账号/姓名" clearable style="width:180px" />
      </el-form-item>
      <el-form-item label="角色">
        <el-select v-model="query.roleId" placeholder="全部" clearable style="width:140px">
          <el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="班级">
        <el-select v-model="query.classId" placeholder="全部" clearable style="width:140px">
          <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-button type="primary" @click="loadData">查询</el-button>
    </el-form>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="username" label="账号" width="120" />
      <el-table-column prop="realName" label="姓名" width="100" />
      <el-table-column label="角色" width="100"><template #default="{row}">{{ roleText(row.roleId) }}</template></el-table-column>
      <el-table-column label="班级" width="120"><template #default="{row}">{{ classText(row.classId) }}</template></el-table-column>
      <el-table-column prop="phone" label="手机" width="130" />
      <el-table-column label="状态" width="80">
        <template #default="{row}">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="lastLogin" label="最近登录" width="160" />
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="warning" @click="openReset(row)">重置密码</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination class="mt-20" v-model:current-page="query.current" v-model:page-size="query.size" :total="total" layout="total, prev, pager, next" @current-change="loadData" />

    <!-- 新增/编辑 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑用户' : '新增用户'" width="560">
      <el-form :model="form" label-width="100px">
        <el-form-item label="账号" v-if="!form.id"><el-input v-model="form.username" /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="form.realName" /></el-form-item>
        <el-form-item label="角色"><el-select v-model="form.roleId"><el-option v-for="r in roles" :key="r.id" :label="r.roleName" :value="r.id" /></el-select></el-form-item>
        <el-form-item label="班级" v-if="form.roleId === 4"><el-select v-model="form.classId" clearable><el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" /></el-select></el-form-item>
        <el-form-item label="手机"><el-input v-model="form.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="form.email" /></el-form-item>
        <el-form-item label="状态"><el-switch v-model="form.status" :active-value="1" :inactive-value="0" active-text="启用" inactive-text="禁用" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 重置密码 -->
    <el-dialog v-model="resetVisible" title="重置密码" width="400">
      <el-form label-width="100px">
        <el-form-item label="账号"><span>{{ resetForm.username }}</span></el-form-item>
        <el-form-item label="新密码"><el-input v-model="resetForm.password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="doReset">确认重置</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { pageUsers, addUser, updateUser, deleteUser, resetPassword, getRoles, getClasses } from '@/api'

const list = ref([])
const total = ref(0)
const loading = ref(false)
const roles = ref([])
const classes = ref([])
const query = reactive({ current: 1, size: 10, keyword: '', roleId: null, classId: null })
const formVisible = ref(false)
const form = reactive({})
const resetVisible = ref(false)
const resetForm = reactive({ id: null, username: '', password: '' })

onMounted(async () => {
  const [r, c] = await Promise.all([getRoles(), getClasses()])
  roles.value = r.data
  classes.value = c.data
  loadData()
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await pageUsers(query)
    list.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

const roleText = (id) => roles.value.find(r => r.id === id)?.roleName || '-'
const classText = (id) => classes.value.find(c => c.id === id)?.className || '-'

const openAdd = () => {
  Object.keys(form).forEach(k => delete form[k])
  form.roleId = roles.value[0]?.id
  form.status = 1
  formVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  formVisible.value = true
}

const save = async () => {
  if (!form.username || !form.realName || !form.roleId) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.id) await updateUser(form)
  else await addUser(form)
  ElMessage.success('保存成功')
  formVisible.value = false
  loadData()
}

const del = async (id) => {
  await ElMessageBox.confirm('确定删除该用户?', '提示', { type: 'warning' })
  await deleteUser(id)
  ElMessage.success('已删除')
  loadData()
}

const openReset = (row) => {
  resetForm.id = row.id
  resetForm.username = row.username
  resetForm.password = ''
  resetVisible.value = true
}

const doReset = async () => {
  if (!resetForm.password) {
    ElMessage.warning('请输入新密码')
    return
  }
  await resetPassword(resetForm.id, resetForm.password)
  ElMessage.success('密码已重置')
  resetVisible.value = false
}
</script>
