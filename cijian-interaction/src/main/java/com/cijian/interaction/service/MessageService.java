package com.cijian.interaction.service;

import com.cijian.interaction.dto.ConversationVO;
import com.cijian.interaction.dto.MessageVO;
import com.cijian.interaction.dto.SendMessageRequest;

import java.util.List;

public interface MessageService {
    List<ConversationVO> listConversations(Long userId);
    List<MessageVO> getMessages(Long conversationId, Long userId, int pageNum, int pageSize);
    MessageVO sendMessage(Long senderId, SendMessageRequest request);
    void markMessagesAsRead(Long conversationId, Long userId);
    int countUnreadMessages(Long userId);
}
