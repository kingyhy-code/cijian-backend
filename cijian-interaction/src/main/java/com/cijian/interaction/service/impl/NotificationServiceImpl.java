package com.cijian.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cijian.interaction.dto.NotificationVO;
import com.cijian.interaction.dto.UserVO;
import com.cijian.interaction.entity.Notification;
import com.cijian.interaction.feign.UserFeignClient;
import com.cijian.interaction.mapper.NotificationMapper;
import com.cijian.interaction.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationMapper notificationMapper;
    private final UserFeignClient userFeignClient;

    @Override
    public List<NotificationVO> listNotifications(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .orderByDesc(Notification::getCreateTime);
        Page<Notification> page = notificationMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return toVOList(page.getRecords());
    }

    @Override
    public int countUnread(Long userId) {
        LambdaQueryWrapper<Notification> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Notification::getUserId, userId)
               .eq(Notification::getIsRead, 0);
        return notificationMapper.selectCount(wrapper).intValue();
    }

    @Override
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationMapper.selectById(notificationId);
        if (notification != null && notification.getUserId().equals(userId)) {
            notification.setIsRead(1);
            notificationMapper.updateById(notification);
        }
    }

    @Override
    public void createNotification(Long userId, Long senderId, String type, String targetType, Long targetId, String content) {
        if (senderId.equals(userId)) return;
        Notification notification = new Notification();
        notification.setUserId(userId);
        notification.setSenderId(senderId);
        notification.setType(type);
        notification.setTargetType(targetType);
        notification.setTargetId(targetId);
        notification.setContent(content);
        notification.setIsRead(0);
        notificationMapper.insert(notification);
    }

    private List<NotificationVO> toVOList(List<Notification> notifications) {
        if (notifications.isEmpty()) return new ArrayList<>();
        List<Long> senderIds = notifications.stream().map(Notification::getSenderId).distinct().collect(Collectors.toList());
        Map<Long, UserVO> userMap = Map.of();
        try {
            userMap = senderIds.stream().collect(Collectors.toMap(
                    id -> id,
                    id -> {
                        try {
                            var r = userFeignClient.getUserById(id);
                            return r != null && r.getData() != null ? r.getData() : null;
                        } catch (Exception e) {
                            return null;
                        }
                    },
                    (a, b) -> a
            ));
        } catch (Exception ignored) {}

        Map<Long, UserVO> finalUserMap = userMap;
        return notifications.stream().map(n -> {
            UserVO sender = finalUserMap.get(n.getSenderId());
            return NotificationVO.builder()
                    .id(n.getId())
                    .senderId(n.getSenderId())
                    .senderName(sender != null ? sender.getNickname() : "未知用户")
                    .senderAvatar(sender != null ? sender.getAvatarUrl() : null)
                    .type(n.getType())
                    .targetType(n.getTargetType())
                    .targetId(n.getTargetId())
                    .content(n.getContent())
                    .isRead(n.getIsRead())
                    .createdAt(n.getCreateTime())
                    .build();
        }).collect(Collectors.toList());
    }
}
