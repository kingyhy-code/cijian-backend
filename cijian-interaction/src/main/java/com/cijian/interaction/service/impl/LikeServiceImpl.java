package com.cijian.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cijian.interaction.entity.Like;
import com.cijian.interaction.enums.TargetType;
import com.cijian.interaction.feign.ContentFeignClient;
import com.cijian.interaction.mapper.LikeMapper;
import com.cijian.interaction.service.LikeService;
import com.cijian.interaction.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import io.seata.spring.annotation.GlobalTransactional;

@Service
@RequiredArgsConstructor
public class LikeServiceImpl implements LikeService {

    private final LikeMapper likeMapper;
    private final ContentFeignClient contentFeignClient;
    private final NotificationService notificationService;

    @Override
    public boolean toggle(Long userId, Integer targetType, Long targetId, Long workId, Integer sentenceIndex) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
               .eq(Like::getTargetType, targetType)
               .eq(Like::getTargetId, targetId);
        Like existing = likeMapper.selectOne(wrapper);

        if (existing != null) {
            // 取消点赞
            existing.setDeleted(1);
            likeMapper.updateById(existing);
            updateTargetCount(targetType, targetId, workId, -1);
            return false;
        }

        try {
            Like like = new Like();
            like.setUserId(userId);
            like.setTargetType(targetType);
            like.setTargetId(targetId);
            like.setWorkId(workId);
            like.setSentenceIndex(sentenceIndex);
            likeMapper.insert(like);
        } catch (org.springframework.dao.DuplicateKeyException e) {
            // 之前取消过点赞，恢复软删除的记录
            likeMapper.restoreLike(userId, targetType, targetId);
        }

        updateTargetCount(targetType, targetId, workId, 1);
        if (targetType == TargetType.WORK.getCode() && workId != null) {
            try { notificationService.createNotification(getWorkAuthorId(workId), userId, "LIKE", "work", workId, "赞了你的作品"); } catch (Exception ignored) {}
        }
        return true;
    }

    @Override
    public boolean isLiked(Long userId, Integer targetType, Long targetId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getUserId, userId)
               .eq(Like::getTargetType, targetType)
               .eq(Like::getTargetId, targetId);
        return likeMapper.selectCount(wrapper) > 0;
    }

    @Override
    public long countByTarget(Integer targetType, Long targetId) {
        LambdaQueryWrapper<Like> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Like::getTargetType, targetType)
               .eq(Like::getTargetId, targetId);
        return likeMapper.selectCount(wrapper);
    }

    @Override
    public long countSentencePraiseByAuthor(Long authorId) {
        return likeMapper.countSentencePraiseByAuthor(authorId);
    }

    private void updateTargetCount(Integer targetType, Long targetId, Long workId, int delta) {
        if (targetType == TargetType.WORK.getCode() && workId != null) {
            contentFeignClient.updateLikeCount(workId, delta);
        }
    }

    private Long getWorkAuthorId(Long workId) {
        try {
            var r = contentFeignClient.getWorkById(workId);
            if (r != null && r.getData() != null) {
                var data = (java.util.Map<String, Object>) r.getData();
                Object authorId = data.get("authorId");
                if (authorId != null) return ((Number) authorId).longValue();
            }
        } catch (Exception ignored) {}
        return 0L;
    }
}
