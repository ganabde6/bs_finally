package com.zhixue.ai.module.exam.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.common.result.ResultCode;
import com.zhixue.ai.module.ai.service.AiService;
import com.zhixue.ai.module.ai.service.SelfPracticeService;
import com.zhixue.ai.module.exam.entity.ExamPaper;
import com.zhixue.ai.module.exam.entity.ExamQuestion;
import com.zhixue.ai.module.exam.service.AiGroupTaskService;
import com.zhixue.ai.module.exam.service.ExamService;
import com.zhixue.ai.module.system.service.SystemService;
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
    private final SelfPracticeService selfPracticeService;
    private final SystemService systemService;
    private final AiGroupTaskService aiGroupTaskService;
    private final com.zhixue.ai.module.exam.service.TeacherDashboardService teacherDashboardService;
    private final com.zhixue.ai.module.system.mapper.SysUserMapper userMapper;
    private final com.zhixue.ai.module.system.mapper.SysClassMapper classMapper;
    private final com.zhixue.ai.module.system.mapper.SysTeacherClassMapper teacherClassMapper;
    private final com.zhixue.ai.module.system.mapper.SysSubjectMapper subjectMapper;

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

    @PostMapping("/question")
    public Result<Void> addQuestion(@RequestBody ExamQuestion q) {
        examService.addQuestion(q);
        return Result.success();
    }

    @PutMapping("/question")
    public Result<Void> updateQuestion(@RequestBody ExamQuestion q) {
        examService.updateQuestion(q);
        return Result.success();
    }

    @DeleteMapping("/question/{id}")
    public Result<Void> deleteQuestion(@PathVariable Long id) {
        examService.deleteQuestion(id);
        return Result.success();
    }

    /** AI 智能组卷: 提交异步任务立即返回 taskId,由前端轮询状态接口获取结果 */
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

    @PostMapping("/paper")
    public Result<Long> createPaper(@RequestBody Map<String, Object> body) {
        com.alibaba.fastjson2.JSON.parseObject(com.alibaba.fastjson2.JSON.toJSONString(body.get("paper")));
        ExamPaper paper = com.alibaba.fastjson2.JSON.parseObject(
                com.alibaba.fastjson2.JSON.toJSONString(body.get("paper")), ExamPaper.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");
        return Result.success(examService.createPaper(paper, questions));
    }

    @PutMapping("/paper")
    public Result<Void> updatePaper(@RequestBody Map<String, Object> body) {
        ExamPaper paper = com.alibaba.fastjson2.JSON.parseObject(
                com.alibaba.fastjson2.JSON.toJSONString(body.get("paper")), ExamPaper.class);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> questions = (List<Map<String, Object>>) body.get("questions");
        examService.updatePaper(paper, questions);
        return Result.success();
    }

    @DeleteMapping("/paper/{id}")
    public Result<Void> deletePaper(@PathVariable Long id) {
        examService.deletePaper(id);
        return Result.success();
    }

    @PutMapping("/paper/{id}/publish")
    public Result<Void> publishPaper(@PathVariable Long id) {
        examService.publishPaper(id);
        return Result.success();
    }

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

    /** 批量 AI 批改 */
    @PostMapping("/correct/{paperId}/batch")
    public Result<?> batchCorrect(@PathVariable Long paperId) {
        return Result.success(aiService.batchCorrectPaper(paperId));
    }

    /** 手动微调批改 */
    @PutMapping("/correct/{correctId}/adjust")
    public Result<Void> adjustCorrect(@PathVariable Long correctId,
                                       @RequestBody Map<String, Object> body) {
        java.math.BigDecimal score = new java.math.BigDecimal(body.get("score").toString());
        String remark = (String) body.get("remark");
        aiService.adjustCorrect(correctId, score, remark);
        return Result.success();
    }

    /** 触发答案雷同查重 */
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
    @PostMapping("/student")
    public Result<Void> addStudent(@RequestBody com.zhixue.ai.module.system.entity.SysUser student) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        systemService.addStudentByTeacher(teacherId, student);
        return Result.success();
    }

    /** 教师编辑学生 */
    @PutMapping("/student")
    public Result<Void> updateStudent(@RequestBody com.zhixue.ai.module.system.entity.SysUser student) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        systemService.updateStudentByTeacher(teacherId, student);
        return Result.success();
    }

    /** 教师删除学生 */
    @DeleteMapping("/student/{id}")
    public Result<Void> deleteStudent(@PathVariable Long id) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        systemService.deleteStudentByTeacher(teacherId, id);
        return Result.success();
    }

    /** 教师重置学生密码 */
    @PutMapping("/student/{id}/reset-password")
    public Result<Void> resetStudentPassword(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Long teacherId = SecurityUtils.getCurrentUserId();
        systemService.resetStudentPasswordByTeacher(teacherId, id, body.get("password"));
        return Result.success();
    }
}
