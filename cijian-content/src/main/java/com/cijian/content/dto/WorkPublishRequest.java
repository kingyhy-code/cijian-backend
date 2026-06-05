package com.cijian.content.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class WorkPublishRequest {

    @NotBlank(message = "标题不能为空")
    @Size(max = 200, message = "标题不能超过200个字符")
    private String title;

    private String summary;

    @NotBlank(message = "正文不能为空")
    private String content;

    private Integer contentType;

    private String coverUrl;

    private Integer isInspiration;

    private Long inspirationFrom;

    private Long topicId;

    private List<String> tagNames;
}
