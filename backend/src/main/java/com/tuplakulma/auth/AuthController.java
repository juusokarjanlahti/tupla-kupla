package com.tuplakulma.auth;

import com.tuplakulma.auth.dto.LoginRequest;
import com.tuplakulma.auth.dto.RegisterRequest;
import com.tuplakulma.auth.dto.UserResponse;
import com.tuplakulma.security.JwtService;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private static final String AUTH_PATH = "/api/auth";

  private final AuthService authService;
  private final RefreshTokenService refreshTokenService;
  private final JwtService jwtService;
  private final String accessCookieName;
  private final String refreshCookieName;
  private final boolean cookieSecure;
  private final long refreshExpirationMs;

  public AuthController(
      AuthService authService,
      RefreshTokenService refreshTokenService,
      JwtService jwtService,
      @Value("${app.jwt.cookie-name}") String accessCookieName,
      @Value("${app.refresh-token.cookie-name}") String refreshCookieName,
      @Value("${app.jwt.cookie-secure}") boolean cookieSecure,
      @Value("${app.refresh-token.expiration-ms}") long refreshExpirationMs) {
    this.authService = authService;
    this.refreshTokenService = refreshTokenService;
    this.jwtService = jwtService;
    this.accessCookieName = accessCookieName;
    this.refreshCookieName = refreshCookieName;
    this.cookieSecure = cookieSecure;
    this.refreshExpirationMs = refreshExpirationMs;
  }

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
    AuthResult result = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, accessTokenCookie(result.accessToken()).toString())
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken()).toString())
        .body(result.user());
  }

  @PostMapping("/login")
  public ResponseEntity<UserResponse> login(@RequestBody LoginRequest request) {
    AuthResult result = authService.login(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, accessTokenCookie(result.accessToken()).toString())
        .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken()).toString())
        .body(result.user());
  }

  @PostMapping("/refresh")
  public ResponseEntity<UserResponse> refresh(
      @CookieValue(value = "${app.refresh-token.cookie-name}", required = false)
          String refreshToken) {
    try {
      AuthResult result = authService.refresh(refreshToken);
      return ResponseEntity.ok()
          .header(HttpHeaders.SET_COOKIE, accessTokenCookie(result.accessToken()).toString())
          .header(HttpHeaders.SET_COOKIE, refreshTokenCookie(result.refreshToken()).toString())
          .body(result.user());
    } catch (InvalidRefreshTokenException ex) {
      return unauthorizedClearingCookies();
    }
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue(value = "${app.refresh-token.cookie-name}", required = false)
          String refreshToken) {
    if (refreshToken != null) {
      refreshTokenService.revoke(refreshToken);
    }
    return ResponseEntity.noContent()
        .header(HttpHeaders.SET_COOKIE, clearedCookie(accessCookieName, "/").toString())
        .header(HttpHeaders.SET_COOKIE, clearedCookie(refreshCookieName, AUTH_PATH).toString())
        .build();
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> me(Authentication authentication) {
    return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
  }

  private ResponseEntity<UserResponse> unauthorizedClearingCookies() {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .header(HttpHeaders.SET_COOKIE, clearedCookie(accessCookieName, "/").toString())
        .header(HttpHeaders.SET_COOKIE, clearedCookie(refreshCookieName, AUTH_PATH).toString())
        .build();
  }

  private ResponseCookie accessTokenCookie(String token) {
    return ResponseCookie.from(accessCookieName, token)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/")
        .maxAge(Duration.ofMillis(jwtService.getExpirationMs()))
        .build();
  }

  private ResponseCookie refreshTokenCookie(String token) {
    return ResponseCookie.from(refreshCookieName, token)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path(AUTH_PATH)
        .maxAge(Duration.ofMillis(refreshExpirationMs))
        .build();
  }

  private ResponseCookie clearedCookie(String name, String path) {
    return ResponseCookie.from(name, "")
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path(path)
        .maxAge(0)
        .build();
  }
}
