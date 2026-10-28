package com.teamflow.notification.dto;

import com.teamflow.notification.entity.NotificationEntity;
import java.time.Instant;
import java.time.ZoneOffset;

public final class NotificationDtos {
  private NotificationDtos() {}

  public record View(
      String id,
      String type,
      String projectId,
      String projectName,
      String taskId,
      String taskTitle,
      String actorName,
      Instant createdAt,
      Instant readAt) {
    public static View from(NotificationEntity value) {
      return new View(
          value.getId().toString(),
          value.getType(),
          value.getProjectId().toString(),
          value.getProjectName(),
          value.getTaskId() == null ? null : value.getTaskId().toString(),
          value.getTaskTitle(),
          value.getActorName(),
          value.getCreatedAt().toInstant(ZoneOffset.UTC),
          value.getReadAt() == null ? null : value.getReadAt().toInstant(ZoneOffset.UTC));
    }
  }

  public record UnreadCount(long count) {}
}
