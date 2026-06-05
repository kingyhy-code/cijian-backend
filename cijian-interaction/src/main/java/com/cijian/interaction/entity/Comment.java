package com.cijian.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cijian.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("comment")
public class Comment extends BaseEntity {

    private Long workId;
    private Long userId;
    private Long parentId;
    private String content;
    private Long likeCount;
    private Long replyCount;
    private Integer status;
}
