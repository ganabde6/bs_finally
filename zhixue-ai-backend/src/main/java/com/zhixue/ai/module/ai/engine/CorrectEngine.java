package com.zhixue.ai.module.ai.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.zhixue.ai.common.constant.SystemConstants;
import com.zhixue.ai.module.ai.entity.AiCorrectRecord;
import com.zhixue.ai.module.ai.entity.AiModelConfig;
import com.zhixue.ai.module.ai.mapper.AiModelConfigMapper;
import com.zhixue.ai.module.exam.entity.ExamQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 全题型智能批改引擎(本地规则版)
 * <p>批改策略:</p>
 * <ul>
 *   <li>客观题(单选/多选/判断):精确比对标准答案</li>
 *   <li>填空题:去空格+忽略大小写比对,支持「|」分隔的多个可接受答案</li>
 *   <li>简答题:基于得分点 JSON 关键词命中数计分</li>
 *   <li>作文题:字数+结构+关键词三维度综合评分</li>
 *   <li>计算题:基于步骤得分点逐行比对,支持步骤分</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CorrectEngine {

    private final AiModelConfigMapper aiModelConfigMapper;

    /** AI模型版本标识 */
    private static final String AI_MODEL = "LOCAL-RULE-V1";

    /**
     * 批改单题
     * @param question 题目
     * @param studentAnswer 学生答案
     * @param fullScore 本题满分
     * @return 批改记录
     */
    public AiCorrectRecord correct(ExamQuestion question, String studentAnswer, BigDecimal fullScore) {
        if (studentAnswer == null) {
            studentAnswer = "";
        }
        AiCorrectRecord record = new AiCorrectRecord();
        record.setQuestionId(question.getId());
        record.setStudentAnswer(studentAnswer);
        record.setFullScore(fullScore);
        record.setCorrectType(SystemConstants.CORRECT_TYPE_AI);
        record.setAiModel(AI_MODEL);

        BigDecimal score;
        Integer isCorrect;
        String scoreDetail;
        String remark;

        Integer type = question.getQuestionType();
        // 题型矫正:填空题但参考答案是长文本主观答案时,按简答题批改与点评,
        // 避免长文本答案被字符串精确比对判 0 分、且点评只回显参考答案
        if (type != null && type == SystemConstants.Q_TYPE_FILL
                && question.getStandardAnswer() != null
                && question.getStandardAnswer().length() > 20) {
            log.warn("题目 {} 标注为填空题但参考答案为长文本({}字),按简答题批改",
                    question.getId(), question.getStandardAnswer().length());
            type = SystemConstants.Q_TYPE_SHORT;
        }
        switch (type) {
            case SystemConstants.Q_TYPE_SINGLE:
            case SystemConstants.Q_TYPE_JUDGE:
                // 单选/判断:精确比对
                boolean ok = studentAnswer.trim().equalsIgnoreCase(question.getStandardAnswer().trim());
                score = ok ? fullScore : BigDecimal.ZERO;
                isCorrect = ok ? 1 : 0;
                scoreDetail = "[{\"step\":\"答案匹配\",\"score\":" + score + "}]";
                remark = ok ? "答案正确" : "答案错误,正确答案:" + question.getStandardAnswer();
                break;
            case SystemConstants.Q_TYPE_MULTI:
                // 多选:排序后比对
                String sa = sortLetters(studentAnswer);
                String ss = sortLetters(question.getStandardAnswer());
                boolean mok = sa.equalsIgnoreCase(ss);
                score = mok ? fullScore : BigDecimal.ZERO;
                isCorrect = mok ? 1 : 0;
                scoreDetail = "[{\"step\":\"多选匹配\",\"score\":" + score + "}]";
                remark = mok ? "答案正确" : "答案错误,正确答案:" + question.getStandardAnswer();
                break;
            case SystemConstants.Q_TYPE_FILL:
                // 填空:支持「|」分隔的多个可接受答案
                boolean fok = false;
                String stdFill = question.getStandardAnswer();
                for (String accept : stdFill.split("\\|")) {
                    if (studentAnswer.trim().equalsIgnoreCase(accept.trim())) {
                        fok = true;
                        break;
                    }
                }
                score = fok ? fullScore : BigDecimal.ZERO;
                isCorrect = fok ? 1 : 0;
                scoreDetail = "[{\"step\":\"填空匹配\",\"score\":" + score + "}]";
                remark = fok ? "答案正确" : "答案错误,参考答案:" + stdFill;
                break;
            case SystemConstants.Q_TYPE_SHORT:
            case SystemConstants.Q_TYPE_CALC:
                // 简答/计算:基于得分点 JSON 计分
                BigDecimal[] sr = correctByScorePoint(question, studentAnswer, fullScore);
                score = sr[0];
                isCorrect = score.compareTo(BigDecimal.ZERO) == 0 ? 0 :
                        (score.compareTo(fullScore) == 0 ? 1 : 2);
                scoreDetail = buildScoreDetail(question, studentAnswer, fullScore, score);
                remark = buildSubjectiveRemark(score, fullScore, question, studentAnswer);
                break;
            case SystemConstants.Q_TYPE_ESSAY:
                // 作文:字数+结构+关键词三维度
                BigDecimal[] er = correctEssay(question, studentAnswer, fullScore);
                score = er[0];
                isCorrect = 2;
                scoreDetail = buildEssayDetail(studentAnswer, fullScore, score);
                remark = buildEssayRemark(score, fullScore, studentAnswer);
                break;
            default:
                score = BigDecimal.ZERO;
                isCorrect = 0;
                scoreDetail = "[]";
                remark = "暂不支持该题型";
        }
        // 应用严苛度系数
        double strictness = getStrictness();
        if (type >= SystemConstants.Q_TYPE_SHORT) {
            score = score.multiply(BigDecimal.valueOf(strictness))
                    .setScale(2, RoundingMode.HALF_UP);
            if (score.compareTo(fullScore) > 0) score = fullScore;
        }
        record.setScore(score);
        record.setIsCorrect(isCorrect);
        record.setScoreDetail(scoreDetail);
        record.setCorrectRemark(remark);
        record.setErrorTag(buildErrorTag(question, isCorrect));
        return record;
    }

    /** 多选答案排序(如 "DB" -> "BD") */
    private String sortLetters(String s) {
        char[] arr = s.replaceAll("\\s+", "").toUpperCase().toCharArray();
        Arrays.sort(arr);
        return new String(arr);
    }

    /** 基于得分点 JSON 计分 */
    private BigDecimal[] correctByScorePoint(ExamQuestion question, String studentAnswer, BigDecimal fullScore) {
        String sp = question.getScorePoint();
        if (sp == null || sp.trim().isEmpty() || "[]".equals(sp.trim())) {
            // 无得分点,检查是否包含标准答案关键词
            String std = question.getStandardAnswer();
            if (std != null && studentAnswer.contains(std)) {
                return new BigDecimal[]{fullScore};
            }
            // 至少给文字努力分(30%)
            return studentAnswer.length() > 5 ?
                    new BigDecimal[]{fullScore.multiply(new BigDecimal("0.3"))} :
                    new BigDecimal[]{BigDecimal.ZERO};
        }
        JSONArray points = JSON.parseArray(sp);
        BigDecimal total = BigDecimal.ZERO;
        for (int i = 0; i < points.size(); i++) {
            JSONObject p = points.getJSONObject(i);
            String kw = p.getString("point");
            if (kw == null) kw = p.getString("step");
            BigDecimal pt = p.getBigDecimal("score");
            if (kw != null && pt != null && studentAnswer.contains(kw)) {
                total = total.add(pt);
            }
        }
        if (total.compareTo(fullScore) > 0) total = fullScore;
        return new BigDecimal[]{total};
    }

    /** 作文评分:字数 30% + 关键词 40% + 结构 30% */
    private BigDecimal[] correctEssay(ExamQuestion question, String studentAnswer, BigDecimal fullScore) {
        double lenScore = 0, kwScore = 0, structScore = 0;
        int len = studentAnswer.length();
        // 字数维度
        if (len >= 600) lenScore = 0.30;
        else if (len >= 400) lenScore = 0.22;
        else if (len >= 200) lenScore = 0.15;
        else if (len > 0) lenScore = 0.05;
        // 关键词维度
        String sp = question.getScorePoint();
        if (sp != null && !sp.isEmpty()) {
            JSONArray points = JSON.parseArray(sp);
            int hit = 0;
            for (int i = 0; i < points.size(); i++) {
                JSONObject p = points.getJSONObject(i);
                String kw = p.getString("point");
                if (kw != null && studentAnswer.contains(kw)) hit++;
            }
            kwScore = points.size() > 0 ? (0.40 * hit / points.size()) : 0.20;
        } else {
            kwScore = 0.20;
        }
        // 结构维度:段落、标点
        long paragraphs = studentAnswer.chars().filter(c -> c == '\n').count() + 1;
        if (paragraphs >= 3) structScore = 0.30;
        else if (paragraphs >= 2) structScore = 0.22;
        else structScore = 0.10;
        // 是否有句号
        if (studentAnswer.contains("。") || studentAnswer.contains(".") || studentAnswer.contains("!")) {
            structScore += 0;
        } else {
            structScore -= 0.05;
        }
        double total = lenScore + kwScore + structScore;
        if (total < 0) total = 0;
        BigDecimal score = fullScore.multiply(BigDecimal.valueOf(total))
                .setScale(2, RoundingMode.HALF_UP);
        if (score.compareTo(fullScore) > 0) score = fullScore;
        return new BigDecimal[]{score};
    }

    private String buildScoreDetail(ExamQuestion q, String answer, BigDecimal full, BigDecimal got) {
        JSONArray arr = new JSONArray();
        String sp = q.getScorePoint();
        if (sp != null && !sp.isEmpty()) {
            try {
                JSONArray points = JSON.parseArray(sp);
                for (int i = 0; i < points.size(); i++) {
                    JSONObject p = points.getJSONObject(i);
                    String kw = p.getString("point");
                    if (kw == null) kw = p.getString("step");
                    BigDecimal pt = p.getBigDecimal("score");
                    boolean hit = kw != null && answer.contains(kw);
                    JSONObject d = new JSONObject();
                    d.put("point", kw);
                    d.put("maxScore", pt);
                    d.put("gotScore", hit ? pt : BigDecimal.ZERO);
                    d.put("hit", hit);
                    arr.add(d);
                }
            } catch (Exception ignored) {}
        }
        if (arr.isEmpty()) {
            JSONObject d = new JSONObject();
            d.put("step", "整体评分");
            d.put("maxScore", full);
            d.put("gotScore", got);
            arr.add(d);
        }
        return arr.toJSONString();
    }

    private String buildEssayDetail(String answer, BigDecimal full, BigDecimal got) {
        JSONArray arr = new JSONArray();
        int len = answer.length();
        JSONObject d1 = new JSONObject();
        d1.put("dim", "字数");
        d1.put("value", len);
        d1.put("comment", len >= 600 ? "达标" : (len >= 400 ? "偏少" : "严重不足"));
        arr.add(d1);
        JSONObject d2 = new JSONObject();
        d2.put("dim", "关键词");
        d2.put("comment", "见得分点");
        arr.add(d2);
        JSONObject d3 = new JSONObject();
        d3.put("dim", "结构");
        long paras = answer.chars().filter(c -> c == '\n').count() + 1;
        d3.put("value", paras);
        d3.put("comment", paras >= 3 ? "结构清晰" : "建议分段");
        arr.add(d3);
        return arr.toJSONString();
    }

    /**
     * 生成针对学生作答的结构化点评:
     * ① 题目知识点及基础知识 ② 解题思路 ③ 结合学生答案诊断可能的思路偏差
     */
    private String buildSubjectiveRemark(BigDecimal score, BigDecimal full, ExamQuestion q, String answer) {
        double ratio = full.doubleValue() > 0 ? score.doubleValue() / full.doubleValue() : 0;
        StringBuilder sb = new StringBuilder();

        // ① 知识点 + 基础知识
        String kp = q.getKnowledgePoint();
        if (kp != null && !kp.isEmpty()) {
            sb.append("【知识点】本题考查:").append(kp).append("。");
        }
        String analysis = q.getAnalysis();
        if (analysis != null && !analysis.isEmpty()) {
            // 提取参考解析中的基础概念作为基础知识铺垫
            String base = analysis.length() > 60 ? analysis.substring(0, 60) : analysis;
            sb.append("基础知识:").append(base).append("。");
        }

        // ② 解题思路(来自得分点顺序 / 参考解析)
        List<String> steps = new ArrayList<>();
        String sp = q.getScorePoint();
        if (sp != null && !sp.isEmpty()) {
            try {
                JSONArray points = JSON.parseArray(sp);
                for (int i = 0; i < points.size(); i++) {
                    JSONObject p = points.getJSONObject(i);
                    String kw = p.getString("point");
                    if (kw == null) kw = p.getString("step");
                    if (kw != null && !kw.isEmpty()) steps.add(kw);
                }
            } catch (Exception ignored) {}
        }
        if (!steps.isEmpty()) {
            sb.append("【解题思路】应按以下要点依次展开:");
            for (int i = 0; i < steps.size(); i++) {
                sb.append(i + 1).append(")").append(steps.get(i));
                if (i < steps.size() - 1) sb.append("→");
            }
            sb.append("。");
        }

        // ③ 学生答案思路诊断
        sb.append("【你的作答分析】");
        if (answer == null || answer.trim().isEmpty()) {
            sb.append("未作答,无法诊断思路,建议先掌握解题思路后重新练习。");
            return sb.toString();
        }
        // 命中/遗漏的得分点
        List<String> missed = new ArrayList<>();
        int hitCount = 0;
        for (String s : steps) {
            if (answer.contains(s)) hitCount++;
            else missed.add(s);
        }
        if (!steps.isEmpty()) {
            if (hitCount == steps.size()) {
                sb.append("你的作答覆盖了全部关键要点,思路完整正确");
                if (ratio >= 0.95) sb.append(",继续保持。");
                else sb.append(",但表述或计算上仍有失分,建议检查推理过程细节。");
                return sb.toString();
            }
            // 推断思路断点:第一个未命中的要点即思路中断处
            String breakpoint = missed.get(0);
            int idx = steps.indexOf(breakpoint);
            sb.append("从你的答案看,你完成了前 ").append(idx).append(" 步的要点");
            if (idx > 0) sb.append("( ").append(String.join("、", steps.subList(0, idx))).append(" )");
            sb.append(",但在「").append(breakpoint).append("」处思路中断。");
            sb.append("可能的思路偏差:① 对「").append(breakpoint).append("」对应的").append(kp == null || kp.isEmpty() ? "知识点" : kp)
              .append("理解不够深入,导致后续推导无法继续;② 审题时可能忽略了该要点的条件;③ 公式或方法选择出现偏差。");
            sb.append("建议:先复习「").append(breakpoint).append("」涉及的基础知识,再对照解题思路逐步重做本题。");
        } else {
            // 无得分点,基于得分率和答案长度诊断
            if (ratio >= 0.9) sb.append("你的作答思路正确、表述完整,继续保持。");
            else if (ratio >= 0.6) sb.append("你的作答方向正确,但关键论述不完整,可能遗漏了题目要求的部分结论,建议对照解题思路检查是否答全。");
            else if (ratio >= 0.3) sb.append("你的作答触及了部分考点,但主体思路偏离,可能对题目要求的理解存在偏差,建议重新审题并梳理解题步骤。");
            else sb.append("你的作答与考点匹配度较低,可能从一开始的审题或方法选择就出现了偏差,建议先掌握本题知识点再重新练习。");
        }
        return sb.toString();
    }

    private String buildEssayRemark(BigDecimal score, BigDecimal full, String answer) {
        double ratio = full.doubleValue() > 0 ? score.doubleValue() / full.doubleValue() : 0;
        StringBuilder sb = new StringBuilder();
        sb.append("得分率:").append(String.format("%.0f%%", ratio * 100)).append("。");
        if (answer.length() < 400) sb.append("字数不达标,建议扩充至600字以上。");
        if (ratio >= 0.8) sb.append("整体表现优秀,主题突出,结构清晰。");
        else if (ratio >= 0.6) sb.append("整体良好,可进一步丰富细节与修辞。");
        else sb.append("建议加强主题立意与结构组织。");
        return sb.toString();
    }

    private String buildErrorTag(ExamQuestion q, Integer isCorrect) {
        if (isCorrect == 1) return null;
        List<String> tags = new ArrayList<>();
        Integer type = q.getQuestionType();
        if (type != null && type <= SystemConstants.Q_TYPE_FILL) {
            tags.add("基础不牢");
        }
        if (q.getKnowledgePoint() != null) {
            tags.add(q.getKnowledgePoint());
        }
        return JSON.toJSONString(tags);
    }

    /** 获取批改严苛度(从数据库读取,默认1.0) */
    private double getStrictness() {
        try {
            AiModelConfig cfg = aiModelConfigMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AiModelConfig>()
                            .eq(AiModelConfig::getConfigKey, "strictness"));
            if (cfg != null) {
                return Double.parseDouble(cfg.getConfigValue());
            }
        } catch (Exception e) {
            log.warn("读取严苛度失败,使用默认值 1.0: {}", e.getMessage());
        }
        return 1.0;
    }
}
