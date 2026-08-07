package com.zhixue.ai.module.ai.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 英语听说 PK 服务
 */
public interface PkLsService {

    /**
     * 创建听说 PK 挑战（自定义题目）
     * @param creatorId 创建者ID
     * @param questionTitle 题目标题
     * @param questionContent 题目内容
     * @param referenceText 参考文本
     * @param questionType 题型
     * @param scorePoints 评分要点
     * @return 房间信息（含房间号）
     */
    Map<String, Object> createChallenge(Long creatorId, String questionTitle, String questionContent,
                                        String referenceText, String questionType, String scorePoints);

    /**
     * 接受挑战
     * @param challengerId 挑战者ID
     * @param roomCode 房间号
     * @return 房间信息
     */
    Map<String, Object> acceptChallenge(Long challengerId, String roomCode);

    /**
     * 获取挑战详情
     * @param roomCode 房间号
     * @return 挑战详情（含题目）
     */
    Map<String, Object> getChallengeDetail(String roomCode);

    /**
     * 提交听说 PK 作答
     * @param studentId 学生ID
     * @param roomCode 房间号
     * @param audioFile 音频文件
     * @param supplementText 补充文字
     * @return 评分结果
     */
    Map<String, Object> submitPkAnswer(Long studentId, String roomCode, MultipartFile audioFile, String supplementText);

    /**
     * 获取 PK 结果（双方评分对比）
     * @param roomCode 房间号
     * @return 双方评分对比
     */
    Map<String, Object> getPkResult(String roomCode);

    /**
     * 获取我的 PK 挑战列表
     * @param userId 用户ID
     * @return 挑战列表
     */
    List<Map<String, Object>> listMyChallenges(Long userId);
}
