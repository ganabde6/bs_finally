package com.zhixue.ai.module.ai.engine;

import com.zhixue.ai.module.ai.entity.AiVariantQuestion;
import com.zhixue.ai.module.exam.entity.ExamQuestion;
import com.zhixue.ai.module.exam.mapper.ExamQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 错题变式题推送引擎
 * <p>策略:同知识点 + 难度递进,从题库中匹配变式题</p>
 */
@Component
@RequiredArgsConstructor
public class VariantEngine {

    private final ExamQuestionMapper questionMapper;

    /**
     * 为错题匹配变式题
     * @param sourceQuestion 原错题
     * @param studentId 学生ID
     * @return 变式题记录(已包装),无匹配返回 null
     */
    public AiVariantQuestion pushVariant(ExamQuestion sourceQuestion, Long studentId) {
        if (sourceQuestion == null || sourceQuestion.getKnowledgePoint() == null) {
            return null;
        }
        // 同知识点 + 同题型 + 不同ID,优先难度+1
        int targetDifficulty = Math.min(5, sourceQuestion.getDifficulty() + 1);
        List<ExamQuestion> candidates = questionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getSubjectId, sourceQuestion.getSubjectId())
                        .eq(ExamQuestion::getQuestionType, sourceQuestion.getQuestionType())
                        .eq(ExamQuestion::getKnowledgePoint, sourceQuestion.getKnowledgePoint())
                        .ne(ExamQuestion::getId, sourceQuestion.getId())
                        .eq(ExamQuestion::getDifficulty, targetDifficulty)
                        .last("LIMIT 1"));
        // 难度+1 找不到,放宽到任意难度
        if (candidates.isEmpty()) {
            candidates = questionMapper.selectList(
                    new LambdaQueryWrapper<ExamQuestion>()
                            .eq(ExamQuestion::getSubjectId, sourceQuestion.getSubjectId())
                            .eq(ExamQuestion::getQuestionType, sourceQuestion.getQuestionType())
                            .eq(ExamQuestion::getKnowledgePoint, sourceQuestion.getKnowledgePoint())
                            .ne(ExamQuestion::getId, sourceQuestion.getId())
                            .last("LIMIT 1"));
        }
        if (candidates.isEmpty()) {
            return null;
        }
        ExamQuestion vq = candidates.get(0);
        AiVariantQuestion variant = new AiVariantQuestion();
        variant.setSourceQuestionId(sourceQuestion.getId());
        variant.setStudentId(studentId);
        variant.setContent(vq.getContent());
        variant.setOptions(vq.getOptions());
        variant.setStandardAnswer(vq.getStandardAnswer());
        variant.setKnowledgePoint(vq.getKnowledgePoint());
        variant.setIsSolved(0);
        return variant;
    }
}
