package com.cijian.interaction.service;

import com.cijian.interaction.dto.AnnotationRequest;
import com.cijian.interaction.dto.AnnotationVO;

import java.util.List;

public interface AnnotationService {

    AnnotationVO create(AnnotationRequest request, Long userId);

    AnnotationVO update(Long annotationId, String content, Long userId);

    void delete(Long annotationId, Long userId);

    List<AnnotationVO> listByWork(Long workId);

    List<AnnotationVO> listByWorkAndSentence(Long workId, Integer sentenceIndex);
}
