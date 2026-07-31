-- ============================================================
-- 智学AI学习测评系统 种子数据
-- 所有演示账号密码统一为:123456 (BCrypt加密)
-- ============================================================
USE zhixue_ai;

-- ---------------------------- 角色 ----------------------------
INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'SUPER_ADMIN',  '超级管理员', '系统最高权限'),
(2, 'SCHOOL_ADMIN', '校级管理员', '校级运维管理'),
(3, 'TEACHER',      '教师',       '教学与学情管理'),
(4, 'STUDENT',      '学生',       '学习与作答');

-- ---------------------------- 权限(菜单级) ----------------------------
-- 学生端
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, path, component, icon, sort) VALUES
(101, 0,  'student:dashboard',  '学习首页',   1, '/student/dashboard',  'student/Dashboard',  'Home',      1),
(102, 0,  'student:paper',      '作业/考试',  1, '/student/paper',      'student/PaperList',  'Document',  2),
(103, 0,  'student:tutor',      'AI助学',     1, '/student/tutor',      'student/Tutor',      'ChatDotRound',3),
(104, 0,  'student:errorbook',  '错题本',     1, '/student/errorbook',  'student/ErrorBook',  'EditPen',   4),
(105, 0,  'student:study',      '学情中心',   1, '/student/study',      'student/StudyCenter','TrendCharts',5);
-- 教师端
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, path, component, icon, sort) VALUES
(201, 0, 'teacher:dashboard',  '教师首页',    1, '/teacher/dashboard',  'teacher/Dashboard',  'Home',      1),
(202, 0, 'teacher:question',   '题库管理',    1, '/teacher/question',   'teacher/Question',   'Files',     2),
(203, 0, 'teacher:paper',      '作业考试管理',1, '/teacher/paper',      'teacher/Paper',      'Document',  3),
(204, 0, 'teacher:correct',    '批改管理',    1, '/teacher/correct',    'teacher/Correct',    'Edit',      4),
(205, 0, 'teacher:classAnalysis','班级学情',  1, '/teacher/classAnalysis','teacher/ClassAnalysis','TrendCharts',5),
(206, 0, 'teacher:feedback',   '家校反馈',    1, '/teacher/feedback',   'teacher/Feedback',   'Message',   6);
-- 管理端
INSERT INTO sys_permission (id, parent_id, perm_code, perm_name, perm_type, path, component, icon, sort) VALUES
(301, 0, 'admin:dashboard',   '数据大屏',     1, '/admin/dashboard',   'admin/Dashboard',    'DataLine',  1),
(302, 0, 'admin:user',        '用户管理',     1, '/admin/user',        'admin/User',         'User',      2),
(303, 0, 'admin:role',        '角色权限',     1, '/admin/role',        'admin/Role',         'Lock',      3),
(304, 0, 'admin:class',       '班级管理',     1, '/admin/class',       'admin/Class',        'OfficeBuilding',4),
(305, 0, 'admin:course',      '课程管理',     1, '/admin/course',      'admin/Course',       'Reading',   5),
(306, 0, 'admin:aiConfig',    'AI配置',       1, '/admin/aiConfig',    'admin/AiConfig',     'MagicStick',6),
(307, 0, 'admin:notice',      '公告管理',     1, '/admin/notice',      'admin/Notice',       'Bell',      7),
(308, 0, 'admin:moderation',  '内容风控',     1, '/admin/moderation',  'admin/Moderation',   'WarnTriangleFilled',8),
(309, 0, 'admin:log',         '系统日志',     1, '/admin/log',         'admin/Log',          'List',      9);

-- ---------------------------- 角色-权限关联 ----------------------------
INSERT INTO sys_role_permission (role_id, permission_id) VALUES
(1, 301),(1, 302),(1, 303),(1, 304),(1, 305),(1, 306),(1, 307),(1, 308),(1, 309),
(2, 301),(2, 302),(2, 304),(2, 305),(2, 307),(2, 308),(2, 309),
(3, 201),(3, 202),(3, 203),(3, 204),(3, 205),(3, 206),
(4, 101),(4, 102),(4, 103),(4, 104),(4, 105);

-- ---------------------------- 学科 ----------------------------
INSERT INTO sys_subject (id, subject_name, subject_code, sort) VALUES
(1, '语文', 'chinese', 1),
(2, '数学', 'math',    2),
(3, '英语', 'english', 3),
(4, '物理', 'physics', 4),
(5, '化学', 'chemistry',5);

-- ---------------------------- 班级 ----------------------------
INSERT INTO sys_class (id, class_name, grade, head_teacher_id, status) VALUES
(1, '高三(1)班', '高三', 3, 1),
(2, '高三(2)班', '高三', 4, 1);

-- ---------------------------- 用户(密码均为123456) ----------------------------
-- BCrypt加密的123456
INSERT INTO sys_user (id, username, password, real_name, role_id, class_id, status, avatar) VALUES
(1, 'admin',    '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '系统管理员', 1, NULL, 1, NULL),
(2, 'schooladmin','$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS','校级管理员', 2, NULL, 1, NULL),
(3, 'teacher01', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '张老师',     3, NULL, 1, NULL),
(4, 'teacher02', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '李老师',     3, NULL, 1, NULL);

-- 学生:每班10名,共20名,ID从11开始
INSERT INTO sys_user (id, username, password, real_name, role_id, class_id, status) VALUES
(11, 'student01', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生01', 4, 1, 1),
(12, 'student02', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生02', 4, 1, 1),
(13, 'student03', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生03', 4, 1, 1),
(14, 'student04', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生04', 4, 1, 1),
(15, 'student05', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生05', 4, 1, 1),
(16, 'student06', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生06', 4, 1, 1),
(17, 'student07', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生07', 4, 1, 1),
(18, 'student08', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生08', 4, 1, 1),
(19, 'student09', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生09', 4, 1, 1),
(20, 'student10', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生10', 4, 1, 1),
(21, 'student11', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生11', 4, 2, 1),
(22, 'student12', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生12', 4, 2, 1),
(23, 'student13', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生13', 4, 2, 1),
(24, 'student14', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生14', 4, 2, 1),
(25, 'student15', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生15', 4, 2, 1),
(26, 'student16', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生16', 4, 2, 1),
(27, 'student17', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生17', 4, 2, 1),
(28, 'student18', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生18', 4, 2, 1),
(29, 'student19', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生19', 4, 2, 1),
(30, 'student20', '$2a$10$GkNy7UyBquJhWj6953GnrOBtzsSLA3YVgMT4Cdd1EQ1fQPw6xYUsS', '学生20', 4, 2, 1);

-- ---------------------------- 教师-班级-学科关系 ----------------------------
-- 张老师:1班语文、英语;李老师:1班数学、2班数学、物理、化学
INSERT INTO sys_teacher_class (teacher_id, class_id, subject_id) VALUES
(3, 1, 1), (3, 1, 3), (3, 2, 1), (3, 2, 3),
(4, 1, 2), (4, 2, 2), (4, 2, 4), (4, 2, 5);

-- ---------------------------- 题库(每科5题,共25题) ----------------------------
-- 语文(学科ID=1)
INSERT INTO exam_question (id, subject_id, question_type, difficulty, knowledge_point, content, options, standard_answer, score_point, analysis, full_score, creator_id) VALUES
(1, 1, 1, 2, '字音字形', '下列加点字注音全对的一项是:', '[{"key":"A","value":"憧憬(chōng)"},{"key":"B","value":"倔强(juè)"},{"key":"C","value":"炽热(chì)"},{"key":"D","value":"惩罚(chěng)"}]', 'A', NULL, 'A项正确;B项应为jué;C项正确;D项应为chéng。本题考查常见字音辨析。', 5.00, 3),
(2, 1, 5, 3, '古诗文鉴赏', '简述《静夜思》表达的情感。', NULL, '表达诗人客居思乡之情,通过月光引发对故乡的思念。', '[{"point":"思乡之情","score":3},{"point":"月光触发","score":2}]', '考查古诗情感把握,需结合意象「月光」分析。', 5.00, 3),
(3, 1, 6, 4, '作文', '请以「成长」为题,写一篇不少于600字的记叙文。', NULL, '要求:主题明确、结构完整、语言流畅、情感真挚。', '[{"point":"主题明确","score":5},{"point":"结构完整","score":5},{"point":"语言流畅","score":5},{"point":"情感真挚","score":5}]', '考查记叙文写作能力。', 20.00, 3),
(4, 1, 4, 2, '成语', '"画蛇添足"比喻____。', NULL, '做多余的事,反而弄巧成拙', NULL, '考查成语含义。', 3.00, 3),
(5, 1, 3, 1, '文学常识', '《红楼梦》作者是曹雪芹。', NULL, '正确', NULL, '考查文学常识。', 2.00, 3);
-- 数学(学科ID=2)
INSERT INTO exam_question (id, subject_id, question_type, difficulty, knowledge_point, content, options, standard_answer, score_point, analysis, full_score, creator_id) VALUES
(6, 2, 1, 2, '函数', '函数 f(x)=x²+1 在 x=2 时的值为:', '[{"key":"A","value":"3"},{"key":"B","value":"4"},{"key":"C","value":"5"},{"key":"D","value":"6"}]', 'C', NULL, 'f(2)=2²+1=5。', 5.00, 4),
(7, 2, 7, 3, '一元二次方程', '解方程 x²-5x+6=0。', NULL, 'x=2 或 x=3', '[{"step":"因式分解(x-2)(x-3)=0","score":3},{"step":"得出x=2或x=3","score":2}]', '(x-2)(x-3)=0,故x=2或x=3。', 5.00, 4),
(8, 2, 7, 4, '导数', '求 y=x³ 在 x=1 处的导数。', NULL, '3', '[{"step":"求导 y''=3x²","score":3},{"step":"代入x=1得3","score":2}]', 'y''=3x²,代入x=1得y''=3。', 5.00, 4),
(9, 2, 4, 2, '三角函数', 'sin30°=____。', NULL, '1/2', NULL, 'sin30°=1/2。', 3.00, 4),
(10, 2, 2, 3, '不等式', '下列不等式中正确的是:', '[{"key":"A","value":"若a>b,则a²>b²"},{"key":"B","value":"若a>b>0,则1/a<1/b"},{"key":"C","value":"若a>b,则|a|>|b|"},{"key":"D","value":"若a>b,则-a>-b"}]', 'BD', NULL, 'B项正确(倒数反向);D项正确(取反变号)。', 5.00, 4);
-- 英语(学科ID=3)
INSERT INTO exam_question (id, subject_id, question_type, difficulty, knowledge_point, content, options, standard_answer, score_point, analysis, full_score, creator_id) VALUES
(11, 3, 1, 2, '词汇', 'Choose the correct word: She ___ to school every day.', '[{"key":"A","value":"go"},{"key":"B","value":"goes"},{"key":"C","value":"going"},{"key":"D","value":"gone"}]', 'B', NULL, '主语She为第三人称单数,动词加s。', 5.00, 3),
(12, 3, 5, 3, '翻译', 'Translate: 我喜欢读书。', NULL, 'I like reading. / I enjoy reading.', '[{"point":"主语I","score":2},{"point":"谓语like/enjoy","score":2},{"point":"宾语reading","score":1}]', '考查基础翻译能力。', 5.00, 3),
(13, 3, 4, 1, '介词', 'He is good ___ math. (填介词)', NULL, 'at', NULL, 'be good at为固定搭配。', 2.00, 3),
(14, 3, 6, 4, '写作', 'Write a short paragraph (about 80 words) about your weekend plan.', NULL, '要求:语法正确、内容连贯、字数达标。', '[{"point":"语法正确","score":5},{"point":"内容连贯","score":5},{"point":"字数达标","score":5}]', '考查英语短文写作。', 15.00, 3),
(15, 3, 3, 1, '语法', 'English is spoken all over the world. (被动语态)', NULL, '正确', NULL, '考查被动语态识别。', 2.00, 3);
-- 物理(学科ID=4)
INSERT INTO exam_question (id, subject_id, question_type, difficulty, knowledge_point, content, options, standard_answer, score_point, analysis, full_score, creator_id) VALUES
(16, 4, 1, 2, '力学', '下列属于矢量的是:', '[{"key":"A","value":"质量"},{"key":"B","value":"时间"},{"key":"C","value":"速度"},{"key":"D","value":"温度"}]', 'C', NULL, '速度既有大小又有方向,是矢量。', 5.00, 4),
(17, 4, 7, 3, '运动学', '一物体做匀加速直线运动,初速度v0=2m/s,加速度a=2m/s²,求3秒后的速度。', NULL, '8m/s', '[{"step":"公式 v=v0+at","score":3},{"step":"代入 v=2+2×3=8","score":2}]', 'v=v0+at=2+2×3=8m/s。', 5.00, 4),
(18, 4, 4, 2, '单位', '力的国际单位是____。', NULL, '牛顿(N)', NULL, '力的SI单位是牛顿,符号N。', 3.00, 4),
(19, 4, 3, 1, '光學', '光在真空中的速度大于在玻璃中的速度。', NULL, '正确', NULL, '光在真空中速度最大。', 2.00, 4),
(20, 4, 5, 4, '能量守恒', '简述能量守恒定律的内容。', NULL, '能量既不会凭空产生,也不会凭空消失,只能从一种形式转化为另一种形式,总量保持不变。', '[{"point":"不生不灭","score":3},{"point":"形式转化","score":2}]', '考查能量守恒基本表述。', 5.00, 4);
-- 化学(学科ID=5)
INSERT INTO exam_question (id, subject_id, question_type, difficulty, knowledge_point, content, options, standard_answer, score_point, analysis, full_score, creator_id) VALUES
(21, 5, 1, 2, '元素', '下列属于金属元素的是:', '[{"key":"A","value":"氧"},{"key":"B","value":"钠"},{"key":"C","value":"氯"},{"key":"D","value":"氢"}]', 'B', NULL, '钠(Na)是金属元素。', 5.00, 4),
(22, 5, 7, 3, '化学方程式', '配平:H2 + O2 → H2O', NULL, '2H2 + O2 = 2H2O', '[{"step":"配平氢","score":3},{"step":"配平氧","score":2}]', '配平后2H2+O2=2H2O。', 5.00, 4),
(23, 5, 4, 2, '周期表', '水的化学式是____。', NULL, 'H2O', NULL, '水分子由2个氢原子和1个氧原子构成。', 2.00, 4),
(24, 5, 3, 1, '酸碱', 'pH=7的溶液呈中性。', NULL, '正确', NULL, 'pH=7为中性。', 2.00, 4),
(25, 5, 5, 3, '氧化还原', '简述氧化反应的概念。', NULL, '物质与氧发生反应失去电子的过程称为氧化反应。', '[{"point":"与氧反应","score":3},{"point":"失去电子","score":2}]', '考查氧化反应基本概念。', 5.00, 4);

-- ---------------------------- 试卷(2份) ----------------------------
-- 1班数学作业(教师ID=4李老师)
INSERT INTO exam_paper (id, paper_name, paper_type, subject_id, class_id, creator_id, total_score, duration, publish_time, deadline, status, description) VALUES
(1, '高三数学周作业(函数与方程)', 1, 2, 1, 4, 18.00, 60, NOW(), DATE_ADD(NOW(), INTERVAL 7 DAY), 1, '本周函数与方程练习');
-- 2班数学考试(教师ID=4李老师)
INSERT INTO exam_paper (id, paper_name, paper_type, subject_id, class_id, creator_id, total_score, duration, publish_time, deadline, status, description) VALUES
(2, '高三数学月考(一)', 2, 2, 2, 4, 23.00, 90, NOW(), DATE_ADD(NOW(), INTERVAL 3 DAY), 1, '本月阶段性测评');

-- ---------------------------- 试卷-题目关联 ----------------------------
INSERT INTO exam_paper_question (paper_id, question_id, score, sort) VALUES
(1, 6, 5.00, 1), (1, 7, 5.00, 2), (1, 9, 3.00, 3), (1, 10, 5.00, 4),
(2, 6, 5.00, 1), (2, 7, 5.00, 2), (2, 8, 5.00, 3), (2, 9, 3.00, 4), (2, 10, 5.00, 5);

-- ---------------------------- 作答示例(2名学生提交作业1) ----------------------------
INSERT INTO exam_answer (id, paper_id, student_id, submit_type, duration, total_score, status, submit_time, start_time) VALUES
(1, 1, 11, 1, 1800, NULL, 1, NOW(), DATE_SUB(NOW(), INTERVAL 30 MINUTE)),
(2, 1, 12, 1, 2100, NULL, 1, NOW(), DATE_SUB(NOW(), INTERVAL 35 MINUTE));

-- ---------------------------- AI批改示例(学生11的作业1批改) ----------------------------
INSERT INTO ai_correct_record (answer_id, question_id, student_answer, score, full_score, is_correct, error_tag, score_detail, correct_remark, correct_type, ai_model) VALUES
(1, 6, 'C', 5.00, 5.00, 1, NULL, '[{"step":"答案匹配","score":5}]', '答案正确', 1, 'LOCAL-RULE-V1'),
(1, 7, 'x=2,x=3', 5.00, 5.00, 1, NULL, '[{"step":"因式分解","score":3},{"step":"求解","score":2}]', '步骤完整,答案正确', 1, 'LOCAL-RULE-V1'),
(1, 9, '1/2', 3.00, 3.00, 1, NULL, NULL, '答案正确', 1, 'LOCAL-RULE-V1'),
(1, 10, 'BD', 5.00, 5.00, 1, NULL, NULL, '答案正确', 1, 'LOCAL-RULE-V1');

-- ---------------------------- 错题本示例 ----------------------------
INSERT INTO ai_error_book (student_id, question_id, paper_id, correct_id, error_type, knowledge_point, review_status) VALUES
(12, 7, 1, NULL, 2, '一元二次方程', 0),
(12, 10, 1, NULL, 3, '不等式', 0);

-- ---------------------------- 学情分析示例 ----------------------------
INSERT INTO ai_study_analysis (student_id, subject_id, avg_score, trend, weak_points, strong_points, suggestion) VALUES
(11, 2, 85.50, '[{"paper":"周作业","score":18}]', '["导数应用"]', '["函数基础","方程求解"]', '建议加强对导数应用题型的练习,巩固概念理解。'),
(12, 2, 72.00, '[{"paper":"周作业","score":15}]', '["一元二次方程","不等式"]', '["函数基础"]', '建议系统复习方程求解与不等式性质,多做变式训练。');

INSERT INTO ai_class_analysis (class_id, subject_id, avg_score, pass_rate, excellent_rate, common_errors, layering, teaching_advice) VALUES
(1, 2, 78.50, 90.00, 30.00, '["不等式性质","导数应用"]', '[{"layer":"优秀","count":3},{"layer":"良好","count":4},{"layer":"待提升","count":3}]', '建议针对不等式与导数模块开展专题复习,分层布置强化训练。');

-- ---------------------------- AI模型配置 ----------------------------
INSERT INTO ai_model_config (config_key, config_value, config_name, description) VALUES
('strictness',          '1.0',  '批改严苛度',     '0.5宽松 ~ 1.5严格,影响主观题扣分力度'),
('enable_tutor',         'true', 'AI答疑开关',     '是否开启学生端AI助学'),
('enable_polish',        'true', '作文润色开关',   '是否开启作文/简答题智能润色'),
('enable_variant_push',  'true', '错题推送开关',   '是否开启错题变式题推送'),
('enable_face_verify',   'false','人脸核验开关',   '是否开启考试人脸核验(需接入图像接口)'),
('enable_screen_monitor','true', '切屏监测开关',   '是否开启线上考试切屏监测'),
('enable_similarity_check','true','答案查重开关',  '是否开启答案雷同查重'),
('ocr_provider',         'local','OCR服务商',      'local/百度/腾讯/阿里,默认local模拟'),
('ai_provider',          'local','AI对话服务商',   'local/通义/文心/讯飞/OpenAI,默认local规则版');

-- ---------------------------- 公告 ----------------------------
INSERT INTO sys_notice (title, content, target_role, publisher_id, publish_time, status) VALUES
('欢迎使用智学AI学习测评系统', '欢迎使用本系统,请各位老师按时布置作业,学生按时完成作答。如有问题请联系管理员。', NULL, 1, NOW(), 1),
('期末考试安排通知', '本学期期末考试将于下月进行,请各位同学做好准备。', 'STUDENT', 1, NOW(), 1);

-- ---------------------------- 系统日志示例 ----------------------------
INSERT INTO sys_log (user_id, username, module, operation, method, ip, cost_ms) VALUES
(1, 'admin', '系统', '系统初始化', 'INIT', '127.0.0.1', 0);
