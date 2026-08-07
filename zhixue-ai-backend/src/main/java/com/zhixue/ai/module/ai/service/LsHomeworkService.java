package com.zhixue.ai.module.ai.service;

import com.zhixue.ai.module.ai.entity.AiLsHomework;
import com.zhixue.ai.module.ai.entity.AiLsHomeworkQuestion;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 教师英语听说作业服务
 */
public interface LsHomeworkService {

    /** 创建作业(草稿) */
    Long createHomework(Long teacherId, AiLsHomework homework);

    /** AI 生成题目 */
    List<Map<String, Object>> generateQuestions(String mode, String params);

    /** 保存题目到作业 */
    void saveQuestions(Long homeworkId, List<AiLsHomeworkQuestion> questions);

    /** 获取作业详情(含题目) */
    Map<String, Object> getHomeworkDetail(Long homeworkId);

    /** 发布作业 */
    void publishHomework(Long homeworkId, Long teacherId);

    /** 教师作业列表 */
    List<Map<String, Object>> listTeacherHomework(Long teacherId);

    /** 学生查看收到的听说作业 */
    List<Map<String, Object>> listStudentHomework(Long studentId);

    /** 学生提交听说作业作答 */
    Map<String, Object> submitHomeworkAnswer(Long studentId, Long questionId, MultipartFile audioFile, String supplementText);

    /** 学生查看某次作业的全部作答记录 */
    List<Map<String, Object>> listStudentHomeworkRecords(Long studentId, Long homeworkId);

    /** 教师查看班级作业报告 */
    List<Map<String, Object>> getClassHomeworkReport(Long homeworkId, Long teacherId);

    /** 复制历史作业 */
    Long copyHomework(Long homeworkId, Long teacherId, boolean regenerate);
}
