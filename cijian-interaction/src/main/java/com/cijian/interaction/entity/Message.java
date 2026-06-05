package com.cijian.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cijian.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("message")
public class Message extends BaseEntity {
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private Integer isRead;
}
