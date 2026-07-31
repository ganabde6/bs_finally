package com.zhixue.ai.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.ai.entity.AiCorrectRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AiCorrectRecordMapper extends BaseMapper<AiCorrectRecord> {

    /** 查询作答记录的批改详情 */
    @Select("SELECT * FROM ai_correct_record WHERE answer_id = #{answerId} ORDER BY question_id")
    List<AiCorrectRecord> selectByAnswerId(Long answerId);
}
