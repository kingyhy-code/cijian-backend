package com.cijian.common.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "test-secret-key-for-junit-testing-purposes");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 3600000L);
    }

    @Test
    void generateToken_shouldReturnValidToken() {
        String token = jwtUtil.generateToken(1L, "testUser");
        assertThat(token).isNotBlank();
    }

    @Test
    void getUserIdFromToken_shouldReturnCorrectUserId() {
        String token = jwtUtil.generateToken(1L, "testUser");
        assertThat(jwtUtil.getUserIdFromToken(token)).isEqualTo(1L);
    }

    @Test
    void getUserIdFromToken_shouldReturnNullForInvalidToken() {
        assertThat(jwtUtil.getUserIdFromToken("invalid.token.here")).isNull();
    }

    @Test
    void validateToken_shouldReturnTrueForValidToken() {
        String token = jwtUtil.generateToken(1L, "testUser");
        assertThat(jwtUtil.validateToken(token)).isTrue();
    }

    @Test
    void validateToken_shouldReturnFalseForInvalidToken() {
        assertThat(jwtUtil.validateToken("invalid.token")).isFalse();
    }

    @Test
    void refreshToken_shouldReturnNewTokenWithSameClaims() {
        String oldToken = jwtUtil.generateToken(1L, "testUser");
        String newToken = jwtUtil.refreshToken(oldToken);
        assertThat(newToken).isNotNull().isNotEqualTo(oldToken);
        assertThat(jwtUtil.getUserIdFromToken(newToken)).isEqualTo(1L);
    }

    @Test
    void refreshToken_shouldReturnNullForInvalidToken() {
        assertThat(jwtUtil.refreshToken("invalid.token")).isNull();
    }
}
