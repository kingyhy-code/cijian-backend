package com.cijian.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cijian.common.enums.ResultCode;
import com.cijian.common.exception.BizException;
import com.cijian.common.util.SensitiveWordChecker;
import com.cijian.interaction.dto.AnnotationRequest;
import com.cijian.interaction.dto.AnnotationVO;
import com.cijian.interaction.dto.UserVO;
import com.cijian.interaction.entity.Annotation;
import com.cijian.interaction.feign.UserFeignClient;
import com.cijian.interaction.mapper.AnnotationMapper;
import com.cijian.interaction.service.AnnotationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnnotationServiceImpl implements AnnotationService {

    private final AnnotationMapper annotationMapper;
    private final UserFeignClient userFeignClient;
    private final SensitiveWordChecker sensitiveWordChecker;

    @Override
    public AnnotationVO create(AnnotationRequest request, Long userId) {
        if (sensitiveWordChecker.matches(request.getContent())) {
            throw new BizException("批注内容包含敏感词，请修改");
        }
        Annotation annotation = new Annotation();
        annotation.setWorkId(request.getWorkId());
        annotation.setUserId(userId);
        annotation.setSentenceIndex(request.getSentenceIndex());
        annotation.setContent(request.getContent());
        annotation.setLikeCount(0L);
        annotation.setStatus(1);
        annotationMapper.insert(annotation);
        return toVO(annotation);
    }

    @Override
    public AnnotationVO update(Long annotationId, String content, Long userId) {
        Annotation annotation = annotationMapper.selectById(annotationId);
        if (annotation == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "批注不存在");
        }
        if (!annotation.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权修改他人批注");
        }
        if (sensitiveWordChecker.matches(content)) {
            throw new BizException("批注内容包含敏感词，请修改");
        }
        annotation.setContent(content);
        annotationMapper.updateById(annotation);
        return toVO(annotation);
    }

    @Override
    public void delete(Long annotationId, Long userId) {
        Annotation annotation = annotationMapper.selectById(annotationId);
        if (annotation == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "批注不存在");
        }
        if (!annotation.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权删除他人批注");
        }
        annotation.setStatus(0);
        annotationMapper.updateById(annotation);
    }

    @Override
    public List<AnnotationVO> listByWork(Long workId) {
        LambdaQueryWrapper<Annotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Annotation::getWorkId, workId)
               .eq(Annotation::getStatus, 1)
               .orderByAsc(Annotation::getSentenceIndex);
        return annotationMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public List<AnnotationVO> listByWorkAndSentence(Long workId, Integer sentenceIndex) {
        LambdaQueryWrapper<Annotation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Annotation::getWorkId, workId)
               .eq(Annotation::getSentenceIndex, sentenceIndex)
               .eq(Annotation::getStatus, 1)
               .orderByAsc(Annotation::getCreateTime);
        return annotationMapper.selectList(wrapper).stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    private AnnotationVO toVO(Annotation a) {
        return AnnotationVO.builder()
                .id(a.getId())
                .workId(a.getWorkId())
                .userId(a.getUserId())
                .userInfo(getUser(a.getUserId()))
                .sentenceIndex(a.getSentenceIndex())
                .content(a.getContent())
                .likeCount(a.getLikeCount())
                .createdAt(a.getCreateTime())
                .build();
    }

    private UserVO getUser(Long userId) {
        try {
            var result = userFeignClient.getUserById(userId);
            if (result != null && result.getData() != null) {
                return result.getData();
            }
        } catch (Exception ignored) {}
        return UserVO.builder().id(userId).nickname("未知用户").build();
    }
}
