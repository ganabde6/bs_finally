package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("exam_risk_log")
public class ExamRiskLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long answerId;
    private Long studentId;
    private Integer riskType;
    private Integer riskLevel;
    private String description;
    private LocalDateTime createTime;
}
