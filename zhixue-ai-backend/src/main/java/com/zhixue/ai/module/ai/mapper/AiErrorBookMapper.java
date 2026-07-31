package com.zhixue.ai.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.ai.entity.AiErrorBook;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface AiErrorBookMapper extends BaseMapper<AiErrorBook> {

    /** 查询学生错题本 */
    @Select("SELECT * FROM ai_error_book WHERE student_id = #{studentId} ORDER BY create_time DESC")
    List<AiErrorBook> selectByStudentId(Long studentId);
}
