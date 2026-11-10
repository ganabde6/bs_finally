package com.zhixue.ai.module.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixue.ai.common.log.OperationLog;
import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.common.result.ResultCode;
import com.zhixue.ai.module.ai.service.AiService;
import com.zhixue.ai.module.ai.service.ContentRiskService;
import com.zhixue.ai.module.ai.service.SelfPracticeService;
import com.zhixue.ai.module.exam.entity.ExamPaper;
import com.zhixue.ai.module.exam.entity.ExamQuestion;
import com.zhixue.ai.module.exam.service.AiGroupTaskService;
import com.zhixue.ai.module.exam.service.ExamService;
import com.zhixue.ai.module.system.service.SystemService;
import com.zhixue.ai.module.system.entity.SysTask;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 教师端 Controller
 */
@RestController
@RequestMapping("/api/teacher")
@RequiredArgsConstructor
public class TeacherController {

    private final ExamService examService;
    private final AiService aiService;
    private final ContentRiskService contentRiskService;
    private final SelfPracticeService selfPracticeService;
    private final SystemService systemService;
    private final AiGroupTaskService aiGroupTaskService;
    private final com.zhixue.ai.module.exam.service.TeacherDashboardService teacherDashboardService;
    private final com.zhixue.ai.module.system.mapper.SysUserMapper userMapper;
    private final com.zhixue.ai.module.system.mapper.SysClassMapper classMapper;
    private final com.zhixue.ai.module.system.mapper.SysTeacherClassMapper teacherClassMapper;
    private final com.zhixue.ai.module.system.mapper.SysSubjectMapper subjectMapper;

    /** 当前教师可见的公告(全员公告 + 面向教师的已发布公告) */
    @GetMapping("/notices")
    public Result<?> notices() {
        return Result.success(systemService.listVisibleNotices("TEACHER"));
    }

    /** 当前教师的待完成任务(管理员派发:面向全体教师 + 指定本人) */
    @GetMapping("/tasks")
    public Result<?> tasks() {
        return Result.success(systemService.listMyPendingTasks(SecurityUtils.getCurrentUserId()));
    }

    @OperationLog(module = "任务管理", operation = "完成任务")
    @PostMapping("/task/{id}/complete")
    public Result<Void> completeTask(@PathVariable Long id) {
        systemService.completeTask(SecurityUtils.getCurrentUserId(), id);
        return Result.success();
    }

    // ============== 学生任务派发 ==============

    @OperationLog(module = "任务管理", operation = "派发学生任务")
    @PostMapping("/student-task")
    public Result<Void> dispatchStudentTask(@RequestBody SysTask task) {
        String text = (task.getTitle() == null ? "" : task.getTitle()) + " "
                + (task.getContent() == null ? "" : task.getContent());
        contentRiskService.audit(text, "派发学生任务");
        systemService.dispatchStudentTask(SecurityUtils.getCurrentUserId(), task);
        return Result.success();
    }

    @OperationLog(module = "任务管理", operation = "查询学生任务列表")
    @GetMapping("/student-tasks")
    public Result<?> studentTasks() {
        return Result.success(systemService.listMyStudentTasks(SecurityUtils.getCurrentUserId()));
    }

    @OperationLog(module = "任务管理", operation = "删除学生任务")
    @DeleteMapping("/student-task/{id}")
    public Result<Void> deleteStudentTask(@PathVariable Long id) {
        systemService.deleteStudentTask(SecurityUtils.getCurrentUserId(), id);
        return Result.success();
    }

    /** 个人信息:教师基本信息 + 我执教的班级/科目明细(供个人信息页展示) */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        com.zhixue.ai.module.system.entity.SysUser me = userMapper.selectById(teacherId);
        Map<String, Object> result = new java.util.HashMap<>();
        if (me != null) {
            result.put("username", me.getUsername());
            result.put("realName", me.getRealName());
            result.put("phone", me.getPhone());
            result.put("email", me.getEmail());
            result.put("avatar", me.getAvatar());
        }
        // 我执教的班级/科目明细(同一班级多科目会分开列出)
        List<Map<String, Object>> courses = new java.util.ArrayList<>();
        List<com.zhixue.ai.module.system.entity.SysTeacherClass> tcs = teacherClassMapper.selectByTeacherId(teacherId);
        for (com.zhixue.ai.module.system.entity.SysTeacherClass tc : tcs) {
            Map<String, Object> c = new java.util.HashMap<>();
            com.zhixue.ai.module.system.entity.SysClass cls =
                    classMapper.selectById(tc.getClassId());
            c.put("className", cls != null ? cls.getClassName() : "");
            com.zhixue.ai.module.system.entity.SysSubject subject = subjectMapper.selectById(tc.getSubjectId());
            c.put("subjectName", subject != null ? subject.getSubjectName() : "");
            courses.add(c);
        }
        result.put("courses", courses);
        return Result.success(result);
    }

    /** 教师首页统计 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(teacherDashboardService.dashboard(teacherId));
    }

    // ============== 题库管理 ==============

    /** 教师任教的学科列表(去重) */
    @GetMapping("/my-subjects")
    public Result<?> mySubjects() {
        return Result.success(systemService.listTeacherSubjects(SecurityUtils.getCurrentUserId()));
    }

    /** 学科专属题型: 按学科编码映射题型[value,label], 未知学科回退通用模板 */
    @GetMapping("/question-types")
    public Result<List<Map<String, Object>>> questionTypes(@RequestParam(required = false) Long subjectId) {
        com.zhixue.ai.module.system.entity.SysSubject subject = null;
        if (subjectId != null) {
            subject = systemService.listTeacherSubjects(SecurityUtils.getCurrentUserId()).stream()
                    .filter(s -> s.getId().equals(subjectId)).findFirst().orElse(null);
        } else {
            List<com.zhixue.ai.module.system.entity.SysSubject> subjects =
                    systemService.listTeacherSubjects(SecurityUtils.getCurrentUserId());
            subject = subjects.isEmpty() ? null : subjects.get(0);
        }
        return Result.success(buildSubjectQuestionTypes(subject == null ? null : subject.getSubjectCode()));
    }

    /** 学科-题型映射: 计算题等理科专属题型按学科动态返回 */
    private List<Map<String, Object>> buildSubjectQuestionTypes(String code) {
        // [value, label] 集合, value 对应 exam_question.question_type(判分引擎依赖,不可变更)
        int[][] defs;
        if (code == null) {
            defs = new int[][]{{1, 0}, {2, 0}, {4, 0}, {5, 0}, {6, 0}, {7, 0}};
        } else {
            switch (code) {
                case "chinese": // 语文: 默写归入填空, 阅读鉴赏归入简答
                    defs = new int[][]{{1, 0}, {4, 0}, {5, 0}, {6, 0}};
                    break;
                case "english": // 英语: 完形/语法归入填空, 翻译归入简答
                    defs = new int[][]{{1, 0}, {2, 0}, {4, 0}, {5, 0}, {6, 0}};
                    break;
                case "math": case "science": case "physics":
                case "chemistry": case "biology": case "it": // 理科: 含计算题, 无作文
                    defs = new int[][]{{1, 0}, {2, 0}, {4, 0}, {5, 0}, {7, 0}};
                    break;
                default: // 其他学科: 通用模板
                    defs = new int[][]{{1, 0}, {2, 0}, {4, 0}, {5, 0}, {6, 0}, {7, 0}};
            }
        }
        String[] labels = {"", "单选题", "多选题", "判断题", "填空题", "简答题", "作文题", "计算题"};
        List<Map<String, Object>> list = new java.util.ArrayList<>();
        for (int[] d : defs) {
            Map<String, Object> m = new java.util.HashMap<>();
            m.put("value", d[0]);
            m.put("label", labels[d[0]]);
            list.add(m);
        }
        return list;
    }

    @GetMapping("/questions")
    public Result<Page<ExamQuestion>> pageQuestions(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Integer questionType,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        // 学科权限过滤: 有任教记录的教师只能查看任教科目题目(跨学科不可见,同科共享)
        List<Long> teachSubjectIds = systemService.listTeacherSubjects(teacherId)
                .stream().map(s -> s.getId()).collect(java.util.stream.Collectors.toList());
        if (!teachSubjectIds.isEmpty()) {
            if (subjectId == null) {
                return Result.success(examService.pageQuestionsInSubjects(current, size, teachSubjectIds, questionType, difficulty, keyword));
            }
            if (!teachSubjectIds.contains(subjectId)) {
                return Result.error(ResultCode.FORBIDDEN.getCode(), "只能查看您任教学科的题目");
            }
        }
        return Result.success(examService.pageQuestions(current, size, subjectId, questionType, difficulty, keyword));
    }

    @GetMapping("/question/{id}")
    public Result<ExamQuestion> getQuestion(@PathVariable Long id) {
        return Result.success(examService.getQuestion(id));
    }

    @OperationLog(module = "题库管理", operation = "新增题目")
    @PostMapping("/question")
    public Result<Void> addQuestion(@RequestBody ExamQuestion q) {
        contentRiskService.audit(questionText(q), "新增题目");
        examService.addQuestion(q);
        return Result.success();
    }

    @OperationLog(module = "题库管理", operation = "编辑题目")
    @PutMapping("/question")
    public Result<Void> updateQuestion(@RequestBody ExamQuestion q) {
        contentRiskService.audit(questionText(q), "编辑题目");
        examService.updateQuestion(q);
        return Result.success();
    }

    @OperationLog(module = "题库管理", operation = "删除题目")
    @DeleteMapping("/question/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        examService.deleteQuestion(id);
        return Result.success();
    }

    /** 一键清空题库: 仅删除教师任教学科的题目,返回删除数量 */
    @OperationLog(module = "题库管理", operation = "一键清空题库")
    @DeleteMapping("/questions/all")
    public Result<Map<String, Object>> deleteAllQuestions() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        List<Long> subjectIds = systemService.listTeacherSubjects(teacherId)
                .stream().map(s -> s.getId()).collect(java.util.stream.Collectors.toList());
        int deleted = examService.deleteAllQuestions(subjectIds);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("deleted", deleted);
        return Result.success(result);
    }

    /** AI 智能组卷: 提交异步任务立即返回 taskId,由前端轮询状态接口获取结果 */
    @OperationLog(module = "AI组卷", operation = "提交AI智能组卷任务")
    @PostMapping("/questions/ai-group")
    public Result<Map<String, Object>> aiGroup(@RequestBody Map<String, Object> body) {
        if (body.get("subjectId") == null) {
            return Result.error(ResultCode.PARAM_ERROR.getCode(), "请选择学科");
        }
        Long teacherId = SecurityUtils.getCurrentUserId();
        // 学科权限校验: 有任教记录的教师只能按任教学科出题
        Long reqSubjectId = Long.valueOf(body.get("subjectId").toString());
        List<Long> teachSubjectIds = systemService.listTeacherSubjects(teacherId)
                .stream().map(s -> s.getId()).collect(java.util.stream.Collectors.toList());
        if (!teachSubjectIds.isEmpty() && !teachSubjectIds.contains(reqSubjectId)) {
            return Result.error(ResultCode.FORBIDDEN.getCode(), "只能为您任教的学科生成题目");
        }
        // 配置字段: subjectId/totalQuestions/questionTypes/typeCounts/easyRatio/mediumRatio/hardRatio/totalScore/knowledgePoints
        String taskId = aiGroupTaskService.submit(body, teacherId);
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("taskId", taskId);
        return Result.success(result);
    }

    /** AI 智能组卷: 查询任务状态(RUNNING/DONE/FAILED)与结果 */
    @GetMapping("/questions/ai-group/status/{taskId}")
    public Result<Map<String, Object>> aiGroupStatus(@PathVariable String taskId) {
        AiGroupTaskService.AiGroupTask task = aiGroupTaskService.get(taskId);
        if (task == null) {
            return Result.error(ResultCode.NOT_FOUND.getCode(), "任务不存在或已过期");
        }
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("status", task.getStatus());
        result.put("questionIds", task.getQuestionIds());
        result.put("source", task.getSource());
        result.put("count", task.getCount());
        result.put("message", task.getMessage());
        return Result.success(result);
    }

    /** 出题记录列表: 教师历次AI出题,同一次出题的题目通过记录归组 */
    @GetMapping("/ai-group/records")
    public Result<List<Map<String, Object>>> aiGroupRecords() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        List<Map<String, Object>> result = new java.util.ArrayList<>();
        for (com.zhixue.ai.module.exam.entity.AiGenerateRecord r : aiGroupTaskService.listRecords(teacherId)) {
            Map<String, Object> item = new java.util.HashMap<>();
            item.put("id", r.getId());
            item.put("subjectId", r.getSubjectId());
            item.put("subjectName", r.getSubjectId() != null && subjectMapper.selectById(r.getSubjectId()) != null
                    ? subjectMapper.selectById(r.getSubjectId()).getSubjectName() : "未知学科");
            item.put("source", r.getSource());
            item.put("count", r.getQuestionCount());
            item.put("createTime", r.getCreateTime());
            result.add(item);
        }
        return Result.success(result);
    }

    /** 出题记录详情: 查看某次出题生成的全部题目 */
    @GetMapping("/ai-group/records/{id}/questions")
    public Result<List<ExamQuestion>> aiGroupRecordQuestions(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(aiGroupTaskService.getRecordQuestions(id, teacherId));
    }

    /** 删除出题记录(仅删记录,不影响已生成的题目) */
    @OperationLog(module = "AI组卷", operation = "删除出题记录")
    @DeleteMapping("/ai-group/records/{id}")
    public Result<Void> deleteAiGroupRecord(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        aiGroupTaskService.deleteRecord(id, teacherId);
        return Result.success();
    }

    /** AI 智能组卷: 获取学科知识点列表(教师出题选知识板块用) */
    @GetMapping("/knowledge-points")
    public Result<List<String>> knowledgePoints(@RequestParam(required = false) Long subjectId) {
        if (subjectId == null) {
            return Result.success(java.util.Collections.emptyList());
        }
        return Result.success(selfPracticeService.getKnowledgePointsBySubject(subjectId));
    }

    // ============== 试卷管理 ==============

    @GetMapping("/papers")
    public Result<Page<ExamPaper>> pagePapers(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Integer paperType,
            @RequestParam(required = false) Long classId) {
        return Result.success(examService.pagePapers(current, size, SecurityUtils.getCurrentUserId(), classId, paperType));
    }

    @GetMapping("/paper/{id}")
    public Result<Map<String, Object>> paperDetail(@PathVariable Long id) {
        return Result.success(examService.getPaperDetailForTeacher(id));
    }

    @OperationLog(module = "试卷管理", operation = "创建试卷")
    @PostMapping("/paper")
    public Result<Long> createPaper(@RequestBody Map<String, Object> body) {
        com.alibaba.fastjson2.JSON.parseObject(com.alibaba.fastjson2.JSON.toJSONString(body.get("paper")));
        ExamPaper paper = com.alibaba.fastjson2.JSON.parseObject(
                com.alibaba.fastjson2.JSON.toJSONString(body.get("paper")), ExamPaper.class);
        contentRiskService.audit(paperText(paper), "创建试卷");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");
        return Result.success(examService.createPaper(paper, questions));
    }

    @OperationLog(module = "试卷管理", operation = "编辑试卷")
    @PutMapping("/paper")
    public Result<Void> updatePaper(@RequestBody Map<String, Object> body) {
        ExamPaper paper = com.alibaba.fastjson2.JSON.parseObject(
                com.alibaba.fastjson2.JSON.toJSONString(body.get("paper")), ExamPaper.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");
        contentRiskService.audit(paperText(paper), "编辑试卷");
        examService.updatePaper(paper, questions);
        return Result.success();
    }

    @OperationLog(module = "试卷管理", operation = "删除试卷")
    @DeleteMapping("/paper/{id}")
    public Result<Void> deletePaper(@PathVariable Long id) {
        examService.deletePaper(id);
        return Result.success();
    }

    @OperationLog(module = "试卷管理", operation = "发布试卷")
    @PutMapping("/paper/{id}/publish")
    public Result<Void> publishPaper(@PathVariable Long id) {
        examService.publishPaper(id);
        return Result.success();
    }

    @OperationLog(module = "试卷管理", operation = "结束考试")
    @PutMapping("/paper/{id}/finish")
    public Result<Void> finishPaper(@PathVariable Long id) {
        examService.finishPaper(id);
        return Result.success();
    }

    // ============== 批改管理 ==============

    /** 试卷作答列表 */
    @GetMapping("/correct/{paperId}/answers")
    public Result<?> paperAnswers(@PathVariable Long paperId) {
        return Result.success(examService.listPaperAnswers(paperId));
    }

    /** 批改详情 */
    @GetMapping("/correct/{answerId}")
    public Result<?> correctDetail(@PathVariable Long answerId) {
        return Result.success(aiService.getCorrectDetails(answerId));
    }

    /** 试卷维度:整套试卷每题的全班作答情况(选项分布+逐人明细) */
    @GetMapping("/correct/{paperId}/class-answers")
    public Result<?> paperClassAnswers(@PathVariable Long paperId) {
        return Result.success(aiService.getPaperClassAnswers(paperId));
    }

    /** 题目维度:该班级所有学生对某题的作答情况 */
    @GetMapping("/correct/question/{questionId}/class-answers")
    public Result<?> questionClassAnswers(@PathVariable Long questionId,
                                          @RequestParam Long classId) {
        return Result.success(aiService.getQuestionClassAnswers(questionId, classId));
    }

    /** 批量 AI 批改 */
    @OperationLog(module = "批改管理", operation = "批量AI批改")
    @PostMapping("/correct/{paperId}/batch")
    public Result<?> batchCorrect(@PathVariable Long paperId) {
        return Result.success(aiService.batchCorrectPaper(paperId));
    }

    /** 手动微调批改 */
    @OperationLog(module = "批改管理", operation = "手动微调批改分数")
    @PutMapping("/correct/{correctId}/adjust")
    public Result<Void> adjustCorrect(@PathVariable Long correctId,
                                       @RequestBody Map<String, Object> body) {
        java.math.BigDecimal score = new java.math.BigDecimal(body.get("score").toString());
        String remark = (String) body.get("remark");
        aiService.adjustCorrect(correctId, score, remark);
        return Result.success();
    }

    /** 触发答案雷同查重 */
    @OperationLog(module = "题库管理", operation = "答案雷同查重")
    @PostMapping("/correct/{paperId}/similarity")
    public Result<?> similarityCheck(@PathVariable Long paperId) {
        int total = 0;
        for (var a : examService.listPaperAnswers(paperId)) {
            total += aiService.similarityCheck(a.getId());
        }
        return Result.success(java.util.Map.of("similarCount", total));
    }

    // ============== 班级学情 ==============

    @GetMapping("/class/{classId}/analysis")
    public Result<?> classAnalysis(@PathVariable Long classId,
                                   @RequestParam(required = false) Long subjectId) {
        return Result.success(aiService.getClassAnalysis(classId, subjectId));
    }

    // ============== 家校反馈 ==============

    @GetMapping("/class/{classId}/students")
    public Result<?> classStudents(@PathVariable Long classId) {
        return Result.success(systemService.listClassStudents(classId));
    }

    @GetMapping("/feedback/{studentId}")
    public Result<String> feedback(@PathVariable Long studentId) {
        return Result.success(aiService.generateComment(studentId));
    }

    /**
     * 将 AI 学情反馈推送到企业微信群(群机器人 Webhook)
     * @param studentId 学生ID
     * @param body {webhookUrl: 企业微信群机器人 Webhook 地址}
     */
    @OperationLog(module = "家校反馈", operation = "推送学情反馈到企业微信")
    @PostMapping("/feedback/{studentId}/push")
    public Result<?> pushFeedback(@PathVariable Long studentId, @RequestBody Map<String, String> body) {
        String webhookUrl = body == null ? null : body.get("webhookUrl");
        if (webhookUrl == null || !webhookUrl.startsWith("https://qyapi.weixin.qq.com/cgi-bin/webhook/send?key=")) {
            return Result.error("Webhook 地址无效:请填写企业微信群机器人的 Webhook 地址");
        }
        String feedback = aiService.generateComment(studentId);
        if (feedback == null || feedback.isBlank()) {
            return Result.error("AI 反馈生成失败,无法推送");
        }
        // 内容风控:对外推送前检测反馈内容,违规自动拦截并记录
        contentRiskService.audit(feedback, "家校反馈");
        // 企业微信群机器人 markdown 消息,限制 4096 字节
        String content = "**【智汇云课堂·学情反馈】**\n" + feedback;
        byte[] max4096 = content.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        if (max4096.length > 4096) {
            content = new String(max4096, 0, 4090, java.nio.charset.StandardCharsets.UTF_8) + "...";
        }
        cn.hutool.json.JSONObject msg = cn.hutool.json.JSONUtil.createObj()
                .set("msgtype", "markdown")
                .set("markdown", cn.hutool.json.JSONUtil.createObj().set("content", content));
        String resp = cn.hutool.http.HttpUtil.post(webhookUrl, msg.toString(), 10000);
        cn.hutool.json.JSONObject respObj;
        try {
            respObj = cn.hutool.json.JSONUtil.parseObj(resp);
        } catch (Exception e) {
            return Result.error("企业微信响应异常,请稍后重试");
        }
        if (respObj.getInt("errcode", -1) == 0) {
            return Result.success("推送成功,请到企业微信群查看");
        }
        return Result.error("推送失败:企业微信返回 " + respObj.getStr("errmsg", "未知错误"));
    }

    /** 拼接题目全部文本字段,供内容风控检测 */
    private String questionText(ExamQuestion q) {
        return String.join(" ",
                q.getContent() == null ? "" : q.getContent(),
                q.getOptions() == null ? "" : q.getOptions(),
                q.getStandardAnswer() == null ? "" : q.getStandardAnswer(),
                q.getAnalysis() == null ? "" : q.getAnalysis(),
                q.getKnowledgePoint() == null ? "" : q.getKnowledgePoint());
    }

    /** 拼接试卷标题与说明,供内容风控检测 */
    private String paperText(ExamPaper paper) {
        return String.join(" ",
                paper.getPaperName() == null ? "" : paper.getPaperName(),
                paper.getDescription() == null ? "" : paper.getDescription());
    }

    // ============== 班级自主学习概况 ==============

    @GetMapping("/class/self-study/stats")
    public Result<List<Map<String, Object>>> classSelfStudyStats(@RequestParam Long classId) {
        return Result.success(selfPracticeService.getClassSelfStudyStats(classId));
    }

    // ============== 学员管理 ==============

    /** 教师所教班级列表 */
    @GetMapping("/my-classes")
    public Result<?> myClasses() {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(systemService.listTeacherClasses(teacherId));
    }

    /** 分页查询教师所教班级的学生 */
    @GetMapping("/students")
    public Result<?> pageStudents(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long classId) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        return Result.success(systemService.pageTeacherStudents(teacherId, current, size, keyword, classId));
    }

    /** 教师添加学生 */
    @OperationLog(module = "学员管理", operation = "教师添加学生")
    @PostMapping("/student")
    public Result<Void> addStudent(@RequestBody com.zhixue.ai.module.system.entity.SysUser student) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        systemService.addStudentByTeacher(teacherId, student);
        return Result.success();
    }

    /** 教师编辑学生 */
    @OperationLog(module = "学员管理", operation = "教师编辑学生")
    @PutMapping("/student")
    public Result<Void> updateStudent(@RequestBody com.zhixue.ai.module.system.entity.SysUser student) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        systemService.updateStudentByTeacher(teacherId, student);
        return Result.success();
    }

    /** 教师删除学生 */
    @OperationLog(module = "学员管理", operation = "教师删除学生")
    @DeleteMapping("/student/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        systemService.deleteStudentByTeacher(teacherId, id);
        return Result.success();
    }

    /** 教师重置学生密码 */
    @OperationLog(module = "学员管理", operation = "教师重置学生密码")
    @PutMapping("/student/{id}/reset-password")
    public Result<Void> resetStudentPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        systemService.resetStudentPasswordByTeacher(teacherId, id, body.get("password"));
        return Result.success();
    }
}
