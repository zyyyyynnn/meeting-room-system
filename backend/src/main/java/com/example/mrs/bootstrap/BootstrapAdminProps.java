package com.example.mrs.bootstrap;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.bootstrap")
public class BootstrapAdminProps {
  private String adminUsername;
  private String adminPassword;
  private String superAdminUsername;
  private String superAdminPassword;
  private boolean resetAdminPasswordOnStart;
}

