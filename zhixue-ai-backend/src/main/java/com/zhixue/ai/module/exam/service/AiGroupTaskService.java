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

    private final Map<String, AiGroupTask> tasks = new ConcurrentHashMap<>();
    private final ExecutorService executor = Executors.newFixedThreadPool(2);
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
            if (!questions.isEmpty()) {
                task.finish("ai", questions.stream().map(ExamQuestion::getId).collect(Collectors.toList()));
                return;
            }
            // 2. 极端兜底: 全部失败时按学科随机抽题
            Long subjectId = config.get("subjectId") != null
                    ? Long.valueOf(config.get("subjectId").toString()) : null;
            List<Long> bankIds = subjectId != null
                    ? examService.aiGroupPaper(subjectId, 10, null, null)
                    : Collections.emptyList();
            task.finish("bank", bankIds);
        } catch (Exception e) {
            log.error("AI 组卷任务 {} 执行失败", task.taskId, e);
            task.fail(e.getMessage());
        }
    }

    /** 查询任务(不存在返回 null) */
    public AiGroupTask get(String taskId) {
        return tasks.get(taskId);
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
