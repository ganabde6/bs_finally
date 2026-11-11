<template>
  <div class="login-page">
    <!-- 左侧插画面板 -->
    <div class="login-left">
      <svg class="left-art" viewBox="0 0 360 760" preserveAspectRatio="xMidYMax slice" aria-hidden="true">
        <!-- 知识轨道(虚线环,火箭居于轨道内) -->
        <circle cx="180" cy="565" r="152" fill="none" stroke="rgba(255,150,77,0.32)" stroke-width="1.5" stroke-dasharray="4 9"/>
        <path d="M 30 380 A 240 240 0 0 1 330 380" fill="none" stroke="rgba(255,255,255,0.10)" stroke-width="1" stroke-dasharray="3 8"/>
        <!-- 轨道节点(知识点) -->
        <circle cx="38" cy="513" r="5" fill="#FF6900"/>
        <circle cx="327" cy="604" r="4" fill="#FF964D"/>
        <circle cx="206" cy="415" r="3.5" fill="rgba(255,255,255,0.75)"/>
        <!-- 悬浮学科符号 -->
        <text x="54" y="296" font-size="22" fill="rgba(255,150,77,0.55)">√</text>
        <text x="296" y="262" font-size="19" fill="rgba(255,255,255,0.38)">π</text>
        <text x="40" y="408" font-size="14" fill="rgba(255,255,255,0.30)">A+</text>
        <text x="318" y="452" font-size="15" fill="rgba(255,150,77,0.45)">%</text>
        <!-- 加号点缀 -->
        <g stroke="rgba(255,255,255,0.32)" stroke-width="2" stroke-linecap="round">
          <path d="M92 322 v12 M86 328 h12"/>
          <path d="M294 350 v10 M289 355 h10"/>
        </g>
        <!-- 学情数据柱状图 -->
        <g fill="rgba(255,255,255,0.5)">
          <rect x="36" y="540" width="13" height="46" rx="4"/>
          <rect x="56" y="520" width="13" height="66" rx="4"/>
          <rect x="76" y="498" width="13" height="88" rx="4"/>
        </g>
        <!-- AI 助学气泡 -->
        <g>
          <rect x="246" y="486" width="64" height="44" rx="12" fill="rgba(255,255,255,0.92)"/>
          <path d="M260 528 L256 542 L274 528 Z" fill="rgba(255,255,255,0.92)"/>
          <text x="278" y="515" text-anchor="middle" font-size="20" font-weight="bold" fill="#CC5400">AI</text>
        </g>
        <!-- 智能批改对勾徽章 -->
        <circle cx="268" cy="430" r="19" fill="rgba(255,150,77,0.9)"/>
        <path d="M259 430 L266 437 L278 424" stroke="#fff" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
        <!-- 知识火箭 -->
        <g>
          <path d="M180 520 Q204 556 204 604 L204 660 Q180 676 156 660 L156 604 Q156 556 180 520 Z" fill="#FFFFFF"/>
          <circle cx="180" cy="596" r="16" fill="#CC5400"/>
          <circle cx="185" cy="590" r="5.5" fill="#FFB480" opacity="0.8"/>
          <path d="M156 636 L128 676 L156 670 Z" fill="#FF964D"/>
          <path d="M204 636 L232 676 L204 670 Z" fill="#FF964D"/>
          <path d="M168 682 Q180 728 192 682 Z" fill="#FFB380"/>
          <path d="M174 682 Q180 714 186 682 Z" fill="#FF6900"/>
        </g>
        <!-- 打开的书本 -->
        <path d="M180 700 Q122 668 64 686 L64 726 Q122 708 180 740 Z" fill="rgba(255,255,255,0.9)"/>
        <path d="M180 700 Q238 668 296 686 L296 726 Q238 708 180 740 Z" fill="rgba(255,255,255,0.72)"/>
        <path d="M180 700 L180 740" stroke="rgba(255,255,255,0.15)" stroke-width="3"/>
      </svg>

      <div class="left-content">
        <h1 class="slogan">智汇云课堂</h1>
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
    </div>

    <!-- 右侧区域 -->
    <div class="login-right">
      <!-- 顶部导航 -->
      <header class="top-bar">
        <div class="brand">
          <!-- 云朵 logo:渐变云身 + 白色四角星火 -->
          <svg class="brand-mark" viewBox="0 0 48 48" aria-hidden="true">
            <defs>
              <linearGradient id="brandCloudGrad" x1="8" y1="10" x2="40" y2="38" gradientUnits="userSpaceOnUse">
                <stop offset="0" stop-color="#FF964D"/>
                <stop offset="1" stop-color="#FF6900"/>
              </linearGradient>
            </defs>
            <g fill="url(#brandCloudGrad)">
              <circle cx="16" cy="28" r="9"/>
              <circle cx="27" cy="22" r="11"/>
              <circle cx="34.5" cy="28.5" r="8"/>
              <rect x="12" y="25" width="26.5" height="12" rx="6"/>
            </g>
            <path d="M24.5 17.5 L26.3 23.7 L32.5 25.5 L26.3 27.3 L24.5 33.5 L22.7 27.3 L16.5 25.5 L22.7 23.7 Z" fill="#fff"/>
            <circle cx="31.5" cy="18.5" r="1.5" fill="#fff" opacity="0.85"/>
          </svg>
          <div class="brand-name-wrap">
            <span class="brand-text">智汇云课堂</span>
            <span class="brand-sub">学练一体  AI赋能</span>
          </div>
        </div>
        <nav class="top-links">
          <span class="top-link" @click="openDoc('agreement')">用户协议</span>
          <span class="top-link" @click="openDoc('privacy')">隐私政策</span>
          <span class="top-link" @click="openDoc('help')">帮助中心</span>
        </nav>
      </header>

      <div class="right-body">
        <!-- 全幅淡网格,呼应左侧面板纹理 -->
        <div class="deco-grid" aria-hidden="true"></div>

        <!-- 左下虚线知识环,与右上轨道对角呼应 -->
        <div class="deco-ring" aria-hidden="true"></div>

        <!-- 右上角知识轨道装饰(缓慢旋转) -->
        <svg class="deco-orbit" viewBox="0 0 260 260" aria-hidden="true">
          <circle cx="130" cy="130" r="118" fill="none" stroke="rgba(255,105,0,0.16)" stroke-width="1.5" stroke-dasharray="5 10"/>
          <circle cx="130" cy="130" r="78" fill="none" stroke="rgba(31,35,41,0.07)" stroke-width="1" stroke-dasharray="3 8"/>
          <circle cx="130" cy="130" r="40" fill="none" stroke="rgba(255,105,0,0.10)" stroke-width="1" stroke-dasharray="2 6"/>
          <circle cx="248" cy="130" r="5" fill="#FF6900" opacity="0.65"/>
          <circle cx="52" cy="130" r="3.5" fill="rgba(31,35,41,0.22)"/>
          <circle cx="130" cy="12" r="3" fill="#FF964D" opacity="0.7"/>
        </svg>

        <!-- 大号品牌水印 -->
        <div class="deco-watermark" aria-hidden="true">智</div>

        <!-- 浮动学科符号 -->
        <div class="deco-symbols" aria-hidden="true">
          <span class="fsym fsym-a">√</span>
          <span class="fsym fsym-b">π</span>
          <span class="fsym fsym-c">A+</span>
          <span class="fsym fsym-d">%</span>
        </div>

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
                <el-input v-model="loginForm.username" placeholder="请输入账号" />
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="loginForm.password" type="password" placeholder="密码" show-password />
              </el-form-item>
              <el-form-item class="check-item">
                <el-checkbox v-model="remember">记住账号</el-checkbox>
              </el-form-item>
              <el-button type="primary" size="large" class="submit-btn" :loading="loginLoading" @click="handleLogin">登 录</el-button>
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
            <el-alert title="注册说明" type="info" :closable="false" show-icon class="reg-alert" style="margin-bottom:16px;font-size:12px">
              <template #default>
                注册账号仅限<strong>学生身份</strong>。教师账号由管理员创建，管理员账号由系统预设。
              </template>
            </el-alert>
            <el-form :model="regForm" :rules="regRules" ref="regRef" size="large" @keyup.enter="handleRegister">
              <el-form-item prop="username">
                <el-input v-model="regForm.username" placeholder="请输入学号/账号" />
              </el-form-item>
              <el-form-item prop="realName">
                <el-input v-model="regForm.realName" placeholder="请输入真实姓名" />
              </el-form-item>
              <el-form-item prop="gradeLevel">
                <el-select v-model="regForm.gradeLevel" placeholder="请选择学段" style="width:100%">
                  <el-option label="小学" :value="1" />
                  <el-option label="初中" :value="2" />
                  <el-option label="高中" :value="3" />
                </el-select>
              </el-form-item>
              <el-form-item prop="classId">
                <el-select v-model="regForm.classId" placeholder="请选择班级" style="width:100%">
                  <el-option v-for="c in classes" :key="c.id" :label="c.className" :value="c.id" />
                </el-select>
              </el-form-item>
              <el-form-item prop="password">
                <el-input v-model="regForm.password" type="password" placeholder="请设置密码(至少6位)" show-password />
              </el-form-item>
              <el-form-item prop="confirmPwd">
                <el-input v-model="regForm.confirmPwd" type="password" placeholder="请再次输入密码" show-password />
              </el-form-item>
              <el-form-item class="check-item">
                <el-checkbox v-model="agreeTerms">
                  已阅读并同意<span class="link-text" @click="openDoc('agreement')">《用户服务协议》</span>和<span class="link-text" @click="openDoc('privacy')">《隐私政策》</span>
                </el-checkbox>
              </el-form-item>
              <el-button type="primary" size="large" class="submit-btn" :loading="regLoading" @click="handleRegister">注 册</el-button>
            </el-form>
            <div class="switch-hint">
              已有账号？<el-link type="primary" underline="never" @click="tab = 'login'">立即登录</el-link>
            </div>
          </div>
        </div>
      </div>

      <!-- 底部信息 -->
      <div class="footer-info">© 2024 智汇云课堂</div>

      <!-- 文档弹窗:用户协议 / 隐私政策 / 帮助中心 -->
      <el-dialog v-model="docVisible" :title="docMeta.title" width="640px" align-center class="doc-dialog">
        <div class="doc-body">
          <template v-for="(sec, i) in docMeta.sections" :key="i">
            <h4 class="doc-h">{{ sec.h }}</h4>
            <p class="doc-p">{{ sec.p }}</p>
          </template>
        </div>
        <template #footer>
          <el-button type="primary" @click="docVisible = false">我知道了</el-button>
        </template>
      </el-dialog>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, getClasses } from '@/api'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// Tab 切换
const tab = ref('login')

// 文档弹窗(用户协议 / 隐私政策 / 帮助中心)
const docVisible = ref(false)
const activeDoc = ref('agreement')
const docData = {
  agreement: {
    title: '用户服务协议',
    sections: [
      { h: '一、适用范围', p: '本协议适用于「智汇云课堂」平台提供的全部功能与服务,包括 AI 智能批改、学情分析、个性化 AI 助学、英语听说、作业考试等。您注册、登录或使用本平台,即视为已阅读并同意本协议。' },
      { h: '二、账号与身份', p: '学生账号由本人自助注册,需填写真实姓名、学段与所属班级;教师账号由管理员创建;管理员账号由系统预设。账号仅限本人使用,请妥善保管登录凭证,因保管不善造成的损失由账号使用人自行承担。' },
      { h: '三、使用规范', p: '用户应将平台用于学习、教学与管理等正当用途,不得进行篡改批改结果、冒用他人身份、干扰系统正常运行等危害平台或其他用户权益的行为。AI 批改结果供学习参考,正式考核成绩请以教师复核为准。' },
      { h: '四、知识产权', p: '平台题库、课程及内容由平台或相关权利人提供并享有知识产权,未经授权不得复制、传播或用于其他商业用途。' },
      { h: '五、服务变更', p: '平台将持续优化功能,可能因系统维护、升级调整部分功能或暂时停止服务,并将尽量提前在平台内公告。' },
      { h: '六、协议更新', p: '平台可根据业务需要修订本协议,修订后的内容自发布之日起生效。您在使用修订后协议前继续访问平台,即视为接受修订后的协议。' }
    ]
  },
  privacy: {
    title: '隐私政策',
    sections: [
      { h: '一、我们收集的信息', p: '账号信息:学号/账号、真实姓名、学段、班级;学习数据:作答记录、批改结果、错题记录与学情统计;使用日志:登录与操作记录。' },
      { h: '二、我们如何使用', p: '用于提供账号服务、帮助教师了解班级学情、由 AI 生成个性化学习建议以及改进产品体验。我们不会将学习数据用于教学无关的用途。' },
      { h: '三、数据安全', p: '平台通过角色权限控制、操作日志审计等方式保护用户数据,各角色仅能访问其职责范围内的数据。' },
      { h: '四、信息共享', p: '我们不出售用户数据。AI 助学等功能均在平台体系内完成计算,不会将您的学习数据提供给无关的第三方。' },
      { h: '五、未成年人保护', p: '平台用户以未成年人学生为主,我们只收集学习所必需的最少信息,并建议监护人在旁指导下使用平台。' },
      { h: '六、您的权利', p: '您可以在平台内查看并更新个人信息;涉及账号变更、注销等需求,请联系教师或管理员处理。' },
      { h: '七、政策更新', p: '本政策如有重大调整,将在平台内以显著方式告知,请您留意。' }
    ]
  },
  help: {
    title: '帮助中心',
    sections: [
      { h: 'Q1 如何注册账号?', p: '在登录页切换到「注册」页签,填写学号/账号、真实姓名、学段和班级(先选学段,班级会自动加载),设置至少 6 位密码,勾选同意协议后点击「注册」。注册账号仅为学生身份,教师账号需由管理员创建。' },
      { h: 'Q2 演示账号是什么?', p: '登录页底部提供一键演示账号:admin(管理员)、teacher01(教师)、student01(学生),密码均为 123456,点击即可快速体验对应角色。' },
      { h: 'Q3 各角色权限有什么区别?', p: '学生:完成作业考试、AI 助学、错题本、学情中心;教师:题库管理、智能出题、组卷、任务派发、批改管理、班级学情、家校反馈;管理员:用户、班级、课程、公告、内容风控、系统日志等全局管理。' },
      { h: 'Q4 AI 批改的结果可信吗?', p: 'AI 支持全题型自动批改、秒级出分,适合日常练习与自我检测;正式考试建议教师到「批改管理」中逐题复核确认。' },
      { h: 'Q5 忘记密码怎么办?', p: '目前暂不支持自助找回密码,请联系您的教师或管理员协助处理。' }
    ]
  }
}
const docMeta = computed(() => docData[activeDoc.value])
const openDoc = (key) => {
  activeDoc.value = key
  docVisible.value = true
}

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
const regForm = reactive({ username: '', realName: '', gradeLevel: null, classId: null, password: '', confirmPwd: '' })

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
  gradeLevel: [{ required: true, message: '请选择学段', trigger: 'change' }],
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
  // 不预加载班级，等选择学段后再加载
})

// 选择学段后自动加载对应班级
watch(() => regForm.gradeLevel, async (val) => {
  regForm.classId = null
  if (!val) {
    classes.value = []
    return
  }
  try {
    const res = await getClasses(val)
    classes.value = res.data || []
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
      classId: regForm.classId,
      gradeLevel: regForm.gradeLevel
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
  background: #fff;
}

/* ===== 左侧插画面板 ===== */
.login-left {
  width: 360px;
  flex-shrink: 0;
  background: linear-gradient(150deg, #23252E 0%, #2A2F3A 45%, #23252E 75%, #1B1F27 100%);
  position: relative;
  overflow: hidden;
}

/* 细网格纹理(左上浓、向下淡出) */
.login-left::before {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image:
    linear-gradient(rgba(255, 255, 255, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.04) 1px, transparent 1px);
  background-size: 32px 32px;
  -webkit-mask-image: radial-gradient(ellipse 130% 100% at 35% 18%, #000 0%, rgba(0, 0, 0, 0.4) 45%, transparent 78%);
  mask-image: radial-gradient(ellipse 130% 100% at 35% 18%, #000 0%, rgba(0, 0, 0, 0.4) 45%, transparent 78%);
}

/* 橙色辉光 + 点阵 */
.login-left::after {
  content: '';
  position: absolute;
  inset: 0;
  z-index: 0;
  background:
    radial-gradient(circle at 88% 10%, rgba(255, 105, 0, 0.22) 0%, rgba(255, 105, 0, 0) 40%),
    radial-gradient(circle at 6% 72%, rgba(255, 150, 77, 0.16) 0%, rgba(255, 150, 77, 0) 38%),
    radial-gradient(rgba(255, 255, 255, 0.05) 1px, transparent 1.6px);
  background-size: auto, auto, 26px 26px;
}

.left-art {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.left-content {
  position: relative;
  z-index: 2;
  padding: 88px 40px 0;
}

.slogan {
  font-size: 32px;
  font-weight: 700;
  color: #fff;
  margin: 0 0 12px 0;
  letter-spacing: 2px;
}

.sub-slogan {
  font-size: 15px;
  color: rgba(255, 255, 255, 0.72);
  margin: 0 0 40px 0;
  letter-spacing: 3px;
}

.features {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
}

.feature-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  flex-shrink: 0;
}

.feature-title {
  font-size: 15px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 3px;
}

.feature-desc {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.65);
}

/* ===== 右侧区域 ===== */
.login-right {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  position: relative;
}

/* 顶部导航 */
.top-bar {
  height: 64px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 32px;
  border-bottom: 1px solid #F0F1F4;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
}

/* 云朵 logo,橙色渐变 + 星火投影 */
.brand-mark {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  filter: drop-shadow(0 5px 12px rgba(255, 105, 0, 0.30));
}

/* 双行品牌名:名称 + 小字 slogan */
.brand-name-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.brand-text {
  font-size: 18px;
  font-weight: 700;
  color: #1F2329;
  letter-spacing: 1.5px;
  line-height: 1.15;
}

.brand-sub {
  font-size: 11px;
  font-weight: 500;
  color: #A0A4B0;
  letter-spacing: 2.5px;
  line-height: 1;
}

.top-links {
  display: flex;
  align-items: center;
  gap: 24px;
}

.top-link {
  font-size: 14px;
  color: #666;
  cursor: pointer;
  transition: color 0.2s;
}

.top-link:hover {
  color: #FF6900;
}

.right-body {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 24px;
  position: relative;
  background:
    radial-gradient(circle at 92% 6%, rgba(255, 105, 0, 0.10) 0%, rgba(255, 105, 0, 0) 36%),
    radial-gradient(circle at 3% 98%, rgba(255, 150, 77, 0.06) 0%, rgba(255, 150, 77, 0) 32%),
    #FAFBFC;
}

/* 左下角点阵,呼应左侧面板纹理 */
.right-body::before {
  content: '';
  position: absolute;
  left: 0;
  bottom: 0;
  width: 240px;
  height: 240px;
  background-image: radial-gradient(rgba(31, 35, 41, 0.055) 1px, transparent 1.6px);
  background-size: 18px 18px;
  -webkit-mask-image: radial-gradient(circle at 0% 100%, #000 0%, transparent 72%);
  mask-image: radial-gradient(circle at 0% 100%, #000 0%, transparent 72%);
  pointer-events: none;
  z-index: 0;
}

/* 右上角点阵,与左下角对角呼应 */
.right-body::after {
  content: '';
  position: absolute;
  right: 0;
  top: 0;
  width: 280px;
  height: 280px;
  background-image: radial-gradient(rgba(31, 35, 41, 0.05) 1px, transparent 1.6px);
  background-size: 18px 18px;
  -webkit-mask-image: radial-gradient(circle at 100% 0%, #000 0%, transparent 70%);
  mask-image: radial-gradient(circle at 100% 0%, #000 0%, transparent 70%);
  pointer-events: none;
  z-index: 0;
}

/* 右上角知识轨道(缓慢旋转,节点沿轨道游走) */
.deco-orbit {
  position: absolute;
  right: -60px;
  top: -40px;
  width: 300px;
  height: 300px;
  z-index: 0;
  pointer-events: none;
  animation: orbitSpin 80s linear infinite;
}

@keyframes orbitSpin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* 大号品牌水印(空心描边,极淡) */
.deco-watermark {
  position: absolute;
  right: 5%;
  top: 50%;
  transform: translateY(-50%);
  font-size: 300px;
  font-weight: 800;
  line-height: 1;
  color: rgba(255, 105, 0, 0.045);
  -webkit-text-stroke: 2px rgba(255, 105, 0, 0.10);
  pointer-events: none;
  z-index: 0;
  user-select: none;
}

/* 浮动学科符号 */
.fsym {
  position: absolute;
  font-weight: 700;
  pointer-events: none;
  z-index: 0;
  animation: fsymFloat 5s ease-in-out infinite;
}

.fsym-a { left: 8%; top: 19%; font-size: 30px; color: rgba(255, 105, 0, 0.40); }
.fsym-b { right: 12%; top: 33%; font-size: 26px; color: rgba(31, 35, 41, 0.20); animation-delay: -1.5s; }
.fsym-c { left: 14%; bottom: 23%; font-size: 21px; color: rgba(255, 122, 30, 0.45); animation-delay: -3s; }
.fsym-d { right: 18%; bottom: 31%; font-size: 23px; color: rgba(31, 35, 41, 0.18); animation-delay: -2s; }

@keyframes fsymFloat {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-10px); }
}

/* 全幅淡网格:呼应左侧面板纹理,自顶部向下淡出 */
.deco-grid {
  position: absolute;
  inset: 0;
  z-index: 0;
  background-image:
    linear-gradient(rgba(31, 35, 41, 0.022) 1px, transparent 1px),
    linear-gradient(90deg, rgba(31, 35, 41, 0.022) 1px, transparent 1px);
  background-size: 36px 36px;
  -webkit-mask-image: radial-gradient(ellipse 100% 80% at 50% 0%, #000 0%, transparent 78%);
  mask-image: radial-gradient(ellipse 100% 80% at 50% 0%, #000 0%, transparent 78%);
  pointer-events: none;
}

/* 左下虚线知识环,与右上轨道对角呼应 */
.deco-ring {
  position: absolute;
  left: -95px;
  bottom: -95px;
  width: 260px;
  height: 260px;
  border: 1.5px dashed rgba(31, 35, 41, 0.10);
  border-radius: 50%;
  z-index: 0;
  pointer-events: none;
}

/* 环上一颗橙色知识点 */
.deco-ring::after {
  content: '';
  position: absolute;
  top: 14px;
  right: 38px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: rgba(255, 105, 0, 0.55);
}

/* 白色卡片 */
.form-card {
  width: 480px;
  max-width: 100%;
  background: #fff;
  border: 1px solid #F0F1F4;
  border-radius: 16px;
  box-shadow: 0 12px 40px rgba(31, 35, 41, 0.08), 0 2px 8px rgba(31, 35, 41, 0.04);
  padding: 44px 44px 40px;
  position: relative;
  z-index: 1;
}

/* 卡片顶部品牌色渐变强调条(左浓右淡) */
.form-card::before {
  content: '';
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 4px;
  border-radius: 15px 15px 0 0;
  background: linear-gradient(90deg, #FF6900 0%, #FF964D 45%, rgba(255, 150, 77, 0) 100%);
}

/* 卡片背后柔光晕,把视觉焦点托起来 */
.form-card::after {
  content: '';
  position: absolute;
  z-index: -1;
  left: 6%;
  right: 6%;
  top: 8%;
  bottom: 8%;
  background: radial-gradient(ellipse at center, rgba(255, 105, 0, 0.12) 0%, rgba(255, 105, 0, 0) 70%);
  filter: blur(28px);
  pointer-events: none;
}

/* Tab 切换 */
.tab-bar {
  display: flex;
  align-items: center;
  gap: 28px;
  margin-bottom: 28px;
}

.tab-item {
  font-size: 20px;
  font-weight: 500;
  color: #999;
  cursor: pointer;
  padding-bottom: 10px;
  border-bottom: 3px solid transparent;
  transition: all 0.25s;
  user-select: none;
}

.tab-item:hover {
  color: #555;
}

.tab-item.active {
  color: #333;
  font-weight: 600;
  border-bottom-color: #FF6900;
}

/* 表单 */
.form-body {
  min-height: 300px;
}

.form-body :deep(.el-form-item) {
  margin-bottom: 22px;
}

.form-body :deep(.check-item) {
  margin-bottom: 18px;
}

/* 小米风格输入框：浅灰底、无描边 */
.form-body :deep(.el-input__wrapper),
.form-body :deep(.el-select__wrapper) {
  background: #F5F5F5;
  box-shadow: none;
  border-radius: 8px;
  padding: 0 14px;
  height: 48px;
}

.form-body :deep(.el-input__wrapper:hover),
.form-body :deep(.el-select__wrapper:hover) {
  background: #EFEFEF;
  box-shadow: none;
}

.form-body :deep(.el-input__wrapper.is-focus),
.form-body :deep(.el-select__wrapper.is-focused) {
  background: #fff;
  box-shadow: 0 0 0 1px #424242 inset;
}

.form-body :deep(.el-input__inner::placeholder) {
  color: #999;
}

/* 提交按钮：小米橙 */
.form-body :deep(.submit-btn) {
  width: 100%;
  height: 46px;
  border-radius: 8px;
  background: #FF6900;
  border-color: #FF6900;
  font-size: 16px;
  letter-spacing: 6px;
  box-shadow: none;
}

.form-body :deep(.submit-btn:hover),
.form-body :deep(.submit-btn:focus) {
  background: #FF7F2A;
  border-color: #FF7F2A;
}

/* 勾选与链接 */
.form-body :deep(.el-checkbox) {
  height: auto;
}

.form-body :deep(.el-checkbox__label) {
  font-size: 13px;
  color: #666;
}

.link-text {
  color: #FF6900;
  cursor: pointer;
}

/* 演示账号 */
.demo-section {
  margin-top: 20px;
}

.demo-section :deep(.el-divider__text) {
  font-size: 12px;
  color: #bbb;
  background: transparent;
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
.form-body :deep(.reg-alert) {
  background: #F7F7F7;
  border: 1px solid #EDEDED;
  border-radius: 8px;
}

.form-body :deep(.reg-alert .el-alert__title) {
  color: #666;
}

.form-body :deep(.reg-alert .el-alert__icon) {
  color: #999;
}

.switch-hint {
  text-align: center;
  margin-top: 18px;
  font-size: 13px;
  color: #999;
}

.form-body :deep(.el-link--primary) {
  color: #FF6900;
}

.form-body :deep(.el-link--primary:hover) {
  color: #FF7F2A;
}

/* 文档弹窗 */
.doc-dialog :deep(.el-dialog__title) {
  font-size: 16px;
  font-weight: 600;
  color: #1F2329;
  letter-spacing: 0.5px;
}

.doc-body {
  max-height: 60vh;
  overflow-y: auto;
  padding: 4px 8px 8px 0;
}

.doc-h {
  font-size: 14px;
  font-weight: 600;
  color: #1F2329;
  margin: 18px 0 6px;
}

.doc-h:first-child {
  margin-top: 0;
}

.doc-p {
  font-size: 13px;
  color: #666;
  line-height: 1.8;
  margin: 0 0 4px;
}

/* 底部信息 */
.footer-info {
  position: absolute;
  left: 32px;
  bottom: 20px;
  font-size: 12px;
  color: #bbb;
}

/* ===== 响应式 ===== */
@media (max-width: 900px) {
  .login-left {
    display: none;
  }

  .top-bar {
    padding: 0 16px;
  }

  .form-card {
    padding: 32px 24px;
  }
}
</style>
