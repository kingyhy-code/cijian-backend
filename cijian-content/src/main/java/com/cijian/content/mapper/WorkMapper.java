package com.cijian.content.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cijian.content.entity.Work;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WorkMapper extends BaseMapper<Work> {

    @Select("SELECT COUNT(*) FROM work WHERE author_id = #{authorId} AND status = #{status} AND deleted = 0")
    long countByAuthor(@Param("authorId") Long authorId, @Param("status") Integer status);

    @Select("SELECT COALESCE(SUM(like_count), 0) FROM work WHERE author_id = #{authorId} AND deleted = 0")
    long sumLikeCountByAuthor(@Param("authorId") Long authorId);

    @Select("SELECT COALESCE(SUM(inspiration_ref_count), 0) FROM work WHERE author_id = #{authorId} AND deleted = 0")
    long sumInspirationRefCountByAuthor(@Param("authorId") Long authorId);

    @Select("SELECT t.name as tagName, COUNT(DISTINCT w.id) as cnt " +
            "FROM work w " +
            "JOIN work_tag_rel wtr ON w.id = wtr.work_id " +
            "JOIN tag t ON wtr.tag_id = t.id " +
            "WHERE w.author_id = #{authorId} AND w.deleted = 0 " +
            "GROUP BY t.name ORDER BY cnt DESC")
    List<Map<String, Object>> tagStatsByAuthor(@Param("authorId") Long authorId);
}
