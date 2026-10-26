package com.teamflow.auth.repository;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface LoginAttemptMapper {
  @Select(
      "SELECT COUNT(*) FROM auth_login_attempt WHERE key_hash=#{key} "
          + "AND failures>=#{limit} AND window_started_at>UTC_TIMESTAMP(3)-INTERVAL 15 MINUTE")
  int isBlocked(@Param("key") String key, @Param("limit") int limit);

  @Insert(
      "INSERT INTO auth_login_attempt(key_hash,failures,window_started_at) "
          + "VALUES(#{key},1,UTC_TIMESTAMP(3)) ON DUPLICATE KEY UPDATE "
          + "failures=IF(window_started_at<UTC_TIMESTAMP(3)-INTERVAL 15 MINUTE,1,failures+1), "
          + "window_started_at=IF(window_started_at<UTC_TIMESTAMP(3)-INTERVAL 15 MINUTE,"
          + "UTC_TIMESTAMP(3),window_started_at)")
  int recordFailure(String key);

  @Delete("DELETE FROM auth_login_attempt WHERE key_hash=#{key}")
  int clear(String key);

  @Delete("DELETE FROM auth_login_attempt WHERE window_started_at<UTC_TIMESTAMP(3)-INTERVAL 1 DAY")
  int deleteExpired();
}
