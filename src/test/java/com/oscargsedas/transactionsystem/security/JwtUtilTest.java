package com.oscargsedas.transactionsystem.security;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class JwtUtilTest {

    @Test
    void initShouldFailWhenSecretIsTooShort() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "short-secret");

        assertThrows(IllegalStateException.class, jwtUtil::init);
    }

    @Test
    void initShouldSucceedWhenSecretHasAtLeast32Bytes() {
        JwtUtil jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret", "dev-jwt-secret-change-me-32-bytes-minimum-key");

        assertDoesNotThrow(jwtUtil::init);
    }
}

