package com.cijian.interaction.enums;

public enum CollectionType {

    STORY(1, "故事"),
    INSPIRATION(2, "灵感"),
    SENTENCE(3, "金句");

    private final int code;
    private final String description;

    CollectionType(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() { return code; }
    public String getDescription() { return description; }
}
