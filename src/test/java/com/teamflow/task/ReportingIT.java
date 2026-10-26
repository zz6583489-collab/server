package com.teamflow.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.teamflow.audit.service.AuditService;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.project.service.ProjectService;
import com.teamflow.task.dto.TaskDtos;
import com.teamflow.task.service.TaskService;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EnabledIfEnvironmentVariable(named = "TEAMFLOW_IT_DB_URL", matches = ".+")
class ReportingIT {
  @Autowired JdbcTemplate jdbc;
  @Autowired TaskService tasks;
  @Autowired ProjectService projects;
  @Autowired AuditService audit;
  private long userId;
  private long projectId;

  @BeforeEach
  void prepare() {
    String username = "report-" + UUID.randomUUID();
    String projectName = "report-" + UUID.randomUUID();
    jdbc.update(
        "INSERT INTO sys_user(username,password_hash,display_name) VALUES(?,?,?)",
        username,
        "test-hash",
        "Reporter");
    userId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
    jdbc.update(
        "INSERT INTO project(name,owner_id,status) VALUES(?,?,'ACTIVE')", projectName, userId);
    projectId = jdbc.queryForObject("SELECT id FROM project WHERE name=?", Long.class, projectName);
    jdbc.update(
        "INSERT INTO project_member(project_id,user_id,member_role) VALUES(?,?,'OWNER')",
        projectId,
        userId);
    addTask("Overdue", "TODO", "LOW", true, "2020-01-01 00:00:00.000");
    addTask("In progress", "IN_PROGRESS", "HIGH", true, "2099-01-01 00:00:00.000");
    addTask("Finished", "DONE", "HIGH", false, "2020-01-01 00:00:00.000");
    addAudit("PROJECT_CREATED", "2026-10-26 00:00:00.000");
    addAudit("TASK_UPDATED", "2026-10-26 01:00:00.000");
  }

  @AfterEach
  void cleanup() {
    jdbc.update("DELETE FROM audit_log WHERE project_id=?", projectId);
    jdbc.update("DELETE FROM task WHERE project_id=?", projectId);
    jdbc.update("DELETE FROM project_member WHERE project_id=?", projectId);
    jdbc.update("DELETE FROM project WHERE id=?", projectId);
    jdbc.update("DELETE FROM sys_user WHERE id=?", userId);
  }

  @Test
  void countsStatusOverdueAndCompletionRate() {
    TaskDtos.Stats result = tasks.stats(projectId, userId);
    assertEquals(3, result.total());
    assertEquals(1, result.todo());
    assertEquals(1, result.inProgress());
    assertEquals(0, result.inReview());
    assertEquals(1, result.done());
    assertEquals(1, result.overdue());
    assertEquals(new BigDecimal("33.3"), result.completionRatePercent());
    BusinessException denied =
        assertThrows(BusinessException.class, () -> tasks.stats(projectId, 9_999_999L));
    assertEquals(ErrorCode.PROJECT_NOT_FOUND, denied.errorCode());
  }

  @Test
  void combinesTaskFiltersWithStablePageCount() {
    var filtered = tasks.page(projectId, userId, 1, 20, new TaskDtos.Filter("DONE", "HIGH", null));
    assertEquals(1, filtered.total());
    assertEquals("Finished", filtered.items().getFirst().title());
    var assigned =
        tasks.page(
            projectId, userId, 1, 20, new TaskDtos.Filter(null, null, Long.toString(userId)));
    assertEquals(2, assigned.total());
    assertEquals(2, assigned.items().size());
    assertEquals(1, projects.page(userId, 1, 20, "ACTIVE").total());
    assertEquals(0, projects.page(userId, 1, 20, "ARCHIVED").total());
  }

  @Test
  void filtersAuditByActorTimeAndAction() {
    var result =
        audit.page(
            projectId,
            1,
            20,
            AuditService.Filter.parse(
                Long.toString(userId),
                "2026-10-26T00:30:00Z",
                "2026-10-26T02:00:00Z",
                "TASK_UPDATED"));
    assertEquals(1, result.total());
    assertEquals("TASK_UPDATED", result.items().getFirst().getAction());
  }

  private void addTask(
      String title, String status, String priority, boolean assigned, String dueAt) {
    jdbc.update(
        "INSERT INTO task(project_id,title,assignee_id,reporter_id,status,priority,due_at) "
            + "VALUES(?,?,?,?,?,?,?)",
        projectId,
        title,
        assigned ? userId : null,
        userId,
        status,
        priority,
        dueAt);
  }

  private void addAudit(String action, String createdAt) {
    jdbc.update(
        "INSERT INTO audit_log(actor_id,project_id,resource_type,resource_id,action,"
            + "trace_id,metadata_json,created_at) VALUES(?,?,?,? ,?,?,?,?)",
        userId,
        projectId,
        "TASK",
        projectId,
        action,
        UUID.randomUUID().toString(),
        "{}",
        createdAt);
  }
}
