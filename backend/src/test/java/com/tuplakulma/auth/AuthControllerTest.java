package com.tuplakulma.auth;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tuplakulma.auth.dto.AuthResponse;
import com.tuplakulma.auth.dto.LoginRequest;
import com.tuplakulma.auth.dto.RegisterRequest;
import com.tuplakulma.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
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
  // it out of the MockMvc chain.
  @MockitoBean private JwtService jwtService;

  @Test
  void registerReturns201WithToken() throws Exception {
    given(authService.register(any())).willReturn(new AuthResponse("a-token"));

    mockMvc
        .perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new RegisterRequest("user@example.com", "password123"))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.accessToken").value("a-token"))
        .andExpect(jsonPath("$.tokenType").value("Bearer"));
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
  void loginReturns200WithToken() throws Exception {
    given(authService.login(any())).willReturn(new AuthResponse("a-token"));

    mockMvc
        .perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    objectMapper.writeValueAsString(
                        new LoginRequest("user@example.com", "password123"))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.accessToken").value("a-token"));
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
}
