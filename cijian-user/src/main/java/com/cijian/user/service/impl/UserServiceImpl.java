package com.cijian.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cijian.common.enums.ResultCode;
import com.cijian.common.exception.BizException;
import com.cijian.common.utils.JwtUtil;
import com.cijian.common.utils.RedisUtil;
import com.cijian.user.dto.*;
import com.cijian.user.entity.User;
import com.cijian.user.entity.UserStatus;
import com.cijian.user.mapper.UserMapper;
import com.cijian.user.service.EmailService;
import com.cijian.user.service.EmailValidator;
import com.cijian.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final EmailService emailService;
    private final EmailValidator emailValidator;

    private static final long TOKEN_EXPIRE_DAYS = 7;
    private static final String TOKEN_KEY_PREFIX = "token:";
    private static final String RESET_TOKEN_PREFIX = "reset:";
    private static final long RESET_TOKEN_EXPIRE_MINUTES = 30;
    private static final String VERIFY_TOKEN_PREFIX = "verify:";
    private static final long VERIFY_TOKEN_EXPIRE_HOURS = 24;
    private static final java.util.Random RANDOM = new java.util.Random();

    @Override
    public UserVO register(RegisterRequest request) {
        emailValidator.validate(request.getEmail());

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, request.getEmail());
        if (userMapper.selectCount(wrapper) > 0) {
            throw new BizException(ResultCode.BIZ_ERROR.getCode(), "邮箱已被注册");
        }

        LambdaQueryWrapper<User> nickWrapper = new LambdaQueryWrapper<>();
        nickWrapper.eq(User::getNickname, request.getNickname());
        if (userMapper.selectCount(nickWrapper) > 0) {
            throw new BizException(ResultCode.BIZ_ERROR.getCode(), "昵称已被使用");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setStatus(UserStatus.UNVERIFIED.getCode());
        userMapper.insert(user);

        String verifyCode = String.format("%06d", RANDOM.nextInt(1000000));
        redisUtil.set(VERIFY_TOKEN_PREFIX + verifyCode, String.valueOf(user.getId()),
                VERIFY_TOKEN_EXPIRE_HOURS, TimeUnit.HOURS);
        emailService.sendVerificationEmail(user.getEmail(), user.getNickname(), verifyCode);

        return toVO(user);
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, request.getEmail());
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "密码错误");
        }

        if (UserStatus.UNVERIFIED.getCode() == user.getStatus()) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "邮箱未验证，请先完成邮箱验证");
        }
        if (UserStatus.NORMAL.getCode() != user.getStatus()) {
            throw new BizException(ResultCode.FORBIDDEN.getCode(), "账号已被禁用或注销");
        }

        user.setLastLoginTime(LocalDateTime.now());
        userMapper.updateById(user);

        String token = jwtUtil.generateToken(user.getId(), user.getNickname());
        redisUtil.set(TOKEN_KEY_PREFIX + user.getId(), token, TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);

        return LoginResponse.builder()
                .token(token)
                .userInfo(toVO(user))
                .build();
    }

    @Override
    public UserVO getUserById(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        return toVO(user);
    }

    @Override
    public UserVO getUserByEmail(String email) {
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getEmail, email);
        User user = userMapper.selectOne(wrapper);
        if (user == null) {
            return null;
        }
        return toVO(user);
    }

    @Override
    public UserVO updateUser(Long id, UserUpdateRequest request) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        if (request.getNickname() != null) {
            LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(User::getNickname, request.getNickname())
                   .ne(User::getId, id);
            if (userMapper.selectCount(wrapper) > 0) {
                throw new BizException(ResultCode.BIZ_ERROR.getCode(), "昵称已被占用");
            }
            user.setNickname(request.getNickname());
        }
        if (request.getBio() != null) {
            user.setBio(request.getBio());
        }
        if (request.getAvatarUrl() != null) {
            user.setAvatarUrl(request.getAvatarUrl());
        }

        userMapper.updateById(user);
        return toVO(user);
    }

    @Override
    public void changePassword(Long id, String oldPassword, String newPassword) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }

        if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "旧密码错误");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
    }

    @Override
    public void logout(Long userId) {
        redisUtil.delete(TOKEN_KEY_PREFIX + userId);
    }

    @Override
    public LoginResponse refreshToken(String oldToken) {
        String newToken = jwtUtil.refreshToken(oldToken);
        if (newToken == null) {
            throw new BizException(ResultCode.UNAUTHORIZED.getCode(), "Token无效或已过期");
        }
        Long userId = jwtUtil.getUserIdFromToken(newToken);
        redisUtil.set(TOKEN_KEY_PREFIX + userId, newToken, TOKEN_EXPIRE_DAYS, TimeUnit.DAYS);
        User user = userMapper.selectById(userId);
        return LoginResponse.builder()
                .token(newToken)
                .userInfo(toVO(user))
                .build();
    }

    @Override
    public void forgotPassword(String email) {
        User user = userMapper.selectOne(
                new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            return;
        }
        String code = String.format("%06d", RANDOM.nextInt(1000000));
        redisUtil.set(RESET_TOKEN_PREFIX + code, String.valueOf(user.getId()),
                RESET_TOKEN_EXPIRE_MINUTES, TimeUnit.MINUTES);
        emailService.sendPasswordResetEmail(user.getEmail(), user.getNickname(), code);
    }

    @Override
    public void resetPassword(String resetToken, String newPassword) {
        String userIdStr = redisUtil.get(RESET_TOKEN_PREFIX + resetToken);
        if (userIdStr == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "重置令牌无效或已过期");
        }
        Long userId = Long.valueOf(userIdStr);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        redisUtil.delete(RESET_TOKEN_PREFIX + resetToken);
        redisUtil.delete(TOKEN_KEY_PREFIX + userId);
    }

    @Override
    public void verifyEmail(String code) {
        String userIdStr = redisUtil.get(VERIFY_TOKEN_PREFIX + code);
        if (userIdStr == null) {
            throw new BizException(ResultCode.BAD_REQUEST.getCode(), "验证码无效或已过期");
        }
        Long userId = Long.valueOf(userIdStr);
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BizException(ResultCode.NOT_FOUND.getCode(), "用户不存在");
        }
        if (UserStatus.UNVERIFIED.getCode() != user.getStatus()) {
            throw new BizException(ResultCode.BIZ_ERROR.getCode(), "该账号已验证");
        }
        user.setStatus(UserStatus.NORMAL.getCode());
        userMapper.updateById(user);
        redisUtil.delete(VERIFY_TOKEN_PREFIX + code);
        log.info("Email verified: userId={}", userId);
    }

    private UserVO toVO(User user) {
        return UserVO.builder()
                .id(user.getId())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .bio(user.getBio())
                .avatarUrl(user.getAvatarUrl())
                .status(user.getStatus())
                .role(user.getRole())
                .createdAt(user.getCreateTime())
                .build();
    }
}
