package com.teamflow.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamflow.auth.dto.LoginRequest;
import com.teamflow.auth.dto.LoginView;
import com.teamflow.auth.dto.RegisterRequest;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.user.entity.UserEntity;
import com.teamflow.user.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class AuthServiceTest {
  private UserMapper users;
  private PasswordEncoder passwords;
  private JwtEncoder encoder;
  private AuthService service;

  @BeforeEach
  void setUp() {
    users = mock(UserMapper.class);
    passwords = mock(PasswordEncoder.class);
    encoder = mock(JwtEncoder.class);
    service = new AuthService(users, passwords, encoder, "teamflow", 15, false);
  }

  @Test
  void loginIssuesShortLivedBearerForActiveUser() {
    UserEntity user = user("ACTIVE");
    when(users.findByUsername("alice")).thenReturn(user);
    when(passwords.matches("correct-password", "hash")).thenReturn(true);
    when(encoder.encode(any()))
        .thenReturn(
            Jwt.withTokenValue("signed-token").header("alg", "HS256").claim("sub", "42").build());

    LoginView result = service.login(new LoginRequest("alice", "correct-password"));

    assertEquals("signed-token", result.accessToken());
    assertEquals("Bearer", result.tokenType());
    assertEquals(900, result.expiresInSeconds());
  }

  @Test
  void loginRejectsDisabledUserWithoutIssuingToken() {
    when(users.findByUsername("alice")).thenReturn(user("DISABLED"));

    BusinessException result =
        assertThrows(
            BusinessException.class,
            () -> service.login(new LoginRequest("alice", "correct-password")));

    assertEquals(ErrorCode.INVALID_CREDENTIALS, result.errorCode());
    verify(encoder, never()).encode(any());
  }

  @Test
  void registrationIsClosedByDefault() {
    BusinessException result =
        assertThrows(
            BusinessException.class,
            () -> service.register(new RegisterRequest("alice", "a-good-password", "Alice", null)));

    assertEquals(ErrorCode.FORBIDDEN, result.errorCode());
    verify(users, never()).insert(any());
  }

  private UserEntity user(String status) {
    UserEntity user = new UserEntity();
    user.setId(42L);
    user.setUsername("alice");
    user.setPasswordHash("hash");
    user.setStatus(status);
    return user;
  }
}
