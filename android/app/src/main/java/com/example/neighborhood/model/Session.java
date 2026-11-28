package com.example.neighborhood.model;

public final class Session {
  public enum Role {
    RESIDENT,
    WORKER,
    ADMIN
  }

  public String userId;
  public String nickname;
  public Role role;
  public boolean canQueryResidents;
  public String accessToken;
  public long expiresAt;

  public Session(String userId, String nickname, Role role, boolean canQueryResidents) {
    this.userId = userId;
    this.nickname = nickname;
    this.role = role;
    this.canQueryResidents = canQueryResidents;
  }
}
