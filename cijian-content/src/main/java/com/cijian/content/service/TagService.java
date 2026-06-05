package com.cijian.content.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cijian.content.entity.Tag;
import com.cijian.content.mapper.TagMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TagService {

    private final TagMapper tagMapper;

    public Tag getOrCreateTag(String name) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Tag::getName, name);
        Tag tag = tagMapper.selectOne(wrapper);
        if (tag == null) {
            tag = new Tag();
            tag.setName(name);
            tag.setUseCount(0L);
            tagMapper.insert(tag);
        }
        return tag;
    }

    public void incrementUseCount(Long tagId) {
        Tag tag = tagMapper.selectById(tagId);
        if (tag != null) {
            tag.setUseCount(tag.getUseCount() + 1);
            tagMapper.updateById(tag);
        }
    }
}
