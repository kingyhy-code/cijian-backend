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
public class WorkSimpleVO {
    private Long id;
    private String title;
    private String summary;
    private String contentPreview;
    private String coverUrl;
    private Integer isInspiration;
    private Integer isMasterpiece;
    private String originalAuthor;
    private String country;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private Long collectCount;
    private Long inspirationRefCount;
    private LocalDateTime publishedAt;
    private UserVO author;
}
