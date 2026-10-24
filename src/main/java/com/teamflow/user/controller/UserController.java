package com.teamflow.user.controller;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.common.response.ApiResponse;
import com.teamflow.user.dto.UserView;
import com.teamflow.user.entity.UserEntity;
import com.teamflow.user.repository.UserMapper;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {
  private final UserMapper userMapper;

  public UserController(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  @GetMapping("/me")
  public ApiResponse<UserView> me(JwtAuthenticationToken authentication) {
    long id;
    try {
      id = Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    UserEntity user = userMapper.findById(id);
    if (user == null || !"ACTIVE".equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.UNAUTHORIZED);
    }
    return ApiResponse.success(UserView.from(user));
  }
}
