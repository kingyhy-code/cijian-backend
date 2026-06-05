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
public class TopicVO {
    private Long id;
    private String name;
    private String description;
    private String coverUrl;
    private Integer status;
    private Long workCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
