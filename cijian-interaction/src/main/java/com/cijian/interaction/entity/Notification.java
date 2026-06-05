package com.cijian.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cijian.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("notification")
public class Notification extends BaseEntity {
    private Long userId;
    private Long senderId;
    private String type;
    private String targetType;
    private Long targetId;
    private String content;
    private Integer isRead;
}
