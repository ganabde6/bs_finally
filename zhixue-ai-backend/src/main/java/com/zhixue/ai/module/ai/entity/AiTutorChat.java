package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_tutor_chat")
public class AiTutorChat {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private String role;
    private String content;
    private Integer chatType;
    private LocalDateTime createTime;
}
