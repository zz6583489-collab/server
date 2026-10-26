package com.teamflow.audit.service;

import com.teamflow.audit.entity.AuditEntity;
import com.teamflow.audit.repository.AuditMapper;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import com.teamflow.common.response.PageResult;
import com.teamflow.common.trace.TraceIdFilter;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/** Writes only named, non-sensitive business events. Call from the caller's transaction. */
@Service
public class AuditService {
  private static final Set<String> PROJECT_ACTIONS =
      Set.of(
          "PROJECT_CREATED",
          "PROJECT_UPDATED",
          "PROJECT_ARCHIVED",
          "MEMBER_ADDED",
          "MEMBER_REMOVED",
          "TASK_CREATED",
          "TASK_UPDATED",
          "TASK_STATUS_CHANGED");
  private final AuditMapper mapper;

  public AuditService(AuditMapper mapper) {
    this.mapper = mapper;
  }

  public void record(
      long actorId, Long projectId, String resourceType, long resourceId, String action) {
    AuditEntity event = new AuditEntity();
    event.setActorId(actorId);
    event.setProjectId(projectId);
    event.setResourceType(resourceType);
    event.setResourceId(resourceId);
    event.setAction(action);
    String traceId = MDC.get(TraceIdFilter.MDC_KEY);
    event.setTraceId(traceId == null ? UUID.randomUUID().toString() : traceId);
    event.setMetadataJson("{}");
    mapper.insert(event);
  }

  public PageResult<AuditEntity> page(long projectId, int page, int pageSize, Filter filter) {
    long offset = PageResult.offset(page, pageSize);
    List<AuditEntity> items =
        mapper.pageFiltered(
            projectId,
            pageSize,
            offset,
            filter.actorId(),
            filter.from(),
            filter.to(),
            filter.action());
    long total =
        mapper.countFiltered(
            projectId, filter.actorId(), filter.from(), filter.to(), filter.action());
    return new PageResult<>(items, total, page, pageSize);
  }

  public record Filter(Long actorId, LocalDateTime from, LocalDateTime to, String action) {
    public static Filter parse(
        String actorText, String fromText, String toText, String actionText) {
      Long actorId = null;
      if (actorText != null) {
        try {
          actorId = Long.parseLong(actorText);
          if (actorId <= 0) throw new NumberFormatException("nonpositive");
        } catch (NumberFormatException exception) {
          throw new BusinessException(ErrorCode.VALIDATION_ERROR);
        }
      }
      LocalDateTime from = parseTime(fromText);
      LocalDateTime to = parseTime(toText);
      if (from != null && to != null && !from.isBefore(to)) {
        throw new BusinessException(ErrorCode.VALIDATION_ERROR);
      }
      if (actionText != null && !PROJECT_ACTIONS.contains(actionText)) {
        throw new BusinessException(ErrorCode.VALIDATION_ERROR);
      }
      return new Filter(actorId, from, to, actionText);
    }

    private static LocalDateTime parseTime(String text) {
      if (text == null) return null;
      try {
        return LocalDateTime.ofInstant(Instant.parse(text), ZoneOffset.UTC);
      } catch (DateTimeParseException exception) {
        throw new BusinessException(ErrorCode.VALIDATION_ERROR);
      }
    }
  }
}
