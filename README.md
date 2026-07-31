# 智学AI学习测评系统

> 基于 JDK 11 + SpringBoot + Vue3 的智慧教育平台，实现作业考试智能批改、学情大数据分析、个性化 AI 助学辅导、智能考试风控等功能。

## 项目简介

智学AI学习测评系统是一套完整的智慧教育解决方案，涵盖学生端、教师端、管理端三大平台，构建「学习 → 练习 → 测评 → 批改 → 分析 → 辅导」的全闭环 AI 智慧教学体系。

### 核心特色

- **AI 深度赋能**：全题型智能批改、学情大数据分析、个性化助学辅导
- **业务场景完善**：均衡兼顾日常作业与阶段性考试
- **三端协同架构**：学生端、教师端、管理端数据互通、权限隔离
- **实时数据同步**：WebSocket 推送批改结果与考试状态

## 技术栈

### 后端

- JDK 11
- SpringBoot 2.7.x
- MyBatis-Plus 3.5.x
- MySQL 8.0
- Spring Security + JWT
- WebSocket (STOMP)
- Lombok / Hutool / FastJSON2 / POI

### 前端

- Vue3 + Vite
- Element Plus
- Pinia
- Vue Router
- Axios
- ECharts
- WebSocket

## 项目结构

```
d:\bs_finally
├── zhixue-ai-backend/          # 后端项目
│   ├── src/main/java/com/zhixue/ai/
│   │   ├── common/              # 公共模块(常量/异常/结果/工具)
│   │   ├── config/              # 配置类(CORS/MyBatis/Security/WebSocket)
│   │   ├── security/            # 安全模块(JWT过滤/用户详情)
│   │   └── module/              # 业务模块
│   │       ├── system/          # 系统管理(用户/角色/权限/班级/学科)
│   │       ├── exam/            # 题库与考试(题目/试卷/作答)
│   │       ├── ai/              # AI引擎(批改/分析/风控/助学)
│   │       └── monitor/         # 监控模块(数据大屏)
│   ├── src/main/resources/
│   │   └── application.yml      # 配置文件
│   └── pom.xml                  # Maven依赖
├── zhixue-ai-frontend/          # 前端项目
│   ├── src/
│   │   ├── api/                 # API接口封装
│   │   ├── layout/              # 布局组件(学生/教师/管理端)
│   │   ├── router/              # 路由配置
│   │   ├── stores/              # 状态管理
│   │   └── views/               # 页面组件
│   │       ├── student/         # 学生端(6个页面)
│   │       ├── teacher/         # 教师端(8个页面)
│   │       └── admin/           # 管理端(9个页面)
│   ├── package.json
│   └── vite.config.js
├── doc/
│   └── sql/
│       ├── schema.sql           # 数据库建表脚本
│       └── data.sql             # 初始化数据
└── README.md
```

## 环境要求

| 组件 | 版本要求 | 说明 |
|:---|:---|:---|
| JDK | 11+ | 后端运行环境 |
| Maven | 3.6+ | 后端构建工具 |
| MySQL | 8.0+ | 数据库 |
| Node.js | 16+ | 前端构建环境 |
| NPM | 8+ | 前端包管理器 |
| Redis | 可选 | 后期可扩展缓存 |

## 快速开始

### 1. 数据库初始化

```bash
# 1. 创建数据库
mysql -u root -p
CREATE DATABASE zhixue_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

# 2. 导入表结构和初始数据
USE zhixue_ai;
SOURCE d:/bs_finally/doc/sql/schema.sql;
SOURCE d:/bs_finally/doc/sql/data.sql;
```

### 2. 后端启动

```bash
# 1. 修改数据库配置(如有需要)
# 编辑 zhixue-ai-backend/src/main/resources/application.yml
# spring.datasource.url/username/password

# 2. Maven 打包
cd d:/bs_finally/zhixue-ai-backend
mvn clean package -DskipTests

# 3. 启动后端服务
java -jar target/zhixue-ai-backend-1.0.0.jar

# 或使用 Maven 直接运行
mvn spring-boot:run
```

后端启动成功后访问：http://localhost:8080

### 3. 前端启动

```bash
# 1. 安装依赖
cd d:/bs_finally/zhixue-ai-frontend
npm install

# 2. 开发模式运行
npm run dev

# 3. 生产环境构建
npm run build
# 构建产物在 dist/ 目录
```

前端开发服务器：http://localhost:5173

## 部署架构

### 生产环境部署

```
┌───────────────┐         ┌───────────────┐         ┌───────────────┐
│  Nginx        │  反向   │  SpringBoot   │  数据   │   MySQL 8.0   │
│  静态资源/代理 │ ──────▶ │  应用服务     │ ──────▶ │   数据库      │
└───────────────┘         └───────────────┘         └───────────────┘
                                  │
                                  ▼
                          ┌───────────────┐
                          │  AI 本地规则  │
                          │  + 第三方接口 │
                          └───────────────┘
```

### Nginx 配置示例

```nginx
server {
    listen 80;
    server_name your-domain.com;

    # 前端静态资源
    location / {
        root /path/to/zhixue-ai-frontend/dist;
        index index.html;
        try_files $uri $uri/ /index.html;
    }

    # 后端 API 代理
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    # WebSocket 代理
    location /ws/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

## 功能模块

### 学生端功能

| 模块 | 功能 |
|:---|:---|
| 学习首页 | 待完成任务、最近成绩、学情概览 |
| 作业/考试 | 在线作答、拍照上传、智能计时、自动交卷 |
| AI 助学 | 全科答疑、错题精讲、作文润色 |
| 错题本 | 错题归档、变式题推送、掌握状态管理 |
| 学情中心 | 成绩趋势、知识点掌握、AI 学情报告 |

### 教师端功能

| 模块 | 功能 |
|:---|:---|
| 教师首页 | 教学数据统计、快捷入口 |
| 题库管理 | 题目 CRUD、AI 智能组卷 |
| 作业/考试管理 | 试卷发布、分层推送、状态管理 |
| 批改管理 | AI 批改、手动微调、相似度查重 |
| 班级学情 | 平均分/及格率统计、分层分析、教学建议 |
| 家校反馈 | AI 生成学情反馈单 |

### 管理端功能

| 模块 | 功能 |
|:---|:---|
| 数据大屏 | 全校统计、图表可视化、实时日志 |
| 用户管理 | 用户 CRUD、角色分配、密码重置 |
| 角色权限 | 角色 CRUD、权限树分配 |
| 班级管理 | 班级 CRUD、班主任分配 |
| 课程管理 | 学科管理、教师任课分配 |
| AI 配置 | 批改严苛度、功能开关配置 |
| 公告管理 | 公告发布、目标角色定向 |
| 内容风控 | 内容预检、合规性检测 |
| 系统日志 | 操作日志查询、模块筛选 |

## 测试账号

| 角色 | 账号 | 密码 | 权限范围 |
|:---|:---|:---|:---|
| 超级管理员 | admin | admin123 | 全部功能 |
| 教师账号 | teacher001 | 123456 | 教师端功能 |
| 学生账号 | student001 | 123456 | 学生端功能 |

> 初始密码建议在首次登录后修改

## API 接口规范

### 统一响应格式

```json
{
  "code": 200,
  "message": "操作成功",
  "data": {},
  "timestamp": 1719500000000
}
```

### 常用状态码

| 状态码 | 说明 |
|:---|:---|
| 200 | 成功 |
| 400 | 参数错误 |
| 401 | 未登录或 token 失效 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |
| 1001 | AI 服务调用失败 |
| 1002 | 考试风控预警 |

### 接口路由

| 端口 | 路由前缀 |
|:---|:---|
| 公共接口 | `/api/auth/**`, `/api/common/**` |
| 学生端 | `/api/student/**` |
| 教师端 | `/api/teacher/**` |
| 管理端 | `/api/admin/**` |

## 开发说明

### 本地开发

1. 启动 MySQL 数据库
2. 启动后端：`mvn spring-boot:run`
3. 启动前端：`npm run dev`
4. 访问：http://localhost:5173

### 数据库表清单

| 模块 | 数据表 | 说明 |
|:---|:---|:---|
| 用户权限 | sys_user | 用户表 |
| 用户权限 | sys_role | 角色表 |
| 用户权限 | sys_permission | 权限表 |
| 用户权限 | sys_role_permission | 角色权限关联表 |
| 用户权限 | sys_class | 班级表 |
| 用户权限 | sys_subject | 学科表 |
| 用户权限 | sys_teacher_class | 教师任课表 |
| 题库测评 | exam_question | 题目表 |
| 题库测评 | exam_paper | 试卷表 |
| 题库测评 | exam_paper_question | 试卷题目关联表 |
| 作答批改 | exam_answer | 作答记录表 |
| AI 学情 | ai_correct_record | AI 批改记录表 |
| AI 学情 | ai_error_book | 错题本表 |
| AI 学情 | ai_study_analysis | 学情分析表 |
| AI 学情 | ai_class_analysis | 班级学情分析表 |
| AI 学情 | ai_tutor_chat | AI 助学聊天记录表 |
| AI 学情 | ai_variant_question | 变式题表 |
| AI 学情 | ai_model_config | AI 模型配置表 |
| 风控 | exam_risk_log | 考试风控日志表 |
| 系统 | sys_notice | 公告表 |
| 系统 | sys_log | 系统操作日志表 |
| 系统 | sys_file | 文件表 |

### 关键配置项

**后端 application.yml 关键配置：**

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/zhixue_ai?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai
    username: root
    password: your_password

jwt:
  secret: your-jwt-secret-key
  expiration: 86400000  # 24小时

file:
  upload-path: /data/upload/  # 文件上传路径
```

**前端 vite.config.js 关键配置：**

```javascript
export default defineConfig({
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
})
```

## 项目特色

1. **AI 驱动的智能批改**：支持客观题、主观题、计算题等多题型批改，计算题支持步骤分
2. **学情大数据分析**：基于历史数据的个人与班级学情画像，为分层教学提供数据支撑
3. **个性化 AI 助学**：绑定个人错题数据，提供精准答疑与变式题推送
4. **智能考试风控**：切屏监测、答案雷同查重、人脸识别等多维度防作弊
5. **实时数据同步**：WebSocket 推送批改结果与考试状态，三端数据毫秒级同步

## 技术亮点

- 前后端分离架构，符合现代软件开发规范
- JWT 无状态认证，支持分布式部署
- MyBatis-Plus 简化 CRUD，提升开发效率
- WebSocket 实时通信，增强用户体验
- ECharts 数据可视化，直观展示学情数据
- 模块化 AI 引擎，易于扩展第三方接口

## 注意事项

1. 生产环境请修改数据库密码和 JWT 密钥
2. 文件上传路径需要有写入权限
3. 生产环境建议配置 HTTPS
4. 定期备份数据库
5. 建议开启 Redis 缓存以提升性能（可选）

## 许可证

本项目仅用于毕业设计与学习交流，请勿用于商业用途。

## 联系方式

如有问题或建议，请通过以下方式联系：

- 项目文档：`智学AI学习测评系统-开发设计文档.md`
- 技术支持：请参考设计文档或联系开发者