package com.teamflow.user.service;

import com.teamflow.audit.service.AuditService;
import com.teamflow.auth.dto.RegisterRequest;
import com.teamflow.auth.repository.AuthSessionMapper;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.common.response.PageResult;
import com.teamflow.project.service.ProjectService;
import com.teamflow.user.dto.UserView;
import com.teamflow.user.entity.UserEntity;
import com.teamflow.user.repository.UserMapper;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminService {
  private final UserMapper users;
  private final PasswordEncoder passwords;
  private final AuthSessionMapper sessions;
  private final ProjectService projects;
  private final AuditService audit;

  public AdminService(
      UserMapper users,
      PasswordEncoder passwords,
      AuthSessionMapper sessions,
      ProjectService projects,
      AuditService audit) {
    this.users = users;
    this.passwords = passwords;
    this.sessions = sessions;
    this.projects = projects;
    this.audit = audit;
  }

  @Transactional
  public void bootstrap(String username, String password) {
    if (users.lockAdminRole() == null) {
      throw new IllegalStateException("SYSTEM_ADMIN role missing; run Flyway first");
    }
    if (users.activeAdminCount() != 0) return;
    if (username.isBlank() || password.length() < 12) {
      throw new IllegalStateException("Bootstrap admin credentials are missing or too weak");
    }
    UserEntity user = new UserEntity();
    user.setUsername(username.trim());
    user.setDisplayName(username.trim());
    user.setPasswordHash(passwords.encode(password));
    user.setStatus("ACTIVE");
    try {
      users.insert(user);
    } catch (DataIntegrityViolationException exception) {
      throw new IllegalStateException("Bootstrap admin username already exists", exception);
    }
    users.grantAdmin(user.getId());
    audit.record(user.getId(), null, "USER", user.getId(), "ADMIN_BOOTSTRAPPED");
  }

  @Transactional
  public UserView createUser(long actorId, RegisterRequest request) {
    requireAdmin(actorId);
    UserEntity user = new UserEntity();
    user.setUsername(request.username().trim());
    user.setPasswordHash(passwords.encode(request.password()));
    user.setDisplayName(request.displayName().trim());
    user.setEmail(
        request.email() == null || request.email().isBlank() ? null : request.email().trim());
    user.setStatus("ACTIVE");
    try {
      users.insert(user);
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.REGISTRATION_CONFLICT);
    }
    audit.record(actorId, null, "USER", user.getId(), "USER_CREATED");
    return UserView.from(user);
  }

  @Transactional
  public UserView changeStatus(long actorId, long targetId, String status) {
    requireAdmin(actorId);
    if (!"ACTIVE".equals(status) && !"DISABLED".equals(status)) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
    UserEntity target = users.findById(targetId);
    if (target == null) throw new BusinessException(ErrorCode.NOT_FOUND);
    if ("DISABLED".equals(status)) {
      if (actorId == targetId) throw new BusinessException(ErrorCode.FORBIDDEN);
      if (users.isAdmin(targetId) != 0) {
        users.lockAdminRole();
        if (users.activeAdminCount() <= 1) {
          throw new BusinessException(ErrorCode.FORBIDDEN);
        }
      }
      if (projects.activeOwnedCount(targetId) != 0) {
        throw new BusinessException(ErrorCode.USER_ACTIVE_PROJECT_OWNER);
      }
      sessions.revokeUser(targetId);
    }
    users.updateStatus(targetId, status);
    audit.record(actorId, null, "USER", targetId, "USER_STATUS_CHANGED");
    return UserView.from(users.findById(targetId));
  }

  private void requireAdmin(long actorId) {
    if (users.isAdmin(actorId) == 0) throw new BusinessException(ErrorCode.FORBIDDEN);
  }

  @Transactional(readOnly = true)
  public PageResult<AdminUser> page(long actorId, int page, int pageSize) {
    requireAdmin(actorId);
    long offset = PageResult.offset(page, pageSize);
    List<AdminUser> items =
        users.pageUsers(pageSize, offset).stream().map(AdminUser::from).toList();
    return new PageResult<>(items, users.countUsers(), page, pageSize);
  }

  public record AdminUser(
      String id, String username, String displayName, String email, String status) {
    static AdminUser from(UserEntity user) {
      return new AdminUser(
          user.getId().toString(),
          user.getUsername(),
          user.getDisplayName(),
          user.getEmail(),
          user.getStatus());
    }
  }
}
