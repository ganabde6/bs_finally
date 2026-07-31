package com.zhixue.ai.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.ai.entity.ExamRiskLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface ExamRiskLogMapper extends BaseMapper<ExamRiskLog> {

    @Select("SELECT * FROM exam_risk_log WHERE answer_id = #{answerId} ORDER BY create_time DESC")
    List<ExamRiskLog> selectByAnswerId(Long answerId);
}
