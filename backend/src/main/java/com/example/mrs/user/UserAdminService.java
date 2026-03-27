package com.example.mrs.user;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mrs.common.BizException;
import com.example.mrs.domain.Role;
import com.example.mrs.entity.SysUserEntity;
import com.example.mrs.mapper.SysUserMapper;
import com.example.mrs.security.JwtPrincipal;
import com.example.mrs.security.SecurityUtil;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserAdminService {
  private final SysUserMapper userMapper;

  public UserAdminService(SysUserMapper userMapper) {
    this.userMapper = userMapper;
  }

  public List<UserAdminDtos.UserResp> listUsers() {
    List<SysUserEntity> list = userMapper.selectList(new LambdaQueryWrapper<SysUserEntity>()
        .orderByAsc(SysUserEntity::getId));
    return list.stream().map(this::toResp).collect(Collectors.toList());
  }

  @Transactional
  public void updateRole(long userId, String role) {
    JwtPrincipal me = SecurityUtil.requirePrincipal();

    String normalized = role == null ? null : role.trim().toUpperCase(Locale.ROOT);
    if (normalized == null) {
      throw BizException.badRequest("角色不能为空");
    }

    Role target;
    try {
      target = Role.valueOf(normalized);
    } catch (IllegalArgumentException e) {
      throw BizException.badRequest("角色非法");
    }

    SysUserEntity user = userMapper.selectById(userId);
    if (user == null) {
      throw BizException.notFound("用户不存在");
    }

    // 不允许任何人修改自己的角色，避免误操作导致权限丢失
    if (user.getId() != null && user.getId() == me.id()) {
      throw BizException.badRequest("不允许修改自己的角色");
    }

    boolean meIsSuperAdmin = Role.SUPER_ADMIN.name().equals(me.role());
    boolean targetIsSuperAdmin = Role.SUPER_ADMIN == target;
    boolean userIsSuperAdmin = Role.SUPER_ADMIN.name().equals(user.getRole());

    // 仅超级管理员可授予超级管理员
    if (targetIsSuperAdmin && !meIsSuperAdmin) {
      throw BizException.forbidden("仅超级管理员可授予超级管理员权限");
    }

    // 非超级管理员不可修改超级管理员账号角色
    if (userIsSuperAdmin && !meIsSuperAdmin) {
      throw BizException.forbidden("仅超级管理员可修改超级管理员账号");
    }

    user.setRole(target.name());
    userMapper.updateById(user);
  }

  @Transactional
  public void updateEnabled(long userId, Boolean enabled) {
    JwtPrincipal me = SecurityUtil.requirePrincipal();

    SysUserEntity user = userMapper.selectById(userId);
    if (user == null) {
      throw BizException.notFound("用户不存在");
    }

    // 保护自己不被停用
    if (user.getId() != null && user.getId() == me.id() && !Boolean.TRUE.equals(enabled)) {
      throw BizException.badRequest("不允许停用当前登录账号");
    }

    boolean meIsSuperAdmin = Role.SUPER_ADMIN.name().equals(me.role());
    boolean userIsSuperAdmin = Role.SUPER_ADMIN.name().equals(user.getRole());

    // 非超级管理员不可停用超级管理员账号
    if (userIsSuperAdmin && !meIsSuperAdmin) {
      throw BizException.forbidden("仅超级管理员可停用超级管理员账号");
    }

    user.setEnabled(Boolean.TRUE.equals(enabled));
    userMapper.updateById(user);
  }

  private UserAdminDtos.UserResp toResp(SysUserEntity user) {
    UserAdminDtos.UserResp resp = new UserAdminDtos.UserResp();
    resp.setId(user.getId());
    resp.setUsername(user.getUsername());
    resp.setRole(user.getRole());
    resp.setEnabled(user.getEnabled());
    return resp;
  }
}
