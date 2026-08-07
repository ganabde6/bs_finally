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

    /**
     * AI 生成变式题
     * @param originalQuestion 原题目内容
     * @param knowledgePoint 知识点
     * @param questionType 题型 (1单选/2多选/3判断/4填空/5简答/6作文)
     * @param variantIndex 第几道变式题(从1开始),用于区分每次生成的内容
     * @return 变式题内容(含题目、选项、答案、解析)
     */
    String generateVariant(String originalQuestion, String knowledgePoint, Integer questionType, int variantIndex);

    /**
     * 变式题作答批改
     * @param questionContent 题目内容(AI 生成的变式题可能内含【答案】【解析】段)
     * @param standardAnswer 标准答案(可能为空)
     * @param studentAnswer 学生答案
     * @return 批改结论,以「正确」或「错误」开头后接简评;无法判定返回 null
     */
    String correctVariant(String questionContent, String standardAnswer, String studentAnswer);

    /**
     * 变式题作答批改(含图片识别)
     * <p>AI 通过多模态接口识别学生上传的草稿纸照片中的解题过程和答案,结合文字答案一起判定</p>
     * @param questionContent 题目内容
     * @param standardAnswer 标准答案
     * @param studentAnswer 学生文字答案(可为空)
     * @param images 图片 base64 列表(data:image/xxx;base64,xxx 格式)
     * @return 批改结论
     */
    String correctVariantWithImages(String questionContent, String standardAnswer, String studentAnswer, java.util.List<String> images);

    /**
     * 语音识别(音频转文本)
     * <p>通过通义千问 qwen-audio-turbo 多模态模型识别学生录音中的英文内容</p>
     * @param base64Audio 音频 base64(不含 data: 前缀)
     * @param format 音频格式(wav/mp3/m4a)
     * @return 识别文本;识别失败返回 null
     */
    String recognizeAudio(String base64Audio, String format);

    /**
     * 英语听说作答评分
     * <p>将 AI 识别文本与参考答案交给文本模型,返回四项评分与改进建议</p>
     * @param recognizedText AI 识别出的学生语音文本
     * @param referenceText 参考文本/标准答案
     * @param questionContent 题目内容
     * @return JSON 字符串: {"pronunciationScore":0-25,"fluencyScore":0-25,"grammarScore":0-25,"contentScore":0-25,"totalScore":0-100,"feedback":"..."};失败返回 null
     */
    String gradeListeningSpeaking(String recognizedText, String referenceText, String questionContent);
}
