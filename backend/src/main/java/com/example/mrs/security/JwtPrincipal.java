package com.example.mrs.security;

public class JwtPrincipal {
  private final long id;
  private final String username;
  private final String role;

  public JwtPrincipal(long id, String username, String role) {
    this.id = id;
    this.username = username;
    this.role = role;
  }

  public long id() {
    return id;
  }

  public String username() {
    return username;
  }

  public String role() {
    return role;
  }
}

