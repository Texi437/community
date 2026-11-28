package com.example.neighborhood.ui.common;

import com.example.neighborhood.model.TimeWindow;
import java.time.*;
import java.time.format.*;
import java.util.Locale;

public final class Times {
  public static final ZoneId ZONE = ZoneId.of("Asia/Shanghai");
  private static final DateTimeFormatter INPUT =
      DateTimeFormatter.ofPattern("uuuu-MM-dd HH:mm", Locale.CHINA)
          .withResolverStyle(ResolverStyle.STRICT);

  private Times() {}

  public static String format(long time) {
    return INPUT.format(Instant.ofEpochMilli(time).atZone(ZONE));
  }

  public static long parse(String text) {
    try {
      return LocalDateTime.parse(text.trim(), INPUT).atZone(ZONE).toInstant().toEpochMilli();
    } catch (DateTimeException e) {
      throw new IllegalArgumentException("时间格式应为 年-月-日 时:分，例如 2026-11-28 09:00");
    }
  }

  public static String range(TimeWindow time) {
    return format(time.start) + " — " + format(time.end);
  }

  public static long future(int days, int hour) {
    return LocalDate.now(ZONE)
        .plusDays(days)
        .atTime(hour, 0)
        .atZone(ZONE)
        .toInstant()
        .toEpochMilli();
  }
}
