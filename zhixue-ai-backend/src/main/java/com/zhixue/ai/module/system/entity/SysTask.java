package com.zhixue.ai.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_task")
public class SysTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String title;
    private String content;
    /** 1=管理员派发的教师任务 2=教师派发的学生任务 */
    private Integer taskType;
    /** 目标教师ID(教师任务,空=全体教师) */
    private Long targetTeacherId;
    /** 目标班级ID(学生任务) */
    private Long targetClassId;
    private LocalDateTime deadline;
    /** 0=待完成 1=已完成 */
    private Integer status;
    private LocalDateTime completeTime;
    private Long publisherId;
    private LocalDateTime createTime;
}
