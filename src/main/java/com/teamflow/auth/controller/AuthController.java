package com.teamflow.auth.controller;

import com.teamflow.auth.dto.LoginRequest;
import com.teamflow.auth.dto.LoginView;
import com.teamflow.auth.dto.RegisterRequest;
import com.teamflow.auth.service.AuthService;
import com.teamflow.common.response.ApiResponse;
import com.teamflow.user.dto.UserView;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 认证
 */
@RestController
@RequestMapping("/v1/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  /**
   * 登录请求
   * @param request 登录参数
   * @return 响应
   */
  @PostMapping("/login")
  public ApiResponse<LoginView> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.success(authService.login(request));
  }

  /**
   * 注册请求
   * @param request 请求参数
   * @return 响应
   */
  @PostMapping("/register")
  public ApiResponse<UserView> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.success(authService.register(request));
  }
}
