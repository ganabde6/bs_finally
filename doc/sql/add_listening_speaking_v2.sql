-- =====================================================
-- 英语听说模块 V2 升级脚本
-- 新增: 学生自主出题(话题/文本/图片)、教师听说作业、同类练习生成
-- =====================================================

USE zhixue_ai;

-- 1. ai_listening_speaking 表新增字段
ALTER TABLE ai_listening_speaking
  ADD COLUMN topic VARCHAR(100) DEFAULT NULL COMMENT '话题标签(如:旅行、动物、健康生活)',
  ADD COLUMN source_type VARCHAR(20) DEFAULT 'PRESET' COMMENT '题目来源: PRESET=预设, AI_TEXT=自定义文本出题, AI_IMAGE=图片出题, AI_TOPIC=话题出题, AI_SIMILAR=同类生成',
  ADD COLUMN student_id BIGINT DEFAULT NULL COMMENT '所属学生ID(自主出题时记录)',
  ADD COLUMN grading_points TEXT DEFAULT NULL COMMENT 'AI 评分要点(JSON)',
  ADD COLUMN image_url VARCHAR(255) DEFAULT NULL COMMENT '出题图片URL(图片出题时)',
  ADD COLUMN score_points TEXT DEFAULT NULL COMMENT '评分要点(教师可编辑)';

-- 2. 教师听说作业表
CREATE TABLE IF NOT EXISTS ai_ls_homework (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  teacher_id BIGINT NOT NULL COMMENT '教师ID',
  title VARCHAR(200) NOT NULL COMMENT '作业名称',
  class_id BIGINT NOT NULL COMMENT '班级ID',
  grade_level INT DEFAULT 0 COMMENT '学段',
  group_mode VARCHAR(20) DEFAULT 'STANDARD' COMMENT '组题模式: STANDARD=考试标准, TOPIC=话题难度, CLASS_ANALYSIS=班级学情, CUSTOM=自定义素材',
  group_params TEXT DEFAULT NULL COMMENT '组题参数(JSON)',
  deadline DATETIME DEFAULT NULL COMMENT '截止时间',
  status INT DEFAULT 0 COMMENT '0=草稿, 1=已发布, 2=已结束',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师英语听说作业表';

-- 3. 教师听说作业题目表
CREATE TABLE IF NOT EXISTS ai_ls_homework_question (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  homework_id BIGINT NOT NULL COMMENT '作业ID',
  title VARCHAR(200) DEFAULT NULL COMMENT '题目标题',
  content TEXT NOT NULL COMMENT '题目内容',
  reference_text TEXT DEFAULT NULL COMMENT '参考文本',
  reference_audio VARCHAR(255) DEFAULT NULL COMMENT '参考音频URL',
  question_type VARCHAR(20) DEFAULT '模仿朗读' COMMENT '题型',
  difficulty INT DEFAULT 2 COMMENT '难度',
  score_points TEXT DEFAULT NULL COMMENT '评分要点(教师可编辑)',
  sort_order INT DEFAULT 0 COMMENT '排序',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='教师听说作业题目表';

-- 4. 学生听说作业作答记录表(教师作业场景)
CREATE TABLE IF NOT EXISTS ai_ls_homework_record (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  homework_id BIGINT NOT NULL COMMENT '作业ID',
  question_id BIGINT NOT NULL COMMENT '作业题目ID',
  student_id BIGINT NOT NULL COMMENT '学生ID',
  audio_path VARCHAR(255) DEFAULT NULL COMMENT '作答音频路径',
  supplement_text TEXT DEFAULT NULL COMMENT '文字补充',
  recognized_text TEXT DEFAULT NULL COMMENT 'AI 识别文本',
  pronunciation_score DECIMAL(5,1) DEFAULT 0 COMMENT '发音分(0-25)',
  fluency_score DECIMAL(5,1) DEFAULT 0 COMMENT '流利度分(0-25)',
  grammar_score DECIMAL(5,1) DEFAULT 0 COMMENT '语法分(0-25)',
  content_score DECIMAL(5,1) DEFAULT 0 COMMENT '内容分(0-25)',
  total_score DECIMAL(5,1) DEFAULT 0 COMMENT '总分(0-100)',
  ai_feedback TEXT DEFAULT NULL COMMENT 'AI 评语',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生听说作业作答记录表';

-- 5. 插入示例话题数据(可选)
-- 初中话题
INSERT INTO ai_listening_speaking (title, content, reference_text, question_type, difficulty, grade_level, topic, source_type, status) VALUES
('故事复述: The Lost Dog', 'Listen to the story about a lost dog and retell it in your own words.', 'One day, a little boy found a lost dog in the park. The dog looked hungry and scared. The boy decided to help the dog find its home. He asked people nearby if they knew the dog. Finally, an old lady came and said it was her dog. She was very happy and thanked the boy.', '故事复述', 1, 2, '动物', 'PRESET', 1),
('模仿朗读: A Healthy Lifestyle', 'Read the following passage aloud with proper pronunciation and intonation.', 'Maintaining a healthy lifestyle is important for everyone. We should eat balanced meals, exercise regularly, and get enough sleep. A healthy body leads to a healthy mind, which helps us study better and enjoy life more.', '模仿朗读', 1, 2, '健康生活', 'PRESET', 1),
('模仿朗读: The Importance of Reading', 'Read the following passage aloud.', 'Reading is one of the most valuable habits a person can develop. It opens doors to new worlds, expands our knowledge, and improves our thinking skills. Through reading, we can learn from the experiences of others and grow as individuals.', '模仿朗读', 2, 2, '学习', 'PRESET', 1),
('角色扮演: 询问旅行计划', 'Role-play: You are planning a trip with your friend. Discuss the destination, transportation, and activities.', 'A: Hi! Have you thought about where we should go for our summer vacation?\nB: I was thinking about visiting Beijing. There are so many famous places to see.\nA: That sounds great! How should we get there?\nB: We could take the high-speed train. It is fast and comfortable.\nA: Good idea! What places should we visit?\nB: We should definitely see the Great Wall and the Forbidden City.', '角色扮演', 2, 2, '旅行', 'PRESET', 1),
('角色扮演: 图书馆借书', 'Role-play: You want to borrow books from the school library. Talk to the librarian.', 'A: Excuse me, I would like to borrow some books.\nB: Sure. Do you have your library card?\nA: Yes, here it is.\nB: Thank you. How many books would you like to borrow?\nA: I would like to borrow three books.\nB: No problem. You can keep them for two weeks.\nA: Thank you very much!', '角色扮演', 3, 2, '校园生活', 'PRESET', 1);

-- 高中话题
INSERT INTO ai_listening_speaking (title, content, reference_text, question_type, difficulty, grade_level, topic, source_type, status) VALUES
('故事复述: Environmental Protection', 'Listen to the passage about environmental protection and retell the key points.', 'Our planet is facing serious environmental challenges. Climate change, pollution, and deforestation are threatening the balance of nature. However, every individual can make a difference. We can reduce waste by recycling, save energy by using public transportation, and protect wildlife by supporting conservation programs. Small actions, when multiplied by millions of people, can transform the world.', '故事复述', 2, 3, '环境保护', 'PRESET', 1),
('模仿朗读: Technology and Education', 'Read the following passage aloud with natural rhythm and expression.', 'Technology has revolutionized the way we learn and teach. Online platforms provide access to knowledge from anywhere in the world. Artificial intelligence can personalize learning experiences for each student. However, technology should complement rather than replace traditional teaching methods. The human connection between teachers and students remains irreplaceable.', '模仿朗读', 2, 3, '科技', 'PRESET', 1),
('角色扮演: Job Interview', 'Role-play: You are applying for a part-time job. Answer the interviewers questions.', 'A: Good morning. Please have a seat. Tell me about yourself.\nB: Good morning. I am a high school student looking for a part-time job to gain work experience.\nA: What skills do you have that would be useful for this position?\nB: I am good at communication and I am a fast learner. I also have experience working in teams.\nA: Why do you want to work here?\nB: I admire your companys commitment to quality service and I believe I can contribute positively.', '角色扮演', 3, 3, '职业规划', 'PRESET', 1);
