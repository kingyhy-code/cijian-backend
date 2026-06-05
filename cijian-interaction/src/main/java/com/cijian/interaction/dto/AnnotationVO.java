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
public class AnnotationVO {
    private Long id;
    private Long workId;
    private Long userId;
    private UserVO userInfo;
    private Integer sentenceIndex;
    private String content;
    private Long likeCount;
    private LocalDateTime createdAt;
}
