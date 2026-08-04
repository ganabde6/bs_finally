-- 根据年级名称自动推断学段：小学 -> 1
UPDATE sys_class SET grade_level = 1 WHERE grade IN ('一年级', '二年级', '三年级', '四年级', '五年级', '六年级');
-- 初中 -> 2
UPDATE sys_class SET grade_level = 2 WHERE grade IN ('七年级', '八年级', '九年级', '初一', '初二', '初三');
-- 高中 -> 3
UPDATE sys_class SET grade_level = 3 WHERE grade IN ('高一', '高二', '高三');
