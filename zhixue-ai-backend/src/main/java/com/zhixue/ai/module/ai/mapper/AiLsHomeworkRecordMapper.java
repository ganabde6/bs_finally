package com.zhixue.ai.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.ai.entity.AiLsHomeworkRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AiLsHomeworkRecordMapper extends BaseMapper<AiLsHomeworkRecord> {

    @Select("SELECT * FROM ai_ls_homework_record WHERE homework_id = #{homeworkId} AND student_id = #{studentId} ORDER BY create_time DESC")
    List<AiLsHomeworkRecord> selectByHomeworkAndStudent(Long homeworkId, Long studentId);

    @Select("SELECT * FROM ai_ls_homework_record WHERE homework_id = #{homeworkId} ORDER BY create_time DESC")
    List<AiLsHomeworkRecord> selectByHomeworkId(Long homeworkId);
}
