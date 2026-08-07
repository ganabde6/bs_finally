-- 英语听说 PK 表
USE zhixue_ai;

CREATE TABLE IF NOT EXISTS pk_ls_room (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_code VARCHAR(20) NOT NULL COMMENT '房间号(6位)',
  creator_id BIGINT NOT NULL COMMENT '创建者ID',
  challenger_id BIGINT DEFAULT NULL COMMENT '挑战者ID',
  question_id BIGINT DEFAULT NULL COMMENT '关联题目ID(可选)',
  question_title VARCHAR(255) COMMENT '题目标题',
  question_content TEXT COMMENT '题目内容',
  reference_text TEXT COMMENT '参考文本',
  question_type VARCHAR(50) COMMENT '题型',
  score_points TEXT COMMENT '评分要点',
  status INT DEFAULT 0 COMMENT '0=等待挑战,1=已接受,2=已完成',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_room_code (room_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英语听说PK房间';

CREATE TABLE IF NOT EXISTS pk_ls_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  room_id BIGINT NOT NULL COMMENT '房间ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  audio_path VARCHAR(255) COMMENT '音频文件路径',
  recognized_text TEXT COMMENT 'AI识别文本',
  pronunciation_score DECIMAL(5,2) DEFAULT 0 COMMENT '发音分(0-25)',
  fluency_score DECIMAL(5,2) DEFAULT 0 COMMENT '流利度分(0-25)',
  grammar_score DECIMAL(5,2) DEFAULT 0 COMMENT '语法分(0-25)',
  content_score DECIMAL(5,2) DEFAULT 0 COMMENT '内容分(0-25)',
  total_score DECIMAL(5,2) DEFAULT 0 COMMENT '总分(0-100)',
  ai_feedback TEXT COMMENT 'AI评语',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  KEY idx_room_id (room_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='英语听说PK作答记录';
