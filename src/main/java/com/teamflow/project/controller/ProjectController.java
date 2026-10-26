package com.teamflow.project.controller;

import com.teamflow.common.response.ApiResponse;
import com.teamflow.common.response.PageResult;
import com.teamflow.common.security.CurrentUser;
import com.teamflow.project.dto.ProjectDtos;
import com.teamflow.project.service.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
  private final ProjectService projects;
  private final CurrentUser currentUser;

  public ProjectController(ProjectService projects, CurrentUser currentUser) {
    this.projects = projects;
    this.currentUser = currentUser;
  }

  @GetMapping
  public ApiResponse<PageResult<ProjectDtos.View>> page(
      @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.success(projects.page(currentUser.id(), page, pageSize));
  }

  @PostMapping
  public ApiResponse<ProjectDtos.View> create(@Valid @RequestBody ProjectDtos.Create input) {
    return ApiResponse.success(projects.create(currentUser.id(), input));
  }

  @GetMapping("/{id}")
  public ApiResponse<ProjectDtos.View> detail(@PathVariable String id) {
    return ApiResponse.success(projects.detail(ProjectService.parseId(id), currentUser.id()));
  }

  @PatchMapping("/{id}")
  public ApiResponse<ProjectDtos.View> edit(
      @PathVariable String id, @Valid @RequestBody ProjectDtos.Edit input) {
    return ApiResponse.success(projects.edit(ProjectService.parseId(id), currentUser.id(), input));
  }

  @PostMapping("/{id}/archive")
  public ApiResponse<Void> archive(@PathVariable String id) {
    projects.archive(ProjectService.parseId(id), currentUser.id());
    return ApiResponse.success(null);
  }

  @GetMapping("/{id}/members")
  public ApiResponse<List<ProjectDtos.Member>> members(@PathVariable String id) {
    return ApiResponse.success(projects.members(ProjectService.parseId(id), currentUser.id()));
  }

  @PostMapping("/{id}/members")
  public ApiResponse<ProjectDtos.Member> addMember(
      @PathVariable String id, @Valid @RequestBody ProjectDtos.AddMember input) {
    return ApiResponse.success(
        projects.addMember(ProjectService.parseId(id), currentUser.id(), input.userId()));
  }

  @DeleteMapping("/{id}/members/{userId}")
  public ApiResponse<Void> removeMember(@PathVariable String id, @PathVariable String userId) {
    projects.removeMember(ProjectService.parseId(id), currentUser.id(), userId);
    return ApiResponse.success(null);
  }
}
