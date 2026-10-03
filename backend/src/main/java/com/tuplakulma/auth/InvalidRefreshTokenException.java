package com.tuplakulma.auth;

public class InvalidRefreshTokenException extends RuntimeException {

  public InvalidRefreshTokenException() {
    super("Invalid or expired refresh token");
  }
}
