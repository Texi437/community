package com.example.neighborhood.model;

public final class TimeWindow {
  public long start;
  public long end;

  public TimeWindow(long start, long end) {
    if (end <= start) throw new IllegalArgumentException("结束时间必须晚于开始时间");
    this.start = start;
    this.end = end;
  }
}
