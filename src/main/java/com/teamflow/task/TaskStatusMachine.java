package com.teamflow.task;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;

/** The only allowed transitions in the P0 task workflow. */
public final class TaskStatusMachine {
  private TaskStatusMachine() {}

  public static void check(String current, String next, boolean owner) {
    if ("IN_REVIEW".equals(current)
        && ("DONE".equals(next) || "IN_PROGRESS".equals(next))
        && !owner) {
      throw new BusinessException(ErrorCode.FORBIDDEN);
    }
    boolean allowed =
        switch (current) {
          case "TODO" -> "IN_PROGRESS".equals(next);
          case "IN_PROGRESS" -> "IN_REVIEW".equals(next);
          case "IN_REVIEW" -> "DONE".equals(next) || "IN_PROGRESS".equals(next);
          case "DONE" -> owner && "IN_PROGRESS".equals(next);
          default -> false;
        };
    if (!allowed) {
      throw new BusinessException(ErrorCode.TASK_INVALID_TRANSITION);
    }
  }
}
