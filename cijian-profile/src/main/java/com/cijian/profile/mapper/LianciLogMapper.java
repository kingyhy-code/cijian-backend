package com.cijian.profile.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cijian.profile.entity.LianciLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface LianciLogMapper extends BaseMapper<LianciLog> {

    @Select("SELECT COUNT(*) FROM lianci_log WHERE user_id = #{userId} AND deleted = 0")
    long countByUserId(@Param("userId") Long userId);

    @Select("SELECT level, COUNT(*) as cnt FROM lianci_log WHERE user_id = #{userId} AND deleted = 0 GROUP BY level")
    List<Map<String, Object>> countByLevel(@Param("userId") Long userId);

    @Select("SELECT suggested_text as word, COUNT(*) as cnt FROM lianci_log " +
            "WHERE user_id = #{userId} AND suggested_text IS NOT NULL AND suggested_text != '' AND deleted = 0 " +
            "GROUP BY suggested_text ORDER BY cnt DESC LIMIT #{limit}")
    List<Map<String, Object>> topReplacementWords(@Param("userId") Long userId, @Param("limit") int limit);
}
