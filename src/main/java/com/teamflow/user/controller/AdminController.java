package com.teamflow.user.controller;

import com.teamflow.auth.dto.RegisterRequest;
import com.teamflow.common.response.ApiResponse;
import com.teamflow.common.response.PageResult;
import com.teamflow.common.security.CurrentUser;
import com.teamflow.project.service.ProjectService;
import com.teamflow.user.dto.UserView;
import com.teamflow.user.service.AdminService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/users")
public class AdminController {
  private final AdminService admins;
  private final CurrentUser currentUser;

  public AdminController(AdminService admins, CurrentUser currentUser) {
    this.admins = admins;
    this.currentUser = currentUser;
  }

  @GetMapping
  public ApiResponse<PageResult<AdminService.AdminUser>> page(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.success(admins.page(currentUser.id(), page, pageSize));
  }

  @PostMapping
  public ApiResponse<UserView> create(@Valid @RequestBody RegisterRequest request) {
    return ApiResponse.success(admins.createUser(currentUser.id(), request));
  }

  @PatchMapping("/{id}/status")
  public ApiResponse<UserView> status(
      @PathVariable String id, @Valid @RequestBody StatusRequest request) {
    return ApiResponse.success(
        admins.changeStatus(currentUser.id(), ProjectService.parseId(id), request.status()));
  }

  public record StatusRequest(@NotBlank @Pattern(regexp = "ACTIVE|DISABLED") String status) {}
}
