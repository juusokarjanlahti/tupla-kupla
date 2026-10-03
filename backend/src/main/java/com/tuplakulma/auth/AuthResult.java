package com.tuplakulma.auth;

import com.tuplakulma.auth.dto.UserResponse;

/**
 * Pairs the signed access token and opaque refresh token (both go into cookies) with the user info
 * returned in the body.
 */
public record AuthResult(UserResponse user, String accessToken, String refreshToken) {}
