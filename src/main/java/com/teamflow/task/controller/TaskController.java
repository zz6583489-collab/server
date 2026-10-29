package com.teamflow.task.controller;

import com.teamflow.common.response.ApiResponse;
import com.teamflow.common.response.PageResult;
import com.teamflow.common.security.CurrentUser;
import com.teamflow.project.service.ProjectService;
import com.teamflow.task.dto.TaskDtos;
import com.teamflow.task.service.TaskService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 任务类操作
 */
@RestController
@RequestMapping("/api/v1")
public class TaskController {
  private final TaskService tasks;
  private final CurrentUser currentUser;

  public TaskController(TaskService tasks, CurrentUser currentUser) {
    this.tasks = tasks;
    this.currentUser = currentUser;
  }

  @GetMapping("/projects/{projectId}/tasks")
  public ApiResponse<PageResult<TaskDtos.View>> page(
      @PathVariable String projectId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String priority,
      @RequestParam(required = false) String assigneeId) {
    return ApiResponse.success(
        tasks.page(
            ProjectService.parseId(projectId),
            currentUser.id(),
            page,
            pageSize,
            new TaskDtos.Filter(status, priority, assigneeId)));
  }

  @GetMapping("/projects/{projectId}/stats")
  public ApiResponse<TaskDtos.Stats> stats(@PathVariable String projectId) {
    return ApiResponse.success(tasks.stats(ProjectService.parseId(projectId), currentUser.id()));
  }

  @PostMapping("/projects/{projectId}/tasks")
  public ApiResponse<TaskDtos.View> create(
      @PathVariable String projectId, @Valid @RequestBody TaskDtos.Create input) {
    return ApiResponse.success(
        tasks.create(ProjectService.parseId(projectId), currentUser.id(), input));
  }

  @GetMapping("/tasks/{id}")
  public ApiResponse<TaskDtos.View> detail(@PathVariable String id) {
    return ApiResponse.success(tasks.detail(ProjectService.parseId(id), currentUser.id()));
  }

  @PatchMapping("/tasks/{id}")
  public ApiResponse<TaskDtos.View> edit(
      @PathVariable String id, @Valid @RequestBody TaskDtos.Edit input) {
    return ApiResponse.success(tasks.edit(ProjectService.parseId(id), currentUser.id(), input));
  }

  @PostMapping("/tasks/{id}/transitions")
  public ApiResponse<TaskDtos.View> transition(
      @PathVariable String id, @Valid @RequestBody TaskDtos.Transition input) {
    return ApiResponse.success(
        tasks.transition(ProjectService.parseId(id), currentUser.id(), input));
  }

  @GetMapping("/tasks/{id}/activities")
  public ApiResponse<PageResult<TaskDtos.Activity>> activities(
      @PathVariable String id,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    return ApiResponse.success(
        tasks.activities(ProjectService.parseId(id), currentUser.id(), page, pageSize));
  }
}
