package com.example.mrs.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(BizException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> handleBiz(BizException e) {
    return ApiResponse.error(e.getCode(), e.getMessage());
  }

  @ExceptionHandler(AccessDeniedException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> handleDenied(AccessDeniedException e) {
    return ApiResponse.error(403, "无权限访问");
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> handleValidation(MethodArgumentNotValidException e) {
    String msg = e.getBindingResult().getAllErrors().stream()
        .findFirst()
        .map(err -> err.getDefaultMessage())
        .orElse("参数校验失败");
    return ApiResponse.error(400, msg);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> handleConstraint(ConstraintViolationException e) {
    return ApiResponse.error(400, e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  @ResponseStatus(HttpStatus.OK)
  public ApiResponse<Void> handleUnknown(Exception e) {
    log.error("Unhandled exception", e);
    return ApiResponse.error(500, "系统错误");
  }
}

