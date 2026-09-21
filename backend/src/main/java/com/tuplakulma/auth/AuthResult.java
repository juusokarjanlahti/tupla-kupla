package com.tuplakulma.auth;

import com.tuplakulma.auth.dto.UserResponse;

/** Pairs the signed JWT (goes into the cookie) with the user info returned in the body. */
public record AuthResult(UserResponse user, String accessToken) {}
