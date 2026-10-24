package com.teamflow.user.dto;

import com.teamflow.user.entity.UserEntity;

public record UserView(Long id, String username, String displayName, String email) {
  public static UserView from(UserEntity user) {
    return new UserView(user.getId(), user.getUsername(), user.getDisplayName(), user.getEmail());
  }
}
