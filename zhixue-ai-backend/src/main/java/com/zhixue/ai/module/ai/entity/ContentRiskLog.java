package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 内容风控记录表(教师/学生内容自动检测违规记录)
 */
@Data
@TableName("content_risk_log")
public class ContentRiskLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String username;
    private String roleCode;
    private String source;
    private String content;
    private String riskReason;
    private String ip;
    private LocalDateTime createTime;
}
