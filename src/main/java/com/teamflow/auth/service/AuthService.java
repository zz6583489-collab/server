package com.teamflow.auth.service;

import com.teamflow.auth.dto.LoginRequest;
import com.teamflow.auth.dto.LoginView;
import com.teamflow.auth.dto.RegisterRequest;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.user.dto.UserView;
import com.teamflow.user.entity.UserEntity;
import com.teamflow.user.repository.UserMapper;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
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

  public AuthService(
      UserMapper userMapper,
      PasswordEncoder passwordEncoder,
      JwtEncoder jwtEncoder,
      @Value("${teamflow.jwt.issuer}") String issuer,
      @Value("${teamflow.jwt.access-token-minutes}") long accessTokenMinutes,
      @Value("${teamflow.registration-enabled}") boolean registrationEnabled) {
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.jwtEncoder = jwtEncoder;
    this.issuer = issuer;
    this.accessTokenMinutes = accessTokenMinutes;
    this.registrationEnabled = registrationEnabled;
  }

  public LoginView login(LoginRequest request) {
    UserEntity user = userMapper.findByUsername(request.username());
    if (user == null
        || !"ACTIVE".equals(user.getStatus())
        || !passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }
    Instant now = Instant.now();
    JwtClaimsSet claims =
        JwtClaimsSet.builder()
            .issuer(issuer)
            .subject(user.getId().toString())
            .issuedAt(now)
            .expiresAt(now.plus(accessTokenMinutes, ChronoUnit.MINUTES))
            .build();
    String token =
        jwtEncoder
            .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
            .getTokenValue();
    return new LoginView(token, "Bearer", accessTokenMinutes * 60);
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
