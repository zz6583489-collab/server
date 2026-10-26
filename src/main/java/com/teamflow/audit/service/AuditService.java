package com.teamflow.audit.service;

import com.teamflow.audit.entity.AuditEntity;
import com.teamflow.audit.repository.AuditMapper;
import com.teamflow.common.trace.TraceIdFilter;
import java.util.List;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

/** Writes only named, non-sensitive business events. Call from the caller's transaction. */
@Service
public class AuditService {
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

  public List<AuditEntity> page(long projectId, int size, long offset) {
    return mapper.page(projectId, size, offset);
  }

  public long count(long projectId) {
    return mapper.count(projectId);
  }
}
