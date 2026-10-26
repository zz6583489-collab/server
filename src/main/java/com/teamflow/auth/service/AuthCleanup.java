package com.teamflow.auth.service;

import com.teamflow.auth.repository.AuthSessionMapper;
import com.teamflow.auth.repository.LoginAttemptMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Purges only expired or long-revoked authentication state. */
@Component
public class AuthCleanup {
  private final AuthSessionMapper sessions;
  private final LoginAttemptMapper attempts;

  public AuthCleanup(AuthSessionMapper sessions, LoginAttemptMapper attempts) {
    this.sessions = sessions;
    this.attempts = attempts;
  }

  @Scheduled(initialDelay = 3_600_000, fixedDelay = 3_600_000)
  public void purgeExpired() {
    attempts.deleteExpired();
    sessions.deleteExpired();
  }
}
