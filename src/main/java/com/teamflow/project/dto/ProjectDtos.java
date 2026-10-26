package com.teamflow.project.dto;

import com.teamflow.project.entity.ProjectEntity;
import com.teamflow.project.entity.ProjectMemberEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.time.ZoneOffset;

public final class ProjectDtos {
  private ProjectDtos() {}

  public record Create(
      @NotBlank @Size(max = 160) String name, @Size(max = 10000) String description) {}

  public record Edit(
      @Size(min = 1, max = 160) String name, @Size(max = 10000) String description) {}

  public record AddMember(@NotBlank @Pattern(regexp = "[1-9][0-9]*") String userId) {}

  public record View(
      String id,
      String name,
      String description,
      String ownerId,
      String status,
      Instant createdAt,
      Instant updatedAt) {
    public static View from(ProjectEntity project) {
      return new View(
          project.getId().toString(),
          project.getName(),
          project.getDescription(),
          project.getOwnerId().toString(),
          project.getStatus(),
          project.getCreatedAt().toInstant(ZoneOffset.UTC),
          project.getUpdatedAt().toInstant(ZoneOffset.UTC));
    }
  }

  public record Member(
      String userId, String username, String displayName, String role, Instant joinedAt) {
    public static Member from(ProjectMemberEntity member) {
      return new Member(
          member.getUserId().toString(),
          member.getUsername(),
          member.getDisplayName(),
          member.getMemberRole(),
          member.getJoinedAt().toInstant(ZoneOffset.UTC));
    }
  }
}
