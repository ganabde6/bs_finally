package com.zhixue.ai.module.ai.engine;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 第三方 AI API 服务实现
 * <p>支持通义千问(DashScope)和 DeepSeek，通过配置切换</p>
 * <p>两个平台均兼容 OpenAI Chat Completions 格式</p>
 */
@Slf4j
@Component
public class ApiAiServiceProvider implements AiServiceProvider {

    @Value("${ai.provider:local}")
    private String provider;

    @Value("${ai.dashscope.api-key:}")
    private String dashScopeApiKey;

    @Value("${ai.dashscope.base-url:https://dashscope.aliyuncs.com/compatible-mode/v1}")
    private String dashScopeBaseUrl;

    @Value("${ai.dashscope.model:qwen-plus}")
    private String dashScopeModel;

    @Value("${ai.dashscope.vision-model:qwen-vl-plus}")
    private String dashScopeVisionModel;

    @Value("${ai.dashscope.audio-model:qwen-audio-turbo}")
    private String dashScopeAudioModel;

    @Value("${ai.deepseek.api-key:}")
    private String deepSeekApiKey;

    @Value("${ai.deepseek.base-url:https://api.deepseek.com/v1}")
    private String deepSeekBaseUrl;

    @Value("${ai.deepseek.model:deepseek-chat}")
    private String deepSeekModel;

    private final RestTemplate restTemplate = new RestTemplate();

    // ===================== 系统提示词 =====================

    private static final String TUTOR_SYSTEM_PROMPT =
            "你是「智学AI」系统的专属助学老师，面向中小学生。请遵循以下原则：\n" +
            "1. 用通俗易懂的语言讲解，避免过于学术化\n" +
            "2. 分步骤解析，逻辑清晰\n" +
            "3. 适当举例帮助理解\n" +
            "4. 鼓励学生独立思考，不要直接给答案\n" +
            "5. 如果题目信息不完整，引导学生补充\n" +
            "6. 回答控制在500字以内，重点突出";

    private static final String POLISH_SYSTEM_PROMPT =
            "你是专业的作文批改老师。请对以下作文/简答进行润色：\n" +
            "1. 纠正错别字和语法错误\n" +
            "2. 优化句子表达，使语言更流畅\n" +
            "3. 给出修改建议并说明原因\n" +
            "4. 提供润色后的完整版本\n" +
            "5. 保持原文的主题和情感不变";

    private static final String COMMENT_SYSTEM_PROMPT =
            "你是一位经验丰富的教师，需要根据学生的学情数据生成家校反馈评语。\n" +
            "要求：\n" +
            "1. 语气温暖、鼓励为主\n" +
            "2. 客观反映学习情况\n" +
            "3. 给出具体的改进建议\n" +
            "4. 字数200字左右\n" +
            "5. 体现家校共育理念";

    private static final String MODERATE_SYSTEM_PROMPT =
            "你是一个内容安全审核员。请判断以下文本是否包含违规内容（如作弊、色情、暴力、毒品、赌博、人身攻击等）。\n" +
            "如果安全，只回复「安全」两个字。\n" +
            "如果违规，回复「违规：」加上违规原因（10 字以内）。";

    private static final String VARIANT_SYSTEM_PROMPT =
            "你是一位经验丰富的命题老师，擅长设计变式题。\n" +
            "变式题的要求：\n" +
            "1. 考查相同的底层知识点，但题型或逻辑发生变化\n" +
            "2. 难度略高于原题（递进式训练）\n" +
            "3. 题目表述清晰，无歧义\n" +
            "4. 提供标准答案和简要解析\n" +
            "5. 如果是选择题，提供 4 个选项（A/B/C/D）\n" +
            "6. 输出格式：\n" +
            "   【题目】xxx\n" +
            "   【选项】A.xxx B.xxx C.xxx D.xxx（仅选择题）\n" +
            "   【答案】xxx\n" +
            "   【解析】xxx";

    private static final String GRADE_VARIANT_SYSTEM_PROMPT =
            "你是一位严格的批改老师。请判断学生对变式题的作答是否正确。\n" +
            "要求：\n" +
            "1. 第一行只输出「正确」或「错误」两个字\n" +
            "2. 第二行起给出 50 字以内的简评:正确则表扬并点出考点,错误则说明错因并给出正确答案\n" +
            "3. 主观表述题意思相近即可判正确,不必逐字一致\n" +
            "4. 不要输出其他无关内容";

    // ===================== 核心调用方法 =====================

    /**
     * 统一调用 AI API
     */
    private String callAiApi(String systemPrompt, String userMessage) {
        return callAiApi(systemPrompt, userMessage, null);
    }

    /**
     * 统一调用 AI API(支持图片)
     * @param images 图片 base64 列表(data:image/xxx;base64,xxx 格式),可为 null
     */
    private String callAiApi(String systemPrompt, String userMessage, java.util.List<String> images) {
        if ("local".equals(provider)) {
            log.warn("AI provider 为 local，未接入真实 API");
            return null;
        }

        String apiKey;
        String baseUrl;
        String model;

        if ("dashscope".equals(provider) || "tongyi".equals(provider) || "qwen".equals(provider)) {
            apiKey = dashScopeApiKey;
            baseUrl = dashScopeBaseUrl;
            // 有图片时使用视觉模型,无图片时使用文本模型
            model = (images != null && !images.isEmpty()) ? dashScopeVisionModel : dashScopeModel;
        } else if ("deepseek".equals(provider)) {
            apiKey = deepSeekApiKey;
            baseUrl = deepSeekBaseUrl;
            model = deepSeekModel;
            // DeepSeek 暂不支持多模态,有图片时返回 null
            if (images != null && !images.isEmpty()) {
                log.warn("DeepSeek 暂不支持图片识别,降级到本地规则");
                return null;
            }
        } else {
            log.error("未知的 AI provider: {}", provider);
            return null;
        }

        if (apiKey == null || apiKey.isEmpty()) {
            log.error("AI API Key 未配置 (provider={})", provider);
            return null;
        }

        try {
            // 构造请求体（OpenAI 兼容格式）
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", model);

            JSONArray messages = new JSONArray();

            JSONObject systemMsg = new JSONObject();
            systemMsg.put("role", "system");
            systemMsg.put("content", systemPrompt);
            messages.add(systemMsg);

            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");

            // 有图片时使用多模态消息格式
            if (images != null && !images.isEmpty()) {
                JSONArray contentArray = new JSONArray();
                // 文字部分
                JSONObject textPart = new JSONObject();
                textPart.put("type", "text");
                textPart.put("text", userMessage);
                contentArray.add(textPart);
                // 图片部分
                for (String img : images) {
                    JSONObject imgPart = new JSONObject();
                    imgPart.put("type", "image_url");
                    JSONObject imgUrl = new JSONObject();
                    imgUrl.put("url", img);
                    imgPart.put("image_url", imgUrl);
                    contentArray.add(imgPart);
                }
                userMsg.put("content", contentArray);
            } else {
                userMsg.put("content", userMessage);
            }
            messages.add(userMsg);

            requestBody.put("messages", messages);
            requestBody.put("temperature", 0.7);
            requestBody.put("max_tokens", 8192);

            // 设置请求头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);

            // 发送请求
            String url = baseUrl + "/chat/completions";
            log.info("调用 AI API: provider={}, model={}, url={}, 图片数={}", provider, model, url,
                    images == null ? 0 : images.size());

            ResponseEntity<String> response = restTemplate.exchange(
                    url, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject respJson = JSON.parseObject(response.getBody());
                JSONArray choices = respJson.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    if (message != null) {
                        return message.getString("content");
                    }
                }
            }

            log.error("AI API 响应异常: {}", response.getBody());
            return null;

        } catch (Exception e) {
            log.error("调用 AI API 失败: provider={}, model={}, error={}", provider, model, e.getMessage(), e);
            return null;
        }
    }

    // ===================== 接口实现 =====================

    @Override
    public String moderateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            return null;
        }
        // 本地规则快速拦截
        String localResult = new LocalRuleAiServiceProvider().moderateContent(content);
        if (localResult != null) return localResult;

        // AI 二次审核
        String result = callAiApi(MODERATE_SYSTEM_PROMPT, content);
        if (result != null && !result.contains("安全")) {
            return result;
        }
        return null;
    }

    @Override
    public String tutorAnswer(String question, String context) {
        if (question == null || question.trim().isEmpty()) {
            return "请输入您的问题";
        }

        StringBuilder userMsg = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            userMsg.append("【学生学情档案】").append(context).append("\n\n");
        }
        userMsg.append("【学生提问】").append(question);

        String result = callAiApi(TUTOR_SYSTEM_PROMPT, userMsg.toString());
        if (result != null) {
            return result;
        }
        // API 调用失败时降级到本地规则
        log.warn("AI API 调用失败，降级到本地规则回答");
        return new LocalRuleAiServiceProvider().tutorAnswer(question, context);
    }

    @Override
    public String tutorAnswerWithImages(String question, String context, java.util.List<String> images) {
        if (question == null || question.trim().isEmpty()) {
            return "请输入您的问题";
        }

        StringBuilder userMsg = new StringBuilder();
        if (context != null && !context.isEmpty()) {
            userMsg.append("【学生学情档案】").append(context).append("\n\n");
        }
        userMsg.append("【学生提问】").append(question);
        userMsg.append("\n\n请结合学生上传的图片一起回答。");

        String result = callAiApi(TUTOR_SYSTEM_PROMPT, userMsg.toString(), images);
        if (result != null) {
            return result;
        }
        // API 调用失败或模型不支持图片时降级到本地规则
        log.warn("AI 图片问答调用失败，降级到本地规则回答");
        return new LocalRuleAiServiceProvider().tutorAnswerWithImages(question, context, images);
    }

    @Override
    public String polishText(String original) {
        if (original == null || original.trim().isEmpty()) {
            return "原文为空，无法润色";
        }

        String result = callAiApi(POLISH_SYSTEM_PROMPT, "请润色以下作文/简答：\n\n" + original);
        if (result != null) {
            return result;
        }
        log.warn("AI API 调用失败，降级到本地规则润色");
        return new LocalRuleAiServiceProvider().polishText(original);
    }

    @Override
    public String polishTextWithImage(String original, java.util.List<String> images) {
        StringBuilder userMsg = new StringBuilder();
        userMsg.append("请识别图片中的手写作文内容，并结合以下文字内容进行润色：\n\n");
        if (original != null && !original.trim().isEmpty()) {
            userMsg.append("【文字内容】").append(original).append("\n\n");
        }
        userMsg.append("请输出完整的润色结果。");

        String result = callAiApi(POLISH_SYSTEM_PROMPT, userMsg.toString(), images);
        if (result != null) {
            return result;
        }
        log.warn("AI 图片润色调用失败，降级到本地规则润色");
        return new LocalRuleAiServiceProvider().polishTextWithImage(original, images);
    }

    @Override
    public String generateComment(String studentName, String performance) {
        StringBuilder userMsg = new StringBuilder();
        if (studentName != null) {
            userMsg.append("学生姓名：").append(studentName).append("\n");
        }
        if (performance != null && !performance.isEmpty()) {
            userMsg.append("学情数据：").append(performance).append("\n");
        }
        userMsg.append("请根据以上信息生成家校反馈评语。");

        String result = callAiApi(COMMENT_SYSTEM_PROMPT, userMsg.toString());
        if (result != null) {
            return result;
        }
        log.warn("AI API 调用失败，降级到本地规则评语");
        return new LocalRuleAiServiceProvider().generateComment(studentName, performance);
    }

    @Override
    public String generateStudySuggestion(String weakPoints, String strongPoints) {
        StringBuilder userMsg = new StringBuilder();
        if (strongPoints != null && !strongPoints.isEmpty()) {
            userMsg.append("优势知识点：").append(strongPoints).append("\n");
        }
        if (weakPoints != null && !weakPoints.isEmpty()) {
            userMsg.append("薄弱知识点：").append(weakPoints).append("\n");
        }
        userMsg.append("请根据以上学情数据，生成个性化的学习提升建议。");

        String result = callAiApi(TUTOR_SYSTEM_PROMPT, userMsg.toString());
        if (result != null) {
            return result;
        }
        log.warn("AI API 调用失败，降级到本地规则建议");
        return new LocalRuleAiServiceProvider().generateStudySuggestion(weakPoints, strongPoints);
    }

    @Override
    public String generateVariant(String originalQuestion, String knowledgePoint, Integer questionType, int variantIndex) {
        if (originalQuestion == null || originalQuestion.trim().isEmpty()) {
            return "原题目为空，无法生成变式题";
        }

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

        StringBuilder userMsg = new StringBuilder();
        userMsg.append("请根据以下原题目，设计第 ").append(variantIndex).append(" 道变式题：\n\n");
        userMsg.append("【原题目】\n").append(originalQuestion).append("\n\n");
        userMsg.append("【知识点】").append(knowledgePoint == null ? "相关知识点" : knowledgePoint).append("\n");
        userMsg.append("【题型】").append(typeLabel).append("\n");
        userMsg.append("【当前是第 ").append(variantIndex).append(" 道变式题，请确保与前面生成的变式题在题型或逻辑上有所不同】\n\n");
        userMsg.append("要求：\n");
        userMsg.append("1. 考查相同的底层知识点，但题型或逻辑要发生变化\n");
        userMsg.append("2. 难度略高于原题\n");
        userMsg.append("3. 如果是选择题，请提供 4 个选项（A/B/C/D）\n");
        userMsg.append("4. 提供标准答案和简要解析\n");
        userMsg.append("5. 按以下格式输出：\n");
        userMsg.append("   【题目】xxx\n");
        userMsg.append("   【选项】A.xxx B.xxx C.xxx D.xxx（仅选择题需要）\n");
        userMsg.append("   【答案】xxx\n");
        userMsg.append("   【解析】xxx");

        String result = callAiApi(VARIANT_SYSTEM_PROMPT, userMsg.toString());
        if (result != null) {
            return result;
        }
        log.warn("AI API 调用失败，降级到本地规则变式题");
        return new LocalRuleAiServiceProvider().generateVariant(originalQuestion, knowledgePoint, questionType, variantIndex);
    }

    @Override
    public String correctVariant(String questionContent, String standardAnswer, String studentAnswer) {
        if (studentAnswer == null || studentAnswer.trim().isEmpty()) {
            return "错误\n未作答";
        }

        StringBuilder userMsg = new StringBuilder();
        userMsg.append("【题目】\n").append(questionContent).append("\n\n");
        if (standardAnswer != null && !standardAnswer.trim().isEmpty()) {
            userMsg.append("【标准答案】\n").append(standardAnswer).append("\n\n");
        }
        userMsg.append("【学生答案】\n").append(studentAnswer);

        String result = callAiApi(GRADE_VARIANT_SYSTEM_PROMPT, userMsg.toString());
        if (result != null) {
            return result;
        }
        log.warn("AI API 调用失败，降级到本地规则批改变式题");
        return new LocalRuleAiServiceProvider().correctVariant(questionContent, standardAnswer, studentAnswer);
    }

    @Override
    public String correctVariantWithImages(String questionContent, String standardAnswer, String studentAnswer, java.util.List<String> images) {
        // 构建系统提示词：要求 AI 识别图片中的解题过程
        String imageGradePrompt = GRADE_VARIANT_SYSTEM_PROMPT +
                "\n\n特别注意：学生上传了草稿纸照片，请仔细识别图片中的手写解题过程和答案。" +
                "如果图片中有清晰的解题步骤，请逐步检查每一步是否正确，然后给出最终判定。" +
                "如果图片模糊无法识别，请根据文字答案进行判定，并在简评中说明图片无法识别。";

        StringBuilder userMsg = new StringBuilder();
        userMsg.append("【题目】\n").append(questionContent).append("\n\n");
        if (standardAnswer != null && !standardAnswer.trim().isEmpty()) {
            userMsg.append("【标准答案】\n").append(standardAnswer).append("\n\n");
        }
        if (studentAnswer != null && !studentAnswer.trim().isEmpty()) {
            userMsg.append("【学生文字答案】\n").append(studentAnswer).append("\n\n");
        }
        userMsg.append("学生还上传了 ").append(images.size()).append(" 张草稿纸照片，请识别其中的解题过程和答案。");

        String result = callAiApi(imageGradePrompt, userMsg.toString(), images);
        if (result != null) {
            return result;
        }
        // AI 图片识别失败，降级到纯文本批改
        log.warn("AI 图片识别批改失败，降级到纯文本批改");
        return correctVariant(questionContent, standardAnswer, studentAnswer);
    }

    // ===================== 英语听说(语音识别 + 评分) =====================

    private static final String ASR_SYSTEM_PROMPT =
            "你是专业的英语语音识别引擎。请将用户提供的音频中的英文内容完整、准确地转写成文本。\n" +
            "要求：\n" +
            "1. 只输出转写出的英文文本，不要输出任何其他解释或备注\n" +
            "2. 保留正确的单词拼写、标点和大小写\n" +
            "3. 如果音频中有口误、重复或停顿词(如 um/uh)，保留原样但去掉无意义的语气词\n" +
            "4. 如果音频为空或无法识别，只输出「无法识别」";

    private static final String GRADE_LISTENING_SPEAKING_SYSTEM_PROMPT =
            "你是一位专业的高考英语听说考试阅卷老师。请根据学生的语音识别文本，对照参考文本，从四个方面评分：\n" +
            "1. pronunciationScore(发音,满分25): 依据识别文本与参考文本的单词发音匹配程度\n" +
            "2. fluencyScore(流利度,满分25): 依据文本长度、句子完整性、有无明显卡顿或重复\n" +
            "3. grammarScore(语法,满分25): 依据句式结构、时态、主谓一致、冠词介词使用\n" +
            "4. contentScore(内容,满分25): 依据要点覆盖程度，内容是否完整传达参考文本核心信息\n" +
            "评分要求：\n" +
            "1. 四项均为 0-25 的整数或一位小数\n" +
            "2. 学生识别文本与参考文本完全一致时，各项给 22-25 分\n" +
            "3. 部分匹配(大意正确但用词/句式不同)时，给 15-21 分\n" +
            "4. 严重偏离或几乎未作答时，给 0-10 分\n" +
            "5. 最后输出 feedback(改进建议)，用中文写 100-150 字，指出发音/流利度/语法/内容方面的具体问题与提升方法\n" +
            "6. 只输出 JSON，格式：{\"pronunciationScore\":0,\"fluencyScore\":0,\"grammarScore\":0,\"contentScore\":0,\"feedback\":\"\"}，不要输出任何其他内容";

    /**
     * 调用通义千问 qwen-audio-turbo 语音识别
     * @param base64Audio 音频 base64(不含 data: 前缀)
     * @param format 音频格式(wav/mp3/m4a)
     * @return 识别文本;失败返回 null
     */
    private String callAiAudioApi(String base64Audio, String format) {
        if ("local".equals(provider)) {
            log.warn("AI provider 为 local，未接入真实 API，无法语音识别");
            return null;
        }
        if (dashScopeApiKey == null || dashScopeApiKey.isEmpty()) {
            log.error("DashScope API Key 未配置，无法语音识别");
            return null;
        }
        try {
            JSONObject requestBody = new JSONObject();
            requestBody.put("model", dashScopeAudioModel);

            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            JSONArray contentArray = new JSONArray();
            // 音频部分(OpenAI 兼容的 input_audio 格式,data 必须为 Data URL: data:audio/<mediatype>;base64,<data>)
            JSONObject audioPart = new JSONObject();
            audioPart.put("type", "input_audio");
            JSONObject audioObj = new JSONObject();
            audioObj.put("data", toAudioDataUrl(base64Audio, format));
            audioPart.put("input_audio", audioObj);
            contentArray.add(audioPart);
            userMsg.put("content", contentArray);
            messages.add(userMsg);

            requestBody.put("messages", messages);
            requestBody.put("max_tokens", 2048);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + dashScopeApiKey);

            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);
            String url = dashScopeBaseUrl + "/chat/completions";
            log.info("调用 AI 语音识别: model={}, format={}", dashScopeAudioModel, format);

            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JSONObject respJson = JSON.parseObject(response.getBody());
                JSONArray choices = respJson.getJSONArray("choices");
                if (choices != null && !choices.isEmpty()) {
                    JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                    if (message != null) {
                        return message.getString("content");
                    }
                }
            }
            log.error("AI 语音识别响应异常: {}", response.getBody());
            return null;
        } catch (Exception e) {
            log.error("调用 AI 语音识别失败: error={}", e.getMessage(), e);
            return null;
        }
    }

    @Override
    public String recognizeAudio(String base64Audio, String format) {
        if (base64Audio == null || base64Audio.isEmpty()) {
            return null;
        }
        return callAiAudioApi(base64Audio, format);
    }

    /**
     * 将音频 base64 转为 Data URL 格式(data:audio/<mediatype>;base64,<data>),
     * Qwen-ASR 的 OpenAI 兼容模式要求 input_audio.data 使用该格式。
     */
    private String toAudioDataUrl(String base64Audio, String format) {
        String mime;
        if ("mp3".equalsIgnoreCase(format)) {
            mime = "audio/mpeg";
        } else if ("m4a".equalsIgnoreCase(format)) {
            mime = "audio/mp4";
        } else {
            mime = "audio/wav";
        }
        return "data:" + mime + ";base64," + base64Audio;
    }

    @Override
    public String gradeListeningSpeaking(String recognizedText, String referenceText, String questionContent) {
        if (recognizedText == null || recognizedText.trim().isEmpty()) {
            return null;
        }
        StringBuilder userMsg = new StringBuilder();
        userMsg.append("【题目内容】\n").append(questionContent == null ? "英语听说练习" : questionContent).append("\n\n");
        if (referenceText != null && !referenceText.trim().isEmpty()) {
            userMsg.append("【参考文本】\n").append(referenceText).append("\n\n");
        }
        userMsg.append("【学生语音识别文本】\n").append(recognizedText);

        String result = callAiApi(GRADE_LISTENING_SPEAKING_SYSTEM_PROMPT, userMsg.toString());
        if (result != null) {
            return result;
        }
        log.warn("AI 听说评分失败，降级到本地规则评分");
        return new LocalRuleAiServiceProvider().gradeListeningSpeaking(recognizedText, referenceText, questionContent);
    }

    // ===================== 英语听说 AI 出题 =====================

    private static final String LS_GENERATE_SYSTEM_PROMPT =
            "你是一位专业的英语听说考试命题老师。请根据用户提供的素材生成一道完整的英语听说练习题。\n" +
            "输出必须为严格的 JSON 格式，不要输出任何其他内容：\n" +
            "{\"title\":\"题目标题\",\"content\":\"题目说明和任务描述(英文)\",\"referenceText\":\"参考文本/标准答案(英文)\",\"questionType\":\"题型\",\"difficulty\":1-3,\"scorePoints\":\"评分要点(中文,JSON数组格式)\"}\n" +
            "要求：\n" +
            "1. content 是给学生看的题目说明，用英文写，描述任务要求\n" +
            "2. referenceText 是标准参考文本，用英文写\n" +
            "3. questionType 必须是以下之一：模仿朗读、故事复述、角色扮演\n" +
            "4. difficulty: 1=简单, 2=中等, 3=困难\n" +
            "5. scorePoints 是中文的评分要点，JSON 数组格式，如 [\"发音准确\",\"流利度好\",\"内容完整\"]";

    @Override
    public String generateLsFromText(String text, String questionType, Integer gradeLevel) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        String gradeLabel = gradeLevel == 1 ? "小学" : gradeLevel == 2 ? "初中" : "高中";
        StringBuilder userMsg = new StringBuilder();
        userMsg.append("请根据以下英文文本，生成一道").append(gradeLabel).append("水平的英语听说练习题：\n\n");
        userMsg.append("【原始文本】\n").append(text).append("\n\n");
        userMsg.append("【题型要求】").append(questionType == null ? "模仿朗读" : questionType).append("\n");
        userMsg.append("【学段】").append(gradeLabel).append("\n");
        userMsg.append("请输出严格的 JSON 格式。");

        String result = callAiApi(LS_GENERATE_SYSTEM_PROMPT, userMsg.toString());
        if (result != null) return result;
        log.warn("AI 文本出题失败");
        return null;
    }

    @Override
    public String generateLsFromTopic(String topic, String questionType, Integer difficulty, Integer gradeLevel) {
        String gradeLabel = gradeLevel == 1 ? "小学" : gradeLevel == 2 ? "初中" : "高中";
        String diffLabel = difficulty == 1 ? "简单" : difficulty == 2 ? "中等" : "困难";
        StringBuilder userMsg = new StringBuilder();
        userMsg.append("请生成一道").append(gradeLabel).append("水平的英语听说练习题：\n\n");
        userMsg.append("【话题】").append(topic == null ? "日常生活" : topic).append("\n");
        userMsg.append("【题型】").append(questionType == null ? "模仿朗读" : questionType).append("\n");
        userMsg.append("【难度】").append(diffLabel).append("\n");
        userMsg.append("请输出严格的 JSON 格式。");

        String result = callAiApi(LS_GENERATE_SYSTEM_PROMPT, userMsg.toString());
        if (result != null) return result;
        log.warn("AI 话题出题失败");
        return null;
    }

    @Override
    public String generateLsFromImage(String imageBase64, String questionType, Integer gradeLevel) {
        String gradeLabel = gradeLevel == 1 ? "小学" : gradeLevel == 2 ? "初中" : "高中";
        String userMsg = "请观察这张图片，为" + gradeLabel + "学生生成一道英语听说练习题。\n" +
                "题型要求：" + (questionType == null ? "看图描述/故事复述" : questionType) + "\n" +
                "请输出严格的 JSON 格式。";

        List<String> images = new ArrayList<>();
        if (imageBase64 != null && !imageBase64.isEmpty()) {
            // 确保 base64 有 data URI 前缀
            String img = imageBase64.startsWith("data:") ? imageBase64 : "data:image/png;base64," + imageBase64;
            images.add(img);
        }

        String result = callAiApi(LS_GENERATE_SYSTEM_PROMPT, userMsg, images);
        if (result != null) return result;
        log.warn("AI 图片出题失败");
        return null;
    }

    @Override
    public String generateSimilarLs(String previousQuestion, String questionType, String topic, Integer gradeLevel) {
        String gradeLabel = gradeLevel == 1 ? "小学" : gradeLevel == 2 ? "初中" : "高中";
        StringBuilder userMsg = new StringBuilder();
        userMsg.append("请基于以下已完成的听说题目，生成一道同类型的新题（话题相似、难度相当）：\n\n");
        userMsg.append("【已完成题目】\n").append(previousQuestion == null ? "英语听说练习" : previousQuestion).append("\n\n");
        userMsg.append("【题型】").append(questionType == null ? "模仿朗读" : questionType).append("\n");
        if (topic != null && !topic.isEmpty()) {
            userMsg.append("【话题】").append(topic).append("\n");
        }
        userMsg.append("【学段】").append(gradeLabel).append("\n");
        userMsg.append("请输出严格的 JSON 格式。");

        String result = callAiApi(LS_GENERATE_SYSTEM_PROMPT, userMsg.toString());
        if (result != null) return result;
        log.warn("AI 同类出题失败");
        return null;
    }

    @Override
    public String generateLsHomework(String mode, String params) {
        StringBuilder userMsg = new StringBuilder();
        userMsg.append("请作为英语听说考试命题老师，根据以下组题要求生成一套听说练习题（3-5道）：\n\n");
        userMsg.append("【组题模式】").append(mode == null ? "STANDARD" : mode).append("\n");
        userMsg.append("【组题参数】\n").append(params == null ? "{}" : params).append("\n\n");
        userMsg.append("请输出严格的 JSON 数组格式，每道题格式如下：\n");
        userMsg.append("[{\"title\":\"...\",\"content\":\"...\",\"referenceText\":\"...\",\"questionType\":\"...\",\"difficulty\":1-3,\"scorePoints\":\"...\"}, ...]\n");
        userMsg.append("题型必须从以下选择：模仿朗读、故事复述、角色扮演");

        String result = callAiApi(LS_GENERATE_SYSTEM_PROMPT, userMsg.toString());
        if (result != null) return result;
        log.warn("AI 组题失败");
        return null;
    }
}
