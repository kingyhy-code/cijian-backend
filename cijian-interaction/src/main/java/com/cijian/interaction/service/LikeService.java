package com.cijian.interaction.service;

public interface LikeService {

    boolean toggle(Long userId, Integer targetType, Long targetId, Long workId, Integer sentenceIndex);

    boolean isLiked(Long userId, Integer targetType, Long targetId);

    long countByTarget(Integer targetType, Long targetId);

    long countSentencePraiseByAuthor(Long authorId);
}
