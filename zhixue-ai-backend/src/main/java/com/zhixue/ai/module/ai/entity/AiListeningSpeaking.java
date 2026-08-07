package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 高考英语听说练习题目表
 */
@Data
@TableName("ai_listening_speaking")
public class AiListeningSpeaking {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 题目标题 */
    private String title;

    /** 题目内容(文字说明/朗读文本) */
    private String content;

    /** 参考文本(标准朗读稿/参考答案) */
    private String referenceText;

    /** 参考音频URL(/upload/xxx) */
    private String referenceAudio;

    /** 学段: 0=通用, 1=小学, 2=初中, 3=高中 */
    private Integer gradeLevel;

    /** 难度: 1=简单, 2=中等, 3=困难 */
    private Integer difficulty;

    /** 题型(模仿朗读/角色扮演/故事复述) */
    private String questionType;

    /** 话题标签(如:旅行、动物、健康生活) */
    private String topic;

    /** 题目来源: PRESET=预设, AI_TEXT=自定义文本, AI_IMAGE=图片出题, AI_TOPIC=话题出题, AI_SIMILAR=同类生成 */
    private String sourceType;

    /** 所属学生ID(自主出题时记录) */
    private Long studentId;

    /** 评分要点(教师可编辑) */
    private String scorePoints;

    /** 出题图片URL(图片出题时) */
    private String imageUrl;

    /** 状态: 0=下架, 1=上架 */
    private Integer status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
