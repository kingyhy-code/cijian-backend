package com.cijian.content.enums;

public enum WorkStatus {

    AUDITING(0, "审核中"),
    PUBLISHED(1, "已发布"),
    REJECTED(2, "审核不通过"),
    DRAFT(3, "草稿");

    private final int code;
    private final String description;

    WorkStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
