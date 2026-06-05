package com.cijian.interaction.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cijian.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("annotation")
public class Annotation extends BaseEntity {

    private Long workId;
    private Long userId;
    private Integer sentenceIndex;
    private String content;
    private Long likeCount;
    private Integer status;
}
