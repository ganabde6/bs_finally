package com.zhixue.ai.module.ai.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixue.ai.common.result.Result;
import com.zhixue.ai.module.ai.service.AiService;
import com.zhixue.ai.module.exam.service.ExamService;
import com.zhixue.ai.module.system.entity.SysClass;
import com.zhixue.ai.module.system.entity.SysSubject;
import com.zhixue.ai.module.system.entity.SysTeacherClass;
import com.zhixue.ai.module.system.entity.SysUser;
import com.zhixue.ai.module.system.mapper.SysClassMapper;
import com.zhixue.ai.module.system.mapper.SysSubjectMapper;
import com.zhixue.ai.module.system.mapper.SysTeacherClassMapper;
import com.zhixue.ai.module.system.mapper.SysUserMapper;
import com.zhixue.ai.module.system.service.SystemService;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 学生端 Controller
 */
@RestController
@RequestMapping("/api/student")
@RequiredArgsConstructor
public class StudentController {

    private final ExamService examService;
    private final AiService aiService;
    private final SysUserMapper userMapper;
    private final SysClassMapper classMapper;
    private final SysTeacherClassMapper teacherClassMapper;
    private final SysSubjectMapper subjectMapper;
    private final SystemService systemService;

    /** 当前学生可见的公告(全员公告 + 面向学生的已发布公告) */
    @GetMapping("/notices")
    public Result<?> notices() {
        return Result.success(systemService.listVisibleNotices("STUDENT"));
    }

    /** 个人信息:学生基本信息 + 所在班级 + 本班全部任教老师(姓名/电话/科目) */
    @GetMapping("/profile")
    public Result<Map<String, Object>> profile() {
        Long studentId = SecurityUtils.getCurrentUserId();
        Long classId = SecurityUtils.getCurrentClassId();
        SysUser me = userMapper.selectById(studentId);
        Map<String, Object> result = new java.util.HashMap<>();
        if (me != null) {
            result.put("username", me.getUsername());
            result.put("realName", me.getRealName());
            result.put("phone", me.getPhone());
            result.put("email", me.getEmail());
            result.put("avatar", me.getAvatar());
            result.put("gradeLevel", me.getGradeLevel());
        }
        if (classId != null) {
            SysClass cls = classMapper.selectById(classId);
            if (cls != null) result.put("className", cls.getClassName());
        }
        // 本班全部任教老师: 执教关系 -> 老师 + 科目
        List<Map<String, Object>> teachers = new java.util.ArrayList<>();
        List<SysTeacherClass> tcs = teacherClassMapper.selectList(
                new LambdaQueryWrapper<SysTeacherClass>().eq(SysTeacherClass::getClassId, classId));
        for (SysTeacherClass tc : tcs) {
            SysUser teacher = userMapper.selectById(tc.getTeacherId());
            if (teacher == null || teacher.getDeleted() == 1) continue;
            Map<String, Object> t = new java.util.HashMap<>();
            t.put("teacherName", teacher.getRealName());
            t.put("phone", teacher.getPhone());
            t.put("email", teacher.getEmail());
            SysSubject subject = subjectMapper.selectById(tc.getSubjectId());
            t.put("subjectName", subject != null ? subject.getSubjectName() : "");
            teachers.add(t);
        }
        result.put("teachers", teachers);
        return Result.success(result);
    }

    /** 学习首页统计 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        Long studentId = SecurityUtils.getCurrentUserId();
        Long classId = SecurityUtils.getCurrentClassId();
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("paperCount", examService.listStudentPapers(classId, studentId).size());
        result.put("answerCount", examService.listStudentAnswers(studentId).size());
        result.put("errorCount", aiService.listErrorBooks(studentId).size());
        return Result.success(result);
    }

    /** 查询本人班级已发布试卷(排除已提交的) */
    @GetMapping("/papers")
    public Result<?> papers() {
        Long studentId = SecurityUtils.getCurrentUserId();
        Long classId = SecurityUtils.getCurrentClassId();
        return Result.success(examService.listStudentPapers(classId, studentId));
    }

    /** 试卷详情(含题目,不含答案) */
    @GetMapping("/paper/{paperId}")
    public Result<Map<String, Object>> paperDetail(@PathVariable Long paperId) {
        return Result.success(examService.getPaperDetail(paperId));
    }

    /** 开始/继续作答 */
    @PostMapping("/answer/start/{paperId}")
    public Result<?> startAnswer(@PathVariable Long paperId) {
        return Result.success(examService.startAnswer(paperId, SecurityUtils.getCurrentUserId()));
    }

    /** 提交作答 */
    @PostMapping("/answer/{answerId}/submit")
    public Result<?> submitAnswer(@PathVariable Long answerId,
                                  @RequestBody Map<String, Object> body) {
        // JSON 反序列化出来的 map key 是 String,必须显式转成 Long,
        // 否则批改时用 Long 型 questionId 取值永远 miss,学生答案会被当成空
        Map<Long, String> answers = new java.util.HashMap<>();
        @SuppressWarnings("unchecked")
        Map<String, Object> raw = (Map<String, Object>) body.get("answers");
        if (raw != null) {
            raw.forEach((k, v) -> answers.put(Long.valueOf(k), v == null ? "" : v.toString()));
        }
        Integer duration = (Integer) body.get("duration");
        examService.submitAnswer(answerId, duration, answers);
        // 自动触发 AI 批改
        return Result.success(aiService.correctAnswer(answerId, answers));
    }

    /** 我的作答记录(可按科目、时间范围筛选,默认按时间倒序) */
    @GetMapping("/answers")
    public Result<?> myAnswers(@RequestParam(required = false) Long subjectId,
                               @RequestParam(required = false) String startDate,
                               @RequestParam(required = false) String endDate) {
        return Result.success(examService.listStudentAnswersWithSubject(
                SecurityUtils.getCurrentUserId(), subjectId, startDate, endDate));
    }

    /** 我的作答记录涉及的科目列表(动态来自作答记录,再按学生学段 gradeLevel 过滤补充) */
    @GetMapping("/answer/subjects")
    public Result<List<Map<String, Object>>> myAnswerSubjects() {
        Long studentId = SecurityUtils.getCurrentUserId();
        // 已有作答记录的科目(动态)
        java.util.List<Map<String, Object>> subjects =
                new java.util.ArrayList<>(examService.listStudentAnswerSubjects(studentId));
        java.util.Set<Long> have = new java.util.HashSet<>();
        for (Map<String, Object> s : subjects) {
            Object id = s.get("subjectId");
            if (id != null) have.add(Long.valueOf(id.toString()));
        }
        // 补充:学生学段 gradeLevel 下可用但还没有作答记录的科目,便于筛选下拉展示完整
        SysUser me = userMapper.selectById(studentId);
        Integer gradeLevel = me != null ? me.getGradeLevel() : null;
        LambdaQueryWrapper<SysSubject> qw = new LambdaQueryWrapper<>();
        if (gradeLevel != null && gradeLevel > 0) {
            qw.and(w -> w.eq(SysSubject::getGradeLevel, gradeLevel).or().eq(SysSubject::getGradeLevel, 0));
        }
        qw.orderByAsc(SysSubject::getSort);
        for (SysSubject sub : subjectMapper.selectList(qw)) {
            if (have.add(sub.getId())) {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("subjectId", sub.getId());
                m.put("subjectName", sub.getSubjectName());
                m.put("noRecord", true); // 标记:该学段科目但暂无作答记录
                subjects.add(m);
            }
        }
        return Result.success(subjects);
    }

    /** 作答批改详情 */
    @GetMapping("/answer/{answerId}/correct")
    public Result<?> correctDetail(@PathVariable Long answerId) {
        return Result.success(aiService.getCorrectDetails(answerId));
    }

    /** 删除我的作答记录(级联删除批改记录) */
    @DeleteMapping("/answer/{answerId}")
    public Result<Void> deleteAnswer(@PathVariable Long answerId) {
        examService.deleteStudentAnswer(answerId, SecurityUtils.getCurrentUserId());
        return Result.success();
    }

    /** 错题本 */
    @GetMapping("/errorbooks")
    public Result<?> errorBooks() {
        return Result.success(aiService.listErrorBooks(SecurityUtils.getCurrentUserId()));
    }

    /** 标记错题已复盘 */
    @PutMapping("/errorbook/{id}/review")
    public Result<Void> reviewError(@PathVariable Long id, @RequestParam Integer status) {
        aiService.reviewErrorBook(id, status);
        return Result.success();
    }

    /** 删除错题 */
    @DeleteMapping("/errorbook/{id}")
    public Result<Void> deleteErrorBook(@PathVariable Long id) {
        aiService.deleteErrorBook(id, SecurityUtils.getCurrentUserId());
        return Result.success();
    }

    /** 推送变式题(支持指定数量) */
    @PostMapping("/errorbook/{id}/variant")
    public Result<?> pushVariant(@PathVariable Long id, @RequestParam(defaultValue = "1") Integer count) {
        return Result.success(aiService.pushVariant(id, count));
    }

    /** 变式题作答批改 */
    @PostMapping("/variant/{id}/answer")
    public Result<?> answerVariant(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String answer = (String) body.get("answer");
        @SuppressWarnings("unchecked")
        java.util.List<String> images = (java.util.List<String>) body.get("images");
        return Result.success(aiService.submitVariantAnswer(id, SecurityUtils.getCurrentUserId(), answer, images));
    }

    /** 我的变式题 */
    @GetMapping("/variants")
    public Result<?> myVariants() {
        return Result.success(aiService.listVariants(SecurityUtils.getCurrentUserId()));
    }

    /** 删除变式题 */
    @DeleteMapping("/variant/{id}")
    public Result<Void> deleteVariant(@PathVariable Long id) {
        aiService.deleteVariant(id, SecurityUtils.getCurrentUserId());
        return Result.success();
    }

    /** 个人学情分析 */
    @GetMapping("/study/analysis")
    public Result<?> studyAnalysis(@RequestParam(required = false) Long subjectId) {
        return Result.success(aiService.getStudentAnalysis(SecurityUtils.getCurrentUserId(), subjectId));
    }

    /** AI 助学问答 */
    @PostMapping("/tutor/chat")
    public Result<?> tutorChat(@RequestBody Map<String, Object> body) {
        String question = (String) body.get("question");
        Integer chatType = (Integer) body.get("chatType");
        String imageBase64 = (String) body.get("imageBase64");
        return Result.success(aiService.tutorChat(SecurityUtils.getCurrentUserId(), question, chatType, imageBase64));
    }

    /** AI 作文润色 */
    @PostMapping("/tutor/polish")
    public Result<?> polish(@RequestBody Map<String, String> body) {
        String content = body.get("content");
        String imageBase64 = body.get("imageBase64");
        return Result.success(aiService.polishEssay(content, imageBase64));
    }

    /** AI 历史对话 */
    @GetMapping("/tutor/history")
    public Result<?> chatHistory() {
        return Result.success(aiService.listChatHistory(SecurityUtils.getCurrentUserId()));
    }

    /** 上报风控事件(切屏/离开等) */
    @PostMapping("/risk/report")
    public Result<?> reportRisk(@RequestBody Map<String, Object> body) {
        Long answerId = Long.valueOf(body.get("answerId").toString());
        Integer riskType = (Integer) body.get("riskType");
        String desc = (String) body.get("description");
        return Result.success(aiService.reportRisk(answerId, SecurityUtils.getCurrentUserId(), riskType, desc));
    }
}
