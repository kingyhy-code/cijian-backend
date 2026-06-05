package com.cijian.content.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.cijian.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("topic")
public class Topic extends BaseEntity {

    private String name;
    private String description;
    private String coverUrl;
    private Long workCount;
}
