package com.teamflow.user.repository;

import com.teamflow.user.entity.UserEntity;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

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
}
