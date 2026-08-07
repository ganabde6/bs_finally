package com.zhixue.ai.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.ai.entity.AiLsHomeworkQuestion;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AiLsHomeworkQuestionMapper extends BaseMapper<AiLsHomeworkQuestion> {

    @Select("SELECT * FROM ai_ls_homework_question WHERE homework_id = #{homeworkId} ORDER BY sort_order ASC")
    List<AiLsHomeworkQuestion> selectByHomeworkId(Long homeworkId);
}
