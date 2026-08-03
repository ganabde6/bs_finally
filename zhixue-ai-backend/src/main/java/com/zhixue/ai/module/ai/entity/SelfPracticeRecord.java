package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 自主练习记录表
 */
@Data
@TableName("self_practice_record")
public class SelfPracticeRecord {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 练习题目快照(JSON数组) */
    private String questionSnapshot;

    /** 总题量 */
    private Integer totalCount;

    /** 正确数 */
    private Integer correctCount;

    /** 正确率 */
    private BigDecimal accuracy;

    /** 练习耗时(秒) */
    private Integer durationSeconds;

    /** 生成来源(错题生成/系统推荐) */
    private String generateSource;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
