package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 学生听说作业作答记录表
 */
@Data
@TableName("ai_ls_homework_record")
public class AiLsHomeworkRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作业ID */
    private Long homeworkId;

    /** 作业题目ID */
    private Long questionId;

    /** 学生ID */
    private Long studentId;

    /** 作答音频路径 */
    private String audioPath;

    /** 文字补充 */
    private String supplementText;

    /** AI 识别文本 */
    private String recognizedText;

    /** 发音分(0-25) */
    private BigDecimal pronunciationScore;

    /** 流利度分(0-25) */
    private BigDecimal fluencyScore;

    /** 语法分(0-25) */
    private BigDecimal grammarScore;

    /** 内容分(0-25) */
    private BigDecimal contentScore;

    /** 总分(0-100) */
    private BigDecimal totalScore;

    /** AI 评语 */
    private String aiFeedback;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
