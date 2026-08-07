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
 * 英语听说 PK 作答记录
 */
@Data
@TableName("pk_ls_record")
public class PkLsRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long roomId;
    private Long studentId;
    private String audioPath;
    private String recognizedText;
    private BigDecimal pronunciationScore;
    private BigDecimal fluencyScore;
    private BigDecimal grammarScore;
    private BigDecimal contentScore;
    private BigDecimal totalScore;
    private String aiFeedback;
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
