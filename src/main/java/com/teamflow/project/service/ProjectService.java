package com.teamflow.project.service;

import com.teamflow.audit.service.AuditService;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.common.response.PageResult;
import com.teamflow.notification.service.NotificationService;
import com.teamflow.project.dto.ProjectDtos;
import com.teamflow.project.entity.ProjectEntity;
import com.teamflow.project.repository.ProjectMapper;
import com.teamflow.user.entity.UserEntity;
import com.teamflow.user.repository.UserMapper;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectService {
  private final ProjectMapper projects;
  private final UserMapper users;
  private final AuditService audit;
  private final NotificationService notifications;

  public ProjectService(
      ProjectMapper projects,
      UserMapper users,
      AuditService audit,
      NotificationService notifications) {
    this.projects = projects;
    this.users = users;
    this.audit = audit;
    this.notifications = notifications;
  }

  @Transactional
  public ProjectDtos.View create(long actorId, ProjectDtos.Create input) {
    ProjectEntity project = new ProjectEntity();
    project.setName(input.name().trim());
    project.setDescription(input.description());
    project.setOwnerId(actorId);
    projects.insert(project);
    projects.addMember(project.getId(), actorId, "OWNER");
    audit.record(actorId, project.getId(), "PROJECT", project.getId(), "PROJECT_CREATED");
    return ProjectDtos.View.from(projects.findById(project.getId()));
  }

  @Transactional(readOnly = true)
  public PageResult<ProjectDtos.View> page(long actorId, int page, int pageSize, String status) {
    long offset = PageResult.offset(page, pageSize);
    if (status != null && !"ACTIVE".equals(status) && !"ARCHIVED".equals(status)) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
    List<ProjectDtos.View> items =
        projects.pageFiltered(actorId, pageSize, offset, status).stream()
            .map(ProjectDtos.View::from)
            .toList();
    return new PageResult<>(items, projects.countFiltered(actorId, status), page, pageSize);
  }

  @Transactional(readOnly = true)
  public ProjectDtos.View detail(long projectId, long actorId) {
    return ProjectDtos.View.from(requireMember(projectId, actorId));
  }

  @Transactional
  public ProjectDtos.View edit(long projectId, long actorId, ProjectDtos.Edit input) {
    ProjectEntity project = requireWritable(projectId, actorId);
    requireOwner(projectId, actorId);
    if (input.name() == null && input.description() == null) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
    if (input.name() != null) {
      if (input.name().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
      project.setName(input.name().trim());
    }
    if (input.description() != null) project.setDescription(input.description());
    projects.update(project);
    audit.record(actorId, projectId, "PROJECT", projectId, "PROJECT_UPDATED");
    return ProjectDtos.View.from(projects.findById(projectId));
  }

  @Transactional
  public void archive(long projectId, long actorId) {
    requireWritable(projectId, actorId);
    requireOwner(projectId, actorId);
    projects.archive(projectId);
    audit.record(actorId, projectId, "PROJECT", projectId, "PROJECT_ARCHIVED");
  }

  @Transactional(readOnly = true)
  public List<ProjectDtos.Member> members(long projectId, long actorId) {
    requireMember(projectId, actorId);
    return projects.members(projectId).stream().map(ProjectDtos.Member::from).toList();
  }

  @Transactional
  public ProjectDtos.Member addMember(long projectId, long actorId, String userIdText) {
    requireWritable(projectId, actorId);
    requireOwner(projectId, actorId);
    long userId = parseId(userIdText);
    UserEntity user = users.findById(userId);
    if (user == null || !"ACTIVE".equals(user.getStatus())) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
    try {
      projects.addMember(projectId, userId, "MEMBER");
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.PROJECT_MEMBER_CONFLICT);
    }
    audit.record(actorId, projectId, "PROJECT_MEMBER", userId, "MEMBER_ADDED");
    notifications.memberAdded(userId, actorId, projectId);
    return ProjectDtos.Member.from(projects.findMember(projectId, userId));
  }

  @Transactional
  public void removeMember(long projectId, long actorId, String userIdText) {
    requireWritable(projectId, actorId);
    requireOwner(projectId, actorId);
    long userId = parseId(userIdText);
    if ("OWNER".equals(projects.memberRole(projectId, userId))) {
      throw new BusinessException(ErrorCode.PROJECT_OWNER_PROTECTED);
    }
    if (projects.assignedTasks(projectId, userId) != 0) {
      throw new BusinessException(ErrorCode.PROJECT_MEMBER_HAS_TASKS);
    }
    try {
      if (projects.removeMember(projectId, userId) == 0) {
        throw new BusinessException(ErrorCode.NOT_FOUND);
      }
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.PROJECT_MEMBER_HAS_TASKS);
    }
    audit.record(actorId, projectId, "PROJECT_MEMBER", userId, "MEMBER_REMOVED");
  }

  public ProjectEntity requireMember(long projectId, long actorId) {
    ProjectEntity project = projects.findById(projectId);
    if (project == null || projects.memberRole(projectId, actorId) == null) {
      throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
    }
    return project;
  }

  public ProjectEntity requireWritable(long projectId, long actorId) {
    ProjectEntity project = projects.findForUpdate(projectId);
    if (project == null || projects.memberRole(projectId, actorId) == null) {
      throw new BusinessException(ErrorCode.PROJECT_NOT_FOUND);
    }
    if (!"ACTIVE".equals(project.getStatus())) {
      throw new BusinessException(ErrorCode.PROJECT_ARCHIVED);
    }
    return project;
  }

  public boolean isOwner(long projectId, long actorId) {
    return "OWNER".equals(projects.memberRole(projectId, actorId));
  }

  public void requireOwner(long projectId, long actorId) {
    if (!isOwner(projectId, actorId)) throw new BusinessException(ErrorCode.FORBIDDEN);
  }

  public boolean isActiveMember(long projectId, long userId) {
    UserEntity user = users.findById(userId);
    return user != null
        && "ACTIVE".equals(user.getStatus())
        && projects.memberRole(projectId, userId) != null;
  }

  public long activeOwnedCount(long userId) {
    return projects.activeOwned(userId);
  }

  public static long parseId(String value) {
    try {
      long id = Long.parseLong(value);
      if (id > 0) return id;
    } catch (NumberFormatException exception) {
      // Map malformed and out-of-range decimal IDs to the public validation error.
    }
    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
  }
}
