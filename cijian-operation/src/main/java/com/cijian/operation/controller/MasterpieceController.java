package com.cijian.operation.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cijian.common.page.PageResult;
import com.cijian.common.result.R;
import com.cijian.operation.entity.Tag;
import com.cijian.operation.entity.Work;
import com.cijian.operation.entity.WorkTagRel;
import com.cijian.operation.mapper.TagMapper;
import com.cijian.operation.mapper.WorkMapper;
import com.cijian.operation.mapper.WorkTagRelMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/operation/masterpiece")
@RequiredArgsConstructor
public class MasterpieceController {

    private final WorkMapper workMapper;
    private final TagMapper tagMapper;
    private final WorkTagRelMapper workTagRelMapper;

    @GetMapping
    public R<PageResult<Work>> list(@RequestParam(defaultValue = "1") int pageNum,
                                     @RequestParam(defaultValue = "20") int pageSize,
                                     @RequestParam(required = false) String country) {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<Work>()
                .eq(Work::getIsMasterpiece, 1);
        if (country != null && !country.isBlank()) {
            wrapper.eq(Work::getCountry, country);
        }
        wrapper.orderByDesc(Work::getCreateTime);
        Page<Work> page = workMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return R.success(PageResult.of(page.getTotal(), page.getRecords(), pageNum, pageSize));
    }

    @GetMapping("/countries")
    public R<List<String>> countries() {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<Work>()
                .eq(Work::getIsMasterpiece, 1)
                .isNotNull(Work::getCountry)
                .ne(Work::getCountry, "")
                .select(Work::getCountry);
        List<String> countries = workMapper.selectList(wrapper).stream()
                .map(Work::getCountry)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        return R.success(countries);
    }

    @PostMapping
    @Transactional
    public R<Long> create(@RequestBody Map<String, Object> body) {
        String title = (String) body.get("title");
        String content = (String) body.get("content");
        String author = (String) body.getOrDefault("author", "佚名");
        String dynasty = (String) body.getOrDefault("dynasty", "");
        String country = (String) body.getOrDefault("country", "");
        if (title == null || content == null) return R.error(400, "标题和正文必填");

        Work w = new Work();
        w.setAuthorId(1L);
        w.setTitle(title);
        w.setContent(content);
        w.setSummary((String) body.getOrDefault("summary", ""));
        w.setCoverUrl((String) body.getOrDefault("coverUrl", ""));
        w.setOriginalAuthor((dynasty.isEmpty() ? "" : "〔" + dynasty + "〕") + author);
        w.setCountry(country);
        w.setIsMasterpiece(1);
        w.setIsInspiration(0);
        w.setContentType(1);
        w.setStatus(1);
        w.setViewCount(0L); w.setLikeCount(0L); w.setCommentCount(0L); w.setCollectCount(0L); w.setInspirationRefCount(0L);
        workMapper.insert(w);

        // 处理标签
        syncTags(w.getId(), body.get("tagNames"));

        return R.success("发布成功", w.getId());
    }

    @PutMapping("/{id}")
    @Transactional
    public R<?> update(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Work w = workMapper.selectById(id);
        if (w == null) return R.error(404, "不存在");
        if (body.containsKey("title")) w.setTitle((String) body.get("title"));
        if (body.containsKey("content")) w.setContent((String) body.get("content"));
        if (body.containsKey("summary")) w.setSummary((String) body.get("summary"));
        if (body.containsKey("coverUrl")) w.setCoverUrl((String) body.get("coverUrl"));
        if (body.containsKey("author") || body.containsKey("dynasty") || body.containsKey("country")) {
            String author = (String) body.getOrDefault("author", "");
            String dynasty = (String) body.getOrDefault("dynasty", "");
            if (!author.isEmpty() || !dynasty.isEmpty())
                w.setOriginalAuthor((dynasty.isEmpty() ? "" : "〔" + dynasty + "〕") + author);
            if (body.containsKey("country")) w.setCountry((String) body.get("country"));
        }
        workMapper.updateById(w);

        if (body.containsKey("tagNames")) {
            syncTags(id, body.get("tagNames"));
        }

        return R.success("已更新");
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable Long id) {
        LambdaQueryWrapper<WorkTagRel> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(WorkTagRel::getWorkId, id);
        workTagRelMapper.delete(relWrapper);
        workMapper.deleteById(id);
        return R.success("已删除");
    }

    @DeleteMapping("/{id}/force")
    public R<?> forceDelete(@PathVariable Long id) {
        workMapper.forceDeleteById(id);
        return R.success("已彻底删除");
    }

    @SuppressWarnings("unchecked")
    private void syncTags(Long workId, Object tagNamesObj) {
        // 清除旧关联
        LambdaQueryWrapper<WorkTagRel> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(WorkTagRel::getWorkId, workId);
        workTagRelMapper.delete(relWrapper);

        List<String> tagNames = new ArrayList<>();
        if (tagNamesObj instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof String s && !s.isBlank()) tagNames.add(s.trim());
            }
        }

        for (String name : tagNames) {
            Tag tag = getOrCreateTag(name);
            WorkTagRel rel = new WorkTagRel();
            rel.setWorkId(workId);
            rel.setTagId(tag.getId());
            workTagRelMapper.insert(rel);
            tag.setUseCount(tag.getUseCount() + 1);
            tagMapper.updateById(tag);
        }
    }

    private Tag getOrCreateTag(String name) {
        Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, name));
        if (tag == null) {
            tag = new Tag();
            tag.setName(name);
            tag.setUseCount(0L);
            tagMapper.insert(tag);
        }
        return tag;
    }
}
