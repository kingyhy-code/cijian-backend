package com.cijian.search.document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkDocument {
    private Long id;
    private String title;
    private String summary;
    private String content;
    private Long authorId;
    private String authorName;
    private Integer status;
    private Integer isInspiration;
    private Integer isMasterpiece;
    private String country;
    private Long viewCount;
    private Long likeCount;
    private Long commentCount;
    private String publishedAt;
}
