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
     * AI 助学答疑(含图片识别)
     * <p>AI 通过多模态接口识别学生上传的图片内容,结合文字问题一起回答</p>
     * @param question 学生提问
     * @param context  学情上下文
     * @param images   图片 base64 列表(data:image/xxx;base64,xxx 格式)
     * @return AI 回答
     */
    String tutorAnswerWithImages(String question, String context, java.util.List<String> images);

    /**
     * 作文/简答题智能润色
     */
    String polishText(String original);

    /**
     * 作文/简答题智能润色(含图片识别)
     * <p>AI 通过多模态接口识别学生上传的手写作文图片,结合文字内容进行润色</p>
     * @param original 文字内容(可为空)
     * @param images   图片 base64 列表(data:image/xxx;base64,xxx 格式)
     * @return 润色结果
     */
    String polishTextWithImage(String original, java.util.List<String> images);

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

    /**
     * AI 生成英语听说题目(自定义文本出题)
     * <p>学生粘贴英文文本,选择题型,AI 生成完整听说练习题目</p>
     * @param text 学生输入的英文文本
     * @param questionType 题型(模仿朗读/故事复述/角色扮演)
     * @param gradeLevel 学段
     * @return JSON: {"title":"...","content":"...","referenceText":"...","questionType":"...","difficulty":1,"scorePoints":"..."}
     */
    String generateLsFromText(String text, String questionType, Integer gradeLevel);

    /**
     * AI 按话题生成听说题目
     * @param topic 话题(如:旅行、动物)
     * @param questionType 题型
     * @param difficulty 难度(1-3)
     * @param gradeLevel 学段
     * @return JSON: {"title":"...","content":"...","referenceText":"...","questionType":"...","difficulty":1,"scorePoints":"..."}
     */
    String generateLsFromTopic(String topic, String questionType, Integer difficulty, Integer gradeLevel);

    /**
     * AI 基于图片生成听说题目
     * @param imageBase64 图片 base64
     * @param questionType 题型
     * @param gradeLevel 学段
     * @return JSON: {"title":"...","content":"...","referenceText":"...","questionType":"...","difficulty":1,"scorePoints":"..."}
     */
    String generateLsFromImage(String imageBase64, String questionType, Integer gradeLevel);

    /**
     * AI 生成同类薄弱练习
     * <p>基于已完成题目的话题/题型/难度,生成同类型新题</p>
     * @param previousQuestion 上一题内容
     * @param questionType 题型
     * @param topic 话题
     * @param gradeLevel 学段
     * @return JSON: {"title":"...","content":"...","referenceText":"...","questionType":"...","difficulty":1,"scorePoints":"..."}
     */
    String generateSimilarLs(String previousQuestion, String questionType, String topic, Integer gradeLevel);

    /**
     * 教师AI组题(批量生成)
     * @param mode 组题模式(STANDARD/TOPIC/CLASS_ANALYSIS/CUSTOM)
     * @param params 组题参数JSON
     * @return JSON数组: [{"title":"...","content":"...","referenceText":"...","questionType":"...","difficulty":1,"scorePoints":"..."}, ...]
     */
    String generateLsHomework(String mode, String params);

    /**
     * AI 按学科+知识点+题型+难度批量生成普通学科新题
     * <p>用于"AI智能组卷"完全由大模型出题的场景,一次性生成 count 道题目</p>
     * @param subjectName 学科名称(如"数学")
     * @param knowledgePoints 知识点列表(可空)
     * @param questionTypes 题型列表(1单选/2多选/3判断/4填空/5简答/7计算)
     * @param difficulty 难度(1-5;0或null表示混合难度)
     * @param count 生成题数
     * @param existingQuestions 已出过的题干列表(可空),AI 应避免重复出这些题
     * @return JSON数组字符串,每个元素含 content/options/standardAnswer/analysis/questionType/difficulty/knowledgePoint 字段;调用失败返回 null
     */
    String generateExamQuestions(String subjectName, java.util.List<String> knowledgePoints,
                                 java.util.List<Integer> questionTypes, Integer difficulty, int count,
                                 java.util.List<String> existingQuestions);
}
