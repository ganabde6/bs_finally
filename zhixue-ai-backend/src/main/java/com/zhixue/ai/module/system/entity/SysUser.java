package com.zhixue.ai.module.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 用户表
 */
@Data
@TableName("sys_user")
public class SysUser {

    @TableId(type = IdType.AUTO)
    private Long id;
    private String username;
    private String password;
    private String realName;
    private Long roleId;
    private Long classId;
    private Integer status;
    private String avatar;
    private String phone;
    private String email;
    private Integer gradeLevel; // 学段: 0=通用, 1=小学, 2=初中, 3=高中
    private LocalDateTime lastLogin;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic
    @TableField(value = "deleted")
    private Integer deleted;
}
