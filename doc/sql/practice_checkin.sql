-- ============================================================
-- 自主智练与自律打卡模块 建表脚本
-- ============================================================

-- ---------------------------- 自主练习记录表 ----------------------------
DROP TABLE IF EXISTS self_practice_record;
CREATE TABLE self_practice_record (
  id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id           BIGINT        NOT NULL COMMENT '学生ID',
  question_snapshot TEXT          NOT NULL COMMENT '练习题目快照(JSON数组,含题干/选项/正确答案/题型)',
  total_count       INT           NOT NULL COMMENT '总题量',
  correct_count     INT           NOT NULL DEFAULT 0 COMMENT '正确数',
  accuracy          DECIMAL(5,2)  NOT NULL DEFAULT 0.00 COMMENT '正确率(保留两位小数)',
  duration_seconds  INT           DEFAULT 0 COMMENT '练习耗时(秒)',
  generate_source   VARCHAR(20)   NOT NULL DEFAULT '系统推荐' COMMENT '生成来源(错题生成/系统推荐)',
  create_time       DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  KEY idx_user_id (user_id),
  KEY idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='自主练习记录表';

-- ---------------------------- 用户打卡表 ----------------------------
DROP TABLE IF EXISTS user_check_in;
CREATE TABLE user_check_in (
  id                BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id           BIGINT        NOT NULL COMMENT '学生ID',
  check_in_date     DATE          NOT NULL COMMENT '打卡日期(年月日)',
  continuous_days   INT           NOT NULL DEFAULT 1 COMMENT '连续打卡天数',
  total_points      INT           NOT NULL DEFAULT 0 COMMENT '累计总积分',
  reward_badge      VARCHAR(20)   DEFAULT NULL COMMENT '获得勋章(铜牌/银牌/金牌)',
  create_time       DATETIME      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_date (user_id, check_in_date),
  KEY idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户打卡表';
