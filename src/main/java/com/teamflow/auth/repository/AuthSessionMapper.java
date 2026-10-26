package com.teamflow.auth.repository;

import com.teamflow.auth.entity.AuthSessionEntity;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface AuthSessionMapper {
  @Insert(
      "INSERT INTO auth_session(id,user_id,refresh_hash,expires_at) "
          + "VALUES(#{id},#{userId},#{refreshHash},#{expiresAt})")
  int insert(AuthSessionEntity session);

  @Select("SELECT id,user_id,refresh_hash,expires_at,revoked_at FROM auth_session WHERE id=#{id}")
  AuthSessionEntity findById(String id);

  @Select(
      "SELECT COUNT(*) FROM auth_session WHERE id=#{id} AND user_id=#{userId} "
          + "AND revoked_at IS NULL AND expires_at>UTC_TIMESTAMP(3)")
  int isActive(@Param("id") String id, @Param("userId") long userId);

  @Update(
      "UPDATE auth_session SET refresh_hash=#{nextHash} "
          + "WHERE id=#{id} AND refresh_hash=#{previousHash} AND revoked_at IS NULL "
          + "AND expires_at>UTC_TIMESTAMP(3)")
  int rotate(
      @Param("id") String id,
      @Param("previousHash") String previousHash,
      @Param("nextHash") String nextHash);

  @Update(
      "UPDATE auth_session SET revoked_at=UTC_TIMESTAMP(3) "
          + "WHERE id=#{id} AND revoked_at IS NULL")
  int revoke(String id);

  @Update(
      "UPDATE auth_session SET revoked_at=UTC_TIMESTAMP(3) "
          + "WHERE user_id=#{userId} AND revoked_at IS NULL")
  int revokeUser(long userId);

  @Delete(
      "DELETE FROM auth_session WHERE expires_at<UTC_TIMESTAMP(3)-INTERVAL 1 DAY "
          + "OR revoked_at<UTC_TIMESTAMP(3)-INTERVAL 1 DAY")
  int deleteExpired();
}
