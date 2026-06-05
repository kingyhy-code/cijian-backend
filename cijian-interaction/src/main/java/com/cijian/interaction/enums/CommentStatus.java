package com.cijian.interaction.enums;

public enum CommentStatus {

    DELETED(0, "已删除"),
    NORMAL(1, "正常");

    private final int code;
    private final String description;

    CommentStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
