package com.tuplakulma.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tuplakulma.auth.dto.LoginRequest;
import com.tuplakulma.auth.dto.RegisterRequest;
import com.tuplakulma.security.JwtService;
import com.tuplakulma.user.User;
import com.tuplakulma.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenService refreshTokenService;

  private AuthService authService;

  @BeforeEach
  void setUp() {
    authService =
        new AuthService(userRepository, passwordEncoder, jwtService, refreshTokenService);
  }

  @Test
  void registerHashesPasswordAndIssuesTokens() {
    when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hashed");
    when(jwtService.generateToken("user@example.com")).thenReturn("a-token");
    when(refreshTokenService.issue(any())).thenReturn("a-refresh-token");

    AuthResult result =
        authService.register(new RegisterRequest("User@Example.com", "password123"));

    assertThat(result.accessToken()).isEqualTo("a-token");
    assertThat(result.refreshToken()).isEqualTo("a-refresh-token");
    assertThat(result.user().email()).isEqualTo("user@example.com");
    verify(userRepository)
        .save(
            ArgumentMatchers.argThat(
                user ->
                    user.getEmail().equals("user@example.com")
                        && user.getPasswordHash().equals("hashed")));
  }

  @Test
  void registerRejectsDuplicateEmail() {
    when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

    assertThatThrownBy(
            () -> authService.register(new RegisterRequest("user@example.com", "password123")))
        .isInstanceOf(EmailAlreadyInUseException.class);
  }

  @Test
  void registerRejectsShortPassword() {
    assertThatThrownBy(() -> authService.register(new RegisterRequest("user@example.com", "short")))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void loginIssuesTokensOnMatchingPassword() {
    User user = new User("user@example.com", "hashed");
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
    when(jwtService.generateToken("user@example.com")).thenReturn("a-token");
    when(refreshTokenService.issue(any())).thenReturn("a-refresh-token");

    AuthResult result = authService.login(new LoginRequest("user@example.com", "password123"));

    assertThat(result.accessToken()).isEqualTo("a-token");
    assertThat(result.refreshToken()).isEqualTo("a-refresh-token");
    assertThat(result.user().email()).isEqualTo("user@example.com");
  }

  @Test
  void loginRejectsUnknownEmail() {
    when(userRepository.findByEmail(eq("nobody@example.com"))).thenReturn(Optional.empty());

    assertThatThrownBy(
            () -> authService.login(new LoginRequest("nobody@example.com", "password123")))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void loginRejectsWrongPassword() {
    User user = new User("user@example.com", "hashed");
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches(any(), eq("hashed"))).thenReturn(false);

    assertThatThrownBy(
            () -> authService.login(new LoginRequest("user@example.com", "wrong-password")))
        .isInstanceOf(InvalidCredentialsException.class);
  }

  @Test
  void refreshRotatesTokenAndIssuesNewAccessToken() {
    User user = new User("user@example.com", "hashed");
    when(refreshTokenService.rotate("old-refresh-token"))
        .thenReturn(new RefreshTokenService.RotationResult(42L, "new-refresh-token"));
    when(userRepository.findById(42L)).thenReturn(Optional.of(user));
    when(jwtService.generateToken("user@example.com")).thenReturn("new-access-token");

    AuthResult result = authService.refresh("old-refresh-token");

    assertThat(result.accessToken()).isEqualTo("new-access-token");
    assertThat(result.refreshToken()).isEqualTo("new-refresh-token");
    assertThat(result.user().email()).isEqualTo("user@example.com");
  }

  @Test
  void refreshThrowsWhenUserNoLongerExists() {
    when(refreshTokenService.rotate("old-refresh-token"))
        .thenReturn(new RefreshTokenService.RotationResult(42L, "new-refresh-token"));
    when(userRepository.findById(42L)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.refresh("old-refresh-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void getCurrentUserReturnsMatchingUser() {
    User user = new User("user@example.com", "hashed");
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

    var response = authService.getCurrentUser("user@example.com");

    assertThat(response.email()).isEqualTo("user@example.com");
  }

  @Test
  void getCurrentUserThrowsWhenUserNoLongerExists() {
    when(userRepository.findByEmail("gone@example.com")).thenReturn(Optional.empty());

    assertThatThrownBy(() -> authService.getCurrentUser("gone@example.com"))
        .isInstanceOf(InvalidCredentialsException.class);
  }
}
