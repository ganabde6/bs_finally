package com.zhixue.ai.module.exam.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 试卷/作业表
 */
@Data
@TableName("exam_paper")
public class ExamPaper {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String paperName;
    private Integer paperType;
    private Long subjectId;
    private Long classId;
    private Long creatorId;
    private BigDecimal totalScore;
    private Integer duration;
    private LocalDateTime publishTime;
    private LocalDateTime deadline;
    private Integer status;
    private String description;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    private Integer deleted;
}
