package com.example.neighborhood.domain;

public final class PrivacyPolicy {
  private PrivacyPolicy() {}

  public static void require(boolean allowed) {
    if (!allowed) throw new IllegalStateException("你没有此项操作权限");
  }
}
