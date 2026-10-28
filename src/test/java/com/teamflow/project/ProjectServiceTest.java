package com.teamflow.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamflow.audit.service.AuditService;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.notification.service.NotificationService;
import com.teamflow.project.dto.ProjectDtos;
import com.teamflow.project.entity.ProjectEntity;
import com.teamflow.project.repository.ProjectMapper;
import com.teamflow.project.service.ProjectService;
import com.teamflow.user.repository.UserMapper;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectServiceTest {
  private ProjectMapper mapper;
  private AuditService audit;
  private ProjectService service;

  @BeforeEach
  void setUp() {
    mapper = mock(ProjectMapper.class);
    audit = mock(AuditService.class);
    service =
        new ProjectService(mapper, mock(UserMapper.class), audit, mock(NotificationService.class));
  }

  @Test
  void creatingProjectAlsoCreatesOwnerAndAuditEvent() {
    when(mapper.insert(any()))
        .thenAnswer(
            invocation -> {
              ProjectEntity project = invocation.getArgument(0);
              project.setId(5L);
              return 1;
            });
    when(mapper.findById(5L)).thenReturn(project("ACTIVE"));

    ProjectDtos.View result = service.create(7L, new ProjectDtos.Create("  Project  ", null));

    assertEquals("5", result.id());
    verify(mapper).addMember(5L, 7L, "OWNER");
    verify(audit).record(7L, 5L, "PROJECT", 5L, "PROJECT_CREATED");
  }

  @Test
  void invisibleProjectUsesSameNotFoundAsMissingProject() {
    when(mapper.findById(5L)).thenReturn(project("ACTIVE"));
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.detail(5L, 99L));
    assertEquals(ErrorCode.PROJECT_NOT_FOUND, exception.errorCode());
  }

  @Test
  void cannotRemoveMemberStillAssignedToAnyTask() {
    when(mapper.findForUpdate(5L)).thenReturn(project("ACTIVE"));
    when(mapper.memberRole(5L, 7L)).thenReturn("OWNER");
    when(mapper.memberRole(5L, 8L)).thenReturn("MEMBER");
    when(mapper.assignedTasks(5L, 8L)).thenReturn(1L);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.removeMember(5L, 7L, "8"));

    assertEquals(ErrorCode.PROJECT_MEMBER_HAS_TASKS, exception.errorCode());
    verify(mapper, never()).removeMember(5L, 8L);
  }

  @Test
  void archivedProjectRejectsMutation() {
    when(mapper.findForUpdate(5L)).thenReturn(project("ARCHIVED"));
    when(mapper.memberRole(5L, 7L)).thenReturn("OWNER");
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.archive(5L, 7L));
    assertEquals(ErrorCode.PROJECT_ARCHIVED, exception.errorCode());
  }

  @Test
  void rejectsUnknownProjectStatusFilter() {
    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.page(7L, 1, 20, "REMOVED"));
    assertEquals(ErrorCode.VALIDATION_ERROR, exception.errorCode());
  }

  private ProjectEntity project(String status) {
    ProjectEntity project = new ProjectEntity();
    project.setId(5L);
    project.setName("Project");
    project.setOwnerId(7L);
    project.setStatus(status);
    project.setCreatedAt(LocalDateTime.now());
    project.setUpdatedAt(LocalDateTime.now());
    return project;
  }
}
