package com.teamflow.notification.repository;

import com.teamflow.notification.entity.NotificationEntity;
import java.util.List;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface NotificationMapper {
  @Insert(
      "INSERT INTO user_notification(recipient_id,actor_id,project_id,task_id,type) "
          + "VALUES(#{recipientId},#{actorId},#{projectId},#{taskId},#{type})")
  int insert(NotificationEntity notification);

  @Select({
    "<script>",
    "SELECT n.id,n.recipient_id,n.actor_id,n.project_id,n.task_id,n.type,n.read_at,n.created_at,",
    "p.name AS project_name,t.title AS task_title,u.display_name AS actor_name",
    "FROM user_notification n",
    "JOIN project_member m ON m.project_id=n.project_id AND m.user_id=n.recipient_id",
    "JOIN project p ON p.id=n.project_id",
    "JOIN sys_user u ON u.id=n.actor_id",
    "LEFT JOIN task t ON t.id=n.task_id",
    "WHERE n.recipient_id=#{recipientId}",
    "<if test='unreadOnly'> AND n.read_at IS NULL</if>",
    "ORDER BY n.created_at DESC,n.id DESC LIMIT #{size} OFFSET #{offset}",
    "</script>"
  })
  List<NotificationEntity> pageVisible(
      @Param("recipientId") long recipientId,
      @Param("unreadOnly") boolean unreadOnly,
      @Param("size") int size,
      @Param("offset") long offset);

  @Select({
    "<script>",
    "SELECT COUNT(*) FROM user_notification n",
    "JOIN project_member m ON m.project_id=n.project_id AND m.user_id=n.recipient_id",
    "WHERE n.recipient_id=#{recipientId}",
    "<if test='unreadOnly'> AND n.read_at IS NULL</if>",
    "</script>"
  })
  long countVisible(
      @Param("recipientId") long recipientId, @Param("unreadOnly") boolean unreadOnly);

  @Select(
      "SELECT n.id FROM user_notification n "
          + "JOIN project_member m ON m.project_id=n.project_id AND m.user_id=n.recipient_id "
          + "WHERE n.id=#{id} AND n.recipient_id=#{recipientId}")
  Long findVisible(@Param("id") long id, @Param("recipientId") long recipientId);

  @Update(
      "UPDATE user_notification n "
          + "JOIN project_member m ON m.project_id=n.project_id AND m.user_id=n.recipient_id "
          + "SET n.read_at=UTC_TIMESTAMP(3) "
          + "WHERE n.id=#{id} AND n.recipient_id=#{recipientId} AND n.read_at IS NULL")
  int markRead(@Param("id") long id, @Param("recipientId") long recipientId);

  @Update(
      "UPDATE user_notification n "
          + "JOIN project_member m ON m.project_id=n.project_id AND m.user_id=n.recipient_id "
          + "SET n.read_at=UTC_TIMESTAMP(3) "
          + "WHERE n.recipient_id=#{recipientId} AND n.read_at IS NULL")
  int markAllRead(long recipientId);

  @Delete("DELETE FROM user_notification WHERE created_at < UTC_TIMESTAMP(3) - INTERVAL 180 DAY")
  int deleteExpired();
}
