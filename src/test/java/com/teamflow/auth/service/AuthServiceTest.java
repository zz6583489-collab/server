package com.teamflow.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamflow.auth.dto.LoginRequest;
import com.teamflow.auth.dto.LoginView;
import com.teamflow.auth.dto.RegisterRequest;
import com.teamflow.auth.entity.AuthSessionEntity;
import com.teamflow.auth.repository.AuthSessionMapper;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.user.entity.UserEntity;
import com.teamflow.user.repository.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtEncoder;

class AuthServiceTest {
  private UserMapper users;
  private PasswordEncoder passwords;
  private JwtEncoder encoder;
  private AuthService service;
  private AuthSessionMapper sessions;
  private LoginThrottleService throttle;

  @BeforeEach
  void setUp() {
    users = mock(UserMapper.class);
    passwords = mock(PasswordEncoder.class);
    encoder = mock(JwtEncoder.class);
    sessions = mock(AuthSessionMapper.class);
    throttle = mock(LoginThrottleService.class);
    service =
        new AuthService(users, passwords, encoder, sessions, throttle, "teamflow", 15, false, 7);
  }

  @Test
  void loginIssuesShortLivedBearerForActiveUser() {
    UserEntity user = user("ACTIVE");
    when(users.findByUsername("alice")).thenReturn(user);
    when(passwords.matches("correct-password", "hash")).thenReturn(true);
    when(encoder.encode(any()))
        .thenReturn(
            Jwt.withTokenValue("signed-token").header("alg", "HS256").claim("sub", "42").build());

    LoginView result =
        service.login(new LoginRequest("alice", "correct-password"), "127.0.0.1").view();

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
            () -> service.login(new LoginRequest("alice", "correct-password"), "127.0.0.1"));

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

  @Test
  void refreshRotatesCredentialAndRejectsOldCredential() {
    UserEntity user = user("ACTIVE");
    when(users.findByUsername("alice")).thenReturn(user);
    when(users.findById(42L)).thenReturn(user);
    when(passwords.matches("correct-password", "hash")).thenReturn(true);
    when(encoder.encode(any()))
        .thenReturn(
            Jwt.withTokenValue("signed-token").header("alg", "HS256").claim("sub", "42").build());
    AuthService.Tokens original =
        service.login(new LoginRequest("alice", "correct-password"), "127.0.0.1");
    ArgumentCaptor<AuthSessionEntity> captured = ArgumentCaptor.forClass(AuthSessionEntity.class);
    verify(sessions).insert(captured.capture());
    AuthSessionEntity stored = captured.getValue();
    when(sessions.findById(stored.getId())).thenReturn(stored);
    when(sessions.rotate(anyString(), anyString(), anyString()))
        .thenAnswer(
            invocation -> {
              stored.setRefreshHash(invocation.getArgument(2));
              return 1;
            });

    AuthService.Tokens rotated = service.refresh(original.refreshCredential());

    assertNotEquals(original.refreshCredential(), rotated.refreshCredential());
    BusinessException replay =
        assertThrows(BusinessException.class, () -> service.refresh(original.refreshCredential()));
    assertEquals(ErrorCode.AUTH_SESSION_REVOKED, replay.errorCode());
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
