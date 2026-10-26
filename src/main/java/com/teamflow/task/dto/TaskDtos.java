package com.teamflow.task.dto;

import com.teamflow.task.entity.TaskActivityEntity;
import com.teamflow.task.entity.TaskEntity;
import com.teamflow.task.entity.TaskStatsEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;

public final class TaskDtos {
  private TaskDtos() {}

  public record Create(
      @NotBlank @Size(max = 200) String title,
      @Size(max = 10000) String description,
      @Pattern(regexp = "[1-9][0-9]*") String assigneeId,
      @Pattern(regexp = "LOW|MEDIUM|HIGH") String priority,
      String dueAt) {}

  public record Edit(
      @NotBlank String version,
      @Size(min = 1, max = 200) String title,
      @Size(max = 10000) String description,
      @Pattern(regexp = "[1-9][0-9]*") String assigneeId,
      boolean unassign,
      @Pattern(regexp = "LOW|MEDIUM|HIGH") String priority,
      String dueAt,
      boolean clearDueAt) {}

  public record Transition(@NotBlank String version, @NotBlank String toStatus) {}

  public record Filter(String status, String priority, String assigneeId) {}

  public record Stats(
      long total,
      long todo,
      long inProgress,
      long inReview,
      long done,
      long overdue,
      BigDecimal completionRatePercent) {
    public static Stats from(TaskStatsEntity value) {
      BigDecimal rate =
          value.getTotal() == 0
              ? BigDecimal.ZERO
              : BigDecimal.valueOf(value.getDone())
                  .multiply(BigDecimal.valueOf(100))
                  .divide(BigDecimal.valueOf(value.getTotal()), 1, RoundingMode.HALF_UP);
      return new Stats(
          value.getTotal(),
          value.getTodo(),
          value.getInProgress(),
          value.getInReview(),
          value.getDone(),
          value.getOverdue(),
          rate);
    }
  }

  public record View(
      String id,
      String projectId,
      String title,
      String description,
      String assigneeId,
      String reporterId,
      String status,
      String priority,
      Instant dueAt,
      String version,
      Instant createdAt,
      Instant updatedAt) {
    public static View from(TaskEntity task) {
      return new View(
          task.getId().toString(),
          task.getProjectId().toString(),
          task.getTitle(),
          task.getDescription(),
          task.getAssigneeId() == null ? null : task.getAssigneeId().toString(),
          task.getReporterId().toString(),
          task.getStatus(),
          task.getPriority(),
          task.getDueAt() == null ? null : task.getDueAt().toInstant(ZoneOffset.UTC),
          task.getVersion().toString(),
          task.getCreatedAt().toInstant(ZoneOffset.UTC),
          task.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
  }

  public record Activity(
      String id,
      String actorId,
      String action,
      String fromValue,
      String toValue,
      Instant createdAt) {
    public static Activity from(TaskActivityEntity activity) {
      return new Activity(
          activity.getId().toString(),
          activity.getActorId().toString(),
          activity.getAction(),
          activity.getFromValue(),
          activity.getToValue(),
          activity.getCreatedAt().toInstant(ZoneOffset.UTC));
    }
  }
}
