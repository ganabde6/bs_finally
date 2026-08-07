package com.zhixue.ai.module.ai.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhixue.ai.module.ai.entity.PkLsRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import java.util.List;

@Mapper
public interface PkLsRecordMapper extends BaseMapper<PkLsRecord> {

    @Select("SELECT * FROM pk_ls_record WHERE room_id = #{roomId} ORDER BY create_time ASC")
    List<PkLsRecord> selectByRoomId(Long roomId);
}
