<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <div class="logo-chip"><el-icon :size="18"><MagicStick /></el-icon></div>
        <span v-if="!isCollapse" class="logo-text">智学AI</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="isCollapse"
        :router="true"
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
.aside {
  background: linear-gradient(180deg, #0B3B37 0%, #134E4A 100%);
  transition: width 0.3s;
  overflow: hidden;
}
.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  gap: 10px;
  font-size: 18px;
  font-weight: 600;
}
.logo-chip {
  width: 32px;
  height: 32px;
  border-radius: 9px;
  background: linear-gradient(135deg, #2DD4BF, #0D9488);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #fff;
  box-shadow: 0 2px 8px rgba(13, 148, 136, 0.45);
}
.logo-text { letter-spacing: 1px; }
.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #E8F1F4;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.main { background: #F0FDFA; padding: 20px; overflow-y: auto; }
.el-menu { border-right: none; background: transparent; }
.el-menu :deep(.el-menu-item),
.el-menu :deep(.el-sub-menu__title) {
  color: rgba(255, 255, 255, 0.72);
  transition: background-color 0.2s ease, color 0.2s ease;
  position: relative;
}
.el-menu :deep(.el-menu-item .el-icon),
.el-menu :deep(.el-sub-menu__title .el-icon) {
  width: 26px;
  height: 26px;
  border-radius: 8px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  transition: background-color 0.2s ease, box-shadow 0.2s ease;
}
.el-menu :deep(.el-menu-item:hover),
.el-menu :deep(.el-sub-menu__title:hover) {
  background: rgba(45, 212, 191, 0.12);
  color: #fff;
}
.el-menu :deep(.el-menu-item.is-active) {
  background: rgba(45, 212, 191, 0.2);
  color: #fff;
  font-weight: 600;
}
.el-menu :deep(.el-menu-item.is-active)::before {
  content: '';
  position: absolute;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  width: 3px;
  height: 20px;
  border-radius: 0 3px 3px 0;
  background: #2DD4BF;
  box-shadow: 0 0 8px rgba(45, 212, 191, 0.8);
}
.el-menu :deep(.el-menu-item.is-active .el-icon) {
  background: linear-gradient(135deg, #2DD4BF, #0D9488);
  color: #fff;
  box-shadow: 0 2px 8px rgba(13, 148, 136, 0.5);
}
.el-menu :deep(.el-menu--popup) { background: #134E4A; }
.el-menu :deep(.el-menu--popup .el-menu-item) { color: rgba(255, 255, 255, 0.72); }
.el-menu :deep(.el-menu--popup .el-menu-item.is-active) { color: #fff; }
.el-breadcrumb :deep(.el-breadcrumb__inner) { color: #134E4A; font-weight: 500; }
</style>
