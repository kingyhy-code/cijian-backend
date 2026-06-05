package com.cijian.interaction.controller;

import com.cijian.common.result.R;
import com.cijian.interaction.dto.ConversationVO;
import com.cijian.interaction.dto.MessageVO;
import com.cijian.interaction.dto.NotificationVO;
import com.cijian.interaction.dto.SendMessageRequest;
import com.cijian.interaction.service.MessageService;
import com.cijian.interaction.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final MessageService messageService;

    // ========== 通知 ==========

    @GetMapping("/notification/list")
    public R<List<NotificationVO>> listNotifications(@RequestHeader("X-User-Id") Long userId,
                                                      @RequestParam(defaultValue = "1") int pageNum,
                                                      @RequestParam(defaultValue = "20") int pageSize) {
        return R.success(notificationService.listNotifications(userId, pageNum, pageSize));
    }

    @GetMapping("/notification/unread-count")
    public R<Integer> countUnreadNotifications(@RequestHeader("X-User-Id") Long userId) {
        return R.success(notificationService.countUnread(userId));
    }

    @PutMapping("/notification/{id}/read")
    public R<String> markNotificationRead(@PathVariable Long id,
                                           @RequestHeader("X-User-Id") Long userId) {
        notificationService.markAsRead(id, userId);
        return R.success("ok");
    }

    // ========== 私信 ==========

    @GetMapping("/message/conversations")
    public R<List<ConversationVO>> listConversations(@RequestHeader("X-User-Id") Long userId) {
        return R.success(messageService.listConversations(userId));
    }

    @GetMapping("/message/{conversationId}")
    public R<List<MessageVO>> getMessages(@PathVariable Long conversationId,
                                           @RequestHeader("X-User-Id") Long userId,
                                           @RequestParam(defaultValue = "1") int pageNum,
                                           @RequestParam(defaultValue = "50") int pageSize) {
        return R.success(messageService.getMessages(conversationId, userId, pageNum, pageSize));
    }

    @PostMapping("/message/send")
    public R<MessageVO> sendMessage(@Valid @RequestBody SendMessageRequest request,
                                     @RequestHeader("X-User-Id") Long userId) {
        return R.success(messageService.sendMessage(userId, request));
    }

    @PutMapping("/message/{conversationId}/read")
    public R<String> markMessagesRead(@PathVariable Long conversationId,
                                       @RequestHeader("X-User-Id") Long userId) {
        messageService.markMessagesAsRead(conversationId, userId);
        return R.success("ok");
    }

    @GetMapping("/message/unread-count")
    public R<Integer> countUnreadMessages(@RequestHeader("X-User-Id") Long userId) {
        return R.success(messageService.countUnreadMessages(userId));
    }
}
