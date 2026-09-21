package com.tuplakulma.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuplakulma.auth.dto.LoginRequest;
import com.tuplakulma.auth.dto.RegisterRequest;
import com.tuplakulma.auth.dto.UserResponse;
import com.tuplakulma.security.JwtService;
import jakarta.servlet.http.Cookie;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean private AuthService authService;
  @MockitoBean private RefreshTokenService refreshTokenService;

  // JwtAuthenticationFilter is picked up by the @WebMvcTest slice because it's a servlet
  // Filter; it still needs a JwtService to construct even though addFilters = false keeps
  // it out of the MockMvc chain. AuthController also depends on it directly for the cookie's
  // max-age.
  @MockitoBean private JwtService jwtService;

  @Test
  void registerReturns201WithUserAndSetsCookies() throws Exception {
    given(jwtService.getExpirationMs()).willReturn(900_000L);
    given(authService.register(any()))
        .willReturn(
            new AuthResult(
                new UserResponse(1L, "user@example.com"), "a-token", "a-refresh-token"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RegisterRequest("user@example.com", "password123"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(cookie().exists("access_token"))
        .andExpect(cookie().httpOnly("access_token", true))
        .andExpect(cookie().value("access_token", "a-token"))
        .andExpect(cookie().httpOnly("refresh_token", true))
        .andExpect(cookie().value("refresh_token", "a-refresh-token"))
        .andExpect(cookie().path("refresh_token", "/api/auth"));
  }

  @Test
  void registerReturns409WhenEmailTaken() throws Exception {
    given(authService.register(any()))
        .willThrow(new EmailAlreadyInUseException("user@example.com"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RegisterRequest("user@example.com", "password123"))))
        .andExpect(status().isConflict());
  }

  @Test
  void loginReturns200WithUserAndSetsCookies() throws Exception {
    given(jwtService.getExpirationMs()).willReturn(900_000L);
    given(authService.login(any()))
        .willReturn(
            new AuthResult(
                new UserResponse(1L, "user@example.com"), "a-token", "a-refresh-token"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LoginRequest("user@example.com", "password123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(cookie().value("access_token", "a-token"))
        .andExpect(cookie().value("refresh_token", "a-refresh-token"));
  }

  @Test
  void loginReturns401OnInvalidCredentials() throws Exception {
    given(authService.login(any())).willThrow(new InvalidCredentialsException());

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(new LoginRequest("user@example.com", "wrong"))))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void refreshReturnsNewCookiesOnValidToken() throws Exception {
    given(jwtService.getExpirationMs()).willReturn(900_000L);
    given(authService.refresh("old-refresh-token"))
        .willReturn(
            new AuthResult(
                new UserResponse(1L, "user@example.com"), "new-token", "new-refresh-token"));

    mockMvc
        .perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "old-refresh-token")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(cookie().value("access_token", "new-token"))
        .andExpect(cookie().value("refresh_token", "new-refresh-token"));
  }

  @Test
  void refreshReturns401AndClearsCookiesWhenTokenInvalid() throws Exception {
    given(authService.refresh("bad-token")).willThrow(new InvalidRefreshTokenException());

    mockMvc
        .perform(post("/api/auth/refresh").cookie(new Cookie("refresh_token", "bad-token")))
        .andExpect(status().isUnauthorized())
        .andExpect(cookie().maxAge("access_token", 0))
        .andExpect(cookie().maxAge("refresh_token", 0));
  }

  @Test
  void refreshReturns401WhenNoCookiePresent() throws Exception {
    given(authService.refresh(isNull())).willThrow(new InvalidRefreshTokenException());

    mockMvc
        .perform(post("/api/auth/refresh"))
        .andExpect(status().isUnauthorized())
        .andExpect(cookie().maxAge("access_token", 0))
        .andExpect(cookie().maxAge("refresh_token", 0));
  }

  @Test
  void logoutClearsBothCookies() throws Exception {
    mockMvc
        .perform(post("/api/auth/logout"))
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge("access_token", 0))
        .andExpect(cookie().maxAge("refresh_token", 0));

    verify(refreshTokenService, never()).revoke(any());
  }

  @Test
  void logoutRevokesRefreshTokenWhenCookiePresent() throws Exception {
    mockMvc
        .perform(post("/api/auth/logout").cookie(new Cookie("refresh_token", "a-refresh-token")))
        .andExpect(status().isNoContent());

    verify(refreshTokenService).revoke(eq("a-refresh-token"));
  }

  @Test
  void meReturnsCurrentUser() throws Exception {
    var authentication =
        new UsernamePasswordAuthenticationToken(
            "user@example.com", null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    given(authService.getCurrentUser("user@example.com"))
        .willReturn(new UserResponse(1L, "user@example.com"));

    // addFilters = false skips the filter that normally bridges SecurityContextHolder to
    // HttpServletRequest#getUserPrincipal(), which is what the Authentication argument
    // resolver reads; set the principal on the request directly instead.
    mockMvc
        .perform(get("/api/auth/me").principal(authentication))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("user@example.com"));
  }
}
