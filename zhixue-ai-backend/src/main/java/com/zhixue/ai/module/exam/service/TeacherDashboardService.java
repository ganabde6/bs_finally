package com.zhixue.ai.module.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixue.ai.common.constant.SystemConstants;
import com.zhixue.ai.module.ai.entity.AiLsHomework;
import com.zhixue.ai.module.ai.entity.AiLsHomeworkRecord;
import com.zhixue.ai.module.ai.mapper.AiLsHomeworkMapper;
import com.zhixue.ai.module.ai.mapper.AiLsHomeworkRecordMapper;
import com.zhixue.ai.module.exam.entity.ExamAnswer;
import com.zhixue.ai.module.exam.entity.ExamPaper;
import com.zhixue.ai.module.exam.mapper.ExamAnswerMapper;
import com.zhixue.ai.module.exam.mapper.ExamPaperMapper;
import com.zhixue.ai.module.system.entity.SysClass;
import com.zhixue.ai.module.system.entity.SysTeacherClass;
import com.zhixue.ai.module.system.entity.SysUser;
import com.zhixue.ai.module.system.mapper.SysClassMapper;
import com.zhixue.ai.module.system.mapper.SysTeacherClassMapper;
import com.zhixue.ai.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 教师首页数据服务
 */
@Service
@RequiredArgsConstructor
public class TeacherDashboardService {

    private final ExamPaperMapper paperMapper;
    private final ExamAnswerMapper answerMapper;
    private final SysUserMapper userMapper;
    private final SysClassMapper classMapper;
    private final SysTeacherClassMapper teacherClassMapper;
    private final AiLsHomeworkMapper lsHomeworkMapper;
    private final AiLsHomeworkRecordMapper lsRecordMapper;

    private static final DateTimeFormatter DAY_FMT = DateTimeFormatter.ofPattern("MM-dd");

    /** 教师首页汇总数据 */
    public Map<String, Object> dashboard(Long teacherId) {
        Map<String, Object> result = new HashMap<>();

        // 教师所教班级
        List<SysTeacherClass> teacherClasses = teacherClassMapper.selectByTeacherId(teacherId);
        List<Long> classIds = teacherClasses.stream()
                .map(SysTeacherClass::getClassId)
                .collect(Collectors.toList());
        Map<Long, SysClass> classMap = classIds.isEmpty() ? Collections.emptyMap()
                : classMapper.selectBatchIds(classIds).stream()
                        .collect(Collectors.toMap(SysClass::getId, c -> c));

        // 所教学生总数
        long studentCount = classIds.isEmpty() ? 0 : userMapper.selectCount(
                new LambdaQueryWrapper<SysUser>()
                        .eq(SysUser::getRoleId, 4L)
                        .in(SysUser::getClassId, classIds));
        result.put("studentCount", studentCount);

        // 教师的全部试卷
        List<ExamPaper> papers = paperMapper.selectList(new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getCreatorId, teacherId)
                .orderByDesc(ExamPaper::getCreateTime));
        result.put("paperCount", papers.size());
        result.put("publishedCount", papers.stream()
                .filter(p -> p.getStatus() != null && p.getStatus() >= SystemConstants.PAPER_STATUS_PUBLISHED)
                .count());

        List<Long> paperIds = papers.stream().map(ExamPaper::getId).collect(Collectors.toList());

        // 全部作答记录
        List<ExamAnswer> answers = paperIds.isEmpty() ? Collections.emptyList()
                : answerMapper.selectList(new LambdaQueryWrapper<ExamAnswer>()
                        .in(ExamAnswer::getPaperId, paperIds));

        // 待批改数 + 按试卷统计待批改(用于待办列表)
        Map<Long, List<ExamAnswer>> answersByPaper = answers.stream()
                .collect(Collectors.groupingBy(ExamAnswer::getPaperId));
        List<Map<String, Object>> pendingList = new ArrayList<>();
        long pendingCorrect = 0;
        for (ExamPaper paper : papers) {
            long pending = answersByPaper.getOrDefault(paper.getId(), Collections.emptyList()).stream()
                    .filter(a -> a.getStatus() != null && a.getStatus() == SystemConstants.ANSWER_STATUS_SUBMITTED)
                    .count();
            if (pending > 0) {
                pendingCorrect += pending;
                Map<String, Object> m = new HashMap<>();
                m.put("paperId", paper.getId());
                m.put("paperName", paper.getPaperName());
                m.put("className", className(classMap, paper.getClassId()));
                m.put("pendingCount", pending);
                m.put("deadline", paper.getDeadline());
                pendingList.add(m);
            }
        }
        pendingList.sort(Comparator.comparing(m -> (Long) m.get("pendingCount"), Comparator.reverseOrder()));
        result.put("pendingCorrect", pendingCorrect);
        result.put("pendingList", pendingList.size() > 5 ? pendingList.subList(0, 5) : pendingList);

        // 最近发布的作业(含提交人数/班级人数)
        List<Map<String, Object>> recentPapers = new ArrayList<>();
        for (ExamPaper paper : papers.stream().limit(5).collect(Collectors.toList())) {
            Map<String, Object> m = new HashMap<>();
            m.put("id", paper.getId());
            m.put("paperName", paper.getPaperName());
            m.put("paperType", paper.getPaperType());
            m.put("status", paper.getStatus());
            m.put("deadline", paper.getDeadline());
            m.put("className", className(classMap, paper.getClassId()));
            List<ExamAnswer> pas = answersByPaper.getOrDefault(paper.getId(), Collections.emptyList());
            long submitted = pas.stream()
                    .filter(a -> a.getStatus() != null && a.getStatus() >= SystemConstants.ANSWER_STATUS_SUBMITTED)
                    .count();
            m.put("submittedCount", submitted);
            m.put("classStudentCount", countClassStudents(paper.getClassId()));
            recentPapers.add(m);
        }
        result.put("recentPapers", recentPapers);

        // 近7天提交趋势(提交数 / 按时提交数)
        LocalDate today = LocalDate.now();
        LocalDateTime sevenDaysAgo = today.minusDays(6).atStartOfDay();
        List<ExamAnswer> recentAnswers = answers.stream()
                .filter(a -> a.getStatus() != null && a.getStatus() >= SystemConstants.ANSWER_STATUS_SUBMITTED)
                .filter(a -> a.getSubmitTime() != null && a.getSubmitTime().isAfter(sevenDaysAgo))
                .collect(Collectors.toList());
        Map<Long, LocalDateTime> deadlineMap = papers.stream()
                .filter(p -> p.getDeadline() != null)
                .collect(Collectors.toMap(ExamPaper::getId, ExamPaper::getDeadline));
        List<Map<String, Object>> trend = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            LocalDateTime start = day.atStartOfDay();
            LocalDateTime end = day.plusDays(1).atStartOfDay();
            List<ExamAnswer> dayAnswers = recentAnswers.stream()
                    .filter(a -> !a.getSubmitTime().isBefore(start) && a.getSubmitTime().isBefore(end))
                    .collect(Collectors.toList());
            long onTime = dayAnswers.stream()
                    .filter(a -> {
                        LocalDateTime dl = deadlineMap.get(a.getPaperId());
                        return dl == null || !a.getSubmitTime().isAfter(dl);
                    })
                    .count();
            Map<String, Object> m = new HashMap<>();
            m.put("date", day.format(DAY_FMT));
            m.put("submitted", dayAnswers.size());
            m.put("onTime", onTime);
            trend.add(m);
        }
        result.put("submitTrend", trend);

        // 成绩分布(按得分率: 优秀>=85%, 良好70~85, 及格60~70, 不及格<60)
        Map<Long, BigDecimal> totalScoreMap = papers.stream()
                .filter(p -> p.getTotalScore() != null)
                .collect(Collectors.toMap(ExamPaper::getId, ExamPaper::getTotalScore, (a, b) -> a));
        long excellent = 0, good = 0, pass = 0, fail = 0;
        for (ExamAnswer a : answers) {
            if (a.getStatus() == null || a.getStatus() < SystemConstants.ANSWER_STATUS_CORRECTED
                    || a.getTotalScore() == null) continue;
            BigDecimal total = totalScoreMap.get(a.getPaperId());
            if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) continue;
            double rate = a.getTotalScore().divide(total, 4, RoundingMode.HALF_UP).doubleValue();
            if (rate >= 0.85) excellent++;
            else if (rate >= 0.70) good++;
            else if (rate >= 0.60) pass++;
            else fail++;
        }
        List<Map<String, Object>> scoreDist = new ArrayList<>();
        scoreDist.add(distItem("优秀", excellent));
        scoreDist.add(distItem("良好", good));
        scoreDist.add(distItem("及格", pass));
        scoreDist.add(distItem("不及格", fail));
        result.put("scoreDistribution", scoreDist);

        // 听说作业统计
        List<AiLsHomework> lsHomeworks = lsHomeworkMapper.selectList(new LambdaQueryWrapper<AiLsHomework>()
                .eq(AiLsHomework::getTeacherId, teacherId));
        Map<String, Object> lsStats = new HashMap<>();
        lsStats.put("homeworkCount", lsHomeworks.size());
        if (!lsHomeworks.isEmpty()) {
            List<Long> lsIds = lsHomeworks.stream().map(AiLsHomework::getId).collect(Collectors.toList());
            List<AiLsHomeworkRecord> lsRecords = lsRecordMapper.selectList(new LambdaQueryWrapper<AiLsHomeworkRecord>()
                    .in(AiLsHomeworkRecord::getHomeworkId, lsIds)
                    .isNotNull(AiLsHomeworkRecord::getTotalScore));
            double avgScore = lsRecords.stream()
                    .map(AiLsHomeworkRecord::getTotalScore)
                    .mapToDouble(BigDecimal::doubleValue)
                    .average().orElse(0);
            lsStats.put("recordCount", lsRecords.size());
            lsStats.put("avgScore", Math.round(avgScore * 10) / 10.0);
        } else {
            lsStats.put("recordCount", 0);
            lsStats.put("avgScore", 0);
        }
        result.put("lsStats", lsStats);

        return result;
    }

    private Map<String, Object> distItem(String name, long count) {
        Map<String, Object> m = new HashMap<>();
        m.put("name", name);
        m.put("value", count);
        return m;
    }

    private String className(Map<Long, SysClass> classMap, Long classId) {
        SysClass c = classId == null ? null : classMap.get(classId);
        return c == null ? "未分配班级" : c.getClassName();
    }

    private long countClassStudents(Long classId) {
        if (classId == null) return 0;
        return userMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, 4L)
                .eq(SysUser::getClassId, classId));
    }
}
