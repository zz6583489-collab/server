package com.teamflow.task.service;

import com.teamflow.audit.service.AuditService;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.common.response.PageResult;
import com.teamflow.project.service.ProjectService;
import com.teamflow.task.TaskStatusMachine;
import com.teamflow.task.dto.TaskDtos;
import com.teamflow.task.entity.TaskActivityEntity;
import com.teamflow.task.entity.TaskEntity;
import com.teamflow.task.repository.TaskMapper;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {
  private final TaskMapper tasks;
  private final ProjectService projects;
  private final AuditService audit;

  public TaskService(TaskMapper tasks, ProjectService projects, AuditService audit) {
    this.tasks = tasks;
    this.projects = projects;
    this.audit = audit;
  }

  @Transactional
  public TaskDtos.View create(long projectId, long actorId, TaskDtos.Create input) {
    projects.requireWritable(projectId, actorId);
    Long assigneeId = parseAssignee(projectId, input.assigneeId());
    TaskEntity task = new TaskEntity();
    task.setProjectId(projectId);
    task.setTitle(input.title().trim());
    task.setDescription(input.description());
    task.setAssigneeId(assigneeId);
    task.setReporterId(actorId);
    task.setPriority(input.priority() == null ? "MEDIUM" : input.priority());
    task.setDueAt(parseDueAt(input.dueAt()));
    try {
      tasks.insert(task);
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.TASK_ASSIGNEE_NOT_MEMBER);
    }
    activity(task.getId(), actorId, "TASK_CREATED", null, null);
    audit.record(actorId, projectId, "TASK", task.getId(), "TASK_CREATED");
    return TaskDtos.View.from(tasks.findById(task.getId()));
  }

  @Transactional(readOnly = true)
  public PageResult<TaskDtos.View> page(long projectId, long actorId, int page, int pageSize) {
    projects.requireMember(projectId, actorId);
    long offset = PageResult.offset(page, pageSize);
    List<TaskDtos.View> items =
        tasks.page(projectId, pageSize, offset).stream().map(TaskDtos.View::from).toList();
    return new PageResult<>(items, tasks.count(projectId), page, pageSize);
  }

  @Transactional(readOnly = true)
  public TaskDtos.View detail(long taskId, long actorId) {
    return TaskDtos.View.from(requireVisible(taskId, actorId));
  }

  @Transactional
  public TaskDtos.View edit(long taskId, long actorId, TaskDtos.Edit input) {
    TaskEntity task = requireVisible(taskId, actorId);
    projects.requireWritable(task.getProjectId(), actorId);
    requireVersion(task, input.version());
    boolean owner = projects.isOwner(task.getProjectId(), actorId);
    boolean reporter = task.getReporterId() == actorId;
    boolean assignee = task.getAssigneeId() != null && task.getAssigneeId() == actorId;
    boolean coreChange =
        input.title() != null
            || input.priority() != null
            || input.dueAt() != null
            || input.clearDueAt();
    boolean assignmentChange = input.assigneeId() != null || input.unassign();
    if (input.dueAt() != null && input.clearDueAt()) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
    if (!coreChange && !assignmentChange && input.description() == null) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
    if ((coreChange && !owner && !reporter)
        || (input.description() != null && !owner && !reporter && !assignee)
        || (assignmentChange && !owner)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    if (input.assigneeId() != null && input.unassign()) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
    if (input.title() != null) {
      if (input.title().isBlank()) throw new BusinessException(ErrorCode.VALIDATION_ERROR);
      task.setTitle(input.title().trim());
    }
    if (input.description() != null) task.setDescription(input.description());
    if (input.priority() != null) task.setPriority(input.priority());
    if (input.dueAt() != null) task.setDueAt(parseDueAt(input.dueAt()));
    if (input.clearDueAt()) task.setDueAt(null);
    if (input.assigneeId() != null) {
      task.setAssigneeId(parseAssignee(task.getProjectId(), input.assigneeId()));
    }
    if (input.unassign()) task.setAssigneeId(null);
    update(task);
    activity(taskId, actorId, "TASK_UPDATED", null, null);
    audit.record(actorId, task.getProjectId(), "TASK", taskId, "TASK_UPDATED");
    return TaskDtos.View.from(tasks.findById(taskId));
  }

  @Transactional
  public TaskDtos.View transition(long taskId, long actorId, TaskDtos.Transition input) {
    TaskEntity task = requireVisible(taskId, actorId);
    projects.requireWritable(task.getProjectId(), actorId);
    requireVersion(task, input.version());
    boolean owner = projects.isOwner(task.getProjectId(), actorId);
    if (task.getAssigneeId() == null || (!owner && task.getAssigneeId() != actorId)) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    TaskStatusMachine.check(task.getStatus(), input.toStatus(), owner);
    String before = task.getStatus();
    task.setStatus(input.toStatus());
    update(task);
    activity(taskId, actorId, "STATUS_CHANGED", before, input.toStatus());
    audit.record(actorId, task.getProjectId(), "TASK", taskId, "TASK_STATUS_CHANGED");
    return TaskDtos.View.from(tasks.findById(taskId));
  }

  @Transactional(readOnly = true)
  public PageResult<TaskDtos.Activity> activities(
      long taskId, long actorId, int page, int pageSize) {
    requireVisible(taskId, actorId);
    long offset = PageResult.offset(page, pageSize);
    List<TaskDtos.Activity> items =
        tasks.activities(taskId, pageSize, offset).stream().map(TaskDtos.Activity::from).toList();
    return new PageResult<>(items, tasks.activityCount(taskId), page, pageSize);
  }

  private TaskEntity requireVisible(long taskId, long actorId) {
    TaskEntity task = tasks.findVisible(taskId, actorId);
    if (task == null) throw new BusinessException(ErrorCode.TASK_NOT_FOUND);
    return task;
  }

  private void requireVersion(TaskEntity task, String version) {
    long expected = parseVersion(version);
    if (task.getVersion() != expected) {
      throw new BusinessException(ErrorCode.TASK_VERSION_CONFLICT);
    }
  }

  private void update(TaskEntity task) {
    try {
      if (tasks.update(task) == 0) {
        throw new BusinessException(ErrorCode.TASK_VERSION_CONFLICT);
      }
    } catch (DataIntegrityViolationException exception) {
      throw new BusinessException(ErrorCode.TASK_ASSIGNEE_NOT_MEMBER);
    }
  }

  private Long parseAssignee(long projectId, String value) {
    if (value == null) return null;
    long userId = ProjectService.parseId(value);
    if (!projects.isActiveMember(projectId, userId)) {
      throw new BusinessException(ErrorCode.TASK_ASSIGNEE_NOT_MEMBER);
    }
    return userId;
  }

  private LocalDateTime parseDueAt(String value) {
    if (value == null) return null;
    try {
      return LocalDateTime.ofInstant(Instant.parse(value), ZoneOffset.UTC);
    } catch (DateTimeParseException exception) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
  }

  private long parseVersion(String value) {
    try {
      long version = Long.parseLong(value);
      if (version >= 0) return version;
    } catch (NumberFormatException exception) {
      // Reject malformed and out-of-range versions with one stable response.
    }
    throw new BusinessException(ErrorCode.VALIDATION_ERROR);
  }

  private void activity(long taskId, long actorId, String action, String before, String after) {
    TaskActivityEntity event = new TaskActivityEntity();
    event.setTaskId(taskId);
    event.setActorId(actorId);
    event.setAction(action);
    event.setFromValue(before);
    event.setToValue(after);
    tasks.addActivity(event);
  }
}
