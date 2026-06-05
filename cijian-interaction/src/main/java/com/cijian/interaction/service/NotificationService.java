package com.cijian.interaction.service;

import com.cijian.interaction.dto.NotificationVO;

import java.util.List;

public interface NotificationService {
    List<NotificationVO> listNotifications(Long userId, int pageNum, int pageSize);
    int countUnread(Long userId);
    void markAsRead(Long notificationId, Long userId);
    void createNotification(Long userId, Long senderId, String type, String targetType, Long targetId, String content);
}
