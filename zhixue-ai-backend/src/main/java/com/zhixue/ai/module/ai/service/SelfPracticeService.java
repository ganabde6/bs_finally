package com.zhixue.ai.module.ai.service;

import java.util.List;
import java.util.Map;

/**
 * 自主智练与自律打卡服务接口
 */
public interface SelfPracticeService {

    /**
     * 智能生成练习题目
     * @param userId 学生ID
     * @return 题目列表(含标准答案)
     */
    List<Map<String, Object>> generatePractice(Long userId);

    /**
     * AI 智能组卷（支持参数化配置）
     * @param userId 学生ID
     * @param config 组卷配置参数
     * @return 包含题目列表的 Map
     */
    Map<String, Object> generatePracticeByConfig(Long userId, Map<String, Object> config);

    /**
     * 获取学科下的知识点列表
     * @param subjectId 学科ID
     * @return 知识点列表
     */
    List<String> getKnowledgePointsBySubject(Long subjectId);

    /**
     * 获取最近练习记录
     * @param userId 学生ID
     * @return 最近练习记录列表
     */
    List<Map<String, Object>> getRecentPracticeRecords(Long userId);

    /**
     * 提交批改
     * @param userId 学生ID
     * @param questionAnswers 题目ID->用户答案
     * @param durationSeconds 练习耗时
     * @return 批改明细列表
     */
    List<Map<String, Object>> submitPractice(Long userId, List<Map<String, Object>> questionAnswers, Integer durationSeconds);

    /**
     * 打卡
     * @param userId 学生ID
     * @return 打卡结果
     */
    Map<String, Object> doCheckIn(Long userId);

    /**
     * 查询打卡状态
     * @param userId 学生ID
     * @return 打卡状态信息
     */
    Map<String, Object> getCheckInStatus(Long userId);

    /**
     * 班级自主学习概况
     * @param classId 班级ID
     * @return 学生列表及统计
     */
    List<Map<String, Object>> getClassSelfStudyStats(Long classId);
}
