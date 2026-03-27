package com.example.mrs;

import com.example.mrs.security.JwtProps;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProps.class, com.example.mrs.bootstrap.BootstrapAdminProps.class})
public class MeetingRoomSystemApplication {
  public static void main(String[] args) {
    SpringApplication.run(MeetingRoomSystemApplication.class, args);
  }
}

