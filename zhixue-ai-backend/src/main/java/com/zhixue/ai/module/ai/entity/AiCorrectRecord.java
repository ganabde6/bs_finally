package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * AI批改记录表(每题级)
 */
@Data
@TableName("ai_correct_record")
public class AiCorrectRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long answerId;
    private Long questionId;
    private String studentAnswer;
    private BigDecimal score;
    private BigDecimal fullScore;
    private Integer isCorrect;
    private String errorTag;
    private String scoreDetail;
    private String correctRemark;
    private Integer correctType;
    private String aiModel;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
