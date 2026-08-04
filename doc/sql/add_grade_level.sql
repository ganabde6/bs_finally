-- ============================================
-- 学段区分方案：方案一 + 方案三 组合
-- 1. sys_user 表新增 grade_level 字段（用户学段）
-- 2. sys_class 表新增 grade_level 字段（班级学段）
-- 3. sys_subject 表新增 grade_level 字段（学科所属学段）
-- 
-- grade_level 取值：
--   0 = 通用（三个学段都有）
--   1 = 小学
--   2 = 初中
--   3 = 高中
-- ============================================

-- 1. 用户表新增学段字段
ALTER TABLE sys_user ADD COLUMN grade_level INT DEFAULT 0 COMMENT '学段: 0=通用, 1=小学, 2=初中, 3=高中';

-- 2. 班级表新增学段字段
ALTER TABLE sys_class ADD COLUMN grade_level INT DEFAULT 0 COMMENT '学段: 0=通用, 1=小学, 2=初中, 3=高中';

-- 3. 学科表新增学段字段
ALTER TABLE sys_subject ADD COLUMN grade_level INT DEFAULT 0 COMMENT '学段: 0=通用, 1=小学, 2=初中, 3=高中';

-- ============================================
-- 初始化学科数据（按学段分类）
-- 注意：以下 INSERT 仅在学科表为空时执行
-- 如果已有数据，请手动 UPDATE 设置 grade_level
-- ============================================

-- 通用学科（三个学段都有）
INSERT INTO sys_subject (subject_name, subject_code, grade_level, sort, create_time) VALUES
('语文', 'chinese', 0, 1, NOW()),
('数学', 'math', 0, 2, NOW()),
('英语', 'english', 0, 3, NOW())
ON DUPLICATE KEY UPDATE grade_level = 0;

-- 小学专属学科
INSERT INTO sys_subject (subject_name, subject_code, grade_level, sort, create_time) VALUES
('道德与法治', 'morality', 1, 10, NOW()),
('科学', 'science', 1, 11, NOW()),
('音乐', 'music', 1, 12, NOW()),
('美术', 'art', 1, 13, NOW()),
('体育', 'pe', 1, 14, NOW()),
('信息技术', 'it', 1, 15, NOW())
ON DUPLICATE KEY UPDATE grade_level = 1;

-- 初中学科（物理/化学/生物/历史/地理/政治 初中高中都有）
INSERT INTO sys_subject (subject_name, subject_code, grade_level, sort, create_time) VALUES
('物理', 'physics', 2, 20, NOW()),
('化学', 'chemistry', 2, 21, NOW()),
('生物', 'biology', 2, 22, NOW()),
('历史', 'history', 2, 23, NOW()),
('地理', 'geography', 2, 24, NOW()),
('政治', 'politics', 2, 25, NOW())
ON DUPLICATE KEY UPDATE grade_level = 2;

-- 高中专属学科（暂无，高中与初中学科相同）
-- 如需添加高中专属学科，取消注释并修改
-- INSERT INTO sys_subject (subject_name, subject_code, grade_level, sort, create_time) VALUES
-- ('通用技术', 'general_tech', 3, 30, NOW())
-- ON DUPLICATE KEY UPDATE grade_level = 3;

-- ============================================
-- 如果已有学科数据，用以下语句更新 grade_level
-- （取消注释后按需执行）
-- ============================================

-- UPDATE sys_subject SET grade_level = 0 WHERE subject_name IN ('语文', '数学', '英语');
-- UPDATE sys_subject SET grade_level = 1 WHERE subject_name IN ('道德与法治', '科学', '音乐', '美术', '体育', '信息技术');
-- UPDATE sys_subject SET grade_level = 2 WHERE subject_name IN ('物理', '化学', '生物', '历史', '地理', '政治');
-- UPDATE sys_subject SET grade_level = 3 WHERE subject_name IN ('通用技术');

-- ============================================
-- 更新已有用户的学段（根据班级推断）
-- 如果班级名称包含年级信息，可自动推断
-- （取消注释后按需执行）
-- ============================================

-- 示例：根据班级名称中的年级推断学段
-- UPDATE sys_user u JOIN sys_class c ON u.class_id = c.id
-- SET u.grade_level = CASE
--   WHEN c.grade IN ('一年级','二年级','三年级','四年级','五年级','六年级') THEN 1
--   WHEN c.grade IN ('七年级','八年级','九年级','初一','初二','初三') THEN 2
--   WHEN c.grade IN ('高一','高二','高三','十年级','十一年级','十二年级') THEN 3
--   ELSE 0
-- END
-- WHERE u.grade_level = 0;

-- ============================================
-- 补充各学段示例班级数据
-- 注意：如果已有班级数据，先更新 grade_level
-- ============================================

-- 更新已有班级的学段（根据年级名称推断）
UPDATE sys_class SET grade_level = 3 WHERE grade IN ('高一', '高二', '高三');
UPDATE sys_class SET grade_level = 2 WHERE grade IN ('初一', '初二', '初三', '七年级', '八年级', '九年级');
UPDATE sys_class SET grade_level = 1 WHERE grade IN ('一年级', '二年级', '三年级', '四年级', '五年级', '六年级');

-- 如果班级数据为空，插入示例班级
INSERT INTO sys_class (class_name, grade, head_teacher_id, grade_level, status, create_time) VALUES
-- 小学班级
('一年级(1)班', '一年级', NULL, 1, 1, NOW()),
('一年级(2)班', '一年级', NULL, 1, 1, NOW()),
('二年级(1)班', '二年级', NULL, 1, 1, NOW()),
('三年级(1)班', '三年级', NULL, 1, 1, NOW()),
('四年级(1)班', '四年级', NULL, 1, 1, NOW()),
('五年级(1)班', '五年级', NULL, 1, 1, NOW()),
('六年级(1)班', '六年级', NULL, 1, 1, NOW()),
-- 初中班级
('七年级(1)班', '七年级', NULL, 2, 1, NOW()),
('七年级(2)班', '七年级', NULL, 2, 1, NOW()),
('八年级(1)班', '八年级', NULL, 2, 1, NOW()),
('九年级(1)班', '九年级', NULL, 2, 1, NOW()),
('九年级(2)班', '九年级', NULL, 2, 1, NOW()),
-- 高中班级
('高一(1)班', '高一', NULL, 3, 1, NOW()),
('高一(2)班', '高一', NULL, 3, 1, NOW()),
('高一(3)班', '高一', NULL, 3, 1, NOW()),
('高二(1)班', '高二', NULL, 3, 1, NOW()),
('高二(2)班', '高二', NULL, 3, 1, NOW()),
('高三(1)班', '高三', NULL, 3, 1, NOW()),
('高三(2)班', '高三', NULL, 3, 1, NOW())
ON DUPLICATE KEY UPDATE grade_level = VALUES(grade_level);
