package com.cijian.interaction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("`like`")
public class Like {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer targetType;
    private Long targetId;
    private Long workId;
    private Integer sentenceIndex;

    @TableField("created_at")
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
