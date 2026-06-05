package com.cijian.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cijian.common.enums.ResultCode;
import com.cijian.common.exception.BizException;
import com.cijian.common.util.SensitiveWordChecker;
import com.cijian.interaction.dto.CommentCreateRequest;
import com.cijian.interaction.dto.CommentUpdateRequest;
import com.cijian.interaction.dto.CommentVO;
import com.cijian.interaction.dto.UserVO;
import com.cijian.interaction.entity.Comment;
import com.cijian.interaction.enums.CommentStatus;
import com.cijian.interaction.feign.ContentFeignClient;
import com.cijian.interaction.feign.UserFeignClient;
import com.cijian.interaction.mapper.CommentMapper;
import com.cijian.interaction.service.CommentService;
import com.cijian.interaction.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import io.seata.spring.annotation.GlobalTransactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentServiceImpl implements CommentService {

    private final CommentMapper commentMapper;
    private final ContentFeignClient contentFeignClient;
    private final UserFeignClient userFeignClient;
    private final NotificationService notificationService;
    private final SensitiveWordChecker sensitiveWordChecker;

    @Override
    public CommentVO create(CommentCreateRequest request, Long userId) {
        if (sensitiveWordChecker.matches(request.getContent())) {
            throw new BizException("评论内容包含敏感词，请修改");
        }
        Comment comment = new Comment();
        comment.setWorkId(request.getWorkId());
        comment.setUserId(userId);
        comment.setParentId(request.getParentId() != null ? request.getParentId() : 0L);
        comment.setContent(request.getContent());
        comment.setLikeCount(0L);
        comment.setReplyCount(0L);
        comment.setStatus(CommentStatus.NORMAL.getCode());
        commentMapper.insert(comment);

        if (comment.getParentId() > 0) {
            Comment parent = commentMapper.selectById(comment.getParentId());
            if (parent != null) {
                parent.setReplyCount(parent.getReplyCount() + 1);
                commentMapper.updateById(parent);
            }
        }

        contentFeignClient.updateCommentCount(request.getWorkId(), 1);

        try {
            if (comment.getParentId() > 0) {
                Comment parent = commentMapper.selectById(comment.getParentId());
                if (parent != null) notificationService.createNotification(parent.getUserId(), userId, "REPLY", "comment", comment.getId(), "回复了你的评论");
            } else {
                notificationService.createNotification(getWorkAuthorId(request.getWorkId()), userId, "COMMENT", "work", request.getWorkId(), "评论了你的作品");
            }
        } catch (Exception ignored) {}

        return toVO(comment);
    }

    @Override
    public CommentVO update(CommentUpdateRequest request, Long userId) {
        Comment comment = commentMapper.selectById(request.getCommentId());
        if (comment == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权修改他人评论");
        }
        if (sensitiveWordChecker.matches(request.getContent())) {
            throw new BizException("评论内容包含敏感词，请修改");
        }
        comment.setContent(request.getContent());
        commentMapper.updateById(comment);
        return toVO(comment);
    }

    @Override
    public void delete(Long commentId, Long userId) {
        Comment comment = commentMapper.selectById(commentId);
        if (comment == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "评论不存在");
        }
        if (!comment.getUserId().equals(userId)) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "无权删除他人评论");
        }
        comment.setStatus(CommentStatus.DELETED.getCode());
        commentMapper.updateById(comment);

        contentFeignClient.updateCommentCount(comment.getWorkId(), -1);
    }

    @Override
    public List<CommentVO> listByWork(Long workId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getWorkId, workId)
               .eq(Comment::getParentId, 0L)
               .eq(Comment::getStatus, CommentStatus.NORMAL.getCode())
               .orderByDesc(Comment::getCreateTime);

        Page<Comment> page = commentMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        List<Comment> roots = page.getRecords();

        if (roots.isEmpty()) {
            return List.of();
        }

        List<Long> rootIds = roots.stream().map(Comment::getId).collect(Collectors.toList());
        LambdaQueryWrapper<Comment> childWrapper = new LambdaQueryWrapper<>();
        childWrapper.in(Comment::getParentId, rootIds)
                    .eq(Comment::getStatus, CommentStatus.NORMAL.getCode())
                    .orderByAsc(Comment::getCreateTime);
        List<Comment> children = commentMapper.selectList(childWrapper);

        Map<Long, List<CommentVO>> childrenMap = children.stream()
                .map(this::toVO)
                .collect(Collectors.groupingBy(vo -> vo.getParentId()));

        return roots.stream().map(root -> {
            CommentVO vo = toVO(root);
            vo.setChildren(childrenMap.getOrDefault(root.getId(), List.of()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public long countByWork(Long workId) {
        LambdaQueryWrapper<Comment> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Comment::getWorkId, workId)
               .eq(Comment::getStatus, CommentStatus.NORMAL.getCode());
        return commentMapper.selectCount(wrapper);
    }

    private CommentVO toVO(Comment comment) {
        return CommentVO.builder()
                .id(comment.getId())
                .workId(comment.getWorkId())
                .userId(comment.getUserId())
                .userInfo(getUser(comment.getUserId()))
                .parentId(comment.getParentId())
                .content(comment.getContent())
                .likeCount(comment.getLikeCount())
                .replyCount(comment.getReplyCount())
                .status(comment.getStatus())
                .createdAt(comment.getCreateTime())
                .children(new ArrayList<>())
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
