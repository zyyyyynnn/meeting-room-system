package com.example.mrs.security;

import com.example.mrs.common.BizException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {
  public static JwtPrincipal requirePrincipal() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || !(auth.getPrincipal() instanceof JwtPrincipal)) {
      throw BizException.forbidden("未登录");
    }
    return (JwtPrincipal) auth.getPrincipal();
  }
}

