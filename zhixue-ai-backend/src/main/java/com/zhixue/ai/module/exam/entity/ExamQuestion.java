package com.zhixue.ai.module.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 题库表
 */
@Data
@TableName("exam_question")
public class ExamQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long subjectId;
    private Integer questionType;
    private Integer difficulty;
    private String knowledgePoint;
    private String content;
    private String options;
    private String standardAnswer;
    private String scorePoint;
    private String analysis;
    private BigDecimal fullScore;
    private Long creatorId;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
