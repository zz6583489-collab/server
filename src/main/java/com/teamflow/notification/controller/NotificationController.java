package com.teamflow.notification.controller;

import com.teamflow.common.response.ApiResponse;
import com.teamflow.common.response.PageResult;
import com.teamflow.common.security.CurrentUser;
import com.teamflow.notification.dto.NotificationDtos;
import com.teamflow.notification.service.NotificationService;
import com.teamflow.project.service.ProjectService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/notifications")
public class NotificationController {
  private final NotificationService notifications;
  private final CurrentUser currentUser;

  public NotificationController(NotificationService notifications, CurrentUser currentUser) {
    this.notifications = notifications;
    this.currentUser = currentUser;
  }

  @GetMapping
  public ApiResponse<PageResult<NotificationDtos.View>> page(
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(defaultValue = "false") boolean unreadOnly) {
    return ApiResponse.success(notifications.page(currentUser.id(), page, pageSize, unreadOnly));
  }

  @GetMapping("/unread-count")
  public ApiResponse<NotificationDtos.UnreadCount> unreadCount() {
    return ApiResponse.success(notifications.unreadCount(currentUser.id()));
  }

  @PostMapping("/{id}/read")
  public ApiResponse<Void> markRead(@PathVariable String id) {
    notifications.markRead(ProjectService.parseId(id), currentUser.id());
    return ApiResponse.success(null);
  }

  @PostMapping("/read-all")
  public ApiResponse<Void> markAllRead() {
    notifications.markAllRead(currentUser.id());
    return ApiResponse.success(null);
  }
}
