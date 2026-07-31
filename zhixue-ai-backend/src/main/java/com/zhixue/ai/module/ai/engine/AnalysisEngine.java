package com.zhixue.ai.module.ai.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhixue.ai.module.ai.entity.*;
import com.zhixue.ai.module.ai.mapper.*;
import com.zhixue.ai.module.exam.entity.ExamAnswer;
import com.zhixue.ai.module.exam.entity.ExamPaper;
import com.zhixue.ai.module.exam.entity.ExamQuestion;
import com.zhixue.ai.module.exam.mapper.ExamAnswerMapper;
import com.zhixue.ai.module.exam.mapper.ExamPaperMapper;
import com.zhixue.ai.module.exam.mapper.ExamQuestionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 学情分析引擎
 * <p>支持:</p>
 * <ul>
 *   <li>个人学情:平均分、成绩趋势、薄弱/优势知识点、提升建议</li>
 *   <li>班级学情:平均分、及格率、优秀率、共性薄弱点、分层归类</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisEngine {

    private final ExamAnswerMapper answerMapper;
    private final AiCorrectRecordMapper correctMapper;
    private final ExamQuestionMapper questionMapper;
    private final ExamPaperMapper paperMapper;
    private final AiStudyAnalysisMapper studyAnalysisMapper;
    private final AiClassAnalysisMapper classAnalysisMapper;
    private final AiErrorBookMapper errorBookMapper;
    private final AiServiceProvider aiServiceProvider;

    /** 及格线比例 */
    private static final double PASS_RATIO = 0.6;
    /** 优秀线比例 */
    private static final double EXCELLENT_RATIO = 0.85;

    /**
     * 生成/更新个人学情分析
     */
    public AiStudyAnalysis analyzeStudent(Long studentId, Long subjectId) {
        // 1. 查询学生该学科所有作答
        List<ExamAnswer> answers = answerMapper.selectList(
                new LambdaQueryWrapper<ExamAnswer>().eq(ExamAnswer::getStudentId, studentId));
        // 过滤学科
        List<ExamAnswer> filtered = new ArrayList<>();
        for (ExamAnswer a : answers) {
            ExamPaper p = paperMapper.selectById(a.getPaperId());
            if (p != null && (subjectId == null || subjectId.equals(p.getSubjectId()))) {
                filtered.add(a);
            }
        }
        if (filtered.isEmpty()) {
            return null;
        }
        // 2. 计算平均分
        double avg = filtered.stream()
                .filter(a -> a.getTotalScore() != null)
                .mapToDouble(a -> a.getTotalScore().doubleValue())
                .average().orElse(0);
        // 3. 成绩趋势
        JSONArray trend = new JSONArray();
        for (ExamAnswer a : filtered) {
            ExamPaper p = paperMapper.selectById(a.getPaperId());
            if (p != null && a.getTotalScore() != null) {
                JSONObject t = new JSONObject();
                t.put("paper", p.getPaperName());
                t.put("score", a.getTotalScore());
                t.put("type", p.getPaperType());
                trend.add(t);
            }
        }
        // 4. 薄弱/优势知识点
        Map<String, int[]> kpStats = new HashMap<>(); // kp -> [对, 错]
        for (ExamAnswer a : filtered) {
            List<AiCorrectRecord> records = correctMapper.selectByAnswerId(a.getId());
            for (AiCorrectRecord r : records) {
                ExamQuestion q = questionMapper.selectById(r.getQuestionId());
                if (q != null && q.getKnowledgePoint() != null) {
                    int[] st = kpStats.computeIfAbsent(q.getKnowledgePoint(), k -> new int[2]);
                    if (r.getIsCorrect() != null && r.getIsCorrect() == 1) st[0]++;
                    else st[1]++;
                }
            }
        }
        List<String> weak = new ArrayList<>();
        List<String> strong = new ArrayList<>();
        for (Map.Entry<String, int[]> e : kpStats.entrySet()) {
            int[] st = e.getValue();
            int total = st[0] + st[1];
            if (total == 0) continue;
            double rate = (double) st[0] / total;
            if (rate < PASS_RATIO) weak.add(e.getKey());
            else if (rate >= EXCELLENT_RATIO) strong.add(e.getKey());
        }
        // 5. AI 生成提升建议
        String suggestion = aiServiceProvider.generateStudySuggestion(
                String.join("、", weak), String.join("、", strong));
        // 6. 持久化
        AiStudyAnalysis exist = studyAnalysisMapper.selectOne(
                new LambdaQueryWrapper<AiStudyAnalysis>()
                        .eq(AiStudyAnalysis::getStudentId, studentId)
                        .eq(subjectId != null, AiStudyAnalysis::getSubjectId, subjectId));
        AiStudyAnalysis analysis = exist != null ? exist : new AiStudyAnalysis();
        analysis.setStudentId(studentId);
        analysis.setSubjectId(subjectId);
        analysis.setAvgScore(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        analysis.setTrend(trend.toJSONString());
        analysis.setWeakPoints(JSON.toJSONString(weak));
        analysis.setStrongPoints(JSON.toJSONString(strong));
        analysis.setSuggestion(suggestion);
        if (exist == null) studyAnalysisMapper.insert(analysis);
        else studyAnalysisMapper.updateById(analysis);
        return analysis;
    }

    /**
     * 生成/更新班级学情分析
     */
    public AiClassAnalysis analyzeClass(Long classId, Long subjectId) {
        // 查询班级所有学生的该学科作答
        List<ExamAnswer> allAnswers = new ArrayList<>();
        // 通过 paper_id 找到对应班级和学科的试卷
        List<ExamPaper> papers = paperMapper.selectList(
                new LambdaQueryWrapper<ExamPaper>()
                        .eq(ExamPaper::getClassId, classId)
                        .eq(subjectId != null, ExamPaper::getSubjectId, subjectId));
        if (papers.isEmpty()) return null;
        List<Double> scores = new ArrayList<>();
        Map<String, Integer> errorKpCount = new HashMap<>();
        int passCount = 0, excellentCount = 0, totalCount = 0;
        JSONArray layering = new JSONArray();
        int excellent = 0, good = 0, improve = 0;
        for (ExamPaper p : papers) {
            List<ExamAnswer> answers = answerMapper.selectByPaperId(p.getId());
            BigDecimal fullScore = p.getTotalScore();
            for (ExamAnswer a : answers) {
                if (a.getTotalScore() == null) continue;
                double s = a.getTotalScore().doubleValue();
                scores.add(s);
                totalCount++;
                double ratio = fullScore.doubleValue() > 0 ? s / fullScore.doubleValue() : 0;
                if (ratio >= EXCELLENT_RATIO) { excellentCount++; excellent++; }
                else if (ratio >= PASS_RATIO) { good++; }
                else { improve++; }
                // 统计错题知识点
                List<AiCorrectRecord> records = correctMapper.selectByAnswerId(a.getId());
                for (AiCorrectRecord r : records) {
                    if (r.getIsCorrect() == null || r.getIsCorrect() != 1) {
                        ExamQuestion q = questionMapper.selectById(r.getQuestionId());
                        if (q != null && q.getKnowledgePoint() != null) {
                            errorKpCount.merge(q.getKnowledgePoint(), 1, Integer::sum);
                        }
                    }
                }
            }
        }
        if (scores.isEmpty()) return null;
        double avg = scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        double passRate = totalCount > 0 ? (double) (passCount + good + excellent) * 100 / totalCount : 0;
        // 重新计算及格率
        long passNum = scores.stream().filter(s -> true).count(); // 占位
        double actualPassRate = totalCount > 0 ? (good + excellent) * 100.0 / totalCount : 0;
        double actualExcellentRate = totalCount > 0 ? excellent * 100.0 / totalCount : 0;
        // 共性薄弱点(错题最多的 top3 知识点)
        List<String> commonErrors = errorKpCount.entrySet().stream()
                .sorted((x, y) -> y.getValue() - x.getValue())
                .limit(3)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        // 分层归类
        JSONObject layerExcellent = new JSONObject();
        layerExcellent.put("layer", "优秀");
        layerExcellent.put("count", excellent);
        layering.add(layerExcellent);
        JSONObject layerGood = new JSONObject();
        layerGood.put("layer", "良好");
        layerGood.put("count", good);
        layering.add(layerGood);
        JSONObject layerImprove = new JSONObject();
        layerImprove.put("layer", "待提升");
        layerImprove.put("count", improve);
        layering.add(layerImprove);
        // 教学建议
        String advice = buildTeachingAdvice(avg, actualPassRate, actualExcellentRate, commonErrors);
        // 持久化
        AiClassAnalysis exist = classAnalysisMapper.selectOne(
                new LambdaQueryWrapper<AiClassAnalysis>()
                        .eq(AiClassAnalysis::getClassId, classId)
                        .eq(subjectId != null, AiClassAnalysis::getSubjectId, subjectId));
        AiClassAnalysis analysis = exist != null ? exist : new AiClassAnalysis();
        analysis.setClassId(classId);
        analysis.setSubjectId(subjectId);
        analysis.setAvgScore(BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP));
        analysis.setPassRate(BigDecimal.valueOf(actualPassRate).setScale(2, RoundingMode.HALF_UP));
        analysis.setExcellentRate(BigDecimal.valueOf(actualExcellentRate).setScale(2, RoundingMode.HALF_UP));
        analysis.setCommonErrors(JSON.toJSONString(commonErrors));
        analysis.setLayering(layering.toJSONString());
        analysis.setTeachingAdvice(advice);
        if (exist == null) classAnalysisMapper.insert(analysis);
        else classAnalysisMapper.updateById(analysis);
        return analysis;
    }

    private String buildTeachingAdvice(double avg, double passRate, double excellentRate, List<String> commonErrors) {
        StringBuilder sb = new StringBuilder();
        sb.append("【班级教学分析报告】\n");
        sb.append("班级平均分:").append(String.format("%.2f", avg)).append(",及格率:")
          .append(String.format("%.1f%%", passRate)).append(",优秀率:")
          .append(String.format("%.1f%%", excellentRate)).append("。\n\n");
        if (!commonErrors.isEmpty()) {
            sb.append("共性薄弱知识点:").append(String.join("、", commonErrors)).append("。\n");
            sb.append("建议:\n");
            sb.append("1. 针对薄弱知识点开展专题复习课\n");
            sb.append("2. 课堂增加互动练习,巩固概念理解\n");
            sb.append("3. 对「待提升」层学生进行个别辅导\n");
            sb.append("4. 对「优秀」层学生布置拓展性题目\n");
        }
        if (passRate < 60) {
            sb.append("\n⚠ 及格率偏低,建议降低教学难度,夯实基础。\n");
        }
        if (excellentRate < 20) {
            sb.append("\n⚠ 优秀率偏低,建议对优秀层学生增加挑战性训练。\n");
        }
        return sb.toString();
    }
}
