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

// 动态加载 KaTeX（数学公式渲染）
function loadKaTeX() {
  return new Promise((resolve, reject) => {
    if (typeof katex !== 'undefined') { resolve(); return }
    const link = document.createElement('link')
    link.rel = 'stylesheet'
    link.href = 'https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.css'
    link.onerror = () => {
      link.href = 'https://unpkg.com/katex@0.16.9/dist/katex.min.css'
    }
    document.head.appendChild(link)

    const script = document.createElement('script')
    script.src = 'https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/katex.min.js'
    script.onload = () => {
      // 加载 auto-render
      const autoScript = document.createElement('script')
      autoScript.src = 'https://cdn.jsdelivr.net/npm/katex@0.16.9/dist/contrib/auto-render.min.js'
      autoScript.onerror = () => {
        autoScript.src = 'https://unpkg.com/katex@0.16.9/dist/contrib/auto-render.min.js'
      }
      autoScript.onload = resolve
      autoScript.onerror = reject
      document.head.appendChild(autoScript)
    }
    script.onerror = () => {
      script.src = 'https://unpkg.com/katex@0.16.9/dist/katex.min.js'
      script.onload = () => {
        const autoScript = document.createElement('script')
        autoScript.src = 'https://unpkg.com/katex@0.16.9/dist/contrib/auto-render.min.js'
        autoScript.onload = resolve
        autoScript.onerror = reject
        document.head.appendChild(autoScript)
      }
      script.onerror = reject
      document.head.appendChild(script)
    }
    document.head.appendChild(script)
  })
}

loadKaTeX().then(() => {
  console.log('[KaTeX] loaded successfully')
}).catch(() => {
  console.warn('[KaTeX] failed to load, math formulas will show as raw text')
})

app.mount('#app')
