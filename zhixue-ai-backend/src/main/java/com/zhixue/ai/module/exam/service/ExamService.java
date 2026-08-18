package com.zhixue.ai.module.exam.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhixue.ai.common.constant.SystemConstants;
import com.zhixue.ai.common.exception.BizException;
import com.zhixue.ai.common.result.ResultCode;
import com.zhixue.ai.module.exam.entity.*;
import com.zhixue.ai.module.exam.mapper.*;
import com.zhixue.ai.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 题库/试卷/作答 服务
 */
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamQuestionMapper questionMapper;
    private final ExamPaperMapper paperMapper;
    private final ExamPaperQuestionMapper paperQuestionMapper;
    private final ExamAnswerMapper answerMapper;

    // ===================== 题库 =====================

    public Page<ExamQuestion> pageQuestions(Long current, Long size, Long subjectId,
                                            Integer questionType, String keyword) {
        return questionMapper.selectPage(new Page<>(current == null ? 1 : current, size == null ? 10 : size),
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(subjectId != null, ExamQuestion::getSubjectId, subjectId)
                        .eq(questionType != null, ExamQuestion::getQuestionType, questionType)
                        .and(StringUtils.hasText(keyword),
                                w -> w.like(ExamQuestion::getContent, keyword)
                                        .or().like(ExamQuestion::getKnowledgePoint, keyword))
                        .orderByDesc(ExamQuestion::getCreateTime));
    }

    public List<ExamQuestion> listQuestions(Long subjectId, Integer questionType) {
        return questionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(subjectId != null, ExamQuestion::getSubjectId, subjectId)
                .eq(questionType != null, ExamQuestion::getQuestionType, questionType)
                .orderByDesc(ExamQuestion::getCreateTime));
    }

    public ExamQuestion getQuestion(Long id) {
        return questionMapper.selectById(id);
    }

    public void addQuestion(ExamQuestion q) {
        q.setCreatorId(SecurityUtils.getCurrentUserId());
        if (q.getDifficulty() == null) q.setDifficulty(3);
        if (q.getFullScore() == null) q.setFullScore(BigDecimal.valueOf(5));
        questionMapper.insert(q);
    }

    public void updateQuestion(ExamQuestion q) {
        questionMapper.updateById(q);
    }

    public void deleteQuestion(Long id) {
        questionMapper.deleteById(id);
    }

    /** AI 智能组卷(简化版:按学科+难度从题库随机抽题) */
    @Transactional
    public List<Long> aiGroupPaper(Long subjectId, Integer totalQuestions, Integer difficulty) {
        List<ExamQuestion> all = questionMapper.selectList(new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getSubjectId, subjectId)
                .ne(ExamQuestion::getQuestionType, SystemConstants.Q_TYPE_JUDGE)
                .eq(difficulty != null, ExamQuestion::getDifficulty, difficulty));
        if (all.size() <= totalQuestions) return all.stream().map(ExamQuestion::getId).collect(Collectors.toList());
        // 随机抽题
        Collections.shuffle(all);
        return all.subList(0, totalQuestions).stream().map(ExamQuestion::getId).collect(Collectors.toList());
    }

    // ===================== 试卷 =====================

    public Page<ExamPaper> pagePapers(Long current, Long size, Long creatorId, Long classId, Integer paperType) {
        return paperMapper.selectPage(new Page<>(current == null ? 1 : current, size == null ? 10 : size),
                new LambdaQueryWrapper<ExamPaper>()
                        .eq(creatorId != null, ExamPaper::getCreatorId, creatorId)
                        .eq(classId != null, ExamPaper::getClassId, classId)
                        .eq(paperType != null, ExamPaper::getPaperType, paperType)
                        .orderByDesc(ExamPaper::getCreateTime));
    }

    /** 学生端:查询本班已发布试卷(排除已提交的) */
    public List<ExamPaper> listStudentPapers(Long classId) {
        return listStudentPapers(classId, null);
    }

    /** 学生端:查询本班已发布试卷(排除已提交的) */
    public List<ExamPaper> listStudentPapers(Long classId, Long studentId) {
        List<ExamPaper> papers = paperMapper.selectList(new LambdaQueryWrapper<ExamPaper>()
                .eq(ExamPaper::getClassId, classId)
                .eq(ExamPaper::getStatus, SystemConstants.PAPER_STATUS_PUBLISHED)
                .orderByDesc(ExamPaper::getPublishTime));
        // 排除学生已提交的试卷
        if (studentId != null && !papers.isEmpty()) {
            List<ExamAnswer> submitted = answerMapper.selectList(
                    new LambdaQueryWrapper<ExamAnswer>()
                            .eq(ExamAnswer::getStudentId, studentId)
                            .ge(ExamAnswer::getStatus, SystemConstants.ANSWER_STATUS_SUBMITTED));
            if (!submitted.isEmpty()) {
                Set<Long> submittedPaperIds = submitted.stream()
                        .map(ExamAnswer::getPaperId)
                        .collect(Collectors.toSet());
                papers = papers.stream()
                        .filter(p -> !submittedPaperIds.contains(p.getId()))
                        .collect(Collectors.toList());
            }
        }
        return papers;
    }

    public ExamPaper getPaper(Long id) {
        return paperMapper.selectById(id);
    }

    /** 试卷详情(含题目列表) */
    public Map<String, Object> getPaperDetail(Long paperId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new BizException(ResultCode.PAPER_NOT_FOUND);
        List<ExamPaperQuestion> pqList = paperQuestionMapper.selectByPaperId(paperId);
        List<Map<String, Object>> questions = new ArrayList<>();
        for (ExamPaperQuestion pq : pqList) {
            ExamQuestion q = questionMapper.selectById(pq.getQuestionId());
            Map<String, Object> m = new HashMap<>();
            m.put("id", q.getId());
            m.put("questionType", q.getQuestionType());
            m.put("difficulty", q.getDifficulty());
            m.put("knowledgePoint", q.getKnowledgePoint());
            m.put("content", q.getContent());
            m.put("options", q.getOptions());
            m.put("score", pq.getScore());
            m.put("sort", pq.getSort());
            // 学生端不返回 standardAnswer/scorePoint/analysis
            questions.add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("paper", paper);
        result.put("questions", questions);
        return result;
    }

    /** 教师端:试卷详情(含答案) */
    public Map<String, Object> getPaperDetailForTeacher(Long paperId) {
        ExamPaper paper = paperMapper.selectById(paperId);
        if (paper == null) throw new BizException(ResultCode.PAPER_NOT_FOUND);
        List<ExamPaperQuestion> pqList = paperQuestionMapper.selectByPaperId(paperId);
        List<Map<String, Object>> questions = new ArrayList<>();
        for (ExamPaperQuestion pq : pqList) {
            ExamQuestion q = questionMapper.selectById(pq.getQuestionId());
            Map<String, Object> m = new HashMap<>();
            m.put("pqId", pq.getId());
            m.put("question", q);
            m.put("score", pq.getScore());
            m.put("sort", pq.getSort());
            questions.add(m);
        }
        Map<String, Object> result = new HashMap<>();
        result.put("paper", paper);
        result.put("questions", questions);
        return result;
    }

    @Transactional
    public Long createPaper(ExamPaper paper, List<Map<String, Object>> questions) {
        paper.setCreatorId(SecurityUtils.getCurrentUserId());
        if (paper.getStatus() == null) paper.setStatus(SystemConstants.PAPER_STATUS_DRAFT);
        if (paper.getStatus() == SystemConstants.PAPER_STATUS_PUBLISHED) {
            paper.setPublishTime(LocalDateTime.now());
        }
        // 计算总分
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> q : questions) {
            Object s = q.get("score");
            if (s != null) total = total.add(new BigDecimal(s.toString()));
        }
        paper.setTotalScore(total);
        paperMapper.insert(paper);
        // 保存题目关联
        int sort = 1;
        for (Map<String, Object> q : questions) {
            ExamPaperQuestion pq = new ExamPaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(Long.valueOf(q.get("questionId").toString()));
            pq.setScore(new BigDecimal(q.get("score").toString()));
            pq.setSort(sort++);
            paperQuestionMapper.insert(pq);
        }
        return paper.getId();
    }

    @Transactional
    public void updatePaper(ExamPaper paper, List<Map<String, Object>> questions) {
        if (paper.getStatus() != null && paper.getStatus() == SystemConstants.PAPER_STATUS_PUBLISHED) {
            paper.setPublishTime(LocalDateTime.now());
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Map<String, Object> q : questions) {
            Object s = q.get("score");
            if (s != null) total = total.add(new BigDecimal(s.toString()));
        }
        paper.setTotalScore(total);
        paperMapper.updateById(paper);
        // 重建题目关联
        paperQuestionMapper.deleteByPaperId(paper.getId());
        int sort = 1;
        for (Map<String, Object> q : questions) {
            ExamPaperQuestion pq = new ExamPaperQuestion();
            pq.setPaperId(paper.getId());
            pq.setQuestionId(Long.valueOf(q.get("questionId").toString()));
            pq.setScore(new BigDecimal(q.get("score").toString()));
            pq.setSort(sort++);
            paperQuestionMapper.insert(pq);
        }
    }

    public void deletePaper(Long id) {
        paperMapper.deleteById(id);
        paperQuestionMapper.deleteByPaperId(id);
    }

    /** 发布试卷 */
    public void publishPaper(Long id) {
        ExamPaper p = new ExamPaper();
        p.setId(id);
        p.setStatus(SystemConstants.PAPER_STATUS_PUBLISHED);
        p.setPublishTime(LocalDateTime.now());
        paperMapper.updateById(p);
    }

    /** 结束试卷 */
    public void finishPaper(Long id) {
        ExamPaper p = new ExamPaper();
        p.setId(id);
        p.setStatus(SystemConstants.PAPER_STATUS_FINISHED);
        paperMapper.updateById(p);
    }

    // ===================== 作答 =====================

    /** 学生开始作答(创建作答记录) */
    public ExamAnswer startAnswer(Long paperId, Long studentId) {
        ExamAnswer exist = answerMapper.selectOne(new LambdaQueryWrapper<ExamAnswer>()
                .eq(ExamAnswer::getPaperId, paperId)
                .eq(ExamAnswer::getStudentId, studentId));
        if (exist != null && exist.getStatus() >= SystemConstants.ANSWER_STATUS_SUBMITTED) {
            throw new BizException(ResultCode.ANSWER_ALREADY_SUBMIT);
        }
        if (exist != null) {
            return exist; // 继续作答
        }
        ExamAnswer answer = new ExamAnswer();
        answer.setPaperId(paperId);
        answer.setStudentId(studentId);
        answer.setSubmitType(1);
        answer.setStatus(SystemConstants.ANSWER_STATUS_NOT_SUBMIT);
        answer.setStartTime(LocalDateTime.now());
        answerMapper.insert(answer);
        return answer;
    }

    /** 提交作答(整体提交) */
    @Transactional
    public ExamAnswer submitAnswer(Long answerId, Integer duration, Map<Long, String> answers) {
        ExamAnswer answer = answerMapper.selectById(answerId);
        if (answer == null) throw new BizException("作答记录不存在");
        if (answer.getStatus() >= SystemConstants.ANSWER_STATUS_SUBMITTED) {
            throw new BizException(ResultCode.ANSWER_ALREADY_SUBMIT);
        }
        answer.setDuration(duration);
        answer.setStatus(SystemConstants.ANSWER_STATUS_SUBMITTED);
        answer.setSubmitTime(LocalDateTime.now());
        answerMapper.updateById(answer);
        return answer;
    }

    public ExamAnswer getAnswer(Long id) {
        return answerMapper.selectById(id);
    }

    /** 学生作答记录列表 */
    public List<ExamAnswer> listStudentAnswers(Long studentId) {
        return answerMapper.selectByStudentId(studentId);
    }

    /** 试卷作答列表(教师端) */
    public List<ExamAnswer> listPaperAnswers(Long paperId) {
        return answerMapper.selectByPaperId(paperId);
    }
}
