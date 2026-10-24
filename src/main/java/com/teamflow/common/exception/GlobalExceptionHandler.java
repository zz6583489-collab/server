package com.teamflow.common.exception;

import com.teamflow.common.response.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {
  private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

  @ExceptionHandler(BusinessException.class)
  public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException exception) {
    ErrorCode error = exception.errorCode();
    log.warn("Business request rejected: {}", error.code());
    return ResponseEntity.status(error.status())
        .body(ApiResponse.failure(error.code(), error.message()));
  }

  @ExceptionHandler({
    MethodArgumentNotValidException.class,
    BindException.class,
    ConstraintViolationException.class,
    HttpMessageNotReadableException.class,
    MethodArgumentTypeMismatchException.class
  })
  public ResponseEntity<ApiResponse<Void>> handleValidation(Exception exception) {
    log.warn("Request validation failed: {}", exception.getClass().getSimpleName());
    ErrorCode error = ErrorCode.VALIDATION_ERROR;
    return ResponseEntity.status(error.status())
        .body(ApiResponse.failure(error.code(), error.message()));
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception exception) {
    log.error("Unhandled request failure", exception);
    ErrorCode error = ErrorCode.INTERNAL_ERROR;
    return ResponseEntity.status(error.status())
        .body(ApiResponse.failure(error.code(), error.message()));
  }

  @ExceptionHandler(NoResourceFoundException.class)
  public ResponseEntity<ApiResponse<Void>> handleNotFound(NoResourceFoundException exception) {
    ErrorCode error = ErrorCode.NOT_FOUND;
    return ResponseEntity.status(error.status())
        .body(ApiResponse.failure(error.code(), error.message()));
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMethod(
      HttpRequestMethodNotSupportedException exception) {
    ErrorCode error = ErrorCode.METHOD_NOT_ALLOWED;
    return ResponseEntity.status(error.status())
        .body(ApiResponse.failure(error.code(), error.message()));
  }

  @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
  public ResponseEntity<ApiResponse<Void>> handleMediaType(
      HttpMediaTypeNotSupportedException exception) {
    ErrorCode error = ErrorCode.UNSUPPORTED_MEDIA_TYPE;
    return ResponseEntity.status(error.status())
        .body(ApiResponse.failure(error.code(), error.message()));
  }
}
