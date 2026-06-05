package com.cijian.content.enums;

public enum ContentType {

    MARKDOWN(1, "Markdown"),
    RICH_TEXT(2, "富文本");

    private final int code;
    private final String description;

    ContentType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
