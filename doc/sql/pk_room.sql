-- 同学PK功能数据库表

-- PK房间表
CREATE TABLE IF NOT EXISTS `pk_room` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_code` VARCHAR(20) NOT NULL COMMENT '房间号(6位随机码)',
  `creator_id` BIGINT NOT NULL COMMENT '创建者ID',
  `subject_id` BIGINT NOT NULL COMMENT '学科ID',
  `question_count` INT NOT NULL DEFAULT 10 COMMENT '题目数量',
  `time_limit_seconds` INT NOT NULL DEFAULT 600 COMMENT '限时(秒)',
  `question_ids` TEXT DEFAULT NULL COMMENT '题目ID列表(JSON数组)',
  `status` TINYINT NOT NULL DEFAULT 0 COMMENT '状态: 0-等待中, 1-进行中, 2-已结束',
  `start_time` DATETIME DEFAULT NULL COMMENT '开始时间',
  `end_time` DATETIME DEFAULT NULL COMMENT '结束时间',
  `create_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_code` (`room_code`),
  KEY `idx_creator` (`creator_id`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PK房间表';

-- PK房间成员表
CREATE TABLE IF NOT EXISTS `pk_room_member` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_id` BIGINT NOT NULL COMMENT '房间ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `real_name` VARCHAR(50) DEFAULT NULL COMMENT '真实姓名',
  `answered_count` INT NOT NULL DEFAULT 0 COMMENT '已答题数',
  `correct_count` INT NOT NULL DEFAULT 0 COMMENT '正确题数',
  `accuracy` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '正确率',
  `finish_time` DATETIME DEFAULT NULL COMMENT '完成时间',
  `join_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_user` (`room_id`, `user_id`),
  KEY `idx_room` (`room_id`),
  KEY `idx_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PK房间成员表';

-- PK答题记录表
CREATE TABLE IF NOT EXISTS `pk_answer_record` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  `room_id` BIGINT NOT NULL COMMENT '房间ID',
  `user_id` BIGINT NOT NULL COMMENT '用户ID',
  `question_id` BIGINT NOT NULL COMMENT '题目ID',
  `user_answer` VARCHAR(500) DEFAULT NULL COMMENT '用户答案',
  `is_correct` TINYINT DEFAULT NULL COMMENT '是否正确: 0-错误, 1-正确',
  `answer_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '答题时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_room_user_question` (`room_id`, `user_id`, `question_id`),
  KEY `idx_room_user` (`room_id`, `user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='PK答题记录表';
