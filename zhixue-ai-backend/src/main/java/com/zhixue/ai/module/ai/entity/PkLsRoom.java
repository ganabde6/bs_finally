package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 英语听说 PK 房间
 */
@Data
@TableName("pk_ls_room")
public class PkLsRoom {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String roomCode;
    private Long creatorId;
    private Long challengerId;
    private Long questionId;
    private String questionTitle;
    private String questionContent;
    private String referenceText;
    private String questionType;
    private String scorePoints;
    private Integer status; // 0=等待挑战, 1=已接受, 2=已完成
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
