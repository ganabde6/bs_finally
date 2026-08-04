package com.zhixue.ai.module.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_variant_question")
public class AiVariantQuestion {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long sourceQuestionId;
    private Long studentId;
    private String content;
    private String options;
    private String standardAnswer;
    private String knowledgePoint;
    private Integer isSolved;
    private String studentAnswer;
    private String studentImages; // 学生上传的图片答案(base64 JSON数组)
    private Integer isCorrect;
    private LocalDateTime createTime;
}
