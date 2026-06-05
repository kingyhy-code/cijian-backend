package com.cijian.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("work_tag_rel")
public class WorkTagRel {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long workId;
    private Long tagId;
    @TableField("created_at")
    private LocalDateTime createTime;
}
