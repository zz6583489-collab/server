package com.teamflow.auth.service;

import com.teamflow.auth.repository.LoginAttemptMapper;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import org.springframework.stereotype.Service;

/** Bounded database-backed account and source-IP login throttling. */
@Service
public class LoginThrottleService {
  private final LoginAttemptMapper attempts;

  public LoginThrottleService(LoginAttemptMapper attempts) {
    this.attempts = attempts;
  }

  public void check(String username, String remoteAddress) {
    if (attempts.isBlocked(key("account", username.toLowerCase(Locale.ROOT)), 5) != 0
        || attempts.isBlocked(key("ip", remoteAddress), 30) != 0) {
      throw new BusinessException(ErrorCode.AUTH_TOO_MANY_ATTEMPTS);
    }
  }

  public void failed(String username, String remoteAddress) {
    attempts.recordFailure(key("account", username.toLowerCase(Locale.ROOT)));
    attempts.recordFailure(key("ip", remoteAddress));
  }

  public void succeeded(String username) {
    attempts.clear(key("account", username.toLowerCase(Locale.ROOT)));
  }

  private String key(String type, String value) {
    try {
      byte[] digest =
          MessageDigest.getInstance("SHA-256")
              .digest((type + ":" + value).getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(digest);
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 unavailable", exception);
    }
  }
}
