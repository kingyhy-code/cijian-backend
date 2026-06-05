package com.cijian.interaction.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cijian.interaction.entity.Like;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface LikeMapper extends BaseMapper<Like> {

    @Select("SELECT COUNT(*) FROM `like` l " +
            "JOIN work w ON l.work_id = w.id " +
            "WHERE l.target_type = 3 AND w.author_id = #{authorId} " +
            "AND l.deleted = 0 AND w.deleted = 0")
    long countSentencePraiseByAuthor(@Param("authorId") Long authorId);

    @Update("UPDATE `like` SET deleted = 0 WHERE user_id = #{userId} AND target_type = #{targetType} AND target_id = #{targetId}")
    void restoreLike(@Param("userId") Long userId, @Param("targetType") Integer targetType, @Param("targetId") Long targetId);
}
