package com.example.mrs.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mrs.auth.dto.AuthDtos;
import com.example.mrs.bootstrap.BootstrapAdminProps;
import com.example.mrs.common.BizException;
import com.example.mrs.domain.Role;
import com.example.mrs.entity.SysUserEntity;
import com.example.mrs.mapper.SysUserMapper;
import com.example.mrs.security.JwtService;
import java.util.Objects;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
  private final SysUserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final BootstrapAdminProps bootstrapAdminProps;

  public AuthService(SysUserMapper userMapper,
                     PasswordEncoder passwordEncoder,
                     AuthenticationManager authenticationManager,
                     JwtService jwtService,
                     BootstrapAdminProps bootstrapAdminProps) {
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.authenticationManager = authenticationManager;
    this.jwtService = jwtService;
    this.bootstrapAdminProps = bootstrapAdminProps;
  }

  public void register(AuthDtos.RegisterReq req) {
    SysUserEntity exists = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
        .eq(SysUserEntity::getUsername, req.getUsername())
        .last("limit 1"));
    if (exists != null) {
      throw BizException.badRequest("用户名已存在");
    }
    SysUserEntity u = new SysUserEntity();
    u.setUsername(req.getUsername());
    u.setPasswordHash(passwordEncoder.encode(req.getPassword()));
    u.setRole(Role.USER.name());
    u.setEnabled(true);
    userMapper.insert(u);
  }

  public AuthDtos.LoginResp login(AuthDtos.LoginReq req) {
    try {
      authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
    } catch (AuthenticationException e) {
      ensureBootstrapAdminForLogin(req);
      try {
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
      } catch (AuthenticationException ignored) {
        throw BizException.badRequest("用户名或密码错误");
      }
    }

    SysUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
        .eq(SysUserEntity::getUsername, req.getUsername())
        .last("limit 1"));
    if (user == null || !Boolean.TRUE.equals(user.getEnabled())) {
      throw BizException.badRequest("用户不可用");
    }
    String token = jwtService.createToken(user.getId(), user.getUsername(), user.getRole());
    AuthDtos.LoginResp resp = new AuthDtos.LoginResp();
    resp.setToken(token);
    resp.setUserId(Objects.requireNonNull(user.getId()));
    resp.setUsername(user.getUsername());
    resp.setRole(user.getRole());
    return resp;
  }

  private void ensureBootstrapAdminForLogin(AuthDtos.LoginReq req) {
    if (!Boolean.TRUE.equals(bootstrapAdminProps.isResetAdminPasswordOnStart())) {
      return;
    }
    String adminUsername = bootstrapAdminProps.getAdminUsername();
    String adminPassword = bootstrapAdminProps.getAdminPassword();
    if (adminUsername == null || adminPassword == null) {
      return;
    }
    if (!adminUsername.equals(req.getUsername()) || !adminPassword.equals(req.getPassword())) {
      return;
    }

    SysUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
        .eq(SysUserEntity::getUsername, adminUsername)
        .last("limit 1"));
    if (user == null) {
      user = new SysUserEntity();
      user.setUsername(adminUsername);
      user.setRole(Role.ADMIN.name());
      user.setEnabled(true);
      user.setPasswordHash(passwordEncoder.encode(adminPassword));
      userMapper.insert(user);
      return;
    }

    user.setRole(Role.ADMIN.name());
    user.setEnabled(true);
    user.setPasswordHash(passwordEncoder.encode(adminPassword));
    userMapper.updateById(user);
  }
}

