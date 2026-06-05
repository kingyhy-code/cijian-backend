package com.cijian.content.controller;

import com.cijian.common.exception.BizException;
import com.cijian.common.page.PageResult;
import com.cijian.common.result.R;
import com.cijian.content.dto.*;
import com.cijian.content.entity.Topic;
import com.cijian.content.service.WorkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/work")
@RequiredArgsConstructor
public class WorkController {

    private final WorkService workService;

    @PostMapping
    public R<Long> publish(@Valid @RequestBody WorkPublishRequest request,
                           @RequestHeader("X-User-Id") Long authorId) {
        Long workId = workService.publish(request, authorId);
        return R.success("发布成功", workId);
    }

    @PutMapping("/{id}")
    public R<?> update(@PathVariable("id") Long id,
                       @Valid @RequestBody WorkPublishRequest request,
                       @RequestHeader("X-User-Id") Long authorId) {
        workService.update(id, request, authorId);
        return R.success("更新成功");
    }

    @DeleteMapping("/{id}")
    public R<?> delete(@PathVariable("id") Long id,
                       @RequestHeader("X-User-Id") Long authorId) {
        workService.delete(id, authorId);
        return R.success("删除成功");
    }

    @GetMapping("/{id}")
    public R<WorkVO> getById(@PathVariable("id") Long id) {
        WorkVO vo = workService.getWorkById(id);
        return R.success(vo);
    }

    @GetMapping("/list")
    public R<PageResult<WorkSimpleVO>> list(WorkListQuery query,
                                            @RequestHeader(value = "X-User-Id", required = false) Long userId) {
        PageResult<WorkSimpleVO> result = workService.listWorks(query, userId);
        return R.success(result);
    }

    @GetMapping("/follow")
    public R<PageResult<WorkSimpleVO>> followFeed(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "NEWEST") String sortBy) {
        return R.success(workService.getFollowFeed(userId, pageNum, pageSize, sortBy));
    }

    @GetMapping("/masterpiece")
    public R<PageResult<WorkSimpleVO>> masterpieceFeed(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "country", required = false) String country,
            @RequestParam(value = "tagName", required = false) String tagName,
            @RequestParam(value = "sortBy", defaultValue = "NEWEST") String sortBy) {
        return R.success(workService.getMasterpieceFeed(pageNum, pageSize, country, tagName, sortBy));
    }

    @GetMapping("/topic/{topicId}")
    public R<PageResult<WorkSimpleVO>> byTopic(
            @PathVariable("topicId") Long topicId,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "NEWEST") String sortBy) {
        return R.success(workService.getWorksByTopic(topicId, pageNum, pageSize, sortBy));
    }

    @GetMapping("/tag")
    public R<PageResult<WorkSimpleVO>> byTag(
            @RequestParam("tagName") String tagName,
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "NEWEST") String sortBy) {
        return R.success(workService.getWorksByTag(tagName, pageNum, pageSize, sortBy));
    }

    @GetMapping("/inspiration")
    public R<PageResult<WorkSimpleVO>> inspirationSquare(
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize,
            @RequestParam(value = "sortBy", defaultValue = "NEWEST") String sortBy) {
        return R.success(workService.getInspirationSquare(pageNum, pageSize, sortBy));
    }

    @PostMapping("/{id}/view")
    public R<?> view(@PathVariable("id") Long id) {
        workService.incrementViewCount(id);
        return R.success("ok");
    }

    @PostMapping("/{id}/likeCount")
    public R<?> updateLikeCount(@PathVariable("id") Long id, @RequestParam("delta") int delta) {
        workService.incrementLikeCount(id, delta);
        return R.success("ok");
    }

    @PostMapping("/{id}/commentCount")
    public R<?> updateCommentCount(@PathVariable("id") Long id, @RequestParam("delta") int delta) {
        workService.incrementCommentCount(id, delta);
        return R.success("ok");
    }

    @PostMapping("/{id}/collectCount")
    public R<?> updateCollectCount(@PathVariable("id") Long id, @RequestParam("delta") int delta) {
        workService.incrementCollectCount(id, delta);
        return R.success("ok");
    }

    @GetMapping("/count/by-author")
    public R<Long> countByAuthor(@RequestParam("authorId") Long authorId,
                                  @RequestParam(value = "status", required = false) Integer status) {
        return R.success(workService.countWorksByAuthor(authorId, status));
    }

    @GetMapping("/like-count/total")
    public R<Long> totalLikeCount(@RequestParam("authorId") Long authorId) {
        return R.success(workService.sumLikeCountByAuthor(authorId));
    }

    @GetMapping("/ref-count/total")
    public R<Long> totalInspirationRefCount(@RequestParam("authorId") Long authorId) {
        return R.success(workService.sumInspirationRefCountByAuthor(authorId));
    }

    @GetMapping("/tags/list")
    public R<List<String>> listTags(@RequestParam(defaultValue = "") String q) {
        return R.success(workService.listTags(q));
    }

    @GetMapping("/tags/stat")
    public R<List<TagStatVO>> getTagStats(@RequestParam("authorId") Long authorId) {
        return R.success(workService.getTagStatsByAuthor(authorId));
    }

    // ── 话题 ──
    @GetMapping("/topics")
    public R<List<Topic>> listTopics() {
        return R.success(workService.listTopics());
    }

    // ── 草稿 ───────────────────────────────────────────

    @PostMapping("/draft")
    public R<Long> saveDraft(@RequestBody Map<String, Object> body,
                             @RequestHeader("X-User-Id") Long authorId) {
        Long draftId = body.get("id") != null ? Long.valueOf(body.get("id").toString()) : null;
        String title = (String) body.getOrDefault("title", "");
        String content = (String) body.getOrDefault("content", "");
        String summary = (String) body.getOrDefault("summary", null);
        String coverUrl = (String) body.getOrDefault("coverUrl", null);
        Long topicId = body.get("topicId") != null ? Long.valueOf(body.get("topicId").toString()) : null;
        @SuppressWarnings("unchecked")
        List<String> tagNames = (List<String>) body.get("tagNames");
        Long workId = workService.saveDraft(authorId, draftId, title, content, summary, coverUrl, topicId, tagNames);
        return R.success("草稿已保存", workId);
    }

    @GetMapping("/drafts")
    public R<List<WorkSimpleVO>> listDrafts(@RequestHeader("X-User-Id") Long authorId) {
        return R.success(workService.listDrafts(authorId));
    }

    @PutMapping("/draft/{id}/publish")
    public R<?> publishDraft(@PathVariable("id") Long id,
                             @Valid @RequestBody WorkPublishRequest request,
                             @RequestHeader("X-User-Id") Long authorId) {
        workService.publishDraft(id, request, authorId);
        return R.success("发布成功");
    }

    @PostMapping("/topic")
    public R<Topic> createTopic(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        if (name == null || name.trim().isEmpty()) throw new BizException("话题名不能为空");
        return R.success(workService.createTopic(name.trim(), body.getOrDefault("description", "")));
    }
}
