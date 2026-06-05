package com.cijian.interaction.enums;

public enum TargetType {

    WORK(1, "作品"),
    COMMENT(2, "评论"),
    SENTENCE(3, "句子");

    private final int code;
    private final String description;

    TargetType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
