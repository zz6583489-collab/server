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

/** 用户 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {
  private final UserMapper userMapper;

  public UserController(UserMapper userMapper) {
    this.userMapper = userMapper;
  }

  /**
   * 个人信息
   *
   * @param authentication 认证信息
   * @return
   */
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
    return ApiResponse.success(UserView.from(user, userMapper.isAdmin(id) != 0));
  }
}
