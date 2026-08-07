package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 教师英语听说作业表
 */
@Data
@TableName("ai_ls_homework")
public class AiLsHomework {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 教师ID */
    private Long teacherId;

    /** 作业名称 */
    private String title;

    /** 班级ID */
    private Long classId;

    /** 学段 */
    private Integer gradeLevel;

    /** 组题模式: STANDARD=考试标准, TOPIC=话题难度, CLASS_ANALYSIS=班级学情, CUSTOM=自定义素材 */
    private String groupMode;

    /** 组题参数(JSON) */
    private String groupParams;

    /** 截止时间 */
    private LocalDateTime deadline;

    /** 0=草稿, 1=已发布, 2=已结束 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
