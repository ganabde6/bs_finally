package com.zhixue.ai.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.ai.entity.SelfPracticeRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface SelfPracticeRecordMapper extends BaseMapper<SelfPracticeRecord> {

    /** 查询某学生最近一次练习日期 */
    @Select("SELECT MAX(create_time) FROM self_practice_record WHERE user_id = #{userId}")
    java.time.LocalDateTime selectLatestPracticeTime(@Param("userId") Long userId);

    /** 查询某学生近N天的练习记录数 */
    @Select("SELECT COUNT(*) FROM self_practice_record WHERE user_id = #{userId} AND create_time >= #{since}")
    int countRecentPractices(@Param("userId") Long userId, @Param("since") java.time.LocalDateTime since);

    /** 查询某学生今日是否有正确率>=30%的练习记录 */
    @Select("SELECT COUNT(*) FROM self_practice_record WHERE user_id = #{userId} AND DATE(create_time) = #{today} AND accuracy >= 30")
    int countEffectivePracticeToday(@Param("userId") Long userId, @Param("today") LocalDate today);
}
