package com.zhixue.ai.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

/** 学生任务完成记录(教师派发班级任务时,每个学生一条) */
@Data
@TableName("sys_task_record")
public class SysTaskRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long taskId;
    private Long studentId;
    /** 0=待完成 1=已完成 */
    private Integer status;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
}
