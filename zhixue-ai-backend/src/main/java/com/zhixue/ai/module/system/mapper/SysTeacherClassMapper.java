package com.zhixue.ai.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.system.entity.SysTeacherClass;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface SysTeacherClassMapper extends BaseMapper<SysTeacherClass> {

    /** 查询教师所教班级学科关系 */
    @Select("SELECT * FROM sys_teacher_class WHERE teacher_id = #{teacherId}")
    List<SysTeacherClass> selectByTeacherId(Long teacherId);
}
