package com.zhixue.ai.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.ai.entity.UserCheckIn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface UserCheckInMapper extends BaseMapper<UserCheckIn> {

    /** 查询某用户今日是否已打卡 */
    @Select("SELECT id FROM user_check_in WHERE user_id = #{userId} AND check_in_date = #{date} LIMIT 1")
    Long selectTodayCheckIn(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** 查询某用户最近一次打卡记录 */
    @Select("SELECT * FROM user_check_in WHERE user_id = #{userId} ORDER BY check_in_date DESC LIMIT 1")
    UserCheckIn selectLatestCheckIn(@Param("userId") Long userId);

    /** 查询某用户近N天的打卡天数 */
    @Select("SELECT COUNT(*) FROM user_check_in WHERE user_id = #{userId} AND check_in_date >= #{since}")
    int countRecentCheckIns(@Param("userId") Long userId, @Param("since") LocalDate since);

    /** 查询某用户最近一次打卡日期 */
    @Select("SELECT MAX(check_in_date) FROM user_check_in WHERE user_id = #{userId}")
    LocalDate selectLatestCheckInDate(@Param("userId") Long userId);

    /** 查询班级学生的打卡统计 */
    @Select("SELECT user_id, COUNT(*) as check_in_days, MAX(check_in_date) as last_check_in_date " +
            "FROM user_check_in WHERE user_id IN (#{userIds}) AND check_in_date >= #{since} " +
            "GROUP BY user_id")
    List<java.util.Map<String, Object>> selectClassCheckInStats(@Param("userIds") List<Long> userIds, @Param("since") LocalDate since);
}
