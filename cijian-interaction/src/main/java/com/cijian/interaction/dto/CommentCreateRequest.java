package com.cijian.interaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CommentCreateRequest {

    @NotNull(message = "作品ID不能为空")
    private Long workId;

    private Long parentId;

    @NotBlank(message = "评论内容不能为空")
    private String content;
}
