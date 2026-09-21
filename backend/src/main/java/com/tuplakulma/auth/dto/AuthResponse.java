package com.tuplakulma.auth.dto;

public record AuthResponse(String accessToken, String tokenType) {

  public AuthResponse(String accessToken) {
    this(accessToken, "Bearer");
  }
}
