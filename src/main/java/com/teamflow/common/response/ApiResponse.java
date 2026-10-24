package com.teamflow.common.response;

import com.teamflow.common.trace.TraceIdFilter;
import org.slf4j.MDC;

public record ApiResponse<T>(String code, String message, T data, String traceId) {
  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>("OK", "success", data, MDC.get(TraceIdFilter.MDC_KEY));
  }

  public static ApiResponse<Void> failure(String code, String message) {
    return new ApiResponse<>(code, message, null, MDC.get(TraceIdFilter.MDC_KEY));
  }
}
