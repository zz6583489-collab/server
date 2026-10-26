package com.teamflow.auth.controller;

import com.teamflow.auth.dto.LoginRequest;
import com.teamflow.auth.dto.LoginView;
import com.teamflow.auth.dto.RegisterRequest;
import com.teamflow.auth.service.AuthService;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.common.response.ApiResponse;
import com.teamflow.user.dto.UserView;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 认证 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
  private final AuthService authService;
  private final boolean secureCookie;
  private final int refreshDays;

  public AuthController(
      AuthService authService,
      @Value("${teamflow.jwt.cookie-secure:true}") boolean secureCookie,
      @Value("${teamflow.jwt.refresh-days:7}") int refreshDays) {
    this.authService = authService;
    this.secureCookie = secureCookie;
    this.refreshDays = refreshDays;
  }

  /**
   * 登录请求
   *
   * @param request 登录参数
   * @return 响应
   */
  @PostMapping("/login")
  public ResponseEntity<ApiResponse<LoginView>> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest servletRequest) {
    AuthService.Tokens tokens = authService.login(request, servletRequest.getRemoteAddr());
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            cookie(tokens.refreshCredential(), Duration.ofDays(refreshDays)).toString())
        .body(ApiResponse.success(tokens.view()));
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<LoginView>> refresh(
      @CookieValue(name = "TF_REFRESH", required = false) String credential,
      @RequestHeader(name = "X-TeamFlow-CSRF", required = false) String csrfHeader) {
    requireCsrfHeader(csrfHeader);
    AuthService.Tokens tokens = authService.refresh(credential);
    return ResponseEntity.ok()
        .header(
            HttpHeaders.SET_COOKIE,
            cookie(tokens.refreshCredential(), Duration.ofDays(refreshDays)).toString())
        .body(ApiResponse.success(tokens.view()));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @CookieValue(name = "TF_REFRESH", required = false) String credential,
      @RequestHeader(name = "X-TeamFlow-CSRF", required = false) String csrfHeader) {
    requireCsrfHeader(csrfHeader);
    authService.logout(credential);
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString())
        .body(ApiResponse.success(null));
  }

  private void requireCsrfHeader(String value) {
    if (!"1".equals(value)) throw new BusinessException(ErrorCode.FORBIDDEN);
  }

  private ResponseCookie cookie(String value, Duration age) {
    return ResponseCookie.from("TF_REFRESH", value)
        .httpOnly(true)
        .secure(secureCookie)
        .sameSite("Strict")
        .path("/api/v1/auth")
        .maxAge(age)
        .build();
  }

  /**
   * 注册请求
   *
   * @param request 请求参数
   * @return 响应
   */
  @PostMapping("/register")
  public ApiResponse<UserView> register(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.success(authService.register(request));
  }
}
