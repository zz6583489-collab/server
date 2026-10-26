package com.teamflow.audit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.teamflow.audit.service.AuditService;
import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class AuditFilterTest {
  @Test
  void acceptsInclusiveUtcStartAndExclusiveUtcEnd() {
    AuditService.Filter filter =
        AuditService.Filter.parse(
            "42", "2026-10-01T00:00:00Z", "2026-11-01T00:00:00Z", "TASK_UPDATED");
    assertEquals(42L, filter.actorId());
    assertEquals("TASK_UPDATED", filter.action());
  }

  @Test
  void rejectsBackwardsRangeAndUnknownAction() {
    BusinessException range =
        assertThrows(
            BusinessException.class,
            () ->
                AuditService.Filter.parse(
                    null, "2026-11-01T00:00:00Z", "2026-10-01T00:00:00Z", null));
    assertEquals(ErrorCode.VALIDATION_ERROR, range.errorCode());
    BusinessException action =
        assertThrows(
            BusinessException.class,
            () -> AuditService.Filter.parse(null, null, null, "PASSWORD_RESET"));
    assertEquals(ErrorCode.VALIDATION_ERROR, action.errorCode());
  }
}
