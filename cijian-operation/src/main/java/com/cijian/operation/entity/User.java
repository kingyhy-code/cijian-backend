package com.cijian.operation.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String nickname;
    private String email;
    private String passwordHash;
    private String bio;
    private String avatarUrl;
    private Integer status;
    private LocalDateTime lastLoginTime;
    @TableField("created_at")
    private LocalDateTime createTime;
    @TableField("updated_at")
    private LocalDateTime updateTime;
    @TableLogic
    private Integer deleted;
}
