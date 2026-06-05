package com.cijian.content.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkVO {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private Integer contentType;
    private String coverUrl;
    private Integer isInspiration;
    private Integer isMasterpiece;
    private String originalAuthor;
    private String country;
    private Long inspirationFrom;
    private Long topicId;
    private String topicName;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long collectCount;
    private Long inspirationRefCount;
    private Integer status;
    private LocalDateTime publishedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private UserVO author;
}
