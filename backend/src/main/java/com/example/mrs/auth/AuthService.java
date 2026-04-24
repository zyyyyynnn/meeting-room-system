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
      ensureBootstrapPrivilegedUserForLogin(req);
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

  private void ensureBootstrapPrivilegedUserForLogin(AuthDtos.LoginReq req) {
    if (!Boolean.TRUE.equals(bootstrapAdminProps.isResetAdminPasswordOnStart())) {
      return;
    }
    if (ensureBootstrapUserForLogin(req, bootstrapAdminProps.getAdminUsername(), bootstrapAdminProps.getAdminPassword(), Role.ADMIN)) {
      return;
    }
    ensureBootstrapUserForLogin(
        req,
        bootstrapAdminProps.getSuperAdminUsername(),
        bootstrapAdminProps.getSuperAdminPassword(),
        Role.SUPER_ADMIN);
  }

  private boolean ensureBootstrapUserForLogin(AuthDtos.LoginReq req, String username, String password, Role role) {
    if (username == null || password == null) {
      return false;
    }
    if (!username.equals(req.getUsername()) || !password.equals(req.getPassword())) {
      return false;
    }
    SysUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
        .eq(SysUserEntity::getUsername, username)
        .last("limit 1"));
    if (user == null) {
      user = new SysUserEntity();
      user.setUsername(username);
      user.setRole(role.name());
      user.setEnabled(true);
      user.setPasswordHash(passwordEncoder.encode(password));
      userMapper.insert(user);
      return true;
    }

    user.setRole(role.name());
    user.setEnabled(true);
    user.setPasswordHash(passwordEncoder.encode(password));
    userMapper.updateById(user);
    return true;
  }
}
