package com.zhixue.ai.module.exam.service;

import com.zhixue.ai.module.ai.service.SelfPracticeService;
import com.zhixue.ai.module.exam.entity.ExamQuestion;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * AI 智能组卷异步任务服务。
 * <p>AI 现场出题耗时可达数分钟(多轮大模型调用),同步等待会触发前端/代理超时,
 * 因此改为:提交任务立即返回 taskId,后台线程生成,前端轮询状态。
 * 任务结果保存在内存中(单实例部署适用),重启后历史任务失效。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiGroupTaskService {

    private final SelfPracticeService selfPracticeService;
    private final ExamService examService;
    private final com.zhixue.ai.module.exam.mapper.AiGenerateRecordMapper generateRecordMapper;

    private final Map<String, AiGroupTask> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(8);
    private final AtomicLong seq = new AtomicLong();

    /** 提交组卷任务,立即返回 taskId */
    public String submit(Map<String, Object> config, Long creatorId) {
        String taskId = "aig" + seq.incrementAndGet();
        AiGroupTask task = new AiGroupTask(taskId);
        tasks.put(taskId, task);
        executor.submit(() -> run(task, config, creatorId));
        return taskId;
    }

    private void run(AiGroupTask task, Map<String, Object> config, Long creatorId) {
        try {
            // 1. 教师组卷: 按题型数量分布 AI 出题 + 题库补齐
            List<ExamQuestion> questions = selfPracticeService.generatePaperForTeacher(config, creatorId);
            List<Long> ids;
            String source;
            if (!questions.isEmpty()) {
                source = "ai";
                ids = questions.stream().map(ExamQuestion::getId).collect(Collectors.toList());
            } else {
                // 2. 极端兜底: 全部失败时按学科随机抽题
                source = "bank";
                Long subjectId = config.get("subjectId") != null
                        ? Long.valueOf(config.get("subjectId").toString()) : null;
                ids = subjectId != null
                        ? examService.aiGroupPaper(subjectId, 10, null, null)
                        : Collections.emptyList();
            }
            task.finish(source, ids);
            // 持久化出题记录:同一次出题的题目通过 question_ids 归组,教师端可回看
            saveRecord(creatorId, config, source, ids);
        } catch (Exception e) {
            log.error("AI 组卷任务 {} 执行失败", task.taskId, e);
            task.fail(e.getMessage());
        }
    }

    /** 保存出题记录(失败仅记日志,不影响出题结果) */
    private void saveRecord(Long teacherId, Map<String, Object> config, String source, List<Long> ids) {
        try {
            com.zhixue.ai.module.exam.entity.AiGenerateRecord record =
                    new com.zhixue.ai.module.exam.entity.AiGenerateRecord();
            record.setTeacherId(teacherId);
            record.setSubjectId(config.get("subjectId") != null
                    ? Long.valueOf(config.get("subjectId").toString()) : null);
            record.setSource(source);
            record.setQuestionIds(com.alibaba.fastjson2.JSON.toJSONString(ids));
            record.setQuestionCount(ids == null ? 0 : ids.size());
            record.setConfig(com.alibaba.fastjson2.JSON.toJSONString(config));
            generateRecordMapper.insert(record);
        } catch (Exception e) {
            log.warn("保存出题记录失败: {}", e.getMessage());
        }
    }

    /** 查询任务(不存在返回 null) */
    public AiGroupTask get(String taskId) {
        return tasks.get(taskId);
    }

    /** 查询教师的出题记录列表(最新在前,最多50条) */
    public java.util.List<com.zhixue.ai.module.exam.entity.AiGenerateRecord> listRecords(Long teacherId) {
        return generateRecordMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.zhixue.ai.module.exam.entity.AiGenerateRecord>()
                        .eq(com.zhixue.ai.module.exam.entity.AiGenerateRecord::getTeacherId, teacherId)
                        .orderByDesc(com.zhixue.ai.module.exam.entity.AiGenerateRecord::getCreateTime)
                        .last("LIMIT 50"));
    }

    /** 查询某次出题记录的题目明细 */
    public java.util.List<ExamQuestion> getRecordQuestions(Long recordId, Long teacherId) {
        com.zhixue.ai.module.exam.entity.AiGenerateRecord record = generateRecordMapper.selectById(recordId);
        if (record == null || !record.getTeacherId().equals(teacherId)) {
            throw new com.zhixue.ai.common.exception.BizException("出题记录不存在");
        }
        java.util.List<Long> ids = com.alibaba.fastjson2.JSON.parseArray(record.getQuestionIds(), Long.class);
        if (ids == null || ids.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        return examService.getQuestionsByIds(ids);
    }

    /** 删除出题记录(仅删记录条目,不影响题目本身),校验只能删自己的记录 */
    public void deleteRecord(Long recordId, Long teacherId) {
        com.zhixue.ai.module.exam.entity.AiGenerateRecord record = generateRecordMapper.selectById(recordId);
        if (record == null || !record.getTeacherId().equals(teacherId)) {
            throw new com.zhixue.ai.common.exception.BizException("出题记录不存在");
        }
        generateRecordMapper.deleteById(recordId);
    }

    /** 组卷任务状态(内存对象) */
    @Getter
    public static class AiGroupTask {
        private final String taskId;
        private volatile String status = "RUNNING"; // RUNNING / DONE / FAILED
        private volatile List<Long> questionIds = Collections.emptyList();
        private volatile String source;
        private volatile String message;

        AiGroupTask(String taskId) {
            this.taskId = taskId;
        }

        void finish(String source, List<Long> ids) {
            this.status = "DONE";
            this.source = source;
            this.questionIds = ids == null ? Collections.emptyList() : new ArrayList<>(ids);
        }

        void fail(String msg) {
            this.status = "FAILED";
            this.message = msg;
        }

        public int getCount() {
            return questionIds.size();
        }
    }
}
