package com.zhixue.ai.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.ai.entity.PkLsRoom;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface PkLsRoomMapper extends BaseMapper<PkLsRoom> {

    @Select("SELECT * FROM pk_ls_room WHERE room_code = #{roomCode}")
    PkLsRoom selectByRoomCode(String roomCode);
}
