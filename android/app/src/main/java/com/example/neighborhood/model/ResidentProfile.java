package com.example.neighborhood.model;

public final class ResidentProfile {
  public String userId;
  public String nickname;
  public String building;
  public String unit;
  public String room;

  public ResidentProfile(
      String userId, String nickname, String building, String unit, String room) {
    this.userId = userId;
    this.nickname = nickname;
    this.building = building;
    this.unit = unit;
    this.room = room;
  }
}
