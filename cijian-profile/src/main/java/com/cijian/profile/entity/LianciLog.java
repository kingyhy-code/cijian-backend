package com.cijian.profile.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("lianci_log")
public class LianciLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;
    private Long workId;
    private Integer level;
    private String originalText;
    private String suggestedText;
    private Integer position;

    @TableField("created_at")
    private LocalDateTime createTime;

    @TableLogic
    private Integer deleted;
}
