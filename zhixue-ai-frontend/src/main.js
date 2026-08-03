import { createApp } from 'vue'
import { createPinia } from 'pinia'
import ElementPlus from 'element-plus'
import 'element-plus/dist/index.css'
import zhCn from 'element-plus/es/locale/lang/zh-cn'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'
import App from './App.vue'
import router from './router'
import './style.css'

const app = createApp(App)
// 注册所有 Element Plus 图标
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}
app.use(createPinia())
app.use(router)
app.use(ElementPlus, { locale: zhCn })

// 全局错误兜底:组件生命周期/事件处理器中未被捕获的异步异常统一在此记录,
// 避免控制台出现 "Unhandled error" / "Uncaught (in promise)" 噪音
// (request.js 拦截器已负责弹出业务错误提示,这里只做日志记录)
app.config.errorHandler = (err, instance, info) => {
  console.error('[VueError]', info, err)
}
window.addEventListener('unhandledrejection', (e) => {
  console.warn('[UnhandledRejection]', e.reason)
  e.preventDefault() // 阻止浏览器再报 "Uncaught (in promise)"
})

app.mount('#app')
