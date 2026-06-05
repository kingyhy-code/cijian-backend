package com.cijian.user.service;

import com.cijian.user.dto.*;

public interface UserService {

    UserVO register(RegisterRequest request);

    LoginResponse login(LoginRequest request);

    UserVO getUserById(Long id);

    UserVO getUserByEmail(String email);

    UserVO updateUser(Long id, UserUpdateRequest request);

    void changePassword(Long id, String oldPassword, String newPassword);

    void logout(Long userId);

    LoginResponse refreshToken(String oldToken);

    void forgotPassword(String email);

    void resetPassword(String resetToken, String newPassword);

    void verifyEmail(String code);
}
