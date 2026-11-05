package com.zhixue.ai.module.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.exam.entity.ExamAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;
import java.util.Map;

@Mapper
public interface ExamAnswerMapper extends BaseMapper<ExamAnswer> {

    /** 查询试卷的所有作答记录 */
    @Select("SELECT * FROM exam_answer WHERE paper_id = #{paperId}")
    List<ExamAnswer> selectByPaperId(Long paperId);

    /** 查询学生的所有作答记录 */
    @Select("SELECT * FROM exam_answer WHERE student_id = #{studentId} ORDER BY create_time DESC")
    List<ExamAnswer> selectByStudentId(Long studentId);

    /** 学生作答记录(带科目):join 试卷取 subjectId,按提交/创建时间倒序 */
    @Select("SELECT a.id, a.paper_id AS paperId, a.student_id AS studentId, a.total_score AS totalScore, " +
            "a.status, a.duration, a.submit_time AS submitTime, a.create_time AS createTime, " +
            "p.paper_name AS paperName, p.subject_id AS subjectId, s.subject_name AS subjectName " +
            "FROM exam_answer a " +
            "LEFT JOIN exam_paper p ON p.id = a.paper_id " +
            "LEFT JOIN sys_subject s ON s.id = p.subject_id " +
            "WHERE a.student_id = #{studentId} " +
            "ORDER BY COALESCE(a.submit_time, a.create_time) DESC")
    List<Map<String, Object>> selectStudentAnswersWithSubject(Long studentId);

    /** 学生作答记录涉及的科目(用于筛选下拉) */
    @Select("SELECT DISTINCT p.subject_id AS subjectId, s.subject_name AS subjectName " +
            "FROM exam_answer a " +
            "LEFT JOIN exam_paper p ON p.id = a.paper_id " +
            "LEFT JOIN sys_subject s ON s.id = p.subject_id " +
            "WHERE a.student_id = #{studentId} AND p.subject_id IS NOT NULL " +
            "ORDER BY p.subject_id")
    List<Map<String, Object>> selectStudentAnswerSubjects(Long studentId);
}
