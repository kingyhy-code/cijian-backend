package com.cijian.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cijian.common.enums.ResultCode;
import com.cijian.common.exception.BizException;
import com.cijian.interaction.dto.ConversationVO;
import com.cijian.interaction.dto.MessageVO;
import com.cijian.interaction.dto.SendMessageRequest;
import com.cijian.interaction.dto.UserVO;
import com.cijian.interaction.entity.Conversation;
import com.cijian.interaction.entity.Follow;
import com.cijian.interaction.entity.Message;
import com.cijian.interaction.feign.UserFeignClient;
import com.cijian.interaction.mapper.ConversationMapper;
import com.cijian.interaction.mapper.FollowMapper;
import com.cijian.interaction.mapper.MessageMapper;
import com.cijian.interaction.service.MessageService;
import com.cijian.interaction.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final FollowMapper followMapper;
    private final UserFeignClient userFeignClient;
    private final NotificationService notificationService;

    @Override
    public List<ConversationVO> listConversations(Long userId) {
        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUser1Id, userId)
               .or()
               .eq(Conversation::getUser2Id, userId)
               .orderByDesc(Conversation::getLastMessageAt);
        List<Conversation> conversations = conversationMapper.selectList(wrapper);

        if (conversations.isEmpty()) return new ArrayList<>();

        List<Long> otherUserIds = conversations.stream()
                .map(c -> c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id())
                .collect(Collectors.toList());

        Map<Long, UserVO> userMap = Map.of();
        try {
            userMap = otherUserIds.stream().distinct().collect(Collectors.toMap(
                    id -> id,
                    id -> {
                        try {
                            var r = userFeignClient.getUserById(id);
                            return r != null && r.getData() != null ? r.getData() : null;
                        } catch (Exception e) { return null; }
                    },
                    (a, b) -> a
            ));
        } catch (Exception ignored) {}

        Map<Long, UserVO> finalUserMap = userMap;
        return conversations.stream().map(c -> {
            Long otherId = c.getUser1Id().equals(userId) ? c.getUser2Id() : c.getUser1Id();
            UserVO u = finalUserMap.get(otherId);
            int unread = countUnreadInConversation(c.getId(), userId);
            return ConversationVO.builder()
                    .id(c.getId())
                    .otherUserId(otherId)
                    .otherUserName(u != null ? u.getNickname() : "未知用户")
                    .otherUserAvatar(u != null ? u.getAvatarUrl() : null)
                    .lastMessage(c.getLastMessage())
                    .lastMessageAt(c.getLastMessageAt())
                    .unreadCount(unread)
                    .build();
        }).collect(Collectors.toList());
    }

    @Override
    public List<MessageVO> getMessages(Long conversationId, Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId)
               .orderByDesc(Message::getCreateTime);
        Page<Message> page = messageMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Message> messages = page.getRecords();
        // reverse to chronological order
        java.util.Collections.reverse(messages);
        return messages.stream().map(m -> MessageVO.builder()
                .id(m.getId())
                .conversationId(m.getConversationId())
                .senderId(m.getSenderId())
                .content(m.getContent())
                .isRead(m.getIsRead())
                .createdAt(m.getCreateTime())
                .build()).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public MessageVO sendMessage(Long senderId, SendMessageRequest request) {
        Long receiverId = request.getReceiverId();

        // 检查是否互相关注
        if (!isMutualFollow(senderId, receiverId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "需要互相关注才能发送私信");
        }

        // 查找或创建会话
        Conversation conversation = findOrCreateConversation(senderId, receiverId);

        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(request.getContent());
        message.setIsRead(0);
        messageMapper.insert(message);

        // 发送私信通知
        try {
            notificationService.createNotification(receiverId, senderId, "MESSAGE", null, conversation.getId(),
                    request.getContent().length() > 50 ? request.getContent().substring(0, 50) + "..." : request.getContent());
        } catch (Exception ignored) {}

        // 更新会话摘要
        conversation.setLastMessage(request.getContent().length() > 100
                ? request.getContent().substring(0, 100) + "..."
                : request.getContent());
        conversation.setLastMessageAt(LocalDateTime.now());
        conversationMapper.updateById(conversation);

        return MessageVO.builder()
                .id(message.getId())
                .conversationId(conversation.getId())
                .senderId(senderId)
                .content(message.getContent())
                .isRead(0)
                .createdAt(message.getCreateTime())
                .build();
    }

    @Override
    public void markMessagesAsRead(Long conversationId, Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId)
               .eq(Message::getReceiverId, userId)
               .eq(Message::getIsRead, 0);
        List<Message> unread = messageMapper.selectList(wrapper);
        unread.forEach(m -> {
            m.setIsRead(1);
            messageMapper.updateById(m);
        });
    }

    @Override
    public int countUnreadMessages(Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getReceiverId, userId)
               .eq(Message::getIsRead, 0);
        return (int) messageMapper.selectCount(wrapper).intValue();
    }

    private boolean isMutualFollow(Long user1Id, Long user2Id) {
        LambdaQueryWrapper<Follow> w1 = new LambdaQueryWrapper<>();
        w1.eq(Follow::getFollowerId, user1Id).eq(Follow::getFollowedId, user2Id).eq(Follow::getStatus, 1);
        boolean user1FollowsUser2 = followMapper.selectCount(w1) > 0;

        LambdaQueryWrapper<Follow> w2 = new LambdaQueryWrapper<>();
        w2.eq(Follow::getFollowerId, user2Id).eq(Follow::getFollowedId, user1Id).eq(Follow::getStatus, 1);
        boolean user2FollowsUser1 = followMapper.selectCount(w2) > 0;

        return user1FollowsUser2 && user2FollowsUser1;
    }

    private Conversation findOrCreateConversation(Long user1Id, Long user2Id) {
        Long uid1 = user1Id < user2Id ? user1Id : user2Id;
        Long uid2 = user1Id < user2Id ? user2Id : user1Id;

        LambdaQueryWrapper<Conversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Conversation::getUser1Id, uid1)
               .eq(Conversation::getUser2Id, uid2);
        Conversation existing = conversationMapper.selectOne(wrapper);
        if (existing != null) return existing;

        Conversation conversation = new Conversation();
        conversation.setUser1Id(uid1);
        conversation.setUser2Id(uid2);
        conversationMapper.insert(conversation);
        return conversation;
    }

    private int countUnreadInConversation(Long conversationId, Long userId) {
        LambdaQueryWrapper<Message> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Message::getConversationId, conversationId)
               .eq(Message::getReceiverId, userId)
               .eq(Message::getIsRead, 0);
        return (int) messageMapper.selectCount(wrapper).intValue();
    }
}
