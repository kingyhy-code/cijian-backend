package com.cijian.content.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cijian.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("work")
public class Work extends BaseEntity {

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
}
