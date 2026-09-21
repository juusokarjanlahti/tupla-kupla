package com.tuplakulma.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
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

  // JwtAuthenticationFilter is picked up by the @WebMvcTest slice because it's a servlet
  // Filter; it still needs a JwtService to construct even though addFilters = false keeps
  // it out of the MockMvc chain. AuthController also depends on it directly for the cookie's
  // max-age.
  @MockitoBean private JwtService jwtService;

  @Test
  void registerReturns201WithUserAndSetsCookie() throws Exception {
    given(jwtService.getExpirationMs()).willReturn(3_600_000L);
    given(authService.register(any()))
        .willReturn(new AuthResult(new UserResponse(1L, "user@example.com"), "a-token"));

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
        .andExpect(cookie().value("access_token", "a-token"));
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
  void loginReturns200WithUserAndSetsCookie() throws Exception {
    given(jwtService.getExpirationMs()).willReturn(3_600_000L);
    given(authService.login(any()))
        .willReturn(new AuthResult(new UserResponse(1L, "user@example.com"), "a-token"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LoginRequest("user@example.com", "password123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("user@example.com"))
        .andExpect(cookie().value("access_token", "a-token"));
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
  void logoutClearsCookie() throws Exception {
    mockMvc
        .perform(post("/api/auth/logout"))
        .andExpect(status().isNoContent())
        .andExpect(cookie().maxAge("access_token", 0));
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
