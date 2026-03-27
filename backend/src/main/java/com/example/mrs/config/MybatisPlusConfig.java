package com.example.mrs.config;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MybatisPlusConfig {
  @Bean
  public MybatisPlusInterceptor mybatisPlusInterceptor() {
    // This project does not rely on MyBatis-Plus paging APIs (IPage/Page),
    // so we keep the interceptor list empty to stay compatible across versions.
    return new MybatisPlusInterceptor();
  }
}

