package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ai_class_analysis")
public class AiClassAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long classId;
    private Long subjectId;
    private BigDecimal avgScore;
    private BigDecimal passRate;
    private BigDecimal excellentRate;
    private String commonErrors;
    private String layering;
    private String teachingAdvice;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
