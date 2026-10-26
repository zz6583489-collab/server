package com.teamflow.audit.repository;

import com.teamflow.audit.entity.AuditEntity;
import java.time.LocalDateTime;
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

  @Select({
    "<script>",
    "SELECT id,actor_id,project_id,resource_type,resource_id,action,trace_id,metadata_json,created_at",
    "FROM audit_log WHERE project_id=#{projectId}",
    "<if test='actorId != null'> AND actor_id=#{actorId}</if>",
    "<if test='from != null'> AND created_at &gt;= #{from}</if>",
    "<if test='to != null'> AND created_at &lt; #{to}</if>",
    "<if test='action != null'> AND action=#{action}</if>",
    "ORDER BY created_at DESC,id DESC LIMIT #{size} OFFSET #{offset}",
    "</script>"
  })
  List<AuditEntity> pageFiltered(
      @Param("projectId") long projectId,
      @Param("size") int size,
      @Param("offset") long offset,
      @Param("actorId") Long actorId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("action") String action);

  @Select({
    "<script>",
    "SELECT COUNT(*) FROM audit_log WHERE project_id=#{projectId}",
    "<if test='actorId != null'> AND actor_id=#{actorId}</if>",
    "<if test='from != null'> AND created_at &gt;= #{from}</if>",
    "<if test='to != null'> AND created_at &lt; #{to}</if>",
    "<if test='action != null'> AND action=#{action}</if>",
    "</script>"
  })
  long countFiltered(
      @Param("projectId") long projectId,
      @Param("actorId") Long actorId,
      @Param("from") LocalDateTime from,
      @Param("to") LocalDateTime to,
      @Param("action") String action);
}
