package com.teamflow.notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.notification.repository.NotificationMapper;
import com.teamflow.notification.service.NotificationService;
import com.teamflow.project.dto.ProjectDtos;
import com.teamflow.project.service.ProjectService;
import com.teamflow.task.dto.TaskDtos;
import com.teamflow.task.service.TaskService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@EnabledIfEnvironmentVariable(named = "TEAMFLOW_IT_DB_URL", matches = ".+")
@Transactional
class NotificationIT {
  @Autowired JdbcTemplate jdbc;
  @Autowired ProjectService projects;
  @Autowired TaskService tasks;
  @Autowired NotificationService notifications;
  @Autowired NotificationMapper mapper;
  private long ownerId;
  private long memberId;
  private long projectId;

  @BeforeEach
  void prepare() {
    ownerId = user("owner");
    memberId = user("member");
    projectId =
        Long.parseLong(projects.create(ownerId, new ProjectDtos.Create("Notice test", "")).id());
    projects.addMember(projectId, ownerId, Long.toString(memberId));
  }

  @Test
  void removedMemberCannotSeeOrMarkOldNotification() {
    var page = notifications.page(memberId, 1, 20, true);
    assertEquals(1, page.total());
    long notificationId = Long.parseLong(page.items().getFirst().id());
    assertEquals(0, notifications.unreadCount(ownerId).count());

    projects.removeMember(projectId, ownerId, Long.toString(memberId));

    assertEquals(0, notifications.unreadCount(memberId).count());
    BusinessException denied =
        assertThrows(
            BusinessException.class, () -> notifications.markRead(notificationId, memberId));
    assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, denied.errorCode());
  }

  @Test
  void taskEventsAreRecipientScopedAndReadActionsAreIdempotent() {
    var task =
        tasks.create(
            projectId,
            ownerId,
            new TaskDtos.Create("Assigned task", null, Long.toString(memberId), "HIGH", null));
    assertEquals(2, notifications.unreadCount(memberId).count());
    assertEquals(0, notifications.unreadCount(ownerId).count());

    var started =
        tasks.transition(
            Long.parseLong(task.id()),
            memberId,
            new TaskDtos.Transition(task.version(), "IN_PROGRESS"));
    var review =
        tasks.transition(
            Long.parseLong(task.id()),
            memberId,
            new TaskDtos.Transition(started.version(), "IN_REVIEW"));
    assertEquals(1, notifications.unreadCount(ownerId).count());
    var rejected =
        tasks.transition(
            Long.parseLong(task.id()),
            ownerId,
            new TaskDtos.Transition(review.version(), "IN_PROGRESS"));
    assertEquals(3, notifications.unreadCount(memberId).count());
    var resubmitted =
        tasks.transition(
            Long.parseLong(task.id()),
            memberId,
            new TaskDtos.Transition(rejected.version(), "IN_REVIEW"));
    tasks.transition(
        Long.parseLong(task.id()), ownerId, new TaskDtos.Transition(resubmitted.version(), "DONE"));
    assertEquals(4, notifications.unreadCount(memberId).count());

    long noticeId =
        Long.parseLong(notifications.page(memberId, 1, 20, true).items().getFirst().id());
    assertEquals(2, notifications.page(ownerId, 1, 20, true).total());
    BusinessException denied =
        assertThrows(BusinessException.class, () -> notifications.markRead(noticeId, ownerId));
    assertEquals(ErrorCode.NOTIFICATION_NOT_FOUND, denied.errorCode());
    notifications.markRead(noticeId, memberId);
    notifications.markRead(noticeId, memberId);
    assertEquals(3, notifications.unreadCount(memberId).count());
    notifications.markAllRead(memberId);
    assertEquals(0, notifications.unreadCount(memberId).count());
    assertEquals(4, notifications.page(memberId, 1, 20, false).total());
  }

  @Test
  void oldNotificationsAreRemovedAfterRetentionPeriod() {
    long id = Long.parseLong(notifications.page(memberId, 1, 20, false).items().getFirst().id());
    jdbc.update(
        "UPDATE user_notification SET created_at=UTC_TIMESTAMP(3)-INTERVAL 181 DAY WHERE id=?", id);
    assertEquals(1, mapper.deleteExpired());
    assertEquals(0, notifications.unreadCount(memberId).count());
  }

  private long user(String prefix) {
    String username = prefix + "-" + UUID.randomUUID();
    jdbc.update(
        "INSERT INTO sys_user(username,password_hash,display_name) VALUES(?,?,?)",
        username,
        "test-hash",
        prefix);
    return jdbc.queryForObject("SELECT id FROM sys_user WHERE username=?", Long.class, username);
  }
}
