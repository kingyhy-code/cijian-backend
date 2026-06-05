package com.cijian.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cijian.common.enums.ResultCode;
import com.cijian.common.exception.BizException;
import com.cijian.common.utils.JwtUtil;
import com.cijian.common.utils.RedisUtil;
import com.cijian.user.dto.LoginRequest;
import com.cijian.user.dto.RegisterRequest;
import com.cijian.user.dto.UserUpdateRequest;
import com.cijian.user.entity.User;
import com.cijian.user.entity.UserStatus;
import com.cijian.user.mapper.UserMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserMapper userMapper;
    @Mock
    private BCryptPasswordEncoder passwordEncoder;
    @Mock
    private JwtUtil jwtUtil;
    @Mock
    private RedisUtil redisUtil;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void register_shouldCreateUserAndReturnVO() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("test@cijian.com");
        request.setPassword("password123");
        request.setNickname("testUser");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
        when(passwordEncoder.encode("password123")).thenReturn("hashedPassword");

        var userVO = userService.register(request);

        assertThat(userVO.getNickname()).isEqualTo("testUser");
        assertThat(userVO.getEmail()).isEqualTo("test@cijian.com");
        verify(userMapper).insert(any(User.class));
    }

    @Test
    void register_shouldThrowWhenEmailExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@cijian.com");
        request.setPassword("password123");
        request.setNickname("testUser");

        when(userMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);

        assertThatThrownBy(() -> userService.register(request))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ResultCode.BIZ_ERROR.getCode());
    }

    @Test
    void login_shouldReturnTokenAndUserInfo() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@cijian.com");
        request.setPassword("password123");

        User user = new User();
        user.setId(1L);
        user.setEmail("test@cijian.com");
        user.setNickname("testUser");
        user.setPasswordHash("hashedPassword");
        user.setStatus(UserStatus.NORMAL.getCode());

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("password123", "hashedPassword")).thenReturn(true);
        when(jwtUtil.generateToken(1L, "testUser")).thenReturn("jwt.token.here");

        var resp = userService.login(request);

        assertThat(resp.getToken()).isEqualTo("jwt.token.here");
        assertThat(resp.getUserInfo().getNickname()).isEqualTo("testUser");
        verify(redisUtil).set(eq("token:1"), eq("jwt.token.here"), eq(7L), eq(TimeUnit.DAYS));
    }

    @Test
    void login_shouldThrowWhenPasswordWrong() {
        LoginRequest request = new LoginRequest();
        request.setEmail("test@cijian.com");
        request.setPassword("wrongPassword");

        User user = new User();
        user.setId(1L);
        user.setPasswordHash("hashedPassword");
        user.setStatus(UserStatus.NORMAL.getCode());

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches("wrongPassword", "hashedPassword")).thenReturn(false);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ResultCode.UNAUTHORIZED.getCode());
    }

    @Test
    void login_shouldThrowWhenAccountDisabled() {
        LoginRequest request = new LoginRequest();
        request.setEmail("disabled@cijian.com");
        request.setPassword("password123");

        User user = new User();
        user.setStatus(2);

        when(userMapper.selectOne(any(LambdaQueryWrapper.class))).thenReturn(user);
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        assertThatThrownBy(() -> userService.login(request))
                .isInstanceOf(BizException.class)
                .extracting("code")
                .isEqualTo(ResultCode.FORBIDDEN.getCode());
    }

    @Test
    void logout_shouldDeleteTokenFromRedis() {
        userService.logout(1L);
        verify(redisUtil).delete("token:1");
    }

    @Test
    void changePassword_shouldUpdateHash() {
        User user = new User();
        user.setId(1L);
        user.setPasswordHash("oldHash");

        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.matches("oldPass", "oldHash")).thenReturn(true);
        when(passwordEncoder.encode("newPass")).thenReturn("newHash");

        userService.changePassword(1L, "oldPass", "newPass");

        verify(userMapper).updateById(argThat(u -> "newHash".equals(u.getPasswordHash())));
    }

    @Test
    void refreshToken_shouldReturnNewToken() {
        User user = new User();
        user.setId(1L);
        user.setNickname("testUser");
        user.setEmail("test@cijian.com");

        when(jwtUtil.refreshToken("old.token")).thenReturn("new.token");
        when(jwtUtil.getUserIdFromToken("new.token")).thenReturn(1L);
        when(userMapper.selectById(1L)).thenReturn(user);

        var resp = userService.refreshToken("old.token");

        assertThat(resp.getToken()).isEqualTo("new.token");
        verify(redisUtil).set("token:1", "new.token", 7, TimeUnit.DAYS);
    }

    @Test
    void resetPassword_shouldUpdatePasswordAndClearSessions() {
        User user = new User();
        user.setId(1L);
        user.setPasswordHash("oldHash");

        when(redisUtil.get("reset:test-uuid")).thenReturn("1");
        when(userMapper.selectById(1L)).thenReturn(user);
        when(passwordEncoder.encode("newPassword")).thenReturn("newHash");

        userService.resetPassword("test-uuid", "newPassword");

        verify(userMapper).updateById(argThat(u -> "newHash".equals(u.getPasswordHash())));
        verify(redisUtil).delete("reset:test-uuid");
        verify(redisUtil).delete("token:1");
    }
}
