package com.zhixue.ai.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 同学PK服务接口
 */
public interface PkService {

    /**
     * 创建PK房间
     * @param userId 创建者ID
     * @param subjectId 学科ID
     * @param questionCount 题目数量
     * @param timeLimitSeconds 限时(秒)
     * @return 房间信息(含房间号)
     */
    Map<String, Object> createRoom(Long userId, Long subjectId, Integer questionCount, Integer timeLimitSeconds);

    /**
     * 加入PK房间
     * @param userId 用户ID
     * @param roomCode 房间号
     * @return 房间信息
     */
    Map<String, Object> joinRoom(Long userId, String roomCode);

    /**
     * 获取房间题目
     * @param userId 用户ID
     * @param roomCode 房间号
     * @return 题目列表
     */
    List<Map<String, Object>> getRoomQuestions(Long userId, String roomCode);

    /**
     * 提交PK答案
     * @param userId 用户ID
     * @param roomCode 房间号
     * @param questionId 题目ID
     * @param userAnswer 用户答案
     * @return 批改结果
     */
    Map<String, Object> submitAnswer(Long userId, String roomCode, Long questionId, String userAnswer);

    /**
     * 查询房间排名
     * @param roomCode 房间号
     * @return 成员排名列表
     */
    List<Map<String, Object>> getRoomRanking(String roomCode);

    /**
     * 获取房间状态
     * @param roomCode 房间号
     * @return 房间状态信息
     */
    Map<String, Object> getRoomStatus(String roomCode);
}
