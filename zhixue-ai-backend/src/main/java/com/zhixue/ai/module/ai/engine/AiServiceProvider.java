package com.zhixue.ai.module.ai.engine;

/**
 * AI 服务提供者抽象接口
 * <p>设计目的:支持本地规则版与第三方 AI 接口(通义/文心/讯飞/OpenAI)自由切换</p>
 * <p>当前默认实现:{@link LocalRuleAiServiceProvider}</p>
 */
public interface AiServiceProvider {

    /**
     * 判定学生答案是否触发内容风控
     * @return 触发返回拦截原因,未触发返回 null
     */
    String moderateContent(String content);

    /**
     * AI 助学答疑(基于学生学情上下文)
     * @param question 学生提问
     * @param context  学情上下文(错题、薄弱点等)
     * @return AI 回答
     */
    String tutorAnswer(String question, String context);

    /**
     * 作文/简答题智能润色
     */
    String polishText(String original);

    /**
     * 生成个性化评语(家校反馈)
     */
    String generateComment(String studentName, String performance);

    /**
     * 生成学情提升建议
     */
    String generateStudySuggestion(String weakPoints, String strongPoints);
}
