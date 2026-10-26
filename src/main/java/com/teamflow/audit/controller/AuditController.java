package com.teamflow.audit.controller;

import com.teamflow.audit.entity.AuditEntity;
import com.teamflow.audit.service.AuditService;
import com.teamflow.common.response.ApiResponse;
import com.teamflow.common.response.PageResult;
import com.teamflow.common.security.CurrentUser;
import com.teamflow.project.service.ProjectService;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/audit-logs")
public class AuditController {
  private final ProjectService projects;
  private final AuditService audit;
  private final CurrentUser currentUser;

  public AuditController(ProjectService projects, AuditService audit, CurrentUser currentUser) {
    this.projects = projects;
    this.audit = audit;
    this.currentUser = currentUser;
  }

  @GetMapping
  public ApiResponse<PageResult<View>> page(
      @PathVariable String projectId,
      @RequestParam(defaultValue = "1") int page,
      @RequestParam(defaultValue = "20") int pageSize) {
    long id = ProjectService.parseId(projectId);
    projects.requireMember(id, currentUser.id());
    projects.requireOwner(id, currentUser.id());
    long offset = PageResult.offset(page, pageSize);
    List<View> items = audit.page(id, pageSize, offset).stream().map(View::from).toList();
    return ApiResponse.success(new PageResult<>(items, audit.count(id), page, pageSize));
  }

  public record View(
      String id,
      String actorId,
      String resourceType,
      String resourceId,
      String action,
      String traceId,
      Instant createdAt) {
    static View from(AuditEntity entity) {
      return new View(
          entity.getId().toString(),
          entity.getActorId().toString(),
          entity.getResourceType(),
          entity.getResourceId() == null ? null : entity.getResourceId().toString(),
          entity.getAction(),
          entity.getTraceId(),
          entity.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
  }
}
