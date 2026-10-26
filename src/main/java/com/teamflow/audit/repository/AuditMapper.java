package com.teamflow.audit.repository;

import com.teamflow.audit.entity.AuditEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface AuditMapper {
  @Insert(
      "INSERT INTO audit_log(actor_id,project_id,resource_type,resource_id,action,trace_id,metadata_json) "
          + "VALUES(#{actorId},#{projectId},#{resourceType},#{resourceId},#{action},#{traceId},#{metadataJson})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(AuditEntity entity);

  @Select(
      "SELECT id,actor_id,project_id,resource_type,resource_id,action,trace_id,metadata_json,created_at "
          + "FROM audit_log WHERE project_id=#{projectId} ORDER BY created_at DESC,id DESC LIMIT #{size} OFFSET #{offset}")
  List<AuditEntity> page(
      @Param("projectId") long projectId, @Param("size") int size, @Param("offset") long offset);

  @Select("SELECT COUNT(*) FROM audit_log WHERE project_id=#{projectId}")
  long count(long projectId);
}
