package com.zhixue.ai.module.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixue.ai.module.ai.entity.*;
import com.zhixue.ai.module.ai.mapper.*;
import com.zhixue.ai.module.exam.entity.ExamAnswer;
import com.zhixue.ai.module.exam.entity.ExamPaper;
import com.zhixue.ai.module.exam.mapper.*;
import com.zhixue.ai.module.system.entity.*;
import com.zhixue.ai.module.system.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 全局数据大屏服务
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final SysUserMapper userMapper;
    private final SysClassMapper classMapper;
    private final SysSubjectMapper subjectMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamAnswerMapper answerMapper;
    private final AiCorrectRecordMapper correctMapper;
    private final AiErrorBookMapper errorBookMapper;
    private final ExamRiskLogMapper riskLogMapper;
    private final AiClassAnalysisMapper classAnalysisMapper;
    private final SysLogMapper logMapper;

    /** 全局大屏统计数据 */
    public Map<String, Object> globalStats() {
        Map<String, Object> result = new HashMap<>();
        // 用户统计
        long studentCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, 4L));
        long teacherCount = userMapper.selectCount(new LambdaQueryWrapper<SysUser>().eq(SysUser::getRoleId, 3L));
        long classCount = classMapper.selectCount(null);
        long subjectCount = subjectMapper.selectCount(null);
        result.put("studentCount", studentCount);
        result.put("teacherCount", teacherCount);
        result.put("classCount", classCount);
        result.put("subjectCount", subjectCount);
        // 试卷统计
        long paperCount = paperMapper.selectCount(null);
        long homeworkCount = paperMapper.selectCount(new LambdaQueryWrapper<ExamPaper>().eq(ExamPaper::getPaperType, 1));
        long examCount = paperMapper.selectCount(new LambdaQueryWrapper<ExamPaper>().eq(ExamPaper::getPaperType, 2));
        result.put("paperCount", paperCount);
        result.put("homeworkCount", homeworkCount);
        result.put("examCount", examCount);
        // 作答统计
        long answerCount = answerMapper.selectCount(null);
        long correctedCount = answerMapper.selectCount(new LambdaQueryWrapper<ExamAnswer>()
                .ge(ExamAnswer::getStatus, 2));
        result.put("answerCount", answerCount);
        result.put("correctedCount", correctedCount);
        double correctRate = answerCount > 0 ? (double) correctedCount * 100 / answerCount : 0;
        result.put("correctRate", Math.round(correctRate * 100) / 100.0);
        // 错题统计
        long errorCount = errorBookMapper.selectCount(null);
        long reviewedCount = errorBookMapper.selectCount(new LambdaQueryWrapper<AiErrorBook>()
                .ge(AiErrorBook::getReviewStatus, 1));
        result.put("errorCount", errorCount);
        result.put("reviewedCount", reviewedCount);
        // 风控预警
        long riskCount = riskLogMapper.selectCount(null);
        long highRiskCount = riskLogMapper.selectCount(new LambdaQueryWrapper<ExamRiskLog>()
                .eq(ExamRiskLog::getRiskLevel, 3));
        result.put("riskCount", riskCount);
        result.put("highRiskCount", highRiskCount);
        // 师生活跃度(近7天登录 - 简化为用户总数)
        result.put("activeUsers", studentCount + teacherCount);
        return result;
    }

    /** 学科试卷分布 */
    public List<Map<String, Object>> paperDistribution() {
        List<SysSubject> subjects = subjectMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysSubject s : subjects) {
            long count = paperMapper.selectCount(new LambdaQueryWrapper<ExamPaper>().eq(ExamPaper::getSubjectId, s.getId()));
            Map<String, Object> m = new HashMap<>();
            m.put("subject", s.getSubjectName());
            m.put("count", count);
            result.add(m);
        }
        return result;
    }

    /** 班级成绩排名 */
    public List<Map<String, Object>> classRanking() {
        List<SysClass> classes = classMapper.selectList(null);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysClass c : classes) {
            List<AiClassAnalysis> analyses = classAnalysisMapper.selectList(
                    new LambdaQueryWrapper<AiClassAnalysis>().eq(AiClassAnalysis::getClassId, c.getId()));
            double avg = analyses.stream()
                    .filter(a -> a.getAvgScore() != null)
                    .mapToDouble(a -> a.getAvgScore().doubleValue())
                    .average().orElse(0);
            Map<String, Object> m = new HashMap<>();
            m.put("className", c.getClassName());
            m.put("avgScore", Math.round(avg * 100) / 100.0);
            result.add(m);
        }
        // 按平均分降序
        result.sort((a, b) -> Double.compare(
                (Double) b.getOrDefault("avgScore", 0.0),
                (Double) a.getOrDefault("avgScore", 0.0)));
        return result;
    }

    /** 风控类型分布 */
    public List<Map<String, Object>> riskTypeDistribution() {
        List<ExamRiskLog> logs = riskLogMapper.selectList(null);
        Map<Integer, Long> grouped = logs.stream()
                .collect(Collectors.groupingBy(ExamRiskLog::getRiskType, Collectors.counting()));
        String[] names = {"", "切屏", "超时", "人脸异常", "答案雷同", "离开窗口"};
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 1; i <= 5; i++) {
            Map<String, Object> m = new HashMap<>();
            m.put("type", names[i]);
            m.put("count", grouped.getOrDefault(i, 0L));
            result.add(m);
        }
        return result;
    }

    /** 最近操作日志 */
    public List<SysLog> recentLogs(int limit) {
        return logMapper.selectList(new LambdaQueryWrapper<SysLog>()
                .orderByDesc(SysLog::getCreateTime).last("LIMIT " + limit));
    }
}
