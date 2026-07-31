package com.zhixue.ai.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_subject")
public class SysSubject {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String subjectName;
    private String subjectCode;
    private Integer sort;
    private LocalDateTime createTime;
}
