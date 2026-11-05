package com.zhixue.ai.module.exam.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 教师AI出题记录:每次AI组卷/出题生成一批题目,记录批次信息,
 * 同一次出题的题目通过 question_ids 关联,便于教师回看出题历史。
 */
@Data
@TableName("ai_generate_record")
public class AiGenerateRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    /** 出题教师ID */
    private Long teacherId;

    /** 学科ID */
    private Long subjectId;

    /** 来源: ai=AI现场出题, bank=题库抽取 */
    private String source;

    /** 本次生成的题目ID列表(JSON数组字符串) */
    private String questionIds;

    /** 题目数量 */
    private Integer questionCount;

    /** 出题配置快照(JSON字符串) */
    private String config;

    /** 出题时间 */
    private LocalDateTime createTime;
}
