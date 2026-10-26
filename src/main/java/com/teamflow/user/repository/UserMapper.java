package com.teamflow.user.repository;

import com.teamflow.user.entity.UserEntity;
import java.util.List;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
  @Select(
      "SELECT id, username, password_hash, display_name, email, status "
          + "FROM sys_user WHERE username = #{username}")
  UserEntity findByUsername(String username);

  @Select(
      "SELECT id, username, password_hash, display_name, email, status "
          + "FROM sys_user WHERE id = #{id}")
  UserEntity findById(long id);

  @Insert(
      "INSERT INTO sys_user (username, password_hash, display_name, email) "
          + "VALUES (#{username}, #{passwordHash}, #{displayName}, #{email})")
  @Options(useGeneratedKeys = true, keyProperty = "id")
  int insert(UserEntity user);

  @Select("SELECT id FROM sys_role WHERE code='SYSTEM_ADMIN' FOR UPDATE")
  Long lockAdminRole();

  @Insert(
      "INSERT INTO sys_user_role(user_id,role_id) SELECT #{userId},id FROM sys_role WHERE code='SYSTEM_ADMIN'")
  int grantAdmin(long userId);

  @Select(
      "SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id "
          + "WHERE ur.user_id=#{userId} AND r.code='SYSTEM_ADMIN'")
  int isAdmin(long userId);

  @Select(
      "SELECT COUNT(*) FROM sys_user_role ur JOIN sys_role r ON r.id=ur.role_id "
          + "JOIN sys_user u ON u.id=ur.user_id WHERE r.code='SYSTEM_ADMIN' AND u.status='ACTIVE'")
  int activeAdminCount();

  @Update("UPDATE sys_user SET status=#{status} WHERE id=#{userId}")
  int updateStatus(@Param("userId") long userId, @Param("status") String status);

  @Select(
      "SELECT id,username,display_name,email,status FROM sys_user "
          + "ORDER BY created_at DESC,id DESC LIMIT #{size} OFFSET #{offset}")
  List<UserEntity> pageUsers(@Param("size") int size, @Param("offset") long offset);

  @Select("SELECT COUNT(*) FROM sys_user")
  long countUsers();
}
