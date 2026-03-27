package com.example.mrs.bootstrap;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mrs.domain.Role;
import com.example.mrs.entity.SysUserEntity;
import com.example.mrs.mapper.SysUserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class BootstrapAdminRunner implements ApplicationRunner {
  private static final int MAX_RETRY = 5;
  private static final long RETRY_INTERVAL_MS = 1000L;

  private final SysUserMapper userMapper;
  private final PasswordEncoder passwordEncoder;
  private final BootstrapAdminProps props;

  public BootstrapAdminRunner(SysUserMapper userMapper, PasswordEncoder passwordEncoder, BootstrapAdminProps props) {
    this.userMapper = userMapper;
    this.passwordEncoder = passwordEncoder;
    this.props = props;
  }

  @Override
  public void run(ApplicationArguments args) {
    String adminUsername = props.getAdminUsername();
    String adminPassword = props.getAdminPassword();
    String superAdminUsername = props.getSuperAdminUsername();
    String superAdminPassword = props.getSuperAdminPassword();

    for (int attempt = 1; attempt <= MAX_RETRY; attempt++) {
      try {
        doBootstrap(adminUsername, adminPassword, Role.ADMIN);
        doBootstrap(superAdminUsername, superAdminPassword, Role.SUPER_ADMIN);
        return;
      } catch (Exception e) {
        if (attempt == MAX_RETRY) {
          log.warn("Bootstrap admin/super-admin skipped after {} retries: {}", MAX_RETRY, e.getMessage());
          return;
        }
        log.warn("Bootstrap admin/super-admin attempt {}/{} failed: {}, retrying...", attempt, MAX_RETRY, e.getMessage());
        try {
          Thread.sleep(RETRY_INTERVAL_MS);
        } catch (InterruptedException ie) {
          Thread.currentThread().interrupt();
          log.warn("Bootstrap admin/super-admin retry interrupted");
          return;
        }
      }
    }
  }

  private void doBootstrap(String username, String password, Role role) {
    if (username == null || username.trim().isEmpty()) {
      return;
    }
    if (password == null || password.trim().isEmpty()) {
      log.warn("Bootstrap {} skipped: password is empty", role.name());
      return;
    }

    SysUserEntity exists = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
        .eq(SysUserEntity::getUsername, username)
        .last("limit 1"));
    if (exists == null) {
      SysUserEntity user = new SysUserEntity();
      user.setUsername(username);
      user.setPasswordHash(passwordEncoder.encode(password));
      user.setRole(role.name());
      user.setEnabled(true);
      userMapper.insert(user);
      log.info("Bootstrap {} created: {}", role.name(), username);
      return;
    }

    if (props.isResetAdminPasswordOnStart()) {
      exists.setPasswordHash(passwordEncoder.encode(password));
      exists.setRole(role.name());
      exists.setEnabled(true);
      userMapper.updateById(exists);
      log.info("Bootstrap {} password reset on start: {}", role.name(), username);
    }
  }
}
