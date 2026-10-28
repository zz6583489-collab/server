package com.teamflow.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamflow.audit.service.AuditService;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.notification.service.NotificationService;
import com.teamflow.project.entity.ProjectEntity;
import com.teamflow.project.service.ProjectService;
import com.teamflow.task.dto.TaskDtos;
import com.teamflow.task.entity.TaskEntity;
import com.teamflow.task.repository.TaskMapper;
import com.teamflow.task.service.TaskService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TaskServiceTest {
  private TaskMapper mapper;
  private ProjectService projects;
  private AuditService audit;
  private NotificationService notifications;
  private TaskService service;

  @BeforeEach
  void setUp() {
    mapper = mock(TaskMapper.class);
    projects = mock(ProjectService.class);
    audit = mock(AuditService.class);
    notifications = mock(NotificationService.class);
    service = new TaskService(mapper, projects, audit, notifications);
    when(mapper.findVisible(10L, 8L)).thenReturn(task());
    when(projects.requireWritable(5L, 8L)).thenReturn(new ProjectEntity());
  }

  @Test
  void assigneeCannotChangeOwnerOnlyFields() {
    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () ->
                service.edit(
                    10L,
                    8L,
                    new TaskDtos.Edit("0", "Renamed", null, null, false, null, null, false)));
    assertEquals(ErrorCode.FORBIDDEN, exception.errorCode());
    verify(mapper, never()).update(any());
  }

  @Test
  void staleVersionRejectsBeforeWritingActivityOrAudit() {
    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.transition(10L, 8L, new TaskDtos.Transition("7", "IN_PROGRESS")));
    assertEquals(ErrorCode.TASK_VERSION_CONFLICT, exception.errorCode());
    verify(mapper, never()).update(any());
    verify(mapper, never()).addActivity(any());
  }

  @Test
  void illegalTransitionRejectsWithoutWriting() {
    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.transition(10L, 8L, new TaskDtos.Transition("0", "DONE")));
    assertEquals(ErrorCode.TASK_INVALID_TRANSITION, exception.errorCode());
    verify(mapper, never()).addActivity(any());
  }

  @Test
  void acceptedTransitionWritesActivityAndAudit() {
    when(mapper.update(any())).thenReturn(1);
    when(mapper.findById(10L)).thenReturn(task());

    service.transition(10L, 8L, new TaskDtos.Transition("0", "IN_PROGRESS"));

    verify(mapper).addActivity(any());
    verify(audit).record(8L, 5L, "TASK", 10L, "TASK_STATUS_CHANGED");
  }

  @Test
  void rejectsUnknownTaskFilterBeforeQuerying() {
    BusinessException exception =
        assertThrows(
            BusinessException.class,
            () -> service.page(5L, 8L, 1, 20, new TaskDtos.Filter("INVALID", null, null)));
    assertEquals(ErrorCode.VALIDATION_ERROR, exception.errorCode());
    verify(mapper, never()).pageFiltered(anyLong(), anyInt(), anyLong(), any(), any(), any());
  }

  @Test
  void emptyProjectStatsHaveZeroCompletionRate() {
    when(mapper.stats(5L)).thenReturn(new com.teamflow.task.entity.TaskStatsEntity());
    TaskDtos.Stats result = service.stats(5L, 8L);
    assertEquals(0, result.total());
    assertEquals(java.math.BigDecimal.ZERO, result.completionRatePercent());
  }

  private TaskEntity task() {
    TaskEntity task = new TaskEntity();
    task.setId(10L);
    task.setProjectId(5L);
    task.setTitle("Task");
    task.setReporterId(7L);
    task.setAssigneeId(8L);
    task.setStatus("TODO");
    task.setPriority("MEDIUM");
    task.setVersion(0L);
    task.setCreatedAt(LocalDateTime.now());
    task.setUpdatedAt(LocalDateTime.now());
    return task;
  }
}
