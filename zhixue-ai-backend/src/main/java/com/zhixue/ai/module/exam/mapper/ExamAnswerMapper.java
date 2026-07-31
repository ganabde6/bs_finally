package com.zhixue.ai.module.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.exam.entity.ExamAnswer;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ExamAnswerMapper extends BaseMapper<ExamAnswer> {

    /** 查询试卷的所有作答记录 */
    @Select("SELECT * FROM exam_answer WHERE paper_id = #{paperId}")
    List<ExamAnswer> selectByPaperId(Long paperId);

    /** 查询学生的所有作答记录 */
    @Select("SELECT * FROM exam_answer WHERE student_id = #{studentId} ORDER BY create_time DESC")
    List<ExamAnswer> selectByStudentId(Long studentId);
}
