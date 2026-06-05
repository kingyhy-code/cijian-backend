package com.cijian.user.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserVO {
    private Long id;
    private String nickname;
    private String email;
    private String bio;
    private String avatarUrl;
    private Integer status;
    private String role;
    private LocalDateTime createdAt;
}
