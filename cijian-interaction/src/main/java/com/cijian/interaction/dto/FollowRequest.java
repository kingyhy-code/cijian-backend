package com.cijian.interaction.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FollowRequest {

    @NotNull(message = "被关注者ID不能为空")
    private Long followedId;
}
