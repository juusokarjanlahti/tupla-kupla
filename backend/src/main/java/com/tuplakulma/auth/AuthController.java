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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final JwtService jwtService;
  private final String cookieName;
  private final boolean cookieSecure;

  public AuthController(
      AuthService authService,
      JwtService jwtService,
      @Value("${app.jwt.cookie-name}") String cookieName,
      @Value("${app.jwt.cookie-secure}") boolean cookieSecure) {
    this.authService = authService;
    this.jwtService = jwtService;
    this.cookieName = cookieName;
    this.cookieSecure = cookieSecure;
  }

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@RequestBody RegisterRequest request) {
    AuthResult result = authService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .header(HttpHeaders.SET_COOKIE, accessTokenCookie(result.accessToken()).toString())
        .body(result.user());
  }

  @PostMapping("/login")
  public ResponseEntity<UserResponse> login(@RequestBody LoginRequest request) {
    AuthResult result = authService.login(request);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, accessTokenCookie(result.accessToken()).toString())
        .body(result.user());
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout() {
    ResponseCookie cleared =
        ResponseCookie.from(cookieName, "")
            .httpOnly(true)
            .secure(cookieSecure)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build();
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cleared.toString()).build();
  }

  @GetMapping("/me")
  public ResponseEntity<UserResponse> me(Authentication authentication) {
    return ResponseEntity.ok(authService.getCurrentUser(authentication.getName()));
  }

  private ResponseCookie accessTokenCookie(String token) {
    return ResponseCookie.from(cookieName, token)
        .httpOnly(true)
        .secure(cookieSecure)
        .sameSite("Lax")
        .path("/")
        .maxAge(Duration.ofMillis(jwtService.getExpirationMs()))
        .build();
  }
}
