package com.zhixue.ai.module.ai.engine;

import com.zhixue.ai.module.ai.entity.AiCorrectRecord;
import com.zhixue.ai.module.ai.entity.ExamRiskLog;
import com.zhixue.ai.module.ai.mapper.AiCorrectRecordMapper;
import com.zhixue.ai.module.ai.mapper.ExamRiskLogMapper;
import com.zhixue.ai.module.exam.entity.ExamAnswer;
import com.zhixue.ai.module.exam.mapper.ExamAnswerMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * 考试风控引擎
 * <p>支持:切屏监测、超时监测、人脸异常、答案雷同查重、离开窗口</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RiskEngine {

    private final ExamRiskLogMapper riskLogMapper;
    private final AiCorrectRecordMapper correctMapper;
    private final ExamAnswerMapper answerMapper;

    /** 雷同度阈值(0-1) */
    private static final double SIMILARITY_THRESHOLD = 0.85;

    /**
     * 记录风控事件
     */
    public ExamRiskLog recordRisk(Long answerId, Long studentId, Integer riskType, String description) {
        ExamRiskLog log = new ExamRiskLog();
        log.setAnswerId(answerId);
        log.setStudentId(studentId);
        log.setRiskType(riskType);
        log.setRiskLevel(decideLevel(riskType, description));
        log.setDescription(description);
        riskLogMapper.insert(log);
        return log;
    }

    /** 根据风控类型决定预警等级 */
    private Integer decideLevel(Integer riskType, String desc) {
        // 切屏>=3次 高危;答案雷同 高危;其余默认低危
        if (riskType == 1 && desc != null && desc.contains("3")) return 3;
        if (riskType == 4) return 3;
        if (riskType == 2) return 2;
        return 1;
    }

    /**
     * 答案雷同查重(对一份作答记录的所有简答/计算题答案与其他学生比对)
     * @return 命中雷同的题数
     */
    public int similarityCheck(Long answerId) {
        ExamAnswer current = answerMapper.selectById(answerId);
        if (current == null) return 0;
        List<AiCorrectRecord> myRecords = correctMapper.selectByAnswerId(answerId);
        // 取同试卷的其他学生作答
        List<ExamAnswer> others = answerMapper.selectByPaperId(current.getPaperId());
        int similarCount = 0;
        for (AiCorrectRecord my : myRecords) {
            if (my.getStudentAnswer() == null || my.getStudentAnswer().length() < 10) continue;
            for (ExamAnswer other : others) {
                if (other.getId().equals(answerId)) continue;
                List<AiCorrectRecord> otherRecords = correctMapper.selectByAnswerId(other.getId());
                for (AiCorrectRecord r : otherRecords) {
                    if (r.getQuestionId().equals(my.getQuestionId()) && r.getStudentAnswer() != null) {
                        double sim = calcSimilarity(my.getStudentAnswer(), r.getStudentAnswer());
                        if (sim >= SIMILARITY_THRESHOLD) {
                            recordRisk(answerId, current.getStudentId(), 4,
                                    "题目" + my.getQuestionId() + " 答案与他人雷同,相似度:" + String.format("%.0f%%", sim * 100));
                            similarCount++;
                            break;
                        }
                    }
                }
            }
        }
        return similarCount;
    }

    /** 简化的相似度计算:基于字符 Jaccard 相似度 */
    private double calcSimilarity(String a, String b) {
        if (a == null || b == null) return 0;
        Set<Character> sa = new HashSet<>();
        for (char c : a.toCharArray()) sa.add(c);
        Set<Character> sb = new HashSet<>();
        for (char c : b.toCharArray()) sb.add(c);
        Set<Character> inter = new HashSet<>(sa);
        inter.retainAll(sb);
        Set<Character> union = new HashSet<>(sa);
        union.addAll(sb);
        return union.isEmpty() ? 0 : (double) inter.size() / union.size();
    }
}
