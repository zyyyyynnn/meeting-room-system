package com.example.mrs.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.mrs.entity.SysUserEntity;
import com.example.mrs.mapper.SysUserMapper;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {
  private final SysUserMapper userMapper;

  public UserDetailsServiceImpl(SysUserMapper userMapper) {
    this.userMapper = userMapper;
  }

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    SysUserEntity user = userMapper.selectOne(new LambdaQueryWrapper<SysUserEntity>()
        .eq(SysUserEntity::getUsername, username)
        .last("limit 1"));
    if (user == null) {
      throw new UsernameNotFoundException("用户不存在");
    }
    return new AuthUser(
        user.getId(),
        user.getUsername(),
        user.getPasswordHash(),
        user.getRole(),
        Boolean.TRUE.equals(user.getEnabled()));
  }
}

