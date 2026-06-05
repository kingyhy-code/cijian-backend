package com.cijian.user.controller;

import com.cijian.common.enums.ResultCode;
import com.cijian.common.exception.BizException;
import com.cijian.common.result.R;
import com.cijian.common.utils.JwtUtil;
import com.cijian.user.dto.*;
import com.cijian.user.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    @PostMapping("/register")
    public R<LoginResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserVO userVO = userService.register(request);
        String token = jwtUtil.generateToken(userVO.getId(), userVO.getNickname());
        LoginResponse resp = LoginResponse.builder().token(token).userInfo(userVO).build();
        return R.success("注册成功", resp);
    }

    @PostMapping("/login")
    public R<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse resp = userService.login(request);
        return R.success(resp);
    }

    @GetMapping("/info")
    public R<UserVO> info(@RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        UserVO userVO = userService.getUserById(userId);
        return R.success(userVO);
    }

    @PutMapping("/info")
    public R<UserVO> updateInfo(@RequestHeader("Authorization") String authHeader,
                                 @Valid @RequestBody UserUpdateRequest request) {
        Long userId = extractUserId(authHeader);
        UserVO userVO = userService.updateUser(userId, request);
        return R.success("更新成功", userVO);
    }

    @PostMapping("/password")
    public R<?> changePassword(@RequestHeader("Authorization") String authHeader,
                                @Valid @RequestBody ChangePasswordRequest request) {
        Long userId = extractUserId(authHeader);
        userService.changePassword(userId, request.getOldPassword(), request.getNewPassword());
        return R.success("密码修改成功");
    }

    @PostMapping("/logout")
    public R<?> logout(@RequestHeader("Authorization") String authHeader) {
        Long userId = extractUserId(authHeader);
        userService.logout(userId);
        return R.success("已退出登录");
    }

    @PostMapping("/token/refresh")
    public R<LoginResponse> refreshToken(@RequestHeader("Authorization") String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        LoginResponse resp = userService.refreshToken(authHeader.substring(7));
        return R.success("Token已刷新", resp);
    }

    @PostMapping("/forgot-password")
    public R<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        userService.forgotPassword(request.getEmail());
        return R.success("如果该邮箱已注册，重置邮件已发送");
    }

    @PostMapping("/reset-password")
    public R<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        userService.resetPassword(request.getCode(), request.getNewPassword());
        return R.success("密码重置成功，请重新登录");
    }

    @PostMapping("/verify-email")
    public R<?> verifyEmail(@RequestParam String code) {
        userService.verifyEmail(code);
        return R.success("邮箱验证成功，现在可以登录了");
    }

    @GetMapping("/{id}")
    public R<UserVO> getUserById(@PathVariable("id") Long id) {
        UserVO userVO = userService.getUserById(id);
        return R.success(userVO);
    }

    private Long extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        String token = authHeader.substring(7);
        Long userId = jwtUtil.getUserIdFromToken(token);
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }
}
