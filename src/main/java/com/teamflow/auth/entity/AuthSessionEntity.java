package com.teamflow.auth.entity;

import java.time.LocalDateTime;

public class AuthSessionEntity {
  private String id;
  private Long userId;
  private String refreshHash;
  private LocalDateTime expiresAt;
  private LocalDateTime revokedAt;

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getRefreshHash() {
    return refreshHash;
  }

  public void setRefreshHash(String refreshHash) {
    this.refreshHash = refreshHash;
  }

  public LocalDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(LocalDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public LocalDateTime getRevokedAt() {
    return revokedAt;
  }

  public void setRevokedAt(LocalDateTime revokedAt) {
    this.revokedAt = revokedAt;
  }
}
