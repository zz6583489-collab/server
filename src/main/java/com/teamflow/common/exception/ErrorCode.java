package com.teamflow.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
  INVALID_CREDENTIALS("AUTH.INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "用户名或密码错误"),
  REGISTRATION_CONFLICT("AUTH.REGISTRATION_CONFLICT", HttpStatus.CONFLICT, "用户名或邮箱已存在"),
  UNAUTHORIZED("AUTH.UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "请先登录"),
  FORBIDDEN("AUTH.FORBIDDEN", HttpStatus.FORBIDDEN, "无权访问"),
  PROJECT_NOT_FOUND("PROJECT.NOT_FOUND", HttpStatus.NOT_FOUND, "项目不存在或不可见"),
  PROJECT_ARCHIVED("PROJECT.ARCHIVED", HttpStatus.CONFLICT, "归档项目不可变更"),
  PROJECT_OWNER_PROTECTED("PROJECT.OWNER_PROTECTED", HttpStatus.CONFLICT, "不能移除项目负责人"),
  PROJECT_MEMBER_HAS_TASKS("PROJECT.MEMBER_HAS_TASKS", HttpStatus.CONFLICT, "成员仍有指派任务"),
  PROJECT_MEMBER_CONFLICT("PROJECT.MEMBER_CONFLICT", HttpStatus.CONFLICT, "成员已在项目中"),
  TASK_NOT_FOUND("TASK.NOT_FOUND", HttpStatus.NOT_FOUND, "任务不存在或不可见"),
  TASK_ASSIGNEE_NOT_MEMBER("TASK.ASSIGNEE_NOT_MEMBER", HttpStatus.BAD_REQUEST, "负责人不是有效项目成员"),
  TASK_INVALID_TRANSITION("TASK.INVALID_TRANSITION", HttpStatus.CONFLICT, "非法状态流转"),
  TASK_VERSION_CONFLICT("TASK.VERSION_CONFLICT", HttpStatus.CONFLICT, "任务版本冲突"),
  NOTIFICATION_NOT_FOUND("NOTIFICATION.NOT_FOUND", HttpStatus.NOT_FOUND, "通知不存在或不可见"),
  AUTH_TOO_MANY_ATTEMPTS("AUTH.TOO_MANY_ATTEMPTS", HttpStatus.TOO_MANY_REQUESTS, "登录尝试过于频繁"),
  AUTH_SESSION_REVOKED("AUTH.SESSION_REVOKED", HttpStatus.UNAUTHORIZED, "会话已失效"),
  USER_ACTIVE_PROJECT_OWNER("USER.ACTIVE_PROJECT_OWNER", HttpStatus.CONFLICT, "用户仍负责活跃项目"),
  VALIDATION_ERROR("COMMON.VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "请求参数无效"),
  NOT_FOUND("COMMON.NOT_FOUND", HttpStatus.NOT_FOUND, "资源不存在"),
  METHOD_NOT_ALLOWED("COMMON.METHOD_NOT_ALLOWED", HttpStatus.METHOD_NOT_ALLOWED, "请求方法不支持"),
  UNSUPPORTED_MEDIA_TYPE(
      "COMMON.UNSUPPORTED_MEDIA_TYPE", HttpStatus.UNSUPPORTED_MEDIA_TYPE, "请求媒体类型不支持"),
  INTERNAL_ERROR("SYSTEM.INTERNAL_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");

  private final String code;
  private final HttpStatus status;
  private final String message;

  ErrorCode(String code, HttpStatus status, String message) {
    this.code = code;
    this.status = status;
    this.message = message;
  }

  public String code() {
    return code;
  }

  public HttpStatus status() {
    return status;
  }

  public String message() {
    return message;
  }
}
