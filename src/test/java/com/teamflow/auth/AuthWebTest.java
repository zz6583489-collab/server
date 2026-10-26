package com.teamflow.auth;

import static org.hamcrest.Matchers.blankOrNullString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.teamflow.auth.controller.AuthController;
import com.teamflow.auth.repository.AuthSessionMapper;
import com.teamflow.auth.repository.LoginAttemptMapper;
import com.teamflow.auth.service.AuthService;
import com.teamflow.auth.service.ClientIpResolver;
import com.teamflow.auth.service.LoginThrottleService;
import com.teamflow.common.security.SecurityConfig;
import com.teamflow.user.controller.UserController;
import com.teamflow.user.entity.UserEntity;
import com.teamflow.user.repository.UserMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(
    controllers = {AuthController.class, UserController.class},
    properties = {
      "teamflow.jwt.secret=local-test-secret-with-more-than-32-bytes",
      "teamflow.registration-enabled=false"
    })
@Import({
  SecurityConfig.class,
  AuthService.class,
  LoginThrottleService.class,
  ClientIpResolver.class
})
class AuthWebTest {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper objectMapper;
  @Autowired PasswordEncoder passwords;
  @MockitoBean UserMapper users;
  @MockitoBean AuthSessionMapper sessions;
  @MockitoBean LoginAttemptMapper attempts;

  @Test
  void protectedEndpointRejectsAnonymousRequestWithTraceId() throws Exception {
    mvc.perform(get("/api/v1/users/me"))
        .andExpect(status().isUnauthorized())
        .andExpect(header().string("X-Trace-Id", not(blankOrNullString())))
        .andExpect(jsonPath("$.code").value("AUTH.UNAUTHORIZED"))
        .andExpect(jsonPath("$.traceId", not(blankOrNullString())));
  }

  @Test
  void loginTokenCanReadCurrentUser() throws Exception {
    UserEntity user = new UserEntity();
    user.setId(7L);
    user.setUsername("alice");
    user.setDisplayName("Alice");
    user.setPasswordHash(passwords.encode("correct-password"));
    user.setStatus("ACTIVE");
    when(users.findByUsername("alice")).thenReturn(user);
    when(users.findById(7L)).thenReturn(user);
    when(sessions.isActive(
            org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq(7L)))
        .thenReturn(1);

    String body =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"alice\",\"password\":\"correct-password\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.code").value("OK"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    JsonNode response = objectMapper.readTree(body);
    String token = response.path("data").path("accessToken").asText();

    mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data.username").value("alice"))
        .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
  }

  @Test
  void disabledRegistrationReturnsForbidden() throws Exception {
    mvc.perform(
            post("/api/v1/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"username\":\"alice\",\"password\":\"correct-password\",\"displayName\":\"Alice\"}"))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("AUTH.FORBIDDEN"));
  }

  @Test
  void invalidLoginPayloadUsesValidationErrorWithoutEchoingPassword() throws Exception {
    String body =
        mvc.perform(
                post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"username\":\"\",\"password\":\"secret-password\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("COMMON.VALIDATION_ERROR"))
            .andExpect(jsonPath("$.message").value("请求参数无效"))
            .andReturn()
            .getResponse()
            .getContentAsString();
    assertFalse(body.contains("secret-password"));
  }
}
