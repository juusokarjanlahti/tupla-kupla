package com.tuplakulma.auth;

import com.tuplakulma.auth.dto.LoginRequest;
import com.tuplakulma.auth.dto.RegisterRequest;
import com.tuplakulma.auth.dto.UserResponse;
import com.tuplakulma.security.JwtService;
import com.tuplakulma.user.User;
import com.tuplakulma.user.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

  private static final int MIN_PASSWORD_LENGTH = 8;

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  public AuthService(
      UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
  }

  public AuthResult register(RegisterRequest request) {
    String email = normalizeEmail(request.email());
    if (request.password() == null || request.password().length() < MIN_PASSWORD_LENGTH) {
      throw new IllegalArgumentException(
          "Password must be at least " + MIN_PASSWORD_LENGTH + " characters");
    }
    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyInUseException(email);
    }

    User user = new User(email, passwordEncoder.encode(request.password()));
    userRepository.save(user);

    return toAuthResult(user);
  }

  public AuthResult login(LoginRequest request) {
    String email = normalizeEmail(request.email());
    User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);

    if (request.password() == null
        || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }

    return toAuthResult(user);
  }

  public UserResponse getCurrentUser(String email) {
    User user = userRepository.findByEmail(email).orElseThrow(InvalidCredentialsException::new);
    return new UserResponse(user.getId(), user.getEmail());
  }

  private AuthResult toAuthResult(User user) {
    return new AuthResult(
        new UserResponse(user.getId(), user.getEmail()), jwtService.generateToken(user.getEmail()));
  }

  private String normalizeEmail(String email) {
    if (email == null || email.isBlank()) {
      throw new IllegalArgumentException("Email is required");
    }
    return email.strip().toLowerCase();
  }
}
