package com.teamflow.task.repository;

import com.teamflow.task.entity.TaskActivityEntity;
import com.teamflow.task.entity.TaskEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface TaskMapper {
  @Insert(
      "INSERT INTO task(project_id,title,description,assignee_id,reporter_id,status,priority,due_at,version) "
          + "VALUES(#{projectId},#{title},#{description},#{assigneeId},#{reporterId},'TODO',#{priority},#{dueAt},0)")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(TaskEntity task);

  @Select(
      "SELECT id,project_id,title,description,assignee_id,reporter_id,status,priority,due_at,version,created_at,updated_at "
          + "FROM task WHERE id=#{id}")
  TaskEntity findById(long id);

  @Select(
      "SELECT t.id,t.project_id,t.title,t.description,t.assignee_id,t.reporter_id,t.status,t.priority,t.due_at,t.version,t.created_at,t.updated_at "
          + "FROM task t JOIN project_member m ON m.project_id=t.project_id "
          + "WHERE t.id=#{id} AND m.user_id=#{userId}")
  TaskEntity findVisible(@Param("id") long id, @Param("userId") long userId);

  @Select(
      "SELECT id,project_id,title,description,assignee_id,reporter_id,status,priority,due_at,version,created_at,updated_at "
          + "FROM task WHERE project_id=#{projectId} ORDER BY created_at DESC,id DESC LIMIT #{size} OFFSET #{offset}")
  List<TaskEntity> page(
      @Param("projectId") long projectId, @Param("size") int size, @Param("offset") long offset);

  @Select("SELECT COUNT(*) FROM task WHERE project_id=#{projectId}")
  long count(long projectId);

  @Update(
      "UPDATE task SET title=#{title},description=#{description},assignee_id=#{assigneeId},"
          + "status=#{status},priority=#{priority},due_at=#{dueAt},version=version+1 "
          + "WHERE id=#{id} AND version=#{version}")
  int update(TaskEntity task);

  @Insert(
      "INSERT INTO task_activity(task_id,actor_id,action,from_value,to_value) "
          + "VALUES(#{taskId},#{actorId},#{action},#{fromValue},#{toValue})")
  int addActivity(TaskActivityEntity activity);

  @Select(
      "SELECT id,task_id,actor_id,action,from_value,to_value,created_at FROM task_activity "
          + "WHERE task_id=#{taskId} ORDER BY created_at DESC,id DESC LIMIT #{size} OFFSET #{offset}")
  List<TaskActivityEntity> activities(
      @Param("taskId") long taskId, @Param("size") int size, @Param("offset") long offset);

  @Select("SELECT COUNT(*) FROM task_activity WHERE task_id=#{taskId}")
  long activityCount(long taskId);
}
