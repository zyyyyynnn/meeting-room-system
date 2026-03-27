package com.example.mrs.common;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
  @Schema(description = "业务码，0 表示成功")
  private int code;

  @Schema(description = "提示信息")
  private String message;

  @Schema(description = "数据")
  private T data;

  public static <T> ApiResponse<T> ok(T data) {
    return new ApiResponse<>(0, "OK", data);
  }

  public static <T> ApiResponse<T> error(int code, String message) {
    return new ApiResponse<>(code, message, null);
  }
}

