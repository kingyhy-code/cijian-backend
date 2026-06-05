package com.cijian.operation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cijian.operation.entity.Work;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkMapper extends BaseMapper<Work> {

    @Delete("DELETE FROM work WHERE id = #{id}")
    int forceDeleteById(Long id);
}
