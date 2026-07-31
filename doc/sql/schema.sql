-- ============================================================
-- 智学AI学习测评系统 数据库建表脚本
-- MySQL 8.0  字符集 utf8mb4  排序规则 utf8mb4_general_ci
-- ============================================================

DROP DATABASE IF EXISTS zhixue_ai;
CREATE DATABASE zhixue_ai DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE zhixue_ai;

-- ---------------------------- 学科表 ----------------------------
DROP TABLE IF EXISTS sys_subject;
CREATE TABLE sys_subject (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  subject_name VARCHAR(50)  NOT NULL COMMENT '学科名称',
  subject_code VARCHAR(20)  NOT NULL COMMENT '学科编码',
  sort         INT          DEFAULT 0 COMMENT '排序',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_code (subject_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学科表';

-- ---------------------------- 班级表 ----------------------------
DROP TABLE IF EXISTS sys_class;
CREATE TABLE sys_class (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  class_name  VARCHAR(50) NOT NULL COMMENT '班级名称',
  grade       VARCHAR(20) COMMENT '年级',
  head_teacher_id BIGINT  COMMENT '班主任ID',
  status      TINYINT     DEFAULT 1 COMMENT '0禁用 1启用',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级表';

-- ---------------------------- 角色表 ----------------------------
DROP TABLE IF EXISTS sys_role;
CREATE TABLE sys_role (
  id         BIGINT      NOT NULL AUTO_INCREMENT,
  role_code  VARCHAR(30) NOT NULL COMMENT '角色编码',
  role_name  VARCHAR(50) NOT NULL COMMENT '角色名称',
  description VARCHAR(200),
  create_time DATETIME   DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_code (role_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色表';

-- ---------------------------- 权限表(菜单级) ----------------------------
DROP TABLE IF EXISTS sys_permission;
CREATE TABLE sys_permission (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  parent_id   BIGINT      DEFAULT 0 COMMENT '父菜单ID',
  perm_code   VARCHAR(50) NOT NULL COMMENT '权限编码',
  perm_name   VARCHAR(50) NOT NULL COMMENT '菜单/权限名称',
  perm_type   TINYINT     DEFAULT 1 COMMENT '1菜单 2按钮',
  path        VARCHAR(100) COMMENT '前端路由路径',
  component   VARCHAR(100) COMMENT '前端组件路径',
  icon        VARCHAR(50),
  sort        INT         DEFAULT 0,
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_perm_code (perm_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='权限表';

-- ---------------------------- 角色-权限关联表 ----------------------------
DROP TABLE IF EXISTS sys_role_permission;
CREATE TABLE sys_role_permission (
  id            BIGINT NOT NULL AUTO_INCREMENT,
  role_id       BIGINT NOT NULL,
  permission_id BIGINT NOT NULL,
  PRIMARY KEY (id),
  UNIQUE KEY uk_role_perm (role_id, permission_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='角色权限关联表';

-- ---------------------------- 用户表 ----------------------------
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  username    VARCHAR(50)  NOT NULL COMMENT '登录账号',
  password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
  real_name   VARCHAR(50)  NOT NULL COMMENT '真实姓名',
  role_id     BIGINT       NOT NULL COMMENT '角色ID',
  class_id    BIGINT       COMMENT '班级ID(学生适用)',
  status      TINYINT      DEFAULT 1 COMMENT '0禁用 1启用',
  avatar      VARCHAR(255) COMMENT '头像URL',
  phone       VARCHAR(20),
  email       VARCHAR(50),
  last_login  DATETIME,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT      DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------- 教师-班级-学科关系表 ----------------------------
DROP TABLE IF EXISTS sys_teacher_class;
CREATE TABLE sys_teacher_class (
  id          BIGINT NOT NULL AUTO_INCREMENT,
  teacher_id  BIGINT NOT NULL,
  class_id    BIGINT NOT NULL,
  subject_id  BIGINT NOT NULL,
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师班级学科关系表';

-- ---------------------------- 题库表 ----------------------------
DROP TABLE IF EXISTS exam_question;
CREATE TABLE exam_question (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  subject_id      BIGINT        NOT NULL COMMENT '学科ID',
  question_type   TINYINT       NOT NULL COMMENT '题型 1单选 2多选 3判断 4填空 5简答 6作文 7计算',
  difficulty      TINYINT       DEFAULT 3 COMMENT '难度 1-5,5最难',
  knowledge_point VARCHAR(100)  COMMENT '知识点',
  content         TEXT          NOT NULL COMMENT '题干',
  options         TEXT          COMMENT '选项JSON(单选/多选/判断适用)',
  standard_answer TEXT          NOT NULL COMMENT '标准答案',
  score_point     TEXT          COMMENT '得分点JSON',
  analysis        TEXT          COMMENT '解析',
  full_score      DECIMAL(5,2)  DEFAULT 0 COMMENT '题目默认满分',
  creator_id      BIGINT        COMMENT '创建者ID',
  create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT       DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_subject (subject_id),
  KEY idx_type (question_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='题库表';

-- ---------------------------- 试卷/作业表 ----------------------------
DROP TABLE IF EXISTS exam_paper;
CREATE TABLE exam_paper (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  paper_name   VARCHAR(100)  NOT NULL COMMENT '试卷/作业名称',
  paper_type   TINYINT       NOT NULL COMMENT '1作业 2考试',
  subject_id   BIGINT        NOT NULL,
  class_id     BIGINT        NOT NULL COMMENT '目标班级',
  creator_id   BIGINT        NOT NULL COMMENT '创建教师ID',
  total_score  DECIMAL(6,2)  DEFAULT 0 COMMENT '试卷总分',
  duration     INT           DEFAULT 60 COMMENT '考试时长(分钟)',
  publish_time DATETIME      COMMENT '发布时间',
  deadline     DATETIME      COMMENT '截止时间',
  status       TINYINT       DEFAULT 0 COMMENT '0草稿 1已发布 2已结束',
  description  VARCHAR(500),
  create_time  DATETIME      DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT       DEFAULT 0,
  PRIMARY KEY (id),
  KEY idx_creator (creator_id),
  KEY idx_class (class_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷/作业表';

-- ---------------------------- 试卷-题目关联表 ----------------------------
DROP TABLE IF EXISTS exam_paper_question;
CREATE TABLE exam_paper_question (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  paper_id    BIGINT       NOT NULL,
  question_id BIGINT       NOT NULL,
  score       DECIMAL(5,2) NOT NULL COMMENT '本题分值',
  sort        INT          DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_paper_question (paper_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='试卷题目关联表';

-- ---------------------------- 作答提交记录表(整卷级) ----------------------------
DROP TABLE IF EXISTS exam_answer;
CREATE TABLE exam_answer (
  id            BIGINT       NOT NULL AUTO_INCREMENT,
  paper_id      BIGINT       NOT NULL,
  student_id    BIGINT       NOT NULL,
  submit_type   TINYINT      DEFAULT 1 COMMENT '1在线 2拍照 3语音',
  duration      INT          DEFAULT 0 COMMENT '实际作答时长(秒)',
  total_score   DECIMAL(6,2) COMMENT '总得分',
  status        TINYINT      DEFAULT 0 COMMENT '0未提交 1已提交 2已批改 3已复核',
  submit_time   DATETIME,
  start_time    DATETIME,
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  update_time   DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_paper_student (paper_id, student_id),
  KEY idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='作答提交记录表';

-- ---------------------------- AI批改记录表(每题级) ----------------------------
DROP TABLE IF EXISTS ai_correct_record;
CREATE TABLE ai_correct_record (
  id             BIGINT       NOT NULL AUTO_INCREMENT,
  answer_id      BIGINT       NOT NULL COMMENT '作答记录ID',
  question_id    BIGINT       NOT NULL,
  student_answer TEXT         COMMENT '学生答案',
  score          DECIMAL(5,2) NOT NULL COMMENT 'AI评分',
  full_score     DECIMAL(5,2) NOT NULL COMMENT '题目满分',
  is_correct     TINYINT      DEFAULT 0 COMMENT '0错 1对 2部分对',
  error_tag      VARCHAR(255) COMMENT '错误标注JSON',
  score_detail   TEXT         COMMENT '得分详情(步骤分)',
  correct_remark TEXT         COMMENT '批改备注',
  correct_type   TINYINT      DEFAULT 1 COMMENT '1AI自动 2教师手动',
  ai_model       VARCHAR(50)  DEFAULT 'LOCAL-RULE-V1',
  create_time    DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_answer (answer_id),
  KEY idx_question (question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI批改记录表';

-- ---------------------------- 错题本表 ----------------------------
DROP TABLE IF EXISTS ai_error_book;
CREATE TABLE ai_error_book (
  id              BIGINT      NOT NULL AUTO_INCREMENT,
  student_id      BIGINT      NOT NULL,
  question_id     BIGINT      NOT NULL,
  paper_id        BIGINT,
  correct_id      BIGINT      COMMENT '关联批改记录ID',
  error_type      TINYINT     DEFAULT 1 COMMENT '1知识点缺失 2计算失误 3审题错误 4思路错误 5表达不清',
  knowledge_point VARCHAR(100),
  review_status   TINYINT     DEFAULT 0 COMMENT '0未复盘 1已复盘 2已掌握',
  create_time     DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_time     DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错题本表';

-- ---------------------------- 变式题表 ----------------------------
DROP TABLE IF EXISTS ai_variant_question;
CREATE TABLE ai_variant_question (
  id                BIGINT      NOT NULL AUTO_INCREMENT,
  source_question_id BIGINT     NOT NULL COMMENT '原题ID',
  student_id        BIGINT      NOT NULL,
  content           TEXT        NOT NULL COMMENT '变式题题干',
  options           TEXT,
  standard_answer   TEXT,
  knowledge_point   VARCHAR(100),
  is_solved         TINYINT     DEFAULT 0 COMMENT '0未作答 1已作答',
  create_time       DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI变式题表';

-- ---------------------------- 个人学情分析表 ----------------------------
DROP TABLE IF EXISTS ai_study_analysis;
CREATE TABLE ai_study_analysis (
  id              BIGINT      NOT NULL AUTO_INCREMENT,
  student_id      BIGINT      NOT NULL,
  subject_id      BIGINT,
  avg_score       DECIMAL(6,2) COMMENT '平均分',
  trend            TEXT        COMMENT '成绩趋势JSON',
  weak_points      TEXT        COMMENT '薄弱知识点JSON',
  strong_points    TEXT        COMMENT '优势模块JSON',
  suggestion       TEXT        COMMENT 'AI提升建议',
  update_time      DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_student_subject (student_id, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='个人学情分析表';

-- ---------------------------- 班级学情分析表 ----------------------------
DROP TABLE IF EXISTS ai_class_analysis;
CREATE TABLE ai_class_analysis (
  id              BIGINT      NOT NULL AUTO_INCREMENT,
  class_id        BIGINT      NOT NULL,
  subject_id      BIGINT,
  avg_score       DECIMAL(6,2),
  pass_rate       DECIMAL(5,2) COMMENT '及格率',
  excellent_rate  DECIMAL(5,2) COMMENT '优秀率',
  common_errors   TEXT         COMMENT '共性薄弱点JSON',
  layering        TEXT         COMMENT '分层归类JSON',
  teaching_advice TEXT         COMMENT '教学优化建议',
  update_time     DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_class_subject (class_id, subject_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='班级学情分析表';

-- ---------------------------- AI助学对话表 ----------------------------
DROP TABLE IF EXISTS ai_tutor_chat;
CREATE TABLE ai_tutor_chat (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  student_id  BIGINT      NOT NULL,
  role        VARCHAR(10) NOT NULL COMMENT 'user/assistant',
  content     TEXT        NOT NULL,
  chat_type   TINYINT     DEFAULT 1 COMMENT '1文字 2拍照 3语音',
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_student (student_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI助学对话记录表';

-- ---------------------------- 考试风控日志表 ----------------------------
DROP TABLE IF EXISTS exam_risk_log;
CREATE TABLE exam_risk_log (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  answer_id    BIGINT      NOT NULL,
  student_id   BIGINT      NOT NULL,
  risk_type    TINYINT     NOT NULL COMMENT '1切屏 2超时 3人脸异常 4答案雷同 5离开窗口',
  risk_level   TINYINT     DEFAULT 1 COMMENT '1低 2中 3高',
  description  VARCHAR(500),
  create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_answer (answer_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='考试风控日志表';

-- ---------------------------- 系统操作日志表 ----------------------------
DROP TABLE IF EXISTS sys_log;
CREATE TABLE sys_log (
  id          BIGINT      NOT NULL AUTO_INCREMENT,
  user_id     BIGINT,
  username    VARCHAR(50),
  module      VARCHAR(50) COMMENT '操作模块',
  operation   VARCHAR(200) COMMENT '操作描述',
  method      VARCHAR(200) COMMENT '请求方法',
  params      TEXT        COMMENT '请求参数',
  ip          VARCHAR(50),
  cost_ms     INT,
  create_time DATETIME    DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统操作日志表';

-- ---------------------------- 公告表 ----------------------------
DROP TABLE IF EXISTS sys_notice;
CREATE TABLE sys_notice (
  id           BIGINT      NOT NULL AUTO_INCREMENT,
  title        VARCHAR(100) NOT NULL,
  content      TEXT         NOT NULL,
  target_role  VARCHAR(30)  COMMENT '目标角色编码(空表示全部)',
  publisher_id BIGINT,
  publish_time DATETIME,
  status       TINYINT      DEFAULT 0 COMMENT '0草稿 1已发布',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='公告表';

-- ---------------------------- AI模型配置表 ----------------------------
DROP TABLE IF EXISTS ai_model_config;
CREATE TABLE ai_model_config (
  id           BIGINT       NOT NULL AUTO_INCREMENT,
  config_key   VARCHAR(50)  NOT NULL,
  config_value VARCHAR(200) NOT NULL,
  config_name  VARCHAR(100),
  description  VARCHAR(255),
  update_time  DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_key (config_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='AI模型配置表';

-- ---------------------------- 文件元数据表 ----------------------------
DROP TABLE IF EXISTS sys_file;
CREATE TABLE sys_file (
  id          BIGINT       NOT NULL AUTO_INCREMENT,
  file_name   VARCHAR(200) NOT NULL,
  file_path   VARCHAR(500) NOT NULL,
  file_size   BIGINT,
  file_type   VARCHAR(50),
  uploader_id BIGINT,
  create_time DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='文件元数据表';
