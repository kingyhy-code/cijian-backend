package com.cijian.interaction.service;

import com.cijian.interaction.dto.CollectVO;
import com.cijian.interaction.dto.CollectionRequest;

import java.util.List;

public interface CollectionService {

    CollectVO add(CollectionRequest request, Long userId);

    void remove(Long collectionId, Long userId);

    List<CollectVO> listByUser(Long userId, Integer collectionType, int pageNum, int pageSize);

    boolean isCollected(Long userId, Integer collectionType, Long targetId);
}
