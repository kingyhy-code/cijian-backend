package com.cijian.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("work")
public class Work {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long authorId;
    private String title;
    private String summary;
    private String content;
    private Integer contentType;
    private String coverUrl;
    private Integer isInspiration;
    private Integer isMasterpiece;
    private String originalAuthor;
    private String country;
    private Long inspirationFrom;
    private Long topicId;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long collectCount;
    private Long inspirationRefCount;
    private Integer status;
    private LocalDateTime publishedAt;
    @TableField("created_at")
    private LocalDateTime createTime;
    @TableField("updated_at")
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
