package com.teamflow.task;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

class TaskStatusMachineTest {
  @Test
  void followsRequiredWorkflow() {
    assertDoesNotThrow(() -> TaskStatusMachine.check("TODO", "IN_PROGRESS", false));
    assertDoesNotThrow(() -> TaskStatusMachine.check("IN_PROGRESS", "IN_REVIEW", false));
    assertDoesNotThrow(() -> TaskStatusMachine.check("IN_REVIEW", "DONE", true));
    assertDoesNotThrow(() -> TaskStatusMachine.check("IN_REVIEW", "IN_PROGRESS", true));
    assertDoesNotThrow(() -> TaskStatusMachine.check("DONE", "IN_PROGRESS", true));
  }

  @Test
  void rejectsIllegalJumpAndMemberReopen() {
    BusinessException jump =
        assertThrows(BusinessException.class, () -> TaskStatusMachine.check("TODO", "DONE", true));
    assertEquals(ErrorCode.TASK_INVALID_TRANSITION, jump.errorCode());
    BusinessException reopen =
        assertThrows(
            BusinessException.class, () -> TaskStatusMachine.check("DONE", "IN_PROGRESS", false));
    assertEquals(ErrorCode.TASK_INVALID_TRANSITION, reopen.errorCode());
    BusinessException review =
        assertThrows(
            BusinessException.class, () -> TaskStatusMachine.check("IN_REVIEW", "DONE", false));
    assertEquals(ErrorCode.FORBIDDEN, review.errorCode());
  }
}
