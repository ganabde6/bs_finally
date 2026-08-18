package com.zhixue.ai.module.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixue.ai.common.constant.SystemConstants;
import com.zhixue.ai.common.exception.BizException;
import com.zhixue.ai.module.ai.engine.AiServiceProvider;
import com.zhixue.ai.module.ai.entity.AiErrorBook;
import com.zhixue.ai.module.ai.entity.SelfPracticeRecord;
import com.zhixue.ai.module.ai.entity.UserCheckIn;
import com.zhixue.ai.module.ai.mapper.AiErrorBookMapper;
import com.zhixue.ai.module.ai.mapper.SelfPracticeRecordMapper;
import com.zhixue.ai.module.ai.mapper.UserCheckInMapper;
import com.zhixue.ai.module.ai.service.SelfPracticeService;
import com.zhixue.ai.module.exam.entity.ExamQuestion;
import com.zhixue.ai.module.exam.mapper.ExamQuestionMapper;
import com.zhixue.ai.module.system.entity.SysSubject;
import com.zhixue.ai.module.system.entity.SysUser;
import com.zhixue.ai.module.system.mapper.SysSubjectMapper;
import com.zhixue.ai.module.system.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 自主智练与自律打卡服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SelfPracticeServiceImpl implements SelfPracticeService {

    private final SelfPracticeRecordMapper practiceRecordMapper;
    private final UserCheckInMapper checkInMapper;
    private final AiErrorBookMapper errorBookMapper;
    private final ExamQuestionMapper questionMapper;
    private final SysUserMapper userMapper;
    private final SysSubjectMapper subjectMapper;
    private final AiServiceProvider aiServiceProvider;

    // ===================== 智能生成练习 =====================

    @Override
    public List<Map<String, Object>> generatePractice(Long userId) {
        // 1. 统计学生错题本中频率最高的前3个薄弱知识点
        List<AiErrorBook> errors = errorBookMapper.selectList(
                new LambdaQueryWrapper<AiErrorBook>()
                        .eq(AiErrorBook::getStudentId, userId)
                        .isNotNull(AiErrorBook::getKnowledgePoint));

        List<String> topKnowledgePoints = new ArrayList<>();
        if (!errors.isEmpty()) {
            Map<String, Long> kpCount = errors.stream()
                    .map(AiErrorBook::getKnowledgePoint)
                    .collect(Collectors.groupingBy(k -> k, Collectors.counting()));
            topKnowledgePoints = kpCount.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(3)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
        }

        // 2. 按知识点匹配选择题(1)和判断题(3)
        List<ExamQuestion> candidates = new ArrayList<>();
        if (!topKnowledgePoints.isEmpty()) {
            for (String kp : topKnowledgePoints) {
                List<ExamQuestion> qs = questionMapper.selectList(
                        new LambdaQueryWrapper<ExamQuestion>()
                                .like(ExamQuestion::getKnowledgePoint, kp)
                                .in(ExamQuestion::getQuestionType, SystemConstants.Q_TYPE_SINGLE, SystemConstants.Q_TYPE_JUDGE)
                                .eq(ExamQuestion::getDeleted, 0));
                candidates.addAll(qs);
            }
            // 去重
            candidates = candidates.stream()
                    .collect(Collectors.collectingAndThen(
                            Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ExamQuestion::getId))),
                            ArrayList::new));
        }

        // 3. 如果错题不足，从题库随机补充中等难度题
        Random random = new Random();
        List<ExamQuestion> selected = new ArrayList<>();
        if (candidates.size() >= 5) {
            Collections.shuffle(candidates, random);
            selected = candidates.subList(0, 5);
        } else {
            selected.addAll(candidates);
            // 补充中等难度基础题
            List<ExamQuestion> extra = questionMapper.selectList(
                    new LambdaQueryWrapper<ExamQuestion>()
                            .in(ExamQuestion::getQuestionType, SystemConstants.Q_TYPE_SINGLE, SystemConstants.Q_TYPE_JUDGE)
                            .eq(ExamQuestion::getDifficulty, 2)
                            .eq(ExamQuestion::getDeleted, 0));
            extra.removeAll(selected);
            Collections.shuffle(extra, random);
            int need = 5 - selected.size();
            for (int i = 0; i < need && i < extra.size(); i++) {
                selected.add(extra.get(i));
            }
        }

        // 4. 如果仍然不足5道，从题库任意抽
        if (selected.size() < 5) {
            List<ExamQuestion> all = questionMapper.selectList(
                    new LambdaQueryWrapper<ExamQuestion>()
                            .in(ExamQuestion::getQuestionType, SystemConstants.Q_TYPE_SINGLE, SystemConstants.Q_TYPE_JUDGE)
                            .eq(ExamQuestion::getDeleted, 0));
            all.removeAll(selected);
            Collections.shuffle(all, random);
            int need = 5 - selected.size();
            for (int i = 0; i < need && i < all.size(); i++) {
                selected.add(all.get(i));
            }
        }

        // 5. 组装返回格式
        List<Map<String, Object>> result = new ArrayList<>();
        for (ExamQuestion q : selected) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("stem", q.getContent());
            item.put("type", q.getQuestionType());
            // 解析选项JSON
            List<String> opts = parseOptions(q.getOptions());
            item.put("options", opts);
            item.put("correctAnswer", q.getStandardAnswer());
            result.add(item);
        }
        return result;
    }

    // ===================== 提交批改 =====================

    @Override
    @Transactional
    public List<Map<String, Object>> submitPractice(Long userId, List<Map<String, Object>> questionAnswers, Integer durationSeconds) {
        List<Map<String, Object>> details = new ArrayList<>();
        int correctCount = 0;
        List<Map<String, Object>> snapshots = new ArrayList<>();

        for (Map<String, Object> qa : questionAnswers) {
            Long questionId = Long.valueOf(qa.get("questionId").toString());
            String userAnswer = qa.get("userAnswer") != null ? qa.get("userAnswer").toString() : "";

            ExamQuestion q = questionMapper.selectById(questionId);
            if (q == null) continue;

            boolean isCorrect = q.getStandardAnswer() != null && q.getStandardAnswer().trim().equals(userAnswer.trim());
            if (isCorrect) correctCount++;

            // 批改明细
            Map<String, Object> detail = new LinkedHashMap<>();
            detail.put("questionId", questionId);
            detail.put("correct", isCorrect);
            detail.put("correctAnswer", q.getStandardAnswer());
            detail.put("userAnswer", userAnswer);
            details.add(detail);

            // 题目快照
            Map<String, Object> snapshot = new LinkedHashMap<>();
            snapshot.put("id", q.getId());
            snapshot.put("stem", q.getContent());
            snapshot.put("type", q.getQuestionType());
            snapshot.put("options", parseOptions(q.getOptions()));
            snapshot.put("correctAnswer", q.getStandardAnswer());
            snapshots.add(snapshot);
        }

        int totalCount = details.size();
        BigDecimal accuracy = totalCount > 0
                ? BigDecimal.valueOf(correctCount * 100.0 / totalCount).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // 判断生成来源
        List<AiErrorBook> errors = errorBookMapper.selectList(
                new LambdaQueryWrapper<AiErrorBook>().eq(AiErrorBook::getStudentId, userId));
        String source = errors.isEmpty() ? "系统推荐" : "错题生成";

        // 保存到 self_practice_record
        SelfPracticeRecord record = new SelfPracticeRecord();
        record.setUserId(userId);
        record.setQuestionSnapshot(JSON.toJSONString(snapshots));
        record.setTotalCount(totalCount);
        record.setCorrectCount(correctCount);
        record.setAccuracy(accuracy);
        record.setDurationSeconds(durationSeconds != null ? durationSeconds : 0);
        record.setGenerateSource(source);
        practiceRecordMapper.insert(record);

        return details;
    }

    // ===================== 打卡 =====================

    @Override
    @Transactional
    public Map<String, Object> doCheckIn(Long userId) {
        LocalDate today = LocalDate.now();

        // 第一步: 检查今日是否已打卡
        Long existId = checkInMapper.selectTodayCheckIn(userId, today);
        if (existId != null) {
            throw new BizException("今日已打卡");
        }

        // 第二步: 检查今日是否有正确率>=30%的练习记录
        int effectiveCount = practiceRecordMapper.countEffectivePracticeToday(userId, today);
        if (effectiveCount == 0) {
            throw new BizException("今天还没有有效练习，先去做题吧");
        }

        // 第三步: 计算连续打卡天数
        LocalDate lastDate = checkInMapper.selectLatestCheckInDate(userId);
        int continuousDays;
        if (lastDate == null) {
            continuousDays = 1;
        } else if (lastDate.equals(today.minusDays(1))) {
            continuousDays = getLastCheckIn(userId).getContinuousDays() + 1;
        } else if (lastDate.isBefore(today.minusDays(1))) {
            continuousDays = 1;
        } else {
            throw new BizException("今日已有打卡记录");
        }

        // 第四步: 计算积分和勋章
        int totalPoints = 10;
        UserCheckIn lastCheckIn = getLastCheckIn(userId);
        if (lastCheckIn != null) {
            totalPoints = lastCheckIn.getTotalPoints() + 10;
        }

        String badge = null;
        if (continuousDays >= 15) {
            badge = "学霸金牌";
        } else if (continuousDays >= 7) {
            badge = "自律银牌";
        } else if (continuousDays >= 3) {
            badge = "勤奋铜牌";
        }

        // 保存打卡记录
        UserCheckIn checkIn = new UserCheckIn();
        checkIn.setUserId(userId);
        checkIn.setCheckInDate(today);
        checkIn.setContinuousDays(continuousDays);
        checkIn.setTotalPoints(totalPoints);
        checkIn.setRewardBadge(badge);
        checkInMapper.insert(checkIn);

        // 返回结果
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("continuousDays", continuousDays);
        result.put("totalPoints", totalPoints);
        result.put("rewardBadge", badge);
        result.put("message", "打卡成功！积分+10");
        return result;
    }

    // ===================== 查询打卡状态 =====================

    @Override
    public Map<String, Object> getCheckInStatus(Long userId) {
        LocalDate today = LocalDate.now();
        Long todayId = checkInMapper.selectTodayCheckIn(userId, today);
        boolean isCheckedInToday = todayId != null;

        UserCheckIn latest = getLastCheckIn(userId);
        int continuousDays = latest != null ? latest.getContinuousDays() : 0;
        int totalPoints = latest != null ? latest.getTotalPoints() : 0;

        // 收集所有勋章
        List<UserCheckIn> allCheckIns = checkInMapper.selectList(
                new LambdaQueryWrapper<UserCheckIn>()
                        .eq(UserCheckIn::getUserId, userId)
                        .isNotNull(UserCheckIn::getRewardBadge));
        Set<String> badgeSet = new LinkedHashSet<>();
        for (UserCheckIn ci : allCheckIns) {
            if (ci.getRewardBadge() != null) {
                badgeSet.add(ci.getRewardBadge());
            }
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("isCheckedInToday", isCheckedInToday);
        result.put("continuousDays", continuousDays);
        result.put("totalPoints", totalPoints);
        result.put("badges", new ArrayList<>(badgeSet));
        return result;
    }

    // ===================== 班级自主学习概况 =====================

    @Override
    public List<Map<String, Object>> getClassSelfStudyStats(Long classId) {
        List<SysUser> students = userMapper.selectByClassId(classId);
        LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
        LocalDate sevenDaysAgoDate = LocalDate.now().minusDays(7);
        LocalDate threeDaysAgo = LocalDate.now().minusDays(3);

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser student : students) {
            Long sid = student.getId();

            // 近7天练习次数
            int practiceCount = practiceRecordMapper.countRecentPractices(sid, sevenDaysAgo);

            // 近7天打卡天数
            int checkInDays = checkInMapper.countRecentCheckIns(sid, sevenDaysAgoDate);

            // 当前连续打卡天数
            UserCheckIn latest = getLastCheckIn(sid);
            int continuousDays = latest != null ? latest.getContinuousDays() : 0;

            // 最近一次练习日期
            LocalDateTime lastPractice = practiceRecordMapper.selectLatestPracticeTime(sid);

            // 最近一次打卡日期
            LocalDate lastCheckInDate = checkInMapper.selectLatestCheckInDate(sid);

            // 判断是否待督促: 连续3天既没有练习也没有打卡
            boolean isInactive = false;
            if (lastPractice == null && lastCheckInDate == null) {
                isInactive = true;
            } else {
                LocalDate lastActive = lastCheckInDate;
                if (lastPractice != null) {
                    LocalDate lastPracticeDate = lastPractice.toLocalDate();
                    if (lastActive == null || lastPracticeDate.isAfter(lastActive)) {
                        lastActive = lastPracticeDate;
                    }
                }
                if (lastActive != null && lastActive.isBefore(threeDaysAgo)) {
                    isInactive = true;
                }
            }

            Map<String, Object> item = new LinkedHashMap<>();
            item.put("userId", sid);
            item.put("username", student.getUsername());
            item.put("realName", student.getRealName());
            item.put("practiceCount", practiceCount);
            item.put("checkInDays", checkInDays);
            item.put("continuousDays", continuousDays);
            item.put("lastPracticeTime", lastPractice);
            item.put("isInactive", isInactive);
            result.add(item);
        }
        return result;
    }

    // ===================== AI 智能组卷（参数化配置） =====================

    @Override
    public Map<String, Object> generatePracticeByConfig(Long userId, Map<String, Object> config) {
        Integer mode = config.get("mode") != null ? Integer.valueOf(config.get("mode").toString()) : 1;
        Long subjectId = config.get("subjectId") != null ? Long.valueOf(config.get("subjectId").toString()) : null;

        if (subjectId == null) {
            throw new BizException("请选择学科");
        }

        List<ExamQuestion> selected = new ArrayList<>();

        // 优先由 AI 大模型按学科+知识点+题型+难度生成新题
        try {
            selected = generateByAi(config, subjectId, userId);
        } catch (Exception e) {
            log.warn("AI 组卷生成失败，回退题库抽题: {}", e.getMessage());
        }
        // AI 生成失败或题量不足时，回退到题库抽题
        Integer questionCount = config.get("questionCount") != null
                ? Integer.valueOf(config.get("questionCount").toString()) : 10;
        if (selected.size() < questionCount) {
            List<ExamQuestion> fallback;
            if (mode == 1) {
                // 模式一：专项板块定向练习
                fallback = generateByMode1(config, subjectId, userId);
            } else {
                // 模式二：考纲大数据智能套卷
                fallback = generateByMode2(config, subjectId);
            }
            // 去重：排除 AI 已生成的题目
            Set<Long> aiIds = selected.stream().map(ExamQuestion::getId).collect(Collectors.toSet());
            for (ExamQuestion q : fallback) {
                if (!aiIds.contains(q.getId()) && selected.size() < questionCount) {
                    selected.add(q);
                    aiIds.add(q.getId());
                }
            }
        }

        // 组装返回格式
        List<Map<String, Object>> questions = new ArrayList<>();
        for (ExamQuestion q : selected) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", q.getId());
            item.put("stem", q.getContent());
            item.put("type", q.getQuestionType());
            item.put("difficulty", q.getDifficulty());
            item.put("analysis", q.getAnalysis());
            List<String> opts = parseOptions(q.getOptions());
            item.put("options", opts);
            item.put("correctAnswer", q.getStandardAnswer());
            questions.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("questions", questions);
        result.put("totalCount", questions.size());
        return result;
    }

    /**
     * AI 大模型组卷：按学科+知识点+题型+难度生成全新题目并入库
     * <p>AI 返回 JSON 数组：[{"content","options","standardAnswer","analysis","questionType","difficulty","knowledgePoint"}]</p>
     * @return 生成的题目列表；AI 不可用或解析失败返回空列表（由调用方回退题库）
     */
    private List<ExamQuestion> generateByAi(Map<String, Object> config, Long subjectId, Long userId) {
        @SuppressWarnings("unchecked")
        List<String> knowledgePoints = config.get("knowledgePoints") != null
                ? (List<String>) config.get("knowledgePoints") : Collections.emptyList();
        Integer questionCount = config.get("questionCount") != null
                ? Integer.valueOf(config.get("questionCount").toString()) : 10;
        Integer difficulty = config.get("difficulty") != null
                ? Integer.valueOf(config.get("difficulty").toString()) : 0;
        @SuppressWarnings("unchecked")
        List<Integer> questionTypes = config.get("questionTypes") != null
                ? ((List<?>) config.get("questionTypes")).stream()
                    .map(t -> Integer.valueOf(t.toString())).collect(Collectors.toList())
                : Arrays.asList(1, 2, 4, 5, 7);

        // 学科名称（prompt 需要）
        String subjectName = "通用学科";
        SysSubject subject = subjectMapper.selectById(subjectId);
        if (subject != null && subject.getSubjectName() != null) {
            subjectName = subject.getSubjectName();
        }

        List<ExamQuestion> result = new ArrayList<>();
        // 库内已有题目内容（归一化后），用于入库前去重
        Set<String> existingContents = questionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getSubjectId, subjectId)
                        .eq(ExamQuestion::getDeleted, 0))
                .stream()
                .map(ExamQuestion::getContent)
                .filter(c -> c != null && !c.trim().isEmpty())
                .map(SelfPracticeServiceImpl::normalizeContent)
                .collect(Collectors.toSet());
        // 本批次已接受的题目内容（批次内去重）
        Set<String> batchContents = new HashSet<>();

        // 循环调用 AI 直到凑够题目数量：单次生成不超过 5 道，避免一次生成过多导致 AI 响应超时
        int maxRounds = (questionCount + 4) / 5 + 1;
        for (int round = 0; round < maxRounds && result.size() < questionCount; round++) {
            int stillNeed = Math.min(questionCount - result.size(), 5);
            // 查询该学科已出过的题目内容（最新的优先），提示 AI 不要重复出题
            List<String> existingQuestions = questionMapper.selectList(
                    new LambdaQueryWrapper<ExamQuestion>()
                            .eq(ExamQuestion::getSubjectId, subjectId)
                            .eq(ExamQuestion::getDeleted, 0)
                            .orderByDesc(ExamQuestion::getId))
                    .stream()
                    .map(ExamQuestion::getContent)
                    .filter(c -> c != null && !c.trim().isEmpty())
                    .limit(50)
                    .collect(Collectors.toList());

            String json = aiServiceProvider.generateExamQuestions(subjectName, knowledgePoints, questionTypes,
                    difficulty, stillNeed, existingQuestions);
            if (json == null || json.trim().isEmpty()) {
                log.warn("AI 组卷第 {} 轮未返回内容", round + 1);
                break;
            }

            try {
                // 提取 JSON 数组（AI 可能包裹 ```json 或夹杂说明文字）
                String clean = json.trim();
                int start = clean.indexOf('[');
                int end = clean.lastIndexOf(']');
                if (start < 0 || end <= start) {
                    log.warn("AI 组卷第 {} 轮返回格式异常，缺少 JSON 数组: {}", round + 1, truncate(json, 200));
                    continue;
                }
                JSONArray arr = JSON.parseArray(clean.substring(start, end + 1));
                if (arr == null || arr.isEmpty()) {
                    log.warn("AI 组卷第 {} 轮返回空数组", round + 1);
                    continue;
                }

                for (int i = 0; i < arr.size() && result.size() < questionCount; i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    if (obj == null) continue;
                    ExamQuestion q = new ExamQuestion();
                    q.setSubjectId(subjectId);
                    q.setQuestionType(obj.getIntValue("questionType"));
                    q.setDifficulty(obj.getIntValue("difficulty"));
                    q.setKnowledgePoint(obj.getString("knowledgePoint"));
                    q.setContent(obj.getString("content"));
                    JSONArray opts = obj.getJSONArray("options");
                    q.setOptions(opts != null ? opts.toJSONString() : null);
                    q.setStandardAnswer(obj.getString("standardAnswer"));
                    q.setAnalysis(obj.getString("analysis"));
                    q.setFullScore(BigDecimal.valueOf(5));
                    q.setCreatorId(userId);
                    q.setDeleted(0);
                    if (q.getContent() == null || q.getContent().trim().isEmpty()
                            || q.getStandardAnswer() == null || q.getStandardAnswer().trim().isEmpty()) {
                        log.warn("AI 组卷第 {} 轮第 {} 题缺少题干/答案，跳过", round + 1, i + 1);
                        continue;
                    }
                    // 去重：与库内已有题目、本批次已生成题目比对，重复则跳过
                    String norm = normalizeContent(q.getContent());
                    if (existingContents.contains(norm) || batchContents.contains(norm)) {
                        log.warn("AI 组卷第 {} 轮第 {} 题与已有题目重复，跳过: {}", round + 1, i + 1, truncate(q.getContent(), 60));
                        continue;
                    }
                    existingContents.add(norm);
                    batchContents.add(norm);
                    questionMapper.insert(q);
                    result.add(q);
                }
            } catch (Exception e) {
                log.error("AI 组卷第 {} 轮 JSON 解析失败: {}", round + 1, truncate(json, 300), e);
            }
        }
        return result;
    }

    /**
     * 归一化题干内容用于去重比较：去掉空白、标点、大小写差异
     */
    private static String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        return content.replaceAll("[\\s\\p{Punct}，。、；：！？（）【】《》“”‘’·—…\\[\\]{}]", "").toLowerCase();
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    /**
     * 模式一：专项板块定向练习
     */
    private List<ExamQuestion> generateByMode1(Map<String, Object> config, Long subjectId, Long userId) {
        @SuppressWarnings("unchecked")
        List<String> knowledgePoints = config.get("knowledgePoints") != null
                ? (List<String>) config.get("knowledgePoints") : Collections.emptyList();
        Integer questionCount = config.get("questionCount") != null
                ? Integer.valueOf(config.get("questionCount").toString()) : 10;
        Integer difficulty = config.get("difficulty") != null
                ? Integer.valueOf(config.get("difficulty").toString()) : 0;
        @SuppressWarnings("unchecked")
        List<Integer> questionTypes = config.get("questionTypes") != null
                ? ((List<?>) config.get("questionTypes")).stream()
                    .map(t -> Integer.valueOf(t.toString())).collect(Collectors.toList())
                : Arrays.asList(1, 3);
        boolean priorityErrors = config.get("priorityErrors") != null
                && Boolean.parseBoolean(config.get("priorityErrors").toString());

        Random random = new Random();
        List<ExamQuestion> selected = new ArrayList<>();

        // 错题优先推送：先从错题本的知识点中抽题
        if (priorityErrors) {
            List<AiErrorBook> errors = errorBookMapper.selectList(
                    new LambdaQueryWrapper<AiErrorBook>()
                            .eq(AiErrorBook::getStudentId, userId)
                            .isNotNull(AiErrorBook::getKnowledgePoint));
            if (!errors.isEmpty()) {
                Set<String> errorKps = errors.stream()
                        .map(AiErrorBook::getKnowledgePoint)
                        .collect(Collectors.toSet());
                List<ExamQuestion> errorCandidates = new ArrayList<>();
                for (String kp : errorKps) {
                    LambdaQueryWrapper<ExamQuestion> wrapper = new LambdaQueryWrapper<ExamQuestion>()
                            .eq(ExamQuestion::getSubjectId, subjectId)
                            .like(ExamQuestion::getKnowledgePoint, kp)
                            .in(ExamQuestion::getQuestionType, questionTypes)
                            .eq(ExamQuestion::getDeleted, 0);
                    if (difficulty != null && difficulty > 0) {
                        if (difficulty >= 3) {
                            // 拔高:匹配难度>=3(含难度4、5的真难题)
                            wrapper.ge(ExamQuestion::getDifficulty, 3);
                        } else {
                            wrapper.eq(ExamQuestion::getDifficulty, difficulty);
                        }
                    }
                    errorCandidates.addAll(questionMapper.selectList(wrapper));
                }
                // 去重
                errorCandidates = errorCandidates.stream()
                        .collect(Collectors.collectingAndThen(
                                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ExamQuestion::getId))),
                                ArrayList::new));
                Collections.shuffle(errorCandidates, random);
                int errorTake = Math.min(questionCount, errorCandidates.size());
                selected.addAll(errorCandidates.subList(0, errorTake));
            }
        }

        // 如果题量不足，从常规题库补充
        if (selected.size() < questionCount) {
            Set<Long> selectedIds = selected.stream().map(ExamQuestion::getId).collect(Collectors.toSet());
            int stillNeed = questionCount - selected.size();

            // 先按知识点匹配
            if (!knowledgePoints.isEmpty()) {
                List<ExamQuestion> kpCandidates = new ArrayList<>();
                for (String kp : knowledgePoints) {
                    LambdaQueryWrapper<ExamQuestion> wrapper = new LambdaQueryWrapper<ExamQuestion>()
                            .eq(ExamQuestion::getSubjectId, subjectId)
                            .like(ExamQuestion::getKnowledgePoint, kp)
                            .in(ExamQuestion::getQuestionType, questionTypes)
                            .eq(ExamQuestion::getDeleted, 0);
                    if (difficulty != null && difficulty > 0) {
                        if (difficulty >= 3) {
                            // 拔高:匹配难度>=3(含难度4、5的真难题)
                            wrapper.ge(ExamQuestion::getDifficulty, 3);
                        } else {
                            wrapper.eq(ExamQuestion::getDifficulty, difficulty);
                        }
                    }
                    kpCandidates.addAll(questionMapper.selectList(wrapper));
                }
                kpCandidates = kpCandidates.stream()
                        .filter(q -> !selectedIds.contains(q.getId()))
                        .collect(Collectors.collectingAndThen(
                                Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ExamQuestion::getId))),
                                ArrayList::new));
                Collections.shuffle(kpCandidates, random);
                int take = Math.min(stillNeed, kpCandidates.size());
                for (int i = 0; i < take; i++) {
                    selected.add(kpCandidates.get(i));
                    selectedIds.add(kpCandidates.get(i).getId());
                }
                stillNeed = questionCount - selected.size();
            }

            // 如果知识点题量仍不足，从整个学科题库随机补充
            if (stillNeed > 0) {
                LambdaQueryWrapper<ExamQuestion> wrapper = new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getSubjectId, subjectId)
                        .in(ExamQuestion::getQuestionType, questionTypes)
                        .eq(ExamQuestion::getDeleted, 0);
                if (difficulty != null && difficulty > 0) {
                    if (difficulty >= 3) {
                        // 拔高:匹配难度>=3(含难度4、5的真难题)
                        wrapper.ge(ExamQuestion::getDifficulty, 3);
                    } else {
                        wrapper.eq(ExamQuestion::getDifficulty, difficulty);
                    }
                }
                List<ExamQuestion> allCandidates = questionMapper.selectList(wrapper).stream()
                        .filter(q -> !selectedIds.contains(q.getId()))
                        .collect(Collectors.toList());
                Collections.shuffle(allCandidates, random);
                int take = Math.min(stillNeed, allCandidates.size());
                for (int i = 0; i < take; i++) {
                    selected.add(allCandidates.get(i));
                }
            }
        }

        return selected;
    }

    /**
     * 模式二：考纲大数据智能套卷
     */
    private List<ExamQuestion> generateByMode2(Map<String, Object> config, Long subjectId) {
        Integer questionCount = config.get("questionCount") != null
                ? Integer.valueOf(config.get("questionCount").toString()) : 20;
        Integer easyRatio = config.get("easyRatio") != null
                ? Integer.valueOf(config.get("easyRatio").toString()) : 60;
        Integer mediumRatio = config.get("mediumRatio") != null
                ? Integer.valueOf(config.get("mediumRatio").toString()) : 30;
        // 拔高题占比 = 100 - 基础 - 中档
        int hardRatio = 100 - easyRatio - mediumRatio;
        if (hardRatio < 0) hardRatio = 0;

        // 各难度题目数量
        int easyCount = Math.round(questionCount * easyRatio / 100f);
        int mediumCount = Math.round(questionCount * mediumRatio / 100f);
        int hardCount = questionCount - easyCount - mediumCount;
        if (hardCount < 0) hardCount = 0;

        Random random = new Random();
        List<ExamQuestion> selected = new ArrayList<>();

        // 按难度分层抽题
        selected.addAll(drawByDifficulty(subjectId, 1, easyCount, random));
        selected.addAll(drawByDifficulty(subjectId, 2, mediumCount, random));
        if (hardCount > 0) {
            selected.addAll(drawByDifficulty(subjectId, 3, hardCount, random));
        }

        // 去重
        selected = selected.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ExamQuestion::getId))),
                        ArrayList::new));

        return selected;
    }

    /**
     * 按难度从题库中随机抽取题目
     */
    private List<ExamQuestion> drawByDifficulty(Long subjectId, int difficulty, int count, Random random) {
        if (count <= 0) return Collections.emptyList();
        LambdaQueryWrapper<ExamQuestion> wrapper = new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getSubjectId, subjectId)
                .eq(ExamQuestion::getDeleted, 0);
        if (difficulty >= 3) {
            // 拔高:匹配难度>=3(含难度4、5的真难题)
            wrapper.ge(ExamQuestion::getDifficulty, 3);
        } else {
            wrapper.eq(ExamQuestion::getDifficulty, difficulty);
        }
        List<ExamQuestion> pool = questionMapper.selectList(wrapper);
        Collections.shuffle(pool, random);
        int actual = Math.min(count, pool.size());
        return pool.subList(0, actual);
    }

    @Override
    public List<String> getKnowledgePointsBySubject(Long subjectId) {
        List<ExamQuestion> questions = questionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getSubjectId, subjectId)
                        .isNotNull(ExamQuestion::getKnowledgePoint)
                        .eq(ExamQuestion::getDeleted, 0));
        // 提取所有知识点，按逗号/顿号分割后去重
        Set<String> kpSet = new LinkedHashSet<>();
        for (ExamQuestion q : questions) {
            String kp = q.getKnowledgePoint();
            if (kp != null && !kp.trim().isEmpty()) {
                // 支持逗号、顿号、分号分隔的多个知识点
                String[] parts = kp.split("[,，、;；]");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        kpSet.add(trimmed);
                    }
                }
            }
        }
        return new ArrayList<>(kpSet);
    }

    @Override
    public List<Map<String, Object>> getRecentPracticeRecords(Long userId) {
        List<SelfPracticeRecord> records = practiceRecordMapper.selectList(
                new LambdaQueryWrapper<SelfPracticeRecord>()
                        .eq(SelfPracticeRecord::getUserId, userId)
                        .orderByDesc(SelfPracticeRecord::getCreateTime)
                        .last("LIMIT 10"));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SelfPracticeRecord r : records) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("id", r.getId());
            item.put("totalCount", r.getTotalCount());
            item.put("correctCount", r.getCorrectCount());
            item.put("accuracy", r.getAccuracy());
            item.put("durationSeconds", r.getDurationSeconds());
            item.put("generateSource", r.getGenerateSource());
            item.put("createTime", r.getCreateTime());
            result.add(item);
        }
        return result;
    }

    // ===================== 工具方法 =====================

    private UserCheckIn getLastCheckIn(Long userId) {
        return checkInMapper.selectLatestCheckIn(userId);
    }

    /**
     * 解析选项JSON字符串为列表
     */
    private List<String> parseOptions(String optionsJson) {
        if (optionsJson == null || optionsJson.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return JSON.parseArray(optionsJson, String.class);
        } catch (Exception e) {
            // 如果不是JSON数组格式，尝试按换行符分割
            String[] parts = optionsJson.split("\\n");
            return Arrays.asList(parts);
        }
    }
}
