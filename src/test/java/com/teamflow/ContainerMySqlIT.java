package com.teamflow;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.project.dto.ProjectDtos;
import com.teamflow.project.service.ProjectService;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
class ContainerMySqlIT {
  private static final String TEST_SECRET = UUID.randomUUID() + UUID.randomUUID().toString();

  @Container
  static final MySQLContainer<?> MYSQL =
      new MySQLContainer<>("mysql:8.4.13")
          .withDatabaseName("teamflow_it")
          .withUsername("teamflow")
          .withPassword(UUID.randomUUID().toString());

  @DynamicPropertySource
  static void properties(DynamicPropertyRegistry registry) {
    registry.add(
        "spring.datasource.url",
        () ->
            MYSQL.getJdbcUrl()
                + (MYSQL.getJdbcUrl().contains("?") ? "&" : "?")
                + "connectionTimeZone=UTC&forceConnectionTimeZoneToSession=true");
    registry.add("spring.datasource.username", MYSQL::getUsername);
    registry.add("spring.datasource.password", MYSQL::getPassword);
    registry.add("teamflow.jwt.secret", () -> TEST_SECRET);
    registry.add("teamflow.bootstrap.username", () -> "");
    registry.add("teamflow.bootstrap.password", () -> "");
  }

  @Autowired Flyway flyway;
  @Autowired JdbcTemplate jdbc;
  @Autowired ProjectService projects;

  @Test
  void freshMigrationEnforcesCheckAndSecondMigrationIsNoop() {
    assertEquals("6", flyway.info().current().getVersion().toString());
    assertEquals(0, flyway.migrate().migrationsExecuted);
    assertThrows(
        DataIntegrityViolationException.class,
        () ->
            jdbc.update(
                "INSERT INTO sys_user(username,password_hash,display_name,status) "
                    + "VALUES(?,?,?,'INVALID')",
                "check-" + UUID.randomUUID(),
                "hash",
                "Check"));
  }

  @Test
  @Transactional
  void projectMembershipImmediatelyGatesCrossAccountAccess() {
    long ownerId = createUser("owner");
    long memberId = createUser("member");
    long projectId =
        Long.parseLong(
            projects.create(ownerId, new ProjectDtos.Create("Container project", null)).id());
    BusinessException denied =
        assertThrows(BusinessException.class, () -> projects.detail(projectId, memberId));
    assertEquals(ErrorCode.PROJECT_NOT_FOUND, denied.errorCode());
    projects.addMember(projectId, ownerId, Long.toString(memberId));
    assertEquals(Long.toString(projectId), projects.detail(projectId, memberId).id());
    projects.removeMember(projectId, ownerId, Long.toString(memberId));
    assertThrows(BusinessException.class, () -> projects.detail(projectId, memberId));
  }

  private long createUser(String prefix) {
    String username = prefix + "-" + UUID.randomUUID();
    jdbc.update(
        "INSERT INTO sys_user(username,password_hash,display_name) VALUES(?,?,?)",
        username,
        "test-hash",
        prefix);
    return jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
  }
}
