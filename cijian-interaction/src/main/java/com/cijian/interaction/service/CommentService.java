package com.cijian.interaction.service;

import com.cijian.interaction.dto.CommentCreateRequest;
import com.cijian.interaction.dto.CommentUpdateRequest;
import com.cijian.interaction.dto.CommentVO;

import java.util.List;

public interface CommentService {

    CommentVO create(CommentCreateRequest request, Long userId);

    CommentVO update(CommentUpdateRequest request, Long userId);

    void delete(Long commentId, Long userId);

    List<CommentVO> listByWork(Long workId, int pageNum, int pageSize);

    long countByWork(Long workId);
}
