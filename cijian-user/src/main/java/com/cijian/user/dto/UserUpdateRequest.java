package com.cijian.user.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserUpdateRequest {

    @Size(min = 1, max = 50, message = "昵称长度为1-50个字符")
    private String nickname;

    @Size(max = 200, message = "简介不能超过200个字符")
    private String bio;

    private String avatarUrl;
}
