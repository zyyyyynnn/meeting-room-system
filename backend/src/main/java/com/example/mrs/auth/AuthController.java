package com.example.mrs.auth;

import com.example.mrs.auth.dto.AuthDtos;
import com.example.mrs.common.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "认证")
@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;

  public AuthController(AuthService authService) {
    this.authService = authService;
  }

  @PostMapping("/register")
  public ApiResponse<Void> register(@Valid @RequestBody AuthDtos.RegisterReq req) {
    authService.register(req);
    return ApiResponse.ok(null);
  }

  @PostMapping("/login")
  public ApiResponse<AuthDtos.LoginResp> login(@Valid @RequestBody AuthDtos.LoginReq req) {
    return ApiResponse.ok(authService.login(req));
  }
}

