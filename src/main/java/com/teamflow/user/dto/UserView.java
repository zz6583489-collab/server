package com.teamflow.user.dto;

import com.teamflow.user.entity.UserEntity;

public record UserView(
    String id, String username, String displayName, String email, boolean systemAdmin) {
  public static UserView from(UserEntity user) {
    return from(user, false);
  }

  public static UserView from(UserEntity user, boolean systemAdmin) {
    return new UserView(
        user.getId().toString(),
        user.getUsername(),
        user.getDisplayName(),
        user.getEmail(),
        systemAdmin);
  }
}
