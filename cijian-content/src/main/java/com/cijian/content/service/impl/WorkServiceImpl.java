package com.cijian.content.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cijian.common.enums.ResultCode;
import com.cijian.common.exception.BizException;
import com.cijian.common.page.PageResult;
import com.cijian.common.result.R;
import com.cijian.common.util.SensitiveWordChecker;
import com.cijian.content.dto.*;
import com.cijian.content.entity.*;
import com.cijian.content.enums.WorkStatus;
import com.cijian.content.feign.SocialFeignClient;
import com.cijian.content.feign.UserFeignClient;
import com.cijian.content.mapper.*;
import com.cijian.content.service.TagService;
import com.cijian.content.service.WorkService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkServiceImpl implements WorkService {

    private final WorkMapper workMapper;
    private final TagMapper tagMapper;
    private final WorkTagRelMapper workTagRelMapper;
    private final TopicMapper topicMapper;
    private final TagService tagService;
    private final UserFeignClient userFeignClient;
    private final SocialFeignClient socialFeignClient;
    private final SensitiveWordChecker sensitiveWordChecker;

    @Override
    @Transactional
    public Long publish(WorkPublishRequest request, Long authorId) {
        checkSensitive(request.getTitle(), request.getSummary(), request.getContent());

        Work work = new Work();
        work.setAuthorId(authorId);
        work.setTitle(request.getTitle());
        work.setSummary(request.getSummary());
        work.setContent(request.getContent());
        work.setContentType(request.getContentType() != null ? request.getContentType() : 1);
        work.setCoverUrl(request.getCoverUrl());
        work.setIsInspiration(request.getIsInspiration() != null ? request.getIsInspiration() : 0);
        work.setTopicId(request.getTopicId());
        work.setViewCount(0L);
        work.setLikeCount(0L);
        work.setCommentCount(0L);
        work.setCollectCount(0L);
        work.setInspirationRefCount(0L);
        work.setStatus(WorkStatus.PUBLISHED.getCode());
        work.setPublishedAt(LocalDateTime.now());

        // 灵感引用处理
        if (request.getInspirationFrom() != null) {
            Work sourceWork = workMapper.selectById(request.getInspirationFrom());
            if (sourceWork == null || sourceWork.getIsInspiration() != 1) {
                throw new BizException(ResultCode.BIZ_ERROR.getCode(), "引用的源作品不存在或非灵感作品");
            }
            work.setInspirationFrom(request.getInspirationFrom());
            workMapper.insert(work);
            updateInspirationRefCount(request.getInspirationFrom(), 1);
            return work.getId();
        }

        workMapper.insert(work);

        // 话题作品数+1
        if (work.getTopicId() != null) {
            Topic topic = topicMapper.selectById(work.getTopicId());
            if (topic != null) {
                topic.setWorkCount(topic.getWorkCount() + 1);
                topicMapper.updateById(topic);
            }
        }

        // 标签处理
        if (request.getTagNames() != null && !request.getTagNames().isEmpty()) {
            syncTags(work.getId(), request.getTagNames());
        }

        return work.getId();
    }

    @Override
    @Transactional
    public void update(Long workId, WorkPublishRequest request, Long authorId) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "作品不存在");
        }
        if (!work.getAuthorId().equals(authorId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权修改他人作品");
        }

        checkSensitive(request.getTitle(), request.getSummary(), request.getContent());

        if (request.getTitle() != null) {
            work.setTitle(request.getTitle());
        }
        if (request.getSummary() != null) {
            work.setSummary(request.getSummary());
        }
        if (request.getContent() != null) {
            work.setContent(request.getContent());
        }
        if (request.getContentType() != null) {
            work.setContentType(request.getContentType());
        }
        if (request.getCoverUrl() != null) {
            work.setCoverUrl(request.getCoverUrl());
        }
        if (request.getIsInspiration() != null) {
            work.setIsInspiration(request.getIsInspiration());
        }
        if (request.getTopicId() != null && !request.getTopicId().equals(work.getTopicId())) {
            // 旧话题-1，新话题+1
            if (work.getTopicId() != null) {
                Topic oldTopic = topicMapper.selectById(work.getTopicId());
                if (oldTopic != null && oldTopic.getWorkCount() > 0) {
                    oldTopic.setWorkCount(oldTopic.getWorkCount() - 1);
                    topicMapper.updateById(oldTopic);
                }
            }
            Topic newTopic = topicMapper.selectById(request.getTopicId());
            if (newTopic != null) {
                newTopic.setWorkCount(newTopic.getWorkCount() + 1);
                topicMapper.updateById(newTopic);
            }
            work.setTopicId(request.getTopicId());
        }

        workMapper.updateById(work);

        if (request.getTagNames() != null) {
            syncTags(workId, request.getTagNames());
        }
    }

    @Override
    public void delete(Long workId, Long authorId) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "作品不存在");
        }
        if (!work.getAuthorId().equals(authorId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权删除他人作品");
        }
        workMapper.deleteById(workId);
    }

    @Override
    public WorkVO getWorkById(Long workId) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "作品不存在");
        }
        return toVO(work);
    }

    @Override
    public PageResult<WorkSimpleVO> listWorks(WorkListQuery query, Long currentUserId) {
        String feedType = query.getFeedType() != null ? query.getFeedType().toUpperCase() : "NEWEST";
        String sortBy = query.getSortBy() != null ? query.getSortBy() : "NEWEST";

        return switch (feedType) {
            case "FOLLOW" -> getFollowFeed(currentUserId, query.getPageNum(), query.getPageSize(), sortBy);
            case "MASTERPIECE" -> getMasterpieceFeed(query.getPageNum(), query.getPageSize(), query.getCountry(), query.getTagName(), sortBy);
            case "TOPIC" -> getWorksByTopic(query.getTopicId(), query.getPageNum(), query.getPageSize(), sortBy);
            case "TAG" -> getWorksByTag(query.getTagName(), query.getPageNum(), query.getPageSize(), sortBy);
            case "INSPIRATION" -> getInspirationSquare(query.getPageNum(), query.getPageSize(), sortBy);
            default -> getPublishedWorks(query.getPageNum(), query.getPageSize(), sortBy);
        };
    }

    @Override
    public PageResult<WorkSimpleVO> getFollowFeed(Long userId, int pageNum, int pageSize, String sortBy) {
        List<Long> followingIds = getFollowingUserIds(userId);
        if (followingIds.isEmpty()) {
            return PageResult.of(0L, Collections.emptyList(), pageNum, pageSize);
        }

        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Work::getAuthorId, followingIds)
               .eq(Work::getStatus, WorkStatus.PUBLISHED.getCode());
        applySort(wrapper, sortBy);

        Page<Work> page = workMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), toSimpleVOList(page.getRecords()), pageNum, pageSize);
    }

    @Override
    public PageResult<WorkSimpleVO> getMasterpieceFeed(int pageNum, int pageSize, String country, String tagName, String sortBy) {
        // 标签筛选需要先查 work_tag_rel
        List<Long> filteredWorkIds = null;
        if (tagName != null && !tagName.isBlank()) {
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagName));
            if (tag == null) return PageResult.of(0L, Collections.emptyList(), pageNum, pageSize);
            filteredWorkIds = workTagRelMapper.selectList(
                    new LambdaQueryWrapper<WorkTagRel>().eq(WorkTagRel::getTagId, tag.getId()))
                    .stream().map(WorkTagRel::getWorkId).collect(Collectors.toList());
            if (filteredWorkIds.isEmpty()) return PageResult.of(0L, Collections.emptyList(), pageNum, pageSize);
        }

        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Work::getIsMasterpiece, 1)
               .eq(Work::getStatus, WorkStatus.PUBLISHED.getCode());
        if (country != null && !country.isBlank()) {
            wrapper.eq(Work::getCountry, country);
        }
        if (filteredWorkIds != null) {
            wrapper.in(Work::getId, filteredWorkIds);
        }
        applySort(wrapper, sortBy);

        Page<Work> page = workMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), toSimpleVOList(page.getRecords()), pageNum, pageSize);
    }

    @Override
    public PageResult<WorkSimpleVO> getWorksByTopic(Long topicId, int pageNum, int pageSize, String sortBy) {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Work::getTopicId, topicId)
               .eq(Work::getStatus, WorkStatus.PUBLISHED.getCode());
        applySort(wrapper, sortBy);

        Page<Work> page = workMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), toSimpleVOList(page.getRecords()), pageNum, pageSize);
    }

    @Override
    public PageResult<WorkSimpleVO> getWorksByTag(String tagName, int pageNum, int pageSize, String sortBy) {
        Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>().eq(Tag::getName, tagName));
        if (tag == null) {
            return PageResult.of(0L, Collections.emptyList(), pageNum, pageSize);
        }

        LambdaQueryWrapper<WorkTagRel> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(WorkTagRel::getTagId, tag.getId());
        List<Long> workIds = workTagRelMapper.selectList(relWrapper).stream()
                .map(WorkTagRel::getWorkId)
                .collect(Collectors.toList());

        if (workIds.isEmpty()) {
            return PageResult.of(0L, Collections.emptyList(), pageNum, pageSize);
        }

        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(Work::getId, workIds)
               .eq(Work::getStatus, WorkStatus.PUBLISHED.getCode());
        applySort(wrapper, sortBy);

        Page<Work> page = workMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), toSimpleVOList(page.getRecords()), pageNum, pageSize);
    }

    @Override
    public PageResult<WorkSimpleVO> getInspirationSquare(int pageNum, int pageSize, String sortBy) {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Work::getIsInspiration, 1)
               .eq(Work::getStatus, WorkStatus.PUBLISHED.getCode());
        applySort(wrapper, sortBy);

        Page<Work> page = workMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), toSimpleVOList(page.getRecords()), pageNum, pageSize);
    }

    @Override
    public void incrementViewCount(Long workId) {
        Work work = workMapper.selectById(workId);
        if (work != null) {
            work.setViewCount(work.getViewCount() + 1);
            workMapper.updateById(work);
        }
    }

    @Override
    public void incrementLikeCount(Long workId, int delta) {
        Work work = workMapper.selectById(workId);
        if (work != null) {
            work.setLikeCount(Math.max(0, work.getLikeCount() + delta));
            workMapper.updateById(work);
        }
    }

    @Override
    public void incrementCommentCount(Long workId, int delta) {
        Work work = workMapper.selectById(workId);
        if (work != null) {
            work.setCommentCount(Math.max(0, work.getCommentCount() + delta));
            workMapper.updateById(work);
        }
    }

    @Override
    public void incrementCollectCount(Long workId, int delta) {
        Work work = workMapper.selectById(workId);
        if (work != null) {
            work.setCollectCount(Math.max(0, work.getCollectCount() + delta));
            workMapper.updateById(work);
        }
    }

    @Override
    public void updateInspirationRefCount(Long workId, int delta) {
        Work work = workMapper.selectById(workId);
        if (work != null) {
            work.setInspirationRefCount(Math.max(0, work.getInspirationRefCount() + delta));
            workMapper.updateById(work);
        }
    }

    @Override
    public long countWorksByAuthor(Long authorId, Integer status) {
        return workMapper.countByAuthor(authorId, status != null ? status : 1);
    }

    @Override
    public long sumLikeCountByAuthor(Long authorId) {
        return workMapper.sumLikeCountByAuthor(authorId);
    }

    @Override
    public List<String> listTags(String q) {
        LambdaQueryWrapper<Tag> wrapper = new LambdaQueryWrapper<Tag>()
                .select(Tag::getName)
                .orderByDesc(Tag::getUseCount);
        if (q != null && !q.isEmpty()) wrapper.like(Tag::getName, q);
        return tagMapper.selectList(wrapper).stream().map(Tag::getName).limit(20).collect(java.util.stream.Collectors.toList());
    }

    @Override
    public long sumInspirationRefCountByAuthor(Long authorId) {
        return workMapper.sumInspirationRefCountByAuthor(authorId);
    }

    @Override
    public List<TagStatVO> getTagStatsByAuthor(Long authorId) {
        return workMapper.tagStatsByAuthor(authorId).stream()
                .map(row -> TagStatVO.builder()
                        .tagName((String) row.get("tagName"))
                        .count(((Number) row.get("cnt")).longValue())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    public List<Topic> listTopics() {
        return topicMapper.selectList(new LambdaQueryWrapper<Topic>()
                .orderByDesc(Topic::getWorkCount));
    }

    @Override
    public Topic createTopic(String name, String description) {
        Topic existing = topicMapper.selectOne(new LambdaQueryWrapper<Topic>().eq(Topic::getName, name));
        if (existing != null) return existing;
        Topic topic = new Topic();
        topic.setName(name);
        topic.setDescription(description);
        topic.setWorkCount(0L);
        topicMapper.insert(topic);
        return topic;
    }

    // --- private helpers ---

    private void checkSensitive(String... texts) {
        for (String text : texts) {
            if (text != null && sensitiveWordChecker.matches(text)) {
                throw new BizException("内容包含敏感词，请修改");
            }
        }
    }

    private PageResult<WorkSimpleVO> getPublishedWorks(int pageNum, int pageSize, String sortBy) {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Work::getStatus, WorkStatus.PUBLISHED.getCode())
               .eq(Work::getIsMasterpiece, 0);  // 排除名家作品
        applySort(wrapper, sortBy);

        Page<Work> page = workMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return PageResult.of(page.getTotal(), toSimpleVOList(page.getRecords()), pageNum, pageSize);
    }

    private void applySort(LambdaQueryWrapper<Work> wrapper, String sortBy) {
        if ("HOTTEST".equalsIgnoreCase(sortBy)) {
            wrapper.last("ORDER BY (COALESCE(like_count, 0) / GREATEST(1, DATEDIFF(NOW(), published_at))) DESC, published_at DESC");
        } else {
            wrapper.orderByDesc(Work::getPublishedAt);
        }
    }

    private void syncTags(Long workId, List<String> tagNames) {
        LambdaQueryWrapper<WorkTagRel> relWrapper = new LambdaQueryWrapper<>();
        relWrapper.eq(WorkTagRel::getWorkId, workId);
        workTagRelMapper.delete(relWrapper);

        for (String name : tagNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            Tag tag = tagService.getOrCreateTag(name.trim());
            WorkTagRel rel = new WorkTagRel();
            rel.setWorkId(workId);
            rel.setTagId(tag.getId());
            workTagRelMapper.insert(rel);
            tagService.incrementUseCount(tag.getId());
        }
    }

    private List<Long> getFollowingUserIds(Long userId) {
        try {
            R<List<Long>> result = socialFeignClient.getFollowingUserIds(userId);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception ignored) {
            // 互动服务暂未就绪，返回空列表
        }
        return Collections.emptyList();
    }

    private UserVO getAuthor(Long authorId) {
        try {
            R<UserVO> result = userFeignClient.getUserById(authorId);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception ignored) {
            // 用户服务不可用时返回占位信息
        }
        return UserVO.builder().id(authorId).nickname("未知用户").build();
    }

    private WorkVO toVO(Work work) {
        return WorkVO.builder()
                .id(work.getId())
                .title(work.getTitle())
                .summary(work.getSummary())
                .content(work.getContent())
                .contentType(work.getContentType())
                .coverUrl(work.getCoverUrl())
                .isInspiration(work.getIsInspiration())
                .isMasterpiece(work.getIsMasterpiece())
                .originalAuthor(work.getOriginalAuthor())
                .country(work.getCountry())
                .inspirationFrom(work.getInspirationFrom())
                .topicId(work.getTopicId())
                .topicName(getTopicName(work.getTopicId()))
                .viewCount(work.getViewCount())
                .likeCount(work.getLikeCount())
                .commentCount(work.getCommentCount())
                .collectCount(work.getCollectCount())
                .inspirationRefCount(work.getInspirationRefCount())
                .status(work.getStatus())
                .publishedAt(work.getPublishedAt())
                .createdAt(work.getCreateTime())
                .updatedAt(work.getUpdateTime())
                .author(getAuthor(work.getAuthorId()))
                .build();
    }

    private WorkSimpleVO toSimpleVO(Work work) {
        return WorkSimpleVO.builder()
                .id(work.getId())
                .title(work.getTitle())
                .summary(work.getSummary())
                .contentPreview(truncateContent(work.getContent(), 120))
                .coverUrl(work.getCoverUrl())
                .isInspiration(work.getIsInspiration())
                .isMasterpiece(work.getIsMasterpiece())
                .originalAuthor(work.getOriginalAuthor())
                .country(work.getCountry())
                .viewCount(work.getViewCount())
                .likeCount(work.getLikeCount())
                .commentCount(work.getCommentCount())
                .collectCount(work.getCollectCount())
                .inspirationRefCount(work.getInspirationRefCount())
                .publishedAt(work.getPublishedAt())
                .author(getAuthor(work.getAuthorId()))
                .build();
    }

    private String truncateContent(String content, int maxLen) {
        if (content == null) return null;
        String cleaned = content.replaceAll("\\*\\*|#|>", "").replaceAll("\\n+", " ").trim();
        return cleaned.length() <= maxLen ? cleaned : cleaned.substring(0, maxLen) + "...";
    }

    private List<WorkSimpleVO> toSimpleVOList(List<Work> works) {
        return works.stream().map(this::toSimpleVO).collect(Collectors.toList());
    }

    private String getTopicName(Long topicId) {
        if (topicId == null) {
            return null;
        }
        Topic topic = topicMapper.selectById(topicId);
        return topic != null ? topic.getName() : null;
    }

    // ── 草稿 ───────────────────────────────────────────

    @Override
    public Long saveDraft(Long authorId, Long draftId, String title, String content,
                          String summary, String coverUrl, Long topicId, List<String> tagNames) {
        Work work;
        if (draftId != null) {
            work = workMapper.selectById(draftId);
            if (work == null || !work.getAuthorId().equals(authorId)) {
                throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权操作此草稿");
            }
        } else {
            work = new Work();
            work.setAuthorId(authorId);
            work.setViewCount(0L);
            work.setLikeCount(0L);
            work.setCommentCount(0L);
            work.setCollectCount(0L);
            work.setInspirationRefCount(0L);
        }

        work.setTitle(title);
        work.setContent(content);
        if (summary != null) work.setSummary(summary);
        if (coverUrl != null) work.setCoverUrl(coverUrl);
        if (topicId != null) work.setTopicId(topicId);
        work.setContentType(1);
        work.setIsInspiration(0);
        work.setStatus(WorkStatus.DRAFT.getCode());

        if (draftId != null) {
            workMapper.updateById(work);
        } else {
            workMapper.insert(work);
        }

        if (tagNames != null && !tagNames.isEmpty()) {
            syncTags(work.getId(), tagNames);
        }

        return work.getId();
    }

    @Override
    public List<WorkSimpleVO> listDrafts(Long authorId) {
        LambdaQueryWrapper<Work> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Work::getAuthorId, authorId)
               .eq(Work::getStatus, WorkStatus.DRAFT.getCode())
               .orderByDesc(Work::getUpdateTime);
        return toSimpleVOList(workMapper.selectList(wrapper));
    }

    @Override
    @Transactional
    public void publishDraft(Long workId, WorkPublishRequest request, Long authorId) {
        Work work = workMapper.selectById(workId);
        if (work == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "草稿不存在");
        }
        if (!work.getAuthorId().equals(authorId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权发布他人草稿");
        }
        if (work.getStatus() == null || work.getStatus() != WorkStatus.DRAFT.getCode()) {
            throw new BizException("该作品已发布，请勿重复操作");
        }

        checkSensitive(request.getTitle(), request.getSummary(), request.getContent());

        work.setTitle(request.getTitle());
        work.setContent(request.getContent());
        if (request.getSummary() != null) work.setSummary(request.getSummary());
        if (request.getContentType() != null) work.setContentType(request.getContentType());
        if (request.getCoverUrl() != null) work.setCoverUrl(request.getCoverUrl());
        if (request.getIsInspiration() != null) work.setIsInspiration(request.getIsInspiration());
        if (request.getTopicId() != null) work.setTopicId(request.getTopicId());

        work.setStatus(WorkStatus.PUBLISHED.getCode());
        work.setPublishedAt(LocalDateTime.now());
        workMapper.updateById(work);

        if (request.getTagNames() != null) {
            syncTags(workId, request.getTagNames());
        }
    }
}
