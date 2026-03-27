package com.example.mrs.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
  @Bean
  public OpenAPI openAPI() {
    return new OpenAPI().info(new Info()
        .title("会议室预约与资源协调系统 API")
        .version("v1")
        .description("Spring Boot + MyBatis-Plus + MySQL + Redis + Knife4j"));
  }
}

