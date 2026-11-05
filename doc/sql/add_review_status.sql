-- 批量复核(AI 自检)功能:为批改记录增加复核状态
-- 0 = 正常(AI 初批与复核结论一致,或教师已确认)
-- 1 = 存疑(AI 复核结论与初批不一致,待教师人工确认)
ALTER TABLE ai_correct_record
    ADD COLUMN review_status TINYINT NOT NULL DEFAULT 0 COMMENT '复核状态:0正常 1存疑待教师确认' AFTER correct_type;
