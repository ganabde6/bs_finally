package com.zhixue.ai.module.ai.service.impl;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
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
    private final com.zhixue.ai.module.exam.mapper.AiGenerateRecordMapper generateRecordMapper;
    private final SysUserMapper userMapper;
    private final SysSubjectMapper subjectMapper;
    private final AiServiceProvider aiServiceProvider;

    // ===================== 智能生成练习 =====================

    /**
     * 教师 AI 现场出题：按学科+难度生成全新题目并入库。
     * 复用 {@link #generateByAi} 的 AI 生成/去重/归一化逻辑。
     */
    @Override
    public List<ExamQuestion> generatePaperForTeacher(Map<String, Object> config, Long creatorId) {
        if (config.get("subjectId") == null) {
            throw new BizException("请选择学科");
        }
        Long subjectId = Long.valueOf(config.get("subjectId").toString());
        int questionCount = config.get("totalQuestions") != null
                ? Integer.valueOf(config.get("totalQuestions").toString()) : 10;
        List<Integer> types = parseTeacherTypeList(config.get("questionTypes"));
        @SuppressWarnings("unchecked")
        List<String> knowledgePoints = config.get("knowledgePoints") != null
                ? (List<String>) config.get("knowledgePoints") : Collections.emptyList();

        // 各题型数量分布(题型 -> 数量)
        Map<Integer, Integer> typeCounts = parseTypeCounts(config.get("typeCounts"), types, questionCount);
        // 难度占比
        int easyRatio = config.get("easyRatio") != null ? Integer.valueOf(config.get("easyRatio").toString()) : 40;
        int mediumRatio = config.get("mediumRatio") != null ? Integer.valueOf(config.get("mediumRatio").toString()) : 40;
        int hardRatio = 100 - easyRatio - mediumRatio;
        if (hardRatio < 0) hardRatio = 0;
        // 总分 -> 每题分值
        BigDecimal totalScore = config.get("totalScore") != null
                ? new BigDecimal(config.get("totalScore").toString()) : BigDecimal.valueOf(100);
        int totalCount = typeCounts.values().stream().mapToInt(Integer::intValue).sum();
        BigDecimal perScore = totalCount > 0
                ? totalScore.divide(BigDecimal.valueOf(totalCount), 1, RoundingMode.HALF_UP) : BigDecimal.ZERO;
        String ratioDesc = "基础" + easyRatio + "% 中档" + mediumRatio + "% 拔高" + hardRatio + "%";

        List<ExamQuestion> result = new ArrayList<>();
        Set<Long> pickedIds = new HashSet<>();
        Random random = new Random();

        // 1. 单次 AI 调用现场出题(全部题型一起出,避免每题型一次串行大模型调用导致耗时成倍增长)
        try {
            Map<String, Object> cfg = new HashMap<>();
            cfg.put("questionCount", questionCount);
            cfg.put("difficulty", 0);
            cfg.put("knowledgePoints", knowledgePoints);
            cfg.put("questionTypes", types);
            cfg.put("difficultyRatio", ratioDesc);
            result = generateByAi(cfg, subjectId, creatorId);
        } catch (Exception ex) {
            log.warn("教师组卷 AI 出题失败: {}", ex.getMessage());
            result = new ArrayList<>();
        }
        result.forEach(q -> pickedIds.add(q.getId()));

        // 近期出题记录已用过的题库题也纳入排除,让多次出题尽量不重复(仅影响题库补齐,AI 现场生成的题本身已去重)
        Set<Long> recentUsedIds = loadRecentUsedQuestionIds(creatorId);

        // 2. 按题型分布用题库补齐(AI 可能题型分布不准或题量不足)
        for (Map.Entry<Integer, Integer> e : typeCounts.entrySet()) {
            int type = e.getKey();
            int count = e.getValue();
            if (count <= 0) continue;
            long have = result.stream().filter(q -> q.getQuestionType() == type).count();
            int need = count - (int) have;
            if (need <= 0) continue;
            Set<Long> excl = new HashSet<>(pickedIds);
            excl.addAll(recentUsedIds);
            List<ExamQuestion> bank = drawByTypeAndDifficulty(
                    subjectId, type, need, easyRatio, mediumRatio, hardRatio, random, excl);
            for (ExamQuestion q : bank) {
                if (!excl.contains(q.getId())) {
                    result.add(q);
                    excl.add(q.getId());
                    pickedIds.add(q.getId());
                }
            }
        }

        // 3. 最终兜底: 总量仍不足时,只从"尚未达配额"的题型中随机补齐,
        // 保证不破坏用户要求的题型数量分布
        while (result.size() < questionCount) {
            // 统计各题型现有数量,找出仍缺的题型
            Map<Integer, Integer> haveByType = new HashMap<>();
            for (ExamQuestion q : result) {
                haveByType.merge(q.getQuestionType(), 1, Integer::sum);
            }
            List<Integer> underQuotaTypes = new ArrayList<>();
            for (Map.Entry<Integer, Integer> e : typeCounts.entrySet()) {
                int quota = e.getValue() == null ? 0 : e.getValue();
                if (quota > 0 && haveByType.getOrDefault(e.getKey(), 0) < quota) {
                    underQuotaTypes.add(e.getKey());
                }
            }
            if (underQuotaTypes.isEmpty()) break;
            Set<Long> excl = new HashSet<>(pickedIds);
            excl.addAll(recentUsedIds);
            List<ExamQuestion> bank = selectByDifficulty(subjectId, null, underQuotaTypes, null, excl);
            if (bank.isEmpty()) break; // 题库中该题型也无题,只能缺着(分布优先于总数)
            Collections.shuffle(bank, random);
            for (ExamQuestion q : bank) {
                if (result.size() >= questionCount) break;
                int quota = typeCounts.getOrDefault(q.getQuestionType(), 0);
                if (quota > 0 && haveByType.merge(q.getQuestionType(), 1, Integer::sum) <= quota
                        && !excl.contains(q.getId())) {
                    result.add(q);
                    excl.add(q.getId());
                    pickedIds.add(q.getId());
                }
            }
        }

        // 4. 裁剪:补齐可能略超请求数量,超出部分从题库补的题(列表尾部)移除
        while (result.size() > questionCount) {
            ExamQuestion extra = result.remove(result.size() - 1);
            if (extra.getId() != null) pickedIds.remove(extra.getId());
        }

        // 5. 统一分配分值并回写数据库(AI 题插入时默认 5 分,需按总分覆盖)
        for (ExamQuestion q : result) {
            if (perScore.compareTo(BigDecimal.ZERO) > 0 && perScore.compareTo(q.getFullScore()) != 0) {
                q.setFullScore(perScore);
                questionMapper.update(null, new LambdaUpdateWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getId, q.getId())
                        .set(ExamQuestion::getFullScore, perScore));
            }
        }
        return result;
    }

    /** 解析所选题型(1/2/4/5/7),非法题型剔除 */
    private List<Integer> parseTeacherTypeList(Object raw) {
        List<Integer> list = new ArrayList<>();
        if (raw instanceof List) {
            for (Object o : (List<?>) raw) {
                try {
                    list.add(Integer.valueOf(o.toString()));
                } catch (Exception ignore) {
                }
            }
        }
        List<Integer> valid = list.stream()
                .filter(t -> t == SystemConstants.Q_TYPE_SINGLE || t == SystemConstants.Q_TYPE_MULTI
                        || t == SystemConstants.Q_TYPE_FILL || t == SystemConstants.Q_TYPE_SHORT
                        || t == SystemConstants.Q_TYPE_CALC)
                .distinct()
                .collect(Collectors.toList());
        return valid.isEmpty() ? Arrays.asList(SystemConstants.Q_TYPE_SINGLE, SystemConstants.Q_TYPE_MULTI,
                SystemConstants.Q_TYPE_FILL, SystemConstants.Q_TYPE_SHORT, SystemConstants.Q_TYPE_CALC) : valid;
    }

    /** 解析题型->数量;未配置时按所选题型平均分配题目总数 */
    private Map<Integer, Integer> parseTypeCounts(Object raw, List<Integer> types, int total) {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (Integer t : types) {
            counts.put(t, 0);
        }
        if (raw instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) raw).entrySet()) {
                try {
                    counts.put(Integer.valueOf(entry.getKey().toString()),
                            Integer.valueOf(entry.getValue().toString()));
                } catch (Exception ignore) {
                }
            }
        }
        int sum = counts.values().stream().mapToInt(Integer::intValue).sum();
        if (sum == 0 && !types.isEmpty()) {
            int base = total / types.size();
            int rem = total % types.size();
            int i = 0;
            for (Integer t : types) {
                counts.put(t, base + (i++ < rem ? 1 : 0));
            }
        }
        return counts;
    }

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

        // 2. 按知识点匹配选择题(1)和填空题(4)
        List<ExamQuestion> candidates = new ArrayList<>();
        if (!topKnowledgePoints.isEmpty()) {
            for (String kp : topKnowledgePoints) {
                List<ExamQuestion> qs = questionMapper.selectList(
                        new LambdaQueryWrapper<ExamQuestion>()
                                .like(ExamQuestion::getKnowledgePoint, kp)
                                .in(ExamQuestion::getQuestionType, SystemConstants.Q_TYPE_SINGLE, SystemConstants.Q_TYPE_FILL)
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
                            .in(ExamQuestion::getQuestionType, SystemConstants.Q_TYPE_SINGLE, SystemConstants.Q_TYPE_FILL)
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
                            .in(ExamQuestion::getQuestionType, SystemConstants.Q_TYPE_SINGLE, SystemConstants.Q_TYPE_FILL)
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

        // 题型数量分布(题型 -> 数量):用于提示词描述 + 接受题目时按题型硬性截断
        Map<Integer, Integer> typeCounts = parseTypeCounts(config.get("typeCounts"), questionTypes, questionCount);
        String[] typeNames = {null, "单选题", "多选题", null, "填空题", "简答题", null, "计算题"};
        StringBuilder typeCountsDesc = new StringBuilder();
        for (Map.Entry<Integer, Integer> e : typeCounts.entrySet()) {
            if (e.getValue() == null || e.getValue() <= 0) continue;
            String name = e.getKey() < typeNames.length ? typeNames[e.getKey()] : null;
            if (name == null) name = "题型" + e.getKey();
            if (typeCountsDesc.length() > 0) typeCountsDesc.append(" ");
            typeCountsDesc.append(name).append(e.getValue()).append("道");
        }

        // 学科名称（prompt 需要）
        String subjectName = "通用学科";
        SysSubject subject = subjectMapper.selectById(subjectId);
        if (subject != null && subject.getSubjectName() != null) {
            subjectName = subject.getSubjectName();
        }

        List<ExamQuestion> result = new ArrayList<>();
        // 一次性加载该学科全部题目(归一化后),同时用于: 库内去重集合 + 提示词"已出过题"列表,
        // 避免每轮 AI 调用都重复全表查询
        List<ExamQuestion> existingList = questionMapper.selectList(
                new LambdaQueryWrapper<ExamQuestion>()
                        .eq(ExamQuestion::getSubjectId, subjectId)
                        .eq(ExamQuestion::getDeleted, 0)
                        .orderByDesc(ExamQuestion::getId));
        // 库内已有题目内容（归一化后），用于入库前去重
        Set<String> existingContents = existingList.stream()
                .map(ExamQuestion::getContent)
                .filter(c -> c != null && !c.trim().isEmpty())
                .map(SelfPracticeServiceImpl::normalizeContent)
                .collect(Collectors.toSet());
        // 提示词"已出过题"列表(最新优先,最多 50 条),任务级缓存,轮次间复用
        List<String> existingQuestions = existingList.stream()
                .map(ExamQuestion::getContent)
                .filter(c -> c != null && !c.trim().isEmpty())
                .limit(50)
                .collect(Collectors.toList());
        // 本批次已接受的题目内容（批次内去重）
        Set<String> batchContents = new HashSet<>();

        // 循环调用 AI 直到凑够题目数量：单次生成不超过 8 道，兼顾单次调用响应时长与总轮次。
        // 轮次上限按平均每轮 6 道有效产出估算(去重/过滤会损失部分),确保能凑够 questionCount
        // 题型分布不做专轮约束:AI 单轮返回的题型可能与请求不符,差额由题库按题型补齐兜底,
        // 避免专轮空转白白增加多轮 AI 调用耗时
        int maxRounds = Math.max(6, (int) Math.ceil(questionCount / 6.0) + 2);
        int emptyStreak = 0;
        for (int round = 0; round < maxRounds && result.size() < questionCount; round++) {
            int stillNeed = Math.min(questionCount - result.size(), 8);
            int beforeRound = result.size();
            // 计算各题型仍缺的配额,本轮只请求仍缺的题型(缺大题时定向补大题),
            // 避免 AI 反复出已配满的题型导致有效产出为零、轮次空转
            Map<Integer, Integer> haveByType = new HashMap<>();
            for (ExamQuestion q : result) {
                haveByType.merge(q.getQuestionType(), 1, Integer::sum);
            }
            Map<Integer, Integer> needByType = new LinkedHashMap<>();
            for (Map.Entry<Integer, Integer> e : typeCounts.entrySet()) {
                int quota = e.getValue() == null ? 0 : e.getValue();
                int lack = quota - haveByType.getOrDefault(e.getKey(), 0);
                if (quota > 0 && lack > 0) needByType.put(e.getKey(), lack);
            }
            List<Integer> roundTypes;
            String roundTypeDesc;
            if (!needByType.isEmpty()) {
                roundTypes = new ArrayList<>(needByType.keySet());
                int lackTotal = needByType.values().stream().mapToInt(Integer::intValue).sum();
                stillNeed = Math.min(stillNeed, lackTotal);
                String[] tn = {null, "单选题", "多选题", null, "填空题", "简答题", null, "计算题"};
                StringBuilder desc = new StringBuilder();
                for (Map.Entry<Integer, Integer> e : needByType.entrySet()) {
                    String name = e.getKey() < tn.length && tn[e.getKey()] != null ? tn[e.getKey()] : "题型" + e.getKey();
                    if (desc.length() > 0) desc.append(" ");
                    desc.append(name).append(e.getValue()).append("道");
                }
                roundTypeDesc = desc.toString();
            } else {
                // 无题型配额约束(如学生自练): 按全部所选题型出
                roundTypes = questionTypes;
                roundTypeDesc = typeCountsDesc.length() > 0 ? typeCountsDesc.toString() : null;
            }
            String json = aiServiceProvider.generateExamQuestions(subjectName, knowledgePoints, roundTypes,
                    difficulty, stillNeed, existingQuestions,
                    (String) config.get("difficultyRatio"),
                    roundTypeDesc);
            if (json == null || json.trim().isEmpty()) {
                log.warn("AI 组卷第 {} 轮未返回内容", round + 1);
                emptyStreak++;
                if (emptyStreak >= 2) {
                    log.warn("AI 组卷连续 {} 轮未返回内容,停止生成,当前已生成 {} 道", emptyStreak, result.size());
                    break;
                }
                continue;
            }
            emptyStreak = 0;

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
                    // 题型矫正:填空题但参考答案是长文本主观答案时归为简答题,
                    // 否则批改引擎会按字符串精确比对判 0 分、点评只回显参考答案
                    if (q.getQuestionType() == SystemConstants.Q_TYPE_FILL) {
                        String std = obj.getString("standardAnswer");
                        if (std != null && std.length() > 20) {
                            log.warn("AI 组卷第 {} 轮第 {} 题标注为填空题但答案为长文本({}字),归为简答题",
                                    round + 1, i + 1, std.length());
                            q.setQuestionType(SystemConstants.Q_TYPE_SHORT);
                        }
                    }
                    // 过滤判断题(3):仅保留选择、填空、大题
                    if (q.getQuestionType() == SystemConstants.Q_TYPE_JUDGE) {
                        log.warn("AI 组卷第 {} 轮第 {} 题为判断题，已按规则过滤", round + 1, i + 1);
                        continue;
                    }
                    // 过滤题型不符的题(如 AI 返回判断题外的其它未选题型),差额由题库按题型补齐兜底
                    if (!questionTypes.contains(q.getQuestionType())) {
                        log.warn("AI 组卷第 {} 轮第 {} 题题型 {} 不在所选范围内,已跳过",
                                round + 1, i + 1, q.getQuestionType());
                        continue;
                    }
                    // 按题型数量分布硬性截断:该题型已达配额时不再接受,避免挤占其它题型名额
                    Integer typeQuota = typeCounts.get(q.getQuestionType());
                    long typeHave = result.stream().filter(x -> x.getQuestionType() == q.getQuestionType()).count();
                    if (typeQuota != null && typeQuota > 0 && typeHave >= typeQuota) {
                        log.warn("AI 组卷第 {} 轮第 {} 题题型 {} 已达配额 {} 道,跳过多出部分",
                                round + 1, i + 1, q.getQuestionType(), typeQuota);
                        continue;
                    }
                    q.setDifficulty(obj.getIntValue("difficulty"));
                    // 难度归一化：请求拔高(>=3)时，AI 返回的难度不得低于 3，避免难度虚标
                    if (difficulty != null && difficulty >= 3 && q.getDifficulty() != null && q.getDifficulty() < 3) {
                        log.warn("AI 组卷第 {} 轮第 {} 题请求拔高难度但 AI 标注难度 {}，已归一化为 3",
                                round + 1, i + 1, q.getDifficulty());
                        q.setDifficulty(3);
                    }
                    // 难度缺失时按请求难度兜底，避免入库难度为 0
                    if (q.getDifficulty() == null || q.getDifficulty() <= 0) {
                        q.setDifficulty(difficulty != null && difficulty > 0 ? difficulty : 3);
                    }
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
                : Arrays.asList(1, 2, 4, 5, 7);
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
                    errorCandidates.addAll(selectByDifficulty(subjectId, kp, questionTypes, difficulty, Collections.emptySet()));
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
                    kpCandidates.addAll(selectByDifficulty(subjectId, kp, questionTypes, difficulty, selectedIds));
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
                List<ExamQuestion> allCandidates = selectByDifficulty(subjectId, null, questionTypes, difficulty, selectedIds);
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

        // 题型分布数量（前端可选微调，合计应等于题目总数）
        Integer singleCount = config.get("singleCount") != null
                ? Integer.valueOf(config.get("singleCount").toString()) : null;
        Integer multiCount = config.get("multiCount") != null
                ? Integer.valueOf(config.get("multiCount").toString()) : null;
        Integer fillCount = config.get("fillCount") != null
                ? Integer.valueOf(config.get("fillCount").toString()) : null;
        Integer shortCount = config.get("shortCount") != null
                ? Integer.valueOf(config.get("shortCount").toString()) : null;

        Random random = new Random();
        List<ExamQuestion> selected = new ArrayList<>();

        if (singleCount != null || multiCount != null || fillCount != null || shortCount != null) {
            // 配置了题型分布：按题型+难度比例分层抽题
            selected.addAll(drawByTypeAndDifficulty(subjectId, SystemConstants.Q_TYPE_SINGLE, singleCount,
                    easyRatio, mediumRatio, hardRatio, random, null));
            selected.addAll(drawByTypeAndDifficulty(subjectId, SystemConstants.Q_TYPE_MULTI, multiCount,
                    easyRatio, mediumRatio, hardRatio, random, null));
            selected.addAll(drawByTypeAndDifficulty(subjectId, SystemConstants.Q_TYPE_FILL, fillCount,
                    easyRatio, mediumRatio, hardRatio, random, null));
            selected.addAll(drawByTypeAndDifficulty(subjectId, SystemConstants.Q_TYPE_SHORT, shortCount,
                    easyRatio, mediumRatio, hardRatio, random, null));
        } else {
            // 未配置题型分布：按难度分层抽题
            int easyCount = Math.round(questionCount * easyRatio / 100f);
            int mediumCount = Math.round(questionCount * mediumRatio / 100f);
            int hardCount = questionCount - easyCount - mediumCount;
            if (hardCount < 0) hardCount = 0;
            selected.addAll(drawByDifficulty(subjectId, 1, easyCount, random));
            selected.addAll(drawByDifficulty(subjectId, 2, mediumCount, random));
            if (hardCount > 0) {
                selected.addAll(drawByDifficulty(subjectId, 3, hardCount, random));
            }
        }

        // 去重
        selected = selected.stream()
                .collect(Collectors.collectingAndThen(
                        Collectors.toCollection(() -> new TreeSet<>(Comparator.comparing(ExamQuestion::getId))),
                        ArrayList::new));

        return selected;
    }

    /**
     * 按题型+难度比例抽题（题型分布微调用）
     *
     * @param questionType 题型（1单选/2多选/4填空/5简答）
     * @param count        该题型题目数量（null 或 <=0 时跳过）
     */
    /**
     * 加载教师近期出题记录中已使用过的题目 id(最新 50 条),
     * 用于题库补齐时排除,让多次出题尽量不重复。
     */
    private Set<Long> loadRecentUsedQuestionIds(Long teacherId) {
        Set<Long> used = new HashSet<>();
        try {
            java.util.List<com.zhixue.ai.module.exam.entity.AiGenerateRecord> records = generateRecordMapper.selectList(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<com.zhixue.ai.module.exam.entity.AiGenerateRecord>()
                            .eq(com.zhixue.ai.module.exam.entity.AiGenerateRecord::getTeacherId, teacherId)
                            .orderByDesc(com.zhixue.ai.module.exam.entity.AiGenerateRecord::getCreateTime)
                            .last("LIMIT 50"));
            for (com.zhixue.ai.module.exam.entity.AiGenerateRecord r : records) {
                if (r.getQuestionIds() == null || r.getQuestionIds().isEmpty()) continue;
                try {
                    java.util.List<Long> ids = com.alibaba.fastjson2.JSON.parseArray(r.getQuestionIds(), Long.class);
                    if (ids != null) used.addAll(ids);
                } catch (Exception ignore) {
                    // 单条记录 JSON 异常不影响其余记录
                }
            }
        } catch (Exception e) {
            log.warn("加载近期出题记录已用题目失败,跳过去重排除: {}", e.getMessage());
        }
        return used;
    }

    private List<ExamQuestion> drawByTypeAndDifficulty(Long subjectId, int questionType, Integer count,
                                                      int easyRatio, int mediumRatio, int hardRatio, Random random,
                                                      Set<Long> excludedIds) {
        if (count == null || count <= 0) return Collections.emptyList();
        int easyCount = Math.round(count * easyRatio / 100f);
        int mediumCount = Math.round(count * mediumRatio / 100f);
        int hardCount = count - easyCount - mediumCount;
        if (hardCount < 0) hardCount = 0;
        Set<Long> excl = excludedIds == null ? Collections.emptySet() : excludedIds;
        List<ExamQuestion> result = new ArrayList<>();
        result.addAll(drawByTypeDifficulty(subjectId, questionType, 1, easyCount, random, excl));
        result.addAll(drawByTypeDifficulty(subjectId, questionType, 2, mediumCount, random, excl));
        if (hardCount > 0) {
            result.addAll(drawByTypeDifficulty(subjectId, questionType, 3, hardCount, random, excl));
        }
        return result;
    }

    /**
     * 按题型+难度从题库中随机抽取题目
     */
    private List<ExamQuestion> drawByTypeDifficulty(Long subjectId, int questionType, int difficulty,
                                                    int count, Random random, Set<Long> excludedIds) {
        if (count <= 0) return Collections.emptyList();
        List<ExamQuestion> pool = selectByDifficulty(subjectId, null,
                Collections.singletonList(questionType), difficulty,
                excludedIds == null ? Collections.emptySet() : excludedIds);
        Collections.shuffle(pool, random);
        int actual = Math.min(count, pool.size());
        return pool.subList(0, actual);
    }

    /**
     * 按难度从题库中随机抽取题目
     */
    private List<ExamQuestion> drawByDifficulty(Long subjectId, int difficulty, int count, Random random) {
        if (count <= 0) return Collections.emptyList();
        List<ExamQuestion> pool = selectByDifficulty(subjectId, null, null, difficulty, Collections.emptySet());
        Collections.shuffle(pool, random);
        int actual = Math.min(count, pool.size());
        return pool.subList(0, actual);
    }

    /**
     * 按难度分层查询题目池，保证难度与要求严格匹配：
     * <ul>
     *   <li>难度 1/2：精确匹配对应难度</li>
     *   <li>难度 &gt;=3（拔高及以上）：优先匹配难度 4、5 的真难题，不足时再补充难度 3，
     *       避免拔高题与中档题(2)难度雷同</li>
     * </ul>
     *
     * @param kp            知识点（可为 null，不按知识点过滤）
     * @param questionTypes 题型列表（可为 null 或空，不按题型过滤）
     * @param excludedIds   需排除的题目 id（可为空）
     */
    private List<ExamQuestion> selectByDifficulty(Long subjectId, String kp, List<Integer> questionTypes,
                                                  Integer difficulty, Set<Long> excludedIds) {
        if (difficulty == null || difficulty <= 0) {
            // 混合难度：不限制难度
            return queryPool(subjectId, kp, questionTypes, null, null, excludedIds);
        }
        if (difficulty >= 3) {
            // 拔高及以上：优先难度 4、5 的真难题
            List<ExamQuestion> hard = queryPool(subjectId, kp, questionTypes, 4, 5, excludedIds);
            if (hard.size() >= 100) {
                return hard;
            }
            // 不足时补充难度 3
            Set<Long> picked = hard.stream().map(ExamQuestion::getId).collect(Collectors.toSet());
            picked.addAll(excludedIds);
            List<ExamQuestion> fill = queryPool(subjectId, kp, questionTypes, 3, 3, picked);
            hard.addAll(fill);
            return hard;
        }
        return queryPool(subjectId, kp, questionTypes, difficulty, difficulty, excludedIds);
    }

    /**
     * 按学科/知识点/题型/难度区间查询题目（含删除过滤与排除 id）
     */
    private List<ExamQuestion> queryPool(Long subjectId, String kp, List<Integer> questionTypes,
                                         Integer minDifficulty, Integer maxDifficulty, Set<Long> excludedIds) {
        LambdaQueryWrapper<ExamQuestion> wrapper = new LambdaQueryWrapper<ExamQuestion>()
                .eq(ExamQuestion::getSubjectId, subjectId)
                .ne(ExamQuestion::getQuestionType, SystemConstants.Q_TYPE_JUDGE)
                .eq(ExamQuestion::getDeleted, 0);
        if (kp != null && !kp.isEmpty()) {
            wrapper.like(ExamQuestion::getKnowledgePoint, kp);
        }
        if (questionTypes != null && !questionTypes.isEmpty()) {
            wrapper.in(ExamQuestion::getQuestionType, questionTypes);
        }
        if (minDifficulty != null) {
            wrapper.ge(ExamQuestion::getDifficulty, minDifficulty);
        }
        if (maxDifficulty != null) {
            wrapper.le(ExamQuestion::getDifficulty, maxDifficulty);
        }
        List<ExamQuestion> list = questionMapper.selectList(wrapper);
        if (excludedIds != null && !excludedIds.isEmpty()) {
            list = list.stream()
                    .filter(q -> !excludedIds.contains(q.getId()))
                    .collect(Collectors.toList());
        }
        return list;
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
