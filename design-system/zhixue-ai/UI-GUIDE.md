# 智学AI前端 UI 重构规范（基于 ui-ux-pro-max 设计系统）

> 设计系统源：`design-system/zhixue-ai/MASTER.md`
> 本文件为页面级实现规范，所有模块（除 Login 外）重构时统一遵守。

## 1. 设计令牌（已在 style.css 全局定义，直接使用 CSS 变量）

| 角色 | 值 | 变量 |
|------|-----|------|
| 主色 | #0D9488 | `var(--color-primary)` |
| 次色 | #2DD4BF | `var(--color-secondary)` |
| 强调色 | #D97706 | `var(--color-accent)` |
| 背景 | #F0FDFA | `var(--color-background)` |
| 前景文字 | #134E4A | `var(--color-foreground)` |
| 弱化色 | #E8F1F4 | `var(--color-muted)` |
| 次要文字 | #60757A | `var(--color-text-secondary)` |
| 间距 | 4/8/16/24/32px | `--space-xs/sm/md/lg/xl` |
| 阴影 | sm/md/lg | `--shadow-sm/md/lg` |

## 2. 页面通用结构（所有页面统一）

```html
<div class="page-container">
  <!-- 页面标题区 -->
  <div class="page-header">
    <div class="page-header-left">
      <h1 class="page-title">页面标题</h1>
      <p class="page-subtitle">副标题/说明</p>
    </div>
    <div class="page-header-right">
      <!-- 主操作按钮 -->
      <el-button type="primary">新增</el-button>
    </div>
  </div>

  <!-- 内容卡片 -->
  <el-card class="content-card">
    ...表格/表单...
  </el-card>
</div>
```

## 3. 标准 CRUD 页模式（admin 用户/角色/班级/学科/公告/日志、teacher 题库/试卷等）

- 页面标题 + 副标题（说明该页用途）
- 主操作按钮放标题右侧（新增类）
- 筛选表单：`<el-form :inline="true">` 内联，置于卡片顶部
- 表格：`stripe` + 圆角（全局已生效），操作列固定右侧、按钮 `size="small"`
- 分页：`layout="total, prev, pager, next"` + `class="mt-20"`
- 弹窗：表单 `label-width="100px"`，footer 取消+保存

## 4. 统计卡片（Dashboard / 学情 / 班级学情）

统一使用全局 `.stat-card`（渐变顶条 + 大数字 + 装饰光斑，已在 style.css 定义），
数字可用内联 style 设不同颜色区分语义。

## 5. 图表卡片

`<el-card>` + `#header` 标题 + 固定高度（300px 左右）容器，ECharts 主色用 #0D9488。

## 6. 交互/动效规范（技能 Pre-Delivery Checklist）

- ❌ 禁止用 emoji 当图标 → 用 Element Plus 图标（已全局注册）
- ✅ 可点击元素 `cursor:pointer`
- ✅ 悬停过渡 150-300ms
- ✅ 文字对比度 ≥ 4.5:1（前景 #134E4A on 白底可满足）
- ✅ 焦点可见（全局 :focus-visible 已生效）
- ✅ prefers-reduced-motion 尊重（全局已生效）
- ✅ 响应式 375/768/1024/1440px：栅格用 el-row/el-col，避免固定宽度溢出

## 7. 每个模块的页面清单

### admin（9 页）
Dashboard(数据大屏) / User(用户管理) / Role(角色权限) / Class(班级管理) /
Course(学科管理) / AiConfig(AI配置) / Notice(公告管理) / Moderation(内容风控) / Log(系统日志)

### student（12 页）
Dashboard(学习首页) / PaperList(作业考试) / Tutor(AI答疑) / ListeningSpeaking(英语听说) /
ErrorBook(错题本) / StudyCenter(学情中心) / SelfPractice(自主智练) /
PracticeConfig(自主智练配置) / PracticePaper(自主智练答题) / PkArena(同学PK) /
PkLs(听说PK) / TakeExam(在线作答)

### teacher（10 页）
Dashboard(教师首页) / Question(题库管理) / Paper(作业考试管理) / PaperEdit(试卷编辑) /
Correct(批改管理) / CorrectDetail(批改详情) / ClassAnalysis(班级学情) /
Feedback(家校反馈) / StudentManage(学员管理) / LsHomework(听说作业)

## 8. 局部样式建议

- 页面若已有 scoped style，保留并统一为设计令牌变量
- 特殊交互页（Tutor 聊天、PracticePaper 答题、PkArena PK）保留现有交互逻辑，仅统一视觉
- 不使用 emoji 作为功能图标（现有 `📎` `💬` `🏆` 等需替换为 Element Plus 图标）
