package com.cijian.interaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CollectionRequest {

    @NotNull(message = "收藏类型不能为空")
    private Integer collectionType;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    private Long workId;
    private Integer sentenceIndex;
    private String note;
}
