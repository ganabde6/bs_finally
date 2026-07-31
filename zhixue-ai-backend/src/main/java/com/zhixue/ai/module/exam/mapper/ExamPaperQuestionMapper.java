package com.zhixue.ai.module.exam.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.exam.entity.ExamPaperQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ExamPaperQuestionMapper extends BaseMapper<ExamPaperQuestion> {

    /** 查询试卷题目列表(按顺序) */
    @Select("SELECT * FROM exam_paper_question WHERE paper_id = #{paperId} ORDER BY sort")
    List<ExamPaperQuestion> selectByPaperId(Long paperId);

    /** 删除试卷的所有题目关联 */
    @org.apache.ibatis.annotations.Delete("DELETE FROM exam_paper_question WHERE paper_id = #{paperId}")
    int deleteByPaperId(Long paperId);
}
