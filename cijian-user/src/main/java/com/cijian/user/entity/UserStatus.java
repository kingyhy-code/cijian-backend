package com.cijian.user.entity;

public enum UserStatus {

    DISABLED(0, "禁用"),
    NORMAL(1, "正常"),
    CANCELLED(2, "已注销"),
    UNVERIFIED(3, "未验证邮箱");

    private final int code;
    private final String desc;

    UserStatus(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
