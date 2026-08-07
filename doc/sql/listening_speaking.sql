-- ============================================================
-- 高考英语听说练习模块 建表脚本
-- 表:
--   1. ai_listening_speaking           听说练习题目表
--   2. ai_listening_speaking_record    学生作答记录表
-- ============================================================

-- ---------------------------- 听说练习题目表 ----------------------------
DROP TABLE IF EXISTS ai_listening_speaking;
CREATE TABLE ai_listening_speaking (
  id              BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  title           VARCHAR(100)  NOT NULL COMMENT '题目标题',
  content         TEXT          NOT NULL COMMENT '题目内容(文字说明/朗读文本)',
  reference_text  TEXT          COMMENT '参考文本(标准朗读稿/参考答案)',
  reference_audio VARCHAR(255)  DEFAULT NULL COMMENT '参考音频URL(/upload/xxx)',
  grade_level     INT           NOT NULL DEFAULT 0 COMMENT '学段: 0=通用, 1=小学, 2=初中, 3=高中',
  difficulty      TINYINT       NOT NULL DEFAULT 2 COMMENT '难度: 1=简单, 2=中等, 3=困难',
  question_type   VARCHAR(20)   DEFAULT '模仿朗读' COMMENT '题型(模仿朗读/角色扮演/故事复述)',
  status          TINYINT       NOT NULL DEFAULT 1 COMMENT '状态: 0=下架, 1=上架',
  create_time     DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  update_time     DATETIME      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (id),
  KEY idx_grade_level (grade_level),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='听说练习题目表';

-- ---------------------------- 学生作答记录表 ----------------------------
DROP TABLE IF EXISTS ai_listening_speaking_record;
CREATE TABLE ai_listening_speaking_record (
  id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id           BIGINT        NOT NULL COMMENT '学生ID',
  question_id       BIGINT        NOT NULL COMMENT '题目ID',
  audio_path        VARCHAR(255)  NOT NULL COMMENT '作答音频路径(/upload/audio/xxx)',
  supplement_text   TEXT          COMMENT '文字补充说明(可选)',
  recognized_text   TEXT          COMMENT 'AI 语音识别文本',
  pronunciation_score DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '发音评分(满分25)',
  fluency_score     DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '流利度评分(满分25)',
  grammar_score     DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '语法评分(满分25)',
  content_score     DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '内容评分(满分25)',
  total_score       DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '总分(满分100)',
  ai_feedback       TEXT          COMMENT 'AI 评语与改进建议',
  create_time       DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_question_id (question_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='听说练习作答记录表';

-- ---------------------------- 示例题目数据 ----------------------------
-- 模拟广东省高考英语听说考试三大题型: 模仿朗读 / 角色扮演 / 故事复述
INSERT INTO ai_listening_speaking (title, content, reference_text, reference_audio, grade_level, difficulty, question_type, status, create_time) VALUES
('模仿朗读: The Importance of Reading',
 '请朗读以下短文,注意语音、语调和节奏。\n\nReading is one of the most important skills in life. It opens our minds to new ideas and helps us understand the world better. When we read, we learn not only about facts, but also about feelings and experiences of other people. A good book can be our best friend, bringing us joy and wisdom at any time.',
 'Reading is one of the most important skills in life. It opens our minds to new ideas and helps us understand the world better. When we read, we learn not only about facts, but also about feelings and experiences of other people. A good book can be our best friend, bringing us joy and wisdom at any time.',
 NULL, 3, 2, '模仿朗读', 1, NOW()),
('角色扮演: 询问旅行计划',
 '情景: 你是一名中国学生,正在与外国朋友 Mike 交谈,他想了解你的暑假计划。请根据提示进行角色扮演,向 Mike 提问并回答他的问题。\n\n提示问题:\n1. 你打算去哪里旅行?\n2. 你打算和谁一起去?\n3. 你会带什么东西?',
 'I am going to travel to Beijing with my parents this summer. We plan to visit the Great Wall and the Forbidden City. I will take my camera and a map with me, because I want to take many photos and never get lost.',
 NULL, 3, 2, '角色扮演', 1, NOW()),
('故事复述: The Lost Dog',
 '请听以下故事,然后用自己的话复述。\n\nTom had a little dog named Lucky. One day, Lucky ran away from home. Tom looked for him everywhere, but he could not find him. Tom was very sad. The next morning, Tom heard a knock on the door. When he opened it, he saw Lucky with a kind old lady. The old lady said she found Lucky in the park and brought him home. Tom was so happy that he thanked the old lady again and again.',
 'Tom had a little dog named Lucky. One day, Lucky ran away from home. Tom looked for him everywhere, but he could not find him. Tom was very sad. The next morning, Tom heard a knock on the door. When he opened it, he saw Lucky with a kind old lady. The old lady said she found Lucky in the park and brought him home. Tom was so happy that he thanked the old lady again and again.',
 NULL, 3, 1, '故事复述', 1, NOW()),
('模仿朗读: A Healthy Lifestyle',
 '请朗读以下短文,注意连读与重音。\n\nA healthy lifestyle is very important for everyone. First, we should eat more vegetables and fruit, and drink enough water every day. Second, we should do exercise regularly, such as running, swimming or playing basketball. Third, we need to sleep at least eight hours every night. If we keep these good habits, we will have a strong body and a happy life.',
 'A healthy lifestyle is very important for everyone. First, we should eat more vegetables and fruit, and drink enough water every day. Second, we should do exercise regularly, such as running, swimming or playing basketball. Third, we need to sleep at least eight hours every night. If we keep these good habits, we will have a strong body and a happy life.',
 NULL, 3, 1, '模仿朗读', 1, NOW()),
('角色扮演: 图书馆借书',
 '情景: 你在图书馆遇到图书管理员,想借一本关于科学的书。请根据提示进行对话。\n\n提示问题:\n1. 你最近在读什么书?\n2. 你多久去一次图书馆?\n3. 你更喜欢纸质书还是电子书?',
 'I am reading a book about space science recently. I go to the library twice a month. I prefer paper books to e-books, because paper books are easier on my eyes and I can take notes on them.',
 NULL, 3, 3, '角色扮演', 1, NOW())
ON DUPLICATE KEY UPDATE title = VALUES(title);
