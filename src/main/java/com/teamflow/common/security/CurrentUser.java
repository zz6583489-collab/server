package com.teamflow.common.security;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.user.entity.UserEntity;
import com.teamflow.user.repository.UserMapper;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

/** Resolves identity from a verified JWT and rechecks the account on each business request. */
@Component
public class CurrentUser {
  private final UserMapper users;

  public CurrentUser(UserMapper users) {
    this.users = users;
  }

  public UserEntity requireActive() {
    if (!(SecurityContextHolder.getContext().getAuthentication()
        instanceof JwtAuthenticationToken token)) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    long id;
    try {
      id = Long.parseLong(token.getName());
    } catch (NumberFormatException exception) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    UserEntity user = users.findById(id);
    if (user == null || !"ACTIVE".equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    return user;
  }

  public long id() {
    return requireActive().getId();
  }
}
