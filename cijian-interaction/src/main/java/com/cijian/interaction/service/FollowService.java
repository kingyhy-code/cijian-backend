package com.cijian.interaction.service;

import com.cijian.interaction.dto.FollowVO;

import java.util.List;

public interface FollowService {

    boolean toggle(Long followerId, Long followedId);

    boolean isFollowing(Long followerId, Long followedId);

    List<FollowVO> listFollowers(Long userId, int pageNum, int pageSize);

    List<FollowVO> listFollowing(Long userId, int pageNum, int pageSize);

    long countFollowers(Long userId);

    long countFollowing(Long userId);
}
