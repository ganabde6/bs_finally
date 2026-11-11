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
                return "包含违规关键词:「" + kw + "」";
            }
        }
        return null;
    }

    @Override
    public String guideAnswer(String question) {
        if (question == null || question.trim().isEmpty()) {
            return "你好!我是智汇云课堂的 AI 向导,有什么网站操作上的问题都可以问我哦。";
        }
        String q = question.toLowerCase();
        // 注册/登录
        if (q.contains("注册") || q.contains("报名")) {
            return "注册学生账号很简单:\n" +
                   "1. 在登录页切换到「注册」标签\n" +
                   "2. 填写姓名、学段(小学/初中),选择所在班级\n" +
                   "3. 设置登录账号和密码,点击注册\n" +
                   "4. 注册成功后用新账号登录,即可进入学生首页\n" +
                   "(教师账号需要管理员在「用户管理」中创建)";
        }
        if (q.contains("登录") || q.contains("登陆")) {
            return "登录步骤:\n" +
                   "1. 打开网站首页,进入登录页\n" +
                   "2. 输入账号和密码\n" +
                   "3. 点击「登录」按钮\n" +
                   "4. 系统会根据你的角色(学生/教师/管理员)自动跳转到对应首页";
        }
        // 功能总览
        if (q.contains("功能") || q.contains("介绍") || q.contains("有什么")) {
            return "智汇云课堂三大端功能:\n" +
                   "【学生端】学习首页、作业/考试作答、作答记录、AI助学问答、英语听说、错题本、学情中心、自主智练与自律打卡\n" +
                   "【教师端】题库管理、智能出题、作业考试管理、批改管理、班级学情、家校反馈、听说作业\n" +
                   "【管理端】数据大屏、用户/班级/课程管理、公告管理、内容风控、系统日志\n" +
                   "具体哪个功能怎么用,可以直接问我哦!";
        }
        // 学生端操作
        if (q.contains("作业") || q.contains("考试") || q.contains("做题") || q.contains("作答")) {
            return "学生做作业的步骤:\n" +
                   "1. 登录后进入「学习首页」,查看今日任务\n" +
                   "2. 点击左侧「作业/考试」,找到教师布置的作业或试卷\n" +
                   "3. 点击「开始作答」,在线完成题目\n" +
                   "4. 提交后 AI 自动批改,成绩和解析会显示在「作答记录」里\n" +
                   "答错的题目会自动进入「错题本」,可以生成变式题巩固";
        }
        if (q.contains("ai助学") || q.contains("提问") || q.contains("答疑") || q.contains("问问题")) {
            return "使用 AI 助学的步骤:\n" +
                   "1. 点击左侧菜单「AI助学」\n" +
                   "2. 在对话框输入你的问题,也可以拍照上传题目\n" +
                   "3. AI 老师会结合你的学情给出讲解\n" +
                   "4. 还可以用「作文润色」功能优化作文\n" +
                   "历史对话记录会保留在页面里,随时回看";
        }
        if (q.contains("听说") || q.contains("英语") || q.contains("朗读") || q.contains("发音")) {
            return "英语听说练习步骤:\n" +
                   "1. 点击左侧「英语听说」,选择一个听说题目\n" +
                   "2. 点击「开始练习」,先听录音\n" +
                   "3. 点击录音按钮,跟着读/说\n" +
                   "4. 提交后 AI 会识别你的语音并打分(发音、流利度、语法、内容)\n" +
                   "5. 也可以点「自主出题」,用自己的文本或话题生成新题";
        }
        if (q.contains("错题")) {
            return "错题本使用步骤:\n" +
                   "1. 点击左侧「错题本」,系统自动收录你答错的题\n" +
                   "2. 点击某道错题查看原解析\n" +
                   "3. 点「生成变式题」,AI 会出同考点的新题\n" +
                   "4. 作答变式题,系统自动批改\n" +
                   "掌握的题可以标记为「已掌握」";
        }
        if (q.contains("打卡") || q.contains("自律") || q.contains("智练")) {
            return "自主智练与打卡步骤:\n" +
                   "1. 点击左侧「自主智练」\n" +
                   "2. 选择学科和知识点,AI 会生成针对性练习\n" +
                   "3. 完成练习后在「自律打卡」里打卡记录\n" +
                   "4. 坚持打卡可以培养学习习惯,学情中心会统计你的坚持情况";
        }
        if (q.contains("学情")) {
            return "查看学情的步骤:\n" +
                   "1. 学生:点击左侧「学情中心」,选择学科查看薄弱点、强项和提升建议\n" +
                   "2. 教师:点击「班级学情」查看全班答题统计与薄弱环节\n" +
                   "3. 教师还可以在「家校反馈」生成评语,推送到企业微信群";
        }
        // 教师端
        if (q.contains("组卷") || q.contains("出题") || q.contains("题库") || q.contains("命题")) {
            return "教师出题组卷步骤:\n" +
                   "1. 「题库管理」可以手动添加、整理题目\n" +
                   "2. 「智能出题」选择学科、知识点、难度,AI 一键生成题目\n" +
                   "3. 进入「作业考试管理」,点「组卷」把题目拼成试卷\n" +
                   "4. 保存后「发布」给学生,学生端就能看到并作答";
        }
        if (q.contains("批改")) {
            return "教师批改步骤:\n" +
                   "1. 学生提交后 AI 自动批改客观题\n" +
                   "2. 点击「批改管理」查看待复核的作答\n" +
                   "3. 点进「批改详情」,对 AI 批改结果人工复核或修改\n" +
                   "4. 复核完成后学生端即可看到最终成绩和解析";
        }
        if (q.contains("反馈") || q.contains("家长") || q.contains("微信")) {
            return "家校反馈步骤:\n" +
                   "1. 点击「家校反馈」,选择学生\n" +
                   "2. AI 根据该生学情自动生成反馈评语\n" +
                   "3. 可以修改后点「推送到企业微信群」\n" +
                   "4. 首次使用需先在企业微信群里添加群机器人,复制 Webhook 地址填入";
        }
        // 默认:能力清单
        return "我是智汇云课堂 AI 向导,可以回答这些问题:\n" +
               "· 如何注册/登录\n" +
               "· 网站有哪些功能\n" +
               "· 学生端:做作业、AI助学、英语听说、错题本、自主智练、学情中心\n" +
               "· 教师端:组卷出题、批改、班级学情、家校反馈\n" +
               "把你的问题发给我吧!(当前为离线模式,回答基于预设规则,接入 AI 服务后可自由问答)";
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
    public String tutorAnswerWithImages(String question, String context, java.util.List<String> images) {
        // 本地规则版:忽略图片,基于文字做回答
        if (images != null && !images.isEmpty()) {
            return "本地规则模式无法识别图片,请在管理端配置通义千问 API Key 以启用 AI 图片识别。";
        }
        return tutorAnswer(question, context);
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
        sb.append("  ").append(polished);
        return sb.toString();
    }

    @Override
    public String polishTextWithImage(String original, java.util.List<String> images) {
        if (images != null && !images.isEmpty()) {
            return "本地规则模式无法识别手写作文图片,请在管理端配置通义千问 API Key 以启用 AI 图片识别润色。";
        }
        return polishText(original);
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

    @Override
    public String recognizeAudio(String base64Audio, String format) {
        // 本地规则版无法进行语音识别,返回 null 由上层提示配置 API
        return null;
    }

    @Override
    public String gradeListeningSpeaking(String recognizedText, String referenceText, String questionContent) {
        // 本地规则版:基于文本相似度做基础评分,无法精确判断语音质量
        if (recognizedText == null || recognizedText.trim().isEmpty()) {
            return null;
        }
        String std = (referenceText == null ? "" : referenceText)
                .replaceAll("[^a-zA-Z\\s]", "").toLowerCase().trim();
        String stu = recognizedText.replaceAll("[^a-zA-Z\\s]", "").toLowerCase().trim();
        if (std.isEmpty() || stu.isEmpty()) {
            return "{\"pronunciationScore\":10,\"fluencyScore\":10,\"grammarScore\":10,\"contentScore\":5,\"feedback\":\"本地规则模式无法精确评分,请在管理端配置通义千问 API Key 以启用 AI 听说评分。\"}";
        }
        // 单词重叠率作为内容/发音近似度
        java.util.Set<String> stdWords = new java.util.HashSet<>(java.util.Arrays.asList(std.split("\\s+")));
        java.util.Set<String> stuWords = new java.util.HashSet<>(java.util.Arrays.asList(stu.split("\\s+")));
        if (stdWords.isEmpty() || stuWords.isEmpty()) {
            return null;
        }
        long hit = stdWords.stream().filter(stuWords::contains).count();
        double ratio = (double) hit / stdWords.size();
        int content = (int) Math.round(ratio * 25);
        int pronunciation = (int) Math.round(Math.min(1.0, ratio * 1.2) * 25);
        int fluency = Math.min(25, (int) Math.round((stu.split("\\s+").length / (double) Math.max(1, stdWords.size())) * 25));
        int grammar = Math.min(25, content + 2);
        String feedback = "本地规则模式评分(基于文本相似度)。建议配置通义千问 API Key 后获得更精准的发音/流利度评分。";
        return "{\"pronunciationScore\":" + pronunciation + ",\"fluencyScore\":" + fluency
                + ",\"grammarScore\":" + grammar + ",\"contentScore\":" + content
                + ",\"feedback\":\"" + feedback + "\"}";
    }

    @Override
    public String generateLsFromText(String text, String questionType, Integer gradeLevel) {
        return null; // 本地规则模式不支持 AI 出题
    }

    @Override
    public String generateLsFromTopic(String topic, String questionType, Integer difficulty, Integer gradeLevel) {
        return null;
    }

    @Override
    public String generateLsFromImage(String imageBase64, String questionType, Integer gradeLevel) {
        return null;
    }

    @Override
    public String generateSimilarLs(String previousQuestion, String questionType, String topic, Integer gradeLevel) {
        return null;
    }

    @Override
    public String generateLsHomework(String mode, String params) {
        return null;
    }

    @Override
    public String generateExamQuestions(String subjectName, java.util.List<String> knowledgePoints,
                                        java.util.List<Integer> questionTypes, Integer difficulty, int count,
                                        java.util.List<String> existingQuestions, String difficultyRatio,
                                        String typeCountsDesc) {
        // 本地规则模式无法生成新题,返回 null 由调用方回退题库抽题
        return null;
    }
}
