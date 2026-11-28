package com.example.neighborhood.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class InterestProfile {

  public enum Intent {
    UNKNOWN,
    WILLING,
    NOT_NOW
  }

  public Map<String, Integer> strengths = new LinkedHashMap<>();
  public Intent intent = Intent.UNKNOWN;
  public List<TimeWindow> availability = new ArrayList<>();
  public long validUntil;
  public int version;
}
