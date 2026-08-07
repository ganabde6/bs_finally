<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <el-icon :size="24"><MagicStick /></el-icon>
        <span v-if="!isCollapse">智学AI</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="isCollapse"
        :router="true"
        background-color="#001529"
        text-color="#bbb"
        active-text-color="#fff"
      >
        <template v-for="m in menus" :key="m.path">
          <!-- 有子菜单时渲染为二级菜单 -->
          <el-sub-menu v-if="m.children && m.children.length" :index="m.path">
            <template #title>
              <el-icon><component :is="m.icon" /></el-icon>
              <span>{{ m.title }}</span>
            </template>
            <el-menu-item v-for="c in m.children" :key="c.path" :index="c.path">
              <el-icon><component :is="c.icon || m.icon" /></el-icon>
              <template #title>{{ c.title }}</template>
            </el-menu-item>
          </el-sub-menu>
          <!-- 无子菜单时渲染为普通菜单项 -->
          <el-menu-item v-else :index="m.path">
            <el-icon><component :is="m.icon" /></el-icon>
            <template #title>{{ m.title }}</template>
          </el-menu-item>
        </template>
      </el-menu>
    </el-aside>
    <el-container>
      <el-header class="header">
        <div class="flex" style="align-items:center; gap:12px">
          <el-icon :size="20" style="cursor:pointer" @click="isCollapse = !isCollapse">
            <Fold v-if="!isCollapse" /><Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="flex" style="align-items:center; gap:12px">
          <el-dropdown @command="handleCommand">
            <span class="flex" style="align-items:center; gap:8px; cursor:pointer">
              <el-avatar :size="32" :src="userStore.userInfo.avatar">{{ userStore.userInfo.realName?.charAt(0) }}</el-avatar>
              <span>{{ userStore.userInfo.realName }}</span>
              <el-tag size="small" type="info">{{ userStore.userInfo.roleName }}</el-tag>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>

    <!-- 个人资料/修改密码 弹窗 -->
    <el-dialog v-model="profileVisible" title="个人资料" width="500">
      <el-form :model="profileForm" label-width="80px">
        <el-form-item label="账号"><el-input v-model="profileForm.username" disabled /></el-form-item>
        <el-form-item label="姓名"><el-input v-model="profileForm.realName" /></el-form-item>
        <el-form-item label="手机"><el-input v-model="profileForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="profileForm.email" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="profileVisible = false">取消</el-button>
        <el-button type="primary" @click="saveProfile">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="pwdVisible" title="修改密码" width="420">
      <el-form :model="pwdForm" label-width="80px">
        <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
        <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="pwdVisible = false">取消</el-button>
        <el-button type="primary" @click="savePwd">确认</el-button>
      </template>
    </el-dialog>
  </el-container>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/stores/user'
import { updateProfile, changePassword } from '@/api'

const props = defineProps({ menus: { type: Array, default: () => [] } })

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()
const isCollapse = ref(false)

const title = computed(() => route.meta.title || '首页')

const profileVisible = ref(false)
const profileForm = reactive({ ...userStore.userInfo })
const pwdVisible = ref(false)
const pwdForm = reactive({ oldPassword: '', newPassword: '' })

const handleCommand = (cmd) => {
  if (cmd === 'profile') {
    Object.assign(profileForm, userStore.userInfo)
    profileVisible.value = true
  } else if (cmd === 'password') {
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdVisible.value = true
  } else if (cmd === 'logout') {
    ElMessageBox.confirm('确定退出登录吗?', '提示', { type: 'warning' }).then(() => {
      userStore.clear()
      router.push('/login')
    }).catch(() => {})
  }
}

const saveProfile = async () => {
  await updateProfile(profileForm)
  ElMessage.success('保存成功')
  await userStore.fetchUserInfo()
  profileVisible.value = false
}

const savePwd = async () => {
  if (!pwdForm.oldPassword || !pwdForm.newPassword) {
    ElMessage.warning('请填写完整')
    return
  }
  await changePassword(pwdForm)
  ElMessage.success('修改成功,请重新登录')
  userStore.clear()
  router.push('/login')
}
</script>

<style scoped>
.layout { height: 100vh; }
.aside { background: #001529; transition: width 0.3s; overflow: hidden; }
.logo { height: 60px; display: flex; align-items: center; justify-content: center; color: #fff; gap: 8px; font-size: 18px; font-weight: 600; }
.header { background: #fff; display: flex; align-items: center; justify-content: space-between; border-bottom: 1px solid #ebeef5; }
.main { background: #f5f7fa; padding: 20px; overflow-y: auto; }
.el-menu { border-right: none; }
</style>
