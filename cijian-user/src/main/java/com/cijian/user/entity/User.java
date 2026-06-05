package com.cijian.user.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cijian.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user")
public class User extends BaseEntity {

    private String nickname;
    private String email;
    private String passwordHash;
    private String bio;
    private String avatarUrl;
    private Integer status;
    private String role;
    private LocalDateTime lastLoginTime;
}
