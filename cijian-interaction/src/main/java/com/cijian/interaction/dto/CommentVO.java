package com.cijian.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentVO {
    private Long id;
    private Long workId;
    private Long userId;
    private UserVO userInfo;
    private Long parentId;
    private String content;
    private Long likeCount;
    private Long replyCount;
    private Integer status;
    private LocalDateTime createdAt;
    private List<CommentVO> children;
}
