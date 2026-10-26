package com.teamflow.project;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;

import com.teamflow.audit.service.AuditService;
import com.teamflow.project.dto.ProjectDtos;
import com.teamflow.project.service.ProjectService;
import com.teamflow.task.dto.TaskDtos;
import com.teamflow.task.service.TaskService;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/** Real MySQL transaction test; run only with an explicitly isolated database URL. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EnabledIfEnvironmentVariable(named = "TEAMFLOW_IT_DB_URL", matches = ".+")
class Phase2TransactionIT {
  @Autowired ProjectService projects;
  @Autowired TaskService tasks;
  @Autowired JdbcTemplate jdbc;
  @MockitoSpyBean AuditService audit;
  private Long userId;
  private Long projectId;

  @AfterEach
  void cleanup() {
    if (projectId != null) {
      jdbc.update("DELETE FROM audit_log WHERE project_id=?", projectId);
      jdbc.update(
          "DELETE FROM task_activity WHERE task_id IN (SELECT id FROM task WHERE project_id=?)",
          projectId);
      jdbc.update("DELETE FROM task WHERE project_id=?", projectId);
      jdbc.update("DELETE FROM project_member WHERE project_id=?", projectId);
      jdbc.update("DELETE FROM project WHERE id=?", projectId);
    }
    if (userId != null) {
      jdbc.update("DELETE FROM sys_user WHERE id=?", userId);
    }
  }

  @Test
  void auditFailureRollsBackTaskAndActivity() {
    String name = "task-rollback-" + UUID.randomUUID();
    String username = "test-" + UUID.randomUUID();
    jdbc.update(
        "INSERT INTO sys_user(username,password_hash,display_name) VALUES(?,?,?)",
        username,
        "test-hash",
        "Test");
    userId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
    jdbc.update("INSERT INTO project(name,owner_id,status) VALUES(?,?,'ACTIVE')", name, userId);
    projectId = jdbc.queryForObject("SELECT id FROM project WHERE name=?", Long.class, name);
    jdbc.update(
        "INSERT INTO project_member(project_id,user_id,member_role) VALUES(?,?,'OWNER')",
        projectId,
        userId);
    doThrow(new IllegalStateException("synthetic audit failure"))
        .when(audit)
        .record(eq(userId), eq(projectId), eq("TASK"), anyLong(), eq("TASK_CREATED"));

    assertThrows(
        IllegalStateException.class,
        () ->
            tasks.create(
                projectId,
                userId,
                new TaskDtos.Create("Rollback task", null, null, "MEDIUM", null)));

    assertEquals(
        0L,
        jdbc.queryForObject("SELECT COUNT(*) FROM task WHERE project_id=?", Long.class, projectId));
    assertEquals(
        0L,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM task_activity a JOIN task t ON t.id=a.task_id WHERE t.project_id=?",
            Long.class,
            projectId));
  }

  @Test
  void auditFailureRollsBackProjectAndOwnerMembership() {
    String name = "rollback-" + UUID.randomUUID();
    String username = "test-" + UUID.randomUUID();
    jdbc.update(
        "INSERT INTO sys_user(username,password_hash,display_name) VALUES(?,?,?)",
        username,
        "test-hash",
        "Test");
    userId = jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
    doThrow(new IllegalStateException("synthetic audit failure"))
        .when(audit)
        .record(anyLong(), any(), anyString(), anyLong(), anyString());

    assertThrows(
        IllegalStateException.class,
        () -> projects.create(userId, new ProjectDtos.Create(name, null)));

    assertEquals(
        0L, jdbc.queryForObject("SELECT COUNT(*) FROM project WHERE name=?", Long.class, name));
    assertEquals(
        0L,
        jdbc.queryForObject(
            "SELECT COUNT(*) FROM project_member WHERE user_id=?", Long.class, userId));
  }
}
