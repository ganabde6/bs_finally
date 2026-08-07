package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 高考英语听说练习作答记录表
 */
@Data
@TableName("ai_listening_speaking_record")
public class AiListeningSpeakingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 学生ID */
    private Long userId;

    /** 题目ID */
    private Long questionId;

    /** 作答音频路径(/upload/audio/xxx) */
    private String audioPath;

    /** 文字补充说明(可选) */
    private String supplementText;

    /** AI 语音识别文本 */
    private String recognizedText;

    /** 发音评分(满分25) */
    private BigDecimal pronunciationScore;

    /** 流利度评分(满分25) */
    private BigDecimal fluencyScore;

    /** 语法评分(满分25) */
    private BigDecimal grammarScore;

    /** 内容评分(满分25) */
    private BigDecimal contentScore;

    /** 总分(满分100) */
    private BigDecimal totalScore;

    /** AI 评语与改进建议 */
    private String aiFeedback;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
