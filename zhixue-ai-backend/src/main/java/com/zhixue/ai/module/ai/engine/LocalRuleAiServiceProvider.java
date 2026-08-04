package com.zhixue.ai.module.ai.engine;

import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 本地规则版 AI 服务实现(默认降级方案)
 * <p>无需第三方接口,基于关键词、模板、规则完成答疑/润色/评语/风控</p>
 * <p>当 ai_provider 配置为 local 时由 ApiAiServiceProvider 内部调用</p>
 * <p>注意: 已移除 @Component, 不再作为独立 Spring Bean, 避免与 ApiAiServiceProvider 冲突</p>
 */
public class LocalRuleAiServiceProvider implements AiServiceProvider {

    /** 违规关键词(简化版,实际可扩展) */
    private static final List<String> FORBIDDEN_KEYWORDS = Arrays.asList(
            "作弊", "代考", "答案泄露", "黑客", "攻击", "色情", "暴力", "赌博", "毒品"
    );

    @Override
    public String moderateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        for (String kw : FORBIDDEN_KEYWORDS) {
            if (content.contains(kw)) {
                return "提问包含违规关键词:「" + kw + "」,已拦截";
            }
        }
        return null;
    }

    @Override
    public String tutorAnswer(String question, String context) {
        if (question == null || question.trim().isEmpty()) {
            return "请输入您的问题";
        }
        // 基于规则的回答:匹配题型/学科关键词
        StringBuilder sb = new StringBuilder();
        sb.append("【AI助学老师】已收到您的提问:\n").append(question).append("\n\n");
        if (context != null && !context.isEmpty()) {
            sb.append("根据您的学情档案(").append(context).append("),");
        }
        // 简单规则识别
        String q = question.toLowerCase();
        if (q.contains("公式") || q.contains("怎么算") || q.contains("计算")) {
            sb.append("这是一道计算类问题。建议步骤:\n")
              .append("1. 仔细审题,明确已知量与求解目标\n")
              .append("2. 选取对应公式/定理\n")
              .append("3. 代入数据,逐步计算\n")
              .append("4. 检查结果合理性\n")
              .append("如需具体解析,请提供完整题目。");
        } else if (q.contains("什么意思") || q.contains("含义") || q.contains("解释")) {
            sb.append("针对概念性问题,建议:\n")
              .append("1. 抓住核心定义\n")
              .append("2. 联想已学过的相关知识\n")
              .append("3. 通过例题加深理解\n")
              .append("请告诉我具体学科与知识点,我可以给出更精准的讲解。");
        } else if (q.contains("作文") || q.contains("写作")) {
            sb.append("作文提升建议:\n")
              .append("1. 主题鲜明,开头点题\n")
              .append("2. 结构清晰:引言-主体-结尾\n")
              .append("3. 多用修辞:比喻、排比、引用\n")
              .append("4. 情感真挚,有细节描写\n")
              .append("5. 结尾升华,呼应开头");
        } else {
            sb.append("建议按以下思路拆解:\n")
              .append("1. 明确题目考查的知识点\n")
              .append("2. 回顾相关公式/概念\n")
              .append("3. 列出已知条件与求解目标\n")
              .append("4. 逐步推导,注意单位与符号\n")
              .append("5. 复核答案合理性\n")
              .append("您可以把完整题目发给我,我会给出详细解析。");
        }
        return sb.toString();
    }

    @Override
    public String polishText(String original) {
        if (original == null || original.trim().isEmpty()) {
            return "原文为空,无法润色";
        }
        // 基于规则的润色:统计字数、检查常见问题
        int len = original.length();
        StringBuilder sb = new StringBuilder();
        sb.append("【AI润色建议】\n");
        sb.append("原文共 ").append(len).append(" 字。\n\n");
        if (len < 100) {
            sb.append("⚠ 字数偏少,建议扩充内容至 200 字以上。\n");
        } else if (len > 1000) {
            sb.append("⚠ 字数较多,注意精炼表达,避免冗余。\n");
        } else {
            sb.append("✓ 字数适中。\n");
        }
        // 检查常见问题
        if (!original.endsWith("。") && !original.endsWith("!") && !original.endsWith("?")) {
            sb.append("⚠ 结尾缺少标点,建议补充句号。\n");
        }
        long commaCount = original.chars().filter(ch -> ch == '，').count();
        if (commaCount > len * 0.1) {
            sb.append("⚠ 逗号使用过多,建议断句。\n");
        }
        sb.append("\n【润色参考】\n");
        // 简单润色:首字母缩进、段末加句号
        String polished = original.trim();
        if (!polished.endsWith("。") && !polished.endsWith("!") && !polished.endsWith("?")) {
            polished = polished + "。";
        }
        sb.append("　　").append(polished);
        return sb.toString();
    }

    @Override
    public String generateComment(String studentName, String performance) {
        StringBuilder sb = new StringBuilder();
        sb.append("【").append(studentName == null ? "该同学" : studentName).append(" 学情反馈单】\n\n");
        if (performance == null || performance.isEmpty()) {
            sb.append("本阶段学习态度端正,完成情况良好。建议继续保持,针对薄弱知识点加强练习。");
        } else {
            sb.append(performance).append("\n\n");
        }
        sb.append("【教师寄语】学习是一场马拉松,贵在坚持。希望家校携手,共同助力孩子成长。");
        return sb.toString();
    }

    @Override
    public String generateStudySuggestion(String weakPoints, String strongPoints) {
        StringBuilder sb = new StringBuilder();
        sb.append("根据您的历次作答数据,AI 已生成个性化提升建议:\n\n");
        if (strongPoints != null && !strongPoints.isEmpty()) {
            sb.append("✓ 优势模块:").append(strongPoints).append("\n");
            sb.append("  建议保持优势,可挑战更高难度题型\n\n");
        }
        if (weakPoints != null && !weakPoints.isEmpty()) {
            sb.append("✗ 薄弱知识点:").append(weakPoints).append("\n");
            sb.append("  建议:\n");
            sb.append("  1. 复习课本基础概念\n");
            sb.append("  2. 重做错题,理解错误原因\n");
            sb.append("  3. 完成 AI 推送的变式题训练\n");
            sb.append("  4. 每周专项练习 30 分钟\n");
        }
        return sb.toString();
    }

    @Override
    public String generateVariant(String originalQuestion, String knowledgePoint, Integer questionType, int variantIndex) {
        // 本地规则版:基于原题目改写,生成变式题
        String typeLabel;
        switch (questionType == null ? 0 : questionType) {
            case 1: typeLabel = "单选题"; break;
            case 2: typeLabel = "多选题"; break;
            case 3: typeLabel = "判断题"; break;
            case 4: typeLabel = "填空题"; break;
            case 5: typeLabel = "简答题"; break;
            case 6: typeLabel = "作文题"; break;
            default: typeLabel = "练习题";
        }
        StringBuilder sb = new StringBuilder();
        sb.append("【AI 变式题 #").append(variantIndex).append("】\n\n");
        sb.append("知识点:").append(knowledgePoint == null ? "通用" : knowledgePoint).append("\n");
        sb.append("题型:").append(typeLabel).append("\n\n");
        sb.append("【原题目】\n").append(originalQuestion).append("\n\n");
        sb.append("【变式题 #").append(variantIndex).append("】\n");
        sb.append("请根据以下知识点「").append(knowledgePoint == null ? "相关知识点" : knowledgePoint).append("」,");
        sb.append("设计一道与原题考查相同底层知识但题型或逻辑发生变化的练习题。\n\n");
        sb.append(" 提示:当前为本地规则模式,变式题由 AI 接口生成效果更佳。");
        sb.append("请在管理端配置通义千问或 DeepSeek API Key 以启用 AI 生成。");
        return sb.toString();
    }

    @Override
    public String correctVariant(String questionContent, String standardAnswer, String studentAnswer) {
        // 本地规则版:仅支持与标准答案做文本比对,无法判定时返回 null
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return "错误\n未作答";
        }
        if (standardAnswer == null || standardAnswer.trim().isEmpty()) {
            return null;
        }
        String std = standardAnswer.replaceAll("\\s", "").toLowerCase();
        String stu = studentAnswer.replaceAll("\\s", "").toLowerCase();
        if (stu.equals(std) || (std.length() <= 10 && stu.contains(std))) {
            return "正确\n回答与标准答案一致";
        }
        return "错误\n与标准答案不符。标准答案:" + standardAnswer.trim();
    }

    @Override
    public String correctVariantWithImages(String questionContent, String standardAnswer, String studentAnswer, java.util.List<String> images) {
        // 本地规则版无法识别图片,降级到纯文本批改
        if (images != null && !images.isEmpty()) {
            return "错误\n本地规则模式无法识别图片内容,请在管理端配置通义千问 API Key 以启用 AI 图片识别批改。";
        }
        return correctVariant(questionContent, standardAnswer, studentAnswer);
    }
}
