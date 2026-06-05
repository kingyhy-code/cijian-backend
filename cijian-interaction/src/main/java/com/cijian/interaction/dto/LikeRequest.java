package com.cijian.interaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LikeRequest {

    @NotNull(message = "目标类型不能为空")
    private Integer targetType;

    @NotNull(message = "目标ID不能为空")
    private Long targetId;

    private Long workId;
    private Integer sentenceIndex;
}
