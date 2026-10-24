package com.teamflow.common.security;

import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SecurityConfigTest {
  @Test
  void rejectsMissingOrWeakSigningSecret() {
    SecurityConfig config = new SecurityConfig();
    assertThrows(IllegalStateException.class, () -> config.jwtSecretKey(""));
    assertThrows(IllegalStateException.class, () -> config.jwtSecretKey("too-short"));
  }
}
