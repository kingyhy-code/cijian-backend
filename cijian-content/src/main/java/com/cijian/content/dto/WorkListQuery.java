package com.cijian.content.dto;

import lombok.Data;

@Data
public class WorkListQuery {
    private String feedType;
    private Long topicId;
    private String tagName;
    private String country;
    private String sortBy;
    private int pageNum = 1;
    private int pageSize = 10;
}
