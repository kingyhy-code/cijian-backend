package com.cijian.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentUpdateRequest {

    @NotNull(message = "评论ID不能为空")
    private Long commentId;

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
