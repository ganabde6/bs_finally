package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户打卡表
 */
@Data
@TableName("user_check_in")
public class UserCheckIn {
    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /** 打卡日期(年月日) */
    private LocalDate checkInDate;

    /** 连续打卡天数 */
    private Integer continuousDays;

    /** 累计总积分 */
    private Integer totalPoints;

    /** 获得勋章(铜牌/银牌/金牌) */
    private String rewardBadge;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
