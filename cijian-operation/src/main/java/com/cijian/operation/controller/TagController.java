package com.cijian.operation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cijian.common.page.PageResult;
import com.cijian.common.result.R;
import com.cijian.operation.entity.Tag;
import com.cijian.operation.entity.WorkTagRel;
import com.cijian.operation.mapper.TagMapper;
import com.cijian.operation.mapper.WorkTagRelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/operation/tags")
@RequiredArgsConstructor
public class TagController {

    private final TagMapper tagMapper;
    private final WorkTagRelMapper workTagRelMapper;

    @GetMapping
    public R<PageResult<Tag>> list(@RequestParam(defaultValue = "1") int pageNum,
                                    @RequestParam(defaultValue = "50") int pageSize) {
        Page<Tag> page = tagMapper.selectPage(new Page<>(pageNum, pageSize),
                new LambdaQueryWrapper<Tag>().orderByDesc(Tag::getUseCount));
        return R.success(PageResult.of(page.getTotal(), page.getRecords(), pageNum, pageSize));
    }

    @PostMapping
    @Transactional
    public R<Long> create(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.isBlank()) return R.error(400, "标签名不能为空");
        Tag existing = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name.trim()));
        if (existing != null) return R.error(400, "标签已存在");
        Tag tag = new Tag();
        tag.setName(name.trim());
        tag.setUseCount(0L);
        tagMapper.insert(tag);
        return R.success("创建成功", tag.getId());
    }

    @PutMapping("/{id}")
    @Transactional
    public R<?> update(@PathVariable Long id, @RequestBody Map<String, String> body) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) return R.error(404, "标签不存在");
        String name = body.get("name");
        if (name == null || name.isBlank()) return R.error(400, "标签名不能为空");
        Tag dup = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name.trim()));
        if (dup != null && !dup.getId().equals(id)) return R.error(400, "标签名重复");
        tag.setName(name.trim());
        tagMapper.updateById(tag);
        return R.success("已更新");
    }

    @DeleteMapping("/{id}")
    @Transactional
    public R<?> delete(@PathVariable Long id) {
        Tag tag = tagMapper.selectById(id);
        if (tag == null) return R.error(404, "标签不存在");
        workTagRelMapper.delete(new LambdaQueryWrapper<WorkTagRel>().eq(WorkTagRel::getTagId, id));
        tagMapper.deleteById(id);
        return R.success("已删除");
    }
}
