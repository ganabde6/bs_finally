package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 教师听说作业题目表
 */
@Data
@TableName("ai_ls_homework_question")
public class AiLsHomeworkQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 作业ID */
    private Long homeworkId;

    /** 题目标题 */
    private String title;

    /** 题目内容 */
    private String content;

    /** 参考文本 */
    private String referenceText;

    /** 参考音频URL */
    private String referenceAudio;

    /** 题型 */
    private String questionType;

    /** 难度: 1=简单, 2=中等, 3=困难 */
    private Integer difficulty;

    /** 评分要点(教师可编辑) */
    private String scorePoints;

    /** 排序 */
    private Integer sortOrder;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
