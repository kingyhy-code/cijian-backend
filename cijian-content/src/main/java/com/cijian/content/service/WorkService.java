package com.cijian.content.service;

import com.cijian.common.page.PageResult;
import com.cijian.content.dto.*;
import com.cijian.content.entity.Topic;

import java.util.List;

public interface WorkService {

    Long publish(WorkPublishRequest request, Long authorId);

    void update(Long workId, WorkPublishRequest request, Long authorId);

    void delete(Long workId, Long authorId);

    WorkVO getWorkById(Long workId);

    PageResult<WorkSimpleVO> listWorks(WorkListQuery query, Long currentUserId);

    PageResult<WorkSimpleVO> getFollowFeed(Long userId, int pageNum, int pageSize, String sortBy);

    PageResult<WorkSimpleVO> getMasterpieceFeed(int pageNum, int pageSize, String country, String tagName, String sortBy);

    PageResult<WorkSimpleVO> getWorksByTopic(Long topicId, int pageNum, int pageSize, String sortBy);

    PageResult<WorkSimpleVO> getWorksByTag(String tagName, int pageNum, int pageSize, String sortBy);

    PageResult<WorkSimpleVO> getInspirationSquare(int pageNum, int pageSize, String sortBy);

    void incrementViewCount(Long workId);

    void incrementLikeCount(Long workId, int delta);

    void incrementCommentCount(Long workId, int delta);

    void incrementCollectCount(Long workId, int delta);

    void updateInspirationRefCount(Long workId, int delta);

    long countWorksByAuthor(Long authorId, Integer status);

    long sumLikeCountByAuthor(Long authorId);

    long sumInspirationRefCountByAuthor(Long authorId);
    List<String> listTags(String q);

    List<TagStatVO> getTagStatsByAuthor(Long authorId);

    List<Topic> listTopics();

    Topic createTopic(String name, String description);

    Long saveDraft(Long authorId, Long draftId, String title, String content, String summary,
                   String coverUrl, Long topicId, List<String> tagNames);

    List<WorkSimpleVO> listDrafts(Long authorId);

    void publishDraft(Long workId, WorkPublishRequest request, Long authorId);
}
