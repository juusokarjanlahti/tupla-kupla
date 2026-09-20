package com.tuplakulma.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.tuplakulma.auth.dto.AuthResponse;
import com.tuplakulma.auth.dto.LoginRequest;
import com.tuplakulma.auth.dto.RegisterRequest;
import com.tuplakulma.security.JwtService;
import com.tuplakulma.user.User;
import com.tuplakulma.user.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private JwtService jwtService;

  private AuthService authService;

  @org.junit.jupiter.api.BeforeEach
  void setUp() {
    authService = new AuthService(userRepository, passwordEncoder, jwtService);
  }

  @Test
  void registerHashesPasswordAndIssuesToken() {
    when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("hashed");
    when(jwtService.generateToken("user@example.com")).thenReturn("a-token");

    AuthResponse response =
        authService.register(new RegisterRequest("User@Example.com", "password123"));

    assertThat(response.accessToken()).isEqualTo("a-token");
    verify(userRepository)
        .save(
            org.mockito.ArgumentMatchers.argThat(
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
  void loginIssuesTokenOnMatchingPassword() {
    User user = new User("user@example.com", "hashed");
    when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
    when(passwordEncoder.matches("password123", "hashed")).thenReturn(true);
    when(jwtService.generateToken("user@example.com")).thenReturn("a-token");

    AuthResponse response = authService.login(new LoginRequest("user@example.com", "password123"));

    assertThat(response.accessToken()).isEqualTo("a-token");
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
}
