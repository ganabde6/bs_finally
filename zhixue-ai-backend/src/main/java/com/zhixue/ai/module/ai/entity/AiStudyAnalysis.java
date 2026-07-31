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
@TableName("ai_study_analysis")
public class AiStudyAnalysis {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long subjectId;
    private BigDecimal avgScore;
    private String trend;
    private String weakPoints;
    private String strongPoints;
    private String suggestion;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
