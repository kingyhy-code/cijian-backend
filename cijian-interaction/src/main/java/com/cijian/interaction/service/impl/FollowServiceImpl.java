package com.cijian.interaction.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cijian.common.enums.ResultCode;
import com.cijian.common.exception.BizException;
import com.cijian.interaction.dto.FollowVO;
import com.cijian.interaction.dto.UserVO;
import com.cijian.interaction.entity.Follow;
import com.cijian.interaction.feign.UserFeignClient;
import com.cijian.interaction.mapper.FollowMapper;
import com.cijian.interaction.service.FollowService;
import com.cijian.interaction.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowMapper followMapper;
    private final UserFeignClient userFeignClient;
    private final NotificationService notificationService;

    @Override
    @Transactional
    public boolean toggle(Long followerId, Long followedId) {
        if (followerId.equals(followedId)) {
            throw new BizException(ResultCode.BIZ_ERROR.getCode(), "不能关注自己");
        }

        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, followerId)
               .eq(Follow::getFollowedId, followedId);
        Follow existing = followMapper.selectOne(wrapper);

        boolean becameFollowing = false;
        if (existing != null) {
            int newStatus = existing.getStatus() == 1 ? 0 : 1;
            existing.setStatus(newStatus);
            followMapper.updateById(existing);
            becameFollowing = newStatus == 1;
        } else {
            Follow follow = new Follow();
            follow.setFollowerId(followerId);
            follow.setFollowedId(followedId);
            follow.setStatus(1);
            followMapper.insert(follow);
            becameFollowing = true;
        }
        if (becameFollowing) {
            try { notificationService.createNotification(followedId, followerId, "FOLLOW", null, null, "关注了你"); } catch (Exception ignored) {}
        }
        return becameFollowing;
    }

    @Override
    public boolean isFollowing(Long followerId, Long followedId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, followerId)
               .eq(Follow::getFollowedId, followedId)
               .eq(Follow::getStatus, 1);
        return followMapper.selectCount(wrapper) > 0;
    }

    @Override
    public List<FollowVO> listFollowers(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowedId, userId)
               .eq(Follow::getStatus, 1)
               .orderByDesc(Follow::getCreateTime);

        Page<Follow> page = followMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.getRecords().stream()
                .map(f -> toVO(f, f.getFollowerId()))
                .collect(Collectors.toList());
    }

    @Override
    public List<FollowVO> listFollowing(Long userId, int pageNum, int pageSize) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, userId)
               .eq(Follow::getStatus, 1)
               .orderByDesc(Follow::getCreateTime);

        Page<Follow> page = followMapper.selectPage(new Page<>(pageNum, pageSize), wrapper);
        return page.getRecords().stream()
                .map(f -> toVO(f, f.getFollowedId()))
                .collect(Collectors.toList());
    }

    @Override
    public long countFollowers(Long userId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowedId, userId)
               .eq(Follow::getStatus, 1);
        return followMapper.selectCount(wrapper);
    }

    @Override
    public long countFollowing(Long userId) {
        LambdaQueryWrapper<Follow> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Follow::getFollowerId, userId)
               .eq(Follow::getStatus, 1);
        return followMapper.selectCount(wrapper);
    }

    private FollowVO toVO(Follow f, Long displayUserId) {
        return FollowVO.builder()
                .id(f.getId())
                .followerId(f.getFollowerId())
                .followedId(f.getFollowedId())
                .status(f.getStatus())
                .userInfo(getUser(displayUserId))
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
