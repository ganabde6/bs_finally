package com.zhixue.ai.module.monitor.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixue.ai.common.constant.SystemConstants;
import com.zhixue.ai.module.exam.entity.ExamAnswer;
import com.zhixue.ai.module.exam.entity.ExamPaper;
import com.zhixue.ai.module.exam.mapper.ExamAnswerMapper;
import com.zhixue.ai.module.exam.mapper.ExamPaperMapper;
import com.zhixue.ai.module.system.entity.SysClass;
import com.zhixue.ai.module.system.entity.SysSubject;
import com.zhixue.ai.module.system.entity.SysUser;
import com.zhixue.ai.module.system.mapper.SysClassMapper;
import com.zhixue.ai.module.system.mapper.SysSubjectMapper;
import com.zhixue.ai.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 作业监测服务(管理端)
 * <p>
 * 全校作业布置与完成情况总览:作业任务进度、班级完成率排行,
 * 服务于家校一体化场景下的作业管理
 */
@Service
@RequiredArgsConstructor
public class HomeworkMonitorService {

    private final ExamPaperMapper paperMapper;
    private final ExamAnswerMapper answerMapper;
    private final SysUserMapper userMapper;
    private final SysClassMapper classMapper;
    private final SysSubjectMapper subjectMapper;

    /** 作业监测概览(仅统计已发布的作业) */
    public Map<String, Object> summary() {
        List<ExamPaper> homeworks = publishedHomeworks();
        Map<Long, Integer> classStudentCount = countStudentsByClass();

        long assigned = 0, submitted = 0, corrected = 0;
        double scoreSum = 0;
        long scoreCount = 0;
        for (ExamPaper p : homeworks) {
            assigned += classStudentCount.getOrDefault(p.getClassId(), 0);
            for (ExamAnswer a : answerMapper.selectByPaperId(p.getId())) {
                if (a.getStatus() == null) continue;
                if (a.getStatus() >= SystemConstants.ANSWER_STATUS_SUBMITTED) submitted++;
                if (a.getStatus() >= SystemConstants.ANSWER_STATUS_CORRECTED && a.getTotalScore() != null) {
                    corrected++;
                    scoreSum += a.getTotalScore().doubleValue();
                    scoreCount++;
                }
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("homeworkCount", homeworks.size());
        result.put("assignedCount", assigned);
        result.put("submittedCount", submitted);
        result.put("pendingCount", assigned - submitted);
        result.put("completionRate", pct(submitted, assigned));
        result.put("pendingCorrectCount", submitted - corrected);
        result.put("correctRate", pct(corrected, submitted));
        result.put("avgScore", scoreCount > 0 ? Math.round(scoreSum * 100 / scoreCount) / 100.0 : 0);
        return result;
    }

    /** 作业任务分页列表(带完成/批改进度) */
    public Page<Map<String, Object>> pageTasks(Long current, Long size, Long classId, Long subjectId, String keyword) {
        Page<ExamPaper> page = paperMapper.selectPage(new Page<>(current, size),
                new LambdaQueryWrapper<ExamPaper>()
                        .eq(ExamPaper::getPaperType, SystemConstants.PAPER_TYPE_HOMEWORK)
                        .ge(ExamPaper::getStatus, SystemConstants.PAPER_STATUS_PUBLISHED)
                        .eq(classId != null, ExamPaper::getClassId, classId)
                        .eq(subjectId != null, ExamPaper::getSubjectId, subjectId)
                        .like(keyword != null && !keyword.isBlank(), ExamPaper::getPaperName, keyword)
                        .orderByDesc(ExamPaper::getPublishTime));

        Map<Long, String> classNameMap = nameMap(classMapper.selectList(null), SysClass::getId, SysClass::getClassName);
        Map<Long, String> subjectNameMap = nameMap(subjectMapper.selectList(null), SysSubject::getId, SysSubject::getSubjectName);
        Map<Long, String> teacherNameMap = teacherNameMap(page.getRecords());
        Map<Long, Integer> classStudentCount = countStudentsByClass();

        List<Map<String, Object>> rows = new ArrayList<>();
        for (ExamPaper p : page.getRecords()) {
            int studentCount = classStudentCount.getOrDefault(p.getClassId(), 0);
            long submitted = 0, corrected = 0;
            double scoreSum = 0;
            long scoreCount = 0;
            for (ExamAnswer a : answerMapper.selectByPaperId(p.getId())) {
                if (a.getStatus() == null) continue;
                if (a.getStatus() >= SystemConstants.ANSWER_STATUS_SUBMITTED) submitted++;
                if (a.getStatus() >= SystemConstants.ANSWER_STATUS_CORRECTED && a.getTotalScore() != null) {
                    corrected++;
                    scoreSum += a.getTotalScore().doubleValue();
                    scoreCount++;
                }
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("paperName", p.getPaperName());
            m.put("subjectName", subjectNameMap.getOrDefault(p.getSubjectId(), "-"));
            m.put("className", classNameMap.getOrDefault(p.getClassId(), "-"));
            m.put("teacherName", teacherNameMap.getOrDefault(p.getCreatorId(), "-"));
            m.put("publishTime", p.getPublishTime());
            m.put("deadline", p.getDeadline());
            m.put("status", p.getStatus());
            m.put("studentCount", studentCount);
            m.put("submittedCount", submitted);
            m.put("completionRate", pct(submitted, studentCount));
            m.put("correctRate", pct(corrected, submitted));
            m.put("avgScore", scoreCount > 0 ? Math.round(scoreSum * 100 / scoreCount) / 100.0 : 0);
            rows.add(m);
        }

        Page<Map<String, Object>> result = new Page<>(page.getCurrent(), page.getSize(), page.getTotal());
        result.setRecords(rows);
        return result;
    }

    /** 班级作业完成率排行 */
    public List<Map<String, Object>> classRanking() {
        List<ExamPaper> homeworks = publishedHomeworks();
        Map<Long, Integer> classStudentCount = countStudentsByClass();

        Map<Long, List<ExamPaper>> byClass = homeworks.stream()
                .collect(Collectors.groupingBy(ExamPaper::getClassId));
        Map<Long, String> classNameMap = nameMap(classMapper.selectList(null), SysClass::getId, SysClass::getClassName);

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, List<ExamPaper>> e : byClass.entrySet()) {
            int studentCount = classStudentCount.getOrDefault(e.getKey(), 0);
            long assigned = (long) e.getValue().size() * studentCount;
            long submitted = 0, corrected = 0;
            double scoreSum = 0;
            long scoreCount = 0;
            for (ExamPaper p : e.getValue()) {
                for (ExamAnswer a : answerMapper.selectByPaperId(p.getId())) {
                    if (a.getStatus() == null) continue;
                    if (a.getStatus() >= SystemConstants.ANSWER_STATUS_SUBMITTED) submitted++;
                    if (a.getStatus() >= SystemConstants.ANSWER_STATUS_CORRECTED && a.getTotalScore() != null) {
                        corrected++;
                        scoreSum += a.getTotalScore().doubleValue();
                        scoreCount++;
                    }
                }
            }
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("classId", e.getKey());
            m.put("className", classNameMap.getOrDefault(e.getKey(), "-"));
            m.put("homeworkCount", e.getValue().size());
            m.put("assignedCount", assigned);
            m.put("submittedCount", submitted);
            m.put("completionRate", pct(submitted, assigned));
            m.put("avgScore", scoreCount > 0 ? Math.round(scoreSum * 100 / scoreCount) / 100.0 : 0);
            result.add(m);
        }
        result.sort((a, b) -> Double.compare(
                (Double) b.getOrDefault("completionRate", 0.0),
                (Double) a.getOrDefault("completionRate", 0.0)));
        return result;
    }

    // ===================== 私有工具 =====================

    /** 全部已发布(含已结束)的作业 */
    private List<ExamPaper> publishedHomeworks() {
        return paperMapper.selectList(new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getPaperType, SystemConstants.PAPER_TYPE_HOMEWORK)
                .ge(ExamPaper::getStatus, SystemConstants.PAPER_STATUS_PUBLISHED));
    }

    /** 各班学生人数(role_id=4 为学生) */
    private Map<Long, Integer> countStudentsByClass() {
        List<SysUser> students = userMapper.selectList(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getRoleId, 4L)
                .isNotNull(SysUser::getClassId));
        return students.stream()
                .collect(Collectors.groupingBy(SysUser::getClassId, Collectors.summingInt(u -> 1)));
    }

    /** 实体列表 -> id->名称 映射 */
    private <T> Map<Long, String> nameMap(List<T> list, Function<T, Long> idFn, Function<T, String> nameFn) {
        Map<Long, String> map = new HashMap<>();
        for (T item : list) {
            map.put(idFn.apply(item), nameFn.apply(item));
        }
        return map;
    }

    /** 创建教师真实姓名映射 */
    private Map<Long, String> teacherNameMap(List<ExamPaper> papers) {
        Set<Long> creatorIds = papers.stream()
                .map(ExamPaper::getCreatorId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (creatorIds.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(creatorIds).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getRealName, (a, b) -> a));
    }

    /** 百分比(保留两位小数) */
    private double pct(long part, long total) {
        return total > 0 ? Math.round((double) part * 10000 / total) / 100.0 : 0;
    }
}
