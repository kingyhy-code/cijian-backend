package com.cijian.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AnnotationRequest {

    @NotNull(message = "作品ID不能为空")
    private Long workId;

    @NotNull(message = "句子序号不能为空")
    private Integer sentenceIndex;

    @NotBlank(message = "批注内容不能为空")
    private String content;
}
