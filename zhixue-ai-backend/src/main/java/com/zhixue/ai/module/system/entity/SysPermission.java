package com.zhixue.ai.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_permission")
public class SysPermission {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long parentId;
    private String permCode;
    private String permName;
    private Integer permType;
    private String path;
    private String component;
    private String icon;
    private Integer sort;
    private LocalDateTime createTime;
}
