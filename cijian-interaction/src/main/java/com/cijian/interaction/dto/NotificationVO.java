package com.cijian.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationVO {
    private Long id;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String type;
    private String targetType;
    private Long targetId;
    private String content;
    private Integer isRead;
    private LocalDateTime createdAt;
}
