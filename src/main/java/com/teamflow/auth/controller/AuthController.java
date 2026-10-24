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

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/login")
  public ApiResponse<LoginView> login(@Valid @RequestBody LoginRequest request) {
    return ApiResponse.success(authService.login(request));
  }

  @PostMapping("/register")
  public ApiResponse<UserView> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.success(authService.register(request));
  }
}
