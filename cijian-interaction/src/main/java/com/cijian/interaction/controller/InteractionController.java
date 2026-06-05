package com.cijian.interaction.controller;

import com.cijian.common.result.R;
import com.cijian.interaction.dto.*;
import com.cijian.interaction.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class InteractionController {

    private final LikeService likeService;
    private final CommentService commentService;
    private final CollectionService collectionService;
    private final AnnotationService annotationService;
    private final FollowService followService;

    // ========== 点赞 ==========

    @PostMapping("/like/toggle")
    public R<Boolean> toggleLike(@Valid @RequestBody LikeRequest request,
                                 @RequestHeader("X-User-Id") Long userId) {
        boolean liked = likeService.toggle(userId, request.getTargetType(), request.getTargetId(),
                request.getWorkId(), request.getSentenceIndex());
        return R.success(liked ? "点赞成功" : "已取消点赞", liked);
    }

    @GetMapping("/like/check")
    public R<Boolean> checkLike(@RequestParam("targetType") Integer targetType,
                                @RequestParam("targetId") Long targetId,
                                @RequestHeader("X-User-Id") Long userId) {
        return R.success(likeService.isLiked(userId, targetType, targetId));
    }

    @GetMapping("/like/sentence-praise")
    public R<Long> countSentencePraise(@RequestParam("authorId") Long authorId) {
        return R.success(likeService.countSentencePraiseByAuthor(authorId));
    }

    // ========== 评论 ==========

    @PostMapping("/comment")
    public R<CommentVO> createComment(@Valid @RequestBody CommentCreateRequest request,
                                       @RequestHeader("X-User-Id") Long userId) {
        return R.success(commentService.create(request, userId));
    }

    @PutMapping("/comment")
    public R<CommentVO> updateComment(@Valid @RequestBody CommentUpdateRequest request,
                                       @RequestHeader("X-User-Id") Long userId) {
        return R.success(commentService.update(request, userId));
    }

    @DeleteMapping("/comment/{id}")
    public R<?> deleteComment(@PathVariable("id") Long id,
                               @RequestHeader("X-User-Id") Long userId) {
        commentService.delete(id, userId);
        return R.success("删除成功");
    }

    @GetMapping("/comment/work/{workId}")
    public R<List<CommentVO>> listComments(@PathVariable("workId") Long workId,
                                            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return R.success(commentService.listByWork(workId, pageNum, pageSize));
    }

    // ========== 收藏 ==========

    @PostMapping("/collection")
    public R<CollectVO> addCollection(@Valid @RequestBody CollectionRequest request,
                                       @RequestHeader("X-User-Id") Long userId) {
        return R.success(collectionService.add(request, userId));
    }

    @DeleteMapping("/collection/{id}")
    public R<?> removeCollection(@PathVariable("id") Long id,
                                  @RequestHeader("X-User-Id") Long userId) {
        collectionService.remove(id, userId);
        return R.success("已取消收藏");
    }

    @GetMapping("/collection/list")
    public R<List<CollectVO>> listCollections(@RequestParam(value = "collectionType", required = false) Integer collectionType,
                                               @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                               @RequestParam(value = "pageSize", defaultValue = "20") int pageSize,
                                               @RequestHeader("X-User-Id") Long userId) {
        return R.success(collectionService.listByUser(userId, collectionType, pageNum, pageSize));
    }

    @GetMapping("/collection/check")
    public R<Boolean> checkCollection(@RequestParam("collectionType") Integer collectionType,
                                       @RequestParam("targetId") Long targetId,
                                       @RequestHeader("X-User-Id") Long userId) {
        return R.success(collectionService.isCollected(userId, collectionType, targetId));
    }

    // ========== 划词批注 ==========

    @PostMapping("/annotation")
    public R<AnnotationVO> createAnnotation(@Valid @RequestBody AnnotationRequest request,
                                             @RequestHeader("X-User-Id") Long userId) {
        return R.success(annotationService.create(request, userId));
    }

    @PutMapping("/annotation/{id}")
    public R<AnnotationVO> updateAnnotation(@PathVariable("id") Long id,
                                             @RequestParam("content") String content,
                                             @RequestHeader("X-User-Id") Long userId) {
        return R.success(annotationService.update(id, content, userId));
    }

    @DeleteMapping("/annotation/{id}")
    public R<?> deleteAnnotation(@PathVariable("id") Long id,
                                  @RequestHeader("X-User-Id") Long userId) {
        annotationService.delete(id, userId);
        return R.success("删除成功");
    }

    @GetMapping("/annotation/work/{workId}")
    public R<List<AnnotationVO>> listAnnotations(@PathVariable("workId") Long workId,
                                                  @RequestParam(value = "sentenceIndex", required = false) Integer sentenceIndex) {
        if (sentenceIndex != null) {
            return R.success(annotationService.listByWorkAndSentence(workId, sentenceIndex));
        }
        return R.success(annotationService.listByWork(workId));
    }

    // ========== 关注 ==========

    @PostMapping("/follow/toggle")
    public R<Boolean> toggleFollow(@Valid @RequestBody FollowRequest request,
                                    @RequestHeader("X-User-Id") Long userId) {
        boolean following = followService.toggle(userId, request.getFollowedId());
        return R.success(following ? "关注成功" : "已取消关注", following);
    }

    @GetMapping("/follow/check")
    public R<Boolean> checkFollow(@RequestParam("followedId") Long followedId,
                                   @RequestHeader("X-User-Id") Long userId) {
        return R.success(followService.isFollowing(userId, followedId));
    }

    @GetMapping("/follow/followers/{userId}")
    public R<List<FollowVO>> listFollowers(@PathVariable("userId") Long userId,
                                            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return R.success(followService.listFollowers(userId, pageNum, pageSize));
    }

    @GetMapping("/follow/following/{userId}")
    public R<List<FollowVO>> listFollowing(@PathVariable("userId") Long userId,
                                            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
                                            @RequestParam(value = "pageSize", defaultValue = "20") int pageSize) {
        return R.success(followService.listFollowing(userId, pageNum, pageSize));
    }

    @GetMapping("/follow/following/{userId}/ids")
    public R<List<Long>> getFollowingUserIds(@PathVariable("userId") Long userId) {
        List<FollowVO> following = followService.listFollowing(userId, 1, 1000);
        List<Long> ids = following.stream().map(FollowVO::getFollowedId).toList();
        return R.success(ids);
    }

    @GetMapping("/follow/count/followers/{userId}")
    public R<Long> countFollowers(@PathVariable("userId") Long userId) {
        return R.success(followService.countFollowers(userId));
    }

    @GetMapping("/follow/count/following/{userId}")
    public R<Long> countFollowing(@PathVariable("userId") Long userId) {
        return R.success(followService.countFollowing(userId));
    }
}
