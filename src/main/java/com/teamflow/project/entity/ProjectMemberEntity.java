package com.teamflow.project.entity;

import java.time.LocalDateTime;

public class ProjectMemberEntity {
  private Long userId;
  private String username;
  private String displayName;
  private String memberRole;
  private LocalDateTime joinedAt;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public String getMemberRole() {
    return memberRole;
  }

  public void setMemberRole(String memberRole) {
    this.memberRole = memberRole;
  }

  public LocalDateTime getJoinedAt() {
    return joinedAt;
  }

  public void setJoinedAt(LocalDateTime joinedAt) {
    this.joinedAt = joinedAt;
  }
}
