package com.cijian.content.enums;

public enum TopicStatus {

    NOT_STARTED(0, "未开始"),
    ONGOING(1, "进行中"),
    ENDED(2, "已结束");

    private final int code;
    private final String description;

    TopicStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
