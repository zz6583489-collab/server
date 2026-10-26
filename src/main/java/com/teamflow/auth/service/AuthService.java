package com.teamflow.auth.service;

import com.teamflow.auth.dto.LoginRequest;
import com.teamflow.auth.dto.LoginView;
import com.teamflow.auth.dto.RegisterRequest;
import com.teamflow.auth.entity.AuthSessionEntity;
import com.teamflow.auth.repository.AuthSessionMapper;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.user.dto.UserView;
import com.teamflow.user.entity.UserEntity;
import com.teamflow.user.repository.UserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {
  private final UserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final JwtEncoder jwtEncoder;
  private final String issuer;
  private final long accessTokenMinutes;
  private final boolean registrationEnabled;
  private final AuthSessionMapper sessions;
  private final LoginThrottleService throttle;
  private final int refreshDays;
  private static final SecureRandom RANDOM = new SecureRandom();

  public AuthService(
      UserMapper userMapper,
      PasswordEncoder passwordEncoder,
      JwtEncoder jwtEncoder,
      AuthSessionMapper sessions,
      LoginThrottleService throttle,
      @Value("${teamflow.jwt.issuer}") String issuer,
      @Value("${teamflow.jwt.access-token-minutes}") long accessTokenMinutes,
      @Value("${teamflow.registration-enabled}") boolean registrationEnabled,
      @Value("${teamflow.jwt.refresh-days:7}") int refreshDays) {
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.jwtEncoder = jwtEncoder;
    this.issuer = issuer;
    this.accessTokenMinutes = accessTokenMinutes;
    this.registrationEnabled = registrationEnabled;
    this.sessions = sessions;
    this.throttle = throttle;
    this.refreshDays = refreshDays;
    if (accessTokenMinutes < 1 || refreshDays < 1) {
      throw new IllegalArgumentException("Token lifetimes must be positive");
    }
  }

  public record Tokens(LoginView view, String refreshCredential) {}

  public Tokens login(LoginRequest request, String remoteAddress) {
    throttle.check(request.username(), remoteAddress);
    UserEntity user = userMapper.findByUsername(request.username());
    if (user == null
        || !"ACTIVE".equals(user.getStatus())
        || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throttle.failed(request.username(), remoteAddress);
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }
    throttle.succeeded(request.username());
    String sessionId = UUID.randomUUID().toString();
    String refreshSecret = randomSecret();
    Instant now = Instant.now();
    AuthSessionEntity session = new AuthSessionEntity();
    session.setId(sessionId);
    session.setUserId(user.getId());
    session.setRefreshHash(hash(refreshSecret));
    session.setExpiresAt(
        LocalDateTime.ofInstant(now.plus(refreshDays, ChronoUnit.DAYS), ZoneOffset.UTC));
    String token = issueAccessToken(user.getId(), sessionId, now);
    sessions.insert(session);
    return new Tokens(
        new LoginView(token, "Bearer", accessTokenMinutes * 60), sessionId + "." + refreshSecret);
  }

  public Tokens refresh(String credential) {
    String[] parts = splitCredential(credential);
    AuthSessionEntity session = sessions.findById(parts[0]);
    if (session == null
        || session.getRevokedAt() != null
        || session.getExpiresAt().isBefore(LocalDateTime.now(ZoneOffset.UTC))) {
      throw new BusinessException(ErrorCode.AUTH_SESSION_REVOKED);
    }
    if (!MessageDigest.isEqual(
        hash(parts[1]).getBytes(StandardCharsets.US_ASCII),
        session.getRefreshHash().getBytes(StandardCharsets.US_ASCII))) {
      throw new BusinessException(ErrorCode.AUTH_SESSION_REVOKED);
    }
    UserEntity user = userMapper.findById(session.getUserId());
    if (user == null || !"ACTIVE".equals(user.getStatus())) {
      sessions.revoke(session.getId());
      throw new BusinessException(ErrorCode.AUTH_SESSION_REVOKED);
    }
    String nextSecret = randomSecret();
    String token = issueAccessToken(user.getId(), session.getId(), Instant.now());
    if (sessions.rotate(session.getId(), session.getRefreshHash(), hash(nextSecret)) == 0) {
      throw new BusinessException(ErrorCode.AUTH_SESSION_REVOKED);
    }
    return new Tokens(
        new LoginView(token, "Bearer", accessTokenMinutes * 60),
        session.getId() + "." + nextSecret);
  }

  public void logout(String credential) {
    if (credential == null) return;
    try {
      String[] parts = splitCredential(credential);
      AuthSessionEntity session = sessions.findById(parts[0]);
      if (session != null
          && MessageDigest.isEqual(
              hash(parts[1]).getBytes(StandardCharsets.US_ASCII),
              session.getRefreshHash().getBytes(StandardCharsets.US_ASCII))) {
        sessions.revoke(parts[0]);
      }
    } catch (BusinessException exception) {
      // An invalid cookie is already logged out; the controller clears it.
    }
  }

  private String issueAccessToken(long userId, String sessionId, Instant now) {
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(Long.toString(userId))
            .claim("sid", sessionId)
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenMinutes, ChronoUnit.MINUTES))
            .build();
    return jwtEncoder
        .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
        .getTokenValue();
  }

  private String randomSecret() {
    byte[] bytes = new byte[32];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  private String hash(String value) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(value.getBytes(StandardCharsets.US_ASCII)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }

  private String[] splitCredential(String credential) {
    if (credential == null || credential.length() > 128) {
      throw new BusinessException(ErrorCode.AUTH_SESSION_REVOKED);
    }
    String[] parts = credential.split("\\.", -1);
    if (parts.length != 2
        || !parts[0].matches("[0-9a-f-]{36}")
        || !parts[1].matches("[A-Za-z0-9_-]{43}")) {
      throw new BusinessException(ErrorCode.AUTH_SESSION_REVOKED);
    }
    return parts;
  }

  @Transactional
  public UserView register(RegisterRequest request) {
    if (!registrationEnabled) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    UserEntity user = new UserEntity();
    user.setUsername(request.username().trim());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setDisplayName(request.displayName().trim());
    user.setEmail(
        request.email() == null || request.email().isBlank() ? null : request.email().trim());
    try {
      userMapper.insert(user);
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.REGISTRATION_CONFLICT);
    }
    return UserView.from(user);
  }
}
