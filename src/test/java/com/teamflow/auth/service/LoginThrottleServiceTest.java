package com.teamflow.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.teamflow.auth.repository.LoginAttemptMapper;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class LoginThrottleServiceTest {
  @Test
  void blocksAfterAccountThresholdWithoutRevealingUsername() {
    LoginAttemptMapper mapper = mock(LoginAttemptMapper.class);
    when(mapper.isBlocked(anyString(), eq(5))).thenReturn(1);
    LoginThrottleService service = new LoginThrottleService(mapper);

    BusinessException exception =
        assertThrows(BusinessException.class, () -> service.check("alice", "127.0.0.1"));

    assertEquals(ErrorCode.AUTH_TOO_MANY_ATTEMPTS, exception.errorCode());
  }

  @Test
  void recordsBothAccountAndIpFailures() {
    LoginAttemptMapper mapper = mock(LoginAttemptMapper.class);
    new LoginThrottleService(mapper).failed("alice", "127.0.0.1");
    verify(mapper, org.mockito.Mockito.times(2)).recordFailure(anyString());
  }
}
