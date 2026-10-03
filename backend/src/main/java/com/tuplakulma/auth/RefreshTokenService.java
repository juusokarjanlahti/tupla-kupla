package com.tuplakulma.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Refresh tokens are opaque, high-entropy random values rather than JWTs: unlike the access
 * token, they must be revocable on demand (logout, or reuse detection below), which a
 * self-contained signed token can't be without a server-side denylist anyway. Only a SHA-256
 * hash of each token is persisted, so a stolen database row can't be replayed as a cookie value.
 */
@Service
public class RefreshTokenService {

  private static final int TOKEN_BYTES = 32;

  private final RefreshTokenRepository repository;
  private final long expirationMs;
  private final SecureRandom secureRandom = new SecureRandom();

  public RefreshTokenService(
      RefreshTokenRepository repository,
      @Value("${app.refresh-token.expiration-ms}") long expirationMs) {
    this.repository = repository;
    this.expirationMs = expirationMs;
  }

  public String issue(Long userId) {
    String rawToken = generateRawToken();
    repository.save(
        new RefreshToken(userId, hash(rawToken), Instant.now().plusMillis(expirationMs)));
    return rawToken;
  }

  /**
   * Validates a refresh token and rotates it: a stored token is single-use, so this revokes it
   * and issues its replacement in the same call. A token that's already revoked is presented
   * only if it was copied and reused after its legitimate owner already rotated past it, so that
   * case revokes every active token for the user rather than just the one presented.
   */
  // noRollbackFor is required here: revokeAllActiveTokensForUser's writes below must
  // commit even though this method then throws, which @Transactional would otherwise
  // roll back as it does for any unchecked exception by default.
  @Transactional(noRollbackFor = InvalidRefreshTokenException.class)
  public RotationResult rotate(String rawToken) {
    if (rawToken == null) {
      throw new InvalidRefreshTokenException();
    }
    RefreshToken stored =
        repository.findByTokenHash(hash(rawToken)).orElseThrow(InvalidRefreshTokenException::new);

    if (stored.isRevoked()) {
      revokeAllActiveTokensForUser(stored.getUserId());
      throw new InvalidRefreshTokenException();
    }
    if (stored.isExpired()) {
      throw new InvalidRefreshTokenException();
    }

    stored.revoke();
    repository.save(stored);
    return new RotationResult(stored.getUserId(), issue(stored.getUserId()));
  }

  public void revoke(String rawToken) {
    repository
        .findByTokenHash(hash(rawToken))
        .ifPresent(
            token -> {
              token.revoke();
              repository.save(token);
            });
  }

  private void revokeAllActiveTokensForUser(Long userId) {
    List<RefreshToken> active = repository.findByUserIdAndRevokedAtIsNull(userId);
    active.forEach(RefreshToken::revoke);
    repository.saveAll(active);
  }

  private String generateRawToken() {
    byte[] bytes = new byte[TOKEN_BYTES];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String rawToken) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 not available", e);
    }
  }

  public record RotationResult(Long userId, String refreshToken) {}
}
