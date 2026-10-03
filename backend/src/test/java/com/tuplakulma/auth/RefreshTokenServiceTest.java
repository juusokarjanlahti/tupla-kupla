package com.tuplakulma.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

  private static final long EXPIRATION_MS = 604_800_000L;

  @Mock private RefreshTokenRepository repository;

  private RefreshTokenService refreshTokenService;

  @BeforeEach
  void setUp() {
    refreshTokenService = new RefreshTokenService(repository, EXPIRATION_MS);
  }

  @Test
  void issueStoresHashedTokenAndReturnsRawToken() {
    ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);

    String rawToken = refreshTokenService.issue(1L);

    verify(repository).save(captor.capture());
    RefreshToken saved = captor.getValue();
    assertThat(saved.getUserId()).isEqualTo(1L);
    assertThat(saved.getTokenHash()).hasSize(64).isNotEqualTo(rawToken);
    assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    assertThat(saved.isRevoked()).isFalse();
  }

  @Test
  void rotateRevokesOldTokenAndIssuesNewOneForSameUser() {
    RefreshToken stored = new RefreshToken(1L, "irrelevant", Instant.now().plusSeconds(60));
    when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));

    RefreshTokenService.RotationResult result = refreshTokenService.rotate("raw-token");

    assertThat(result.userId()).isEqualTo(1L);
    assertThat(result.refreshToken()).isNotBlank();
    assertThat(stored.isRevoked()).isTrue();
    verify(repository, times(2)).save(any());
    verify(repository, never()).findByUserIdAndRevokedAtIsNull(any());
  }

  @Test
  void rotateRevokesEveryActiveTokenWhenAnAlreadyRevokedTokenIsPresented() {
    RefreshToken stored = new RefreshToken(1L, "irrelevant", Instant.now().plusSeconds(60));
    stored.revoke();
    when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));
    RefreshToken otherActive = new RefreshToken(1L, "other-hash", Instant.now().plusSeconds(60));
    when(repository.findByUserIdAndRevokedAtIsNull(1L)).thenReturn(List.of(otherActive));

    assertThatThrownBy(() -> refreshTokenService.rotate("stolen-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);

    assertThat(otherActive.isRevoked()).isTrue();
    verify(repository).saveAll(List.of(otherActive));
    verify(repository, never()).save(any());
  }

  @Test
  void rotateDoesNotRollBackTheReuseRevocationItThrowsAfter() throws NoSuchMethodException {
    // @Transactional rolls back on any unchecked exception by default, which would silently
    // undo the reuse-detection revocation since rotate() always throws right after it. Mockito
    // doesn't run a real transaction, so this checks the annotation directly rather than the
    // (unobservable here) rollback behavior.
    Method rotate = RefreshTokenService.class.getMethod("rotate", String.class);
    Transactional transactional = rotate.getAnnotation(Transactional.class);

    assertThat(transactional).isNotNull();
    assertThat(transactional.noRollbackFor()).contains(InvalidRefreshTokenException.class);
  }

  @Test
  void rotateRejectsExpiredToken() {
    RefreshToken expired = new RefreshToken(1L, "irrelevant", Instant.now().minusSeconds(1));
    when(repository.findByTokenHash(any())).thenReturn(Optional.of(expired));

    assertThatThrownBy(() -> refreshTokenService.rotate("raw-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);
    verify(repository, never()).save(any());
  }

  @Test
  void rotateRejectsUnknownToken() {
    when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> refreshTokenService.rotate("raw-token"))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void rotateRejectsNullToken() {
    assertThatThrownBy(() -> refreshTokenService.rotate(null))
        .isInstanceOf(InvalidRefreshTokenException.class);
  }

  @Test
  void revokeMarksMatchingTokenRevoked() {
    RefreshToken stored = new RefreshToken(1L, "irrelevant", Instant.now().plusSeconds(60));
    when(repository.findByTokenHash(any())).thenReturn(Optional.of(stored));

    refreshTokenService.revoke("raw-token");

    assertThat(stored.isRevoked()).isTrue();
    verify(repository).save(stored);
  }

  @Test
  void revokeIsANoOpWhenTokenIsUnknown() {
    when(repository.findByTokenHash(any())).thenReturn(Optional.empty());

    refreshTokenService.revoke("raw-token");

    verify(repository, never()).save(any());
  }
}
