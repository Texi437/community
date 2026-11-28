package com.example.neighborhood.model;

import java.util.List;

public final class DictionaryOption {
  public final String code;
  public final String label;

  public DictionaryOption(String code, String label) {
    this.code = code;
    this.label = label;
  }

  public static String label(List<DictionaryOption> options, String code, String fallback) {
    for (DictionaryOption option : options) if (option.code.equals(code)) return option.label;
    return fallback;
  }

  @Override public String toString() { return label; }
}
