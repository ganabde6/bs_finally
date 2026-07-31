package com.zhixue.ai.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.ai.entity.AiTutorChat;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AiTutorChatMapper extends BaseMapper<AiTutorChat> {

    @Select("SELECT * FROM ai_tutor_chat WHERE student_id = #{studentId} ORDER BY create_time ASC LIMIT 100")
    List<AiTutorChat> selectByStudentId(Long studentId);
}
