package com.teamflow.project.repository;

import com.teamflow.project.entity.ProjectEntity;
import com.teamflow.project.entity.ProjectMemberEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ProjectMapper {
  @Insert(
      "INSERT INTO project(name,description,owner_id,status) VALUES(#{name},#{description},#{ownerId},'ACTIVE')")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(ProjectEntity project);

  @Select(
      "SELECT id,name,description,owner_id,status,created_at,updated_at FROM project WHERE id=#{id}")
  ProjectEntity findById(long id);

  @Select(
      "SELECT id,name,description,owner_id,status,created_at,updated_at FROM project WHERE id=#{id} FOR UPDATE")
  ProjectEntity findForUpdate(long id);

  @Select({
    "<script>",
    "SELECT p.id,p.name,p.description,p.owner_id,p.status,p.created_at,p.updated_at",
    "FROM project p JOIN project_member m ON m.project_id=p.id WHERE m.user_id=#{userId}",
    "<if test='status != null'> AND p.status=#{status}</if>",
    "ORDER BY p.created_at DESC,p.id DESC LIMIT #{size} OFFSET #{offset}",
    "</script>"
  })
  List<ProjectEntity> pageFiltered(
      @Param("userId") long userId,
      @Param("size") int size,
      @Param("offset") long offset,
      @Param("status") String status);

  @Select({
    "<script>",
    "SELECT COUNT(*) FROM project p JOIN project_member m",
    "ON m.project_id=p.id WHERE m.user_id=#{userId}",
    "<if test='status != null'> AND p.status=#{status}</if>",
    "</script>"
  })
  long countFiltered(@Param("userId") long userId, @Param("status") String status);

  @Select(
      "SELECT member_role FROM project_member WHERE project_id=#{projectId} AND user_id=#{userId}")
  String memberRole(@Param("projectId") long projectId, @Param("userId") long userId);

  @Insert(
      "INSERT INTO project_member(project_id,user_id,member_role) VALUES(#{projectId},#{userId},#{role})")
  int addMember(
      @Param("projectId") long projectId, @Param("userId") long userId, @Param("role") String role);

  @Select(
      "SELECT m.user_id,u.username,u.display_name,m.member_role,m.joined_at "
          + "FROM project_member m JOIN sys_user u ON u.id=m.user_id "
          + "WHERE m.project_id=#{projectId} ORDER BY m.joined_at,m.id")
  List<ProjectMemberEntity> members(long projectId);

  @Select(
      "SELECT m.user_id,u.username,u.display_name,m.member_role,m.joined_at "
          + "FROM project_member m JOIN sys_user u ON u.id=m.user_id "
          + "WHERE m.project_id=#{projectId} AND m.user_id=#{userId}")
  ProjectMemberEntity findMember(@Param("projectId") long projectId, @Param("userId") long userId);

  @Select("SELECT COUNT(*) FROM task WHERE project_id=#{projectId} AND assignee_id=#{userId}")
  long assignedTasks(@Param("projectId") long projectId, @Param("userId") long userId);

  @Delete(
      "DELETE FROM project_member WHERE project_id=#{projectId} AND user_id=#{userId} AND member_role='MEMBER'")
  int removeMember(@Param("projectId") long projectId, @Param("userId") long userId);

  @Update(
      "UPDATE project SET name=#{name},description=#{description} WHERE id=#{id} AND status='ACTIVE'")
  int update(ProjectEntity project);

  @Update("UPDATE project SET status='ARCHIVED' WHERE id=#{id} AND status='ACTIVE'")
  int archive(long id);

  @Select("SELECT COUNT(*) FROM project WHERE owner_id=#{userId} AND status='ACTIVE'")
  long activeOwned(long userId);
}
