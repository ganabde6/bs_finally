<template>
  <div class="login-page">
    <!-- 左侧装饰面板 -->
    <div class="login-left">
      <div class="left-content">
        <div class="brand">
          <svg viewBox="0 0 48 48" width="48" height="48" fill="#fff">
            <rect x="4" y="4" width="40" height="40" rx="10" fill="#FF6700"/>
            <text x="24" y="32" text-anchor="middle" font-size="18" font-weight="bold" fill="#fff">智</text>
          </svg>
          <span class="brand-text">智学AI</span>
        </div>
        <h1 class="slogan">智学AI学习测评系统</h1>
        <p class="sub-slogan">学练一体 · 以测促学 · AI赋能</p>
        <div class="features">
          <div class="feature-item">
            <div class="feature-icon">📝</div>
            <div>
              <div class="feature-title">AI智能批改</div>
              <div class="feature-desc">全题型自动批改，秒级出分</div>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">📊</div>
            <div>
              <div class="feature-title">学情大数据分析</div>
              <div class="feature-desc">精准定位薄弱知识点</div>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">🤖</div>
            <div>
              <div class="feature-title">个性化AI助学</div>
              <div class="feature-desc">千人千面答疑与错题推送</div>
            </div>
          </div>
        </div>
      </div>
      <div class="left-decoration">
        <div class="deco-circle deco-1"></div>
        <div class="deco-circle deco-2"></div>
        <div class="deco-circle deco-3"></div>
      </div>
    </div>

    <!-- 右侧表单面板 -->
    <div class="login-right">
      <div class="form-card">
        <!-- 顶部切换标签 -->
        <div class="tab-bar">
          <span :class="['tab-item', { active: tab === 'login' }]" @click="tab = 'login'">登录</span>
          <span :class="['tab-item', { active: tab === 'register' }]" @click="tab = 'register'">注册</span>
        </div>

        <!-- 登录表单 -->
        <div v-if="tab === 'login'" class="form-body">
          <el-form :model="loginForm" :rules="loginRules" ref="loginRef" size="large" @keyup.enter="handleLogin">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="请输入账号" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" placeholder="密码" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="remember">记住账号</el-checkbox>
            </el-form-item>
            <el-button type="primary" size="large" style="width:100%" :loading="loginLoading" @click="handleLogin">登 录</el-button>
          </el-form>

          <!-- 演示账号 -->
          <div class="demo-section">
            <el-divider content-position="left">演示账号（密码均为 123456）</el-divider>
            <div class="demo-tags">
              <el-tag type="danger" @click="quickLogin('admin','123456')" style="cursor:pointer">管理员 admin</el-tag>
              <el-tag type="success" @click="quickLogin('teacher01','123456')" style="cursor:pointer">教师 teacher01</el-tag>
              <el-tag type="warning" @click="quickLogin('student01','123456')" style="cursor:pointer">学生 student01</el-tag>
            </div>
          </div>
        </div>

        <!-- 注册表单 -->
        <div v-if="tab === 'register'" class="form-body">
          <el-alert title="注册说明" type="info" :closable="false" show-icon style="margin-bottom:16px;font-size:12px">
            <template #default>
              注册账号仅限<strong>学生身份</strong>。教师账号由管理员创建，管理员账号由系统预设。
            </template>
          </el-alert>
          <el-form :model="regForm" :rules="regRules" ref="regRef" size="large" @keyup.enter="handleRegister">
            <el-form-item prop="username">
              <el-input v-model="regForm.username" placeholder="请输入学号/账号" :prefix-icon="User" />
            </el-form-item>
            <el-form-item prop="realName">
              <el-input v-model="regForm.realName" placeholder="请输入真实姓名" :prefix-icon="UserFilled" />
            </el-form-item>
            <el-form-item prop="classId">
              <el-select v-model="regForm.classId" placeholder="请选择班级" style="width:100%">
                <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
              </el-select>
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="regForm.password" type="password" placeholder="请设置密码(至少6位)" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item prop="confirmPwd">
              <el-input v-model="regForm.confirmPwd" type="password" placeholder="请再次输入密码" :prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item>
              <el-checkbox v-model="agreeTerms">
                已阅读并同意<span class="link-text">《用户服务协议》</span>和<span class="link-text">《隐私政策》</span>
              </el-checkbox>
            </el-form-item>
            <el-button type="primary" size="large" style="width:100%" :loading="regLoading" @click="handleRegister">注 册</el-button>
          </el-form>
          <div class="switch-hint">
            已有账号？<el-link type="primary" underline="never" @click="tab = 'login'">立即登录</el-link>
          </div>
        </div>
      </div>

      <!-- 底部信息 -->
      <div class="footer-info">
        <span>© 2024 智学AI学习测评系统</span>
        <el-divider direction="vertical" />
        <el-link underline="never" type="info" style="font-size:12px">用户协议</el-link>
        <el-divider direction="vertical" />
        <el-link underline="never" type="info" style="font-size:12px">隐私政策</el-link>
        <el-divider direction="vertical" />
        <el-link underline="never" type="info" style="font-size:12px">帮助中心</el-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, UserFilled } from '@element-plus/icons-vue'
import { login, register, getClasses } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// Tab 切换
const tab = ref('login')

// 登录
const loginRef = ref()
const loginLoading = ref(false)
const remember = ref(false)
const loginForm = reactive({ username: '', password: '' })
const loginRules = {
  username: [{ required: true, message: '请输入账号', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

// 注册
const regRef = ref()
const regLoading = ref(false)
const agreeTerms = ref(false)
const classes = ref([])
const regForm = reactive({ username: '', realName: '', classId: null, password: '', confirmPwd: '' })

const validateConfirmPwd = (rule, value, callback) => {
  if (value !== regForm.password) {
    callback(new Error('两次输入的密码不一致'))
  } else {
    callback()
  }
}

const regRules = {
  username: [
    { required: true, message: '请输入学号/账号', trigger: 'blur' },
    { min: 3, max: 20, message: '长度在3-20个字符', trigger: 'blur' }
  ],
  realName: [{ required: true, message: '请输入真实姓名', trigger: 'blur' }],
  classId: [{ required: true, message: '请选择班级', trigger: 'change' }],
  password: [
    { required: true, message: '请设置密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPwd: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPwd, trigger: 'blur' }
  ]
}

onMounted(async () => {
  try {
    const res = await getClasses()
    classes.value = res.data
  } catch {}
})

const quickLogin = (u, p) => {
  loginForm.username = u
  loginForm.password = p
  handleLogin()
}

const handleLogin = async () => {
  await loginRef.value.validate()
  loginLoading.value = true
  try {
    const res = await login(loginForm)
    userStore.setToken(res.data.token)
    userStore.setUserInfo(res.data.userInfo)
    userStore.setPermissions(res.data.userInfo.permissions)
    ElMessage.success('登录成功')
    router.push(userStore.homePath)
  } finally {
    loginLoading.value = false
  }
}

const handleRegister = async () => {
  await regRef.value.validate()
  if (!agreeTerms.value) {
    ElMessage.warning('请先同意用户服务协议和隐私政策')
    return
  }
  regLoading.value = true
  try {
    await register({
      username: regForm.username,
      password: regForm.password,
      realName: regForm.realName,
      classId: regForm.classId
    })
    ElMessage.success('注册成功，请登录')
    // 自动填充登录表单
    loginForm.username = regForm.username
    loginForm.password = ''
    tab.value = 'login'
  } finally {
    regLoading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  overflow: hidden;
  background: #f5f5f5;
}

/* ===== 左侧装饰面板 ===== */
.login-left {
  flex: 1;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 30%, #0f3460 60%, #533483 100%);
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.left-content {
  position: relative;
  z-index: 2;
  padding: 60px;
  max-width: 520px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 40px;
}

.brand-text {
  font-size: 24px;
  font-weight: 600;
  color: #fff;
  letter-spacing: 2px;
}

.slogan {
  font-size: 36px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px 0;
  line-height: 1.3;
}

.sub-slogan {
  font-size: 16px;
  color: rgba(255, 255, 255, 0.7);
  margin: 0 0 48px 0;
  letter-spacing: 4px;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.feature-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  flex-shrink: 0;
}

.feature-title {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.6);
}

/* 装饰圆 */
.left-decoration {
  position: absolute;
  inset: 0;
  z-index: 1;
  pointer-events: none;
}

.deco-circle {
  position: absolute;
  border-radius: 50%;
  border: 1px solid rgba(255, 255, 255, 0.08);
}

.deco-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  right: -100px;
  background: radial-gradient(circle, rgba(255, 103, 0, 0.15) 0%, transparent 70%);
}

.deco-2 {
  width: 300px;
  height: 300px;
  bottom: -50px;
  left: -50px;
  background: radial-gradient(circle, rgba(83, 52, 131, 0.3) 0%, transparent 70%);
}

.deco-3 {
  width: 200px;
  height: 200px;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: radial-gradient(circle, rgba(255, 255, 255, 0.05) 0%, transparent 70%);
}

/* ===== 右侧表单面板 ===== */
.login-right {
  width: 480px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: #fff;
  padding: 40px;
  position: relative;
}

.form-card {
  width: 100%;
  max-width: 380px;
}

/* Tab 切换 */
.tab-bar {
  display: flex;
  align-items: center;
  gap: 24px;
  margin-bottom: 32px;
}

.tab-item {
  font-size: 22px;
  font-weight: 500;
  color: #999;
  cursor: pointer;
  padding-bottom: 8px;
  border-bottom: 2px solid transparent;
  transition: all 0.3s;
  user-select: none;
}

.tab-item.active {
  color: #333;
  font-weight: 600;
  border-bottom-color: #ff6700;
}

.tab-item:hover {
  color: #333;
}

/* 表单 */
.form-body {
  min-height: 300px;
}

.form-body :deep(.el-input__wrapper) {
  border-radius: 8px;
  box-shadow: 0 0 0 1px #e0e0e0 inset;
  padding: 4px 12px;
}

.form-body :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #ff6700 inset;
}

.form-body :deep(.el-input__wrapper.is-focus) {
  box-shadow: 0 0 0 1px #ff6700 inset;
}

.form-body :deep(.el-button--primary) {
  background: #ff6700;
  border-color: #ff6700;
  border-radius: 8px;
  height: 44px;
  font-size: 16px;
  letter-spacing: 4px;
}

.form-body :deep(.el-button--primary:hover) {
  background: #f25b00;
  border-color: #f25b00;
}

.form-body :deep(.el-form-item) {
  margin-bottom: 20px;
}

.form-body :deep(.el-checkbox__label) {
  font-size: 13px;
  color: #666;
}

.form-body :deep(.el-link) {
  font-size: 13px;
}

.link-text {
  color: #ff6700;
  cursor: pointer;
}

/* 演示账号 */
.demo-section {
  margin-top: 16px;
}

.demo-section :deep(.el-divider__text) {
  font-size: 12px;
  color: #bbb;
}

.demo-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.demo-tags .el-tag {
  cursor: pointer;
  font-size: 12px;
}

/* 注册提示 */
.switch-hint {
  text-align: center;
  margin-top: 20px;
  font-size: 13px;
  color: #999;
}

/* 底部信息 */
.footer-info {
  position: absolute;
  bottom: 24px;
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #bbb;
}

.footer-info :deep(.el-divider) {
  margin: 0 8px;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .login-left {
    display: none;
  }
  .login-right {
    width: 100%;
  }
}
</style>
