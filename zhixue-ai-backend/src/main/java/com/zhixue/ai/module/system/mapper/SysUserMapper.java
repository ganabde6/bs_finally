package com.zhixue.ai.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {

    /** 根据用户名查询用户(逻辑删除已过滤) */
    @Select("SELECT * FROM sys_user WHERE username = #{username} AND deleted = 0")
    SysUser selectByUsername(String username);

    /** 根据班级ID查询学生列表 */
    @Select("SELECT * FROM sys_user WHERE class_id = #{classId} AND role_id = 4 AND deleted = 0")
    java.util.List<SysUser> selectByClassId(Long classId);
}
