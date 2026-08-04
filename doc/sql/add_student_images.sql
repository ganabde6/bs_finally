-- 为 ai_variant_question 表添加 student_images 字段
ALTER TABLE ai_variant_question ADD COLUMN student_images TEXT COMMENT '学生上传的图片答案(base64 JSON数组)';
