package com.tuplakulma.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

  private JwtService jwtService;

  @BeforeEach
  void setUp() {
    jwtService = new JwtService("test-secret-key-at-least-32-bytes-long!!", 60_000);
  }

  @Test
  void generatesTokenThatCarriesTheSubject() {
    String token = jwtService.generateToken("user@example.com");

    assertThat(jwtService.isTokenValid(token)).isTrue();
    assertThat(jwtService.extractSubject(token)).isEqualTo("user@example.com");
  }

  @Test
  void rejectsTamperedToken() {
    String token = jwtService.generateToken("user@example.com");
    String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

    assertThat(jwtService.isTokenValid(tampered)).isFalse();
  }

  @Test
  void rejectsTokenSignedWithADifferentKey() {
    JwtService otherService = new JwtService("a-completely-different-32-byte-secret!!", 60_000);
    String token = otherService.generateToken("user@example.com");

    assertThat(jwtService.isTokenValid(token)).isFalse();
  }

  @Test
  void rejectsGarbageInput() {
    assertThat(jwtService.isTokenValid("not-a-jwt")).isFalse();
  }

  @Test
  void rejectsExpiredToken() throws InterruptedException {
    JwtService shortLived = new JwtService("test-secret-key-at-least-32-bytes-long!!", 1);
    String token = shortLived.generateToken("user@example.com");

    Thread.sleep(10);

    assertThat(shortLived.isTokenValid(token)).isFalse();
  }
}
