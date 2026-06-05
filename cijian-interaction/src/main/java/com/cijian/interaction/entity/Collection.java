package com.cijian.interaction.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("collection")
public class Collection {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Integer collectionType;
    private Long targetId;
    private Long workId;
    private Integer sentenceIndex;
    private String note;

    @TableField("created_at")
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
