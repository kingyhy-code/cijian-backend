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
public class FollowVO {
    private Long id;
    private Long followerId;
    private Long followedId;
    private Integer status;
    private UserVO userInfo;
}
