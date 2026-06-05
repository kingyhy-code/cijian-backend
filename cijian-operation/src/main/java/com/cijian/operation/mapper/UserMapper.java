package com.cijian.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cijian.operation.entity.User;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<User> {

    @Delete("DELETE FROM user WHERE id = #{id}")
    int forceDeleteById(Long id);

    @Delete("DELETE FROM annotation WHERE user_id = #{userId}")
    int deleteAnnotationsByUserId(Long userId);

    @Delete("DELETE FROM comment WHERE user_id = #{userId}")
    int deleteCommentsByUserId(Long userId);

    @Delete("DELETE FROM work WHERE author_id = #{authorId}")
    int deleteWorksByAuthorId(Long authorId);
}
