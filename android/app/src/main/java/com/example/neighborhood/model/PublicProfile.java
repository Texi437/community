package com.example.neighborhood.model;

/** The complete public identity contract: no private fields can be serialized. */
public final class PublicProfile {
  public final String nickname;

  public PublicProfile(String nickname) {
    this.nickname = nickname;
  }
}
