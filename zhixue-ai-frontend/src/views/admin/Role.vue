<template>
  <el-card>
    <template #header>
      <div class="card-header">
        <span>角色权限管理</span>
        <el-button type="primary" :icon="Plus" @click="openAdd">新增角色</el-button>
      </div>
    </template>
    <el-table :data="list" stripe v-loading="loading">
      <el-table-column type="index" label="#" width="50" />
      <el-table-column prop="roleCode" label="角色编码" width="160" />
      <el-table-column prop="roleName" label="角色名称" width="140" />
      <el-table-column prop="description" label="描述" show-overflow-tooltip />
      <el-table-column prop="createTime" label="创建时间" width="180" />
      <el-table-column label="操作" width="220" fixed="right">
        <template #default="{row}">
          <el-button size="small" @click="openEdit(row)">编辑</el-button>
          <el-button size="small" type="success" @click="openPerm(row)">分配权限</el-button>
          <el-button size="small" type="danger" @click="del(row.id)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑角色 -->
    <el-dialog v-model="formVisible" :title="form.id ? '编辑角色' : '新增角色'" width="480">
      <el-form :model="form" label-width="100px">
        <el-form-item label="角色编码"><el-input v-model="form.roleCode" :disabled="!!form.id" /></el-form-item>
        <el-form-item label="角色名称"><el-input v-model="form.roleName" /></el-form-item>
        <el-form-item label="描述"><el-input v-model="form.description" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="formVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 分配权限 -->
    <el-dialog v-model="permVisible" title="分配权限" width="520">
      <div class="mb-20">当前角色:<el-tag class="ml-10">{{ currentRole?.roleName }}</el-tag></div>
      <el-tree
        ref="permTree"
        :data="permTreeData"
        show-checkbox
        node-key="id"
        :props="{ label: 'permName', children: 'children' }"
        :default-checked-keys="checkedKeys"
      />
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" @click="savePerm">保存</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import { roles, addRole, updateRole, deleteRole, permissions, rolePermissions, assignPermissions } from '@/api'

const list = ref([])
const loading = ref(false)
const formVisible = ref(false)
const form = reactive({})
const permVisible = ref(false)
const currentRole = ref(null)
const permTreeData = ref([])
const checkedKeys = ref([])
const permTree = ref()

onMounted(() => loadData())

const loadData = async () => {
  loading.value = true
  try {
    const res = await roles()
    list.value = res.data
  } finally {
    loading.value = false
  }
}

const openAdd = () => {
  Object.keys(form).forEach(k => delete form[k])
  formVisible.value = true
}

const openEdit = (row) => {
  Object.assign(form, row)
  formVisible.value = true
}

const save = async () => {
  if (!form.roleCode || !form.roleName) {
    ElMessage.warning('请填写完整信息')
    return
  }
  if (form.id) await updateRole(form)
  else await addRole(form)
  ElMessage.success('保存成功')
  formVisible.value = false
  loadData()
}

const del = async (id) => {
  await ElMessageBox.confirm('确定删除该角色?', '提示', { type: 'warning' })
  await deleteRole(id)
  ElMessage.success('已删除')
  loadData()
}

// 分配权限
const openPerm = async (row) => {
  currentRole.value = row
  const [p, rp] = await Promise.all([permissions(), rolePermissions(row.id)])
  permTreeData.value = buildTree(p.data)
  checkedKeys.value = rp.data
  permVisible.value = true
}

// 构建权限树
const buildTree = (data) => {
  const map = {}
  const roots = []
  data.forEach(item => {
    map[item.id] = { ...item, children: [] }
  })
  data.forEach(item => {
    if (item.parentId && map[item.parentId]) {
      map[item.parentId].children.push(map[item.id])
    } else {
      roots.push(map[item.id])
    }
  })
  return roots
}

const savePerm = async () => {
  const checked = permTree.value.getCheckedKeys()
  const halfChecked = permTree.value.getHalfCheckedKeys()
  await assignPermissions(currentRole.value.id, [...checked, ...halfChecked])
  ElMessage.success('权限已更新')
  permVisible.value = false
}
</script>
