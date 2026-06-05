package com.cijian.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cijian.common.enums.ResultCode;
import com.cijian.common.exception.BizException;
import com.cijian.common.result.R;
import com.cijian.interaction.dto.CollectVO;
import com.cijian.interaction.dto.CollectionRequest;
import com.cijian.interaction.dto.WorkVO;
import com.cijian.interaction.entity.Collection;
import com.cijian.interaction.feign.ContentFeignClient;
import com.cijian.interaction.mapper.CollectionMapper;
import com.cijian.interaction.service.CollectionService;
import com.cijian.interaction.service.NotificationService;
import io.seata.spring.annotation.GlobalTransactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final CollectionMapper collectionMapper;
    private final ContentFeignClient contentFeignClient;
    private final NotificationService notificationService;

    @Override
    public CollectVO add(CollectionRequest request, Long userId) {
        LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collection::getUserId, userId)
               .eq(Collection::getCollectionType, request.getCollectionType())
               .eq(Collection::getTargetId, request.getTargetId());
        if (collectionMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.BIZ_ERROR.getCode(), "已收藏，请勿重复操作");
        }

        Collection collection = new Collection();
        collection.setUserId(userId);
        collection.setCollectionType(request.getCollectionType());
        collection.setTargetId(request.getTargetId());
        collection.setWorkId(request.getWorkId());
        collection.setSentenceIndex(request.getSentenceIndex());
        collection.setNote(request.getNote());
        collectionMapper.insert(collection);

        if (request.getWorkId() != null) {
            contentFeignClient.updateCollectCount(request.getWorkId(), 1);
        }

        if (request.getWorkId() != null) {
            try { notificationService.createNotification(getWorkAuthorId(request.getWorkId()), userId, "COLLECT", "work", request.getWorkId(), "收藏了你的作品"); } catch (Exception ignored) {}
        }

        return toVO(collection);
    }

    @Override
    public void remove(Long collectionId, Long userId) {
        Collection collection = collectionMapper.selectById(collectionId);
        if (collection == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "收藏不存在");
        }
        if (!collection.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权操作他人收藏");
        }
        collectionMapper.deleteById(collectionId);

        if (collection.getWorkId() != null) {
            contentFeignClient.updateCollectCount(collection.getWorkId(), -1);
        }
    }

    @Override
    public List<CollectVO> listByUser(Long userId, Integer collectionType, int pageNum, int pageSize) {
        LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collection::getUserId, userId)
               .orderByDesc(Collection::getCreateTime);
        if (collectionType != null) {
            wrapper.eq(Collection::getCollectionType, collectionType);
        }

        Page<Collection> page = collectionMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public boolean isCollected(Long userId, Integer collectionType, Long targetId) {
        LambdaQueryWrapper<Collection> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Collection::getUserId, userId)
               .eq(Collection::getCollectionType, collectionType)
               .eq(Collection::getTargetId, targetId);
        return collectionMapper.selectCount(wrapper) > 0;
    }

    private CollectVO toVO(Collection c) {
        String workTitle = null;
        String workAuthor = null;
        if (c.getWorkId() != null) {
            try {
                R<?> result = contentFeignClient.getWorkById(c.getWorkId());
                if (result != null && result.getData() != null) {
                    // 简单提取标题
                    workTitle = "作品#" + c.getWorkId();
                }
            } catch (Exception ignored) {}
        }

        return CollectVO.builder()
                .id(c.getId())
                .collectionType(c.getCollectionType())
                .targetId(c.getTargetId())
                .workId(c.getWorkId())
                .sentenceIndex(c.getSentenceIndex())
                .note(c.getNote())
                .workTitle(workTitle)
                .workAuthor(workAuthor)
                .createdAt(c.getCreateTime())
                .build();
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
