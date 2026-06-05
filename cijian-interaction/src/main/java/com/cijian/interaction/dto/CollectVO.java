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
public class CollectVO {
    private Long id;
    private Integer collectionType;
    private Long targetId;
    private Long workId;
    private Integer sentenceIndex;
    private String note;
    private String workTitle;
    private String workAuthor;
    private LocalDateTime createdAt;
}
