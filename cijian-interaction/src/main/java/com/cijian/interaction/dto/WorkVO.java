package com.cijian.interaction.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkVO {
    private Long id;
    private String title;
    private Long authorId;
    private String authorName;
}
