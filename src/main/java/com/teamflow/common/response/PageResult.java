package com.teamflow.common.response;

import com.teamflow.common.exception.BusinessException;
import com.teamflow.common.exception.ErrorCode;
import java.util.List;

public record PageResult<T>(List<T> items, long total, int page, int pageSize) {
  public static void validate(int page, int pageSize) {
    if (page < 1 || pageSize < 1 || pageSize > 100) {
      throw new BusinessException(ErrorCode.VALIDATION_ERROR);
    }
  }

  public static long offset(int page, int pageSize) {
    validate(page, pageSize);
    return ((long) page - 1) * pageSize;
  }
}
